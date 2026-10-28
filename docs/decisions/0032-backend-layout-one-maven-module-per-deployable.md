# 0032 — Backend layout: one Maven reactor, one module and one root package per deployable

- **Status**: Accepted
- **Date**: 2026-10-28
- **Related**: D-017, D-020, D-022, D-026

## Context

D-017 plans three deployables: the modular monolith, the position view and the payment
simulator. Iteration 1 builds only the monolith, but its build layout and package naming set the
shape the other two will follow. A frontend may come later with its own, non-Maven tooling. The
original package convention, `com.schakir.booking.<module>`, produced
`com.schakir.booking.booking` for the booking module.

## Options considered

1. **One Maven reactor at the repository root** — one command from the root; Maven owns the
   root, where a non-Maven frontend would also live.
2. **One Maven reactor in `backend/`** — `backend/pom.xml` is the aggregator and the parent of
   every deployable, with the Maven wrapper next to it; the repository root stays
   technology-neutral; commands run from `backend/`.
3. **Independent Maven projects per deployable** — full isolation; build configuration, wrapper
   and CI job duplicated for each.

For packages: one shared root (`com.schakir.hdb.<module>`) or one root per deployable
(`com.schakir.hdb.<deployable>.<module>`).

## Decision

Option 2. `backend/` is the Maven root; each deployable is one module of the reactor, with its own
`pom.xml` and its own artifact: `backend/core` for the modular monolith, later
`backend/position-view` and `backend/payment-simulator`. The parent inherits
`spring-boot-starter-parent` and imports the Spring Modulith BOM.
Each deployable has its own root package, `com.schakir.hdb.<deployable>`; the monolith's modules
live under `com.schakir.hdb.core.<module>`.

## Consequences

- One build command, one CI job and one place for shared build configuration (Java version,
  Spring Boot, Spring Modulith, Spotless).
- Each deployable still builds and deploys alone (`./mvnw -pl <module> -am package`).
- Deployables share one version number and one Spring Boot version by default: upgrading the
  parent touches all of them at once, and releasing them in lockstep becomes tempting.
- A shared `common` module becomes tempting and would couple the deployables; forbidding it is
  tracked in a deferred issue, to be done when the second deployable arrives.
- The package of a class names its deployable in logs and stack traces; component scanning and
  Spring Modulith detection never reach another deployable's classes.
- Commands run from `backend/`, not from the repository root.

## Revisit when

A deployable needs a different Spring Boot version or its own release cadence, or a frontend
needs tooling at the repository root that conflicts with this layout.
