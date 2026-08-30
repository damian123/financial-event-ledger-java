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
@Table(name = "outbox")
public class OutboxMessage {

    @Id
    private UUID id;

    @Column(name = "event_pk", nullable = false)
    private UUID eventPk;

    @Column(name = "event_id", nullable = false, length = 128)
    private String eventId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OutboxStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    protected OutboxMessage() {
    }

    public OutboxMessage(UUID id, UUID eventPk, String eventId, String payload, Instant createdAt) {
        this.id = id;
        this.eventPk = eventPk;
        this.eventId = eventId;
        this.payload = payload;
        this.status = OutboxStatus.PENDING;
        this.attemptCount = 0;
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

    public String getPayload() {
        return payload;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public String getLastError() {
        return lastError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void markPublished(Instant at) {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = at;
        this.lastError = null;
    }

    public void recordFailure(String error) {
        this.attemptCount += 1;
        this.lastError = truncate(error);
    }

    public void markDeadLettered() {
        this.status = OutboxStatus.DEAD_LETTERED;
    }

    public void requeue() {
        this.status = OutboxStatus.PENDING;
        this.attemptCount = 0;
        this.lastError = null;
        this.publishedAt = null;
    }

    private static String truncate(String error) {
        if (error == null) {
            return "unknown error";
        }
        return error.length() > 2000 ? error.substring(0, 2000) : error;
    }
}
