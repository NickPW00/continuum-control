package com.infnet.continuum_control.service;

import com.infnet.continuum_control.dto.intervencao.CriarIntervencaoRequest;
import com.infnet.continuum_control.dto.intervencao.IntervencaoResponse;
import com.infnet.continuum_control.exception.RecursoNaoEncontradoException;
import com.infnet.continuum_control.model.entities.Agente;
import com.infnet.continuum_control.model.entities.Intervencao;
import com.infnet.continuum_control.model.entities.Missao;
import com.infnet.continuum_control.model.enums.TipoRegistro;
import com.infnet.continuum_control.repository.AgenteRepository;
import com.infnet.continuum_control.repository.IntervencaoRepository;
import com.infnet.continuum_control.repository.MissaoRepository;
import com.infnet.continuum_control.security.PosseDeMissao;
import com.infnet.continuum_control.security.UsuarioAutenticado;
import com.infnet.continuum_control.service.indicadores.IndicadoresService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/** Intervenções (§10): só do agente do usuário (S4), só na missão em que ele atua (S3). */
@Service
@RequiredArgsConstructor
public class IntervencaoService {

    private final MissaoRepository missaoRepository;
    private final AgenteRepository agenteRepository;
    private final IntervencaoRepository intervencaoRepository;
    private final PosseDeMissao posse;
    private final HistoricoService historico;
    private final IndicadoresService indicadores;
    private final Clock clock;

    /** M9. A posse (403) é conferida antes das guardas do domínio (409), como pede a D56. */
    @Transactional
    public IntervencaoResponse registrar(Long missaoId, CriarIntervencaoRequest req, UsuarioAutenticado usuario) {
        Missao missao = buscarMissao(missaoId);
        posse.exigir(missaoId, usuario);
        Agente agente = agenteRepository.getReferenceById(usuario.agenteId());
        Intervencao intervencao = missao.registrarIntervencao(agente, req.acao(), req.justificativa(),
                req.impactoEstimado(), req.resultadoObservado(), LocalDateTime.now(clock));
        missaoRepository.flush();
        historico.registrar(missao.getAnomalia(), missao, TipoRegistro.INTERVENCAO,
                "Intervenção de " + agente.getCodigo() + ": " + req.acao(), null, req.impactoEstimado(), usuario);
        if (intervencao.isSevera()) {
            indicadores.invalidar();
        }
        return IntervencaoResponse.de(intervencao);
    }

    @Transactional(readOnly = true)
    public Page<IntervencaoResponse> listar(Long missaoId, Pageable pageable) {
        buscarMissao(missaoId);
        return intervencaoRepository.findByMissaoId(missaoId, pageable).map(IntervencaoResponse::de);
    }

    /** O resultado pode ser conhecido só depois; só o autor o informa, uma vez (doc 05, seção 9). */
    @Transactional
    public IntervencaoResponse registrarResultado(Long missaoId, Long intervencaoId, String resultado,
                                                  UsuarioAutenticado usuario) {
        buscarMissao(missaoId);
        posse.exigir(missaoId, usuario);
        Intervencao intervencao = intervencaoRepository.findByIdAndMissaoId(intervencaoId, missaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Intervenção", intervencaoId));
        if (!intervencao.getAgente().getId().equals(usuario.agenteId())) {
            throw new AccessDeniedException("Só o agente que registrou a intervenção pode informar o resultado");
        }
        intervencao.registrarResultado(resultado);
        return IntervencaoResponse.de(intervencao);
    }

    private Missao buscarMissao(Long id) {
        return missaoRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Missão", id));
    }
}
