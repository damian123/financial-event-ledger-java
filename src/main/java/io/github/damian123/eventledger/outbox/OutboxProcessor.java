package io.github.damian123.eventledger.outbox;

import io.github.damian123.eventledger.config.LedgerProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxProcessor {

    private final OutboxWorkService outboxWorkService;
    private final LedgerProperties properties;

    public OutboxProcessor(OutboxWorkService outboxWorkService, LedgerProperties properties) {
        this.outboxWorkService = outboxWorkService;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${eventledger.outbox.poll-interval-ms:1000}")
    public void scheduledDrain() {
        if (!properties.isEnabled()) {
            return;
        }
        outboxWorkService.processPending();
    }

    public int processPending() {
        return outboxWorkService.processPending();
    }
}
