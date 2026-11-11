package com.infnet.continuum_control.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Representação única dos erros (§18): todo erro é um ProblemDetail (RFC 9457, D48).
 * <ul>
 *   <li>400 dados inválidos, com {@code campos}</li>
 *   <li>401 não autenticado e 403 sem permissão (perfil ou posse)</li>
 *   <li>404 recurso inexistente</li>
 *   <li>409 conflito com o estado: guarda do domínio (com {@code regra}), constraint do banco
 *       (com {@code constraint}) ou concorrência ({@code @Version})</li>
 *   <li>500 erro inesperado, com {@code traceId} para achar o detalhe no log</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String MENSAGEM_INTEGRIDADE = "A operação viola uma restrição de integridade";

    /** Doc 05, tabela 3.3: a mensagem de cada constraint que pode chegar ao banco. */
    private static final Map<String, String> MENSAGENS_CONSTRAINT = Map.ofEntries(
            Map.entry("uk_agente_codigo", "Já existe um agente com este código"),
            Map.entry("uk_usuario_login", "Este login já está em uso"),
            Map.entry("uk_usuario_agente", "Este agente já tem um usuário"),
            Map.entry("ck_usuario_agente_perfil", "Usuário com perfil AGENTE precisa de um agente vinculado"),
            Map.entry("uk_missao_codigo", "Já existe uma missão com este código"),
            Map.entry("uk_missao_agente", "O agente já está designado nesta missão"),
            Map.entry("uk_autorizacao_missao", "A missão já foi autorizada"),
            Map.entry("uk_encerramento_missao", "A missão já foi encerrada"),
            Map.entry("uk_missao_em_execucao", "Já existe uma missão em execução para esta anomalia"),
            Map.entry("fk_intervencao_participacao", "O agente não está designado nesta missão"),
            Map.entry("fk_anomalia_evento", "O evento tem anomalias e não pode ser removido")
    );

    @ExceptionHandler(RegraDeNegocioException.class)
    public ProblemDetail regraDeNegocio(RegraDeNegocioException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        if (ex.getRegra() != null) {
            problema.setProperty("regra", ex.getRegra());
        }
        return problema;
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ProblemDetail naoEncontrado(RecursoNaoEncontradoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** 403: o @PreAuthorize e a regra de posse lançam esta exceção dentro do controller. */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail semPermissao(AccessDeniedException ex) {
        String detalhe = ex.getMessage() != null && !ex.getMessage().startsWith("Access Denied")
                ? ex.getMessage() : "O seu perfil não tem acesso a esta operação";
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, detalhe);
    }

    /** 401: login com credenciais inválidas (S2: sempre a mesma mensagem). */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> naoAutenticado(AuthenticationException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Login ou senha inválidos");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).header(HttpHeaders.WWW_AUTHENTICATE, "Bearer").body(problema);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail integridade(DataIntegrityViolationException ex) {
        String constraint = nomeDaConstraint(ex);
        String mensagem = MENSAGEM_INTEGRIDADE;
        if (constraint != null) {
            mensagem = MENSAGENS_CONSTRAINT.getOrDefault(constraint, mensagem);
            if (!MENSAGENS_CONSTRAINT.containsKey(constraint) && constraint.startsWith("fk_")) {
                mensagem = "O registro é usado por outros e não pode ser removido";
            }
            if (constraint.startsWith("ck_") && !MENSAGENS_CONSTRAINT.containsKey(constraint)) {
                log.error("Violação de {} chegou ao banco: a validação anterior deveria ter barrado", constraint, ex);
            }
        }
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, mensagem);
        if (constraint != null) {
            problema.setProperty("constraint", constraint);
        }
        return problema;
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail concorrencia(OptimisticLockingFailureException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "O registro foi alterado por outra operação; tente de novo");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail inesperado(Exception ex) {
        String traceId = UUID.randomUUID().toString();
        log.error("Erro inesperado [traceId={}]", traceId, ex);
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno; contate o suporte informando o traceId");
        problema.setProperty("traceId", traceId);
        return problema;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(erro -> campos.putIfAbsent(erro.getField(), erro.getDefaultMessage()));

        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Dados de entrada inválidos");
        problema.setProperty("campos", campos);
        return ResponseEntity.badRequest().body(problema);
    }

    private String nomeDaConstraint(DataIntegrityViolationException ex) {
        Throwable causa = ex.getCause();
        while (causa != null) {
            if (causa instanceof org.hibernate.exception.ConstraintViolationException cve) {
                return cve.getConstraintName();
            }
            causa = causa.getCause();
        }
        return null;
    }
}
