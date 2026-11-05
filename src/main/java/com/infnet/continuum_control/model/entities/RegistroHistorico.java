package com.infnet.continuum_control.model.entities;

import com.infnet.continuum_control.model.enums.TipoRegistro;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;


public class RegistroHistorico {
    private long id;
    private Anomalia anomalia;
    private TipoRegistro tipo;
    private LocalDateTime dataHora;
    private String descricao;
    private String valorAnterior;
    private String valorNovo;
    private Usuario usuario;
}
