package com.infnet.continuum_control.dto.usuario;

import java.time.LocalDateTime;

public record LoginResponse(String token, String tipo, LocalDateTime expiraEm, UsuarioResponse usuario) {
}
