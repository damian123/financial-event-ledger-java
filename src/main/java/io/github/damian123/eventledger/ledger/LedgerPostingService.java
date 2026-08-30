package io.github.damian123.eventledger.ledger;

import io.github.damian123.eventledger.domain.EventType;
import io.github.damian123.eventledger.domain.FinancialEvent;
import io.github.damian123.eventledger.domain.JournalDirection;
import io.github.damian123.eventledger.domain.JournalLine;
import io.github.damian123.eventledger.persist.JournalLineRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
public class LedgerPostingService {

    private final JournalLineRepository journalLineRepository;
    private final Clock clock;

    public LedgerPostingService(JournalLineRepository journalLineRepository, Clock clock) {
        this.journalLineRepository = journalLineRepository;
        this.clock = clock;
    }

    /**
     * Posts a balanced debit/credit pair. POSTING and FEE debit the primary account;
     * REVERSAL credits it so a later reverse of the same amount offsets the original.
     */
    public List<JournalLine> post(FinancialEvent event) {
        JournalDirection primaryDirection = event.getType() == EventType.REVERSAL
                ? JournalDirection.CREDIT
                : JournalDirection.DEBIT;
        JournalDirection counterDirection = primaryDirection == JournalDirection.DEBIT
                ? JournalDirection.CREDIT
                : JournalDirection.DEBIT;

        JournalLine primary = new JournalLine(
                UUID.randomUUID(),
                event.getId(),
                event.getEventId(),
                event.getAccountId(),
                primaryDirection,
                event.getAmount(),
                event.getCurrency(),
                event.getDescription(),
                clock.instant()
        );
        JournalLine counter = new JournalLine(
                UUID.randomUUID(),
                event.getId(),
                event.getEventId(),
                event.getCounterAccountId(),
                counterDirection,
                event.getAmount(),
                event.getCurrency(),
                event.getDescription(),
                clock.instant()
        );
        return journalLineRepository.saveAll(List.of(primary, counter));
    }
}
