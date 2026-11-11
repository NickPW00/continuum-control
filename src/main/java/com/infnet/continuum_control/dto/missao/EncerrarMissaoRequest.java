package com.infnet.continuum_control.dto.missao;

import com.infnet.continuum_control.model.entities.DadosEncerramento;
import com.infnet.continuum_control.model.enums.Impacto;
import com.infnet.continuum_control.model.enums.SituacaoAnomalia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Corpo de concluir e registrar-falha (§13). */
public record EncerrarMissaoRequest(
        @NotBlank(message = "O resultado é obrigatório")
        @Size(max = 4000, message = "O resultado deve ter no máximo 4000 caracteres")
        String resultado,

        @NotBlank(message = "O resumo é obrigatório")
        @Size(max = 4000, message = "O resumo deve ter no máximo 4000 caracteres")
        String resumo,

        @NotNull(message = "O impacto observado é obrigatório")
        Impacto impactoObservado,

        @NotNull(message = "A situação final da anomalia é obrigatória")
        SituacaoAnomalia situacaoFinalAnomalia,

        @Size(max = 4000, message = "As observações devem ter no máximo 4000 caracteres")
        String observacoes
) {
    public DadosEncerramento paraDados() {
        return new DadosEncerramento(resultado, resumo, impactoObservado, situacaoFinalAnomalia, observacoes);
    }
}
