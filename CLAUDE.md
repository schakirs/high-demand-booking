# CLAUDE.md

## Project

High-Demand Booking Platform: a learning project for software architecture.
It sells a limited number of seats under a massive simultaneous demand, without ever
overselling. Correctness beats availability: when in doubt, slow down or reject (D-002).

Architecture decisions live in `docs/decisions/` (one ADR per decision): the rules below say
what to do, the ADRs say why. Read the relevant ADRs before changing anything they cover.
Business rules (R1, R2…) are defined in `docs/reference.md`.

## Scope — iteration 1

In scope: create an event, hold a seat, confirm a hold, read a hold, hold expiry.

Out of scope — do not add, even if it looks missing:
- remaining seats in any response (deferred to the read path, D-027);
- state, tables or event consumers in `booking` (stateless facade, D-024);
- waiting room, queue, admission, payment, notification;
- sale closing, checks on `event_date`, event modification or rescheduling (D-028);
- live admin actions (manual sale start, pause);
- a frontend.

## Stack

Java 25 · Spring Boot 4.1.x · Spring Modulith · Maven · Flyway · PostgreSQL 18 ·
Testcontainers · k6 · Prometheus + Grafana · google-java-format (Spotless)

## Repository layout

```
contracts/openapi.yaml   API source of truth (OpenAPI 3.1.1)
backend/                 modular monolith (Spring Boot)
load-tests/              k6 scenarios
docs/reference.md        project reference: problem, business rules, flows
docs/decisions/          ADRs
docs/fr/                 French translations of docs/
docker-compose.yml       PostgreSQL 18, Prometheus, Grafana
```

## Architecture rules — non-negotiable

1. **Modular monolith.** Modules: `event`, `user`, `inventory`, `booking`, under
   `com.schakir.booking.<module>`. A module's public API is its top-level package; everything in
   sub-packages is internal. Never use another module's internal types.
   The Spring Modulith verification test must always pass.
2. **Dependencies (D-027).** Allowed: `event → inventory` (stock creation only) and
   `booking → inventory` (calls). `inventory` never calls nor listens to another module.
   Nothing depends on `booking`. `user` only validates tokens; the current user comes from the
   security context. No cycles. `event` creates the stock from its own `EventCreated`
   (after-commit listener) through the idempotent
   `inventory.createStock(eventId, capacity, opensAt, holdDuration)`.
3. **No transaction spans two modules.** Cross-module effects go through events published
   with the Spring Modulith event publication registry (transactional outbox, D-013).
4. **Inventory is the single source of truth** for seats and holds (D-010).
   Every seat or hold state change is a single atomic conditional SQL update, e.g.
   `UPDATE ... SET available = available - 1 WHERE event_id = ? AND available > 0`.
   Never read-then-write. Never use in-memory locks (`synchronized`, `ReentrantLock`, caches)
   to protect seat logic: the application runs on several instances.
5. **Time comes from PostgreSQL (D-028).** `now()` in SQL is the single source of truth for
   time: never compare or compute business times with the JVM clock. Use `now()`, not
   `clock_timestamp()`, unless approved.
   - A hold is refused while `now() < opens_at`, inside the same conditional update as the
     seat decrement.
   - `expires_at = now() + the stock's hold_duration`, computed in SQL.
   - A hold is expired as soon as `expires_at <= now()`, whatever its status column says;
     every check (confirm, read, new hold) applies this rule.
   - Event creation rejects a `sale_start_time` not later than `now()` plus the configured
     margin (D-027).
6. **Sold out vs temporarily unavailable (D-029).** The stock row holds `capacity`, `available`
   and `confirmed`. When the decrement fails: `confirmed = capacity` → `event-sold-out`;
   otherwise `seats-temporarily-unavailable` with `Retry-After: 30`. `confirmed` is
   incremented in the confirmation transaction.
7. **Event consumers are idempotent.** Delivery is at-least-once.
8. **The API contract is the source of truth.** Controllers implement the interfaces
   generated from `contracts/openapi.yaml`. Never add an endpoint, field, header or problem
   type that is not in the contract. Errors use RFC 9457 Problem Details with
   `type: /problems/<kebab-case-name>`.

## Never do without explicit approval

- Modify `contracts/openapi.yaml`.
- Modify, weaken, skip or delete invariant tests (tagged `@Tag("invariant")`).
- Add or upgrade a dependency.
- Change the database schema other than by a new Flyway migration.
  Never edit a migration that has already been applied.
- Modify anything under `docs/` (reference, ADRs, translations) or this file.
  Never rewrite the body of an accepted ADR: propose a new ADR that amends or supersedes it.
- Disable a test, lower an assertion, or catch and ignore an exception to make a build pass.
- Push to `main` or merge a pull request.

If a request conflicts with a rule or a decision, or is ambiguous, stop and ask.
Do not guess.

## Conventions

- JSON properties in snake_case (global Jackson configuration).
- Times: `Instant` in Java, `timestamptz` in PostgreSQL, ISO 8601 UTC in the API.
- Identifiers: UUID.
- Logs: structured JSON, with a correlation ID on every request.
- Identity (D-030): bearer tokens signed with a local key, generated by a script for test users
  and an admin. Never add token issuance to the application.
- Formatting: google-java-format through Spotless. Run `./mvnw spotless:apply` before committing.
- Language: code, comments, commits and documentation in English. Documents under `docs/` have
  a French translation in `docs/fr/`, updated in the same pull request. This file and the
  contract are English only.

## Testing

- **Unit tests**: domain rules, plain JUnit, no Spring context.
- **Integration tests**: anything touching the database uses Testcontainers with
  PostgreSQL 18. Never H2, never a mocked repository for SQL behaviour.
- **Concurrency tests** for inventory: many concurrent threads, then assert the invariants
  (no overselling, at most one active hold or booking per user and event).
- **Module tests**: Spring Modulith verification and module-level tests.
- **Time in tests**: move the data, not the time. SQL fixtures place `expires_at` or
  `opens_at` in the past; never fake the clock. Await asynchronous work (expiry sweep, event
  listeners) with Awaitility, never `Thread.sleep`.
- A bug fix starts with a failing test that reproduces it.

## Workflow

- One ticket per Claude Code session (the board allows 2 in progress, D-019).
  Branch name: `<issue-number>-<short-slug>`.
- Before writing code, propose a plan: files to touch, approach, tests to add.
  Wait for approval.
- Small commits, Conventional Commits format, with the issue number:
  `feat(inventory): hold a seat atomically (#12)`.
- Before saying a task is done: `./mvnw verify` is green, formatting applied, and a short
  explanation of what changed in each file.

## Commands

```
docker compose up -d          # PostgreSQL, Prometheus, Grafana
./mvnw verify                 # build, all tests, Modulith verification, format check
./mvnw spotless:apply         # format the code
k6 run load-tests/<file>.js   # run a load scenario
```
