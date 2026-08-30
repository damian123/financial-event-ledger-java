package io.github.damian123.eventledger.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.damian123.eventledger.domain.AccountType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountBalanceResponse(
        UUID accountId,
        String code,
        AccountType type,
        String currency,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal debitTotal,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal creditTotal,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal signedBalance,
        Instant asOf
) {
}
