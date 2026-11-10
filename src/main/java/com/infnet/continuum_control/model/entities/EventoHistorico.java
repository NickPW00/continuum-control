package com.infnet.continuum_control.model.entities;

import com.infnet.continuum_control.model.enums.Importancia;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "evento_historico")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventoHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "acontecimento", nullable = false, length = 200)
    private String acontecimento;

    @Column(name = "descricao", nullable = false, columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "data", nullable = false)
    private LocalDate data;

    @Column(name = "localizacao", nullable = false, length = 150)
    private String localizacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "importancia", nullable = false, length = 10)
    private Importancia importancia;

    public EventoHistorico(String acontecimento, String descricao, LocalDate data,
                           String localizacao, Importancia importancia) {
        this.acontecimento = Objects.requireNonNull(acontecimento, "O acontecimento é obrigatório");
        this.descricao = Objects.requireNonNull(descricao, "A descrição é obrigatória");
        this.data = Objects.requireNonNull(data, "A data é obrigatória");
        this.localizacao = Objects.requireNonNull(localizacao, "A localização é obrigatória");
        this.importancia = Objects.requireNonNull(importancia, "A importância é obrigatória");
    }

    public boolean isCritico(){
        return this.importancia == Importancia.CRITICA;
    }

    public void atualizar(String acontecimento, String descricao, LocalDate data,
                          String localizacao, Importancia importancia) {
        if (acontecimento != null) this.acontecimento = acontecimento;
        if (descricao != null) this.descricao = descricao;
        if (data != null) this.data = data;
        if (localizacao != null) this.localizacao = localizacao;
        if (importancia != null) this.importancia = importancia;
    }
}
