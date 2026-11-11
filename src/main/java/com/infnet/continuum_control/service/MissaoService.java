package com.infnet.continuum_control.service;

import com.infnet.continuum_control.dto.missao.*;
import com.infnet.continuum_control.exception.RecursoNaoEncontradoException;
import com.infnet.continuum_control.exception.RegraDeNegocioException;
import com.infnet.continuum_control.model.entities.*;
import com.infnet.continuum_control.model.enums.*;
import com.infnet.continuum_control.repository.*;
import com.infnet.continuum_control.security.UsuarioAutenticado;
import com.infnet.continuum_control.service.indicadores.IndicadoresService;
import jakarta.persistence.criteria.Join;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Casos de uso da missão. Cada método público é uma transação (§9, §20): as operações compostas
 * do doc 03 (6.1 iniciar, 6.2 encerrar, 6.3 cancelar) mudam missão, agentes, anomalia e histórico
 * juntos; se qualquer passo falhar, nada é gravado.
 */
@Service
@RequiredArgsConstructor
public class MissaoService {

    static final Set<SituacaoMissao> PENDENTES = EnumSet.of(SituacaoMissao.PLANEJADA, SituacaoMissao.AUTORIZADA);

    private final MissaoRepository missaoRepository;
    private final AnomaliaRepository anomaliaRepository;
    private final AgenteRepository agenteRepository;
    private final UsuarioRepository usuarioRepository;
    private final HistoricoService historico;
    private final IndicadoresService indicadores;
    private final Clock clock;

    /** M1. */
    @Transactional
    public MissaoResponse criar(CriarMissaoRequest req, UsuarioAutenticado usuario) {
        if (missaoRepository.existsByCodigo(req.codigo())) {
            throw new RegraDeNegocioException(null, "Já existe uma missão com o código " + req.codigo());
        }
        Anomalia anomalia = anomaliaRepository.findById(req.anomaliaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Anomalia", req.anomaliaId()));
        Missao missao = missaoRepository.save(new Missao(req.codigo(), req.objetivo(), req.proposito(), anomalia,
                req.destinoTemporal(), req.prioridade()));
        historico.registrar(anomalia, missao, TipoRegistro.MISSAO_CRIADA,
                "Missão " + missao.getCodigo() + " criada para " + missao.getProposito(), null, missao.getCodigo(), usuario);
        return MissaoResponse.de(missao);
    }

    @Transactional(readOnly = true)
    public Page<MissaoResponse> listar(SituacaoMissao situacao, Long anomaliaId, Long agenteId,
                                       PropositoMissao proposito, Pageable pageable) {
        Specification<Missao> doAgente = (root, query, cb) -> {
            query.distinct(true);
            Join<Missao, MissaoAgente> participacao = root.join("participacoes");
            return cb.equal(participacao.get("agente").get("id"), agenteId);
        };
        Specification<Missao> filtros = Filtros.de(Missao.class)
                .igual("situacao", situacao)
                .igual("proposito", proposito)
                .igualNoCaminho("anomalia", "id", anomaliaId)
                .condicao(doAgente, agenteId != null)
                .montar();
        return missaoRepository.findAll(filtros, pageable).map(MissaoResponse::de);
    }

    @Transactional(readOnly = true)
    public MissaoResponse buscarPorId(Long id) {
        return MissaoResponse.de(buscarEntidade(id));
    }

    /** D28: só em PLANEJADA. */
    @Transactional
    public MissaoResponse atualizar(Long id, AtualizarMissaoRequest req) {
        Missao missao = buscarEntidade(id);
        missao.atualizarDados(req.objetivo(), req.destinoTemporal(), req.prioridade());
        return MissaoResponse.de(missao);
    }

    /** M2. */
    @Transactional
    public MissaoResponse designar(Long id, Long agenteId, UsuarioAutenticado usuario) {
        Missao missao = buscarEntidade(id);
        Agente agente = buscarAgente(agenteId);
        missao.designar(agente, agora());
        historico.registrar(missao.getAnomalia(), missao, TipoRegistro.AGENTE_DESIGNADO,
                "Agente " + agente.getCodigo() + " designado para a missão " + missao.getCodigo(),
                null, agente.getCodigo(), usuario);
        missaoRepository.flush();
        return MissaoResponse.de(missao);
    }

    /** M3 (D25). */
    @Transactional
    public void removerDesignacao(Long id, Long agenteId, UsuarioAutenticado usuario) {
        Missao missao = buscarEntidade(id);
        Agente agente = buscarAgente(agenteId);
        missao.removerDesignacao(agente);
        historico.registrar(missao.getAnomalia(), missao, TipoRegistro.AGENTE_REMOVIDO,
                "Agente " + agente.getCodigo() + " removido da missão " + missao.getCodigo(),
                agente.getCodigo(), null, usuario);
    }

    /** M4: o responsável é o usuário autenticado (S4). */
    @Transactional
    public MissaoResponse autorizar(Long id, String observacao, UsuarioAutenticado usuario) {
        Missao missao = buscarEntidade(id);
        Usuario responsavel = usuarioRepository.findById(usuario.id())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", usuario.id()));
        missao.autorizar(responsavel, observacao, agora());
        historico.registrar(missao.getAnomalia(), missao, TipoRegistro.AUTORIZACAO,
                "Missão " + missao.getCodigo() + " autorizada por " + responsavel.getLogin() + ": " + observacao,
                SituacaoMissao.PLANEJADA, SituacaoMissao.AUTORIZADA, usuario);
        missaoRepository.flush();
        return MissaoResponse.de(missao);
    }

    /**
     * Operação 6.1 (M5/M6): missão para EM_EXECUCAO, cada agente para EM_MISSAO (G2) e a anomalia
     * para EM_ANALISE ou EM_CORRECAO, com os registros, numa única transação.
     */
    @Transactional
    public MissaoResponse iniciar(Long id, UsuarioAutenticado usuario) {
        Missao missao = buscarEntidade(id);
        Anomalia anomalia = missao.getAnomalia();
        if (missao.isPendente() && missaoRepository.existsByAnomaliaIdAndSituacao(anomalia.getId(), SituacaoMissao.EM_EXECUCAO)) {
            throw new RegraDeNegocioException("D23", "Já existe uma missão em execução para esta anomalia");
        }
        SituacaoAnomalia anomaliaAntes = anomalia.getSituacao();

        missao.iniciar(agora());

        for (MissaoAgente participacao : missao.getParticipacoes()) {
            historico.registrarAgente(participacao.getAgente(), SituacaoAgente.DISPONIVEL,
                    "Missão " + missao.getCodigo() + " iniciada", null);
        }
        historico.registrar(anomalia, missao, TipoRegistro.MISSAO_INICIADA,
                "Missão " + missao.getCodigo() + " iniciada com " + missao.getParticipacoes().size() + " agente(s)",
                null, SituacaoMissao.EM_EXECUCAO, usuario);
        registrarMudancaDaAnomalia(anomalia, missao, anomaliaAntes, "início da missão " + missao.getCodigo(), usuario);

        missaoRepository.flush();   // uk_missao_em_execucao e @Version dos agentes barram inícios concorrentes
        indicadores.invalidar();
        return MissaoResponse.de(missao);
    }

    /** Operação 6.2 (M10). */
    @Transactional
    public MissaoResponse concluir(Long id, EncerrarMissaoRequest req, UsuarioAutenticado usuario) {
        return encerrar(id, req, usuario, SituacaoMissao.CONCLUIDA);
    }

    /** Operação 6.2 (M11). */
    @Transactional
    public MissaoResponse registrarFalha(Long id, EncerrarMissaoRequest req, UsuarioAutenticado usuario) {
        return encerrar(id, req, usuario, SituacaoMissao.FALHOU);
    }

    private MissaoResponse encerrar(Long id, EncerrarMissaoRequest req, UsuarioAutenticado usuario,
                                    SituacaoMissao desfecho) {
        Missao missao = buscarEntidade(id);
        Anomalia anomalia = missao.getAnomalia();
        SituacaoAnomalia anomaliaAntes = anomalia.getSituacao();
        List<MissaoAgente> abertas = missao.getParticipacoes().stream().filter(MissaoAgente::isAberta).toList();

        if (desfecho == SituacaoMissao.CONCLUIDA) {
            missao.concluir(req.paraDados(), agora());
        } else {
            missao.registrarFalha(req.paraDados(), agora());
        }

        String verbo = desfecho == SituacaoMissao.CONCLUIDA ? "concluída" : "falhou";
        for (MissaoAgente participacao : abertas) {
            historico.registrarAgente(participacao.getAgente(), SituacaoAgente.EM_MISSAO,
                    "Missão " + missao.getCodigo() + " " + verbo, null);
        }
        historico.registrar(anomalia, missao, TipoRegistro.ENCERRAMENTO,
                "Missão " + missao.getCodigo() + " " + verbo + ": " + req.resumo(),
                SituacaoMissao.EM_EXECUCAO, desfecho, usuario);
        registrarMudancaDaAnomalia(anomalia, missao, anomaliaAntes, "encerramento da missão " + missao.getCodigo(), usuario);
        cancelarInadmissiveis(anomalia, "a anomalia passou a " + anomalia.getSituacao());

        missaoRepository.flush();
        indicadores.invalidar();
        return MissaoResponse.de(missao);
    }

    /** Operação 6.3 (M7). */
    @Transactional
    public MissaoResponse cancelar(Long id, String motivo, UsuarioAutenticado usuario) {
        Missao missao = buscarEntidade(id);
        SituacaoMissao antes = missao.getSituacao();
        missao.cancelar(agora());
        historico.registrar(missao.getAnomalia(), missao, TipoRegistro.MISSAO_CANCELADA,
                "Missão " + missao.getCodigo() + " cancelada: " + motivo, antes, SituacaoMissao.CANCELADA, usuario);
        return MissaoResponse.de(missao);
    }

    /**
     * M8 (D24): quando a anomalia deixa de admitir uma missão pendente, ela é cancelada na mesma
     * transação. Chamado pelo encerramento (6.2) e por declarar irreversível (6.4).
     */
    @Transactional
    public void cancelarInadmissiveis(Anomalia anomalia, String motivo) {
        for (Missao pendente : missaoRepository.findByAnomaliaIdAndSituacaoIn(anomalia.getId(), PENDENTES)) {
            if (!anomalia.podeReceberMissao(pendente.getProposito())) {
                SituacaoMissao antes = pendente.getSituacao();
                pendente.cancelar(agora());
                historico.registrar(anomalia, pendente, TipoRegistro.MISSAO_CANCELADA,
                        "Missão " + pendente.getCodigo() + " cancelada automaticamente (M8): " + motivo,
                        antes, SituacaoMissao.CANCELADA, null);
            }
        }
    }

    /** Operação 6.5, passo 2: um agente inativado sai das missões pendentes (M3), porque não volta. */
    @Transactional
    public void removerDesignacoesPendentes(Agente agente, UsuarioAutenticado usuario) {
        for (Missao missao : missaoRepository.findComAgenteNasSituacoes(agente.getId(), PENDENTES)) {
            missao.removerDesignacao(agente);
            historico.registrar(missao.getAnomalia(), missao, TipoRegistro.AGENTE_REMOVIDO,
                    "Agente " + agente.getCodigo() + " removido da missão " + missao.getCodigo() + " porque foi inativado",
                    agente.getCodigo(), null, usuario);
        }
    }

    Missao buscarEntidade(Long id) {
        return missaoRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Missão", id));
    }

    private Agente buscarAgente(Long id) {
        return agenteRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Agente", id));
    }

    private void registrarMudancaDaAnomalia(Anomalia anomalia, Missao missao, SituacaoAnomalia antes,
                                            String causa, UsuarioAutenticado usuario) {
        if (anomalia.getSituacao() != antes) {
            historico.registrar(anomalia, missao, TipoRegistro.MUDANCA_SITUACAO,
                    "Situação da anomalia alterada pelo " + causa, antes, anomalia.getSituacao(), usuario);
        }
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(clock);
    }
}
