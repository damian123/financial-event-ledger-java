package io.github.damian123.eventledger.ledger;

import io.github.damian123.eventledger.api.dto.AccountResponse;
import io.github.damian123.eventledger.api.dto.CreateAccountRequest;
import io.github.damian123.eventledger.api.error.ApiException;
import io.github.damian123.eventledger.domain.Account;
import io.github.damian123.eventledger.ingest.Currencies;
import io.github.damian123.eventledger.persist.AccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final Clock clock;

    public AccountService(AccountRepository accountRepository, Clock clock) {
        this.accountRepository = accountRepository;
        this.clock = clock;
    }

    @Transactional
    public AccountResponse create(CreateAccountRequest request) {
        String currency = Currencies.requireIso4217(request.currency());
        String code = request.code().trim();
        if (accountRepository.existsByCode(code)) {
            throw new ApiException(HttpStatus.CONFLICT, "ACCOUNT_CODE_EXISTS", "Account code already exists");
        }
        Account account = new Account(
                UUID.randomUUID(),
                code,
                request.name().trim(),
                request.type(),
                currency,
                clock.instant()
        );
        return AccountResponse.from(accountRepository.save(account));
    }

    @Transactional(readOnly = true)
    public AccountResponse get(UUID id) {
        return accountRepository.findById(id)
                .map(AccountResponse::from)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Account not found"));
    }
}
