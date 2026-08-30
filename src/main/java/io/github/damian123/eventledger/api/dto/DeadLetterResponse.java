package io.github.damian123.eventledger.api.dto;

import io.github.damian123.eventledger.domain.DeadLetter;
import io.github.damian123.eventledger.domain.DeadLetterSource;

import java.time.Instant;
import java.util.UUID;

public record DeadLetterResponse(
        UUID id,
        UUID eventPk,
        String eventId,
        DeadLetterSource source,
        String payload,
        String lastError,
        int attemptCount,
        Instant createdAt,
        Instant requeuedAt
) {
    public static DeadLetterResponse from(DeadLetter deadLetter) {
        return new DeadLetterResponse(
                deadLetter.getId(),
                deadLetter.getEventPk(),
                deadLetter.getEventId(),
                deadLetter.getSource(),
                deadLetter.getPayload(),
                deadLetter.getLastError(),
                deadLetter.getAttemptCount(),
                deadLetter.getCreatedAt(),
                deadLetter.getRequeuedAt()
        );
    }
}
