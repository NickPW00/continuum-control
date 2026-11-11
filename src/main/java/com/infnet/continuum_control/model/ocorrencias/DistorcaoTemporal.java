package com.infnet.continuum_control.model.ocorrencias;

import com.infnet.continuum_control.model.enums.TipoOcorrencia;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("ocorrencias")
@TypeAlias("DISTORCAO_TEMPORAL")
@Getter
@Setter
public class DistorcaoTemporal extends Ocorrencia {

    @NotNull(message = "A intensidade é obrigatória")
    @Min(value = 1, message = "A intensidade vai de 1 a 10")
    @Max(value = 10, message = "A intensidade vai de 1 a 10")
    private Integer intensidade;

    /** Duração em segundos (doc 04, tabela 4.1; doc 05, 1.2). */
    @NotNull(message = "A duração é obrigatória")
    @Min(value = 0, message = "A duração não pode ser negativa")
    private Long duracao;

    public DistorcaoTemporal() {
        super(TipoOcorrencia.DISTORCAO_TEMPORAL);
    }
}
