package com.infnet.continuum_control.model.entities;

import com.infnet.continuum_control.model.enums.TipoRegistro;

import java.time.LocalDate;

public class RegisitroHistorico {
    private long id;
    private Anomalia anomalia;
    private TipoRegistro tipo;
    private LocalDate dataHora;
    private String descricao;
    private String valorAnterior;
    private String valorNovo;
    private Usuario usuario;
}
