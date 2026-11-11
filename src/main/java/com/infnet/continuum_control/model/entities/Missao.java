package com.infnet.continuum_control.model.entities;

import com.infnet.continuum_control.exception.RegraDeNegocioException;
import com.infnet.continuum_control.model.enums.*;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Missão temporal (§6) e a sua máquina de estados (doc 03, seção 3). A missão comanda os agentes
 * e a anomalia: iniciar e encerrar mudam os três na mesma transação (§9).
 */
@Entity
@Table(name = "missao")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Missao {

    private static final Set<SituacaoMissao> PENDENTES = EnumSet.of(SituacaoMissao.PLANEJADA, SituacaoMissao.AUTORIZADA);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", nullable = false, length = 20, updatable = false)
    private String codigo;

    @Column(name = "objetivo", nullable = false, columnDefinition = "TEXT")
    private String objetivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "proposito", nullable = false, length = 15, updatable = false)
    private PropositoMissao proposito;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "anomalia_id", nullable = false, updatable = false)
    private Anomalia anomalia;

    @Type(DataHistoricaType.class)
    @Column(name = "destino_temporal", nullable = false)
    private LocalDate destinoTemporal;

    @Enumerated(EnumType.STRING)
    @Column(name = "prioridade", nullable = false, length = 10)
    private Prioridade prioridade;

    @Enumerated(EnumType.STRING)
    @Column(name = "situacao", nullable = false, length = 12)
    private SituacaoMissao situacao;

    @Column(name = "data_inicio")
    private LocalDateTime dataInicio;

    @Column(name = "data_fim")
    private LocalDateTime dataFim;

    @OneToMany(mappedBy = "missao", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    @OrderBy("id")
    private List<MissaoAgente> participacoes = new ArrayList<>();

    @OneToMany(mappedBy = "missao", cascade = CascadeType.PERSIST)
    @OrderBy("dataHora")
    private List<Intervencao> intervencoes = new ArrayList<>();

    @OneToOne(mappedBy = "missao", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    private Autorizacao autorizacao;

    @OneToOne(mappedBy = "missao", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    private Encerramento encerramento;

    /** M1: toda missão nasce PLANEJADA, e só para uma anomalia que a admite (doc 03, 5.3). */
    public Missao(String codigo, String objetivo, PropositoMissao proposito, Anomalia anomalia,
                  LocalDate destinoTemporal, Prioridade prioridade) {
        this.codigo = Objects.requireNonNull(codigo, "O código é obrigatório");
        this.objetivo = Objects.requireNonNull(objetivo, "O objetivo é obrigatório");
        this.proposito = Objects.requireNonNull(proposito, "O propósito é obrigatório");
        this.anomalia = Objects.requireNonNull(anomalia, "A anomalia é obrigatória");
        this.destinoTemporal = Objects.requireNonNull(destinoTemporal, "O destino temporal é obrigatório");
        this.prioridade = Objects.requireNonNull(prioridade, "A prioridade é obrigatória");
        if (!anomalia.podeReceberMissao(proposito)) {
            throw new RegraDeNegocioException("M1", "A anomalia na situação " + anomalia.getSituacao()
                    + " não admite missão de " + proposito);
        }
        this.situacao = SituacaoMissao.PLANEJADA;
    }

    public boolean isPendente() {
        return PENDENTES.contains(this.situacao);
    }

    /** D28: depois de autorizada, a missão não muda o que foi autorizado. */
    public void atualizarDados(String objetivo, LocalDate destinoTemporal, Prioridade prioridade) {
        if (this.situacao != SituacaoMissao.PLANEJADA) {
            throw new RegraDeNegocioException("D28", "Os dados da missão só podem ser alterados enquanto ela está PLANEJADA; situação atual: " + this.situacao);
        }
        if (objetivo != null) this.objetivo = objetivo;
        if (destinoTemporal != null) this.destinoTemporal = destinoTemporal;
        if (prioridade != null) this.prioridade = prioridade;
    }

    /** M2: só em PLANEJADA/AUTORIZADA, só agente DISPONIVEL e ainda não designado. */
    public MissaoAgente designar(Agente agente, LocalDateTime agora) {
        Objects.requireNonNull(agente, "O agente é obrigatório");
        if (!isPendente()) {
            throw new RegraDeNegocioException("M2", "Só é possível designar agentes em missões PLANEJADA ou AUTORIZADA; situação atual: " + this.situacao);
        }
        if (!agente.podeSerDesignado()) {
            throw new RegraDeNegocioException("M2", "Somente agentes disponíveis podem ser designados; situação do agente: " + agente.getSituacao());
        }
        if (participacaoDe(agente).isPresent()) {
            throw new RegraDeNegocioException("M2", "O agente já está designado nesta missão");
        }
        MissaoAgente participacao = new MissaoAgente(this, agente, agora);
        this.participacoes.add(participacao);
        return participacao;
    }

    /** M3 (D25): antes do início a participação é apagada, e o histórico guarda a remoção. */
    public void removerDesignacao(Agente agente) {
        if (!isPendente()) {
            throw new RegraDeNegocioException("M3", "Só é possível remover designações de missões PLANEJADA ou AUTORIZADA; situação atual: " + this.situacao);
        }
        MissaoAgente participacao = participacaoDe(agente)
                .orElseThrow(() -> new RegraDeNegocioException("M3", "O agente não está designado nesta missão"));
        this.participacoes.remove(participacao);
    }

    /** §8 e A5: emergência ou anomalia de risco CRITICO. Avaliada ao iniciar, e não na criação (D28). */
    public boolean exigeAutorizacao() {
        return this.prioridade == Prioridade.EMERGENCIA || this.anomalia.getRisco() == NivelRisco.CRITICO;
    }

    /** M4: só a partir de PLANEJADA, por um SUPERVISOR ou ADMINISTRADOR. */
    public Autorizacao autorizar(Usuario responsavel, String observacao, LocalDateTime agora) {
        Objects.requireNonNull(responsavel, "O responsável é obrigatório");
        if (this.situacao != SituacaoMissao.PLANEJADA) {
            throw new RegraDeNegocioException("M4", "Só missões PLANEJADA podem ser autorizadas; situação atual: " + this.situacao);
        }
        if (responsavel.getPerfil() != Perfil.SUPERVISOR && responsavel.getPerfil() != Perfil.ADMINISTRADOR) {
            throw new RegraDeNegocioException("M4", "Só SUPERVISOR ou ADMINISTRADOR autorizam missões");
        }
        this.autorizacao = new Autorizacao(this, responsavel, observacao, agora);
        this.situacao = SituacaoMissao.AUTORIZADA;
        return this.autorizacao;
    }

    /**
     * M5/M6 (operação 6.1, sem a parte de serviço): confere podeIniciar(), passa a missão para
     * EM_EXECUCAO, cada agente para EM_MISSAO (G2) e a anomalia para EM_ANALISE ou EM_CORRECAO.
     * A regra "nenhuma outra missão em execução na anomalia" (D23) depende de outras linhas e fica
     * no serviço e no índice único do banco.
     */
    public void iniciar(LocalDateTime agora) {
        String regra = this.situacao == SituacaoMissao.PLANEJADA ? "M5" : "M6";
        if (!isPendente()) {
            throw new RegraDeNegocioException(regra, "Só missões PLANEJADA ou AUTORIZADA podem ser iniciadas; situação atual: " + this.situacao);
        }
        if (this.situacao == SituacaoMissao.PLANEJADA && exigeAutorizacao()) {
            throw new RegraDeNegocioException("M5", "A missão exige autorização antes de iniciar ("
                    + (this.prioridade == Prioridade.EMERGENCIA ? "prioridade EMERGENCIA" : "anomalia de risco CRITICO") + ")");
        }
        if (this.participacoes.isEmpty()) {
            throw new RegraDeNegocioException(regra, "A missão precisa de pelo menos um agente designado para iniciar");
        }
        for (MissaoAgente p : this.participacoes) {
            if (!p.getAgente().podeSerDesignado()) {
                throw new RegraDeNegocioException(regra, "O agente " + p.getAgente().getCodigo()
                        + " não está disponível; situação: " + p.getAgente().getSituacao());
            }
        }
        if (!this.anomalia.podeReceberMissao(this.proposito)) {
            throw new RegraDeNegocioException("D14", "A anomalia na situação " + this.anomalia.getSituacao()
                    + " não admite missão de " + this.proposito);
        }
        this.situacao = SituacaoMissao.EM_EXECUCAO;
        this.dataInicio = agora;
        this.participacoes.forEach(p -> p.getAgente().entrarEmMissao());
        this.anomalia.iniciarAtendimento(this.proposito);
    }

    /** M9: só missões em execução recebem intervenções (§10), e só de agentes designados. */
    public Intervencao registrarIntervencao(Agente agente, String acao, String justificativa,
                                            Impacto impactoEstimado, String resultadoObservado, LocalDateTime agora) {
        exigirEmExecucao("receber intervenções");
        exigirParticipante(agente);
        Intervencao intervencao = new Intervencao(this, agente, acao, justificativa, impactoEstimado, resultadoObservado, agora);
        this.intervencoes.add(intervencao);
        return intervencao;
    }

    /** M9: a ocorrência fica no MongoDB, mas as mesmas condições valem para registrá-la. */
    public void conferirRegistroDeOcorrencia(Agente agente) {
        exigirEmExecucao("receber ocorrências");
        exigirParticipante(agente);
    }

    /** M10 (operação 6.2). */
    public Encerramento concluir(DadosEncerramento dados, LocalDateTime agora) {
        return encerrar(SituacaoMissao.CONCLUIDA, dados, agora);
    }

    /** M11 (operação 6.2). Uma falha nunca estabiliza a anomalia (§13). */
    public Encerramento registrarFalha(DadosEncerramento dados, LocalDateTime agora) {
        return encerrar(SituacaoMissao.FALHOU, dados, agora);
    }

    private Encerramento encerrar(SituacaoMissao desfecho, DadosEncerramento dados, LocalDateTime agora) {
        String regra = desfecho == SituacaoMissao.CONCLUIDA ? "M10" : "M11";
        if (this.situacao != SituacaoMissao.EM_EXECUCAO) {
            throw new RegraDeNegocioException(regra, "Só missões EM_EXECUCAO podem ser encerradas; situação atual: " + this.situacao);
        }
        Objects.requireNonNull(dados.situacaoFinalAnomalia(), "A situação final da anomalia é obrigatória");
        boolean anomaliaIrreversivel = this.anomalia.getSituacao() == SituacaoAnomalia.IRREVERSIVEL;
        Set<SituacaoAnomalia> permitidas = situacoesFinaisPermitidas(this.proposito, desfecho, anomaliaIrreversivel);
        if (!permitidas.contains(dados.situacaoFinalAnomalia())) {
            throw new RegraDeNegocioException("D35", "Uma missão de " + this.proposito + " " + desfecho
                    + " só admite como situação final da anomalia: " + permitidas);
        }

        this.encerramento = new Encerramento(this, dados, agora);
        this.situacao = desfecho;
        this.dataFim = agora;
        for (MissaoAgente p : this.participacoes) {
            if (!p.isAberta()) continue;
            p.encerrarParticipacao(agora);
            if (desfecho == SituacaoMissao.FALHOU && registrouIntervencaoSevera(p.getAgente())) {
                p.getAgente().suspenderPorFalha();   // G4 (D30)
            } else {
                p.getAgente().liberar();             // G3
            }
        }
        this.anomalia.encerrarAtendimento(dados.situacaoFinalAnomalia());
        return this.encerramento;
    }

    /** Tabela 5.2 do doc 03: situações finais da anomalia por propósito e desfecho. */
    public static Set<SituacaoAnomalia> situacoesFinaisPermitidas(PropositoMissao proposito, SituacaoMissao desfecho,
                                                                  boolean anomaliaIrreversivel) {
        if (anomaliaIrreversivel) {
            return EnumSet.of(SituacaoAnomalia.IRREVERSIVEL);
        }
        boolean concluida = desfecho == SituacaoMissao.CONCLUIDA;
        return switch (proposito) {
            case INVESTIGACAO -> concluida
                    ? EnumSet.of(SituacaoAnomalia.CONFIRMADA, SituacaoAnomalia.ESTABILIZADA, SituacaoAnomalia.IRREVERSIVEL)
                    : EnumSet.of(SituacaoAnomalia.DETECTADA, SituacaoAnomalia.CONFIRMADA, SituacaoAnomalia.IRREVERSIVEL);
            case CORRECAO -> concluida
                    ? EnumSet.of(SituacaoAnomalia.ESTABILIZADA, SituacaoAnomalia.CONFIRMADA)
                    : EnumSet.of(SituacaoAnomalia.CONFIRMADA, SituacaoAnomalia.IRREVERSIVEL);
        };
    }

    /** M7/M8 (operação 6.3): os agentes continuam DISPONIVEL, só as participações são encerradas. */
    public void cancelar(LocalDateTime agora) {
        if (!isPendente()) {
            throw new RegraDeNegocioException("M7", "Só missões PLANEJADA ou AUTORIZADA podem ser canceladas; situação atual: " + this.situacao);
        }
        this.situacao = SituacaoMissao.CANCELADA;
        this.dataFim = agora;
        this.participacoes.forEach(p -> p.encerrarParticipacao(agora));
    }

    public boolean registrouIntervencaoSevera(Agente agente) {
        return this.intervencoes.stream().anyMatch(i -> i.isSevera() && i.eDoAgente(agente));
    }

    public Optional<MissaoAgente> participacaoDe(Agente agente) {
        return this.participacoes.stream().filter(p -> p.eDoAgente(agente)).findFirst();
    }

    public boolean temParticipacaoDoAgente(Long agenteId) {
        return this.participacoes.stream().anyMatch(p -> p.eDoAgente(agenteId));
    }

    private void exigirEmExecucao(String acao) {
        if (this.situacao != SituacaoMissao.EM_EXECUCAO) {
            throw new RegraDeNegocioException("M9", "Só missões em execução podem " + acao + "; situação atual: " + this.situacao);
        }
    }

    private void exigirParticipante(Agente agente) {
        Objects.requireNonNull(agente, "O agente é obrigatório");
        if (participacaoDe(agente).isEmpty()) {
            throw new RegraDeNegocioException("M9", "O agente não está designado nesta missão");
        }
    }
}
