package com.infnet.continuum_control.dto.intervencao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResultadoIntervencaoRequest(
        @NotBlank(message = "O resultado observado é obrigatório")
        @Size(max = 4000, message = "O resultado observado deve ter no máximo 4000 caracteres")
        String resultadoObservado
) {
}
