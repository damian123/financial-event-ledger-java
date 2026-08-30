package io.github.damian123.eventledger.ingest;

import io.github.damian123.eventledger.api.error.ApiException;
import org.springframework.http.HttpStatus;

import java.util.Currency;

public final class Currencies {

    private Currencies() {
    }

    public static String requireIso4217(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_CURRENCY", "Currency is required");
        }
        String code = raw.trim().toUpperCase();
        try {
            Currency.getInstance(code);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_CURRENCY", "Currency must be an ISO-4217 code");
        }
        return code;
    }
}
