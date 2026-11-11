package com.infnet.continuum_control.model.entities;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "missao_agente")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MissaoAgente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "missao_id", nullable = false, updatable = false)
    private Missao missao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agente_id", nullable = false, updatable = false)
    private Agente agente;

    @Column(name = "data_designacao", nullable = false, updatable = false)
    private LocalDateTime dataDesignacao;

    @Column(name = "data_saida")
    private LocalDateTime dataSaida;

    MissaoAgente(Missao missao, Agente agente, LocalDateTime agora) {
        this.missao = Objects.requireNonNull(missao);
        this.agente = Objects.requireNonNull(agente);
        this.dataDesignacao = Objects.requireNonNull(agora);
    }

    public boolean isAberta() {
        return this.dataSaida == null;
    }

    public boolean eDoAgente(Agente outro) {
        return Entidades.mesmoAgente(this.agente, outro);
    }

    public boolean eDoAgente(Long agenteId) {
        return agenteId != null && agenteId.equals(this.agente.getId());
    }

    void encerrarParticipacao(LocalDateTime agora) {
        if (this.dataSaida == null) {
            this.dataSaida = agora;
        }
    }
}
