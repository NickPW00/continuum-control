package com.infnet.continuum_control.service;

import com.infnet.continuum_control.dto.evento_historico.CriarEventoRequest;
import com.infnet.continuum_control.dto.evento_historico.EventoResponse;
import com.infnet.continuum_control.model.entities.EventoHistorico;
import com.infnet.continuum_control.repository.EventoHistoricoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventoHistoricoService {

    private final EventoHistoricoRepository eventoHistoricoRepository;

    private EventoResponse criar(CriarEventoRequest request){
        EventoHistorico eventoHistorico = new EventoHistorico(request.acontecimento(),
                request.descricao(),
                request.data(),
                request.localizacao(),
                request.importancia()
        );
        return EventoResponse.de(eventoHistoricoRepository.save(eventoHistorico));
    }
    private EventoResponse buscarPorId(){}
    private EventoResponse listar(){}
    private EventoResponse atualizar(){}
    private EventoResponse remvoer(){}
}
