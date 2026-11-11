package com.infnet.continuum_control.repository;

import com.infnet.continuum_control.model.entities.RegistroAgente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegistroAgenteRepository extends JpaRepository<RegistroAgente, Long> {

    Page<RegistroAgente> findByAgenteId(Long agenteId, Pageable pageable);
}
