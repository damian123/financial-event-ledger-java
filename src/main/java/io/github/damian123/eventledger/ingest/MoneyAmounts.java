package io.github.damian123.eventledger.ingest;

import io.github.damian123.eventledger.api.error.ApiException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;

/**
 * Exact decimal money: scale may be at most 2. Rounding is never applied.
 */
public final class MoneyAmounts {

    private static final Pattern CANONICAL = Pattern.compile("\\d+(\\.\\d+)?");

    private MoneyAmounts() {
    }

    public static BigDecimal parseExactCents(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_AMOUNT", "Amount is required");
        }
        String trimmed = raw.trim();
        if (!CANONICAL.matcher(trimmed).matches()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_AMOUNT",
                    "Amount must be a non-negative decimal without exponent notation"
            );
        }
        BigDecimal value = new BigDecimal(trimmed);
        if (value.scale() > 2) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "AMOUNT_SCALE",
                    "Amount scale must not exceed 2 decimal places; rounding is not applied to money"
            );
        }
        if (value.signum() <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_AMOUNT", "Amount must be greater than zero");
        }
        try {
            return value.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "AMOUNT_SCALE",
                    "Amount cannot be represented at scale 2 without rounding"
            );
        }
    }
}
