# 0022 — Technology stack

- **Status**: Accepted
- **Date**: 2026-10-09 (date of the reference document; original decision date not recorded)
- **Related**: D-016, D-020, D-026, D-028

## Context

The stack must be supported for the life of the project and familiar where novelty brings
nothing to learn (12 years of Java and Spring Boot).

## Options considered

Not recorded at decision time.

## Decision

Java 25 (LTS), Spring Boot 4.1.x and the compatible Spring Modulith version, Maven, Flyway,
PostgreSQL 18, with the same major version locally, in CI and in the cloud.

## Consequences

- Supported versions; familiar tools.
- Flyway migrations are forward-only.
- With virtual threads, the connection pool becomes the limit and must be protected
  explicitly.

## Revisit when

Candidate signal (not in the reference): a version reaches end of support, or the connection
pool limits hold throughput in load tests.
