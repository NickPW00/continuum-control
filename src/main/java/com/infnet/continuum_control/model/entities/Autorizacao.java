package com.infnet.continuum_control.model.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
public class Autorizacao {
    @Id
    private long id;
    private Missao missao;
    private Usuario responsavel;
    private LocalDateTime dataHora;
    private String observacao;
}
