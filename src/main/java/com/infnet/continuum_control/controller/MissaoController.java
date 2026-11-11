package com.infnet.continuum_control.controller;

import com.infnet.continuum_control.dto.missao.*;
import com.infnet.continuum_control.model.enums.PropositoMissao;
import com.infnet.continuum_control.model.enums.SituacaoMissao;
import com.infnet.continuum_control.security.Acesso;
import com.infnet.continuum_control.security.UsuarioAutenticado;
import com.infnet.continuum_control.service.MissaoService;
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

/**
 * Missões (doc 05, seção 8). Cada transição do doc 03 é uma rota própria, {@code POST /{id}/{acao}},
 * que responde com a missão já no novo estado (D45).
 */
@RestController
@RequestMapping("/missoes")
@RequiredArgsConstructor
public class MissaoController {

    private final MissaoService missaoService;

    @PostMapping
    @PreAuthorize(Acesso.OPERACAO)
    public ResponseEntity<MissaoResponse> criar(@RequestBody @Valid CriarMissaoRequest request,
                                                @AuthenticationPrincipal UsuarioAutenticado usuario,
                                                UriComponentsBuilder uriBuilder) {
        MissaoResponse missao = missaoService.criar(request, usuario);
        return ResponseEntity.created(uriBuilder.path("/missoes/{id}").buildAndExpand(missao.id()).toUri()).body(missao);
    }

    @GetMapping
    public Page<MissaoResponse> listar(@RequestParam(required = false) SituacaoMissao situacao,
                                       @RequestParam(required = false) Long anomaliaId,
                                       @RequestParam(required = false) Long agenteId,
                                       @RequestParam(required = false) PropositoMissao proposito,
                                       @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return missaoService.listar(situacao, anomaliaId, agenteId, proposito, pageable);
    }

    @GetMapping("/{id}")
    public MissaoResponse buscarPorId(@PathVariable Long id) {
        return missaoService.buscarPorId(id);
    }

    @PatchMapping("/{id}")
    @PreAuthorize(Acesso.OPERACAO)
    public MissaoResponse atualizar(@PathVariable Long id, @RequestBody @Valid AtualizarMissaoRequest request) {
        return missaoService.atualizar(id, request);
    }

    @PostMapping("/{id}/designacoes")
    @PreAuthorize(Acesso.OPERACAO)
    public ResponseEntity<MissaoResponse> designar(@PathVariable Long id, @RequestBody @Valid DesignacaoRequest request,
                                                   @AuthenticationPrincipal UsuarioAutenticado usuario,
                                                   UriComponentsBuilder uriBuilder) {
        MissaoResponse missao = missaoService.designar(id, request.agenteId(), usuario);
        return ResponseEntity.created(uriBuilder.path("/missoes/{id}/designacoes/{agenteId}")
                .buildAndExpand(id, request.agenteId()).toUri()).body(missao);
    }

    @DeleteMapping("/{id}/designacoes/{agenteId}")
    @PreAuthorize(Acesso.OPERACAO)
    public ResponseEntity<Void> removerDesignacao(@PathVariable Long id, @PathVariable Long agenteId,
                                                  @AuthenticationPrincipal UsuarioAutenticado usuario) {
        missaoService.removerDesignacao(id, agenteId, usuario);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/autorizar")
    @PreAuthorize(Acesso.SUPERVISAO)
    public MissaoResponse autorizar(@PathVariable Long id, @RequestBody @Valid AutorizarRequest request,
                                    @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return missaoService.autorizar(id, request.observacao(), usuario);
    }

    @PostMapping("/{id}/iniciar")
    @PreAuthorize(Acesso.OPERACAO)
    public MissaoResponse iniciar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return missaoService.iniciar(id, usuario);
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize(Acesso.OPERACAO)
    public MissaoResponse cancelar(@PathVariable Long id, @RequestBody @Valid CancelarMissaoRequest request,
                                   @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return missaoService.cancelar(id, request.motivo(), usuario);
    }

    @PostMapping("/{id}/concluir")
    @PreAuthorize(Acesso.OPERACAO)
    public MissaoResponse concluir(@PathVariable Long id, @RequestBody @Valid EncerrarMissaoRequest request,
                                   @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return missaoService.concluir(id, request, usuario);
    }

    @PostMapping("/{id}/registrar-falha")
    @PreAuthorize(Acesso.OPERACAO)
    public MissaoResponse registrarFalha(@PathVariable Long id, @RequestBody @Valid EncerrarMissaoRequest request,
                                         @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return missaoService.registrarFalha(id, request, usuario);
    }
}
