package com.infnet.continuum_control.model.entities;

import com.infnet.continuum_control.model.enums.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Montagem de entidades para os testes de unidade, sem banco. */
final class Cenario {

    static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 11, 12, 0);

    private Cenario() {
    }

    static EventoHistorico evento() {
        return new EventoHistorico("Fundação de Roma", "Rômulo funda a cidade", LocalDate.of(-752, 4, 21),
                "Roma", Importancia.CRITICA);
    }

    static Anomalia anomalia(NivelRisco risco) {
        return new Anomalia(evento(), "Data divergente", risco, null, "Anotações", null, AGORA);
    }

    static Agente agente(String codigo) {
        return new Agente(codigo, "Agente " + codigo, "Roma Antiga");
    }

    static Missao missao(Anomalia anomalia, PropositoMissao proposito, Prioridade prioridade) {
        return new Missao("MI-" + System.nanoTime() % 100000, "Objetivo", proposito, anomalia,
                LocalDate.of(-752, 4, 20), prioridade);
    }

    /** Missão já EM_EXECUCAO, com os agentes informados designados. */
    static Missao emExecucao(Anomalia anomalia, PropositoMissao proposito, Agente... agentes) {
        Missao missao = missao(anomalia, proposito, Prioridade.NORMAL);
        for (Agente agente : agentes) {
            missao.designar(agente, AGORA);
        }
        missao.iniciar(AGORA);
        return missao;
    }

    static Usuario usuario(Perfil perfil) {
        return new Usuario(perfil.name().toLowerCase(), "hash", perfil, null);
    }

    static DadosEncerramento encerramento(SituacaoAnomalia situacaoFinal) {
        return new DadosEncerramento("Resultado", "Resumo", Impacto.MINIMO, situacaoFinal, null);
    }
}
