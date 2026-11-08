package com.infnet.continuum_control.dto.evento_historico;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EventoHistoricoRequest (
        @NotBlank(message = "O código é obrigatório")
        @Size(max = 20, message = "O código deve ter no máximo 20 caracteres")
        String acontecimento,

        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres")
        String nome,

        @NotBlank(message = "A especialidade é obrigatória")
        @Size(max = 80, message = "A especialidade deve ter no máximo 80 caracteres")
        String especialidade
){
}
