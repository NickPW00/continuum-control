package com.infnet.continuum_control.model.enums;

/** Estado geral da linha temporal (§14), derivado do Índice de Estabilidade (doc 01, D36). */
public enum EstadoLinhaTemporal {
    ESTAVEL, ATENCAO, INSTAVEL, CRITICO;

    public static EstadoLinhaTemporal doIndice(int indice) {
        if (indice >= 80) return ESTAVEL;
        if (indice >= 50) return ATENCAO;
        if (indice >= 20) return INSTAVEL;
        return CRITICO;
    }
}
