# 0004 — Concrete domain language; only the inventory is generic

- **Status**: Accepted
- **Date**: 2026-10-09 (date of the reference document; original decision date not recorded)
- **Related**: D-024, D-027

## Context

The system sells seats for events. It is tempting to model a generic "resource reservation"
platform from the start, but there is only one use case. The inventory (seats, holds,
counters) is the only part whose behaviour does not depend on what is being sold.

## Options considered

1. **Generic model everywhere** (resource, slot, reservation) — reusable in theory; rejected:
   generalising without a second use case is guessing.
2. **Concrete language (Event, Hold, Booking), generic inventory only.**

## Decision

The domain uses concrete language (Event, Hold, Booking). Only the inventory is generic.

## Consequences

- Code and documents read like the business they serve.
- The inventory stays reusable and must not depend on event concepts (enforced by D-027).
- Renames may be needed later if a second use case appears.

## Revisit when

A second use case (another kind of resource to sell) is actually planned.
