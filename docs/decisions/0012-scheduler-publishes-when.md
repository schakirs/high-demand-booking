# 0012 — The scheduler publishes the "when"; components execute the "what"

- **Status**: Accepted
- **Date**: 2026-10-09 (date of the reference document; original decision date not recorded)
- **Related**: R1, R2, D-006, D-028

## Context

Several moments drive a sale: waiting room opening, sale opening (lottery), sale closing.
With several instances, each component scheduling its own timers would risk missed or
duplicated triggers and inconsistent clocks.

## Options considered

1. **Each component schedules its own time-based actions** — no extra component; time and
   multi-instance handling duplicated everywhere.
2. **A scheduler publishes time events; each component reacts idempotently.**

## Decision

The scheduler publishes the "when"; each component executes the "what" idempotently.

## Consequences

- Time and multi-instance handling are concentrated in one place.
- A duplicated trigger has no effect.
- One more component.
- Scope (foundation review, 2026-10-26): the scheduler triggers business moments (waiting room
  opening, sale opening, sale closing). The expiry of data a component owns, such as holds,
  stays with that component (0031). In iteration 1 there is no scheduler: the sale opening is
  checked by inventory against the database clock (0028).

## Revisit when

Candidate signal (not in the reference): a missed or duplicated trigger is observed, or time
checks move entirely into the components that own the data.
