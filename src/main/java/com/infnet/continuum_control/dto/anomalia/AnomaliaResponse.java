package com.infnet.continuum_control.dto.anomalia;

import com.infnet.continuum_control.model.entities.Anomalia;
import com.infnet.continuum_control.model.enums.NivelRisco;
import com.infnet.continuum_control.model.enums.OrigemAnomalia;
import com.infnet.continuum_control.model.enums.SituacaoAnomalia;

import java.time.LocalDateTime;

/** {@code ativa} e {@code critica} são calculados com as definições do doc 03 (5.3). */
public record AnomaliaResponse(
        Long id,
        Long eventoId,
        String divergenciaObservada,
        LocalDateTime dataHoraDeteccao,
        NivelRisco risco,
        SituacaoAnomalia situacao,
        OrigemAnomalia origem,
        String observacoes,
        boolean ativa,
        boolean critica
) {
    public static AnomaliaResponse de(Anomalia a) {
        return new AnomaliaResponse(a.getId(), a.getEvento().getId(), a.getDivergenciaObservada(),
                a.getDataHoraDeteccao(), a.getRisco(), a.getSituacao(), a.getOrigem(), a.getObservacoes(),
                a.isAtiva(), a.isCritica());
    }
}
