package com.infnet.continuum_control.repository;

import com.infnet.continuum_control.model.entities.Agente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgenteRepository extends JpaRepository<Agente,Long> {

}
