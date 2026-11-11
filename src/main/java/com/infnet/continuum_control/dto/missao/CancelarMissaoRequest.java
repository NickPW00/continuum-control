package com.infnet.continuum_control.dto.missao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelarMissaoRequest(
        @NotBlank(message = "O motivo é obrigatório")
        @Size(max = 4000, message = "O motivo deve ter no máximo 4000 caracteres")
        String motivo
) {
}
