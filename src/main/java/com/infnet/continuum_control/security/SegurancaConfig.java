package com.infnet.continuum_control.security;

import com.infnet.continuum_control.config.ContinuumProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Autenticação e autorização (§22, doc 06). Três camadas: o filtro confere o token (401),
 * {@code @PreAuthorize} confere o perfil (403) e o serviço confere a posse do recurso (403).
 */
@Configuration
@EnableMethodSecurity
public class SegurancaConfig {

    public static final String EMISSOR = "continuum-control";

    @Bean
    SecurityFilterChain api(HttpSecurity http, UsuarioDoToken conversor, RespostasDeSeguranca respostas) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())                                   // API sem cookies (D53)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()   // S1
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o
                        .jwt(j -> j.jwtAuthenticationConverter(conversor))
                        .authenticationEntryPoint(respostas))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(respostas)
                        .accessDeniedHandler(respostas))
                .build();
    }

    @Bean
    JwtDecoder jwtDecoder(ContinuumProperties propriedades) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(chave(propriedades))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(EMISSOR));
        return decoder;
    }

    @Bean
    JwtEncoder jwtEncoder(ContinuumProperties propriedades) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chave(propriedades)));
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    private static SecretKey chave(ContinuumProperties propriedades) {
        byte[] bytes = propriedades.jwt().segredo().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET precisa de pelo menos 32 bytes (HS256)");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }
}
