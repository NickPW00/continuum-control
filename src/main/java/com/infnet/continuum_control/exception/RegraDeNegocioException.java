package com.infnet.continuum_control.exception;

import lombok.Getter;

@Getter
public class RegraDeNegocioException extends RuntimeException {

    private final String regra;

    public RegraDeNegocioException(String regra, String mensagem) {
        super(mensagem);
        this.regra = regra;
    }
}
