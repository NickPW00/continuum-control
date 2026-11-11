package com.infnet.continuum_control.config;

import com.infnet.continuum_control.model.enums.NivelRisco;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

/**
 * Parâmetros operacionais da aplicação (§24): mudam por ambiente, sem recompilar.
 * Valores em application.yaml, sobrescritos por variáveis de ambiente.
 */
@ConfigurationProperties(prefix = "continuum")
public record ContinuumProperties(
        Indice indice,
        Cache cache,
        Outbox outbox,
        Jwt jwt
) {

    /** Pesos e janela do Índice de Estabilidade (doc 01, D36). */
    public record Indice(
            Duration janela,
            Map<NivelRisco, Integer> penalidadeRisco,
            int penalidadeMissaoEmExecucao,
            int penalidadeMissaoFalhou,
            int penalidadeIntervencaoSevera
    ) {
    }

    public record Cache(Duration indicadoresValidade) {
    }

    /** Entregador do outbox (doc 04, seção 6). */
    public record Outbox(
            long intervaloMs,
            Duration esperaMaxima,
            Duration alertaPendenteApos,
            Duration retencaoProcessados
    ) {
    }

    /** Tokens de acesso (doc 06, seção 2). */
    public record Jwt(String segredo, Duration validade) {
    }
}
