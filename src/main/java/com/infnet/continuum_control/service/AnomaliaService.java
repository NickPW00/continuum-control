package com.infnet.continuum_control.service;

import com.infnet.continuum_control.dto.anomalia.*;
import com.infnet.continuum_control.exception.RecursoNaoEncontradoException;
import com.infnet.continuum_control.exception.RegraDeNegocioException;
import com.infnet.continuum_control.model.entities.Anomalia;
import com.infnet.continuum_control.model.entities.EventoHistorico;
import com.infnet.continuum_control.model.entities.RegistroHistorico;
import com.infnet.continuum_control.model.enums.*;
import com.infnet.continuum_control.repository.*;
import com.infnet.continuum_control.security.UsuarioAutenticado;
import com.infnet.continuum_control.service.indicadores.IndicadoresService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AnomaliaService {

    private final AnomaliaRepository anomaliaRepository;
    private final EventoHistoricoRepository eventoRepository;
    private final MissaoRepository missaoRepository;
    private final RegistroHistoricoRepository registroHistoricoRepository;
    private final MissaoService missaoService;
    private final HistoricoService historico;
    private final IndicadoresService indicadores;
    private final Clock clock;

    /** N1: nasce DETECTADA; a detecção pode anteceder o registro, mas não estar no futuro. */
    @Transactional
    public AnomaliaResponse criar(CriarAnomaliaRequest req) {
        EventoHistorico evento = eventoRepository.findById(req.eventoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento histórico", req.eventoId()));
        Anomalia anomalia = anomaliaRepository.save(new Anomalia(evento, req.divergenciaObservada(), req.risco(),
                req.origem(), req.observacoes(), req.dataHoraDeteccao(), LocalDateTime.now(clock)));
        indicadores.invalidar();
        return AnomaliaResponse.de(anomalia);
    }

    /** Consultas do §16; os filtros se combinam (E). */
    @Transactional(readOnly = true)
    public Page<AnomaliaResponse> listar(SituacaoAnomalia situacao, NivelRisco risco, Boolean ativas,
                                         Boolean criticas, Long eventoId, Pageable pageable) {
        Specification<Anomalia> ativa = (root, q, cb) -> root.get("situacao").in(Anomalia.SITUACOES_ATIVAS);
        Specification<Anomalia> critica = ativa.and((root, q, cb) -> cb.equal(root.get("risco"), NivelRisco.CRITICO));
        Specification<Anomalia> filtros = Filtros.de(Anomalia.class)
                .igual("situacao", situacao)
                .igual("risco", risco)
                .igualNoCaminho("evento", "id", eventoId)
                .condicao(ativa, Boolean.TRUE.equals(ativas))
                .condicao(Specification.not(ativa), Boolean.FALSE.equals(ativas))
                .condicao(critica, Boolean.TRUE.equals(criticas))
                .condicao(Specification.not(critica), Boolean.FALSE.equals(criticas))
                .montar();
        return anomaliaRepository.findAll(filtros, pageable).map(AnomaliaResponse::de);
    }

    @Transactional(readOnly = true)
    public AnomaliaResponse buscarPorId(Long id) {
        return AnomaliaResponse.de(buscarEntidade(id));
    }

    @Transactional
    public AnomaliaResponse atualizar(Long id, AtualizarAnomaliaRequest req) {
        Anomalia anomalia = buscarEntidade(id);
        anomalia.atualizarDados(req.divergenciaObservada(), req.origem(), req.observacoes());
        return AnomaliaResponse.de(anomalia);
    }

    /** N7: reclassificar o risco de uma anomalia ativa, com registro MUDANCA_RISCO. */
    @Transactional
    public AnomaliaResponse alterarRisco(Long id, AlterarRiscoRequest req, UsuarioAutenticado usuario) {
        Anomalia anomalia = buscarEntidade(id);
        NivelRisco anterior = anomalia.getRisco();
        anomalia.alterarRisco(req.risco());
        historico.registrar(anomalia, null, TipoRegistro.MUDANCA_RISCO, req.justificativa(),
                anterior, anomalia.getRisco(), usuario);
        indicadores.invalidar();
        return AnomaliaResponse.de(anomalia);
    }

    /**
     * Operação 6.4 (N6): sem missão em execução, a anomalia passa a IRREVERSIVEL, as correções
     * pendentes são canceladas (M8) e a justificativa vai para o histórico (D33).
     */
    @Transactional
    public AnomaliaResponse declararIrreversivel(Long id, String justificativa, UsuarioAutenticado usuario) {
        Anomalia anomalia = buscarEntidade(id);
        if (missaoRepository.existsByAnomaliaIdAndSituacao(id, SituacaoMissao.EM_EXECUCAO)) {
            throw new RegraDeNegocioException("N6", "Não é possível declarar irreversível uma anomalia com missão em execução");
        }
        SituacaoAnomalia anterior = anomalia.getSituacao();
        anomalia.declararIrreversivel(justificativa);
        historico.registrar(anomalia, null, TipoRegistro.MUDANCA_SITUACAO,
                "Anomalia declarada irreversível: " + justificativa, anterior, anomalia.getSituacao(), usuario);
        missaoService.cancelarInadmissiveis(anomalia, "a anomalia foi declarada IRREVERSIVEL");
        indicadores.invalidar();
        return AnomaliaResponse.de(anomalia);
    }

    /** §12: a sequência de acontecimentos da anomalia, filtrável por missão (D37) e por tipo. */
    @Transactional(readOnly = true)
    public Page<RegistroHistoricoResponse> historico(Long id, Long missaoId, TipoRegistro tipo, Pageable pageable) {
        buscarEntidade(id);
        Specification<RegistroHistorico> filtros = Filtros.de(RegistroHistorico.class)
                .igualNoCaminho("anomalia", "id", id)
                .igualNoCaminho("missao", "id", missaoId)
                .igual("tipo", tipo)
                .montar();
        return registroHistoricoRepository.findAll(filtros, pageable).map(RegistroHistoricoResponse::de);
    }

    private Anomalia buscarEntidade(Long id) {
        return anomaliaRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Anomalia", id));
    }
}
