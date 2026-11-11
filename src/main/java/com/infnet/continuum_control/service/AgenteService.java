package com.infnet.continuum_control.service;

import com.infnet.continuum_control.dto.agente.AgenteRequest;
import com.infnet.continuum_control.dto.agente.AgenteResponse;
import com.infnet.continuum_control.dto.agente.AtualizarAgenteRequest;
import com.infnet.continuum_control.dto.agente.RegistroAgenteResponse;
import com.infnet.continuum_control.exception.RecursoNaoEncontradoException;
import com.infnet.continuum_control.exception.RegraDeNegocioException;
import com.infnet.continuum_control.model.entities.Agente;
import com.infnet.continuum_control.model.enums.SituacaoAgente;
import com.infnet.continuum_control.repository.AgenteRepository;
import com.infnet.continuum_control.repository.Filtros;
import com.infnet.continuum_control.repository.RegistroAgenteRepository;
import com.infnet.continuum_control.repository.UsuarioRepository;
import com.infnet.continuum_control.security.UsuarioAutenticado;
import com.infnet.continuum_control.service.indicadores.IndicadoresService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso do agente. Toda mudança de situação grava um RegistroAgente na mesma transação
 * (G1 a G7, D31); as automáticas (G2 a G4) são gravadas pelo MissaoService.
 */
@Service
@RequiredArgsConstructor
public class AgenteService {

    private final AgenteRepository agenteRepository;
    private final RegistroAgenteRepository registroAgenteRepository;
    private final UsuarioRepository usuarioRepository;
    private final MissaoService missaoService;
    private final HistoricoService historico;
    private final IndicadoresService indicadores;

    /** G1: nasce DISPONIVEL, com o primeiro registro (situação anterior nula). */
    @Transactional
    public AgenteResponse cadastrar(AgenteRequest request, UsuarioAutenticado usuario) {
        if (agenteRepository.existsByCodigo(request.codigo())) {
            throw new RegraDeNegocioException("G1", "Já existe um agente com o código " + request.codigo());
        }
        Agente agente = agenteRepository.save(new Agente(request.codigo(), request.nome(), request.especialidade()));
        historico.registrarAgente(agente, null, "Cadastro do agente", usuario);
        indicadores.invalidar();
        return AgenteResponse.de(agente);
    }

    @Transactional(readOnly = true)
    public AgenteResponse buscarPorId(Long id) {
        return AgenteResponse.de(buscarEntidade(id));
    }

    /** §16: todos, por situação (DISPONIVEL atende "agentes disponíveis") e por especialidade. */
    @Transactional(readOnly = true)
    public Page<AgenteResponse> listar(SituacaoAgente situacao, String especialidade, Pageable pageable) {
        var filtros = Filtros.de(Agente.class)
                .igual("situacao", situacao)
                .igual("especialidade", especialidade)
                .montar();
        return agenteRepository.findAll(filtros, pageable).map(AgenteResponse::de);
    }

    @Transactional
    public AgenteResponse atualizar(Long id, AtualizarAgenteRequest request) {
        Agente agente = buscarEntidade(id);
        agente.atualizar(request.nome(), request.especialidade());
        return AgenteResponse.de(agente);
    }

    /** G5. */
    @Transactional
    public AgenteResponse suspender(Long id, String motivo, UsuarioAutenticado usuario) {
        Agente agente = buscarEntidade(id);
        SituacaoAgente anterior = agente.getSituacao();
        agente.suspender(motivo);
        historico.registrarAgente(agente, anterior, motivo, usuario);
        indicadores.invalidar();
        return AgenteResponse.de(agente);
    }

    /** G6. */
    @Transactional
    public AgenteResponse reativar(Long id, String motivo, UsuarioAutenticado usuario) {
        Agente agente = buscarEntidade(id);
        SituacaoAgente anterior = agente.getSituacao();
        agente.reativar(motivo);
        historico.registrarAgente(agente, anterior, motivo, usuario);
        indicadores.invalidar();
        return AgenteResponse.de(agente);
    }

    /**
     * Operação 6.5 (G7): o agente fica INATIVO, sai das missões pendentes e o usuário ligado a ele
     * é desativado (S6). Ele não é removido: continua nos registros (§3, §12).
     */
    @Transactional
    public AgenteResponse inativar(Long id, String motivo, UsuarioAutenticado usuario) {
        Agente agente = buscarEntidade(id);
        SituacaoAgente anterior = agente.getSituacao();
        agente.inativar(motivo);
        historico.registrarAgente(agente, anterior, motivo, usuario);
        missaoService.removerDesignacoesPendentes(agente, usuario);
        usuarioRepository.findByAgente_Id(agente.getId())
                .filter(u -> u.isAtivo())
                .ifPresent(u -> u.desativar());
        indicadores.invalidar();
        return AgenteResponse.de(agente);
    }

    @Transactional(readOnly = true)
    public Page<RegistroAgenteResponse> historico(Long id, Pageable pageable) {
        buscarEntidade(id);
        return registroAgenteRepository.findByAgenteId(id, pageable).map(RegistroAgenteResponse::de);
    }

    private Agente buscarEntidade(Long id) {
        return agenteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agente", id));
    }
}
