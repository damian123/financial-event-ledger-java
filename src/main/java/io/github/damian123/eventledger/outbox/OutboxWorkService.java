package io.github.damian123.eventledger.outbox;

import io.github.damian123.eventledger.audit.AuditService;
import io.github.damian123.eventledger.config.LedgerProperties;
import io.github.damian123.eventledger.domain.AuditAction;
import io.github.damian123.eventledger.domain.OutboxMessage;
import io.github.damian123.eventledger.domain.OutboxStatus;
import io.github.damian123.eventledger.persist.OutboxRepository;
import io.github.damian123.eventledger.retry.DeadLetterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Service
public class OutboxWorkService {

    private static final Logger log = LoggerFactory.getLogger(OutboxWorkService.class);

    private final OutboxRepository outboxRepository;
    private final DownstreamPublisher publisher;
    private final DeadLetterService deadLetterService;
    private final AuditService auditService;
    private final LedgerProperties properties;
    private final Clock clock;

    public OutboxWorkService(
            OutboxRepository outboxRepository,
            DownstreamPublisher publisher,
            DeadLetterService deadLetterService,
            AuditService auditService,
            LedgerProperties properties,
            Clock clock
    ) {
        this.outboxRepository = outboxRepository;
        this.publisher = publisher;
        this.deadLetterService = deadLetterService;
        this.auditService = auditService;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public int processPending() {
        List<OutboxMessage> batch = outboxRepository.lockByStatus(
                OutboxStatus.PENDING,
                PageRequest.of(0, properties.getBatchSize())
        );
        int published = 0;
        for (OutboxMessage message : batch) {
            try {
                publisher.publish(new PublishedEvent(message.getId(), message.getEventId(), message.getPayload()));
                message.markPublished(clock.instant());
                outboxRepository.save(message);
                auditService.append(message.getEventId(), AuditAction.PUBLISH, "Outbox message published");
                published++;
            } catch (RuntimeException ex) {
                message.recordFailure(ex.getMessage());
                outboxRepository.save(message);
                log.warn("Outbox publish failed for {} attempt {}", message.getEventId(), message.getAttemptCount());
                if (message.getAttemptCount() >= properties.getMaxAttempts()) {
                    deadLetterService.captureFromOutbox(message);
                }
            }
        }
        return published;
    }
}
