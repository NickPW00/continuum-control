package com.infnet.continuum_control.integracao;

import com.infnet.continuum_control.service.indicadores.IndicadoresService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;

/**
 * §9, §20 e §21: iniciar a missão muda missão, agentes, anomalia e histórico como uma unidade.
 * Aqui o último passo da operação 6.1 (invalidar o cache, depois de todas as gravações já terem
 * ido ao banco com flush) falha de propósito, e nada do que veio antes pode ficar gravado.
 */
class AtomicidadeIntegracaoTest extends IntegracaoBase {

    @MockitoSpyBean
    private IndicadoresService indicadores;

    @Test
    @DisplayName("Falha no fim de iniciar a missão desfaz tudo: missão, agentes, anomalia e histórico continuam como estavam")
    void iniciarETudoOuNada() throws Exception {
        long anomalia = criarAnomalia("BAIXO");
        long agente = criarAgente();
        long missao = criarMissao(anomalia, "CORRECAO", "NORMAL");
        designar(missao, agente);
        int registrosAntes = get("/anomalias/" + anomalia + "/historico", operador).corpo().get("content").size();

        doThrow(new IllegalStateException("falha simulada no último passo da operação")).when(indicadores).invalidar();

        assertThat(post("/missoes/" + missao + "/iniciar", operador, null).status()).isEqualTo(500);

        assertThat(get("/missoes/" + missao, operador).texto("situacao")).isEqualTo("PLANEJADA");
        assertThat(get("/missoes/" + missao, operador).corpo().get("dataInicio").isNull()).isTrue();
        assertThat(get("/agentes/" + agente, operador).texto("situacao")).isEqualTo("DISPONIVEL");
        assertThat(get("/anomalias/" + anomalia, operador).texto("situacao")).isEqualTo("DETECTADA");
        assertThat(get("/agentes/" + agente + "/historico", supervisor).corpo().get("content")).hasSize(1);
        assertThat(get("/anomalias/" + anomalia + "/historico", operador).corpo().get("content")).hasSize(registrosAntes);
    }
}
