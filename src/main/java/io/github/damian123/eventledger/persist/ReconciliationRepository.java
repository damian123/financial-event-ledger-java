package io.github.damian123.eventledger.persist;

import io.github.damian123.eventledger.domain.Reconciliation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReconciliationRepository extends JpaRepository<Reconciliation, UUID> {
}
