package com.infnet.continuum_control.security;

/**
 * Expressões de {@code @PreAuthorize} da matriz de permissões (doc 06, seção 5). Rotas sem
 * anotação exigem só autenticação: todo perfil consulta.
 */
public final class Acesso {

    /** OPERADOR, SUPERVISOR e ADMINISTRADOR: operações do dia a dia. */
    public static final String OPERACAO = "hasAnyRole('OPERADOR','SUPERVISOR','ADMINISTRADOR')";

    /** SUPERVISOR e ADMINISTRADOR: autorizar missões, declarar irreversível, remover eventos. */
    public static final String SUPERVISAO = "hasAnyRole('SUPERVISOR','ADMINISTRADOR')";

    /** Só ADMINISTRADOR: agentes e usuários. */
    public static final String ADMINISTRACAO = "hasRole('ADMINISTRADOR')";

    /** Só AGENTE: intervenções e ocorrências, sempre com a regra de posse S3. */
    public static final String AGENTE = "hasRole('AGENTE')";

    private Acesso() {
    }
}
