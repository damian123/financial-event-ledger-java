package io.github.damian123.eventledger.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.damian123.eventledger.domain.FinancialEvent;
import io.github.damian123.eventledger.domain.JournalLine;
import io.github.damian123.eventledger.domain.OutboxMessage;
import io.github.damian123.eventledger.persist.OutboxRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public OutboxService(OutboxRepository outboxRepository, ObjectMapper objectMapper, Clock clock) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public OutboxMessage enqueue(FinancialEvent event, List<JournalLine> lines) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("eventId", event.getEventId());
        body.put("eventPk", event.getId().toString());
        body.put("type", event.getType().name());
        body.put("accountId", event.getAccountId().toString());
        body.put("counterAccountId", event.getCounterAccountId().toString());
        body.put("amount", event.getAmount().toPlainString());
        body.put("currency", event.getCurrency());
        body.put("journalLineIds", lines.stream().map(line -> line.getId().toString()).toList());
        String payload;
        try {
            payload = objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize outbox payload", ex);
        }
        return outboxRepository.save(new OutboxMessage(
                UUID.randomUUID(),
                event.getId(),
                event.getEventId(),
                payload,
                clock.instant()
        ));
    }
}
