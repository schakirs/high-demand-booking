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
contracts/redocly.yaml   Redocly lint config (recommended ruleset + project rules, D-036)
contracts/package.json   pins @redocly/cli; package-lock.json committed
backend/                 Maven root (backend/pom.xml, backend/mvnw, backend/.mvn/)
backend/core             modular monolith (Spring Boot, Spring Modulith)
load-tests/              k6 scenarios
docs/reference.md        project reference: problem, business rules, flows
docs/decisions/          ADRs
.local/identity/         local ES256 key pair for test tokens (gitignored, D-038)
docker-compose.yml       PostgreSQL 18, Prometheus, Grafana
observability/           Prometheus scrape config, Grafana datasource provisioning
```

The API interfaces and models are generated from `contracts/openapi.yaml` at build time
(D-036), into `backend/core/target/generated-sources/openapi/...`, never committed. A
contract change regenerates them on the next build.

## Architecture rules — non-negotiable

1. **Modular monolith.** Every deployable has its own root package: `com.schakir.hdb.<deployable>`
   (D-032). The monolith's deployable is `core`; its modules are `event`, `user`, `inventory`,
   `booking`, under `com.schakir.hdb.core.<module>`. A module's public API is its top-level package;
   everything in sub-packages is internal. Never use another module's internal types. The Spring
   Modulith verification test must always pass. A module that owns data has its own PostgreSQL
   schema, named after the module (D-033): SQL always schema-qualifies table names
   (`inventory.stock`), never relies on `search_path`, and a module never reads or writes another
   module's schema. A module's migrations live in `db/migration/<module>/` (Spring Modulith's
   module-specific Flyway support, D-033).
2. **Dependencies (D-027).** Allowed: `event → inventory` (stock creation only) and
   `booking → inventory` (calls). `inventory` never calls nor listens to another module.
   Nothing depends on `booking`. `user` owns token validation and the security filter chain
   (D-038); it may depend on `web` (to build its 401/403 problems) and nothing depends on
   `user` — it is declared `@ApplicationModule(allowedDependencies = "web")`. Controllers read
   the caller with `@AuthenticationPrincipal Jwt` (a Spring Security type, not a `user` type)
   and pass a plain `UUID` (the token's `sub`) to the domain; the domain never sees Spring
   Security types. A user id is never read from the request (header, path, query or body).
   No cycles. `event` creates the stock from its own `EventCreated`
   (after-commit listener) through the idempotent
   `inventory.createStock(eventId, capacity, opensAt, holdDuration)`.
   The `web` module (shared HTTP/problem-details plumbing, D-037) depends on no other module;
   `event` and `booking` may depend on it; `inventory` never does — `inventory` is declared
   `@ApplicationModule(allowedDependencies = {})`, so the Modulith verification test rejects
   any dependency it might grow, on `web` or anything else.
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
9. **Error handling (D-037).** `web`'s `TechnicalProblemHandler` (lowest precedence) turns
   validation failures, malformed requests and an unknown JSON property into
   `400 /problems/validation-error`, and any other unhandled exception into
   `500 about:blank` (logged at ERROR with its stack trace, nothing technical in the body).
   Each module adds its own `@RestControllerAdvice` for its business problems, ordered ahead
   of the shared one. Build every problem response with `ProblemDetailFactory` — never
   `ResponseEntity.body(problemDetail)` directly, since Spring MVC fills a null `instance` with
   the request URI otherwise.
10. **Security (D-038).** Every API operation has an explicit URL rule in `user`'s
    `SecurityFilterChain`; anything not listed is `denyAll()` (fail closed). Admin
    operations require `SCOPE_admin`. Never use `@PreAuthorize` or other method security:
    an `AccessDeniedException` raised inside MVC would become a 500 through
    `TechnicalProblemHandler`. Adding an operation to the contract means adding its rule;
    the contract-driven security test fails the build otherwise. Only `/actuator/health`
    and `/actuator/prometheus` are public — never open `/actuator/**`.

## Never do without explicit approval

- Modify `contracts/openapi.yaml`.
- Modify, weaken, skip or delete invariant tests (tagged `@Tag("invariant")`).
- Add or upgrade a dependency.
- Change the database schema other than by a new Flyway migration.
  Never edit a migration that has already been applied.
- Change the connection pool settings (D-034).
- Change `.github/workflows/` or the pinned action versions.
- Modify anything under `docs/` (reference, ADRs) or this file.
  Never rewrite the body of an accepted ADR: propose a new ADR that amends or supersedes it.
- Disable a test, lower an assertion, or catch and ignore an exception to make a build pass.
- Push to `main` or merge a pull request.

If a request conflicts with a rule or a decision, or is ambiguous, stop and ask.
Do not guess.

## Conventions

- JSON properties in snake_case (global Jackson configuration). An absent optional field is
  omitted, never sent as a JSON `null` (`spring.jackson.default-property-inclusion: non_null`):
  a nullable field would otherwise violate its own typed schema.
- Times: `Instant` in Java, `timestamptz` in PostgreSQL, ISO 8601 UTC in the API.
- Identifiers: UUID.
- Logs: structured JSON, with a correlation ID on every request.
- Identity (D-030, D-038): JWT signed with ES256; the application holds the public key only
  (from configuration, no default: the application does not start without it). Validated:
  `exp`/`nbf` with a 60 s clock skew, `iss = hdb-token-script`, `aud` contains `hdb-core`,
  `sub` is a UUID; admin = scope `admin`. Tokens are generated by `TestTokens` in
  `backend/core/src/test` — never add token issuance or a private key to `src/main`.
  Keys live in `.local/identity/` and tokens in `load-tests/tokens.json`, both gitignored.
- Formatting: google-java-format through Spotless. Run `./mvnw spotless:apply` before committing.
- Language: code, comments, commits and documentation in English only.

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

Maven commands run from `backend/`.

```
docker compose up -d          # PostgreSQL, Prometheus, Grafana
./mvnw verify                 # build, all tests, Modulith verification, format check
./mvnw spotless:apply         # format the code
./mvnw -pl core test-compile exec:java@tokens -Dtokens.users=1000 -Dtokens.ttl=PT1H
                              # ES256 keys (.local/identity/) + tokens (load-tests/tokens.json)
k6 run load-tests/<file>.js   # run a load scenario
```

To lint the contract alone, without the rest of the build, run from `contracts/`:
```
npm ci --ignore-scripts && npm run lint
```
