package com.infnet.continuum_control.repository;

import com.infnet.continuum_control.model.entities.Missao;
import com.infnet.continuum_control.model.enums.SituacaoMissao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface MissaoRepository extends JpaRepository<Missao, Long>, JpaSpecificationExecutor<Missao> {

    boolean existsByCodigo(String codigo);

    boolean existsByAnomaliaIdAndSituacao(Long anomaliaId, SituacaoMissao situacao);

    List<Missao> findByAnomaliaIdAndSituacaoIn(Long anomaliaId, Collection<SituacaoMissao> situacoes);

    @Query("select distinct m from Missao m join m.participacoes p where p.agente.id = :agenteId and m.situacao in :situacoes")
    List<Missao> findComAgenteNasSituacoes(Long agenteId, Collection<SituacaoMissao> situacoes);

    long countBySituacao(SituacaoMissao situacao);

    long countBySituacaoAndDataFimGreaterThanEqual(SituacaoMissao situacao, LocalDateTime desde);
}
