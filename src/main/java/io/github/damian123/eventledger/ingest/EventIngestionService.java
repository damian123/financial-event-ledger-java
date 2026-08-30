package io.github.damian123.eventledger.ingest;

import io.github.damian123.eventledger.api.dto.EventResponse;
import io.github.damian123.eventledger.api.dto.IngestEventRequest;
import io.github.damian123.eventledger.api.dto.IngestOutcome;
import io.github.damian123.eventledger.api.error.ApiException;
import io.github.damian123.eventledger.audit.AuditService;
import io.github.damian123.eventledger.domain.FinancialEvent;
import io.github.damian123.eventledger.domain.JournalLine;
import io.github.damian123.eventledger.persist.FinancialEventRepository;
import io.github.damian123.eventledger.persist.JournalLineRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

@Service
public class EventIngestionService {

    private final FinancialEventRepository eventRepository;
    private final JournalLineRepository journalLineRepository;
    private final EventPostingService eventPostingService;
    private final AuditService auditService;

    public EventIngestionService(
            FinancialEventRepository eventRepository,
            JournalLineRepository journalLineRepository,
            EventPostingService eventPostingService,
            AuditService auditService
    ) {
        this.eventRepository = eventRepository;
        this.journalLineRepository = journalLineRepository;
        this.eventPostingService = eventPostingService;
        this.auditService = auditService;
    }

    public IngestOutcome ingest(String idempotencyKey, IngestEventRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MISSING_IDEMPOTENCY_KEY", "Idempotency-Key header is required");
        }
        if (idempotencyKey.length() > 128) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_IDEMPOTENCY_KEY", "Idempotency-Key is too long");
        }
        String currency = Currencies.requireIso4217(request.currency());
        IngestEventRequest normalized = new IngestEventRequest(
                request.eventId().trim(),
                request.accountId(),
                request.counterAccountId(),
                request.amount(),
                currency,
                request.type(),
                request.occurredAt(),
                request.description().trim()
        );
        BigDecimal amount = MoneyAmounts.parseExactCents(normalized.amount());
        String hash = PayloadHasher.sha256(normalized);

        return eventRepository.findByIdempotencyKey(idempotencyKey)
                .map(existing -> replayOrConflict(existing, hash, normalized.eventId()))
                .orElseGet(() -> createOrRecover(idempotencyKey, normalized, hash, amount));
    }

    private IngestOutcome createOrRecover(
            String idempotencyKey,
            IngestEventRequest request,
            String hash,
            BigDecimal amount
    ) {
        try {
            EventResponse created = eventPostingService.acceptNew(idempotencyKey, request, hash, amount);
            return new IngestOutcome(created, false);
        } catch (RuntimeException ex) {
            if (!isUniqueViolation(ex)) {
                throw ex;
            }
            return eventRepository.findByIdempotencyKey(idempotencyKey)
                    .map(existing -> replayOrConflict(existing, hash, request.eventId()))
                    .or(() -> eventRepository.findByEventId(request.eventId()).map(existing -> {
                        throw new ApiException(
                                HttpStatus.CONFLICT,
                                "DUPLICATE_EVENT_ID",
                                "Event id already exists under a different idempotency key"
                        );
                    }))
                    .orElseThrow(() -> ex);
        }
    }

    private IngestOutcome replayOrConflict(FinancialEvent existing, String hash, String eventId) {
        if (!existing.getPayloadHash().equals(hash)) {
            auditService.append(eventId, io.github.damian123.eventledger.domain.AuditAction.CONFLICT,
                    "Idempotency key reused with a different payload");
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "IDEMPOTENCY_CONFLICT",
                    "Idempotency-Key was reused with a different payload"
            );
        }
        List<JournalLine> lines = journalLineRepository.findByEventIdOrderByPostedAtAsc(existing.getEventId());
        return new IngestOutcome(EventResponse.from(existing, lines), true);
    }

    @Transactional(readOnly = true)
    public EventResponse get(String eventId) {
        FinancialEvent event = eventRepository.findByEventId(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "EVENT_NOT_FOUND", "Event not found"));
        List<JournalLine> lines = journalLineRepository.findByEventIdOrderByPostedAtAsc(event.getEventId());
        return EventResponse.from(event, lines);
    }

    private static boolean isUniqueViolation(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SQLException sql && "23505".equals(sql.getSQLState())) {
                return true;
            }
            if (current instanceof org.hibernate.JDBCException jdbc && "23505".equals(jdbc.getSQLState())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
