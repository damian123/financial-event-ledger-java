package io.github.damian123.eventledger.api.dto;

import io.github.damian123.eventledger.domain.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

public record IngestEventRequest(
        @NotBlank @Size(max = 128) String eventId,
        @NotNull UUID accountId,
        @NotNull UUID counterAccountId,
        @NotBlank String amount,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @NotNull EventType type,
        @NotNull OffsetDateTime occurredAt,
        @NotBlank @Size(max = 512) String description
) {
}
