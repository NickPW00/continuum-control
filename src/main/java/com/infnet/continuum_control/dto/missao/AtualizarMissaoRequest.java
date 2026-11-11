package com.infnet.continuum_control.dto.missao;

import com.infnet.continuum_control.model.enums.Prioridade;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** PATCH /missoes/{id}: só em PLANEJADA (D28). */
public record AtualizarMissaoRequest(
        @Size(max = 4000, message = "O objetivo deve ter no máximo 4000 caracteres")
        @Pattern(regexp = "(?s).*\\S.*", message = "O objetivo não pode ser vazio")
        String objetivo,

        LocalDate destinoTemporal,

        Prioridade prioridade
) {
}
