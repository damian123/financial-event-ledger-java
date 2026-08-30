package io.github.damian123.eventledger.persist;

import io.github.damian123.eventledger.domain.ReconciliationFinding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReconciliationFindingRepository extends JpaRepository<ReconciliationFinding, UUID> {

    List<ReconciliationFinding> findByReconciliationIdOrderByKindAsc(UUID reconciliationId);
}
