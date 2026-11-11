package com.infnet.continuum_control.repository;

import com.infnet.continuum_control.model.entities.Intervencao;
import com.infnet.continuum_control.model.enums.Impacto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface IntervencaoRepository extends JpaRepository<Intervencao, Long> {

    Page<Intervencao> findByMissaoId(Long missaoId, Pageable pageable);

    Optional<Intervencao> findByIdAndMissaoId(Long id, Long missaoId);

    long countByImpactoEstimadoAndDataHoraGreaterThanEqual(Impacto impacto, LocalDateTime desde);
}
