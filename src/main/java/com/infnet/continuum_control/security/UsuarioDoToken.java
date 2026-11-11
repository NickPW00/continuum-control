package com.infnet.continuum_control.security;

import com.infnet.continuum_control.model.entities.Usuario;
import com.infnet.continuum_control.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Transforma o token já validado em um usuário autenticado, lendo o usuário do banco pelo
 * {@code sub} (doc 06, seção 3). Assim, desativar um usuário ou mudar o seu perfil vale na
 * requisição seguinte (S7, D54), e a autorização usa o perfil do banco, não o do token (D55).
 */
@Component
@RequiredArgsConstructor
public class UsuarioDoToken implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Long id;
        try {
            id = Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException e) {
            throw new DisabledException("Token sem usuário válido");
        }
        Usuario usuario = usuarioRepository.findById(id)
                .filter(Usuario::isAtivo)
                .orElseThrow(() -> new DisabledException("Usuário inexistente ou desativado"));
        UsuarioAutenticado principal = UsuarioAutenticado.de(usuario);
        var autoridades = List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getPerfil().name()));
        return UsernamePasswordAuthenticationToken.authenticated(principal, jwt, autoridades);
    }
}
