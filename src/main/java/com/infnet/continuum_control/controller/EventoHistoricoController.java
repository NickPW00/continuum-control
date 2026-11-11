package com.infnet.continuum_control.controller;


import com.infnet.continuum_control.dto.evento_historico.AtualizarEventoRequest;
import com.infnet.continuum_control.dto.evento_historico.CriarEventoRequest;
import com.infnet.continuum_control.dto.evento_historico.EventoResponse;
import com.infnet.continuum_control.model.enums.Importancia;
import com.infnet.continuum_control.security.Acesso;
import org.springframework.security.access.prepost.PreAuthorize;
import com.infnet.continuum_control.service.EventoHistoricoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.format.annotation.DateTimeFormat;

import java.net.URI;
import java.time.LocalDate;

@RestController
@RequestMapping("/eventos")
@RequiredArgsConstructor
public class EventoHistoricoController {

    private final EventoHistoricoService eventoHistoricoService;

    @PostMapping
    @PreAuthorize(Acesso.OPERACAO)
    public ResponseEntity<EventoResponse> criar(@RequestBody @Valid CriarEventoRequest request, UriComponentsBuilder uriBuilder){
        EventoResponse evento = eventoHistoricoService.criar(request);
        URI uri = uriBuilder.path("/eventos/{id}").buildAndExpand(evento.id()).toUri();
        return ResponseEntity.created(uri).body(evento);
    }

    @GetMapping
    public Page<EventoResponse> listar(
            @RequestParam(required = false) Importancia importancia,
            @RequestParam(required = false) @DateTimeFormat (iso = DateTimeFormat.ISO.DATE)LocalDate dataDe,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)LocalDate dataAte,
            @PageableDefault(size = 20, sort = "data") Pageable pageable
            ){
        return eventoHistoricoService.listar(importancia, dataDe, dataAte, pageable);
    }

    @GetMapping("/{id}")
    public EventoResponse buscarPorId(@PathVariable Long id){
        return eventoHistoricoService.buscarPorId(id);
    }

    @PatchMapping("/{id}")
    @PreAuthorize(Acesso.OPERACAO)
    public EventoResponse atualizar(@PathVariable Long id, @RequestBody @Valid AtualizarEventoRequest request) {
        return eventoHistoricoService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Acesso.SUPERVISAO)
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        eventoHistoricoService.remover(id);
        return ResponseEntity.noContent().build();
    }

}
