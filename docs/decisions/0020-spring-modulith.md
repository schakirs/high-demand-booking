# 0020 — Spring Modulith for module boundaries and as the outbox

- **Status**: Accepted
- **Date**: 2026-10-09 (date of the reference document; original decision date not recorded)
- **Related**: D-017, D-027; amends 0013 (the registry is the outbox)

## Context

A modular monolith only stays modular if its boundaries are checked. The outbox (0013) needs
a storage table and a relay.

## Options considered

1. **Conventions plus a hand-written outbox and relay** — no extra dependency; boundaries
   unchecked and an outbox to build and operate.
2. **Spring Modulith** — boundary verification on every build, and its event publication
   registry as the outbox.

## Decision

Spring Modulith enforces module boundaries, and its event publication registry serves as the
outbox (0013).

## Consequences

- Boundaries are verified automatically on every build; cycles fail the build (see 0027).
- The outbox comes ready-made.
- One more dependency, with its conventions (e.g. listening to another module's event type
  counts as a dependency).

## Revisit when

Candidate signal (not in the reference): a module is extracted as a deployable, or the
registry's at-least-once replay is not enough (incomplete publications in production).
