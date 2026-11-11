package com.infnet.continuum_control.security;

import com.infnet.continuum_control.model.entities.Usuario;
import com.infnet.continuum_control.model.enums.Perfil;

/**
 * Quem está fazendo a requisição, montado com os dados do banco a cada requisição (doc 06, seção 3).
 * É o principal da autenticação e chega aos controllers por {@code @AuthenticationPrincipal}.
 */
public record UsuarioAutenticado(Long id, String login, Perfil perfil, Long agenteId) {

    public static UsuarioAutenticado de(Usuario usuario) {
        return new UsuarioAutenticado(usuario.getId(), usuario.getLogin(), usuario.getPerfil(), usuario.getAgenteId());
    }
}
