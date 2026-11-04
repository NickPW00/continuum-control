package com.infnet.continuum_control.model.entities;

import com.infnet.continuum_control.model.enums.*;

import java.time.LocalDateTime;

public class Anomalia {
    private long id;
    private EventoHistorico evento;
    private String divergenciaObservada;
    private LocalDateTime dataHoraDetectada;
    private NivelRisco risco;
    private SituacaoAnomalia situacao;
    private OrigemAnomalia origem;
    private String observacoes;

    public boolean podeReceberMissao(PropositoMissao p){
        boolean isAnomaliaAtiva = this.isAtiva();
        boolean isInvestigacao = this.situacao == SituacaoAnomalia.IRREVERSIVEL && p == PropositoMissao.INVESTIGACAO;

        if (isAnomaliaAtiva) return true;
        return isInvestigacao;
    }

    public void alterarRisco(NivelRisco novo) {
        this.risco = novo;
    }

    public void declararIrreversivel(String justificativa){
        boolean isIrreversivel = this.situacao == SituacaoAnomalia.DETECTADA || this.situacao == SituacaoAnomalia.COFIRMADA;
        if (isIrreversivel){
            this.situacao = SituacaoAnomalia.IRREVERSIVEL;
            this.observacoes = justificativa;
        }
    }

    public boolean isAtiva(){
        return this.situacao != SituacaoAnomalia.IRREVERSIVEL && this.situacao != SituacaoAnomalia.ESTABILIZADA;
    }

    public boolean isCritica(){
        return this.risco == NivelRisco.CRITICO;
    }

    private void iniciarAtendimento(PropositoMissao p) {
        if (p == PropositoMissao.CORRECAO) {
            this.situacao = SituacaoAnomalia.EM_CORRECAO;
        }
        else this.situacao = SituacaoAnomalia.EM_ANALISE;
    }

    private void encerrarAtendimento(){}
}
