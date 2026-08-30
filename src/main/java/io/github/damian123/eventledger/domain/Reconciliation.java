package io.github.damian123.eventledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reconciliations")
public class Reconciliation {

    @Id
    private UUID id;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "event_count", nullable = false)
    private int eventCount;

    @Column(name = "journal_line_count", nullable = false)
    private int journalLineCount;

    @Column(name = "debit_total", nullable = false, precision = 20, scale = 2)
    private BigDecimal debitTotal;

    @Column(name = "credit_total", nullable = false, precision = 20, scale = 2)
    private BigDecimal creditTotal;

    @Column(name = "accepted_event_amount_total", nullable = false, precision = 20, scale = 2)
    private BigDecimal acceptedEventAmountTotal;

    @Column(name = "finding_count", nullable = false)
    private int findingCount;

    protected Reconciliation() {
    }

    public Reconciliation(UUID id, Instant startedAt) {
        this.id = id;
        this.startedAt = startedAt;
        this.status = "RUNNING";
        this.debitTotal = BigDecimal.ZERO.setScale(2);
        this.creditTotal = BigDecimal.ZERO.setScale(2);
        this.acceptedEventAmountTotal = BigDecimal.ZERO.setScale(2);
    }

    public void complete(
            Instant completedAt,
            int eventCount,
            int journalLineCount,
            BigDecimal debitTotal,
            BigDecimal creditTotal,
            BigDecimal acceptedEventAmountTotal,
            int findingCount
    ) {
        this.completedAt = completedAt;
        this.eventCount = eventCount;
        this.journalLineCount = journalLineCount;
        this.debitTotal = debitTotal;
        this.creditTotal = creditTotal;
        this.acceptedEventAmountTotal = acceptedEventAmountTotal;
        this.findingCount = findingCount;
        this.status = findingCount == 0 ? "CLEAN" : "FINDINGS";
    }

    public UUID getId() {
        return id;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getStatus() {
        return status;
    }

    public int getEventCount() {
        return eventCount;
    }

    public int getJournalLineCount() {
        return journalLineCount;
    }

    public BigDecimal getDebitTotal() {
        return debitTotal;
    }

    public BigDecimal getCreditTotal() {
        return creditTotal;
    }

    public BigDecimal getAcceptedEventAmountTotal() {
        return acceptedEventAmountTotal;
    }

    public int getFindingCount() {
        return findingCount;
    }
}
