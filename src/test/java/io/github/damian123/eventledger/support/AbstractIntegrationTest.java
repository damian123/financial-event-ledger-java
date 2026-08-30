package io.github.damian123.eventledger.support;

import io.github.damian123.eventledger.api.dto.AccountResponse;
import io.github.damian123.eventledger.api.dto.EventResponse;
import io.github.damian123.eventledger.domain.AccountType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", SharedPostgres.CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", SharedPostgres.CONTAINER::getUsername);
        registry.add("spring.datasource.password", SharedPostgres.CONTAINER::getPassword);
    }

    @Autowired
    protected TestRestTemplate rest;

    protected AccountResponse openAccount(String prefix, AccountType type, String currency) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", prefix + "-" + UUID.randomUUID().toString().substring(0, 8));
        body.put("name", prefix + " account");
        body.put("type", type.name());
        body.put("currency", currency);
        ResponseEntity<AccountResponse> response = rest.postForEntity("/api/v1/accounts", body, AccountResponse.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("Unable to open account: " + response.getStatusCode());
        }
        return response.getBody();
    }

    protected ResponseEntity<EventResponse> ingest(
            String idempotencyKey,
            String eventId,
            UUID accountId,
            UUID counterAccountId,
            String amount,
            String type,
            String description
    ) {
        return ingestAt(
                idempotencyKey,
                eventId,
                accountId,
                counterAccountId,
                amount,
                "USD",
                type,
                "2026-03-01T12:00:00Z",
                description
        );
    }

    protected ResponseEntity<EventResponse> ingestAt(
            String idempotencyKey,
            String eventId,
            UUID accountId,
            UUID counterAccountId,
            String amount,
            String currency,
            String type,
            String occurredAt,
            String description
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (idempotencyKey != null) {
            headers.set("Idempotency-Key", idempotencyKey);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("eventId", eventId);
        body.put("accountId", accountId.toString());
        body.put("counterAccountId", counterAccountId.toString());
        body.put("amount", amount);
        body.put("currency", currency);
        body.put("type", type);
        body.put("occurredAt", occurredAt);
        body.put("description", description);
        return rest.exchange("/api/v1/events", HttpMethod.POST, new HttpEntity<>(body, headers), EventResponse.class);
    }
}
