# 0011 — Asynchronous notification, outside the critical path, with retries

- **Status**: Accepted
- **Date**: 2026-10-09 (date of the reference document; original decision date not recorded)
- **Related**: D-013, D-017

## Context

After a booking is confirmed, the user receives an email or a push notification. The
notification service is not critical; the booking is.

## Options considered

1. **Synchronous notification inside the booking flow** — immediate; a notification failure
   or slowdown would fail or slow down the booking.
2. **Asynchronous notification with retries.**

## Decision

Notification is asynchronous, outside the critical path, with retries.

## Consequences

- A non-critical component can never make a critical one fail.
- The confirmation email is delayed.
- Notification is the first candidate for a teaching extraction (0017).

## Revisit when

Candidate signal (not in the reference): users need the notification as the only proof of
booking within a strict delay.
