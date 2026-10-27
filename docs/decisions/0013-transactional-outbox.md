# 0013 — Publish events through a transactional outbox

- **Status**: Accepted, amended by 0020 (the outbox is the Spring Modulith registry)
- **Date**: 2026-10-09 (date of the reference document; original decision date not recorded)
- **Related**: D-010, D-011, D-016, D-020, D-024

## Context

State changes (hold created, expired, confirmed) must be published to other modules. Writing
to the database and then to a broker is a dual write: a crash between the two loses or
invents an event.

## Options considered

1. **Dual write: database then broker** — simple; loses events on failure.
2. **Transactional outbox** — the event is written in the same transaction as the state, then
   relayed.

## Decision

Events are published through a transactional outbox.

## Consequences

- No dual write between the database and a broker.
- Delivery is at least once: consumers must be idempotent.
- A relay must be operated; with 0020, the Spring Modulith event publication registry plays
  this role.
- An equivalent must be found if the inventory moves to Redis (D-016 stage 3).

## Revisit when

The inventory moves to Redis, or events must leave the monolith through a broker.
