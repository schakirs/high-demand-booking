# 0017 — Modular monolith plus two separate deployables

- **Status**: Accepted
- **Date**: 2026-10-09 (date of the reference document; original decision date not recorded)
- **Related**: D-009, D-011, D-014, D-020

## Context

The position read path needs computing resources separate from the booking path (D-009). The
payment simulator must behave like a real network call (latency, timeouts, duplicated
webhooks). The other modules can be isolated inside one process with bulkheads.

## Options considered

1. **One deployable for everything** — simplest; the read path competes with booking, and an
   in-process simulator cannot reproduce a real network call.
2. **Modular monolith + position view + payment simulator** as separate deployables.
3. **One service per module** — maximum isolation; distributed complexity without a learning
   objective that needs it now.

## Decision

A modular monolith plus two separate deployables: the position view and the payment
simulator. Other isolations are handled with bulkheads.

## Consequences

- Three deployables to operate instead of many.
- A deployment restarts every module of the monolith.
- The first teaching extraction will be Notification.

## Revisit when

A fix must be deployed during a sale (a restart of every module becomes a real problem).
