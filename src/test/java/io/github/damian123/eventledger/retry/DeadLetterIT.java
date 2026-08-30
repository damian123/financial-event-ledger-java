package io.github.damian123.eventledger.retry;

import io.github.damian123.eventledger.api.dto.AccountResponse;
import io.github.damian123.eventledger.api.dto.DeadLetterResponse;
import io.github.damian123.eventledger.api.dto.EventResponse;
import io.github.damian123.eventledger.domain.AccountType;
import io.github.damian123.eventledger.domain.OutboxStatus;
import io.github.damian123.eventledger.outbox.InMemoryDownstreamPublisher;
import io.github.damian123.eventledger.outbox.OutboxProcessor;
import io.github.damian123.eventledger.persist.OutboxRepository;
import io.github.damian123.eventledger.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DeadLetterIT extends AbstractIntegrationTest {

    @Autowired
    private InMemoryDownstreamPublisher publisher;

    @Autowired
    private OutboxProcessor outboxProcessor;

    @Autowired
    private OutboxRepository outboxRepository;

    @BeforeEach
    void resetPublisher() {
        publisher.clear();
        for (int i = 0; i < 20; i++) {
            if (outboxProcessor.processPending() == 0) {
                break;
            }
        }
        publisher.clear();
    }

    @Test
    void repeatedPublishFailureMovesToDeadLetterAndRetryRequeues() {
        AccountResponse cash = openAccount("CASH", AccountType.ASSET, "USD");
        AccountResponse clearing = openAccount("CLR", AccountType.ASSET, "USD");
        String eventId = "evt-" + UUID.randomUUID();

        publisher.failNext(3);
        ResponseEntity<EventResponse> ingested = ingest(
                "key-" + eventId, eventId, cash.id(), clearing.id(), "5.00", "POSTING", "Poison publish"
        );
        assertThat(ingested.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        assertThat(outboxProcessor.processPending()).isZero();
        assertThat(outboxProcessor.processPending()).isZero();
        assertThat(outboxProcessor.processPending()).isZero();

        assertThat(outboxRepository.findByEventId(eventId))
                .first()
                .satisfies(row -> {
                    assertThat(row.getStatus()).isEqualTo(OutboxStatus.DEAD_LETTERED);
                    assertThat(row.getAttemptCount()).isEqualTo(3);
                });

        DeadLetterResponse[] listed = rest.getForEntity("/api/v1/dead-letters", DeadLetterResponse[].class).getBody();
        assertThat(listed).isNotNull();
        DeadLetterResponse dead = Arrays.stream(listed)
                .filter(item -> eventId.equals(item.eventId()) && item.requeuedAt() == null)
                .findFirst()
                .orElseThrow();
        assertThat(dead.lastError()).contains("injected downstream publish failure");
        assertThat(dead.attemptCount()).isEqualTo(3);

        ResponseEntity<DeadLetterResponse> retried = rest.postForEntity(
                "/api/v1/dead-letters/{id}/retry",
                null,
                DeadLetterResponse.class,
                dead.id()
        );
        assertThat(retried.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(retried.getBody()).isNotNull();
        assertThat(retried.getBody().requeuedAt()).isNotNull();

        assertThat(outboxRepository.findByEventId(eventId))
                .first()
                .satisfies(row -> assertThat(row.getStatus()).isEqualTo(OutboxStatus.PENDING));

        assertThat(outboxProcessor.processPending()).isEqualTo(1);
        assertThat(publisher.publishedFor(eventId)).hasSize(1);
        assertThat(outboxRepository.findByEventId(eventId))
                .first()
                .satisfies(row -> assertThat(row.getStatus()).isEqualTo(OutboxStatus.PUBLISHED));
    }

    @Test
    void successfulPublishLandsInInMemorySink() {
        AccountResponse cash = openAccount("CASH", AccountType.ASSET, "USD");
        AccountResponse clearing = openAccount("CLR", AccountType.ASSET, "USD");
        String eventId = "evt-" + UUID.randomUUID();
        ingest("key-" + eventId, eventId, cash.id(), clearing.id(), "3.33", "POSTING", "Happy publish");

        assertThat(outboxProcessor.processPending()).isGreaterThanOrEqualTo(1);
        assertThat(publisher.publishedFor(eventId)).hasSize(1);
    }
}
