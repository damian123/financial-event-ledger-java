package io.github.damian123.eventledger.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AccountLedgerResponse(
        UUID accountId,
        String code,
        String currency,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal debitTotal,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal creditTotal,
        List<JournalLineResponse> lines
) {
}
