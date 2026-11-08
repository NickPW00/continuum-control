package com.infnet.continuum_control.repository;

import com.infnet.continuum_control.model.entities.Agente;
import com.infnet.continuum_control.model.enums.SituacaoAgente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgenteRepository extends JpaRepository<Agente,Long> {

    boolean existsByCodigo(String codigo);

    Optional<Agente> findByCodigo(String codigo);

    List<Agente> findBySituacao(SituacaoAgente situacao);

    List<Agente> findByEspecialidade(String especialidade);

    List<Agente> findBySituacaoAndEspecialidade(SituacaoAgente situacao, String especialidade);
}
