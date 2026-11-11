package com.infnet.continuum_control.controller;

import com.infnet.continuum_control.model.enums.TipoOcorrencia;
import com.infnet.continuum_control.model.ocorrencias.Ocorrencia;
import com.infnet.continuum_control.security.Acesso;
import com.infnet.continuum_control.security.UsuarioAutenticado;
import com.infnet.continuum_control.service.ocorrencias.OcorrenciaService;
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

/** Ocorrências (doc 05, seção 9). O corpo muda com o {@code tipo} (D51). */
@RestController
@RequestMapping("/missoes/{missaoId}/ocorrencias")
@RequiredArgsConstructor
public class OcorrenciaController {

    private final OcorrenciaService ocorrenciaService;

    @PostMapping
    @PreAuthorize(Acesso.AGENTE)
    public ResponseEntity<Ocorrencia> registrar(@PathVariable Long missaoId, @RequestBody @Valid Ocorrencia ocorrencia,
                                                @AuthenticationPrincipal UsuarioAutenticado usuario,
                                                UriComponentsBuilder uriBuilder) {
        Ocorrencia registrada = ocorrenciaService.registrar(missaoId, ocorrencia, usuario);
        return ResponseEntity.created(uriBuilder.path("/missoes/{missaoId}/ocorrencias/{id}")
                .buildAndExpand(missaoId, registrada.getId()).toUri()).body(registrada);
    }

    @GetMapping
    public Page<Ocorrencia> listar(@PathVariable Long missaoId, @RequestParam(required = false) TipoOcorrencia tipo,
                                   @PageableDefault(size = 20, sort = "dataHora", direction = Sort.Direction.ASC) Pageable pageable) {
        return ocorrenciaService.listar(missaoId, tipo, pageable);
    }

    @GetMapping("/{ocorrenciaId}")
    public Ocorrencia buscar(@PathVariable Long missaoId, @PathVariable String ocorrenciaId) {
        return ocorrenciaService.buscar(missaoId, ocorrenciaId);
    }
}
