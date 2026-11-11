package com.infnet.continuum_control.model.entities;

import com.infnet.continuum_control.exception.RegraDeNegocioException;
import com.infnet.continuum_control.model.enums.*;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "anomalia")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Anomalia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id", nullable = false, updatable = false)
    private EventoHistorico evento;

    @Column(name = "divergencia_observada", nullable = false, columnDefinition = "TEXT")
    private String divergenciaObservada;

    @Column(name = "data_hora_deteccao", nullable = false, updatable = false)
    private LocalDateTime dataHoraDeteccao;

    @Enumerated(EnumType.STRING)
    @Column(name = "risco", nullable = false, length = 10)
    private NivelRisco risco;

    @Enumerated(EnumType.STRING)
    @Column(name = "situacao", nullable = false, length = 15)
    private SituacaoAnomalia situacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem", length = 30)
    private OrigemAnomalia origem;

    @Column(name = "observacoes", columnDefinition = "TEXT")
    private String observacoes;

    public Anomalia(EventoHistorico evento, String divergenciaObservada, NivelRisco risco,
                    OrigemAnomalia origem, String observacoes,
                    LocalDateTime dataHoraDeteccao, LocalDateTime agora) {
        this.evento = Objects.requireNonNull(evento, "O evento histórico é obrigatório");
        this.divergenciaObservada = Objects.requireNonNull(divergenciaObservada, "A divergência observada é obrigatória");
        this.risco = Objects.requireNonNull(risco, "O risco é obrigatório");
        Objects.requireNonNull(agora, "O instante atual é obrigatório");
        this.origem = origem;
        this.observacoes = observacoes;
        this.situacao = SituacaoAnomalia.DETECTADA;
        this.dataHoraDeteccao = (dataHoraDeteccao != null) ? dataHoraDeteccao : agora;
        if (this.dataHoraDeteccao.isAfter(agora)) {
            throw new RegraDeNegocioException("N1", "A data de detecção não pode estar no futuro");
        }
    }

    public boolean podeReceberMissao(PropositoMissao p) {
        Objects.requireNonNull(p, "O propósito da missão é obrigatório");
        if (this.isAtiva()) return true;
        return this.situacao == SituacaoAnomalia.IRREVERSIVEL && p == PropositoMissao.INVESTIGACAO;
    }

    public void alterarRisco(NivelRisco novo) {
        Objects.requireNonNull(novo, "O novo risco é obrigatório");
        if (!this.isAtiva()) {
            throw new RegraDeNegocioException("N7", "Só é possível alterar o risco de uma anomalia ativa; situação atual: " + this.situacao);
        }
        this.risco = novo;
    }

    public void declararIrreversivel(String justificativa) {
        Objects.requireNonNull(justificativa, "A justificativa é obrigatória");
        if (this.situacao != SituacaoAnomalia.DETECTADA && this.situacao != SituacaoAnomalia.CONFIRMADA) {
            throw new RegraDeNegocioException("N6", "Só é possível declarar irreversível uma anomalia DETECTADA ou CONFIRMADA; situação atual: " + this.situacao);
        }
        this.situacao = SituacaoAnomalia.IRREVERSIVEL;
    }

    public boolean isAtiva() {
        return this.situacao == SituacaoAnomalia.DETECTADA
                || this.situacao == SituacaoAnomalia.EM_ANALISE
                || this.situacao == SituacaoAnomalia.CONFIRMADA
                || this.situacao == SituacaoAnomalia.EM_CORRECAO;
    }

    public boolean isCritica() {
        return this.isAtiva() && this.risco == NivelRisco.CRITICO;
    }

    void iniciarAtendimento(PropositoMissao p) {
        if (!this.podeReceberMissao(p)) {
            throw new RegraDeNegocioException("D14", "A anomalia na situação " + this.situacao + " não admite missão de " + p);
        }
        if (this.situacao == SituacaoAnomalia.IRREVERSIVEL) return;

        if (this.situacao != SituacaoAnomalia.DETECTADA && this.situacao != SituacaoAnomalia.CONFIRMADA) {
            throw new RegraDeNegocioException("D23", "Já existe um atendimento em curso para a anomalia; situação atual: " + this.situacao);
        }
        this.situacao = (p == PropositoMissao.CORRECAO)
                ? SituacaoAnomalia.EM_CORRECAO
                : SituacaoAnomalia.EM_ANALISE;
    }

    void encerrarAtendimento(SituacaoAnomalia situacaoFinal) {
        Objects.requireNonNull(situacaoFinal, "A situação final é obrigatória");
        Set<SituacaoAnomalia> permitidas = switch (this.situacao) {
            case EM_ANALISE -> EnumSet.of(SituacaoAnomalia.DETECTADA, SituacaoAnomalia.CONFIRMADA,
                    SituacaoAnomalia.ESTABILIZADA, SituacaoAnomalia.IRREVERSIVEL);
            case EM_CORRECAO -> EnumSet.of(SituacaoAnomalia.CONFIRMADA,
                    SituacaoAnomalia.ESTABILIZADA, SituacaoAnomalia.IRREVERSIVEL);
            case IRREVERSIVEL -> EnumSet.of(SituacaoAnomalia.IRREVERSIVEL);
            default -> throw new RegraDeNegocioException("D35", "Não há atendimento em curso para a anomalia; situação atual: " + this.situacao);
        };
        if (!permitidas.contains(situacaoFinal)) {
            throw new RegraDeNegocioException("D35", "Situação final " + situacaoFinal + " não é permitida a partir de " + this.situacao);
        }
        this.situacao = situacaoFinal;
    }
}
