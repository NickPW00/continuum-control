package com.infnet.continuum_control.service;

import com.infnet.continuum_control.dto.usuario.LoginResponse;
import com.infnet.continuum_control.dto.usuario.UsuarioResponse;
import com.infnet.continuum_control.exception.RecursoNaoEncontradoException;
import com.infnet.continuum_control.exception.RegraDeNegocioException;
import com.infnet.continuum_control.model.entities.Usuario;
import com.infnet.continuum_control.repository.UsuarioRepository;
import com.infnet.continuum_control.security.EmissorDeToken;
import com.infnet.continuum_control.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** Login e senha do próprio usuário (doc 06, seção 2). */
@Service
@RequiredArgsConstructor
public class AutenticacaoService {

    /** S2: a mesma mensagem para login inexistente, senha errada e usuário desativado. */
    static final String CREDENCIAIS_INVALIDAS = "Login ou senha inválidos";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmissorDeToken emissor;

    @Transactional(readOnly = true)
    public LoginResponse login(String login, String senha) {
        Usuario usuario = usuarioRepository.findByLogin(login)
                .filter(u -> u.isAtivo() && passwordEncoder.matches(senha, u.getSenhaHash()))
                .orElseThrow(() -> new BadCredentialsException(CREDENCIAIS_INVALIDAS));
        EmissorDeToken.TokenEmitido token = emissor.emitir(usuario);
        return new LoginResponse(token.token(), "Bearer",
                LocalDateTime.ofInstant(token.expiraEm(), ZoneOffset.UTC), UsuarioResponse.de(usuario));
    }

    @Transactional(readOnly = true)
    public UsuarioResponse eu(UsuarioAutenticado usuario) {
        return UsuarioResponse.de(buscar(usuario.id()));
    }

    @Transactional
    public void alterarSenha(UsuarioAutenticado usuario, String senhaAtual, String novaSenha) {
        Usuario entidade = buscar(usuario.id());
        if (!passwordEncoder.matches(senhaAtual, entidade.getSenhaHash())) {
            throw new RegraDeNegocioException(null, "A senha atual não confere");
        }
        entidade.alterarSenha(passwordEncoder.encode(novaSenha));
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", id));
    }
}
