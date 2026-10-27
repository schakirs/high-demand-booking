# 0009 — Separate the position read path from the booking path (CQRS)

- **Status**: Accepted
- **Date**: 2026-10-09 (date of the reference document; original decision date not recorded)
- **Related**: R2b, R13, D-014, D-017

## Context

Position reads dominate the load by a factor of about 100: ~150,000 req/s with a fixed 10 s
refresh, ~60,000 req/s with R13, and they tolerate slight inaccuracy. Booking writes are a few
thousand per second and tolerate no error. Serving both from the same model would let the read
volume threaten the write path.

## Options considered

1. **One model for reads and writes** — no synchronisation; reads compete with the critical
   path for the same resources.
2. **Separate read path (position) and booking path.**

## Decision

The position read path is separated from the booking path (CQRS).

## Consequences

- Reads can be scaled and cached independently (D-014, D-017).
- The displayed position can lag slightly behind reality.
- Two models must be kept in sync.

## Revisit when

Candidate signal (not in the reference): measured read volume is low enough for the booking
model to serve it without affecting hold latency.
