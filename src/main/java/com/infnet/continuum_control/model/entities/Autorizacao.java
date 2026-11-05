package com.infnet.continuum_control.model.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


public class Autorizacao {
    private long id;
    private Missao missao;
    private Usuario responsavel;
    private LocalDateTime dataHora;
    private String observacao;
}
