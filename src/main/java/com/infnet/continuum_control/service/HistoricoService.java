package com.infnet.continuum_control.service;

import com.infnet.continuum_control.model.entities.*;
import com.infnet.continuum_control.model.enums.SituacaoAgente;
import com.infnet.continuum_control.model.enums.TipoRegistro;
import com.infnet.continuum_control.repository.RegistroAgenteRepository;
import com.infnet.continuum_control.repository.RegistroHistoricoRepository;
import com.infnet.continuum_control.repository.UsuarioRepository;
import com.infnet.continuum_control.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Grava as duas linhas do tempo do sistema (§12): a da anomalia ({@code registro_historico}) e a
 * do agente ({@code registro_agente}, D31). Sempre dentro da transação de quem chama
 * (MANDATORY): o registro e a mudança que ele descreve são gravados juntos ou nenhum dos dois.
 */
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class HistoricoService {

    private final RegistroHistoricoRepository registroHistoricoRepository;
    private final RegistroAgenteRepository registroAgenteRepository;
    private final UsuarioRepository usuarioRepository;
    private final Clock clock;

    public void registrar(Anomalia anomalia, Missao missao, TipoRegistro tipo, String descricao,
                          Object valorAnterior, Object valorNovo, UsuarioAutenticado usuario) {
        registroHistoricoRepository.save(new RegistroHistorico(anomalia, missao, tipo, descricao,
                texto(valorAnterior), texto(valorNovo), referencia(usuario), agora()));
    }

    public void registrarAgente(Agente agente, SituacaoAgente anterior, String motivo, UsuarioAutenticado usuario) {
        registroAgenteRepository.save(new RegistroAgente(agente, anterior, agente.getSituacao(), motivo,
                referencia(usuario), agora()));
    }

    private Usuario referencia(UsuarioAutenticado usuario) {
        return usuario == null ? null : usuarioRepository.getReferenceById(usuario.id());
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(clock);
    }

    private static String texto(Object valor) {
        return valor == null ? null : valor.toString();
    }
}
