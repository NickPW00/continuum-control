package com.infnet.continuum_control.model.ocorrencias;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.infnet.continuum_control.model.enums.TipoOcorrencia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Ocorrência (§11), guardada no MongoDB, coleção {@code ocorrencias} (doc 04, seção 4).
 * Cada subtipo tem campos próprios (D4). O campo {@code tipo} é ao mesmo tempo o discriminador
 * do Jackson (corpo da API e payload do outbox, D51) e do Spring Data (no lugar de {@code _class}).
 */
@Document("ocorrencias")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "tipo")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ObservacaoHistorica.class, name = "OBSERVACAO_HISTORICA"),
        @JsonSubTypes.Type(value = AlteracaoDetectada.class, name = "ALTERACAO_DETECTADA"),
        @JsonSubTypes.Type(value = EvidenciaTemporal.class, name = "EVIDENCIA_TEMPORAL"),
        @JsonSubTypes.Type(value = DistorcaoTemporal.class, name = "DISTORCAO_TEMPORAL"),
        @JsonSubTypes.Type(value = EncontroInesperado.class, name = "ENCONTRO_INESPERADO"),
        @JsonSubTypes.Type(value = IncidenteOperacional.class, name = "INCIDENTE_OPERACIONAL")
})
@Getter
@Setter
public abstract class Ocorrencia {

    @Id
    private String id;

    @Setter(AccessLevel.NONE)
    private TipoOcorrencia tipo;

    private Long missaoId;

    private Long agenteId;

    private LocalDateTime dataHora;

    @NotBlank(message = "A observação é obrigatória")
    @Size(max = 4000, message = "A observação deve ter no máximo 4000 caracteres")
    private String observacao;

    protected Ocorrencia(TipoOcorrencia tipo) {
        this.tipo = tipo;
    }

    /** Identidade, missão, autor e instante vêm do servidor, nunca do corpo da requisição (S4). */
    public void preencher(String id, Long missaoId, Long agenteId, LocalDateTime agora) {
        this.id = id;
        this.missaoId = missaoId;
        this.agenteId = agenteId;
        this.dataHora = agora;
    }
}
