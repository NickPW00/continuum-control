package com.infnet.continuum_control.model.entities;

import com.infnet.continuum_control.model.enums.Perfil;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@Getter
@Setter

public class Usuario {
    @Id
    private Long id;
    private String login;
    private String senhaHash;
    private Perfil perfil;
    @ManyToOne
    @JoinColumn(name = "agente_id")
    private Agente agente;
}
