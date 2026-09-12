# ADR 001: Double-Entry Journal Is the Source of Truth

## Status

Accepted.

## Context

Payment systems cannot treat a transfer as two independent balance updates. A partial update can create or destroy money. A bank-grade ledger needs a conservation invariant: every accepted transaction must create signed journal entries whose total is zero.

## Decision

Arbiter models settlement as one debit journal entry and one credit journal entry. The ledger repository validates the entry types, currency, duplicate transaction key, available funds, and zero-sum invariant inside the atomic write method.

## Consequences

- Account balances can be reconciled from opening balance plus journal history.
- Duplicate requests can return existing journal entries without posting again.
- Future DynamoDB or SQL adapters must preserve the same repository contract.

