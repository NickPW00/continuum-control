package com.infnet.continuum_control.dto.evento_historico;

import com.infnet.continuum_control.model.enums.Importancia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AtualizarEventoRequest(
        @Size(max = 200, message = "O código deve ter no máximo 20 caracteres")
        String acontecimento,

        String descricao,

        LocalDate data,

        @Size(max = 150, message = "O código deve ter no máximo 150 caracteres")
        String localizacao,

        Importancia importancia
) {
}
