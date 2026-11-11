package com.infnet.continuum_control.service.ocorrencias;

import com.infnet.continuum_control.config.ContinuumProperties;
import com.infnet.continuum_control.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Monitoramento e limpeza do outbox (doc 04, 6.5). Aparece em /actuator/health como "outbox":
 * com uma pendente mais antiga que o limite, o status vira ATRASADO (mapeado para HTTP 200, para
 * não derrubar o contêiner) e a aplicação registra um WARN.
 */
@Slf4j
@Component("outbox")
@RequiredArgsConstructor
public class ManutencaoOutbox implements HealthIndicator {

    private final OutboxRepository outboxRepository;
    private final ContinuumProperties propriedades;
    private final Clock clock;

    @Override
    public Health health() {
        long pendentes = outboxRepository.countByProcessadoEmIsNull();
        var maisAntiga = outboxRepository.criadoEmDaPendenteMaisAntiga();
        Health.Builder saude = Health.up();
        if (maisAntiga.isPresent()) {
            Duration espera = Duration.between(maisAntiga.get(), LocalDateTime.now(clock));
            saude.withDetail("pendenteMaisAntigaHaSegundos", espera.toSeconds());
            if (espera.compareTo(propriedades.outbox().alertaPendenteApos()) > 0) {
                log.warn("Outbox atrasado: {} pendente(s), a mais antiga há {}", pendentes, espera);
                saude = Health.status("ATRASADO").withDetail("pendenteMaisAntigaHaSegundos", espera.toSeconds());
            }
        }
        return saude.withDetail("pendentes", pendentes).build();
    }

    /** A15: apaga as linhas processadas há mais tempo que a retenção (padrão 7 dias). */
    @Scheduled(cron = "${continuum.outbox.limpeza-cron:0 0 3 * * *}")
    @Transactional
    public void limpar() {
        int apagadas = outboxRepository.apagarProcessadasAntesDe(
                LocalDateTime.now(clock).minus(propriedades.outbox().retencaoProcessados()));
        if (apagadas > 0) {
            log.info("Outbox: {} linha(s) processada(s) apagada(s)", apagadas);
        }
    }
}
