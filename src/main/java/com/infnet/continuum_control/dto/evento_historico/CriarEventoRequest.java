package com.infnet.continuum_control.dto.evento_historico;

import com.infnet.continuum_control.model.enums.Importancia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CriarEventoRequest(
        @NotBlank(message = "O acontecimento é obrigatório")
        @Size(max = 200, message = "O código deve ter no máximo 20 caracteres")
        String acontecimento,

        @NotBlank(message = "A descricao é obrigatório")
        String descricao,

        @NotBlank(message = "A data é obrigatória")
        LocalDate data,

        @NotBlank(message = "O código é obrigatório")
        @Size(max = 150, message = "O código deve ter no máximo 150 caracteres")
        String localizacao,

        @NotBlank(message = "O importancia é obrigatório")
        Importancia importancia
){
}
