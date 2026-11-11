package com.infnet.continuum_control.dto.anomalia;

import com.infnet.continuum_control.model.enums.NivelRisco;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AlterarRiscoRequest(
        @NotNull(message = "O risco é obrigatório")
        NivelRisco risco,

        @NotBlank(message = "A justificativa é obrigatória")
        @Size(max = 4000, message = "A justificativa deve ter no máximo 4000 caracteres")
        String justificativa
) {
}
