package com.infnet.continuum_control.model.entities;

import com.infnet.continuum_control.model.enums.Importancia;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@NoArgsConstructor
public class EventoHistorico {
    @Id
    private long id;
    private String acontecimento;
    private String descricao;
    private LocalDate data;
    private String localizacao;
    private Importancia importancia;

    boolean isCritico(){return true;}
}
