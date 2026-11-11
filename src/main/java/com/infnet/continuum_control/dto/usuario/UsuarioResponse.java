package com.infnet.continuum_control.dto.usuario;

import com.infnet.continuum_control.model.entities.Usuario;
import com.infnet.continuum_control.model.enums.Perfil;

/** A senha e o hash nunca aparecem (doc 05, 4.2). */
public record UsuarioResponse(Long id, String login, Perfil perfil, Long agenteId, boolean ativo) {

    public static UsuarioResponse de(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getLogin(), u.getPerfil(), u.getAgenteId(), u.isAtivo());
    }
}
