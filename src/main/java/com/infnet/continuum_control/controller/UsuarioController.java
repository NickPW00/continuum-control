package com.infnet.continuum_control.controller;

import com.infnet.continuum_control.dto.usuario.*;
import com.infnet.continuum_control.model.enums.Perfil;
import com.infnet.continuum_control.security.Acesso;
import com.infnet.continuum_control.security.UsuarioAutenticado;
import com.infnet.continuum_control.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

/** Usuários (doc 05, seção 4): só ADMINISTRADOR. Usuários não são removidos, são desativados. */
@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
@PreAuthorize(Acesso.ADMINISTRACAO)
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping
    public ResponseEntity<UsuarioResponse> criar(@RequestBody @Valid CriarUsuarioRequest request,
                                                 UriComponentsBuilder uriBuilder) {
        UsuarioResponse usuario = usuarioService.criar(request);
        return ResponseEntity.created(uriBuilder.path("/usuarios/{id}").buildAndExpand(usuario.id()).toUri()).body(usuario);
    }

    @GetMapping
    public Page<UsuarioResponse> listar(@RequestParam(required = false) Perfil perfil,
                                        @RequestParam(required = false) Boolean ativo,
                                        @PageableDefault(size = 20, sort = "login") Pageable pageable) {
        return usuarioService.listar(perfil, ativo, pageable);
    }

    @GetMapping("/{id}")
    public UsuarioResponse buscarPorId(@PathVariable Long id) {
        return usuarioService.buscarPorId(id);
    }

    @PatchMapping("/{id}")
    public UsuarioResponse atualizar(@PathVariable Long id, @RequestBody @Valid AtualizarUsuarioRequest request) {
        return usuarioService.atualizar(id, request);
    }

    @PostMapping("/{id}/desativar")
    public UsuarioResponse desativar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return usuarioService.desativar(id, usuario);
    }

    @PostMapping("/{id}/reativar")
    public UsuarioResponse reativar(@PathVariable Long id) {
        return usuarioService.reativar(id);
    }

    @PostMapping("/{id}/redefinir-senha")
    public ResponseEntity<Void> redefinirSenha(@PathVariable Long id, @RequestBody @Valid NovaSenhaRequest request) {
        usuarioService.redefinirSenha(id, request.novaSenha());
        return ResponseEntity.noContent().build();
    }
}
