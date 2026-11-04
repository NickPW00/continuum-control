package com.infnet.continuum_control.model.entities;

import com.infnet.continuum_control.model.enums.Perfil;

public class Usuario {
    private Long id;
    private String login;
    private String senhaHash;
    private Perfil perfil;
    private Agente agente;
}
