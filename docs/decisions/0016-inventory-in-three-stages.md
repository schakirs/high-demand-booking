# 0016 — Inventory in three stages, starting with a PostgreSQL conditional update

- **Status**: Accepted
- **Date**: 2026-10-09 (date of the reference document; original decision date not recorded)
- **Related**: R3, R9, R12, D-002, D-010, D-028, D-029

## Context

Holding a seat is a contention problem, not a volume problem: at an admission rate of 500/s,
every hold writes the same stock counter. The reference scenario (40,000 seats) needs a first
wave of ~80 s at that rate. The waiting room caps the arrival rate on the hold path (R9), so
the inventory does not have to absorb the raw peak. Integrity comes first (D-002): the
invariant held + booked <= capacity (R12) must hold at all times, with strong durability.
The 500 holds/s figure is an assumption to be measured by the load test.

## Options considered

The three approaches are recorded as successive stages rather than competing options:

1. **PostgreSQL conditional update on a counter, plus a CHECK constraint** — one row per event,
   `available = available - 1 WHERE available > 0`; simple, durable, atomic; throughput capped
   by a single hot row.
2. **One row per seat, claimed with `SELECT ... FOR UPDATE SKIP LOCKED`** — spreads contention
   across rows and supports numbered seats; more rows and more complex queries.
3. **Atomic operations in Redis** — highest throughput; weaker durability and a second source
   of truth to reconcile.

## Decision

Start with stage 1 for the MVP; move to stage 2, then 3, only when their signal appears.
The inventory contract stays stable so implementations are interchangeable.

## Consequences

- Strong durability and a simple, testable implementation for the MVP.
- Throughput is capped by the database row; the cap must be measured.
- Implementations must stay behind a stable contract.
- Moving to Redis removes `now()` as a shared clock (0028) and requires an outbox equivalent
  (0013).

## Revisit when

- Numbered seats are introduced, or the first wave takes more than 2 minutes → stage 2.
- Several concurrent sales exceed what the database can absorb → stage 3.
