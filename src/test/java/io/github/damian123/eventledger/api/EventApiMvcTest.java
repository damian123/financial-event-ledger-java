package io.github.damian123.eventledger.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.damian123.eventledger.domain.Account;
import io.github.damian123.eventledger.domain.AccountType;
import io.github.damian123.eventledger.persist.AccountRepository;
import io.github.damian123.eventledger.support.SharedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EventApiMvcTest {

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", SharedPostgres.CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", SharedPostgres.CONTAINER::getUsername);
        registry.add("spring.datasource.password", SharedPostgres.CONTAINER::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void swaggerUiAndOpenApiAreAvailable() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Financial Event Ledger"));
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/swagger-ui/index.html"));
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    void ingestViaMockMvcRequiresIdempotencyKeyAndCreatesEvent() throws Exception {
        Account cash = accountRepository.save(new Account(
                UUID.randomUUID(),
                "MVC-CASH-" + UUID.randomUUID().toString().substring(0, 8),
                "MVC cash",
                AccountType.ASSET,
                "USD",
                Instant.now()
        ));
        Account clearing = accountRepository.save(new Account(
                UUID.randomUUID(),
                "MVC-CLR-" + UUID.randomUUID().toString().substring(0, 8),
                "MVC clearing",
                AccountType.ASSET,
                "USD",
                Instant.now()
        ));
        String eventId = "evt-" + UUID.randomUUID();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("eventId", eventId);
        body.put("accountId", cash.getId().toString());
        body.put("counterAccountId", clearing.getId().toString());
        body.put("amount", "6.00");
        body.put("currency", "USD");
        body.put("type", "POSTING");
        body.put("occurredAt", "2026-03-01T12:00:00Z");
        body.put("description", "MockMvc ingest");

        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("Idempotency-Key")));

        mockMvc.perform(post("/api/v1/events")
                        .header("Idempotency-Key", "mvc-" + eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId").value(eventId))
                .andExpect(jsonPath("$.journalLines.length()").value(2));
    }
}
