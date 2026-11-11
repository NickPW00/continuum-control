package com.infnet.continuum_control.repository;

import com.infnet.continuum_control.model.entities.RegistroHistorico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface RegistroHistoricoRepository extends JpaRepository<RegistroHistorico, Long>,
        JpaSpecificationExecutor<RegistroHistorico> {
}
