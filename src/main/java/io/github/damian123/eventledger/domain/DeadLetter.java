package io.github.damian123.eventledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dead_letters")
public class DeadLetter {

    @Id
    private UUID id;

    @Column(name = "event_pk")
    private UUID eventPk;

    @Column(name = "event_id", length = 128)
    private String eventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DeadLetterSource source;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "last_error", nullable = false, columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "requeued_at")
    private Instant requeuedAt;

    protected DeadLetter() {
    }

    public DeadLetter(
            UUID id,
            UUID eventPk,
            String eventId,
            DeadLetterSource source,
            String payload,
            String lastError,
            int attemptCount,
            Instant createdAt
    ) {
        this.id = id;
        this.eventPk = eventPk;
        this.eventId = eventId;
        this.source = source;
        this.payload = payload;
        this.lastError = lastError;
        this.attemptCount = attemptCount;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEventPk() {
        return eventPk;
    }

    public String getEventId() {
        return eventId;
    }

    public DeadLetterSource getSource() {
        return source;
    }

    public String getPayload() {
        return payload;
    }

    public String getLastError() {
        return lastError;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getRequeuedAt() {
        return requeuedAt;
    }

    public boolean isOpen() {
        return requeuedAt == null;
    }

    public void markRequeued(Instant at) {
        this.requeuedAt = at;
    }
}
