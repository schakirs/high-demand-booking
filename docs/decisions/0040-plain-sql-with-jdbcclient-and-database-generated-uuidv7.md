# 0040 — Plain SQL with JdbcClient and database-generated UUIDv7

- **Status**: Accepted
- **Date**: 2026-11-04
- **Related**: D-010, D-016, D-028; CLAUDE.md rule 4 (single atomic conditional update), rule 5
  (PostgreSQL clock)

## Context

`#7`/`#12` are the first ticket to write to a module's own table (`event.event`). The project
needs one persistence tool for every module, consistent with "every seat or hold state change is
a single atomic conditional SQL update" (CLAUDE.md rule 4) and "`now()` in SQL is the single
source of truth for time" (rule 5): both rule out any tool that hides the SQL behind a
read-modify-write abstraction.

## Options considered

1. **Spring Data JDBC** — repository abstraction over explicit SQL; still pushes towards
   load-then-save for anything beyond a trivial CRUD mapping, working against rule 4.
2. **JPA/Hibernate** — rejected: its identity map, dirty checking and cache push towards
   read-modify-write, the opposite of an atomic conditional update; a `uuidv7()` default also
   needs a generator strategy fighting the entity lifecycle.
3. **jOOQ** — type-safe dynamic SQL, a real alternative; adds a code-generation step and a
   dependency not otherwise needed in iteration 1's single-statement queries.
4. **`JdbcClient`, explicit SQL** (chosen) — already on the classpath through
   `spring-boot-starter-jdbc`; no abstraction between the Java call and the statement that runs.
5. **UUID v4, generated in the JVM** — a second id-generation mechanism and a second
   `RETURNING`-free round trip (insert then re-read, or construct the row in Java without a
   database-verified id) to get `created_at`; `#7`/`#12` already need `RETURNING` for
   `created_at`, so a JVM-generated id gains nothing in exchange for bypassing the primary key's
   own default.
6. **UUID v7, generated in the JVM** — same `RETURNING` shape as PostgreSQL's own `uuidv7()`;
   kept as a candidate (see *Revisit when*) but not chosen, since the database default already
   covers iteration 1 without a library dependency.

## Decision

Options 4 + PostgreSQL 18's own `uuidv7()`. `JdbcClient`, one SQL statement per operation,
schema-qualified (D-033), `now()` computed in SQL (rule 5). No Spring Data, no JPA: the
repository and the service exchange a domain record (e.g. `EventRecord`), never the generated
contract DTO (`event.api.model.Event`) — a contract change must not ripple into the SQL code.
Primary keys default to `uuidv7()`; the generated id is always read back through
`RETURNING ... id, created_at`, never re-queried.

## Consequences

- B-tree locality for high-insert tables (the inventory's holds, from `#8` onward): `uuidv7()`'s
  time-ordered prefix avoids the random-insert-position cost of `uuidv4()` on a primary key
  index.
- A v7 id reveals its own creation time to anyone who can decode it (accepted: ids are not a
  confidentiality boundary in this project).
- Every query is hand-written SQL: no generated query DSL, no compile-time check that a column
  name still exists — caught instead by the Testcontainers integration tests this ticket adds.
- The domain record adds one mapping step (repository ⇄ service ⇄ controller) that a direct reuse
  of the generated model would have skipped, in exchange for isolating a contract change from the
  SQL code.

## Revisit when

- The inventory moves to Redis (D-016 stage 3): `uuidv7()` as a database default no longer
  applies, and a JVM-side UUIDv7 library becomes the natural replacement.
- A query needs type-safe dynamic SQL (jOOQ's own trigger condition, option 3 above).
- An id must not leak its own creation time.
