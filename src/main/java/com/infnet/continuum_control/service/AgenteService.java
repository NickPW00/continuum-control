package com.infnet.continuum_control.service;

import com.infnet.continuum_control.dto.agente.AgenteRequest;
import com.infnet.continuum_control.dto.agente.AgenteResponse;
import com.infnet.continuum_control.exception.RecursoNaoEncontradoException;
import com.infnet.continuum_control.exception.RegraDeNegocioException;
import com.infnet.continuum_control.model.entities.Agente;
import com.infnet.continuum_control.model.enums.SituacaoAgente;
import com.infnet.continuum_control.repository.AgenteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AgenteService {

    private final AgenteRepository agenteRepository;

    @Transactional
    public AgenteResponse cadastrar(AgenteRequest request) {
        if (agenteRepository.existsByCodigo(request.codigo())) {
            throw new RegraDeNegocioException("Já existe um agente com o código " + request.codigo());
        }
        Agente agente = new Agente(request.codigo(), request.nome(), request.especialidade());
        return AgenteResponse.de(agenteRepository.save(agente));
    }

    @Transactional(readOnly = true)
    public AgenteResponse buscarPorId(Long id) {
        return AgenteResponse.de(buscarEntidade(id));
    }

    @Transactional(readOnly = true)
    public List<AgenteResponse> listar(SituacaoAgente situacao, String especialidade) {
        List<Agente> agentes;
        if (situacao != null && especialidade != null) {
            agentes = agenteRepository.findBySituacaoAndEspecialidade(situacao, especialidade);
        } else if (situacao != null) {
            agentes = agenteRepository.findBySituacao(situacao);
        } else if (especialidade != null) {
            agentes = agenteRepository.findByEspecialidade(especialidade);
        } else {
            agentes = agenteRepository.findAll();
        }
        return agentes.stream().map(AgenteResponse::de).toList();
    }

    @Transactional
    public AgenteResponse suspender(Long id) {
        Agente agente = buscarEntidade(id);
        agente.suspender();
        return AgenteResponse.de(agente);
    }

    @Transactional
    public AgenteResponse reativar(Long id) {
        Agente agente = buscarEntidade(id);
        agente.reativar();
        return AgenteResponse.de(agente);
    }

    @Transactional
    public AgenteResponse inativar(Long id) {
        Agente agente = buscarEntidade(id);
        agente.inativar();
        return AgenteResponse.de(agente);
    }

    private Agente buscarEntidade(Long id) {
        return agenteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agente", id));
    }
}
