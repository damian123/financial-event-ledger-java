package io.github.damian123.eventledger.api.dto;

import io.github.damian123.eventledger.domain.Account;
import io.github.damian123.eventledger.domain.AccountType;

import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String code,
        String name,
        AccountType type,
        String currency,
        Instant createdAt
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getCode(),
                account.getName(),
                account.getType(),
                account.getCurrency(),
                account.getCreatedAt()
        );
    }
}
