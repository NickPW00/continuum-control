package com.infnet.continuum_control.security;

import com.infnet.continuum_control.repository.MissaoAgenteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Regra S3 (doc 06): o usuário AGENTE só registra intervenção ou ocorrência numa missão em que o
 * seu agente tem participação. É conferida antes das guardas do domínio (D56): um agente estranho
 * à missão recebe 403 e nunca um 409 que revelaria o estado dela.
 */
@Component
@RequiredArgsConstructor
public class PosseDeMissao {

    private final MissaoAgenteRepository missaoAgenteRepository;

    public void exigir(Long missaoId, UsuarioAutenticado usuario) {
        if (usuario.agenteId() == null
                || !missaoAgenteRepository.existsByMissaoIdAndAgenteId(missaoId, usuario.agenteId())) {
            throw new AccessDeniedException("O seu agente não participa desta missão");
        }
    }
}
