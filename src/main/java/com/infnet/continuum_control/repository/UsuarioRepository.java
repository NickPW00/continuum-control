package com.infnet.continuum_control.repository;

import com.infnet.continuum_control.model.entities.Usuario;
import com.infnet.continuum_control.model.enums.Perfil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long>, JpaSpecificationExecutor<Usuario> {

    Optional<Usuario> findByLogin(String login);

    boolean existsByLogin(String login);

    boolean existsByAgente_Id(Long agenteId);

    Optional<Usuario> findByAgente_Id(Long agenteId);

    long countByPerfilAndAtivoTrue(Perfil perfil);
}
