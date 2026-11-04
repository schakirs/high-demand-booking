# 0039 — Operating the event publication registry

- **Status**: Accepted
- **Date**: 2026-11-04
- **Related**: D-013, D-020, D-027, D-033; amends 0027 (margin larger than the registry's
  resubmission delay), amends 0033 (the registry's schema)

## Context

`#7`/`#12` need the event publication registry (D-013, D-020) in place for `POST /v1/events` to
publish `EventCreated` in the same transaction as the insert. The registry needs a storage table
and an operating policy: where its schema lives, how it initializes, what replays it, and what
absorbs the duplicates at-least-once delivery produces.

## Decision

JDBC registry (`spring-modulith-starter-jdbc`). Its table lives in a dedicated schema,
`modulith`, created by a Flyway `__root` migration — not `public`: this amends 0033, which left
the registry as "infrastructure, not a module's data" without naming its schema. A framework
table shared by modules must live in its own schema, exactly like a module's own data, because
`public` only works through `search_path`. Schema initialization is explicit and off
(`spring.modulith.events.jdbc.schema-initialization.enabled: false`): the Flyway migration owns
the DDL, copied from the library's own `schemas/v2/schema-postgresql.sql`, to compare at every
Modulith upgrade. `republish-outstanding-events-on-restart: true`.

No periodic resubmission job in this ticket (deferred): Spring Modulith 2.1 does not resubmit
incomplete publications while the application keeps running — its staleness monitor only marks
them failed. Until a resubmission job exists, the sale-opening margin (ADR 0027, default 1 h,
configurable, validated at startup) only covers a restart; a publication stuck while the
application stays up relies on the alert of `#13` and a manual action. This amends 0027, whose
"margin larger than the registry's resubmission delay" assumed a resubmission job that does not
exist yet.

Duplicates (restart replay, several instances, a crash before completion) are absorbed by an
idempotent consumer (`inventory.createStock`, keyed uniquely, `ON CONFLICT DO NOTHING`, `#8`),
never by locking the registry table. Event types are internal to their publishing module and
evolve by addition only (fields added, never renamed or removed: stored publications are
replayed from their serialized JSON). Completion mode is left at Modulith's own default
(`update`). `POST /v1/events` is not made idempotent in this ticket: it is an admin operation, at
low volume; a duplicate call costs one extra event and one extra stock, never an oversell.

## Consequences

- The registry's schema is visible in `information_schema.schemata`, like every module's own
  schema (D-033).
- A stuck publication, while the application keeps running, is only caught by monitoring
  (`#13`), not by this project's own code, until a resubmission job exists.
- `createStock`'s idempotency key carries the real correctness guarantee against duplicate
  delivery; the registry itself is never a locking or deduplication point.

## Revisit when

- A periodic resubmission job is introduced: the startup check for the sale-opening margin then
  becomes "margin > resubmission delay", restoring 0027's original assumption.
- Incomplete publications are observed outside of transient failures.
- The table grows enough to matter (completion mode `delete` or `archive`).
- Events need to leave the monolith (D-013's own revisit condition).
