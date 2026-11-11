package com.infnet.continuum_control.service.indicadores;

import com.infnet.continuum_control.config.ContinuumProperties;
import com.infnet.continuum_control.model.entities.Anomalia;
import com.infnet.continuum_control.model.enums.*;
import com.infnet.continuum_control.repository.AgenteRepository;
import com.infnet.continuum_control.repository.AnomaliaRepository;
import com.infnet.continuum_control.repository.IntervencaoRepository;
import com.infnet.continuum_control.repository.MissaoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;

/**
 * Indicadores (§14, §15) com cache no Redis (doc 04, seção 5, D42).
 * <ul>
 *   <li>Leitura cache-aside: lê a chave; se não existir, calcula no PostgreSQL e grava com validade.</li>
 *   <li>Invalidação depois do commit: quem muda algo contado no índice chama {@link #invalidar()}.</li>
 *   <li>Falha aberta: com o Redis fora do ar, a requisição calcula direto no PostgreSQL.</li>
 * </ul>
 */
@Slf4j
@Service
public class IndicadoresService {

    static final String CHAVE = "continuum:indicadores::atual";

    private final StringRedisTemplate redis;
    private final JsonMapper jsonMapper;
    private final AgenteRepository agenteRepository;
    private final AnomaliaRepository anomaliaRepository;
    private final MissaoRepository missaoRepository;
    private final IntervencaoRepository intervencaoRepository;
    private final ContinuumProperties propriedades;
    private final Clock clock;

    public IndicadoresService(StringRedisTemplate redis, JsonMapper jsonMapper, AgenteRepository agenteRepository,
                              AnomaliaRepository anomaliaRepository, MissaoRepository missaoRepository,
                              IntervencaoRepository intervencaoRepository, ContinuumProperties propriedades, Clock clock) {
        this.redis = redis;
        this.jsonMapper = jsonMapper;
        this.agenteRepository = agenteRepository;
        this.anomaliaRepository = anomaliaRepository;
        this.missaoRepository = missaoRepository;
        this.intervencaoRepository = intervencaoRepository;
        this.propriedades = propriedades;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public EstadoOperacional obter() {
        EstadoOperacional emCache = lerCache();
        if (emCache != null) {
            return emCache;
        }
        EstadoOperacional calculado = calcular();
        gravarCache(calculado);
        return calculado;
    }

    /** Apaga a chave quando a transação atual confirma (ou na hora, se não houver transação). */
    public void invalidar() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    apagarCache();
                }
            });
        } else {
            apagarCache();
        }
    }

    EstadoOperacional calcular() {
        LocalDateTime agora = LocalDateTime.now(clock);
        LocalDateTime inicioJanela = agora.minus(propriedades.indice().janela());

        Map<NivelRisco, Long> ativasPorRisco = new EnumMap<>(NivelRisco.class);
        anomaliaRepository.contarPorRisco(Anomalia.SITUACOES_ATIVAS)
                .forEach(c -> ativasPorRisco.put(c.getRisco(), c.getQuantidade()));
        long anomaliasAtivas = ativasPorRisco.values().stream().mapToLong(Long::longValue).sum();
        long emExecucao = missaoRepository.countBySituacao(SituacaoMissao.EM_EXECUCAO);
        long falhas = missaoRepository.countBySituacaoAndDataFimGreaterThanEqual(SituacaoMissao.FALHOU, inicioJanela);
        long severas = intervencaoRepository.countByImpactoEstimadoAndDataHoraGreaterThanEqual(Impacto.SEVERO, inicioJanela);

        CalculadoraIndice.Resultado resultado = CalculadoraIndice.calcular(
                new CalculadoraIndice.Contagens(ativasPorRisco, emExecucao, falhas, severas), propriedades.indice());

        return new EstadoOperacional(
                agenteRepository.countBySituacao(SituacaoAgente.DISPONIVEL),
                agenteRepository.countBySituacao(SituacaoAgente.EM_MISSAO),
                emExecucao,
                anomaliasAtivas,
                ativasPorRisco.getOrDefault(NivelRisco.CRITICO, 0L),
                resultado.indice(),
                resultado.estadoGeral(),
                propriedades.indice().janela().toDays(),
                resultado.penalidades(),
                agora);
    }

    private EstadoOperacional lerCache() {
        try {
            String json = redis.opsForValue().get(CHAVE);
            return json == null ? null : jsonMapper.readValue(json, EstadoOperacional.class);
        } catch (RuntimeException e) {
            log.warn("Cache de indicadores indisponível na leitura; calculando no PostgreSQL: {}", e.getMessage());
            return null;
        }
    }

    private void gravarCache(EstadoOperacional estado) {
        try {
            redis.opsForValue().set(CHAVE, jsonMapper.writeValueAsString(estado),
                    propriedades.cache().indicadoresValidade());
        } catch (RuntimeException e) {
            log.warn("Cache de indicadores indisponível na gravação: {}", e.getMessage());
        }
    }

    private void apagarCache() {
        try {
            redis.delete(CHAVE);
        } catch (RuntimeException e) {
            log.warn("Cache de indicadores indisponível na invalidação; a validade de {} limita o valor antigo: {}",
                    propriedades.cache().indicadoresValidade(), e.getMessage());
        }
    }
}
