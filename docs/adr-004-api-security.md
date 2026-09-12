# ADR 004: Protected Ledger APIs

## Status

Accepted.

## Context

Even a portfolio ledger should not expose anonymous state-changing endpoints. Senior fintech reviewers expect clear service-boundary controls.

## Decision

Arbiter requires `x-arbiter-api-key` for ledger operations. `GET /health`, `GET /config`, and static frontend assets remain public. The local run script prints an ephemeral development key when `ARBITER_API_KEY` is not supplied.

## Consequences

- Local demos remain easy to run.
- Protected endpoints are no longer anonymous.
- Real production deployments should replace the API key with OAuth/JWT service identity and account-level authorization.

