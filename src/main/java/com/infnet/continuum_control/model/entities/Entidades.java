package com.infnet.continuum_control.model.entities;

final class Entidades {

    private Entidades() {
    }

    /**
     * Compara dois agentes pelo id; sem id (entidades ainda não gravadas, como nos testes
     * de unidade), compara pela própria instância.
     */
    static boolean mesmoAgente(Agente a, Agente b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return a.getId() != null && a.getId().equals(b.getId());
    }
}
