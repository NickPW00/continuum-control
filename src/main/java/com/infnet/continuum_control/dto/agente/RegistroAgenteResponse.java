package com.infnet.continuum_control.dto.agente;

import com.infnet.continuum_control.model.entities.RegistroAgente;
import com.infnet.continuum_control.model.enums.SituacaoAgente;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public record RegistroAgenteResponse(
        SituacaoAgente situacaoAnterior,
        SituacaoAgente situacaoNova,
        String motivo,
        LocalDateTime dataHora,
        Long usuarioId
) {
    public static RegistroAgenteResponse de(RegistroAgente r) {
        return new RegistroAgenteResponse(r.getSituacaoAnterior(), r.getSituacaoNova(), r.getMotivo(),
                r.getDataHora(), r.getUsuario() == null ? null : r.getUsuario().getId());
    }

    public static record MotivoRequest(
            @NotBlank(message = "O motivo é obrigatório")
            String motivo
    ) {
    }
}
