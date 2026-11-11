package com.infnet.continuum_control.service.indicadores;

import com.infnet.continuum_control.config.ContinuumProperties;
import com.infnet.continuum_control.model.enums.EstadoLinhaTemporal;
import com.infnet.continuum_control.model.enums.NivelRisco;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/** Índice de Estabilidade (doc 01, D36): o exemplo do documento e as duas propriedades pedidas. */
class CalculadoraIndiceTest {

    private static final ContinuumProperties.Indice PESOS = new ContinuumProperties.Indice(Duration.ofDays(30),
            Map.of(NivelRisco.BAIXO, 2, NivelRisco.MODERADO, 5, NivelRisco.ALTO, 10, NivelRisco.CRITICO, 20), 2, 8, 5);

    private static CalculadoraIndice.Contagens contagens(long baixo, long moderado, long alto, long critico,
                                                         long emExecucao, long falhas, long severas) {
        Map<NivelRisco, Long> porRisco = new EnumMap<>(NivelRisco.class);
        porRisco.put(NivelRisco.BAIXO, baixo);
        porRisco.put(NivelRisco.MODERADO, moderado);
        porRisco.put(NivelRisco.ALTO, alto);
        porRisco.put(NivelRisco.CRITICO, critico);
        return new CalculadoraIndice.Contagens(porRisco, emExecucao, falhas, severas);
    }

    @Test
    @DisplayName("Exemplo do D36: ALTO + CRITICO + 1 em execução + 1 severa = 63, ATENCAO")
    void exemploDoDocumento() {
        var resultado = CalculadoraIndice.calcular(contagens(0, 0, 1, 1, 1, 0, 1), PESOS);
        assertThat(resultado.indice()).isEqualTo(63);
        assertThat(resultado.estadoGeral()).isEqualTo(EstadoLinhaTemporal.ATENCAO);
    }

    @Test
    @DisplayName("Sem problemas o índice é 100; com muitos, nunca fica abaixo de 0")
    void limites() {
        assertThat(CalculadoraIndice.calcular(contagens(0, 0, 0, 0, 0, 0, 0), PESOS).indice()).isEqualTo(100);
        var muitos = CalculadoraIndice.calcular(contagens(50, 50, 50, 50, 50, 50, 50), PESOS);
        assertThat(muitos.indice()).isZero();
        assertThat(muitos.estadoGeral()).isEqualTo(EstadoLinhaTemporal.CRITICO);
    }

    @Test
    @DisplayName("Propriedades: fica entre 0 e 100, e um problema a mais nunca aumenta o índice")
    void propriedades() {
        Random aleatorio = new Random(42);
        for (int i = 0; i < 2000; i++) {
            long[] c = new long[7];
            for (int j = 0; j < 7; j++) c[j] = aleatorio.nextInt(8);
            int antes = CalculadoraIndice.calcular(contagens(c[0], c[1], c[2], c[3], c[4], c[5], c[6]), PESOS).indice();
            assertThat(antes).isBetween(0, 100);
            int fator = aleatorio.nextInt(7);
            c[fator]++;
            int depois = CalculadoraIndice.calcular(contagens(c[0], c[1], c[2], c[3], c[4], c[5], c[6]), PESOS).indice();
            assertThat(depois).isLessThanOrEqualTo(antes);
        }
    }

    @Test
    @DisplayName("Faixas do estado geral (§14)")
    void faixas() {
        assertThat(EstadoLinhaTemporal.doIndice(80)).isEqualTo(EstadoLinhaTemporal.ESTAVEL);
        assertThat(EstadoLinhaTemporal.doIndice(79)).isEqualTo(EstadoLinhaTemporal.ATENCAO);
        assertThat(EstadoLinhaTemporal.doIndice(50)).isEqualTo(EstadoLinhaTemporal.ATENCAO);
        assertThat(EstadoLinhaTemporal.doIndice(49)).isEqualTo(EstadoLinhaTemporal.INSTAVEL);
        assertThat(EstadoLinhaTemporal.doIndice(20)).isEqualTo(EstadoLinhaTemporal.INSTAVEL);
        assertThat(EstadoLinhaTemporal.doIndice(19)).isEqualTo(EstadoLinhaTemporal.CRITICO);
    }
}
