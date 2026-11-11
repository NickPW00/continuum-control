package com.infnet.continuum_control.model.entities;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Gravação pendente em outro banco (doc 04, D40). Não é entidade de domínio: é a ponte que deixa
 * a ocorrência e o histórico entrarem na mesma transação do PostgreSQL.
 */
@Entity
@Table(name = "outbox")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MensagemOutbox {

    public static final String DESTINO_OCORRENCIAS = "MONGO_OCORRENCIAS";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "destino", nullable = false, length = 30, updatable = false)
    private String destino;

    @Column(name = "chave", nullable = false, length = 40, updatable = false)
    private String chave;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb", updatable = false)
    private String payload;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "processado_em")
    private LocalDateTime processadoEm;

    @Column(name = "proxima_tentativa", nullable = false)
    private LocalDateTime proximaTentativa;

    @Column(name = "tentativas", nullable = false)
    private Integer tentativas;

    @Column(name = "ultimo_erro", columnDefinition = "TEXT")
    private String ultimoErro;

    public MensagemOutbox(String destino, String chave, String payload, LocalDateTime agora) {
        this.destino = Objects.requireNonNull(destino);
        this.chave = Objects.requireNonNull(chave);
        this.payload = Objects.requireNonNull(payload);
        this.criadoEm = Objects.requireNonNull(agora);
        this.proximaTentativa = agora;
        this.tentativas = 0;
    }

    public void marcarProcessada(LocalDateTime agora) {
        this.processadoEm = agora;
        this.ultimoErro = null;
    }

    /** Espera crescente: 2^tentativas segundos, até o máximo configurado. Nenhuma linha é abandonada. */
    public void adiar(String erro, LocalDateTime agora, Duration esperaMaxima) {
        this.tentativas++;
        this.ultimoErro = erro;
        long segundos = Math.min(1L << Math.min(this.tentativas, 20), esperaMaxima.toSeconds());
        this.proximaTentativa = agora.plusSeconds(segundos);
    }
}
