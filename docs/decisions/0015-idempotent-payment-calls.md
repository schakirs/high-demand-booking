# 0015 — Idempotent payment calls, reconciliation, idempotent webhooks

- **Status**: Accepted
- **Date**: 2026-10-09 (date of the reference document; original decision date not recorded)
- **Related**: R6, R8, D-003

## Context

A timeout on a payment call does not say whether the payment was created. The provider
answers later through webhooks, which can arrive more than once.

## Options considered

Not recorded at decision time.

## Decision

Payment calls are idempotent (same key on retry), payments are reconciled, and webhooks are
acknowledged first and then processed idempotently.

## Consequences

- A retried call never creates a second payment; a duplicated webhook has no effect.
- Reconciliation logic must be maintained.

## Revisit when

Candidate signal (not in the reference): the real payment provider replaces the simulator and
offers different idempotency guarantees.
