# 0024 — Booking exposes the API; its state will be fed by inventory events

- **Status**: Accepted
- **Date**: 2026-10-09 (date of the reference document; original decision date not recorded)
- **Related**: D-004, D-009, D-010, D-013, D-027; revised 2026-10-26 (foundation review)

## Context

The public booking API must stay stable when admission and payment arrive. The inventory is
an internal, generic capability (D-004). In the first version of flow A, booking and inventory
both wrote state for the same hold (a dual write).

## Options considered

1. **Inventory exposes the hold API directly** — fewer hops; makes the generic inventory part
   of the public contract.
2. **Booking writes its own state alongside the inventory** — booking owns its data; dual
   write in flow A.
3. **Booking is the facade; its state is a projection of inventory events.**

## Decision

The booking API stays exposed by booking, the facade and future orchestrator. Booking never
writes hold state itself: when it keeps state, that state is fed entirely by inventory events
(HoldCreated, HoldConfirmed, HoldExpired). In iteration 1 it keeps none.

## Consequences

- The public contract stays stable when admission and payment arrive.
- The inventory remains internal and generic.
- No more dual write in flow A.
- Iteration 1 (foundation review, 2026-10-26): booking is a stateless facade. Every hold
  endpoint, `GET /v1/holds/{id}` included, calls inventory (`hold`, `confirm`, `getHold`), so a
  read right after a confirmation never lags. The projection is deferred until payment needs
  it; inventory still publishes its events, with no consumer yet.

## Revisit when

Admission and payment arrive and booking starts orchestrating (it then gains its own state).
