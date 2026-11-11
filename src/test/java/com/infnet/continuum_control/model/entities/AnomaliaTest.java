package com.infnet.continuum_control.model.entities;

import com.infnet.continuum_control.exception.RegraDeNegocioException;
import com.infnet.continuum_control.model.enums.NivelRisco;
import com.infnet.continuum_control.model.enums.PropositoMissao;
import com.infnet.continuum_control.model.enums.SituacaoAnomalia;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Máquina de estados da anomalia (doc 03, tabela 5.1). */
class AnomaliaTest {

    @Test
    @DisplayName("N1: nasce DETECTADA; sem data de detecção vale agora; no futuro é recusada")
    void criacao() {
        Anomalia anomalia = Cenario.anomalia(NivelRisco.ALTO);
        assertThat(anomalia.getSituacao()).isEqualTo(SituacaoAnomalia.DETECTADA);
        assertThat(anomalia.getDataHoraDeteccao()).isEqualTo(Cenario.AGORA);
        assertThat(anomalia.isAtiva()).isTrue();

        var passado = Cenario.AGORA.minusDays(3);
        assertThat(new Anomalia(Cenario.evento(), "x", NivelRisco.BAIXO, null, null, passado, Cenario.AGORA)
                .getDataHoraDeteccao()).isEqualTo(passado);

        assertThatThrownBy(() -> new Anomalia(Cenario.evento(), "x", NivelRisco.BAIXO, null, null,
                Cenario.AGORA.plusMinutes(1), Cenario.AGORA))
                .isInstanceOf(RegraDeNegocioException.class).extracting("regra").isEqualTo("N1");
    }

    @Test
    @DisplayName("5.3: crítica é ativa e de risco CRITICO")
    void critica() {
        assertThat(Cenario.anomalia(NivelRisco.CRITICO).isCritica()).isTrue();
        assertThat(Cenario.anomalia(NivelRisco.ALTO).isCritica()).isFalse();
    }

    @Test
    @DisplayName("N7: altera o risco de uma anomalia ativa; risco igual ou anomalia final é recusado")
    void alterarRisco() {
        Anomalia anomalia = Cenario.anomalia(NivelRisco.BAIXO);
        anomalia.alterarRisco(NivelRisco.CRITICO);
        assertThat(anomalia.getRisco()).isEqualTo(NivelRisco.CRITICO);
        assertThatThrownBy(() -> anomalia.alterarRisco(NivelRisco.CRITICO)).extracting("regra").isEqualTo("N7");

        anomalia.declararIrreversivel("Perdida");
        assertThatThrownBy(() -> anomalia.alterarRisco(NivelRisco.BAIXO)).extracting("regra").isEqualTo("N7");
    }

    @Test
    @DisplayName("N6 e D33: só de DETECTADA/CONFIRMADA; a justificativa não sobrescreve as observações")
    void declararIrreversivel() {
        Anomalia anomalia = Cenario.anomalia(NivelRisco.ALTO);
        anomalia.declararIrreversivel("A divergência se espalhou");
        assertThat(anomalia.getSituacao()).isEqualTo(SituacaoAnomalia.IRREVERSIVEL);
        assertThat(anomalia.getObservacoes()).isEqualTo("Anotações");
        assertThat(anomalia.isAtiva()).isFalse();

        Anomalia emAnalise = Cenario.anomalia(NivelRisco.ALTO);
        Cenario.emExecucao(emAnalise, PropositoMissao.INVESTIGACAO, Cenario.agente("AG-1"));
        assertThatThrownBy(() -> emAnalise.declararIrreversivel("x")).extracting("regra").isEqualTo("N6");
    }

    @Test
    @DisplayName("5.3: anomalia IRREVERSIVEL aceita investigação, nunca correção (§5)")
    void podeReceberMissao() {
        Anomalia anomalia = Cenario.anomalia(NivelRisco.ALTO);
        assertThat(anomalia.podeReceberMissao(PropositoMissao.CORRECAO)).isTrue();
        anomalia.declararIrreversivel("x");
        assertThat(anomalia.podeReceberMissao(PropositoMissao.CORRECAO)).isFalse();
        assertThat(anomalia.podeReceberMissao(PropositoMissao.INVESTIGACAO)).isTrue();
    }

    @Test
    @DisplayName("N2/N3: o início da missão leva a EM_ANALISE ou EM_CORRECAO conforme o propósito")
    void inicioDeAtendimento() {
        Anomalia investigada = Cenario.anomalia(NivelRisco.ALTO);
        Cenario.emExecucao(investigada, PropositoMissao.INVESTIGACAO, Cenario.agente("AG-1"));
        assertThat(investigada.getSituacao()).isEqualTo(SituacaoAnomalia.EM_ANALISE);

        Anomalia corrigida = Cenario.anomalia(NivelRisco.ALTO);
        Cenario.emExecucao(corrigida, PropositoMissao.CORRECAO, Cenario.agente("AG-2"));
        assertThat(corrigida.getSituacao()).isEqualTo(SituacaoAnomalia.EM_CORRECAO);
    }

    @Test
    @DisplayName("D26: investigação em anomalia IRREVERSIVEL não muda a situação")
    void investigacaoEmIrreversivel() {
        Anomalia anomalia = Cenario.anomalia(NivelRisco.ALTO);
        anomalia.declararIrreversivel("x");
        Missao missao = Cenario.emExecucao(anomalia, PropositoMissao.INVESTIGACAO, Cenario.agente("AG-1"));
        assertThat(anomalia.getSituacao()).isEqualTo(SituacaoAnomalia.IRREVERSIVEL);
        missao.concluir(Cenario.encerramento(SituacaoAnomalia.IRREVERSIVEL), Cenario.AGORA);
        assertThat(anomalia.getSituacao()).isEqualTo(SituacaoAnomalia.IRREVERSIVEL);
    }
}
