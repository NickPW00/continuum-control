package com.infnet.continuum_control.model.entities;

import com.infnet.continuum_control.model.enums.SituacaoAgente;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "agente")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Agente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", nullable = false, length = 20, updatable = false)
    private String codigo;

    @Column(name = "nome", nullable = false, length = 120)
    private String nome;

    @Column(name = "especialidade", nullable = false, length = 80)
    private String especialidade;

    @Enumerated(EnumType.STRING)
    @Column(name = "situacao", nullable = false, length = 10)
    private SituacaoAgente situacao;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public Agente(String codigo, String nome, String especialidade) {
        this.codigo = codigo;
        this.nome = nome;
        this.especialidade = especialidade;
        this.situacao = SituacaoAgente.DISPONIVEL;
    }

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
