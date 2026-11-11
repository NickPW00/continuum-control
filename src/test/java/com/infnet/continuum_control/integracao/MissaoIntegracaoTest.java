package com.infnet.continuum_control.integracao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Operações compostas do doc 03 (seção 6) e os caminhos da tabela 8.1, pela API. */
class MissaoIntegracaoTest extends IntegracaoBase {

    @Test
    @DisplayName("Fluxo completo: autorização exigida, início, intervenção severa, falha e suspensão (6.1, 6.2, G4)")
    void fluxoCompleto() throws Exception {
        long anomalia = criarAnomalia("CRITICO");
        long ana = criarAgente();
        long bruno = criarAgente();
        String tokenAna = usuarioDoAgente(ana);
        long missao = criarMissao(anomalia, "CORRECAO", "NORMAL");
        designar(missao, ana);
        designar(missao, bruno);

        Resposta semAutorizacao = post("/missoes/" + missao + "/iniciar", operador, null);
        assertThat(semAutorizacao.status()).isEqualTo(409);
        assertThat(semAutorizacao.texto("regra")).isEqualTo("M5");

        assertThat(post("/missoes/" + missao + "/autorizar", supervisor, "{\"observacao\":\"Risco aceito\"}").texto("situacao"))
                .isEqualTo("AUTORIZADA");
        Resposta iniciada = post("/missoes/" + missao + "/iniciar", operador, null);
        assertThat(iniciada.texto("situacao")).isEqualTo("EM_EXECUCAO");
        assertThat(get("/agentes/" + ana, operador).texto("situacao")).isEqualTo("EM_MISSAO");
        assertThat(get("/anomalias/" + anomalia, operador).texto("situacao")).isEqualTo("EM_CORRECAO");

        assertThat(post("/missoes/" + missao + "/intervencoes", tokenAna,
                "{\"acao\":\"Desviar o mensageiro\",\"justificativa\":\"j\",\"impactoEstimado\":\"SEVERO\"}").status()).isEqualTo(201);

        Resposta estabilizarNaFalha = post("/missoes/" + missao + "/registrar-falha", operador, """
                {"resultado":"Falhou","resumo":"r","impactoObservado":"SEVERO","situacaoFinalAnomalia":"ESTABILIZADA"}""");
        assertThat(estabilizarNaFalha.status()).isEqualTo(409);
        assertThat(estabilizarNaFalha.texto("regra")).isEqualTo("D35");

        Resposta falhou = post("/missoes/" + missao + "/registrar-falha", operador, """
                {"resultado":"Falhou","resumo":"r","impactoObservado":"SEVERO","situacaoFinalAnomalia":"CONFIRMADA"}""");
        assertThat(falhou.texto("situacao")).isEqualTo("FALHOU");
        assertThat(get("/agentes/" + ana, operador).texto("situacao")).isEqualTo("SUSPENSO");
        assertThat(get("/agentes/" + bruno, operador).texto("situacao")).isEqualTo("DISPONIVEL");
        assertThat(get("/anomalias/" + anomalia, operador).texto("situacao")).isEqualTo("CONFIRMADA");

        assertThat(tipos(get("/anomalias/" + anomalia + "/historico", operador).corpo())).containsExactly(
                "MISSAO_CRIADA", "AGENTE_DESIGNADO", "AGENTE_DESIGNADO", "AUTORIZACAO", "MISSAO_INICIADA",
                "MUDANCA_SITUACAO", "INTERVENCAO", "ENCERRAMENTO", "MUDANCA_SITUACAO");

        JsonNode historicoAna = get("/agentes/" + ana + "/historico", supervisor).corpo().get("content");
        assertThat(historicoAna).hasSize(3);
        assertThat(historicoAna.get(2).get("situacaoNova").asString()).isEqualTo("SUSPENSO");
    }

    @Test
    @DisplayName("Tabela 8.1 (D23): duas missões na mesma anomalia não executam ao mesmo tempo")
    void umaMissaoEmExecucaoPorAnomalia() throws Exception {
        long anomalia = criarAnomalia("BAIXO");
        long primeira = criarMissao(anomalia, "INVESTIGACAO", "NORMAL");
        long segunda = criarMissao(anomalia, "INVESTIGACAO", "NORMAL");
        long agenteDaSegunda = criarAgente();
        designar(primeira, criarAgente());
        designar(segunda, agenteDaSegunda);

        assertThat(post("/missoes/" + primeira + "/iniciar", operador, null).status()).isEqualTo(200);
        Resposta r = post("/missoes/" + segunda + "/iniciar", operador, null);
        assertThat(r.status()).isEqualTo(409);
        assertThat(r.texto("regra")).isEqualTo("D23");
        assertThat(get("/agentes/" + agenteDaSegunda, operador).texto("situacao")).isEqualTo("DISPONIVEL");
    }

    @Test
    @DisplayName("Tabela 8.1 (D24): declarar irreversível cancela as correções pendentes e mantém as investigações")
    void irreversivelCancelaCorrecoes() throws Exception {
        long anomalia = criarAnomalia("ALTO");
        long correcao = criarMissao(anomalia, "CORRECAO", "NORMAL");
        long investigacao = criarMissao(anomalia, "INVESTIGACAO", "NORMAL");

        Resposta r = post("/anomalias/" + anomalia + "/declarar-irreversivel", supervisor, "{\"justificativa\":\"Perdida\"}");
        assertThat(r.texto("situacao")).isEqualTo("IRREVERSIVEL");
        assertThat(get("/missoes/" + correcao, operador).texto("situacao")).isEqualTo("CANCELADA");
        assertThat(get("/missoes/" + investigacao, operador).texto("situacao")).isEqualTo("PLANEJADA");

        Resposta novaCorrecao = post("/missoes", operador, """
                {"codigo":"%s","objetivo":"o","proposito":"CORRECAO","anomaliaId":%d,"destinoTemporal":"2000-01-01","prioridade":"ALTA"}"""
                .formatted(unico("MI"), anomalia));
        assertThat(novaCorrecao.status()).isEqualTo(409);
        assertThat(novaCorrecao.texto("regra")).isEqualTo("M1");
    }

    @Test
    @DisplayName("Tabela 8.1 (D25): agente suspenso depois da designação pode ser removido, e a missão inicia com os outros")
    void removerAgenteSuspenso() throws Exception {
        long missao = criarMissao(criarAnomalia("BAIXO"), "INVESTIGACAO", "NORMAL");
        long a0 = criarAgente();
        long a1 = criarAgente();
        designar(missao, a0);
        designar(missao, a1);
        post("/agentes/" + a0 + "/suspender", admin, "{\"motivo\":\"Treinamento\"}");

        assertThat(post("/missoes/" + missao + "/iniciar", operador, null).status()).isEqualTo(409);
        assertThat(chamar(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .delete("/missoes/" + missao + "/designacoes/" + a0), operador, null).status()).isEqualTo(204);
        assertThat(post("/missoes/" + missao + "/iniciar", operador, null).status()).isEqualTo(200);
    }

    @Test
    @DisplayName("D50: data antes de Cristo vai e volta pela API e pelo PostgreSQL")
    void dataAntesDeCristo() throws Exception {
        long evento = criarEvento("-0752-04-21");
        assertThat(get("/eventos/" + evento, operador).texto("data")).isEqualTo("-0752-04-21");
        JsonNode antigos = get("/eventos?dataAte=0001-01-01&size=100", operador).corpo().get("content");
        List<String> datas = new ArrayList<>();
        antigos.forEach(e -> datas.add(e.get("data").asString()));
        assertThat(datas).contains("-0752-04-21");
    }

    @Test
    @DisplayName("Erros (§18): 400 com campos, 404, 409 de constraint e de regra, página limitada a 100")
    void erros() throws Exception {
        Resposta invalido = post("/eventos", operador, "{\"acontecimento\":\"x\"}");
        assertThat(invalido.status()).isEqualTo(400);
        assertThat(invalido.corpo().get("campos").has("data")).isTrue();
        assertThat(invalido.bruta().getContentType()).contains("application/problem+json");

        assertThat(get("/missoes/999999", operador).status()).isEqualTo(404);
        assertThat(post("/anomalias", operador, "{\"eventoId\":999999,\"divergenciaObservada\":\"x\",\"risco\":\"BAIXO\"}").status())
                .isEqualTo(404);

        long anomalia = criarAnomalia("BAIXO");
        long evento = get("/anomalias/" + anomalia, operador).corpo().get("eventoId").asLong();
        Resposta removerEvento = chamar(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .delete("/eventos/" + evento), supervisor, null);
        assertThat(removerEvento.status()).isEqualTo(409);
        assertThat(removerEvento.texto("constraint")).isEqualTo("fk_anomalia_evento");

        assertThat(get("/agentes?size=500", operador).corpo().get("page").get("size").asInt()).isEqualTo(100);
    }

    private static List<String> tipos(JsonNode pagina) {
        List<String> tipos = new ArrayList<>();
        pagina.get("content").forEach(r -> tipos.add(r.get("tipo").asString()));
        return tipos;
    }
}
