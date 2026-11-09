package com.infnet.continuum_control.repository;

import com.infnet.continuum_control.model.entities.EventoHistorico;
import com.infnet.continuum_control.model.enums.Importancia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EventoHistoricoRepository extends JpaRepository<EventoHistorico, Long> {

    List<EventoHistorico> findByImportancia(Importancia importancia);

    List<EventoHistorico> findByDataBetween(LocalDate inicio, LocalDate fim);
}
