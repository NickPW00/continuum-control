package com.infnet.continuum_control.model.entities;

import com.infnet.continuum_control.exception.RegraDeNegocioException;
import com.infnet.continuum_control.model.enums.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static com.infnet.continuum_control.model.entities.Cenario.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Máquina de estados da missão (doc 03, tabela 3.1): transição permitida e guarda violada. */
class MissaoTest {

    private static void falhaComRegra(String regra, Executable acao) {
        RegraDeNegocioException ex = assertThrows(RegraDeNegocioException.class, acao);
        assertEquals(regra, ex.getRegra());
    }

    @Test
    @DisplayName("M1: nasce PLANEJADA; correção sobre anomalia IRREVERSIVEL é recusada")
    void criar() {
        Missao missao = missao(anomalia(NivelRisco.ALTO), PropositoMissao.CORRECAO, Prioridade.NORMAL);
        assertThat(missao.getSituacao()).isEqualTo(SituacaoMissao.PLANEJADA);

        Anomalia irreversivel = anomalia(NivelRisco.ALTO);
        irreversivel.declararIrreversivel("x");
        falhaComRegra("M1", () -> missao(irreversivel, PropositoMissao.CORRECAO, Prioridade.NORMAL));
        assertThat(missao(irreversivel, PropositoMissao.INVESTIGACAO, Prioridade.NORMAL).getSituacao())
                .isEqualTo(SituacaoMissao.PLANEJADA);
    }

    @Test
    @DisplayName("M2: só agente DISPONIVEL, uma vez por missão, e só antes do início")
    void designar() {
        Missao missao = missao(anomalia(NivelRisco.ALTO), PropositoMissao.CORRECAO, Prioridade.NORMAL);
        Agente ana = agente("AG-1");
        missao.designar(ana, AGORA);
        assertThat(missao.getParticipacoes()).hasSize(1);
        falhaComRegra("M2", () -> missao.designar(ana, AGORA));

        Agente suspenso = agente("AG-2");
        suspenso.suspender("x");
        falhaComRegra("M2", () -> missao.designar(suspenso, AGORA));

        missao.iniciar(AGORA);
        falhaComRegra("M2", () -> missao.designar(agente("AG-3"), AGORA));
    }

    @Test
    @DisplayName("M3 (D25): remover designação só antes do início")
    void removerDesignacao() {
        Missao missao = missao(anomalia(NivelRisco.ALTO), PropositoMissao.CORRECAO, Prioridade.NORMAL);
        Agente ana = agente("AG-1");
        Agente bruno = agente("AG-2");
        missao.designar(ana, AGORA);
        missao.designar(bruno, AGORA);
        missao.removerDesignacao(bruno);
        assertThat(missao.getParticipacoes()).hasSize(1);
        falhaComRegra("M3", () -> missao.removerDesignacao(bruno));

        missao.iniciar(AGORA);
        falhaComRegra("M3", () -> missao.removerDesignacao(ana));
    }

    @Test
    @DisplayName("M4: só a partir de PLANEJADA e só por SUPERVISOR ou ADMINISTRADOR")
    void autorizar() {
        Missao missao = missao(anomalia(NivelRisco.CRITICO), PropositoMissao.CORRECAO, Prioridade.NORMAL);
        falhaComRegra("M4", () -> missao.autorizar(usuario(Perfil.OPERADOR), "ok", AGORA));
        missao.autorizar(usuario(Perfil.SUPERVISOR), "Risco aceito", AGORA);
        assertThat(missao.getSituacao()).isEqualTo(SituacaoMissao.AUTORIZADA);
        assertThat(missao.getAutorizacao().getObservacao()).isEqualTo("Risco aceito");
        falhaComRegra("M4", () -> missao.autorizar(usuario(Perfil.ADMINISTRADOR), "de novo", AGORA));
    }

    @Test
    @DisplayName("M5 e P3: missão que exige autorização não vai de PLANEJADA para EM_EXECUCAO (§8)")
    void exigeAutorizacao() {
        Missao emergencia = missao(anomalia(NivelRisco.BAIXO), PropositoMissao.CORRECAO, Prioridade.EMERGENCIA);
        emergencia.designar(agente("AG-1"), AGORA);
        assertThat(emergencia.exigeAutorizacao()).isTrue();
        falhaComRegra("M5", () -> emergencia.iniciar(AGORA));

        Missao critica = missao(anomalia(NivelRisco.CRITICO), PropositoMissao.CORRECAO, Prioridade.BAIXA);
        critica.designar(agente("AG-2"), AGORA);
        falhaComRegra("M5", () -> critica.iniciar(AGORA));

        critica.autorizar(usuario(Perfil.SUPERVISOR), "ok", AGORA);
        critica.iniciar(AGORA);   // M6
        assertThat(critica.getSituacao()).isEqualTo(SituacaoMissao.EM_EXECUCAO);
    }

    @Test
    @DisplayName("D28: a exigência de autorização é avaliada ao iniciar, não na criação")
    void autorizacaoAvaliadaAoIniciar() {
        Anomalia anomalia = anomalia(NivelRisco.BAIXO);
        Missao missao = missao(anomalia, PropositoMissao.CORRECAO, Prioridade.NORMAL);
        missao.designar(agente("AG-1"), AGORA);
        anomalia.alterarRisco(NivelRisco.CRITICO);
        falhaComRegra("M5", () -> missao.iniciar(AGORA));
    }

    @Test
    @DisplayName("M5: sem agente, ou com agente que deixou de estar disponível, não inicia (§7, D11)")
    void podeIniciar() {
        Missao semAgente = missao(anomalia(NivelRisco.ALTO), PropositoMissao.CORRECAO, Prioridade.NORMAL);
        falhaComRegra("M5", () -> semAgente.iniciar(AGORA));

        Missao missao = missao(anomalia(NivelRisco.ALTO), PropositoMissao.CORRECAO, Prioridade.NORMAL);
        Agente ana = agente("AG-1");
        missao.designar(ana, AGORA);
        ana.suspender("Suspensa depois da designação");
        falhaComRegra("M5", () -> missao.iniciar(AGORA));
    }

    @Test
    @DisplayName("6.1: iniciar muda missão, agentes e anomalia juntos")
    void iniciar() {
        Anomalia anomalia = anomalia(NivelRisco.ALTO);
        Agente ana = agente("AG-1");
        Agente bruno = agente("AG-2");
        Missao missao = emExecucao(anomalia, PropositoMissao.CORRECAO, ana, bruno);
        assertThat(missao.getSituacao()).isEqualTo(SituacaoMissao.EM_EXECUCAO);
        assertThat(missao.getDataInicio()).isEqualTo(AGORA);
        assertThat(ana.getSituacao()).isEqualTo(SituacaoAgente.EM_MISSAO);
        assertThat(bruno.getSituacao()).isEqualTo(SituacaoAgente.EM_MISSAO);
        assertThat(anomalia.getSituacao()).isEqualTo(SituacaoAnomalia.EM_CORRECAO);
        falhaComRegra("M6", () -> missao.iniciar(AGORA));
    }

    @Test
    @DisplayName("M9: intervenção só em execução e só de agente designado")
    void intervencao() {
        Agente ana = agente("AG-1");
        Missao planejada = missao(anomalia(NivelRisco.ALTO), PropositoMissao.CORRECAO, Prioridade.NORMAL);
        planejada.designar(ana, AGORA);
        falhaComRegra("M9", () -> planejada.registrarIntervencao(ana, "a", "j", Impacto.MINIMO, null, AGORA));

        planejada.iniciar(AGORA);
        Intervencao intervencao = planejada.registrarIntervencao(ana, "a", "j", Impacto.SEVERO, null, AGORA);
        assertThat(intervencao.isSevera()).isTrue();
        falhaComRegra("M9", () -> planejada.registrarIntervencao(agente("AG-9"), "a", "j", Impacto.MINIMO, null, AGORA));
    }

    @Test
    @DisplayName("M10 (6.2): concluir libera os agentes, fecha as participações e leva a anomalia à situação final")
    void concluir() {
        Anomalia anomalia = anomalia(NivelRisco.ALTO);
        Agente ana = agente("AG-1");
        Missao missao = emExecucao(anomalia, PropositoMissao.CORRECAO, ana);
        missao.concluir(encerramento(SituacaoAnomalia.ESTABILIZADA), AGORA.plusHours(1));

        assertThat(missao.getSituacao()).isEqualTo(SituacaoMissao.CONCLUIDA);
        assertThat(missao.getEncerramento()).isNotNull();
        assertThat(missao.getDataFim()).isEqualTo(AGORA.plusHours(1));
        assertThat(ana.getSituacao()).isEqualTo(SituacaoAgente.DISPONIVEL);
        assertThat(missao.getParticipacoes()).allMatch(p -> !p.isAberta());
        assertThat(anomalia.getSituacao()).isEqualTo(SituacaoAnomalia.ESTABILIZADA);
        falhaComRegra("M10", () -> missao.concluir(encerramento(SituacaoAnomalia.ESTABILIZADA), AGORA));   // P4
    }

    @Test
    @DisplayName("M11 e G4 (D30): na falha, só quem fez intervenção SEVERO fica SUSPENSO")
    void falhaSuspendeQuemFezIntervencaoSevera() {
        Anomalia anomalia = anomalia(NivelRisco.ALTO);
        Agente ana = agente("AG-1");
        Agente bruno = agente("AG-2");
        Missao missao = emExecucao(anomalia, PropositoMissao.CORRECAO, ana, bruno);
        missao.registrarIntervencao(ana, "Desviar o mensageiro", "j", Impacto.SEVERO, null, AGORA);
        missao.registrarIntervencao(bruno, "Observar", "j", Impacto.MINIMO, null, AGORA);

        missao.registrarFalha(encerramento(SituacaoAnomalia.CONFIRMADA), AGORA);

        assertThat(missao.getSituacao()).isEqualTo(SituacaoMissao.FALHOU);
        assertThat(ana.getSituacao()).isEqualTo(SituacaoAgente.SUSPENSO);
        assertThat(bruno.getSituacao()).isEqualTo(SituacaoAgente.DISPONIVEL);
        assertThat(anomalia.getSituacao()).isEqualTo(SituacaoAnomalia.CONFIRMADA);
    }

    @Test
    @DisplayName("P2: uma falha nunca estabiliza a anomalia (§13)")
    void falhaNaoEstabiliza() {
        Missao missao = emExecucao(anomalia(NivelRisco.ALTO), PropositoMissao.CORRECAO, agente("AG-1"));
        falhaComRegra("D35", () -> missao.registrarFalha(encerramento(SituacaoAnomalia.ESTABILIZADA), AGORA));
        assertThat(missao.getSituacao()).isEqualTo(SituacaoMissao.EM_EXECUCAO);
    }

    @Test
    @DisplayName("M7 (6.3): cancelar só pendente; os agentes continuam DISPONIVEL")
    void cancelar() {
        Agente ana = agente("AG-1");
        Missao missao = missao(anomalia(NivelRisco.ALTO), PropositoMissao.CORRECAO, Prioridade.NORMAL);
        missao.designar(ana, AGORA);
        missao.cancelar(AGORA);
        assertThat(missao.getSituacao()).isEqualTo(SituacaoMissao.CANCELADA);
        assertThat(ana.getSituacao()).isEqualTo(SituacaoAgente.DISPONIVEL);
        assertThat(missao.getParticipacoes()).allMatch(p -> !p.isAberta());

        Missao emExecucao = emExecucao(anomalia(NivelRisco.ALTO), PropositoMissao.CORRECAO, agente("AG-2"));
        falhaComRegra("M7", () -> emExecucao.cancelar(AGORA));
    }

    @Test
    @DisplayName("D28: os dados só mudam em PLANEJADA")
    void atualizarDados() {
        Missao missao = missao(anomalia(NivelRisco.CRITICO), PropositoMissao.CORRECAO, Prioridade.NORMAL);
        missao.atualizarDados("Novo objetivo", null, Prioridade.ALTA);
        assertThat(missao.getObjetivo()).isEqualTo("Novo objetivo");
        missao.autorizar(usuario(Perfil.SUPERVISOR), "ok", AGORA);
        falhaComRegra("D28", () -> missao.atualizarDados("Outro", null, null));
    }
}
