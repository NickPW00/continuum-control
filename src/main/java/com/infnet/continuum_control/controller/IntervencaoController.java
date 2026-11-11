package com.infnet.continuum_control.controller;

import com.infnet.continuum_control.dto.intervencao.CriarIntervencaoRequest;
import com.infnet.continuum_control.dto.intervencao.IntervencaoResponse;
import com.infnet.continuum_control.dto.intervencao.ResultadoIntervencaoRequest;
import com.infnet.continuum_control.security.Acesso;
import com.infnet.continuum_control.security.UsuarioAutenticado;
import com.infnet.continuum_control.service.IntervencaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

/** Intervenções (doc 05, seção 9). */
@RestController
@RequestMapping("/missoes/{missaoId}/intervencoes")
@RequiredArgsConstructor
public class IntervencaoController {

    private final IntervencaoService intervencaoService;

    @PostMapping
    @PreAuthorize(Acesso.AGENTE)
    public ResponseEntity<IntervencaoResponse> registrar(@PathVariable Long missaoId,
                                                         @RequestBody @Valid CriarIntervencaoRequest request,
                                                         @AuthenticationPrincipal UsuarioAutenticado usuario,
                                                         UriComponentsBuilder uriBuilder) {
        IntervencaoResponse intervencao = intervencaoService.registrar(missaoId, request, usuario);
        return ResponseEntity.created(uriBuilder.path("/missoes/{missaoId}/intervencoes/{id}")
                .buildAndExpand(missaoId, intervencao.id()).toUri()).body(intervencao);
    }

    @GetMapping
    public Page<IntervencaoResponse> listar(@PathVariable Long missaoId,
                                            @PageableDefault(size = 20, sort = "dataHora", direction = Sort.Direction.ASC) Pageable pageable) {
        return intervencaoService.listar(missaoId, pageable);
    }

    @PatchMapping("/{intervencaoId}")
    @PreAuthorize(Acesso.AGENTE)
    public IntervencaoResponse registrarResultado(@PathVariable Long missaoId, @PathVariable Long intervencaoId,
                                                  @RequestBody @Valid ResultadoIntervencaoRequest request,
                                                  @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return intervencaoService.registrarResultado(missaoId, intervencaoId, request.resultadoObservado(), usuario);
    }
}
