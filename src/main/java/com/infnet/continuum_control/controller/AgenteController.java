package com.infnet.continuum_control.controller;

import com.infnet.continuum_control.dto.agente.*;
import com.infnet.continuum_control.model.enums.SituacaoAgente;
import com.infnet.continuum_control.security.Acesso;
import com.infnet.continuum_control.security.UsuarioAutenticado;
import com.infnet.continuum_control.service.AgenteService;
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

import java.net.URI;

/** Agentes (doc 05, seção 5). Não há DELETE: um agente desligado é inativado (D46). */
@RestController
@RequestMapping("/agentes")
@RequiredArgsConstructor
public class AgenteController {

    private final AgenteService agenteService;

    @PostMapping
    @PreAuthorize(Acesso.ADMINISTRACAO)
    public ResponseEntity<AgenteResponse> cadastrar(@RequestBody @Valid AgenteRequest request,
                                                    @AuthenticationPrincipal UsuarioAutenticado usuario,
                                                    UriComponentsBuilder uriBuilder) {
        AgenteResponse agente = agenteService.cadastrar(request, usuario);
        URI uri = uriBuilder.path("/agentes/{id}").buildAndExpand(agente.id()).toUri();
        return ResponseEntity.created(uri).body(agente);
    }

    @GetMapping
    public Page<AgenteResponse> listar(@RequestParam(required = false) SituacaoAgente situacao,
                                       @RequestParam(required = false) String especialidade,
                                       @PageableDefault(size = 20, sort = "codigo") Pageable pageable) {
        return agenteService.listar(situacao, especialidade, pageable);
    }

    @GetMapping("/{id}")
    public AgenteResponse buscarPorId(@PathVariable Long id) {
        return agenteService.buscarPorId(id);
    }

    @PatchMapping("/{id}")
    @PreAuthorize(Acesso.ADMINISTRACAO)
    public AgenteResponse atualizar(@PathVariable Long id, @RequestBody @Valid AtualizarAgenteRequest request) {
        return agenteService.atualizar(id, request);
    }

    @PostMapping("/{id}/suspender")
    @PreAuthorize(Acesso.ADMINISTRACAO)
    public AgenteResponse suspender(@PathVariable Long id, @RequestBody @Valid RegistroAgenteResponse.MotivoRequest request,
                                    @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return agenteService.suspender(id, request.motivo(), usuario);
    }

    @PostMapping("/{id}/reativar")
    @PreAuthorize(Acesso.ADMINISTRACAO)
    public AgenteResponse reativar(@PathVariable Long id, @RequestBody @Valid RegistroAgenteResponse.MotivoRequest request,
                                   @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return agenteService.reativar(id, request.motivo(), usuario);
    }

    @PostMapping("/{id}/inativar")
    @PreAuthorize(Acesso.ADMINISTRACAO)
    public AgenteResponse inativar(@PathVariable Long id, @RequestBody @Valid RegistroAgenteResponse.MotivoRequest request,
                                   @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return agenteService.inativar(id, request.motivo(), usuario);
    }

    @GetMapping("/{id}/historico")
    @PreAuthorize(Acesso.SUPERVISAO)
    public Page<RegistroAgenteResponse> historico(@PathVariable Long id,
                                                  @PageableDefault(size = 20, sort = {"dataHora", "id"}, direction = Sort.Direction.ASC) Pageable pageable) {
        return agenteService.historico(id, pageable);
    }
}
