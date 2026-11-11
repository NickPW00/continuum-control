package com.infnet.continuum_control.model.entities;

import com.infnet.continuum_control.exception.RegraDeNegocioException;
import com.infnet.continuum_control.model.enums.Perfil;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Entity
@Table(name = "usuario")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "login", nullable = false, length = 50, updatable = false)
    private String login;

    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "perfil", nullable = false, length = 15)
    private Perfil perfil;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agente_id")
    private Agente agente;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo;

    public Usuario(String login, String senhaHash, Perfil perfil, Agente agente) {
        this.login = Objects.requireNonNull(login, "O login é obrigatório");
        this.senhaHash = Objects.requireNonNull(senhaHash, "A senha é obrigatória");
        definirPerfil(perfil, agente);
        this.ativo = true;
    }

    public void alterarPerfil(Perfil perfil, Agente agente) {
        definirPerfil(perfil, agente);
    }

    private void definirPerfil(Perfil perfil, Agente agente) {
        Objects.requireNonNull(perfil, "O perfil é obrigatório");
        if (perfil == Perfil.AGENTE && agente == null) {
            throw new RegraDeNegocioException("D38", "Usuário com perfil AGENTE precisa de um agente vinculado");
        }
        this.perfil = perfil;
        this.agente = agente;
    }

    public void alterarSenha(String senhaHash) {
        this.senhaHash = Objects.requireNonNull(senhaHash, "A senha é obrigatória");
    }

    public void desativar() {
        if (!this.ativo) {
            throw new RegraDeNegocioException("S5", "O usuário já está desativado");
        }
        this.ativo = false;
    }

    public void reativar() {
        if (this.ativo) {
            throw new RegraDeNegocioException("S6", "O usuário já está ativo");
        }
        this.ativo = true;
    }

    public boolean isAtivo() {
        return Boolean.TRUE.equals(this.ativo);
    }

    public Long getAgenteId() {
        return this.agente == null ? null : this.agente.getId();
    }
}
