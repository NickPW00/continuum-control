package com.infnet.continuum_control.model.entities;

import com.infnet.continuum_control.model.enums.Prioridade;
import com.infnet.continuum_control.model.enums.PropositoMissao;
import com.infnet.continuum_control.model.enums.SituacaoMissao;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@NoArgsConstructor
public class Missao {
    @Id
    private long id;
    private String codigo;
    private String objetivo;
    private PropositoMissao proposito;
    private Anomalia anomalia;
    private LocalDate destinoTemporal;
    private Prioridade prioridade;
    private SituacaoMissao situacao;
    private List<MissaoAgente> participacoes;
    private LocalDateTime dataInicio;
    private LocalDateTime dataFim;

    public void designar(){}
    public void removerDesignar(Agente agente){}
    public boolean exigeAutorizacao(){return true;}
    public Autorizacao autorizar(Usuario responsavel){return null;}
    public void iniciar(){}
    public void concluir(Encerramento e){}
    public void registrarFalha(Encerramento e){}
    public void cancelar(){}
    public void registrarIntervencao(Intervencao i){}
    public void registrarOcorrencia(Ocorrencia o){}
}
