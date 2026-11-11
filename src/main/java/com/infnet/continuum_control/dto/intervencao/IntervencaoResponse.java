package com.infnet.continuum_control.dto.intervencao;

import com.infnet.continuum_control.model.entities.Intervencao;
import com.infnet.continuum_control.model.enums.Impacto;

import java.time.LocalDateTime;

public record IntervencaoResponse(
        Long id,
        Long missaoId,
        Long agenteId,
        LocalDateTime dataHora,
        String acao,
        String justificativa,
        Impacto impactoEstimado,
        String resultadoObservado
) {
    public static IntervencaoResponse de(Intervencao i) {
        return new IntervencaoResponse(i.getId(), i.getMissao().getId(), i.getAgente().getId(), i.getDataHora(),
                i.getAcao(), i.getJustificativa(), i.getImpactoEstimado(), i.getResultadoObservado());
    }
}
