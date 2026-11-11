package com.infnet.continuum_control.dto.missao;

import jakarta.validation.constraints.NotNull;

public record DesignacaoRequest(
        @NotNull(message = "O agente é obrigatório")
        Long agenteId
) {
}
