package com.infnet.continuum_control.dto.missao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** O responsável vem do token (S4); o §8 exige "pelo menos" uma observação. */
public record AutorizarRequest(
        @NotBlank(message = "A observação é obrigatória")
        @Size(max = 4000, message = "A observação deve ter no máximo 4000 caracteres")
        String observacao
) {
}
