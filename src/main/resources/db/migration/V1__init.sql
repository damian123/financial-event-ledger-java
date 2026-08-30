CREATE TABLE accounts (
    id          UUID PRIMARY KEY,
    code        VARCHAR(64)  NOT NULL,
    name        VARCHAR(256) NOT NULL,
    type        VARCHAR(32)  NOT NULL,
    currency    VARCHAR(3)   NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_accounts_code UNIQUE (code)
);

CREATE TABLE events (
    id                  UUID PRIMARY KEY,
    event_id            VARCHAR(128)   NOT NULL,
    idempotency_key     VARCHAR(128)   NOT NULL,
    account_id          UUID           NOT NULL REFERENCES accounts (id),
    counter_account_id  UUID           NOT NULL REFERENCES accounts (id),
    amount              NUMERIC(20, 2) NOT NULL,
    currency            VARCHAR(3)     NOT NULL,
    type                VARCHAR(32)    NOT NULL,
    occurred_at         TIMESTAMPTZ    NOT NULL,
    description         VARCHAR(512)   NOT NULL,
    payload_hash        VARCHAR(64)    NOT NULL,
    status              VARCHAR(32)    NOT NULL,
    attempt_count       INTEGER        NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uq_events_event_id UNIQUE (event_id),
    CONSTRAINT uq_events_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT ck_events_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_events_distinct_accounts CHECK (account_id <> counter_account_id)
);

CREATE INDEX idx_events_account ON events (account_id);
CREATE INDEX idx_events_status ON events (status);

CREATE TABLE journal_lines (
    id           UUID PRIMARY KEY,
    event_pk     UUID           NOT NULL REFERENCES events (id),
    event_id     VARCHAR(128)   NOT NULL,
    account_id   UUID           NOT NULL REFERENCES accounts (id),
    direction    VARCHAR(8)     NOT NULL,
    amount       NUMERIC(20, 2) NOT NULL,
    currency     VARCHAR(3)     NOT NULL,
    description  VARCHAR(512)   NOT NULL,
    posted_at    TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_journal_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_journal_direction CHECK (direction IN ('DEBIT', 'CREDIT'))
);

CREATE INDEX idx_journal_account_posted ON journal_lines (account_id, posted_at);
CREATE INDEX idx_journal_event_id ON journal_lines (event_id);
CREATE INDEX idx_journal_event_pk ON journal_lines (event_pk);

CREATE TABLE outbox (
    id             UUID PRIMARY KEY,
    event_pk       UUID         NOT NULL REFERENCES events (id),
    event_id       VARCHAR(128) NOT NULL,
    payload        TEXT         NOT NULL,
    status         VARCHAR(32)  NOT NULL,
    attempt_count  INTEGER      NOT NULL DEFAULT 0,
    last_error     TEXT,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    published_at   TIMESTAMPTZ
);

CREATE INDEX idx_outbox_pending ON outbox (status, created_at);

CREATE TABLE dead_letters (
    id             UUID PRIMARY KEY,
    event_pk       UUID REFERENCES events (id),
    event_id       VARCHAR(128),
    source         VARCHAR(32)  NOT NULL,
    payload        TEXT         NOT NULL,
    last_error     TEXT         NOT NULL,
    attempt_count  INTEGER      NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    requeued_at    TIMESTAMPTZ
);

CREATE INDEX idx_dead_letters_created ON dead_letters (created_at DESC);

CREATE TABLE audit_records (
    id          UUID PRIMARY KEY,
    event_id    VARCHAR(128),
    action      VARCHAR(32)  NOT NULL,
    details     TEXT         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_event ON audit_records (event_id, created_at);

CREATE TABLE reconciliations (
    id                           UUID PRIMARY KEY,
    started_at                   TIMESTAMPTZ    NOT NULL,
    completed_at                 TIMESTAMPTZ,
    status                       VARCHAR(32)    NOT NULL,
    event_count                  INTEGER        NOT NULL DEFAULT 0,
    journal_line_count           INTEGER        NOT NULL DEFAULT 0,
    debit_total                  NUMERIC(20, 2) NOT NULL DEFAULT 0,
    credit_total                 NUMERIC(20, 2) NOT NULL DEFAULT 0,
    accepted_event_amount_total  NUMERIC(20, 2) NOT NULL DEFAULT 0,
    finding_count                INTEGER        NOT NULL DEFAULT 0
);

CREATE TABLE reconciliation_findings (
    id                 UUID PRIMARY KEY,
    reconciliation_id  UUID         NOT NULL REFERENCES reconciliations (id) ON DELETE CASCADE,
    kind               VARCHAR(64)  NOT NULL,
    event_id           VARCHAR(128),
    details            TEXT         NOT NULL
);

CREATE INDEX idx_findings_reconciliation ON reconciliation_findings (reconciliation_id);
