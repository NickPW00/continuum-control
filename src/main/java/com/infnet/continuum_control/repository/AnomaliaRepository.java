package com.infnet.continuum_control.repository;

import com.infnet.continuum_control.model.entities.Anomalia;
import com.infnet.continuum_control.model.enums.NivelRisco;
import com.infnet.continuum_control.model.enums.SituacaoAnomalia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface AnomaliaRepository extends JpaRepository<Anomalia, Long>, JpaSpecificationExecutor<Anomalia> {

    boolean existsByEventoId(Long eventoId);

    long countBySituacaoIn(Collection<SituacaoAnomalia> situacoes);

    long countBySituacaoInAndRisco(Collection<SituacaoAnomalia> situacoes, NivelRisco risco);

    @Query("select a.risco as risco, count(a) as quantidade from Anomalia a where a.situacao in :situacoes group by a.risco")
    List<ContagemPorRisco> contarPorRisco(Collection<SituacaoAnomalia> situacoes);

    interface ContagemPorRisco {
        NivelRisco getRisco();

        long getQuantidade();
    }
}
