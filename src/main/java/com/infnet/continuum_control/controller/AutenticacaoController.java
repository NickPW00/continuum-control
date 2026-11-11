package com.infnet.continuum_control.controller;

import com.infnet.continuum_control.dto.usuario.AlterarSenhaRequest;
import com.infnet.continuum_control.dto.usuario.LoginRequest;
import com.infnet.continuum_control.dto.usuario.LoginResponse;
import com.infnet.continuum_control.dto.usuario.UsuarioResponse;
import com.infnet.continuum_control.security.UsuarioAutenticado;
import com.infnet.continuum_control.service.AutenticacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Autenticação (doc 05, seção 4.1). {@code POST /auth/login} é a única rota pública além do health (S1). */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AutenticacaoController {

    private final AutenticacaoService autenticacaoService;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody @Valid LoginRequest request) {
        return autenticacaoService.login(request.login(), request.senha());
    }

    @GetMapping("/eu")
    public UsuarioResponse eu(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return autenticacaoService.eu(usuario);
    }

    @PutMapping("/senha")
    public ResponseEntity<Void> alterarSenha(@RequestBody @Valid AlterarSenhaRequest request,
                                             @AuthenticationPrincipal UsuarioAutenticado usuario) {
        autenticacaoService.alterarSenha(usuario, request.senhaAtual(), request.novaSenha());
        return ResponseEntity.noContent().build();
    }
}
