package com.infnet.continuum_control.dto.usuario;

import com.infnet.continuum_control.model.enums.Perfil;

public record AtualizarUsuarioRequest(Perfil perfil, Long agenteId) {
}
