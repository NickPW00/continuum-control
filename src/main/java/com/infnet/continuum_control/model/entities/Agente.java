package com.infnet.continuum_control.model.entities;

import com.infnet.continuum_control.model.enums.SituacaoAgente;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Agente {
    @Id
    private Long id;
    private String nome;
    private SituacaoAgente situacao;
    private String especialidade;

    public boolean podeSerDesignado(){
        return this.situacao == SituacaoAgente.DISPONIVEL;
    }

    public void entrarEmMissao(){
        this.situacao = SituacaoAgente.EM_MISSAO;
    }

    public void liberar(){
        if (this.situacao == SituacaoAgente.EM_MISSAO){this.situacao = SituacaoAgente.DISPONIVEL;}
    }

    public void suspender(){
        if (this.situacao == SituacaoAgente.DISPONIVEL){this.situacao = SituacaoAgente.SUSPENSO;}
    }

    public void reativar(){
        if (this.situacao == SituacaoAgente.SUSPENSO){this.situacao = SituacaoAgente.DISPONIVEL;}
    }

    public void inativar(){
        if (this.situacao == SituacaoAgente.SUSPENSO || this.situacao == SituacaoAgente.DISPONIVEL){
            this.situacao = SituacaoAgente.INATIVO;
        }
    }
}
