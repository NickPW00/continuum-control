package com.infnet.continuum_control.dto.anomalia;

import com.infnet.continuum_control.model.enums.NivelRisco;
import com.infnet.continuum_control.model.enums.OrigemAnomalia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CriarAnomaliaRequest(
        @NotNull(message = "O evento é obrigatório")
        Long eventoId,

        @NotBlank(message = "A divergência observada é obrigatória")
        @Size(max = 4000, message = "A divergência observada deve ter no máximo 4000 caracteres")
        String divergenciaObservada,

        @NotNull(message = "O risco é obrigatório")
        NivelRisco risco,

        OrigemAnomalia origem,

        @Size(max = 4000, message = "As observações devem ter no máximo 4000 caracteres")
        String observacoes,

        LocalDateTime dataHoraDeteccao
) {
}
