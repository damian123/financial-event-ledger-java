package io.github.damian123.eventledger.api;

import io.github.damian123.eventledger.api.dto.AccountBalanceResponse;
import io.github.damian123.eventledger.api.dto.AccountLedgerResponse;
import io.github.damian123.eventledger.api.dto.AccountResponse;
import io.github.damian123.eventledger.api.dto.CreateAccountRequest;
import io.github.damian123.eventledger.ledger.AccountBalanceService;
import io.github.damian123.eventledger.ledger.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Accounts")
public class AccountController {

    private final AccountService accountService;
    private final AccountBalanceService accountBalanceService;

    public AccountController(AccountService accountService, AccountBalanceService accountBalanceService) {
        this.accountService = accountService;
        this.accountBalanceService = accountBalanceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Open a ledger account")
    public AccountResponse create(@Valid @RequestBody CreateAccountRequest request) {
        return accountService.create(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch an account")
    public AccountResponse get(@PathVariable UUID id) {
        return accountService.get(id);
    }

    @GetMapping("/{id}/balance")
    @Operation(summary = "Derived account balance from journal lines")
    public AccountBalanceResponse balance(@PathVariable UUID id) {
        return accountBalanceService.balance(id);
    }

    @GetMapping("/{id}/ledger")
    @Operation(summary = "Journal lines for an account")
    public AccountLedgerResponse ledger(@PathVariable UUID id) {
        return accountBalanceService.ledger(id);
    }
}
