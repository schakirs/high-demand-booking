# 0029 — Distinguish "sold out" from "temporarily unavailable" from iteration 1

- **Status**: Accepted
- **Date**: 2026-10-25
- **Related**: R10, R12, D-002, D-016, D-021, D-025

## Context

R10 requires "no seat for now" while holds are active and "sold out" only when every seat is
booked. The iteration 1 contract returns `/problems/event-sold-out` in both cases. Without a
waiting room, retrying is the client's only recourse, so it must know whether retrying is
worth it. The distinction must be computed inside the hold transaction, without reading the
holds table, on a row that already takes ~500 writes/s.

## Options considered

1. **Keep a single `event-sold-out` problem in iteration 1** — no contract change; violates R10
   and pushes clients to retry blindly or give up wrongly.
2. **Count confirmed holds when the decrement fails** — exact; reads the holds table on the hot
   path at the worst moment.
3. **Maintain a `confirmed` counter on the stock row** — updated in the confirmation
   transaction; one extra column read on the same row when the decrement fails.

## Decision

Option 3. A `confirmed` counter lives on the stock row and is updated in the confirmation
transaction. When the decrement fails: `confirmed = capacity` → `/problems/event-sold-out`
(final); otherwise `/problems/seats-temporarily-unavailable` with `Retry-After: 30`.
A contract change ticket is created.

## Consequences

- R10 is honoured from iteration 1; clients can tell "retry later" from "give up".
- Confirmation now writes the stock row too (contention on the same row at confirmation rate).
- Accepted limits: an expired hold not yet swept extends the "temporary" state; `Retry-After`
  is only a hint, real protection will come from rate limiting and the waiting room.
- New database constraints become possible and should be added:
  `confirmed <= capacity`, `available + confirmed <= capacity`.
- Contract change (new problem type, `Retry-After` header) under D-021.

## Revisit when

The waiting room makes the distinction visible elsewhere, retries after `Retry-After` show up as
a measurable load spike in k6, or the sweep lag makes the temporary state noticeably long.
