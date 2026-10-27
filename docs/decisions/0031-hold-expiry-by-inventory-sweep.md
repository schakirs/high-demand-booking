# 0031 — Hold expiry by a periodic sweep owned by inventory

- **Status**: Accepted
- **Date**: 2026-10-26
- **Related**: R5, R9, R12, D-010, D-012, D-016, D-028; closes the open question "periodic
  sweep or delayed messages"

## Context

Every hold must be expired once `expires_at <= now()`, its seat released and `HoldExpired`
published (flow C). In the reference scenario, holds expire at up to ~40,000 distinct instants
per sale. D-012 gives the scheduler the "when" of time-driven actions, but that decision was
about a few business moments per event (opening, closing). Correctness never depends on the
sweep: every check (confirm, read, new hold) already treats a hold as expired as soon as
`expires_at <= now()` (0028); the sweep only releases the seat.

## Options considered

1. **The scheduler publishes one delayed message per hold** — follows D-012 to the letter;
   one message per hold, a broker or delay queue, and an expiry path outside the data owner.
2. **Inventory sweeps expired holds periodically** — batches with `FOR UPDATE SKIP LOCKED`,
   one conditional update per hold; no extra infrastructure; release lag up to one sweep
   interval.

## Decision

Option 2, beyond iteration 1. Inventory expires its own holds with a periodic sweep. The
scheduler keeps the business moments (waiting room opening, sale opening, sale closing).

## Consequences

- Expiry stays inside the data owner, atomic with the counter re-increment.
- Safe with several instances (`SKIP LOCKED`, conditional update).
- A released seat becomes available up to one sweep interval late, which extends the
  "temporarily unavailable" state (0029).
- Batch size and interval must be tuned and observed.

## Revisit when

The sweep lag becomes visible in seat availability at the end of a sale, the sweep cost shows
in hold latency, or inventory moves to Redis (D-016 stage 3), where key expiry may replace it.
