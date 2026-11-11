package com.infnet.continuum_control.dto.agente;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** PATCH /agentes/{id}: o código não muda (doc 05, seção 5). */
public record AtualizarAgenteRequest(
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres")
        @Pattern(regexp = "(?s).*\\S.*", message = "O nome não pode ser vazio")
        String nome,

        @Size(max = 80, message = "A especialidade deve ter no máximo 80 caracteres")
        @Pattern(regexp = "(?s).*\\S.*", message = "A especialidade não pode ser vazia")
        String especialidade
) {
}
