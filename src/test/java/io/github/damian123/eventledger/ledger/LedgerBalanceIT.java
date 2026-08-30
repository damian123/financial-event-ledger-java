package io.github.damian123.eventledger.ledger;

import io.github.damian123.eventledger.api.dto.AccountBalanceResponse;
import io.github.damian123.eventledger.api.dto.AccountLedgerResponse;
import io.github.damian123.eventledger.api.dto.AccountResponse;
import io.github.damian123.eventledger.api.dto.EventResponse;
import io.github.damian123.eventledger.domain.AccountType;
import io.github.damian123.eventledger.domain.JournalDirection;
import io.github.damian123.eventledger.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LedgerBalanceIT extends AbstractIntegrationTest {

    @Test
    void reversalPostsOffsettingLinesAndRestoresBalance() {
        AccountResponse cash = openAccount("CASH", AccountType.ASSET, "USD");
        AccountResponse clearing = openAccount("CLR", AccountType.ASSET, "USD");

        String postingId = "evt-" + UUID.randomUUID();
        ResponseEntity<EventResponse> posting = ingest(
                "key-" + postingId,
                postingId,
                cash.id(),
                clearing.id(),
                "75.00",
                "POSTING",
                "Outbound transfer"
        );
        assertThat(posting.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        AccountBalanceResponse afterPosting = rest.getForObject(
                "/api/v1/accounts/{id}/balance",
                AccountBalanceResponse.class,
                cash.id()
        );
        assertThat(afterPosting).isNotNull();
        assertThat(afterPosting.signedBalance()).isEqualByComparingTo(new BigDecimal("75.00"));

        String reversalId = "evt-" + UUID.randomUUID();
        ResponseEntity<EventResponse> reversal = ingest(
                "key-" + reversalId,
                reversalId,
                cash.id(),
                clearing.id(),
                "75.00",
                "REVERSAL",
                "Reverse outbound transfer"
        );
        assertThat(reversal.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(reversal.getBody()).isNotNull();
        assertThat(reversal.getBody().journalLines())
                .anySatisfy(line -> {
                    assertThat(line.accountId()).isEqualTo(cash.id());
                    assertThat(line.direction()).isEqualTo(JournalDirection.CREDIT);
                });

        AccountBalanceResponse afterReversal = rest.getForObject(
                "/api/v1/accounts/{id}/balance",
                AccountBalanceResponse.class,
                cash.id()
        );
        assertThat(afterReversal).isNotNull();
        assertThat(afterReversal.debitTotal()).isEqualByComparingTo(new BigDecimal("75.00"));
        assertThat(afterReversal.creditTotal()).isEqualByComparingTo(new BigDecimal("75.00"));
        assertThat(afterReversal.signedBalance()).isEqualByComparingTo(BigDecimal.ZERO);

        AccountLedgerResponse ledger = rest.getForObject(
                "/api/v1/accounts/{id}/ledger",
                AccountLedgerResponse.class,
                cash.id()
        );
        assertThat(ledger).isNotNull();
        assertThat(ledger.lines()).hasSize(2);
    }

    @Test
    void balanceIsDerivedFromJournalNotACachedField() {
        AccountResponse cash = openAccount("CASH", AccountType.ASSET, "USD");
        AccountResponse revenue = openAccount("REV", AccountType.REVENUE, "USD");
        String eventId = "evt-" + UUID.randomUUID();
        ingest("key-" + eventId, eventId, cash.id(), revenue.id(), "8.40", "FEE", "Monthly fee");

        AccountBalanceResponse cashBalance = rest.getForObject(
                "/api/v1/accounts/{id}/balance",
                AccountBalanceResponse.class,
                cash.id()
        );
        AccountBalanceResponse revenueBalance = rest.getForObject(
                "/api/v1/accounts/{id}/balance",
                AccountBalanceResponse.class,
                revenue.id()
        );
        assertThat(cashBalance.signedBalance()).isEqualByComparingTo(new BigDecimal("8.40"));
        assertThat(revenueBalance.signedBalance()).isEqualByComparingTo(new BigDecimal("8.40"));
    }
}
