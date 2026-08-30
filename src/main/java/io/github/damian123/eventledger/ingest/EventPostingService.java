package io.github.damian123.eventledger.ingest;

import io.github.damian123.eventledger.api.dto.EventResponse;
import io.github.damian123.eventledger.api.dto.IngestEventRequest;
import io.github.damian123.eventledger.api.error.ApiException;
import io.github.damian123.eventledger.audit.AuditService;
import io.github.damian123.eventledger.domain.Account;
import io.github.damian123.eventledger.domain.AuditAction;
import io.github.damian123.eventledger.domain.EventStatus;
import io.github.damian123.eventledger.domain.EventType;
import io.github.damian123.eventledger.domain.FinancialEvent;
import io.github.damian123.eventledger.domain.JournalLine;
import io.github.damian123.eventledger.ledger.LedgerPostingService;
import io.github.damian123.eventledger.outbox.OutboxService;
import io.github.damian123.eventledger.persist.AccountRepository;
import io.github.damian123.eventledger.persist.FinancialEventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
public class EventPostingService {

    private final FinancialEventRepository eventRepository;
    private final AccountRepository accountRepository;
    private final LedgerPostingService ledgerPostingService;
    private final OutboxService outboxService;
    private final AuditService auditService;
    private final Clock clock;

    public EventPostingService(
            FinancialEventRepository eventRepository,
            AccountRepository accountRepository,
            LedgerPostingService ledgerPostingService,
            OutboxService outboxService,
            AuditService auditService,
            Clock clock
    ) {
        this.eventRepository = eventRepository;
        this.accountRepository = accountRepository;
        this.ledgerPostingService = ledgerPostingService;
        this.outboxService = outboxService;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public EventResponse acceptNew(String idempotencyKey, IngestEventRequest request, String payloadHash, BigDecimal amount) {
        Account account = requireAccount(request.accountId());
        Account counter = requireAccount(request.counterAccountId());
        if (account.getId().equals(counter.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SAME_ACCOUNT", "Account and counter-account must differ");
        }
        if (!account.getCurrency().equals(request.currency()) || !counter.getCurrency().equals(request.currency())) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "CURRENCY_MISMATCH",
                    "Event currency must match both ledger accounts"
            );
        }

        FinancialEvent event = new FinancialEvent(
                UUID.randomUUID(),
                request.eventId(),
                idempotencyKey,
                account.getId(),
                counter.getId(),
                amount,
                request.currency(),
                request.type(),
                request.occurredAt(),
                request.description(),
                payloadHash,
                EventStatus.ACCEPTED,
                clock.instant()
        );
        eventRepository.saveAndFlush(event);

        List<JournalLine> lines = ledgerPostingService.post(event);
        outboxService.enqueue(event, lines);
        auditService.append(event.getEventId(), AuditAction.INGEST, "Accepted " + event.getType() + " for " + amount.toPlainString() + " " + event.getCurrency());
        AuditAction postAction = event.getType() == EventType.REVERSAL ? AuditAction.REVERSE : AuditAction.POST;
        auditService.append(event.getEventId(), postAction, "Posted balanced debit/credit pair");
        return EventResponse.from(event, lines);
    }

    private Account requireAccount(UUID id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ACCOUNT_NOT_FOUND", "Unknown account " + id));
    }
}
