package com.infnet.continuum_control.service.indicadores;

import com.infnet.continuum_control.model.enums.EstadoLinhaTemporal;

import java.time.LocalDateTime;

/**
 * Estado Operacional (§14) com o Índice de Estabilidade (§15) e a sua composição. É o valor que
 * fica no Redis: um cálculo atende os dois endpoints de indicadores (doc 04, 5.1).
 */
public record EstadoOperacional(
        long agentesDisponiveis,
        long agentesEmMissao,
        long missoesEmExecucao,
        long anomaliasAtivas,
        long anomaliasCriticas,
        int indiceEstabilidade,
        EstadoLinhaTemporal estadoGeral,
        long janelaDias,
        CalculadoraIndice.Penalidades penalidades,
        LocalDateTime calculadoEm
) {
}
