package com.infnet.continuum_control.model.entities;

import com.infnet.continuum_control.model.enums.TipoOcorrencia;

import java.time.LocalDateTime;

public abstract class Ocorrencia {
    private long id;
    private Missao missao;
    private Agente agente;
    private LocalDateTime dataHora;
    private String observacao;
    private TipoOcorrencia tipo;
}
