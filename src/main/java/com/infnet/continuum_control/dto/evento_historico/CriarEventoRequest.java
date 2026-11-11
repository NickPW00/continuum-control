package com.infnet.continuum_control.dto.evento_historico;

import com.infnet.continuum_control.model.enums.Importancia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CriarEventoRequest(
        @NotBlank(message = "O acontecimento é obrigatório")
        @Size(max = 200, message = "O acontecimento deve ter no máximo 200 caracteres")
        String acontecimento,

        @NotBlank(message = "A descrição é obrigatória")
        String descricao,

        @NotNull(message = "A data é obrigatória")
        LocalDate data,

        @NotBlank(message = "A localização é obrigatória")
        @Size(max = 150, message = "A localização deve ter no máximo 150 caracteres")
        String localizacao,

        @NotNull(message = "A importância é obrigatória")
        Importancia importancia
){
}
