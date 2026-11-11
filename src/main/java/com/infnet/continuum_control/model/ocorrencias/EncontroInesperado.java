package com.infnet.continuum_control.model.ocorrencias;

import com.infnet.continuum_control.model.enums.TipoOcorrencia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("ocorrencias")
@TypeAlias("ENCONTRO_INESPERADO")
@Getter
@Setter
public class EncontroInesperado extends Ocorrencia {

    @NotBlank(message = "Os envolvidos são obrigatórios")
    private String envolvidos;

    @NotNull(message = "Informe se houve contato")
    private Boolean houveContato;

    public EncontroInesperado() {
        super(TipoOcorrencia.ENCONTRO_INESPERADO);
    }
}
