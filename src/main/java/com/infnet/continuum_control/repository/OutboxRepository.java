package com.infnet.continuum_control.repository;

import com.infnet.continuum_control.model.entities.MensagemOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OutboxRepository extends JpaRepository<MensagemOutbox, Long> {

    /**
     * Trava a próxima linha pendente que já pode ser tentada. SKIP LOCKED faz várias instâncias
     * entregarem ao mesmo tempo sem pegar a mesma linha (doc 04, 6.3).
     */
    @Query(value = """
            SELECT * FROM outbox
            WHERE processado_em IS NULL AND proxima_tentativa <= :agora
            ORDER BY proxima_tentativa
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<MensagemOutbox> travarProximaPendente(LocalDateTime agora);

    long countByProcessadoEmIsNull();

    @Query("select min(o.criadoEm) from MensagemOutbox o where o.processadoEm is null")
    Optional<LocalDateTime> criadoEmDaPendenteMaisAntiga();

    @Modifying
    @Query("delete from MensagemOutbox o where o.processadoEm is not null and o.processadoEm < :limite")
    int apagarProcessadasAntesDe(LocalDateTime limite);
}
