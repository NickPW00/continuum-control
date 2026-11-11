package com.infnet.continuum_control.controller;

import com.infnet.continuum_control.dto.agente.AgenteRequest;
import com.infnet.continuum_control.dto.agente.AgenteResponse;
import com.infnet.continuum_control.dto.agente.MotivoRequest;
import com.infnet.continuum_control.model.enums.SituacaoAgente;
import com.infnet.continuum_control.service.AgenteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/agentes")
@RequiredArgsConstructor
public class AgenteController {

    private final AgenteService agenteService;

    @PostMapping
    public ResponseEntity<AgenteResponse> cadastrar(@RequestBody @Valid AgenteRequest request,
                                                    UriComponentsBuilder uriBuilder) {
        AgenteResponse agente = agenteService.cadastrar(request);
        URI uri = uriBuilder.path("/agentes/{id}").buildAndExpand(agente.id()).toUri();
        return ResponseEntity.created(uri).body(agente);
    }

    @GetMapping("/{id}")
    public AgenteResponse buscarPorId(@PathVariable Long id) {
        return agenteService.buscarPorId(id);
    }

    @GetMapping
    public List<AgenteResponse> listar(@RequestParam(required = false) SituacaoAgente situacao,
                                       @RequestParam(required = false) String especialidade) {
        return agenteService.listar(situacao, especialidade);
    }

    @PostMapping("/{id}/suspender")
    public AgenteResponse suspender(@PathVariable Long id, @RequestBody @Valid MotivoRequest request) {
        return agenteService.suspender(id, request.motivo());
    }

    @PostMapping("/{id}/reativar")
    public AgenteResponse reativar(@PathVariable Long id, @RequestBody @Valid MotivoRequest request) {
        return agenteService.reativar(id, request.motivo());
    }

    @PostMapping("/{id}/inativar")
    public AgenteResponse inativar(@PathVariable Long id, @RequestBody @Valid MotivoRequest request) {
        return agenteService.inativar(id, request.motivo());
    }
}
