package io.github.damian123.eventledger.domain;

public enum AccountType {
    ASSET,
    LIABILITY,
    EQUITY,
    REVENUE,
    EXPENSE;

    public boolean isDebitNormal() {
        return this == ASSET || this == EXPENSE;
    }
}
