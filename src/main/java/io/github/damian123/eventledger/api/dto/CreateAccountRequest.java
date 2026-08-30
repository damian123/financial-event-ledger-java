package io.github.damian123.eventledger.api.dto;

import io.github.damian123.eventledger.domain.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(
        @NotBlank @Size(max = 64) String code,
        @NotBlank @Size(max = 256) String name,
        @NotNull AccountType type,
        @NotBlank @Size(min = 3, max = 3) String currency
) {
}
