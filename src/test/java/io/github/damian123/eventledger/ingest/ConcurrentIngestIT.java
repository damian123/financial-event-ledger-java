package io.github.damian123.eventledger.ingest;

import io.github.damian123.eventledger.api.dto.AccountResponse;
import io.github.damian123.eventledger.domain.AccountType;
import io.github.damian123.eventledger.persist.JournalLineRepository;
import io.github.damian123.eventledger.persist.OutboxRepository;
import io.github.damian123.eventledger.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ConcurrentIngestIT extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JournalLineRepository journalLineRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @Test
    void concurrentDuplicateIngestDoesNotDoublePost() throws Exception {
        AccountResponse cash = openAccount("CASH", AccountType.ASSET, "USD");
        AccountResponse clearing = openAccount("CLR", AccountType.ASSET, "USD");
        String eventId = "evt-" + UUID.randomUUID();
        String key = "idem-" + eventId;

        int workers = 8;
        ExecutorService pool = Executors.newFixedThreadPool(workers);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(workers);
        AtomicInteger created = new AtomicInteger();
        AtomicInteger replayed = new AtomicInteger();
        AtomicInteger other = new AtomicInteger();

        for (int i = 0; i < workers; i++) {
            pool.submit(() -> {
                try {
                    start.await(10, TimeUnit.SECONDS);
                    RestTemplate client = restTemplate();
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.set("Idempotency-Key", key);
                    Map<String, Object> body = payload(eventId, cash.id(), clearing.id());
                    ResponseEntity<String> response = client.exchange(
                            "http://localhost:" + port + "/api/v1/events",
                            HttpMethod.POST,
                            new HttpEntity<>(body, headers),
                            String.class
                    );
                    if (response.getStatusCode() == HttpStatus.CREATED) {
                        created.incrementAndGet();
                    } else if (response.getStatusCode() == HttpStatus.OK) {
                        replayed.incrementAndGet();
                    } else {
                        other.incrementAndGet();
                    }
                } catch (Exception ex) {
                    other.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();

        assertThat(created.get()).isEqualTo(1);
        assertThat(replayed.get()).isEqualTo(workers - 1);
        assertThat(other.get()).isZero();
        assertThat(journalLineRepository.findByEventIdOrderByPostedAtAsc(eventId)).hasSize(2);
        assertThat(outboxRepository.findByEventId(eventId)).hasSize(1);
    }

    private static Map<String, Object> payload(String eventId, UUID accountId, UUID counterAccountId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("eventId", eventId);
        body.put("accountId", accountId.toString());
        body.put("counterAccountId", counterAccountId.toString());
        body.put("amount", "18.00");
        body.put("currency", "USD");
        body.put("type", "POSTING");
        body.put("occurredAt", "2026-03-01T12:00:00Z");
        body.put("description", "Concurrent duplicate");
        return body;
    }

    private static RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(8000);
        return new RestTemplate(factory);
    }
}
