# 0008 — Decouple visible expiry from actual release (grace period)

- **Status**: Accepted
- **Date**: 2026-10-09 (date of the reference document; original decision date not recorded)
- **Related**: R5, R6, R7, R8, D-010

## Context

A hold lasts 5 minutes (R5). A payment may be in progress when the countdown ends; if the seat
is released at that instant, the payment completes for a seat that is gone and must be
refunded (R8). Late payments cannot be eliminated, but they can be made rare.

## Options considered

1. **Release at expiry and only handle late payments** (automatic refund, R8) — simple; makes
   late payments frequent.
2. **Keep the seat held for a grace period when a payment is in progress.**

## Decision

Visible expiry and actual release are decoupled: if a payment is in progress at expiry, the
seat stays held for 1 more minute (configurable, R7).

## Consequences

- Late payments become rarer instead of only being handled.
- A seat can stay immobilised one minute longer.
- R8 (automatic, idempotent refund) is still required for payments beyond the grace period.

## Revisit when

Candidate signal (not in the reference): the late-payment rate after grace stays significant,
or grace periods visibly delay seat availability at the end of a sale.
