package io.github.damian123.eventledger.ingest;

import io.github.damian123.eventledger.api.error.ApiException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyAmountsTest {

    @Test
    void acceptsWholeUnitsAndPadsScale() {
        assertThat(MoneyAmounts.parseExactCents("10")).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(MoneyAmounts.parseExactCents("10.5")).isEqualByComparingTo(new BigDecimal("10.50"));
        assertThat(MoneyAmounts.parseExactCents("10.50")).isEqualByComparingTo(new BigDecimal("10.50"));
    }

    @Test
    void rejectsScaleGreaterThanTwo() {
        assertThatThrownBy(() -> MoneyAmounts.parseExactCents("10.123"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("scale");
    }

    @Test
    void rejectsZeroNegativeAndScientificNotation() {
        assertThatThrownBy(() -> MoneyAmounts.parseExactCents("0")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> MoneyAmounts.parseExactCents("-1.00")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> MoneyAmounts.parseExactCents("1e2")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> MoneyAmounts.parseExactCents("abc")).isInstanceOf(ApiException.class);
    }
}
