package com.infnet.continuum_control.integracao;

import com.infnet.continuum_control.repository.OutboxRepository;
import com.infnet.continuum_control.repository.RegistroHistoricoRepository;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Persistência nos três bancos (doc 04, seção 9): outbox, validador do MongoDB e cache no Redis. */
class PersistenciaIntegracaoTest extends IntegracaoBase {

    private static final String CHAVE_CACHE = "continuum:indicadores::atual";

    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private StringRedisTemplate redis;
    @Autowired
    private OutboxRepository outboxRepository;
    @Autowired
    private RegistroHistoricoRepository registroHistoricoRepository;

    private record MissaoEmExecucao(long id, String tokenDoAgente) {
    }

    private MissaoEmExecucao missaoEmExecucao() throws Exception {
        long agente = criarAgente();
        String token = usuarioDoAgente(agente);
        long missao = criarMissao(criarAnomalia("BAIXO"), "INVESTIGACAO", "NORMAL");
        designar(missao, agente);
        assertThat(post("/missoes/" + missao + "/iniciar", operador, null).status()).isEqualTo(200);
        return new MissaoEmExecucao(missao, token);
    }

    @Test
    @DisplayName("OB1 e 4.3: a ocorrência passa pelo outbox, chega ao MongoDB com o tipo como discriminador e volta pela API")
    void ocorrenciaChegaAoMongo() throws Exception {
        MissaoEmExecucao m = missaoEmExecucao();
        Resposta r = post("/missoes/" + m.id() + "/ocorrencias", m.tokenDoAgente(), """
                {"tipo":"DISTORCAO_TEMPORAL","observacao":"Oscilação percebida no fórum","intensidade":7,"duracao":2700}""");
        assertThat(r.status()).isEqualTo(201);
        String id = r.texto("id");

        Document bruto = mongoTemplate.getCollection("ocorrencias").find(new Document("_id", new ObjectId(id))).first();
        assertThat(bruto).isNotNull();
        assertThat(bruto.getString("tipo")).isEqualTo("DISTORCAO_TEMPORAL");
        assertThat(bruto.containsKey("_class")).isFalse();
        assertThat(bruto.get("missaoId")).isInstanceOf(Long.class);
        assertThat(bruto.get("dataHora")).isInstanceOf(Date.class);

        Resposta lida = get("/missoes/" + m.id() + "/ocorrencias/" + id, operador);
        assertThat(lida.texto("tipo")).isEqualTo("DISTORCAO_TEMPORAL");
        assertThat(lida.corpo().get("duracao").asLong()).isEqualTo(2700);
        assertThat(lida.texto("observacao")).isEqualTo("Oscilação percebida no fórum");

        assertThat(outboxRepository.findAll()).filteredOn(o -> o.getChave().equals(id))
                .singleElement().satisfies(o -> assertThat(o.getProcessadoEm()).isNotNull());
        assertThat(registroHistoricoRepository.findAll())
                .anyMatch(h -> id.equals(h.getValorNovo()) && h.getTipo().name().equals("OCORRENCIA"));
    }

    @Test
    @DisplayName("Um documento de cada um dos seis tipos é gravado e lido de volta (doc 04, 9.3)")
    void seisTipos() throws Exception {
        MissaoEmExecucao m = missaoEmExecucao();
        String[] corpos = {
                "{\"tipo\":\"OBSERVACAO_HISTORICA\",\"observacao\":\"o\",\"fatoObservado\":\"f\",\"conformeEsperado\":true}",
                "{\"tipo\":\"ALTERACAO_DETECTADA\",\"observacao\":\"o\",\"valorEsperado\":\"a\",\"valorObservado\":\"b\"}",
                "{\"tipo\":\"EVIDENCIA_TEMPORAL\",\"observacao\":\"o\",\"objeto\":\"moeda\",\"periodoEstimado\":\"XXI\",\"grauConfiabilidade\":0.85}",
                "{\"tipo\":\"DISTORCAO_TEMPORAL\",\"observacao\":\"o\",\"intensidade\":3,\"duracao\":60}",
                "{\"tipo\":\"ENCONTRO_INESPERADO\",\"observacao\":\"o\",\"envolvidos\":\"cônsul\",\"houveContato\":false}",
                "{\"tipo\":\"INCIDENTE_OPERACIONAL\",\"observacao\":\"o\",\"gravidade\":\"SEVERO\",\"medidaTomada\":\"recuo\"}"
        };
        for (String corpo : corpos) {
            assertThat(post("/missoes/" + m.id() + "/ocorrencias", m.tokenDoAgente(), corpo).status()).as(corpo).isEqualTo(201);
        }
        assertThat(get("/missoes/" + m.id() + "/ocorrencias", operador).corpo().get("page").get("totalElements").asInt())
                .isEqualTo(6);
        assertThat(get("/missoes/" + m.id() + "/ocorrencias?tipo=INCIDENTE_OPERACIONAL", operador)
                .corpo().get("content").get(0).get("gravidade").asString()).isEqualTo("SEVERO");
    }

    @Test
    @DisplayName("400: campos do tipo validados na API; tipo inexistente recusado")
    void ocorrenciaInvalida() throws Exception {
        MissaoEmExecucao m = missaoEmExecucao();
        Resposta foraDaFaixa = post("/missoes/" + m.id() + "/ocorrencias", m.tokenDoAgente(),
                "{\"tipo\":\"DISTORCAO_TEMPORAL\",\"observacao\":\"o\",\"intensidade\":11,\"duracao\":1}");
        assertThat(foraDaFaixa.status()).isEqualTo(400);
        assertThat(foraDaFaixa.corpo().get("campos").has("intensidade")).isTrue();
        assertThat(post("/missoes/" + m.id() + "/ocorrencias", m.tokenDoAgente(),
                "{\"tipo\":\"INEXISTENTE\",\"observacao\":\"o\"}").status()).isEqualTo(400);
    }

    @Test
    @DisplayName("MG2: o validador do MongoDB recusa um documento com campos de outro tipo")
    void validadorDoMongo() {
        Document invalido = new Document("_id", new ObjectId())
                .append("tipo", "DISTORCAO_TEMPORAL").append("missaoId", 1L).append("agenteId", 1L)
                .append("dataHora", new Date()).append("observacao", "o")
                .append("fatoObservado", "campo de outro tipo");
        assertThatThrownBy(() -> mongoTemplate.getCollection("ocorrencias").insertOne(invalido))
                .hasMessageContaining("Document failed validation");
    }

    @Test
    @DisplayName("OB2: falha no caso de uso não deixa nem histórico nem outbox")
    void falhaNaoDeixaNada() throws Exception {
        long agente = criarAgente();
        String token = usuarioDoAgente(agente);
        long missao = criarMissao(criarAnomalia("BAIXO"), "INVESTIGACAO", "NORMAL");
        designar(missao, agente);          // planejada: ainda não aceita ocorrências
        long antes = outboxRepository.count();

        Resposta r = post("/missoes/" + missao + "/ocorrencias", token,
                "{\"tipo\":\"OBSERVACAO_HISTORICA\",\"observacao\":\"o\",\"fatoObservado\":\"f\",\"conformeEsperado\":true}");
        assertThat(r.status()).isEqualTo(409);
        assertThat(r.texto("regra")).isEqualTo("M9");
        assertThat(outboxRepository.count()).isEqualTo(antes);
    }

    @Test
    @DisplayName("RD1/RD2: o índice fica no Redis com validade e é invalidado depois do commit de quem o altera")
    void cacheDosIndicadores() throws Exception {
        redis.delete(CHAVE_CACHE);
        Resposta primeira = get("/estado-operacional", operador);
        assertThat(primeira.status()).isEqualTo(200);
        assertThat(redis.hasKey(CHAVE_CACHE)).isTrue();
        assertThat(redis.getExpire(CHAVE_CACHE)).isBetween(1L, 60L);
        assertThat(get("/indice-estabilidade", operador).texto("calculadoEm")).isEqualTo(primeira.texto("calculadoEm"));

        criarAnomalia("CRITICO");
        assertThat(redis.hasKey(CHAVE_CACHE)).isFalse();
        Resposta depois = get("/estado-operacional", operador);
        assertThat(depois.corpo().get("anomaliasCriticas").asLong())
                .isEqualTo(primeira.corpo().get("anomaliasCriticas").asLong() + 1);
    }
}
