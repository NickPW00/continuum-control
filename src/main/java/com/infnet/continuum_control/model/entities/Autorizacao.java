package com.infnet.continuum_control.model.entities;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "autorizacao")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Autorizacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "missao_id", nullable = false, updatable = false)
    private Missao missao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsavel_id", nullable = false, updatable = false)
    private Usuario responsavel;

    @Column(name = "data_hora", nullable = false, updatable = false)
    private LocalDateTime dataHora;

    @Column(name = "observacao", nullable = false, columnDefinition = "TEXT")
    private String observacao;

    Autorizacao(Missao missao, Usuario responsavel, String observacao, LocalDateTime agora) {
        this.missao = Objects.requireNonNull(missao);
        this.responsavel = Objects.requireNonNull(responsavel, "O responsável é obrigatório");
        this.observacao = Objects.requireNonNull(observacao, "A observação é obrigatória");
        this.dataHora = Objects.requireNonNull(agora);
    }
}
