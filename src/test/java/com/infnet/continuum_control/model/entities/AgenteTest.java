package com.infnet.continuum_control.model.entities;

import com.infnet.continuum_control.exception.RegraDeNegocioException;
import com.infnet.continuum_control.model.enums.SituacaoAgente;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Máquina de estados do agente (doc 03, tabela 4.1): cada transição permitida e a guarda violada. */
class AgenteTest {

    private static void falhaComRegra(String regra, Executable acao) {
        RegraDeNegocioException ex = assertThrows(RegraDeNegocioException.class, acao);
        assertEquals(regra, ex.getRegra());
    }

    @Test
    @DisplayName("G1: nasce DISPONIVEL e pode ser designado")
    void nasceDisponivel() {
        Agente agente = Cenario.agente("AG-1");
        assertThat(agente.getSituacao()).isEqualTo(SituacaoAgente.DISPONIVEL);
        assertThat(agente.podeSerDesignado()).isTrue();
    }

    @Test
    @DisplayName("G2/G3: entra em missão e é liberado; fora de EM_MISSAO a liberação falha (D34)")
    void entrarEmMissaoELiberar() {
        Agente agente = Cenario.agente("AG-1");
        agente.entrarEmMissao();
        assertThat(agente.getSituacao()).isEqualTo(SituacaoAgente.EM_MISSAO);
        falhaComRegra("G2", agente::entrarEmMissao);
        agente.liberar();
        assertThat(agente.getSituacao()).isEqualTo(SituacaoAgente.DISPONIVEL);
        falhaComRegra("G3", agente::liberar);
    }

    @Test
    @DisplayName("G4: suspensão por falha só a partir de EM_MISSAO")
    void suspenderPorFalha() {
        Agente agente = Cenario.agente("AG-1");
        falhaComRegra("G4", agente::suspenderPorFalha);
        agente.entrarEmMissao();
        agente.suspenderPorFalha();
        assertThat(agente.getSituacao()).isEqualTo(SituacaoAgente.SUSPENSO);
    }

    @Test
    @DisplayName("G5/G6: suspender só de DISPONIVEL e reativar só de SUSPENSO")
    void suspenderEReativar() {
        Agente agente = Cenario.agente("AG-1");
        falhaComRegra("G6", () -> agente.reativar("motivo"));
        agente.suspender("Treinamento");
        assertThat(agente.getSituacao()).isEqualTo(SituacaoAgente.SUSPENSO);
        falhaComRegra("G5", () -> agente.suspender("de novo"));
        agente.reativar("Voltou");
        assertThat(agente.getSituacao()).isEqualTo(SituacaoAgente.DISPONIVEL);
    }

    @Test
    @DisplayName("4.2: agente em missão não muda por ação manual")
    void emMissaoNaoMudaManualmente() {
        Agente agente = Cenario.agente("AG-1");
        agente.entrarEmMissao();
        falhaComRegra("G5", () -> agente.suspender("x"));
        falhaComRegra("G7", () -> agente.inativar("x"));
    }

    @Test
    @DisplayName("G7 e P5: INATIVO é final")
    void inativoEFinal() {
        Agente agente = Cenario.agente("AG-1");
        agente.suspender("x");
        agente.inativar("Desligado");
        assertThat(agente.getSituacao()).isEqualTo(SituacaoAgente.INATIVO);
        assertThat(agente.podeSerDesignado()).isFalse();
        falhaComRegra("G6", () -> agente.reativar("x"));
        falhaComRegra("G5", () -> agente.suspender("x"));
        falhaComRegra("G2", agente::entrarEmMissao);
    }
}
