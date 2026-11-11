package com.infnet.continuum_control.model.ocorrencias;

import com.infnet.continuum_control.model.enums.Impacto;
import com.infnet.continuum_control.model.enums.TipoOcorrencia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("ocorrencias")
@TypeAlias("INCIDENTE_OPERACIONAL")
@Getter
@Setter
public class IncidenteOperacional extends Ocorrencia {

    @NotNull(message = "A gravidade é obrigatória")
    private Impacto gravidade;

    @NotBlank(message = "A medida tomada é obrigatória")
    private String medidaTomada;

    public IncidenteOperacional() {
        super(TipoOcorrencia.INCIDENTE_OPERACIONAL);
    }
}
