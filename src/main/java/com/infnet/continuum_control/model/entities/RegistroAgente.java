package com.infnet.continuum_control.model.entities;

import com.infnet.continuum_control.model.enums.SituacaoAgente;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;
import java.util.Objects;

/** Linha do tempo das mudanças de situação de um agente (D31). Só recebe inserções. */
@Entity
@Immutable
@Table(name = "registro_agente")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RegistroAgente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agente_id", nullable = false, updatable = false)
    private Agente agente;

    @Enumerated(EnumType.STRING)
    @Column(name = "situacao_anterior", length = 10, updatable = false)
    private SituacaoAgente situacaoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "situacao_nova", nullable = false, length = 10, updatable = false)
    private SituacaoAgente situacaoNova;

    @Column(name = "motivo", nullable = false, columnDefinition = "TEXT", updatable = false)
    private String motivo;

    @Column(name = "data_hora", nullable = false, updatable = false)
    private LocalDateTime dataHora;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", updatable = false)
    private Usuario usuario;

    public RegistroAgente(Agente agente, SituacaoAgente situacaoAnterior, SituacaoAgente situacaoNova,
                          String motivo, Usuario usuario, LocalDateTime agora) {
        this.agente = Objects.requireNonNull(agente);
        this.situacaoAnterior = situacaoAnterior;
        this.situacaoNova = Objects.requireNonNull(situacaoNova);
        this.motivo = Objects.requireNonNull(motivo);
        this.usuario = usuario;
        this.dataHora = Objects.requireNonNull(agora);
    }
}
