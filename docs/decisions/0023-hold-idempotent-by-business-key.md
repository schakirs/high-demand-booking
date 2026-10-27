# 0023 — Holds are idempotent by business key (event, user)

- **Status**: Accepted
- **Date**: 2026-10-09 (date of the reference document; original decision date not recorded)
- **Related**: R3, R4; amends 0003 (mechanism for holds)

## Context

D-003 requires every operation to survive retries. For holds, R3 already enforces at most one
active hold or booking per (event, user), through a unique index. A replayed hold request
must not take a second seat.

## Options considered

1. **Client idempotency key** (D-003 mechanism) — generic; redundant with the unique index.
2. **Business key (event, user)** — a replayed request receives the existing active hold.

## Decision

Holds are idempotent by the business key (event, user) rather than by an idempotency key.
A replayed request receives the existing active hold. D-003 stays the principle; the
mechanism is chosen per operation.

## Consequences

- No key storage for holds.
- A second request from the same user, retry or another tab, receives the same hold.

## Revisit when

Several seats per user are introduced: an idempotency key becomes necessary again.
