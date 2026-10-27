# 0027 — Dependency direction: inventory depends on no module

- **Status**: Accepted
- **Date**: 2026-10-25, revised 2026-10-26 (foundation review)
- **Related**: D-004, D-009, D-013, D-014, D-016, D-020, D-024, D-028; replaces the original
  flow 0, where inventory listened to `EventCreated`

## Context

Inventory originally listened to `EventCreated` to create its stock, while `GET /v1/events/{id}`
returned `remaining_seats`, which only inventory knows. Spring Modulith counts listening to
another module's event type as a dependency, so `inventory → event` (listener) plus
`event → inventory` (read of remaining seats) was a cycle, and the verification test fails on
cycles (D-020).

Inventory is meant to be a generic capability (D-004): it should know seats and holds, not what
an "event" is. It also needs, per event, the capacity, the opening time (D-028) and the hold
duration (R5, `hold_duration_seconds` in the contract).

The foundation review (2026-10-26) also found that remaining seats are not needed to prove
iteration 1 (correct holds under concurrency): clients already know whether to retry from the
409 problem types (D-029), and the load test checks the database directly. Remaining seats are
a volatile, high-volume read that belongs to the read path (D-009, D-014).

## Options considered

1. **Event serves the details, inventory exposes availability** — no cycle in reads; changes
   the contract, the client makes two calls, and inventory still listens to `EventCreated`.
2. **Event keeps a projection of remaining seats fed by inventory events** — CQRS-consistent
   (D-009); the cycle remains.
3. **Defer remaining seats; inventory listens to `EventCreated`** — no cycle once the read edge
   is gone, no self-consumed event; inventory knows the event module's types (amends D-004) and
   must change when `EventCreated` changes.
4. **Defer remaining seats; event drives inventory by a call** — event publishes `EventCreated`
   and consumes it itself, after commit, to call `inventory.createStock(...)`; inventory stays
   generic.

## Decision

Option 4. Remaining seats are removed from `GET /v1/events/{id}` in iteration 1 (contract
1.2.0) and deferred to the read path. Inventory depends on no module. Event publishes
`EventCreated` and listens to it itself (after-commit listener, replayed by the Modulith event
publication registry) to call `inventory.createStock(eventId, capacity, opensAt, holdDuration)`,
which is idempotent. Module graph: `event → inventory` (stock creation only),
`booking → inventory`.

## Consequences

- Inventory stays generic and reusable: it changes only for its own needs, never because the
  event module changes. Acyclic graph checked at every build.
- Stock creation is outside the event transaction, so it relies on the registry (at-least-once)
  without a distributed transaction. `createStock` must be idempotent: a replayed publication
  must not create a second stock or reset counters.
- The hold duration and the opening time are copied into the stock; immutability of event data
  in iteration 1 keeps both copies consistent (D-028).
- There is a short window where the event exists but its stock does not. A hold then gets
  `UNKNOWN` from inventory and booking returns `404 /problems/event-not-found`. To keep this
  window before the sale opens, `sale_start_time` must be later than `now()` plus a configurable
  margin, larger than the registry's resubmission delay.
- A stock that is never created (publication stuck in the registry) would make a sale
  impossible: a metric and an alert on incomplete event publications are required.
- A self-consumed event is less obvious to read than a direct call; it must be documented in
  the event module.
- Clients cannot see remaining seats in iteration 1.

## Revisit when

- Remaining seats are designed with the read path (waiting room and position view).
- Incomplete `EventCreated` publications are observed outside of transient failures.
- A second consumer of inventory needs inventory to react to its events (inventory would then
  need its own inbound event contract), or inventory is extracted as a deployable.
