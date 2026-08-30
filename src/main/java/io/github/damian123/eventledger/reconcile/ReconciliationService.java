package io.github.damian123.eventledger.reconcile;

import io.github.damian123.eventledger.api.dto.ReconciliationResponse;
import io.github.damian123.eventledger.api.error.ApiException;
import io.github.damian123.eventledger.audit.AuditService;
import io.github.damian123.eventledger.domain.AuditAction;
import io.github.damian123.eventledger.domain.EventStatus;
import io.github.damian123.eventledger.domain.FinancialEvent;
import io.github.damian123.eventledger.domain.FindingKind;
import io.github.damian123.eventledger.domain.JournalDirection;
import io.github.damian123.eventledger.domain.JournalLine;
import io.github.damian123.eventledger.domain.Reconciliation;
import io.github.damian123.eventledger.domain.ReconciliationFinding;
import io.github.damian123.eventledger.persist.FinancialEventRepository;
import io.github.damian123.eventledger.persist.JournalLineRepository;
import io.github.damian123.eventledger.persist.ReconciliationFindingRepository;
import io.github.damian123.eventledger.persist.ReconciliationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReconciliationService {

    private final FinancialEventRepository eventRepository;
    private final JournalLineRepository journalLineRepository;
    private final ReconciliationRepository reconciliationRepository;
    private final ReconciliationFindingRepository findingRepository;
    private final AuditService auditService;
    private final Clock clock;

    public ReconciliationService(
            FinancialEventRepository eventRepository,
            JournalLineRepository journalLineRepository,
            ReconciliationRepository reconciliationRepository,
            ReconciliationFindingRepository findingRepository,
            AuditService auditService,
            Clock clock
    ) {
        this.eventRepository = eventRepository;
        this.journalLineRepository = journalLineRepository;
        this.reconciliationRepository = reconciliationRepository;
        this.findingRepository = findingRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public ReconciliationResponse run() {
        Reconciliation reconciliation = new Reconciliation(UUID.randomUUID(), clock.instant());
        reconciliationRepository.save(reconciliation);

        List<FinancialEvent> accepted = eventRepository.findByStatus(EventStatus.ACCEPTED);
        List<JournalLine> lines = journalLineRepository.findAll();
        Map<String, List<JournalLine>> linesByEvent = lines.stream()
                .collect(Collectors.groupingBy(JournalLine::getEventId));

        BigDecimal debitTotal = BigDecimal.ZERO.setScale(2);
        BigDecimal creditTotal = BigDecimal.ZERO.setScale(2);
        for (JournalLine line : lines) {
            if (line.getDirection() == JournalDirection.DEBIT) {
                debitTotal = debitTotal.add(line.getAmount());
            } else {
                creditTotal = creditTotal.add(line.getAmount());
            }
        }
        BigDecimal acceptedAmount = accepted.stream()
                .map(FinancialEvent::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2);

        List<ReconciliationFinding> findings = new ArrayList<>();
        Set<String> unbalancedReported = new HashSet<>();

        for (FinancialEvent event : accepted) {
            List<JournalLine> eventLines = linesByEvent.getOrDefault(event.getEventId(), List.of());
            if (eventLines.isEmpty()) {
                findings.add(finding(
                        reconciliation.getId(),
                        FindingKind.MISSING_JOURNAL,
                        event.getEventId(),
                        "Accepted event has no journal lines"
                ));
                continue;
            }
            if (unbalanced(eventLines)) {
                unbalancedReported.add(event.getEventId());
                findings.add(finding(
                        reconciliation.getId(),
                        FindingKind.UNBALANCED,
                        event.getEventId(),
                        "Debit and credit totals for the event do not match"
                ));
            }
        }

        for (Map.Entry<String, List<JournalLine>> entry : linesByEvent.entrySet()) {
            if (!unbalancedReported.contains(entry.getKey()) && unbalanced(entry.getValue())) {
                findings.add(finding(
                        reconciliation.getId(),
                        FindingKind.UNBALANCED,
                        entry.getKey(),
                        "Journal lines for event id do not balance"
                ));
            }
        }

        for (String duplicateEventId : eventRepository.findDuplicateEventIds()) {
            findings.add(finding(
                    reconciliation.getId(),
                    FindingKind.DUPLICATE_EVENT_ID,
                    duplicateEventId,
                    "event_id appears more than once"
            ));
        }

        if (debitTotal.compareTo(creditTotal) != 0) {
            boolean already = findings.stream().anyMatch(f -> f.getKind() == FindingKind.UNBALANCED);
            if (!already) {
                findings.add(finding(
                        reconciliation.getId(),
                        FindingKind.UNBALANCED,
                        null,
                        "Control totals: debit " + debitTotal.toPlainString() + " credit " + creditTotal.toPlainString()
                ));
            }
        }

        findingRepository.saveAll(findings);
        reconciliation.complete(
                clock.instant(),
                accepted.size(),
                lines.size(),
                debitTotal,
                creditTotal,
                acceptedAmount,
                findings.size()
        );
        reconciliationRepository.save(reconciliation);
        auditService.append(null, AuditAction.RECONCILE,
                "Reconciliation " + reconciliation.getId() + " status=" + reconciliation.getStatus()
                        + " findings=" + findings.size());
        return ReconciliationResponse.from(reconciliation, findings);
    }

    @Transactional(readOnly = true)
    public ReconciliationResponse get(UUID id) {
        Reconciliation reconciliation = reconciliationRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RECONCILIATION_NOT_FOUND", "Reconciliation not found"));
        List<ReconciliationFinding> findings = findingRepository.findByReconciliationIdOrderByKindAsc(id);
        return ReconciliationResponse.from(reconciliation, findings);
    }

    private static boolean unbalanced(List<JournalLine> lines) {
        BigDecimal debit = BigDecimal.ZERO;
        BigDecimal credit = BigDecimal.ZERO;
        for (JournalLine line : lines) {
            if (line.getDirection() == JournalDirection.DEBIT) {
                debit = debit.add(line.getAmount());
            } else {
                credit = credit.add(line.getAmount());
            }
        }
        return debit.compareTo(credit) != 0;
    }

    private static ReconciliationFinding finding(UUID reconciliationId, FindingKind kind, String eventId, String details) {
        return new ReconciliationFinding(UUID.randomUUID(), reconciliationId, kind, eventId, details);
    }
}
