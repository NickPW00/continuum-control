package com.infnet.continuum_control.model.entities.filhos.ocorrencia;

import com.infnet.continuum_control.model.entities.Ocorrencia;
import com.infnet.continuum_control.model.enums.Impacto;

public class IncidenteOperacional extends Ocorrencia {
    private Impacto gravidade;
    private String medidaTomada;
}
