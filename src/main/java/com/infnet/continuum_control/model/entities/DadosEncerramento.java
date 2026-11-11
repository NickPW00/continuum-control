package com.infnet.continuum_control.model.entities;

import com.infnet.continuum_control.model.enums.Impacto;
import com.infnet.continuum_control.model.enums.SituacaoAnomalia;

/** As cinco informações do encerramento pedidas pelo §13. */
public record DadosEncerramento(
        String resultado,
        String resumo,
        Impacto impactoObservado,
        SituacaoAnomalia situacaoFinalAnomalia,
        String observacoes
) {
}
