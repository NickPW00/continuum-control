package com.infnet.continuum_control.dto.anomalia;

import com.infnet.continuum_control.model.enums.OrigemAnomalia;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** PATCH /anomalias/{id}: só divergência, origem e observações. Risco e situação têm ações próprias. */
public record AtualizarAnomaliaRequest(
        @Size(max = 4000, message = "A divergência observada deve ter no máximo 4000 caracteres")
        @Pattern(regexp = "(?s).*\\S.*", message = "A divergência observada não pode ser vazia")
        String divergenciaObservada,

        OrigemAnomalia origem,

        @Size(max = 4000, message = "As observações devem ter no máximo 4000 caracteres")
        String observacoes
) {
}
