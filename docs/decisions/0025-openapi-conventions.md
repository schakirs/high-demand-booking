# 0025 — OpenAPI 3.1.1, snake_case, ISO 8601 UTC; read a hold

- **Status**: Accepted
- **Date**: 2026-10-09 (date of the reference document; original decision date not recorded)
- **Related**: D-021, D-028

## Context

The contract (0021) needs a version and conventions. A client that reloads the page must be
able to restore its countdown, which the first contract did not allow.

## Options considered

Not recorded at decision time.

## Decision

OpenAPI 3.1.1, properties in snake_case, dates in ISO 8601 UTC. `GET /v1/holds/{holdId}` is
added.

## Consequences

- A recent version with mature tooling support.
- The client can restore its countdown.
- Jackson configuration must be explicit (snake_case, UTC).
- The generator's 3.1 support must be verified (ticket 5).

## Revisit when

The generator's 3.1 support proves insufficient.
