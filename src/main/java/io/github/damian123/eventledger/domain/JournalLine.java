package io.github.damian123.eventledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "journal_lines")
public class JournalLine {

    @Id
    private UUID id;

    @Column(name = "event_pk", nullable = false)
    private UUID eventPk;

    @Column(name = "event_id", nullable = false, length = 128)
    private String eventId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private JournalDirection direction;

    @Column(nullable = false, precision = 20, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 512)
    private String description;

    @Column(name = "posted_at", nullable = false)
    private Instant postedAt;

    protected JournalLine() {
    }

    public JournalLine(
            UUID id,
            UUID eventPk,
            String eventId,
            UUID accountId,
            JournalDirection direction,
            BigDecimal amount,
            String currency,
            String description,
            Instant postedAt
    ) {
        this.id = id;
        this.eventPk = eventPk;
        this.eventId = eventId;
        this.accountId = accountId;
        this.direction = direction;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.postedAt = postedAt;
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

    public UUID getAccountId() {
        return accountId;
    }

    public JournalDirection getDirection() {
        return direction;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getDescription() {
        return description;
    }

    public Instant getPostedAt() {
        return postedAt;
    }
}
