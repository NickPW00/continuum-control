package com.infnet.continuum_control.model.entities;

import com.infnet.continuum_control.model.enums.TipoRegistro;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;
import java.util.Objects;

/** Linha do tempo de uma anomalia (§12). Só recebe inserções (D9, D20). */
@Entity
@Immutable
@Table(name = "registro_historico")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RegistroHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "anomalia_id", nullable = false, updatable = false)
    private Anomalia anomalia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "missao_id", updatable = false)
    private Missao missao;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20, updatable = false)
    private TipoRegistro tipo;

    @Column(name = "data_hora", nullable = false, updatable = false)
    private LocalDateTime dataHora;

    @Column(name = "descricao", nullable = false, columnDefinition = "TEXT", updatable = false)
    private String descricao;

    @Column(name = "valor_anterior", length = 255, updatable = false)
    private String valorAnterior;

    @Column(name = "valor_novo", length = 255, updatable = false)
    private String valorNovo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", updatable = false)
    private Usuario usuario;

    public RegistroHistorico(Anomalia anomalia, Missao missao, TipoRegistro tipo, String descricao,
                             String valorAnterior, String valorNovo, Usuario usuario, LocalDateTime agora) {
        this.anomalia = Objects.requireNonNull(anomalia);
        this.missao = missao;
        this.tipo = Objects.requireNonNull(tipo);
        this.descricao = Objects.requireNonNull(descricao);
        this.valorAnterior = valorAnterior;
        this.valorNovo = valorNovo;
        this.usuario = usuario;
        this.dataHora = Objects.requireNonNull(agora);
    }
}
