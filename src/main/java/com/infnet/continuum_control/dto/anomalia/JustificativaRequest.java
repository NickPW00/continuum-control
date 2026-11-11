package com.infnet.continuum_control.dto.anomalia;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JustificativaRequest(
        @NotBlank(message = "A justificativa é obrigatória")
        @Size(max = 4000, message = "A justificativa deve ter no máximo 4000 caracteres")
        String justificativa
) {
}
