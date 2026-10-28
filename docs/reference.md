# High-Demand Booking Platform — Project reference

Oct 9, 2026 · revised Oct 26, 2026 (foundation review) · @Sami

## 1. Problem

We receive massive, simultaneous booking requests for limited resources over a very short
period. The system must absorb this peak without ever selling more than the capacity: an
oversell is irreversible, whereas waiting is acceptable if the user knows where they stand.

**Reference scenario**: a 40,000-seat concert, sale opening Saturday at 10:00, 1.5 million
people connected.

### Identified risks

- **Integrity**: oversell, double allocation, inconsistent data.
- **Load**: saturated threads and database; the F5 effect amplifies the peak.
- **Replayed requests**: ambiguous timeouts, client retries, mobile networks.
- **Ghost seats**: seats held by users who never pay.
- **Domino effect**: a saturated event must not degrade the others.
- **Malicious traffic**: bots and scalpers (out of MVP).

### Success criteria

- **Zero oversell**: at all times, held + booked seats ≤ capacity. Checked automatically after
  every load test.
- **Visibility**: the position is refreshed according to R13.
- **Responsiveness**: a seat hold is confirmed in under 100 ms at p99, payment excluded.
- **Durability**: a confirmed booking survives a crash.

Guiding principle: losing because demand exceeds supply is normal; losing unfairly or without
knowing it is a system failure.

## 2. Actors

Actors interacting with the system, including non-human and malicious ones (out of MVP).

| Actor | Wants | Can | Failure for them |
| --- | --- | --- | --- |
| Participant | Get a seat | Browse, join the queue, hold, pay | Losing unfairly or without knowing it |
| Organiser | Sell their seats without incident | Create an event, set capacity and sale window, see statistics (after MVP) | Oversell, collapsed sale, loss of credibility |
| Administrator | Sales without incident | MVP: read-only dashboard. After MVP: pause, throttle | Being blind or powerless during an incident |
| Scheduler | — | Publish time events: opening, closing | Missed or duplicated trigger |
| Payment provider (simulated) | — | Process a payment, send a webhook | — |
| Notification service | — | Send email or push | — |
| Scalpers, bots (out of MVP) | Grab seats | Multiple accounts, multiple tabs, automation | — |

A queue alone does not stop a scalper: they get one position per account. IP blocking
penalises legitimate users who share an address.

## 3. MVP scope

The MVP covers a complete journey with general admission, one seat per user, and simulated
payment.

### Participant journey

1. Browses the list of events on sale.
2. Opens an event page and asks for a seat.
3. Enters the waiting room, then the queue; sees their position, the remaining seats and their
   chances.
4. Is admitted: a seat is held and a 5-minute countdown starts.
5. Pays (simulated payment).
6. Receives the booking confirmation.

### Unhappy paths

- **No seat for now**: the user stays informed; they are sent away only when the event is
  definitively sold out.
- **Hold expired before payment**: the seat is released; the user is told and can rejoin at
  the end of the queue.
- **Payment failed**: retry possible within the remaining time, without extension.
- **Late payment**: grace period, then automatic refund.

### MVP features

- Catalogue: list and details of events.
- Waiting room and queue, always active (D-005); position, remaining seats, chances.
- Temporary hold, countdown, automatic expiry.
- Realistic simulated payment: variable latency, failures, timeouts, asynchronous webhooks,
  sometimes duplicated.
- Confirmation displayed and simulated notification, asynchronous.
- Idempotent queue, booking and payment operations.
- Minimal identity: test users, signed token (D-007).
- Admin: event creation through the API, read-only dashboard.
- Observability: metrics and logs of how a sale unfolds.
- Reproducible load test, with an automatic invariant check.

### After the MVP

Several seats per user · event modification (dates immutable in iteration 1, D-028) · live
admin actions · organiser interface and analytics · disabling the queue · numbered seats,
categories, prices · real payment provider · protection against bots and scalpers · real
authentication.

### Out of the project for now

Peer-to-peer resale · on-demand refunds · multi-tenancy.

## 4. Business rules

Each rule must be able to become an automated test.

| Id | Subject | Rule |
| --- | --- | --- |
| R1 | Waiting room | Opens 30 min before the sale (configurable), with no capacity limit. Before it opens and after the sale closes: browsing only. |
| R2 | Fairness | At sale opening, those present receive a randomly drawn rank; later arrivals are queued FIFO behind them. |
| R2b | Information | Each participant sees their position, the remaining seats and an estimate of their chances; they can leave the queue at any time. |
| R3 | Uniqueness | A user has at most one active hold OR one booking per event, including under concurrent requests. |
| R4 | Queue idempotency | Joining the queue is idempotent per (user, event): a new request returns the existing position. |
| R5 | Hold | On admission, the inventory holds a seat for 5 min (configurable), with a countdown. |
| R6 | Retry | After a failed payment, retry is possible within the remaining time, without extension. |
| R7 | Grace | If a payment is in progress at expiry, the seat stays held 1 more minute (configurable). |
| R8 | Late payment | A payment confirmed after the seat was released triggers an automatic, idempotent refund. |
| R9 | Admission | The next participant is admitted if a seat is available AND the admission rate, set from the measured inventory throughput, is not exceeded. A seat becomes available again when a hold expires. |
| R10 | Sold out | "No seat for now" while holds are active; "sold out" only when every seat is booked. |
| R11 | Capacity | Increase allowed during the sale; reduction not supported in the MVP (later: limited to seats neither held nor booked). |
| R12 | Invariant | At all times, held + booked seats ≤ capacity. |
| R13 | Refresh | Interval imposed by the server according to the estimated wait: < 2 min → 5 s; 2 to 15 min → 15 s; > 15 min → 60 s. |
| R14 | Payment outage: detection | Automatic detection by a circuit breaker on the payment gateway. |
| R15 | Payment outage: admissions | During the outage, admissions are suspended. |
| R16 | Payment outage: holds | Active countdowns are frozen during the outage (accepted exception to R6). |
| R17 | Payment outage: recovery | Participants and the administrator are informed; admissions and countdowns resume automatically on recovery. |

R7 covers a slow payment on the user's side; R16 covers an outage on our side.

## 5. Load estimates

Position reads dominate the load by a factor of about 100, but the critical path is a
contention problem, not a volume problem.

| Operation | Peak (reference scenario) | Nature of the problem |
| --- | --- | --- |
| Position read, fixed every 10 s | \~150,000 req/s | Volume, tolerates imprecision |
| Position read, with R13 | \~60,000 req/s, mostly absorbed by the cache and the CDN (D-014) | Volume |
| Waiting room entries (worst case: 750,000 in 2 min) | \~6,000 req/s | Volume, writes |
| Holds, admission at 500/s | 500 writes/s on a single counter | Contention, consistency |
| Payments | \~130 req/s | Low |

Sale duration, with no hold limit: \~98% of seats sold in 12 min, tail up to 25 to 30 min (30%
expiries per wave). First wave of 40,000 admissions at 500/s: \~80 s.

The 500 holds/s throughput is an assumption to be measured by the load test.

## 6. Architecture

Three deployables (D-017): a modular monolith, the position view and the payment simulator.

[embedded content: MVP architecture · 3 deployables]

Position reads bypass the monolith; only the admission cursor reaches them. The inventory,
highlighted, owns holds and bookings.

### Communication between components

| Interaction | Mode | Why |
| --- | --- | --- |
| Admission → Inventory: hold a seat | Synchronous | Admission needs the result to continue. Orchestration between admission, booking and inventory is still to be designed (section 8) |
| Inventory → Booking: hold created, confirmed, expired | Asynchronous, through the outbox (D-013) | Booking only needs to know, reliably; no consumer in iteration 1 (D-024) |
| Booking → Payment: start a payment | Synchronous, idempotent (D-015) | The payment URL is needed immediately |
| Payment → Booking: result | Asynchronous: webhook acknowledged, then processed | The provider answers later, sometimes twice |
| Booking → Notification | Asynchronous (D-011) | Outside the critical path |
| Admission → Position view: cursor | Asynchronous (D-014) | Reads tolerate a slight delay |
| Scheduler → Queue: sale opened | Asynchronous, idempotent (D-012) | A duplicated trigger has no effect |

## 7. Decision log

Decisions are grouped in five tables, most recent first. This first table holds the
architecture decisions. Each decision has its ADR in `docs/decisions/`. An accepted ADR is not
rewritten: a change is a new ADR that amends it (partial change) or supersedes it (full
change). This rule applies from the first commit of the ADRs (ticket 1).

| Id | Decision | Why | Accepted consequences |
| --- | --- | --- | --- |
| D-017 | Modular monolith + two separate deployables: position view and payment simulator | The read path needs separate computing resources; the simulator must reproduce a real network call. Other isolations are handled with bulkheads | A deployment restarts every module. First teaching extraction: Notification. Real signal: a fix needed during a sale |
| D-016 | Inventory in three stages: Postgres conditional update + CHECK (MVP) → one row per seat + SKIP LOCKED → atomic Redis | The queue caps the arrival rate; approach 1 is enough, with strong durability | Throughput capped by the database. Signals: numbered seats or a first wave > 2 min (to 2); capacity exceeded by several sales (to 3). Stable contract, interchangeable implementations |
| D-015 | Idempotent payment calls (same key on retry), reconciliation, webhooks acknowledged then processed idempotently | A timeout does not say whether the payment was created | Reconciliation logic to maintain |
| D-014 | Position = rank − admission cursor; a single cursor per event, served by cache and CDN | The rank is fixed after the draw; only the cursor moves | Pessimistic position (abandons not deducted); slight propagation delay |
| D-013 | Events published through a transactional outbox | Avoids the database + broker dual write | At-least-once delivery, idempotent consumers; relay provided by the Spring Modulith registry (D-020); an equivalent to find if the inventory moves to Redis |
| D-012 | The scheduler publishes the "when"; each component executes the "what" idempotently | Concentrates time and multi-instance handling | One more component; it triggers business moments, while expiries stay with the module that owns the data (D-031) |
| D-011 | Asynchronous notification, outside the critical path, with retries | A non-critical component must never make a critical one fail | Delayed confirmation email |
| D-010 | The hold belongs to the inventory; atomic conversion "hold → booking if still active" | A single source of truth; the expiry/payment race is settled by one operation | Booking depends on inventory events; its status can briefly lag |
| D-009 | Separate the read path (position) from the booking path (CQRS) | \~150,000 reads/s (fixed 10 s refresh; \~60,000 with R13) that tolerate errors, against a few thousand writes/s that do not | Slightly lagging position; two models to keep in sync |
| D-008 | Visible expiry and actual release decoupled (grace period) | Make late payments rarer rather than only handling them | Seat immobilised 1 more minute |
| D-007 | Minimal identity: test users and signed token | Authentication is not a learning goal and would hinder load tests | Component to replace before any real exposure |
| D-006 | Lottery at opening, then FIFO | Order no longer depends on milliseconds; connections spread out | Arriving very early gives no advantage; atomic draw at opening |
| D-005 | Queue always active in the MVP | The queue guarantees order, not only load handling; a single path | Instant but useless step for low-demand events |
| D-004 | Concrete domain language (Event, Hold, Booking); only the inventory is generic | Generalising without a second use case is guessing | Possible renames later |
| D-003 | The server assumes retries will happen: client-generated idempotency key | The client cannot be controlled | Keys to store |
| D-002 | Integrity before availability on the booking path | Unavailability is recoverable, an oversell is not | The system slows down or refuses rather than risk an oversell |
| D-001 | Learning comes before any commercial goal | Project goal | More complexity than the real need, each addition tied to a learning goal |

### Working decisions

Decisions on the way of working and the stack, taken before the first line of code.

| Id | Decision | Why | Accepted consequences |
| --- | --- | --- | --- |
| D-022 | Stack: Java 25 (LTS), Spring Boot 4.1.x and the compatible Spring Modulith version, Maven, Flyway, Postgres 18 (same major version locally, in CI and in the cloud) | Supported versions; familiar tools where novelty brings nothing | Forward-only Flyway migrations; virtual threads: the connection pool becomes the limit and must be protected explicitly |
| D-021 | Contract-first API: OpenAPI contract written and validated before implementation; errors as Problem Details (RFC 9457); version in the URL (/v1) | The API is designed before it is coded; Claude Code implements a validated contract | Contract and code to keep in sync |
| D-020 | Spring Modulith to enforce module boundaries, and its event registry as the outbox | Boundaries checked automatically on every build; outbox provided (D-013) | One more dependency and its conventions |
| D-019 | Kanban on GitHub Issues + Projects, WIP limit of 2; ticket number in every branch and commit | Solo work without sprints; tickets linked to commits and pull requests | Discipline to keep the board up to date |
| D-018 | Trunk-based development: one main branch, short branches per ticket, merge through pull requests; the same artefact is promoted from one environment to the next | Less divergence; what is deployed is what was tested | The main branch must always stay deployable |

### Iteration 1 decisions

Decisions from the flow review.

| Id | Decision | Why | Accepted consequences |
| --- | --- | --- | --- |
| D-032 | Backend layout: one Maven reactor in `backend/`, aggregator and parent of every deployable, wrapper included; one module and one root package per deployable (`com.schakir.hdb.<deployable>`); the monolith's modules live under `com.schakir.hdb.core.<module>`. The parent inherits `spring-boot-starter-parent` and imports the Spring Modulith BOM | One build command, one CI job, shared build configuration (Java, Spring Boot, Spring Modulith, Spotless) in one place; the repository root stays technology-neutral for a later, non-Maven frontend; a class's package names its deployable in logs and stack traces, and component scanning never crosses deployables | Deployables share one version number and one Spring Boot version by default, tempting lockstep upgrades and releases; a shared `common` module would couple deployables and is deferred until the second one arrives; commands run from `backend/`, not the repository root |
| D-024 | The booking API stays exposed by booking, facade and future orchestrator; its state is fed entirely by inventory events (HoldCreated, HoldConfirmed, HoldExpired) | The public contract stays stable when admission and payment arrive; the inventory stays an internal, generic capability (D-004); no more dual write in flow A | In iteration 1, booking is a stateless facade: all its operations, reads included, call the inventory; the projection is deferred until payment arrives |
| D-023 | Hold idempotent by business key (event, user) rather than by idempotency key; a replayed request receives the existing active hold | The unique index on (event, user) already guarantees uniqueness; a technical key would be redundant | A second request from the same user, retry or another tab, receives the same hold. The key will be needed again with several seats per user. D-003 stays the principle; the mechanism is chosen per operation |

### Foundation decisions

Decisions that close the foundation phase, from the iteration 1 ticket review and the
foundation review.

| Id | Decision | Why | Accepted consequences |
| --- | --- | --- | --- |
| D-031 | Hold expiry by a periodic inventory sweep, beyond iteration 1. The scheduler keeps the business moments (waiting room opening, sale opening and closing) | Up to \~40,000 expiries per sale, on data the inventory owns; correctness does not depend on the sweep (every check applies `expires_at <= now()`) | A released seat comes back at most one sweep interval late; batch size and interval to tune |
| D-030 | Simulated identity: the contract keeps the Bearer scheme; a script generates signed tokens for N test users and one administrator; Spring Security validates them with a local key | R3 and load tests need distinct identities taken from the token, without making authentication a goal (D-007) | Token issuance never exists in the production profile; component to replace before any real exposure |
| D-029 | R10 from iteration 1: `confirmed` counter on the stock row, updated in the confirmation transaction. Failed decrement: `confirmed = capacity` → `/problems/event-sold-out` (final); otherwise `/problems/seats-temporarily-unavailable` with `Retry-After: 30` | Without a queue, retrying is the client's only recourse: it must know whether retrying is worth it | An expired hold not yet swept extends the "temporary" state; `Retry-After` is only a hint, real protection will come from rate limiting and the queue; contract change |
| D-028 | Sale opening: the inventory stores `opens_at` and refuses any hold while `now() < opens_at`, inside its conditional update. Event dates immutable in iteration 1 (no rescheduling). Source of truth for time: the PostgreSQL clock (`now()`, transaction start; `clock_timestamp()` only if a need appears) | Atomic check with the decrement, without a synchronous call; one clock for all instances | `opens_at` exists in two modules, kept consistent by immutability; tests move the data, not the time; from iteration 2, defence in depth behind admission; every time comparison uses the database clock |
| D-027 | Dependency direction: the inventory, a generic component (D-004), depends on no module. `remaining_seats` is removed from `GET /v1/events/{id}` in iteration 1 (contract 1.2.0) and deferred to the read path. Event publishes `EventCreated` and listens to it itself (after-commit listener, replayed by the Modulith registry) to call `inventory.createStock(eventId, capacity, opensAt, holdDuration)`, idempotent. Without stock: a hold gets UNKNOWN then `404 event-not-found`; `sale_start_time` must exceed `now()` by a configurable margin | The inventory stays generic: it changes only for its own needs; no more event ↔ inventory cycle | Graph: `event → inventory` (stock creation), `booking → inventory`; stock-less window, always before opening; metric and alert on incomplete registry publications; self-consumed event to document; no remaining seats visible in iteration 1 |

### Setup decisions

| Id | Decision | Why | Accepted consequences |
| --- | --- | --- | --- |
| D-026 | Iteration 1 entirely local and in CI; tooling: k6 for load, Prometheus + Grafana for local observability, google-java-format through Spotless enforced in CI; decisions as one ADR file per decision in `docs/decisions/` | The iteration's goal can be proven without the cloud; free, standard tools; stable formatting despite AI-generated code; ADRs readable by a recruiter | No measurement in a cloud environment during iteration 1; more files to maintain |
| D-025 | OpenAPI 3.1.1 contract, snake\_case properties, ISO 8601 UTC dates; `GET /v1/holds/{holdId}` added | Recent version with mature tooling support; the client must be able to restore its countdown | Explicit Jackson configuration; generator's 3.1 support to verify |

## 7b. Iteration 1: flows between modules

Scope: an event created by the administrator, test users with tokens, no queue and no payment.
A user holds a seat, then confirms it before it expires.

### Flow 0: event creation (D-027)

1. Admin → Event: `POST /v1/events` with capacity, sale opening and hold duration ·
   synchronous. The opening must exceed `now()` by a configurable margin, larger than the
   registry's resubmission delay. One transaction: event insert and `EventCreated` in the
   outbox.
2. Event → Event: an after-commit listener of the event module receives `EventCreated`,
   replayed by the Modulith registry on failure · asynchronous.
3. Event → Inventory: `createStock(event, capacity, opensAt, holdDuration)` · synchronous,
   idempotent. The inventory creates the stock row (event, capacity, available seats,
   confirmed = 0, `opens_at`, hold duration).
4. Stock-less window (a few milliseconds, always before opening thanks to the margin): a hold
   gets UNKNOWN from the inventory and booking returns `404 event-not-found` (D-027).
   `GET /v1/events/{id}` is served by the event module alone, without remaining seats. A
   publication left incomplete raises an alert.
5. Event dates are immutable in iteration 1 (D-028).

### Flow A: hold a seat

1. Client → Booking: `POST /v1/events/{id}/holds` with the token · synchronous.
2. Booking → Inventory: `hold(event, user)` · synchronous, in process.
3. Inventory, one transaction: if the user has an active, non-expired hold, it is returned
   as is; if they have an active but expired hold, it is expired first (conditional update,
   re-increment); otherwise, conditional decrement of the counter (`available > 0` and
   `now() >= opens_at`, D-028), hold insert (`expires_at = now()` + the stock's hold
   duration), `HoldCreated` in the outbox. If the decrement fails, the stock row decides:
   sale not open, sold out (`confirmed = capacity`) or temporarily unavailable (D-029).
4. Response: `201` new hold · `200` existing hold · `404` unknown event or stock not created
   yet · `409` sold out, final · `409` seats temporarily unavailable, with `Retry-After: 30` ·
   `409` already booked · `409` sale not open.
5. `HoldCreated` is published; no consumer in iteration 1 (D-024).

### Flow B: confirm

1. Client → Booking: `POST /v1/holds/{id}/confirmations` · synchronous.
2. Booking → Inventory: `confirm(holdId, user)`.
3. Inventory, one transaction: conditional update (active, not expired according to `now()`,
   same user), increment of the stock row's `confirmed` counter (D-029), `HoldConfirmed` in
   the outbox.
4. Response: `200` confirmed, including on a double click · `409` expired · `404` not found or
   owned by another user.
5. `HoldConfirmed` is published; no consumer in iteration 1.
6. Read: `GET /v1/holds/{id}` → Booking → inventory `getHold(holdId, user)`; status `EXPIRED`
   as soon as `expires_at <= now()`, with no lag after a confirmation.

### Flow C: expire

1. Inventory scheduler: selects a batch of expired active holds (`FOR UPDATE SKIP LOCKED`).
2. Per hold, one transaction: conditional update "active → expired"; if it succeeds,
   re-increment of the counter and `HoldExpired` in the outbox.
3. `HoldExpired` is published; no consumer in iteration 1. The sweep stays with the inventory
   beyond iteration 1 (D-031).

### Inventory public interface

| Element | Signature |
| --- | --- |
| Operation | `createStock(event, capacity, opensAt, holdDuration)` → idempotent (D-027) |
| Operation | `hold(event, user)` → Held {id, expiry, new or existing} · SoldOut · TemporarilyUnavailable · AlreadyBooked · NotOpen · Unknown |
| Operation | `confirm(holdId, user)` → Confirmed · Expired · NotFound |
| Operation | `getHold(holdId, user)` → Hold {id, event, status, expiry, confirmation} · NotFound |
| Published events | `HoldCreated`, `HoldConfirmed`, `HoldExpired` |
| Listened event | none (D-027) |

Dependencies (D-027): Event → Inventory (stock creation) and Booking → Inventory, by calls.
Nobody listens to inventory events in iteration 1. The user module only validates the
simulated tokens (D-030); the current user comes from the security context. The inventory
depends on no module. No cycle. No public release operation.

## 8. Open questions

- [ ] How long should idempotency keys be kept?
- [ ] What to do with a retry received while the original request is still in progress?
- [x] Which source of truth for time across several scheduler instances? → the PostgreSQL
  clock (D-028).
- [ ] How to run the lottery atomically over a large number of entries?
- [ ] How to detect abandons in the queue to refine position and chances?
- [x] How to implement hold expiry: periodic sweep or delayed messages? → inventory sweep
  (D-031).
- [ ] Which broker for events between modules and towards the position view?
- [ ] Real throughput of the Postgres inventory, to measure (assumption: 500 holds/s).
- [ ] Remaining seats: who serves them, at what rate and with what freshness, on the read path
  (deferred by D-027)?
- [ ] Sale closing and past events (`event_date`): which check, and where (R1)?
- [ ] Orchestration in iteration 2: does admission call booking or the inventory?
- [ ] Manual sale start by the administrator: real need (demo, test, incident)? Tied to live
  admin actions, after the MVP.
