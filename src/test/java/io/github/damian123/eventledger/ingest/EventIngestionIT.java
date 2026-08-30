package io.github.damian123.eventledger.ingest;

import io.github.damian123.eventledger.api.dto.AccountResponse;
import io.github.damian123.eventledger.api.dto.AuditRecordResponse;
import io.github.damian123.eventledger.api.dto.EventResponse;
import io.github.damian123.eventledger.domain.AccountType;
import io.github.damian123.eventledger.domain.AuditAction;
import io.github.damian123.eventledger.domain.JournalDirection;
import io.github.damian123.eventledger.domain.OutboxStatus;
import io.github.damian123.eventledger.persist.JournalLineRepository;
import io.github.damian123.eventledger.persist.OutboxRepository;
import io.github.damian123.eventledger.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EventIngestionIT extends AbstractIntegrationTest {

    @Autowired
    private JournalLineRepository journalLineRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @Test
    void ingestPostsBalancedJournalAndOutboxRow() {
        AccountResponse cash = openAccount("CASH", AccountType.ASSET, "USD");
        AccountResponse deposits = openAccount("DEP", AccountType.LIABILITY, "USD");
        String eventId = "evt-" + UUID.randomUUID();

        ResponseEntity<EventResponse> response = ingest(
                "key-" + eventId,
                eventId,
                cash.id(),
                deposits.id(),
                "100.25",
                "POSTING",
                "Customer deposit"
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        EventResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status().name()).isEqualTo("ACCEPTED");
        assertThat(body.journalLines()).hasSize(2);
        assertThat(body.journalLines())
                .extracting(line -> line.direction())
                .containsExactlyInAnyOrder(JournalDirection.DEBIT, JournalDirection.CREDIT);
        assertThat(body.journalLines())
                .allSatisfy(line -> assertThat(line.amount()).isEqualByComparingTo(new BigDecimal("100.25")));

        var debit = body.journalLines().stream().filter(l -> l.direction() == JournalDirection.DEBIT).findFirst().orElseThrow();
        var credit = body.journalLines().stream().filter(l -> l.direction() == JournalDirection.CREDIT).findFirst().orElseThrow();
        assertThat(debit.accountId()).isEqualTo(cash.id());
        assertThat(credit.accountId()).isEqualTo(deposits.id());

        assertThat(journalLineRepository.findByEventIdOrderByPostedAtAsc(eventId)).hasSize(2);
        assertThat(outboxRepository.findByEventId(eventId))
                .hasSize(1)
                .first()
                .satisfies(row -> {
                    assertThat(row.getStatus()).isEqualTo(OutboxStatus.PENDING);
                    assertThat(row.getPayload()).contains(eventId);
                    assertThat(row.getPayload()).contains("100.25");
                });
    }

    @Test
    void identicalPayloadReplaysOriginalResult() {
        AccountResponse cash = openAccount("CASH", AccountType.ASSET, "USD");
        AccountResponse clearing = openAccount("CLR", AccountType.ASSET, "USD");
        String eventId = "evt-" + UUID.randomUUID();
        String key = "idem-" + eventId;

        ResponseEntity<EventResponse> first = ingest(key, eventId, cash.id(), clearing.id(), "40.00", "POSTING", "Same");
        ResponseEntity<EventResponse> second = ingest(key, eventId, cash.id(), clearing.id(), "40.00", "POSTING", "Same");

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(second.getBody()).isNotNull();
        assertThat(first.getBody()).isNotNull();
        assertThat(second.getBody().id()).isEqualTo(first.getBody().id());
        assertThat(journalLineRepository.findByEventIdOrderByPostedAtAsc(eventId)).hasSize(2);
        assertThat(outboxRepository.findByEventId(eventId)).hasSize(1);
    }

    @Test
    void conflictingIdempotencyKeyReturns409() {
        AccountResponse cash = openAccount("CASH", AccountType.ASSET, "USD");
        AccountResponse clearing = openAccount("CLR", AccountType.ASSET, "USD");
        String eventId = "evt-" + UUID.randomUUID();
        String key = "idem-" + eventId;

        ingest(key, eventId, cash.id(), clearing.id(), "12.00", "POSTING", "Original");

        ResponseEntity<ProblemDetail> conflict = rest.exchange(
                "/api/v1/events",
                org.springframework.http.HttpMethod.POST,
                conflictEntity(key, eventId, cash.id(), clearing.id(), "99.00"),
                ProblemDetail.class
        );

        assertThat(conflict.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(conflict.getBody()).isNotNull();
        assertThat(conflict.getBody().getTitle()).isEqualTo("IDEMPOTENCY_CONFLICT");

        ResponseEntity<AuditRecordResponse[]> audit = rest.getForEntity(
                "/api/v1/events/{id}/audit",
                AuditRecordResponse[].class,
                eventId
        );
        assertThat(audit.getBody()).isNotNull();
        assertThat(Arrays.stream(audit.getBody()).map(AuditRecordResponse::action))
                .contains(AuditAction.INGEST, AuditAction.POST, AuditAction.CONFLICT);
    }

    @Test
    void scaleGreaterThanTwoIsRejected() {
        AccountResponse cash = openAccount("CASH", AccountType.ASSET, "USD");
        AccountResponse clearing = openAccount("CLR", AccountType.ASSET, "USD");
        String eventId = "evt-" + UUID.randomUUID();

        ResponseEntity<ProblemDetail> response = rest.exchange(
                "/api/v1/events",
                org.springframework.http.HttpMethod.POST,
                conflictEntity("key-" + eventId, eventId, cash.id(), clearing.id(), "10.123"),
                ProblemDetail.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("AMOUNT_SCALE");
        assertThat(journalLineRepository.findByEventIdOrderByPostedAtAsc(eventId)).isEmpty();
    }

    @Test
    void missingIdempotencyKeyIsRejected() {
        AccountResponse cash = openAccount("CASH", AccountType.ASSET, "USD");
        AccountResponse clearing = openAccount("CLR", AccountType.ASSET, "USD");
        String eventId = "evt-" + UUID.randomUUID();

        ResponseEntity<ProblemDetail> response = rest.exchange(
                "/api/v1/events",
                org.springframework.http.HttpMethod.POST,
                conflictEntity(null, eventId, cash.id(), clearing.id(), "1.00"),
                ProblemDetail.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).containsIgnoringCase("Idempotency-Key");
    }

    @Test
    void auditHistoryIncludesIngestAndPost() {
        AccountResponse cash = openAccount("CASH", AccountType.ASSET, "USD");
        AccountResponse fees = openAccount("FEE", AccountType.REVENUE, "USD");
        String eventId = "evt-" + UUID.randomUUID();
        ingest("key-" + eventId, eventId, cash.id(), fees.id(), "2.50", "FEE", "Wire fee");

        List<AuditRecordResponse> records = Arrays.asList(rest.getForEntity(
                "/api/v1/events/{id}/audit",
                AuditRecordResponse[].class,
                eventId
        ).getBody());
        assertThat(records).extracting(AuditRecordResponse::action).contains(AuditAction.INGEST, AuditAction.POST);
    }

    private static org.springframework.http.HttpEntity<java.util.Map<String, Object>> conflictEntity(
            String key,
            String eventId,
            UUID accountId,
            UUID counterAccountId,
            String amount
    ) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        if (key != null) {
            headers.set("Idempotency-Key", key);
        }
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("eventId", eventId);
        body.put("accountId", accountId.toString());
        body.put("counterAccountId", counterAccountId.toString());
        body.put("amount", amount);
        body.put("currency", "USD");
        body.put("type", "POSTING");
        body.put("occurredAt", "2026-03-01T12:00:00Z");
        body.put("description", "Changed payload");
        return new org.springframework.http.HttpEntity<>(body, headers);
    }
}
