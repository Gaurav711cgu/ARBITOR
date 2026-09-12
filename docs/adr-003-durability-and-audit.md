# ADR 003: Durable Local Store Before Cloud Adapter

## Status

Accepted.

## Context

The project must avoid fake production claims. Running DynamoDB, Kafka, Redis, and S3 locally without real deployment evidence would blur the line between architecture and proof.

## Decision

The current implementation uses a local append-only ledger log and a local append-only audit log. Both records are checksum-protected and force writes to stable storage with `fsync`. Startup replay fails if checksum validation or balance replay validation fails.

## Consequences

- Local benchmark claims are real durability claims for this implementation.
- Throughput is lower than an async or cloud-backed design, but the benchmark is honest.
- Production adapters can be added behind the same repository and audit interfaces.

