package com.infnet.continuum_control.model.ocorrencias;

import com.infnet.continuum_control.model.enums.TipoOcorrencia;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("ocorrencias")
@TypeAlias("EVIDENCIA_TEMPORAL")
@Getter
@Setter
public class EvidenciaTemporal extends Ocorrencia {

    @NotBlank(message = "O objeto é obrigatório")
    private String objeto;

    @NotBlank(message = "O período estimado é obrigatório")
    private String periodoEstimado;

    @NotNull(message = "O grau de confiabilidade é obrigatório")
    @DecimalMin(value = "0.0", message = "O grau de confiabilidade vai de 0 a 1")
    @DecimalMax(value = "1.0", message = "O grau de confiabilidade vai de 0 a 1")
    private Double grauConfiabilidade;

    public EvidenciaTemporal() {
        super(TipoOcorrencia.EVIDENCIA_TEMPORAL);
    }
}
