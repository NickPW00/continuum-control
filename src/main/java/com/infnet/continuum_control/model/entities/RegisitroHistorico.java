package com.infnet.continuum_control.model.entities;

import com.infnet.continuum_control.model.enums.TipoRegistro;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@NoArgsConstructor
public class RegisitroHistorico {
    @Id
    private long id;
    private Anomalia anomalia;
    private TipoRegistro tipo;
    private LocalDate dataHora;
    private String descricao;
    private String valorAnterior;
    private String valorNovo;
    private Usuario usuario;
}
