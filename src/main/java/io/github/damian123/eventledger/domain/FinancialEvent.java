package io.github.damian123.eventledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "events")
public class FinancialEvent {

    @Id
    private UUID id;

    @Column(name = "event_id", nullable = false, unique = true, length = 128)
    private String eventId;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 128)
    private String idempotencyKey;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "counter_account_id", nullable = false)
    private UUID counterAccountId;

    @Column(nullable = false, precision = 20, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private EventType type;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @Column(nullable = false, length = 512)
    private String description;

    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private EventStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected FinancialEvent() {
    }

    public FinancialEvent(
            UUID id,
            String eventId,
            String idempotencyKey,
            UUID accountId,
            UUID counterAccountId,
            BigDecimal amount,
            String currency,
            EventType type,
            OffsetDateTime occurredAt,
            String description,
            String payloadHash,
            EventStatus status,
            Instant createdAt
    ) {
        this.id = id;
        this.eventId = eventId;
        this.idempotencyKey = idempotencyKey;
        this.accountId = accountId;
        this.counterAccountId = counterAccountId;
        this.amount = amount;
        this.currency = currency;
        this.type = type;
        this.occurredAt = occurredAt;
        this.description = description;
        this.payloadHash = payloadHash;
        this.status = status;
        this.attemptCount = 0;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public UUID getCounterAccountId() {
        return counterAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public EventType getType() {
        return type;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }

    public String getDescription() {
        return description;
    }

    public String getPayloadHash() {
        return payloadHash;
    }

    public EventStatus getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void markAccepted() {
        this.status = EventStatus.ACCEPTED;
    }

    public void recordAttempt() {
        this.attemptCount += 1;
    }

    public void markDeadLettered() {
        this.status = EventStatus.DEAD_LETTERED;
    }
}
