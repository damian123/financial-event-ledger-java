package io.github.damian123.eventledger.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.damian123.eventledger.domain.Reconciliation;
import io.github.damian123.eventledger.domain.ReconciliationFinding;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReconciliationResponse(
        UUID id,
        Instant startedAt,
        Instant completedAt,
        String status,
        int eventCount,
        int journalLineCount,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal debitTotal,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal creditTotal,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal acceptedEventAmountTotal,
        int findingCount,
        List<ReconciliationFindingResponse> findings
) {
    public static ReconciliationResponse from(Reconciliation reconciliation, List<ReconciliationFinding> findings) {
        return new ReconciliationResponse(
                reconciliation.getId(),
                reconciliation.getStartedAt(),
                reconciliation.getCompletedAt(),
                reconciliation.getStatus(),
                reconciliation.getEventCount(),
                reconciliation.getJournalLineCount(),
                reconciliation.getDebitTotal(),
                reconciliation.getCreditTotal(),
                reconciliation.getAcceptedEventAmountTotal(),
                reconciliation.getFindingCount(),
                findings.stream().map(ReconciliationFindingResponse::from).toList()
        );
    }
}
