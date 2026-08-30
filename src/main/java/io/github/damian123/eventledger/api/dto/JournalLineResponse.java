package io.github.damian123.eventledger.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.damian123.eventledger.domain.JournalDirection;
import io.github.damian123.eventledger.domain.JournalLine;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record JournalLineResponse(
        UUID id,
        String eventId,
        UUID accountId,
        JournalDirection direction,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount,
        String currency,
        String description,
        Instant postedAt
) {
    public static JournalLineResponse from(JournalLine line) {
        return new JournalLineResponse(
                line.getId(),
                line.getEventId(),
                line.getAccountId(),
                line.getDirection(),
                line.getAmount(),
                line.getCurrency(),
                line.getDescription(),
                line.getPostedAt()
        );
    }
}
