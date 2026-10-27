# High-Demand Booking Platform

A learning project for software architecture: sell a limited number of seats under a massive
simultaneous demand, without ever overselling. Correctness beats availability (D-002).

**Iteration 1 scope:** create an event, hold a seat, confirm a hold, hold expiry.
No waiting room, no payment, no frontend.

## Repository layout

| Path | Content |
|---|---|
| `contracts/openapi.yaml` | API source of truth (OpenAPI 3.1.1) |
| `backend/` | Modular monolith (Spring Boot, Spring Modulith) |
| `load-tests/` | k6 scenarios |
| `docs/reference.md` | Project reference: problem, actors, business rules, flows |
| `docs/decisions/` | Architecture Decision Records, indexed in `docs/decisions/README.md` |
| `docs/fr/` | French translation of `docs/` (English is the source) |
| `CLAUDE.md` | Rules for the AI coding assistant |

## Workflow

- Every change starts from an issue, on a short branch `<issue-number>-<short-slug>`,
  and reaches `main` through a pull request (D-018, D-019).
- Board: Backlog → Ready → In progress (WIP 2) → In review → Done.
- Commits follow Conventional Commits with the issue number, e.g.
  `feat(inventory): hold a seat atomically (#9)`.
- A decision that changes gets a new ADR that amends or supersedes the earlier one.
- A document change updates its French translation in `docs/fr/` in the same PR.

## Stack

Java 25 · Spring Boot 4.1.x · Spring Modulith · Maven · Flyway · PostgreSQL 18 ·
Testcontainers · k6 · Prometheus + Grafana · google-java-format (Spotless)
