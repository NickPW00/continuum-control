package com.infnet.continuum_control.service.ocorrencias;

/** Evento publicado na transação que gravou a ocorrência no outbox; ouvido depois do commit. */
public record OcorrenciaRegistrada(String ocorrenciaId) {
}
