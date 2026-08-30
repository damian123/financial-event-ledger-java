package io.github.damian123.eventledger.domain;

public enum AuditAction {
    INGEST,
    CONFLICT,
    POST,
    REVERSE,
    RECONCILE,
    DEAD_LETTER,
    RETRY,
    PUBLISH
}
