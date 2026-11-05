package com.infnet.continuum_control.model.entities;

import com.infnet.continuum_control.model.enums.Impacto;
import com.infnet.continuum_control.model.enums.SituacaoAnomalia;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


public class Encerramento {
    private long id;
    private Missao missao;
    private String resultado;
    private String resumo;
    private Impacto impactoObservado;
    private SituacaoAnomalia situacaoFinalAnomalia;
    private LocalDateTime dataHora;
    private String observacoes;
}
