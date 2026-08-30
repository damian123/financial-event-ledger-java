package io.github.damian123.eventledger.api;

import io.github.damian123.eventledger.api.dto.AuditRecordResponse;
import io.github.damian123.eventledger.api.dto.EventResponse;
import io.github.damian123.eventledger.api.dto.IngestEventRequest;
import io.github.damian123.eventledger.api.dto.IngestOutcome;
import io.github.damian123.eventledger.audit.AuditService;
import io.github.damian123.eventledger.ingest.EventIngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/events")
@Tag(name = "Events")
public class EventController {

    private final EventIngestionService eventIngestionService;
    private final AuditService auditService;

    public EventController(EventIngestionService eventIngestionService, AuditService auditService) {
        this.eventIngestionService = eventIngestionService;
        this.auditService = auditService;
    }

    @PostMapping
    @Operation(summary = "Ingest a financial posting event")
    @ApiResponse(responseCode = "201", description = "Event accepted and journaled")
    @ApiResponse(responseCode = "200", description = "Idempotent replay of an identical payload")
    @ApiResponse(responseCode = "409", description = "Idempotency key reused with a different payload")
    public ResponseEntity<EventResponse> ingest(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody IngestEventRequest request
    ) {
        IngestOutcome outcome = eventIngestionService.ingest(idempotencyKey, request);
        HttpStatus status = outcome.replay() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status)
                .header("Idempotency-Key", idempotencyKey)
                .body(outcome.body());
    }

    @GetMapping("/{eventId}")
    @Operation(summary = "Fetch an ingested event and its journal lines")
    public EventResponse get(@PathVariable String eventId) {
        return eventIngestionService.get(eventId);
    }

    @GetMapping("/{eventId}/audit")
    @Operation(summary = "Append-only audit history for an event")
    public List<AuditRecordResponse> audit(@PathVariable String eventId) {
        return auditService.history(eventId).stream().map(AuditRecordResponse::from).toList();
    }
}
