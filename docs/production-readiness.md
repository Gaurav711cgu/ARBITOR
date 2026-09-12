# Production Readiness Plan

This document separates what Arbiter already proves from what must be added before regulated financial traffic.

## Current Guarantees

| Area | Current implementation | Evidence |
| --- | --- | --- |
| Double-entry invariant | Atomic debit and credit pair with signed amounts summing to zero | `./scripts/test.sh`, `./scripts/brutal-test.sh` |
| Durable writes | Append-only file ledger with checksum and `fsync` before state mutation | `FileLedgerRepository` and benchmark artifacts |
| Idempotency | `EndToEndId` is the transaction key; duplicate submissions return `DUPLICATE` | Concurrent duplicate test and benchmark |
| Reconciliation | Account balance equals opening balance plus journal history; global journal sum equals zero | `ReconciliationService` |
| Fraud pre-screening | Rule scorer declines high-risk payments before final ledger post | fraud tests and benchmark |
| ISO payment entry | `pain.001`-style XML endpoint extracts `EndToEndId`, debtor, creditor, amount, currency, purpose | parser tests |
| API hardening | content-type checks, request-size cap, XML XXE defense, security headers | API implementation |

## Required Before Real Money

| Priority | Work item | Acceptance gate |
| --- | --- | --- |
| P0 | Replace file ledger with DynamoDB `TransactWriteItems` | Both journal legs and idempotency record commit or fail together |
| P0 | Add authenticated operators and service clients | No state-changing endpoint accepts anonymous traffic |
| P0 | Add tenant and account authorization | Caller cannot view or mutate accounts outside its scope |
| P0 | Add official ISO 20022 schema validation | Full sample corpus parses or fails with structured errors |
| P0 | Move audit events to Kafka | Every settlement creates exactly one immutable event with no offset gaps |
| P1 | Add Redis idempotency fast path with durable fallback | cache loss cannot cause duplicate settlement |
| P1 | Add S3 Object Lock archive | audit records retained under compliance-mode policy |
| P1 | Add OpenTelemetry | each settlement emits spans for parse, fraud score, ledger post, audit publish |
| P1 | Add CI benchmark gates | pull requests fail if reconciliation or duplicate guarantees regress |
| P2 | Add operational dashboards | latency, duplicate rate, decline rate, balance drift, and replay failures visible |

## Senior Review Notes

The most important design decision is that Arbiter models a payment as immutable journal entries, not as direct balance updates. Balances are derived and verified against the journal. Idempotency is tied to the payment's `EndToEndId`, and duplicate handling is enforced inside the atomic write path, not only through an API pre-check.

The local file ledger is deliberately conservative: it uses `fsync` for each durable append, which limits throughput but makes benchmark results honest. A production DynamoDB implementation should improve throughput while preserving the same repository contract and acceptance tests.

