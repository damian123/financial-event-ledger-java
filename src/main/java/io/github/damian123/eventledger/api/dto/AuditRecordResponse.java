package io.github.damian123.eventledger.api.dto;

import io.github.damian123.eventledger.domain.AuditAction;
import io.github.damian123.eventledger.domain.AuditRecord;

import java.time.Instant;
import java.util.UUID;

public record AuditRecordResponse(
        UUID id,
        String eventId,
        AuditAction action,
        String details,
        Instant createdAt
) {
    public static AuditRecordResponse from(AuditRecord record) {
        return new AuditRecordResponse(
                record.getId(),
                record.getEventId(),
                record.getAction(),
                record.getDetails(),
                record.getCreatedAt()
        );
    }
}
