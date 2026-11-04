package com.infnet.continuum_control.model.entities;

import java.time.LocalDateTime;

public class MissaoAgente {
    private long id;
    private Missao missao;
    private Agente agente;
    private LocalDateTime dataDesignacao;
    private LocalDateTime dataSaida;
    public void encerrarParticipacao(){}
}
