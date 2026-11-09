package com.infnet.continuum_control.dto.agente;

import com.infnet.continuum_control.model.entities.Agente;
import com.infnet.continuum_control.model.enums.SituacaoAgente;

public record AgenteResponse(
        Long id,
        String codigo,
        String nome,
        String especialidade,
        SituacaoAgente situacao
) {

    public static AgenteResponse de(Agente agente) {
        return new AgenteResponse(
                agente.getId(),
                agente.getCodigo(),
                agente.getNome(),
                agente.getEspecialidade(),
                agente.getSituacao()
        );
    }
}
