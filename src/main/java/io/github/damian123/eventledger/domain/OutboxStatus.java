package io.github.damian123.eventledger.domain;

public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    DEAD_LETTERED
}
