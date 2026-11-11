package com.infnet.continuum_control.repository;

import com.infnet.continuum_control.model.entities.EventoHistorico;
import com.infnet.continuum_control.model.enums.Importancia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface EventoHistoricoRepository extends JpaRepository<EventoHistorico, Long> {

    Page<EventoHistorico> findByImportancia(Importancia importancia, Pageable pageable);

    Page<EventoHistorico> findByDataBetween(LocalDate dataDe, LocalDate dataAte, Pageable pageable);

    Page<EventoHistorico> findByImportanciaAndDataBetween(Importancia importancia, LocalDate dataDe,
                                                          LocalDate dataAte, Pageable pageable);
}
