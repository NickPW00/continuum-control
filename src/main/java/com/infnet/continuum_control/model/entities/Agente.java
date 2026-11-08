package com.infnet.continuum_control.model.entities;

import com.infnet.continuum_control.exception.RegraDeNegocioException;
import com.infnet.continuum_control.model.enums.SituacaoAgente;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

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
        this.codigo = Objects.requireNonNull(codigo, "O código é obrigatório");
        this.nome = Objects.requireNonNull(nome, "O nome é obrigatório");
        this.especialidade = Objects.requireNonNull(especialidade, "A especialidade é obrigatória");
        this.situacao = SituacaoAgente.DISPONIVEL;
    }

    public boolean podeSerDesignado(){
        return this.situacao == SituacaoAgente.DISPONIVEL;
    }

    public void entrarEmMissao(){
        if(this.situacao == SituacaoAgente.DISPONIVEL){
            this.situacao = SituacaoAgente.EM_MISSAO;
        }
        else throw new RegraDeNegocioException("O agente não pode entrar em missão; situação atual: " + this.situacao);
    }

    public void liberar(){
        if (this.situacao == SituacaoAgente.EM_MISSAO){
            this.situacao = SituacaoAgente.DISPONIVEL;
        }
        else throw new RegraDeNegocioException("O agente não pode ser liberado; situação atual: " + this.situacao);
    }

    public void suspender(){
        if (this.situacao == SituacaoAgente.DISPONIVEL){
            this.situacao = SituacaoAgente.SUSPENSO;
        }
        else throw new RegraDeNegocioException("O agente não pode ser suspenso; situação atual: " + this.situacao);
    }

    void suspenderPorFalha(){
        if (this.situacao == SituacaoAgente.EM_MISSAO){
            this.situacao = SituacaoAgente.SUSPENSO;
        }
        else throw new RegraDeNegocioException("O agente não pode ser suspenso por falha de missão; situação atual: " + this.situacao);
    }

    public void reativar(){
        if (this.situacao == SituacaoAgente.SUSPENSO){
            this.situacao = SituacaoAgente.DISPONIVEL;
        }
        else throw new RegraDeNegocioException("O agente não pode ser reativado; situação atual: " + this.situacao);
    }

    public void inativar(){
        if (this.situacao == SituacaoAgente.SUSPENSO
                || this.situacao == SituacaoAgente.DISPONIVEL){
            this.situacao = SituacaoAgente.INATIVO;
        }
        else throw new RegraDeNegocioException("O agente não pode ser inativado; situação atual: " + this.situacao);
    }
}
