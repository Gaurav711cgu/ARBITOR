# ADR 002: EndToEndId Is the Idempotency Boundary

## Status

Accepted.

## Context

Financial clients retry requests after network timeouts, process crashes, and upstream failures. Retry behavior must not produce duplicate settlement.

## Decision

Arbiter uses the payment `EndToEndId` as the transaction idempotency key. The repository checks processed transaction ids inside the atomic write path. If a concurrent duplicate races past the pre-check, the ledger service catches the duplicate write and returns the existing transaction result.

## Consequences

- Client retries are safe.
- Duplicate handling is not dependent on process-local locks alone.
- Future storage adapters must enforce transaction-key uniqueness in the durable store.

