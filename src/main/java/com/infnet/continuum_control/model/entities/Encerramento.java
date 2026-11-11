package com.infnet.continuum_control.model.entities;

import com.infnet.continuum_control.model.enums.Impacto;
import com.infnet.continuum_control.model.enums.SituacaoAnomalia;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "encerramento")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Encerramento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "missao_id", nullable = false, updatable = false)
    private Missao missao;

    @Column(name = "resultado", nullable = false, columnDefinition = "TEXT")
    private String resultado;

    @Column(name = "resumo", nullable = false, columnDefinition = "TEXT")
    private String resumo;

    @Enumerated(EnumType.STRING)
    @Column(name = "impacto_observado", nullable = false, length = 15)
    private Impacto impactoObservado;

    @Enumerated(EnumType.STRING)
    @Column(name = "situacao_final_anomalia", nullable = false, length = 15)
    private SituacaoAnomalia situacaoFinalAnomalia;

    @Column(name = "data_hora", nullable = false, updatable = false)
    private LocalDateTime dataHora;

    @Column(name = "observacoes", columnDefinition = "TEXT")
    private String observacoes;

    Encerramento(Missao missao, DadosEncerramento dados, LocalDateTime agora) {
        this.missao = Objects.requireNonNull(missao);
        this.resultado = Objects.requireNonNull(dados.resultado(), "O resultado é obrigatório");
        this.resumo = Objects.requireNonNull(dados.resumo(), "O resumo é obrigatório");
        this.impactoObservado = Objects.requireNonNull(dados.impactoObservado(), "O impacto observado é obrigatório");
        this.situacaoFinalAnomalia = Objects.requireNonNull(dados.situacaoFinalAnomalia(), "A situação final é obrigatória");
        this.observacoes = dados.observacoes();
        this.dataHora = Objects.requireNonNull(agora);
    }
}
