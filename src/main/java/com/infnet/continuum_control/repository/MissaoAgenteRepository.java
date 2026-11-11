package com.infnet.continuum_control.repository;

import com.infnet.continuum_control.model.entities.MissaoAgente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MissaoAgenteRepository extends JpaRepository<MissaoAgente, Long> {

    boolean existsByMissaoIdAndAgenteId(Long missaoId, Long agenteId);
}
