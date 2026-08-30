package io.github.damian123.eventledger.api.dto;

import io.github.damian123.eventledger.domain.FindingKind;
import io.github.damian123.eventledger.domain.ReconciliationFinding;

import java.util.UUID;

public record ReconciliationFindingResponse(
        UUID id,
        FindingKind kind,
        String eventId,
        String details
) {
    public static ReconciliationFindingResponse from(ReconciliationFinding finding) {
        return new ReconciliationFindingResponse(
                finding.getId(),
                finding.getKind(),
                finding.getEventId(),
                finding.getDetails()
        );
    }
}
