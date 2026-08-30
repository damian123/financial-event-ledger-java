# Limitations

This repository is a portfolio project using fictional data. It is not a production ledger.

- **No authentication or authorization.** Every HTTP endpoint is open.
- **Single-node outbox poller.** Pending rows are claimed with a pessimistic lock in one process. There is no competing-consumer cluster, partition assignment, or broker.
- **At-least-once publication only.** The in-process publisher can deliver the same outbox payload more than once after a crash between publish and commit.
- **Not a production ledger.** There is no chart-of-accounts workflow, multi-book close, period lock, FX revaluation, or regulatory reporting.
- **Fictional data.** Seed accounts and examples are invented. Nothing here is connected to an employer, client, or live financial system.
- **Money model is USD-style cents.** Amounts are `numeric(20,2)`. Currencies with a different minor-unit exponent are not modeled.
- **No multi-currency netting.** An event's currency must match both accounts; cross-currency postings are rejected.
