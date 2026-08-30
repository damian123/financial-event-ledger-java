package io.github.damian123.eventledger.reconcile;

import io.github.damian123.eventledger.api.dto.AccountResponse;
import io.github.damian123.eventledger.api.dto.EventResponse;
import io.github.damian123.eventledger.api.dto.ReconciliationResponse;
import io.github.damian123.eventledger.domain.AccountType;
import io.github.damian123.eventledger.domain.FindingKind;
import io.github.damian123.eventledger.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReconciliationIT extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void cleanRunHasMatchingControlTotals() {
        AccountResponse cash = openAccount("CASH", AccountType.ASSET, "USD");
        AccountResponse deposits = openAccount("DEP", AccountType.LIABILITY, "USD");
        String eventId = "evt-" + UUID.randomUUID();
        ResponseEntity<EventResponse> ingested = ingest(
                "key-" + eventId, eventId, cash.id(), deposits.id(), "20.00", "POSTING", "Clean posting"
        );
        assertThat(ingested.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<ReconciliationResponse> report = rest.postForEntity(
                "/api/v1/reconciliations/run",
                null,
                ReconciliationResponse.class
        );
        assertThat(report.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(report.getBody()).isNotNull();
        assertThat(report.getBody().debitTotal()).isEqualByComparingTo(report.getBody().creditTotal());
        assertThat(report.getBody().findings())
                .noneMatch(finding -> eventId.equals(finding.eventId()));
    }

    @Test
    void detectsForcedImbalanceAndDuplicateEventIds() {
        AccountResponse cash = openAccount("CASH", AccountType.ASSET, "USD");
        AccountResponse deposits = openAccount("DEP", AccountType.LIABILITY, "USD");
        String eventId = "evt-" + UUID.randomUUID();
        EventResponse accepted = ingest(
                "key-" + eventId, eventId, cash.id(), deposits.id(), "15.00", "POSTING", "To unbalance"
        ).getBody();
        assertThat(accepted).isNotNull();

        jdbcTemplate.update("""
                        INSERT INTO journal_lines
                            (id, event_pk, event_id, account_id, direction, amount, currency, description, posted_at)
                        VALUES (?, ?, ?, ?, 'DEBIT', 1.00, 'USD', 'forced imbalance', now())
                        """,
                UUID.randomUUID(),
                accepted.id(),
                eventId,
                cash.id()
        );

        jdbcTemplate.update("ALTER TABLE events DROP CONSTRAINT uq_events_event_id");
        jdbcTemplate.update("""
                        INSERT INTO events (
                            id, event_id, idempotency_key, account_id, counter_account_id, amount, currency, type,
                            occurred_at, description, payload_hash, status, attempt_count, created_at
                        ) VALUES (
                            ?, ?, ?, ?, ?, 15.00, 'USD', 'POSTING', now(), 'duplicate fixture',
                            'ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff', 'ACCEPTED', 0, now()
                        )
                        """,
                UUID.randomUUID(),
                eventId,
                "dup-key-" + UUID.randomUUID(),
                cash.id(),
                deposits.id()
        );

        try {
            ReconciliationResponse report = rest.postForEntity(
                    "/api/v1/reconciliations/run",
                    null,
                    ReconciliationResponse.class
            ).getBody();
            assertThat(report).isNotNull();
            assertThat(report.status()).isEqualTo("FINDINGS");
            assertThat(report.findings())
                    .anySatisfy(finding -> {
                        assertThat(finding.kind()).isEqualTo(FindingKind.UNBALANCED);
                        assertThat(finding.eventId()).isEqualTo(eventId);
                    });
            assertThat(report.findings())
                    .anySatisfy(finding -> {
                        assertThat(finding.kind()).isEqualTo(FindingKind.DUPLICATE_EVENT_ID);
                        assertThat(finding.eventId()).isEqualTo(eventId);
                    });
            assertThat(report.debitTotal()).isNotEqualByComparingTo(report.creditTotal());
        } finally {
            jdbcTemplate.update("DELETE FROM events WHERE payload_hash = 'ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff'");
            jdbcTemplate.execute("""
                    DO $$
                    BEGIN
                      IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_events_event_id') THEN
                        ALTER TABLE events ADD CONSTRAINT uq_events_event_id UNIQUE (event_id);
                      END IF;
                    END $$;
                    """);
        }
    }
}
