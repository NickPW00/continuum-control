package com.infnet.continuum_control.controller;

import com.infnet.continuum_control.model.enums.EstadoLinhaTemporal;
import com.infnet.continuum_control.service.indicadores.CalculadoraIndice;
import com.infnet.continuum_control.service.indicadores.EstadoOperacional;
import com.infnet.continuum_control.service.indicadores.IndicadoresService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * Indicadores (doc 05, seção 10). Os dois leem o mesmo cálculo, guardado no Redis;
 * {@code calculadoEm} diz de quando é o valor (D52).
 */
@RestController
@RequiredArgsConstructor
public class IndicadoresController {

    private final IndicadoresService indicadoresService;

    public record EstadoOperacionalResponse(long agentesDisponiveis, long agentesEmMissao, long missoesEmExecucao,
                                            long anomaliasAtivas, long anomaliasCriticas, int indiceEstabilidade,
                                            EstadoLinhaTemporal estadoGeral, LocalDateTime calculadoEm) {
    }

    public record IndiceResponse(int indice, EstadoLinhaTemporal estadoGeral, long janelaDias,
                                 CalculadoraIndice.Penalidades penalidades, LocalDateTime calculadoEm) {
    }

    @GetMapping("/estado-operacional")
    public EstadoOperacionalResponse estadoOperacional() {
        EstadoOperacional e = indicadoresService.obter();
        return new EstadoOperacionalResponse(e.agentesDisponiveis(), e.agentesEmMissao(), e.missoesEmExecucao(),
                e.anomaliasAtivas(), e.anomaliasCriticas(), e.indiceEstabilidade(), e.estadoGeral(), e.calculadoEm());
    }

    @GetMapping("/indice-estabilidade")
    public IndiceResponse indiceEstabilidade() {
        EstadoOperacional e = indicadoresService.obter();
        return new IndiceResponse(e.indiceEstabilidade(), e.estadoGeral(), e.janelaDias(), e.penalidades(), e.calculadoEm());
    }
}
