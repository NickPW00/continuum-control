package com.infnet.continuum_control.model.entities;

import com.infnet.continuum_control.model.enums.Perfil;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class Usuario {
    private Long id;
    private String login;
    private String senhaHash;
    private Perfil perfil;
    private Agente agente;
}
