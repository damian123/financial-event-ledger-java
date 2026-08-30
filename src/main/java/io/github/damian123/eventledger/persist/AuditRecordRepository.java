package io.github.damian123.eventledger.persist;

import io.github.damian123.eventledger.domain.AuditRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditRecordRepository extends JpaRepository<AuditRecord, UUID> {

    List<AuditRecord> findByEventIdOrderByCreatedAtAsc(String eventId);
}
