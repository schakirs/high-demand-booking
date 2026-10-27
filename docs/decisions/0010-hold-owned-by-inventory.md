# 0010 — Holds belong to the inventory; atomic hold-to-booking conversion

- **Status**: Accepted
- **Date**: 2026-10-09 (date of the reference document; original decision date not recorded)
- **Related**: R3, R5, R7, R8, R12, D-002, D-024, D-029

## Context

A hold can expire at the very moment its payment succeeds. If hold state lived in several
places, the expiry/payment race would have to be arbitrated across them, with a risk of
releasing a seat that is also being booked.

## Options considered

1. **Hold state shared between booking and inventory** — each module owns part of the
   lifecycle; the race must be settled across two sources of truth.
2. **Inventory owns the hold; conversion is one atomic operation.**

## Decision

The hold belongs to the inventory. Converting a hold into a booking is a single atomic
operation, "hold → booking if still active".

## Consequences

- One source of truth for seats and holds; the expiry/payment race is settled by one
  operation.
- Booking depends on inventory events; its status can briefly lag (see 0024).

## Revisit when

Candidate signal (not in the reference): the inventory moves to a store where the atomic
conversion is no longer available (D-016 stage 3).
