package com.infnet.continuum_control.service.indicadores;

import com.infnet.continuum_control.config.ContinuumProperties;
import com.infnet.continuum_control.model.enums.EstadoLinhaTemporal;
import com.infnet.continuum_control.model.enums.NivelRisco;

import java.util.EnumMap;
import java.util.Map;

/**
 * Fórmula do Índice de Estabilidade Temporal (§15, doc 01 D36): parte de 100 e perde pontos por
 * anomalias ativas (pelo risco), missões em execução, falhas recentes e intervenções severas
 * recentes. Função pura, sem banco, para poder ser testada isoladamente.
 */
public final class CalculadoraIndice {

    private CalculadoraIndice() {
    }

    public record Contagens(Map<NivelRisco, Long> anomaliasAtivasPorRisco, long missoesEmExecucao,
                            long missoesFalhas, long intervencoesSeveras) {
    }

    public record Penalidades(Map<NivelRisco, Long> anomaliasAtivas, long missoesEmExecucao,
                              long missoesFalhas, long intervencoesSeveras) {

        long total() {
            return anomaliasAtivas.values().stream().mapToLong(Long::longValue).sum()
                    + missoesEmExecucao + missoesFalhas + intervencoesSeveras;
        }
    }

    public record Resultado(int indice, EstadoLinhaTemporal estadoGeral, Penalidades penalidades) {
    }

    public static Resultado calcular(Contagens contagens, ContinuumProperties.Indice pesos) {
        Map<NivelRisco, Long> porRisco = new EnumMap<>(NivelRisco.class);
        for (NivelRisco risco : NivelRisco.values()) {
            long quantidade = contagens.anomaliasAtivasPorRisco().getOrDefault(risco, 0L);
            porRisco.put(risco, quantidade * pesos.penalidadeRisco().getOrDefault(risco, 0));
        }
        Penalidades penalidades = new Penalidades(porRisco,
                contagens.missoesEmExecucao() * pesos.penalidadeMissaoEmExecucao(),
                contagens.missoesFalhas() * pesos.penalidadeMissaoFalhou(),
                contagens.intervencoesSeveras() * pesos.penalidadeIntervencaoSevera());
        int indice = (int) Math.max(0, 100 - penalidades.total());
        return new Resultado(indice, EstadoLinhaTemporal.doIndice(indice), penalidades);
    }
}
