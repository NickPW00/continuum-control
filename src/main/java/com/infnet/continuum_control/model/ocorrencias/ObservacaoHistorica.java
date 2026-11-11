package com.infnet.continuum_control.model.ocorrencias;

import com.infnet.continuum_control.model.enums.TipoOcorrencia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("ocorrencias")
@TypeAlias("OBSERVACAO_HISTORICA")
@Getter
@Setter
public class ObservacaoHistorica extends Ocorrencia {

    @NotBlank(message = "O fato observado é obrigatório")
    private String fatoObservado;

    @NotNull(message = "Informe se o fato está conforme o esperado")
    private Boolean conformeEsperado;

    public ObservacaoHistorica() {
        super(TipoOcorrencia.OBSERVACAO_HISTORICA);
    }
}
