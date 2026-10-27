# 0028 — Sale opening enforced by inventory, PostgreSQL clock as time source

- **Status**: Accepted
- **Date**: 2026-10-25
- **Related**: R1, D-002, D-010, D-012, D-016, D-027; closes the open question on the time source
  across instances

## Context

A hold must be refused before the sale opens (R1, `409 /problems/sale-not-open`). The stock is
created well before the opening (D-027), so the existence of the stock is not enough. The check
must not introduce a read-then-write race nor a synchronous call on the hold path (target p99
< 100 ms, ~500 holds/s). Several application instances run with possibly drifting clocks.
From iteration 2, the Admission (fed by the Planificateur, D-012) will gate access to holds.

## Options considered

1. **Inventory stores `opens_at` and checks it in its conditional update** — atomic, no extra
   call; duplicates the opening time from the event module; rescheduling must be propagated.
2. **Booking queries the event module on every hold** — single copy of the data; adds a
   synchronous call on the critical path and a check-then-act across two modules.

## Decision

Option 1. Inventory stores `opens_at` and refuses any hold while `now() < opens_at`, inside its
conditional update. Event dates are immutable in iteration 1: there is no rescheduling
operation; event modification is planned after iteration 1. From iteration 2, this check becomes defence in depth behind the Admission.
The source of truth for time is the PostgreSQL clock (`now()`).

## Consequences

- The opening check is atomic with the seat decrement; no race at the opening instant.
- One clock for all instances; application clock drift cannot open a sale early.
- `opens_at` exists in two modules; immutability keeps both copies consistent in iteration 1.
  When event modification arrives, inventory will need an operation allowed only before the
  opening (conditional update on `opens_at > now()`).
- `now()` is the transaction start time: kept because transactions are short;
  `clock_timestamp()` only if a need appears.
- Every time comparison that matters (hold expiry, confirmation, `GET /v1/holds/{id}` status)
  uses the database clock. Tests move the data, not the time: SQL fixtures place `expires_at`
  in the past; the expiry sweep is awaited with Awaitility.

## Revisit when

- Event modification is introduced: rescheduling must then be designed.
- A long transaction makes `now()` drift noticeably.
- Inventory moves to Redis (D-016 step 3), which removes `now()` as a shared clock.
- The Admission makes the check redundant and its cost shows in hold latency.
