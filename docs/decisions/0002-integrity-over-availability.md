# 0002 — Integrity before availability on the booking path

- **Status**: Accepted
- **Date**: 2026-10-09 (date of the reference document; original decision date not recorded)
- **Related**: R3, R12, D-010, D-016

## Context

An oversold seat is irreversible: someone holds a ticket that does not exist. A period of
unavailability is recoverable: users wait, retry, or are told where they stand. Under the
reference scenario (40,000 seats, 1.5 million connected users), the booking path will be
pushed past its limits, so the system needs a rule for what to give up first.

## Options considered

1. **Favour availability** — keep accepting holds under stress and repair conflicts later;
   rejected because an oversell cannot be repaired.
2. **Favour integrity** — slow down or refuse when correctness is at risk.

## Decision

Integrity comes before availability on the booking path: the system slows down or refuses
rather than risk an oversell.

## Consequences

- The invariant held + booked <= capacity (R12) is never traded for throughput.
- Users can receive refusals or waits that a more permissive system would avoid.
- Every mechanism on the booking path (counters, conversions, retries) must fail closed.

## Revisit when

Candidate signal (not in the reference): a use case appears where overselling can be
compensated (e.g. overbooking with a guaranteed fallback).
