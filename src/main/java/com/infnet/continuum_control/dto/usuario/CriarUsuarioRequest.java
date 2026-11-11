package com.infnet.continuum_control.dto.usuario;

import com.infnet.continuum_control.model.enums.Perfil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** O limite de 72 caracteres vem do BCrypt, que ignora o que passa de 72 bytes. */
public record CriarUsuarioRequest(
        @NotBlank(message = "O login é obrigatório")
        @Size(max = 50, message = "O login deve ter no máximo 50 caracteres")
        @Pattern(regexp = "[a-z0-9._-]+", message = "O login aceita só letras minúsculas, números, ponto, sublinhado e hífen")
        String login,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 8, max = 72, message = "A senha deve ter de 8 a 72 caracteres")
        String senha,

        @NotNull(message = "O perfil é obrigatório")
        Perfil perfil,

        Long agenteId
) {
}
