package io.github.damian123.eventledger.api.dto;

public record IngestOutcome(EventResponse body, boolean replay) {
}
