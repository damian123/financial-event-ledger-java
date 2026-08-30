package io.github.damian123.eventledger.outbox;

import java.util.UUID;

public record PublishedEvent(UUID outboxId, String eventId, String payload) {
}
