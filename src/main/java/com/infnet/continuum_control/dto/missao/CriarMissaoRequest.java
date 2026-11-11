package com.infnet.continuum_control.dto.missao;

import com.infnet.continuum_control.model.enums.Prioridade;
import com.infnet.continuum_control.model.enums.PropositoMissao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CriarMissaoRequest(
        @NotBlank(message = "O código é obrigatório")
        @Size(max = 20, message = "O código deve ter no máximo 20 caracteres")
        String codigo,

        @NotBlank(message = "O objetivo é obrigatório")
        @Size(max = 4000, message = "O objetivo deve ter no máximo 4000 caracteres")
        String objetivo,

        @NotNull(message = "O propósito é obrigatório")
        PropositoMissao proposito,

        @NotNull(message = "A anomalia é obrigatória")
        Long anomaliaId,

        @NotNull(message = "O destino temporal é obrigatório")
        LocalDate destinoTemporal,

        @NotNull(message = "A prioridade é obrigatória")
        Prioridade prioridade
) {
}
