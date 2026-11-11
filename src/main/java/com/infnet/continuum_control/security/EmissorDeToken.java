package com.infnet.continuum_control.security;

import com.infnet.continuum_control.config.ContinuumProperties;
import com.infnet.continuum_control.model.entities.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

/** Emite o JWT do login (doc 06, seção 2). {@code perfil} e {@code agenteId} são só informativos. */
@Component
@RequiredArgsConstructor
public class EmissorDeToken {

    private final JwtEncoder jwtEncoder;
    private final ContinuumProperties propriedades;
    private final Clock clock;

    public TokenEmitido emitir(Usuario usuario) {
        Instant agora = clock.instant();
        Instant expira = agora.plus(propriedades.jwt().validade());
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(SegurancaConfig.EMISSOR)
                .subject(String.valueOf(usuario.getId()))
                .issuedAt(agora)
                .expiresAt(expira)
                .claim("perfil", usuario.getPerfil().name());
        if (usuario.getAgenteId() != null) {
            claims.claim("agenteId", usuario.getAgenteId());
        }
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(cabecalho, claims.build())).getTokenValue();
        return new TokenEmitido(token, expira);
    }

    public record TokenEmitido(String token, Instant expiraEm) {
    }
}
