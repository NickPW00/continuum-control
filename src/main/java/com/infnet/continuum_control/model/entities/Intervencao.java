package com.infnet.continuum_control.model.entities;

import com.infnet.continuum_control.exception.RegraDeNegocioException;
import com.infnet.continuum_control.model.enums.Impacto;
import com.infnet.continuum_control.model.enums.SituacaoMissao;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "intervencao")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Intervencao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "missao_id", nullable = false, updatable = false)
    private Missao missao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agente_id", nullable = false, updatable = false)
    private Agente agente;

    @Column(name = "data_hora", nullable = false, updatable = false)
    private LocalDateTime dataHora;

    @Column(name = "acao", nullable = false, columnDefinition = "TEXT")
    private String acao;

    @Column(name = "justificativa", nullable = false, columnDefinition = "TEXT")
    private String justificativa;

    @Enumerated(EnumType.STRING)
    @Column(name = "impacto_estimado", nullable = false, length = 15)
    private Impacto impactoEstimado;

    @Column(name = "resultado_observado", columnDefinition = "TEXT")
    private String resultadoObservado;

    Intervencao(Missao missao, Agente agente, String acao, String justificativa,
                Impacto impactoEstimado, String resultadoObservado, LocalDateTime agora) {
        this.missao = Objects.requireNonNull(missao);
        this.agente = Objects.requireNonNull(agente);
        this.acao = Objects.requireNonNull(acao, "A ação é obrigatória");
        this.justificativa = Objects.requireNonNull(justificativa, "A justificativa é obrigatória");
        this.impactoEstimado = Objects.requireNonNull(impactoEstimado, "O impacto estimado é obrigatório");
        this.resultadoObservado = resultadoObservado;
        this.dataHora = Objects.requireNonNull(agora);
    }

    public boolean isSevera() {
        return this.impactoEstimado == Impacto.SEVERO;
    }

    public boolean eDoAgente(Agente outro) {
        return Entidades.mesmoAgente(this.agente, outro);
    }

    /** O resultado pode ser conhecido só depois da ação; é informado uma vez, com a missão em execução. */
    public void registrarResultado(String resultado) {
        Objects.requireNonNull(resultado, "O resultado observado é obrigatório");
        if (this.missao.getSituacao() != SituacaoMissao.EM_EXECUCAO) {
            throw new RegraDeNegocioException("M9", "Só missões em execução aceitam alterações nas intervenções");
        }
        if (this.resultadoObservado != null) {
            throw new RegraDeNegocioException("M9", "O resultado observado desta intervenção já foi informado");
        }
        this.resultadoObservado = resultado;
    }
}
