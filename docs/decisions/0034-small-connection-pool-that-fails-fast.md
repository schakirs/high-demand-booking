# 0034 — Small connection pool that fails fast

- **Status**: Accepted
- **Date**: 2026-10-29
- **Related**: D-022 (consequence), D-002, D-021

## Context

D-022's consequence: with virtual threads, the connection pool becomes the limit and must be
protected explicitly. Every hold of an event updates the same stock row (D-010, D-016), so
more connections do not raise that row's throughput — they only move the queue from the JVM
into PostgreSQL's own lock waiters. The success criterion is a hold confirmed in under 100 ms
at p99 (docs/reference.md, section 1). Without a waiting room in iteration 1 (D-005 is
MVP-wide, not iteration-1 scope), the connection pool's timeout is the only back-pressure a
request meets before reaching the database.

## Options considered

1. **Large pool, default (30 s) timeout** — more concurrent requests in flight, but the
   database still serializes on the same row; requests queue invisibly for up to 30 s instead
   of failing fast (D-002: slow down or reject, don't let a request sit and hope).
2. **Small pool, default (30 s) timeout** — caps concurrency but still queues for 30 s under
   overload.
3. **Small pool that fails fast** — caps concurrency and refuses quickly under overload.

## Decision

Option 3. `maximum-pool-size: 10`, `minimum-idle: 10` (fixed-size pool), `connection-timeout:
250` (milliseconds), virtual threads on (`spring.threads.virtual.enabled: true`).

250 is HikariCP's own hard floor, not a tuned value: `HikariConfig.SOFT_TIMEOUT_FLOOR = 250`,
and `setConnectionTimeout`/`setValidationTimeout` throw `IllegalArgumentException` for any
value from 1 to 249 ms (confirmed by reading `HikariConfig.java` at tag `HikariCP-7.0.2`; 0 is
special-cased to mean "no timeout"). Below the floor the application fails to start rather
than silently falling back to the 30 s default — misconfiguring this value is itself caught
fast. The property is written as a plain millisecond integer (`250`, not `250ms`): Spring
binds `spring.datasource.hikari.*` directly onto `HikariConfig`'s setters by reflection, and
`connectionTimeout` is a primitive `long`; a `"250ms"` string fails that binding with a
`ConversionFailedException`/`NumberFormatException` (verified directly against Spring's
`Binder`, not assumed) — Duration-style suffixes only convert onto `java.time.Duration`-typed
properties, which this is not.

## Consequences

- Under overload, some requests fail fast instead of waiting.
- The HTTP mapping of a pool-exhaustion failure is not decided yet (the contract has no 503
  for it; to decide before the hold ticket, D-021).
- `instances × maximum-pool-size` must stay below PostgreSQL's `max_connections`.
- The pool is shared by every operation (confirm, read, expiry sweep, event registry, health),
  so a burst of holds can starve confirmations and the sweep — a priority-inversion risk.
  Candidate mitigations, not decided: a separate pool for confirm and sweep, an
  application-level limit on concurrent holds, and the waiting room's admission rate
  (iteration 2).

## Revisit when

Load tests show `hikaricp_connections_pending` high while the database is not saturated, p99
holds above 100 ms, confirmations failing or slowing during a burst of holds, or the waiting
room arrives (iteration 2).
