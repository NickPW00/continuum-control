package com.infnet.continuum_control.service;

import com.infnet.continuum_control.dto.evento_historico.AtualizarEventoRequest;
import com.infnet.continuum_control.dto.evento_historico.CriarEventoRequest;
import com.infnet.continuum_control.dto.evento_historico.EventoResponse;
import com.infnet.continuum_control.exception.RecursoNaoEncontradoException;
import com.infnet.continuum_control.model.entities.Agente;
import com.infnet.continuum_control.model.entities.EventoHistorico;
import com.infnet.continuum_control.model.enums.Importancia;
import com.infnet.continuum_control.repository.EventoHistoricoRepository;

import jdk.jfr.Event;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class EventoHistoricoService {

    private final EventoHistoricoRepository eventoHistoricoRepository;

    @Transactional
    public EventoResponse criar(CriarEventoRequest request){
        EventoHistorico eventoHistorico = new EventoHistorico(request.acontecimento(),
                request.descricao(),
                request.data(),
                request.localizacao(),
                request.importancia()
        );
        return EventoResponse.de(eventoHistoricoRepository.save(eventoHistorico));
    }

    @Transactional(readOnly = true)
    public EventoResponse buscarPorId(Long id){return EventoResponse.de(buscarEntidade(id));}

    @Transactional(readOnly = true)
    public Page<EventoResponse> listar(Importancia importancia, LocalDate dataDe, LocalDate dataAte, Pageable pageable){
        boolean temPeriodo = dataDe != null && dataAte != null;
        Page<EventoHistorico> eventos;
        if (importancia != null && temPeriodo) {
            eventos = eventoHistoricoRepository.findByImportanciaAndDataBetween(importancia, dataDe, dataAte, pageable);
        } else if (importancia != null) {
            eventos = eventoHistoricoRepository.findByImportancia(importancia, pageable);
        } else if (temPeriodo) {
            eventos = eventoHistoricoRepository.findByDataBetween(dataDe, dataAte, pageable);
        } else {
            eventos = eventoHistoricoRepository.findAll(pageable);
        }
        return eventos.map(EventoResponse::de);
    }

    @Transactional
    public EventoResponse atualizar(Long id, AtualizarEventoRequest request){
        EventoHistorico evento = buscarEntidade(id);
        evento.atualizar(request.acontecimento(), request.descricao(), request.data(),
                request.localizacao(), request.importancia());
        return EventoResponse.de(evento);
    }

    @Transactional
    public void remover(Long id){
        EventoHistorico evento = buscarEntidade(id);
        eventoHistoricoRepository.delete(evento);
        eventoHistoricoRepository.flush();
    }

    private EventoHistorico buscarEntidade(Long id) {
        return eventoHistoricoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("EventoHistorico", id));
    }
}
