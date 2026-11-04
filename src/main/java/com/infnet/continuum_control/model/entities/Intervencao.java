package com.infnet.continuum_control.model.entities;

import com.infnet.continuum_control.model.enums.Impacto;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
public class Intervencao {
    @Id
    private long id;
    private Missao missao;
    private Agente agente;
    private LocalDateTime dataHora;
    private String acao;
    private String justificativa;
    private Impacto impactoEstimado;
    private String resultadoObservado;

    public boolean isSevera(){return false;}
}
