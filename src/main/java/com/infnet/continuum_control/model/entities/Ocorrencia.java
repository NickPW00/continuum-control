package com.infnet.continuum_control.model.entities;

import com.infnet.continuum_control.model.enums.TipoOcorrencia;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
public abstract class Ocorrencia {
    @Id
    private long id;
    private Missao missao;
    private Agente agente;
    private LocalDateTime dataHora;
    private String observacao;
    private TipoOcorrencia tipo;
}
