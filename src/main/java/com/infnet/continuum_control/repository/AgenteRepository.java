package com.infnet.continuum_control.repository;

import com.infnet.continuum_control.model.entities.Agente;
import com.infnet.continuum_control.model.enums.SituacaoAgente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AgenteRepository extends JpaRepository<Agente, Long>, JpaSpecificationExecutor<Agente> {

    boolean existsByCodigo(String codigo);

    Optional<Agente> findByCodigo(String codigo);

    long countBySituacao(SituacaoAgente situacao);
}
