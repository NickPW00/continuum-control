package com.infnet.continuum_control.dto.intervencao;

import com.infnet.continuum_control.model.enums.Impacto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** O agente é sempre o do usuário autenticado: o corpo não tem agenteId (S4). */
public record CriarIntervencaoRequest(
        @NotBlank(message = "A ação é obrigatória")
        @Size(max = 4000, message = "A ação deve ter no máximo 4000 caracteres")
        String acao,

        @NotBlank(message = "A justificativa é obrigatória")
        @Size(max = 4000, message = "A justificativa deve ter no máximo 4000 caracteres")
        String justificativa,

        @NotNull(message = "O impacto estimado é obrigatório")
        Impacto impactoEstimado,

        @Size(max = 4000, message = "O resultado observado deve ter no máximo 4000 caracteres")
        String resultadoObservado
) {
}
