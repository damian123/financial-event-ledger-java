package io.github.damian123.eventledger.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.damian123.eventledger.domain.EventStatus;
import io.github.damian123.eventledger.domain.EventType;
import io.github.damian123.eventledger.domain.FinancialEvent;
import io.github.damian123.eventledger.domain.JournalLine;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record EventResponse(
        UUID id,
        String eventId,
        String idempotencyKey,
        EventStatus status,
        UUID accountId,
        UUID counterAccountId,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount,
        String currency,
        EventType type,
        OffsetDateTime occurredAt,
        String description,
        Instant createdAt,
        List<JournalLineResponse> journalLines
) {
    public static EventResponse from(FinancialEvent event, List<JournalLine> lines) {
        return new EventResponse(
                event.getId(),
                event.getEventId(),
                event.getIdempotencyKey(),
                event.getStatus(),
                event.getAccountId(),
                event.getCounterAccountId(),
                event.getAmount(),
                event.getCurrency(),
                event.getType(),
                event.getOccurredAt(),
                event.getDescription(),
                event.getCreatedAt(),
                lines.stream().map(JournalLineResponse::from).toList()
        );
    }
}
