package io.github.damian123.eventledger.retry;

import io.github.damian123.eventledger.api.error.ApiException;
import io.github.damian123.eventledger.audit.AuditService;
import io.github.damian123.eventledger.domain.AuditAction;
import io.github.damian123.eventledger.domain.DeadLetter;
import io.github.damian123.eventledger.domain.DeadLetterSource;
import io.github.damian123.eventledger.domain.OutboxMessage;
import io.github.damian123.eventledger.persist.DeadLetterRepository;
import io.github.damian123.eventledger.persist.OutboxRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
public class DeadLetterService {

    private final DeadLetterRepository deadLetterRepository;
    private final OutboxRepository outboxRepository;
    private final AuditService auditService;
    private final Clock clock;

    public DeadLetterService(
            DeadLetterRepository deadLetterRepository,
            OutboxRepository outboxRepository,
            AuditService auditService,
            Clock clock
    ) {
        this.deadLetterRepository = deadLetterRepository;
        this.outboxRepository = outboxRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public DeadLetter captureFromOutbox(OutboxMessage message) {
        DeadLetter deadLetter = new DeadLetter(
                UUID.randomUUID(),
                message.getEventPk(),
                message.getEventId(),
                DeadLetterSource.OUTBOX,
                message.getPayload(),
                message.getLastError() == null ? "unknown error" : message.getLastError(),
                message.getAttemptCount(),
                clock.instant()
        );
        message.markDeadLettered();
        outboxRepository.save(message);
        DeadLetter saved = deadLetterRepository.save(deadLetter);
        auditService.append(
                message.getEventId(),
                AuditAction.DEAD_LETTER,
                "Outbox message moved to dead letter after " + message.getAttemptCount() + " attempts: " + saved.getLastError()
        );
        return saved;
    }

    @Transactional(readOnly = true)
    public List<DeadLetter> list() {
        return deadLetterRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public DeadLetter retry(UUID id) {
        DeadLetter deadLetter = deadLetterRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "DEAD_LETTER_NOT_FOUND", "Dead letter not found"));
        if (!deadLetter.isOpen()) {
            throw new ApiException(HttpStatus.CONFLICT, "DEAD_LETTER_REQUEUED", "Dead letter has already been requeued");
        }
        OutboxMessage message = outboxRepository.findByEventPk(deadLetter.getEventPk())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "OUTBOX_NOT_FOUND", "Outbox row for dead letter is missing"));
        message.requeue();
        outboxRepository.save(message);
        deadLetter.markRequeued(clock.instant());
        deadLetterRepository.save(deadLetter);
        auditService.append(deadLetter.getEventId(), AuditAction.RETRY, "Dead letter " + deadLetter.getId() + " requeued");
        return deadLetter;
    }
}
