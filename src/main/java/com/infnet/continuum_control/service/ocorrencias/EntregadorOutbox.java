package com.infnet.continuum_control.service.ocorrencias;

import com.infnet.continuum_control.config.ContinuumProperties;
import com.infnet.continuum_control.model.entities.MensagemOutbox;
import com.infnet.continuum_control.model.ocorrencias.Ocorrencia;
import com.infnet.continuum_control.repository.OutboxRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Entregador do outbox (doc 04, 6.2 e 6.3). Leva cada linha pendente ao MongoDB e a marca como
 * processada, uma transação do PostgreSQL por linha.
 * <ul>
 *   <li>{@code FOR UPDATE SKIP LOCKED}: várias execuções ao mesmo tempo nunca pegam a mesma linha.</li>
 *   <li>Gravação pelo {@code _id} (upsert): entregar duas vezes dá no mesmo documento.</li>
 *   <li>Documento recusado: a linha espera (2^tentativas s) e a fila segue.</li>
 *   <li>MongoDB fora do ar: a linha espera e o ciclo para; nenhuma linha é abandonada.</li>
 * </ul>
 */
@Slf4j
@Component
public class EntregadorOutbox {

    private enum Resultado { NADA_PENDENTE, ENTREGUE, RECUSADA, MONGO_INDISPONIVEL }

    private final OutboxRepository outboxRepository;
    private final MongoTemplate mongoTemplate;
    private final JsonMapper jsonMapper;
    private final ContinuumProperties propriedades;
    private final Clock clock;
    private final TransactionTemplate novaTransacao;

    public EntregadorOutbox(OutboxRepository outboxRepository, MongoTemplate mongoTemplate, JsonMapper jsonMapper,
                            ContinuumProperties propriedades, Clock clock, PlatformTransactionManager transacoes) {
        this.outboxRepository = outboxRepository;
        this.mongoTemplate = mongoTemplate;
        this.jsonMapper = jsonMapper;
        this.propriedades = propriedades;
        this.clock = clock;
        this.novaTransacao = new TransactionTemplate(transacoes);
        this.novaTransacao.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** No caso normal, a ocorrência já está no MongoDB quando a resposta chega ao cliente. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoRegistrarOcorrencia(OcorrenciaRegistrada evento) {
        entregarPendentes();
    }

    /** Pega o que ficou pendente (MongoDB fora do ar, queda da aplicação). */
    @Scheduled(fixedDelayString = "${continuum.outbox.intervalo-ms:2000}")
    public void entregarAgendado() {
        entregarPendentes();
    }

    public int entregarPendentes() {
        int entregues = 0;
        while (true) {
            Resultado resultado;
            try {
                resultado = novaTransacao.execute(status -> entregarUma());
            } catch (RuntimeException e) {
                log.warn("Falha ao processar o outbox: {}", e.getMessage());
                return entregues;
            }
            if (resultado == Resultado.ENTREGUE) entregues++;
            if (resultado == Resultado.NADA_PENDENTE || resultado == Resultado.MONGO_INDISPONIVEL) {
                return entregues;
            }
        }
    }

    private Resultado entregarUma() {
        LocalDateTime agora = LocalDateTime.now(clock);
        Optional<MensagemOutbox> pendente = outboxRepository.travarProximaPendente(agora);
        if (pendente.isEmpty()) {
            return Resultado.NADA_PENDENTE;
        }
        MensagemOutbox mensagem = pendente.get();
        try {
            Ocorrencia ocorrencia = jsonMapper.readValue(mensagem.getPayload(), Ocorrencia.class);
            mongoTemplate.save(ocorrencia);
            mensagem.marcarProcessada(agora);
            return Resultado.ENTREGUE;
        } catch (DataAccessResourceFailureException e) {
            mensagem.adiar(e.getMessage(), agora, propriedades.outbox().esperaMaxima());
            log.warn("MongoDB indisponível; ocorrência {} adiada para {}", mensagem.getChave(), mensagem.getProximaTentativa());
            return Resultado.MONGO_INDISPONIVEL;
        } catch (RuntimeException e) {
            mensagem.adiar(e.getMessage(), agora, propriedades.outbox().esperaMaxima());
            log.warn("Ocorrência {} recusada ({} tentativa(s)): {}", mensagem.getChave(), mensagem.getTentativas(), e.getMessage());
            return Resultado.RECUSADA;
        }
    }
}
