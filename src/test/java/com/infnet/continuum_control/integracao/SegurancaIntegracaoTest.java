package com.infnet.continuum_control.integracao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Cenários mínimos de segurança do doc 06, seção 10. */
class SegurancaIntegracaoTest extends IntegracaoBase {

    @Test
    @DisplayName("S1: rota protegida sem token ou com token adulterado responde 401 com WWW-Authenticate")
    void semToken() throws Exception {
        Resposta semToken = get("/missoes", null);
        assertThat(semToken.status()).isEqualTo(401);
        assertThat(semToken.bruta().getHeader("WWW-Authenticate")).startsWith("Bearer");

        String adulterado = operador.substring(0, operador.length() - 4) + "abcd";
        assertThat(get("/missoes", adulterado).status()).isEqualTo(401);

        assertThat(get("/actuator/health", null).status()).isEqualTo(200);
    }

    @Test
    @DisplayName("S2: senha errada e usuário desativado recebem a mesma resposta")
    void loginNaoRevelaOMotivo() throws Exception {
        Resposta senhaErrada = post("/auth/login", null, "{\"login\":\"operador.teste\",\"senha\":\"errada\"}");
        Resposta inexistente = post("/auth/login", null, "{\"login\":\"ninguem\",\"senha\":\"x\"}");
        assertThat(senhaErrada.status()).isEqualTo(401);
        assertThat(inexistente.status()).isEqualTo(401);
        assertThat(senhaErrada.texto("detail")).isEqualTo(inexistente.texto("detail")).isEqualTo("Login ou senha inválidos");
    }

    @Test
    @DisplayName("S7: usuário desativado perde o acesso na requisição seguinte, mesmo com token válido")
    void desativadoPerdeOAcesso() throws Exception {
        long agente = criarAgente();
        String tokenDoAgente = usuarioDoAgente(agente);
        assertThat(get("/auth/eu", tokenDoAgente).status()).isEqualTo(200);
        long usuarioId = get("/auth/eu", tokenDoAgente).corpo().get("id").asLong();

        assertThat(post("/usuarios/" + usuarioId + "/desativar", admin, null).status()).isEqualTo(200);
        assertThat(get("/auth/eu", tokenDoAgente).status()).isEqualTo(401);
    }

    @Test
    @DisplayName("Matriz (doc 06, seção 5): perfil sem ✓ recebe 403")
    void perfilSemPermissao() throws Exception {
        long agente = criarAgente();
        String tokenDoAgente = usuarioDoAgente(agente);
        long anomalia = criarAnomalia("BAIXO");
        long missao = criarMissao(anomalia, "INVESTIGACAO", "NORMAL");

        assertThat(get("/usuarios", operador).status()).isEqualTo(403);
        assertThat(post("/agentes", supervisor, "{\"codigo\":\"X\",\"nome\":\"X\",\"especialidade\":\"X\"}").status()).isEqualTo(403);
        assertThat(post("/missoes/" + missao + "/autorizar", operador, "{\"observacao\":\"ok\"}").status()).isEqualTo(403);
        assertThat(post("/missoes/" + missao + "/iniciar", tokenDoAgente, null).status()).isEqualTo(403);
        assertThat(post("/anomalias/" + anomalia + "/declarar-irreversivel", operador, "{\"justificativa\":\"x\"}").status()).isEqualTo(403);
        assertThat(get("/agentes/" + agente + "/historico", operador).status()).isEqualTo(403);
        assertThat(post("/missoes/" + missao + "/intervencoes", operador,
                "{\"acao\":\"a\",\"justificativa\":\"j\",\"impactoEstimado\":\"MINIMO\"}").status()).isEqualTo(403);

        // todo perfil consulta
        assertThat(get("/missoes/" + missao, tokenDoAgente).status()).isEqualTo(200);
        assertThat(get("/estado-operacional", tokenDoAgente).status()).isEqualTo(200);
    }

    @Test
    @DisplayName("S3 e D56: agente estranho à missão recebe 403; designado em missão encerrada recebe 409 (M9)")
    void posseDaMissao() throws Exception {
        long designado = criarAgente();
        long estranho = criarAgente();
        String tokenDesignado = usuarioDoAgente(designado);
        String tokenEstranho = usuarioDoAgente(estranho);
        long missao = criarMissao(criarAnomalia("BAIXO"), "INVESTIGACAO", "NORMAL");
        designar(missao, designado);
        post("/missoes/" + missao + "/iniciar", operador, null);

        String intervencao = "{\"acao\":\"a\",\"justificativa\":\"j\",\"impactoEstimado\":\"MINIMO\",\"agenteId\":" + estranho + "}";
        assertThat(post("/missoes/" + missao + "/intervencoes", tokenEstranho, intervencao).status()).isEqualTo(403);

        Resposta valida = post("/missoes/" + missao + "/intervencoes", tokenDesignado, intervencao);
        assertThat(valida.status()).isEqualTo(201);
        assertThat(valida.corpo().get("agenteId").asLong()).as("S4: o agenteId do corpo é ignorado").isEqualTo(designado);

        post("/missoes/" + missao + "/concluir", operador, """
                {"resultado":"ok","resumo":"ok","impactoObservado":"MINIMO","situacaoFinalAnomalia":"CONFIRMADA"}""");
        Resposta depois = post("/missoes/" + missao + "/intervencoes", tokenDesignado, intervencao);
        assertThat(depois.status()).isEqualTo(409);
        assertThat(depois.texto("regra")).isEqualTo("M9");
    }

    @Test
    @DisplayName("D55: trocar o perfil vale na requisição seguinte, mesmo com o token já emitido")
    void perfilDoBancoNaoDoToken() throws Exception {
        String login = unico("promovido").toLowerCase();
        Resposta criado = post("/usuarios", admin, """
                {"login":"%s","senha":"%s","perfil":"OPERADOR"}""".formatted(login, SENHA));
        String tokenAntigo = token(login);
        assertThat(get("/usuarios", tokenAntigo).status()).isEqualTo(403);

        chamar(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .patch("/usuarios/" + criado.corpo().get("id").asLong()), admin, "{\"perfil\":\"ADMINISTRADOR\"}");
        assertThat(get("/usuarios", tokenAntigo).status()).isEqualTo(200);
    }

    @Test
    @DisplayName("S5: ninguém desativa a si mesmo")
    void naoDesativaASiMesmo() throws Exception {
        long id = get("/auth/eu", admin).corpo().get("id").asLong();
        Resposta r = post("/usuarios/" + id + "/desativar", admin, null);
        assertThat(r.status()).isEqualTo(409);
        assertThat(r.texto("regra")).isEqualTo("S5");
    }

    @Test
    @DisplayName("S6: inativar o agente desativa o usuário ligado a ele")
    void inativarAgenteDesativaUsuario() throws Exception {
        long agente = criarAgente();
        String tokenDoAgente = usuarioDoAgente(agente);
        assertThat(post("/agentes/" + agente + "/inativar", admin, "{\"motivo\":\"Desligado\"}").status()).isEqualTo(200);
        assertThat(get("/auth/eu", tokenDoAgente).status()).isEqualTo(401);
    }
}
