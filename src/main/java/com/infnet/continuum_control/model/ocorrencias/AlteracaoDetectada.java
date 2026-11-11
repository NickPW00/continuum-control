package com.infnet.continuum_control.model.ocorrencias;

import com.infnet.continuum_control.model.enums.TipoOcorrencia;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("ocorrencias")
@TypeAlias("ALTERACAO_DETECTADA")
@Getter
@Setter
public class AlteracaoDetectada extends Ocorrencia {

    @NotBlank(message = "O valor esperado é obrigatório")
    private String valorEsperado;

    @NotBlank(message = "O valor observado é obrigatório")
    private String valorObservado;

    public AlteracaoDetectada() {
        super(TipoOcorrencia.ALTERACAO_DETECTADA);
    }
}
