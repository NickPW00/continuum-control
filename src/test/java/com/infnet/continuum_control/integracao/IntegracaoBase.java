package com.infnet.continuum_control.integracao;

import com.infnet.continuum_control.model.entities.Usuario;
import com.infnet.continuum_control.model.enums.Perfil;
import com.infnet.continuum_control.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * Base dos testes de integração (§23): os três bancos reais sobem com Testcontainers e são
 * compartilhados por todas as classes (doc 04, seção 7).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegracaoBase {

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");

    @ServiceConnection
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7");

    @ServiceConnection(name = "redis")
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7").withExposedPorts(6379);

    static {
        Startables.deepStart(POSTGRES, MONGO, REDIS).join();
    }

    private static final AtomicInteger SEQUENCIA = new AtomicInteger();
    protected static final String SENHA = "senha-de-teste";

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected JsonMapper json;
    @Autowired
    protected UsuarioRepository usuarioRepository;
    @Autowired
    protected PasswordEncoder passwordEncoder;

    protected String admin;
    protected String supervisor;
    protected String operador;

    @BeforeEach
    void entrarComOsPerfisFixos() throws Exception {
        admin = token(usuarioFixo("admin.teste", Perfil.ADMINISTRADOR));
        supervisor = token(usuarioFixo("supervisor.teste", Perfil.SUPERVISOR));
        operador = token(usuarioFixo("operador.teste", Perfil.OPERADOR));
    }

    private String usuarioFixo(String login, Perfil perfil) {
        if (!usuarioRepository.existsByLogin(login)) {
            usuarioRepository.save(new Usuario(login, passwordEncoder.encode(SENHA), perfil, null));
        }
        return login;
    }

    /** Sufixo único: os bancos são compartilhados entre os testes. */
    protected static String unico(String prefixo) {
        return prefixo + "-" + SEQUENCIA.incrementAndGet() + "-" + (System.nanoTime() % 10000);
    }

    protected String token(String login) throws Exception {
        Resposta r = chamar(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/auth/login"), null,"{\"login\":\"" + login + "\",\"senha\":\"" + SENHA + "\"}");
        assertThat(r.status()).as("login de %s", login).isEqualTo(200);
        return r.corpo().get("token").asString();
    }

    protected Resposta chamar(MockHttpServletRequestBuilder requisicao, String token, String corpo) throws Exception {
        if (token != null) requisicao.header("Authorization", "Bearer " + token);
        if (corpo != null) requisicao.contentType(MediaType.APPLICATION_JSON).content(corpo.getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse resposta = mvc.perform(requisicao).andReturn().getResponse();
        String texto = resposta.getContentAsString(StandardCharsets.UTF_8);
        return new Resposta(resposta.getStatus(), texto.isBlank() ? null : json.readTree(texto), resposta);
    }

    protected Resposta get(String url, String token) throws Exception {
        return chamar(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url), token, null);
    }

    protected Resposta post(String url, String token, String corpo) throws Exception {
        return chamar(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url), token, corpo);
    }

    // ---------- montagem de cenários pela própria API ----------

    protected long criarEvento(String data) throws Exception {
        Resposta r = post("/eventos", operador, """
                {"acontecimento":"Evento %s","descricao":"Descrição","data":"%s","localizacao":"Roma","importancia":"ALTA"}
                """.formatted(unico("E"), data));
        assertThat(r.status()).isEqualTo(201);
        return r.corpo().get("id").asLong();
    }

    protected long criarAnomalia(String risco) throws Exception {
        long evento = criarEvento("1969-07-20");
        Resposta r = post("/anomalias", operador, """
                {"eventoId":%d,"divergenciaObservada":"Divergência","risco":"%s"}""".formatted(evento, risco));
        assertThat(r.status()).isEqualTo(201);
        return r.corpo().get("id").asLong();
    }

    protected long criarAgente() throws Exception {
        Resposta r = post("/agentes", admin, """
                {"codigo":"%s","nome":"Agente de teste","especialidade":"Roma Antiga"}""".formatted(unico("AG")));
        assertThat(r.status()).isEqualTo(201);
        return r.corpo().get("id").asLong();
    }

    /** Cria um usuário AGENTE ligado ao agente e devolve o token dele. */
    protected String usuarioDoAgente(long agenteId) throws Exception {
        String login = unico("agente").toLowerCase();
        Resposta r = post("/usuarios", admin, """
                {"login":"%s","senha":"%s","perfil":"AGENTE","agenteId":%d}""".formatted(login, SENHA, agenteId));
        assertThat(r.status()).isEqualTo(201);
        return token(login);
    }

    protected long criarMissao(long anomaliaId, String proposito, String prioridade) throws Exception {
        Resposta r = post("/missoes", operador, """
                {"codigo":"%s","objetivo":"Objetivo","proposito":"%s","anomaliaId":%d,
                 "destinoTemporal":"1969-07-20","prioridade":"%s"}""".formatted(unico("MI"), proposito, anomaliaId, prioridade));
        assertThat(r.status()).isEqualTo(201);
        return r.corpo().get("id").asLong();
    }

    protected void designar(long missaoId, long agenteId) throws Exception {
        assertThat(post("/missoes/" + missaoId + "/designacoes", operador, "{\"agenteId\":" + agenteId + "}").status())
                .isEqualTo(201);
    }

    protected record Resposta(int status, JsonNode corpo, MockHttpServletResponse bruta) {
        String texto(String campo) {
            return corpo == null || corpo.get(campo) == null ? null : corpo.get(campo).asString();
        }
    }
}
