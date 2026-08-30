package io.github.damian123.eventledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "reconciliation_findings")
public class ReconciliationFinding {

    @Id
    private UUID id;

    @Column(name = "reconciliation_id", nullable = false)
    private UUID reconciliationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private FindingKind kind;

    @Column(name = "event_id", length = 128)
    private String eventId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String details;

    protected ReconciliationFinding() {
    }

    public ReconciliationFinding(UUID id, UUID reconciliationId, FindingKind kind, String eventId, String details) {
        this.id = id;
        this.reconciliationId = reconciliationId;
        this.kind = kind;
        this.eventId = eventId;
        this.details = details;
    }

    public UUID getId() {
        return id;
    }

    public UUID getReconciliationId() {
        return reconciliationId;
    }

    public FindingKind getKind() {
        return kind;
    }

    public String getEventId() {
        return eventId;
    }

    public String getDetails() {
        return details;
    }
}
