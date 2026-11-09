package com.infnet.continuum_control.dto.evento_historico;

import com.infnet.continuum_control.model.entities.EventoHistorico;
import com.infnet.continuum_control.model.enums.Importancia;

import java.time.LocalDate;

public record EventoResponse(
        Long id,
        String acontecimento,
        String descricao,
        LocalDate data,
        String localizacao,
        Importancia importancia
) {
    public static EventoResponse de(EventoHistorico evento){
        return new EventoResponse(
                evento.getId(),
                evento.getAcontecimento(),
                evento.getDescricao(),
                evento.getData(),
                evento.getLocalizacao(),
                evento.getImportancia()
        );
    }
}
