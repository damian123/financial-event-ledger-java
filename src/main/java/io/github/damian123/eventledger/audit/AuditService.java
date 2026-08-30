package io.github.damian123.eventledger.audit;

import io.github.damian123.eventledger.domain.AuditAction;
import io.github.damian123.eventledger.domain.AuditRecord;
import io.github.damian123.eventledger.persist.AuditRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
public class AuditService {

    private final AuditRecordRepository auditRecordRepository;
    private final Clock clock;

    public AuditService(AuditRecordRepository auditRecordRepository, Clock clock) {
        this.auditRecordRepository = auditRecordRepository;
        this.clock = clock;
    }

    @Transactional
    public void append(String eventId, AuditAction action, String details) {
        auditRecordRepository.save(new AuditRecord(
                UUID.randomUUID(),
                eventId,
                action,
                details,
                clock.instant()
        ));
    }

    @Transactional(readOnly = true)
    public List<AuditRecord> history(String eventId) {
        return auditRecordRepository.findByEventIdOrderByCreatedAtAsc(eventId);
    }
}
