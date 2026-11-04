package com.infnet.continuum_control.model.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
public class MissaoAgente {
    @Id
    private long id;
    private Missao missao;
    private Agente agente;
    private LocalDateTime dataDesignacao;
    private LocalDateTime dataSaida;
    public void encerrarParticipacao(){}
}
