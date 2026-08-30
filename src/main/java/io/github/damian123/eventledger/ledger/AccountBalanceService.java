package io.github.damian123.eventledger.ledger;

import io.github.damian123.eventledger.api.dto.AccountBalanceResponse;
import io.github.damian123.eventledger.api.dto.AccountLedgerResponse;
import io.github.damian123.eventledger.api.dto.JournalLineResponse;
import io.github.damian123.eventledger.api.error.ApiException;
import io.github.damian123.eventledger.domain.Account;
import io.github.damian123.eventledger.domain.JournalLine;
import io.github.damian123.eventledger.persist.AccountRepository;
import io.github.damian123.eventledger.persist.JournalLineRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
public class AccountBalanceService {

    private final AccountRepository accountRepository;
    private final JournalLineRepository journalLineRepository;
    private final Clock clock;

    public AccountBalanceService(
            AccountRepository accountRepository,
            JournalLineRepository journalLineRepository,
            Clock clock
    ) {
        this.accountRepository = accountRepository;
        this.journalLineRepository = journalLineRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AccountBalanceResponse balance(UUID accountId) {
        Account account = requireAccount(accountId);
        BigDecimal debit = scale(journalLineRepository.debitTotal(accountId));
        BigDecimal credit = scale(journalLineRepository.creditTotal(accountId));
        BigDecimal signed = account.getType().isDebitNormal()
                ? debit.subtract(credit)
                : credit.subtract(debit);
        return new AccountBalanceResponse(
                account.getId(),
                account.getCode(),
                account.getType(),
                account.getCurrency(),
                debit,
                credit,
                signed,
                clock.instant()
        );
    }

    @Transactional(readOnly = true)
    public AccountLedgerResponse ledger(UUID accountId) {
        Account account = requireAccount(accountId);
        List<JournalLine> lines = journalLineRepository.findByAccountIdOrderByPostedAtAsc(accountId);
        BigDecimal debit = scale(journalLineRepository.debitTotal(accountId));
        BigDecimal credit = scale(journalLineRepository.creditTotal(accountId));
        return new AccountLedgerResponse(
                account.getId(),
                account.getCode(),
                account.getCurrency(),
                debit,
                credit,
                lines.stream().map(JournalLineResponse::from).toList()
        );
    }

    private Account requireAccount(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Account not found"));
    }

    private static BigDecimal scale(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2) : value.setScale(2);
    }
}
