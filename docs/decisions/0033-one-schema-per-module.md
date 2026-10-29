# 0033 — One PostgreSQL schema per module

- **Status**: Accepted
- **Date**: 2026-10-29
- **Related**: D-017, D-020, D-027

## Context

Spring Modulith checks module boundaries in code, not in SQL: nothing in PostgreSQL stops a
query from crossing a boundary that only the build enforces. Several modules share one
database (iteration 1: `event` and `inventory`; `booking` is a stateless facade, D-024, and
`user` has no table, D-030). A module may later be extracted as its own deployable (D-017),
and its data should be easy to tell apart and to move.

## Options considered

1. **One `public` schema, table names prefixed by module** — simplest to query across
   modules; the boundary is only a naming convention, easy to cross by accident.
2. **One schema per module that owns data, one application role for all schemas** — the
   boundary is visible in `information_schema`; a module never reads or writes another
   module's schema in its SQL; the role itself does not enforce this (no per-schema grants).
3. **One schema and one dedicated role per module** — the database itself would refuse a
   cross-module query; more roles and grants to manage for a solo iteration 1.

## Decision

Option 2. Each module that owns data gets its own PostgreSQL schema, named after the module
(`event`, `inventory`). Flyway owns the whole schema, using Spring Modulith's module-specific
migration support (2.x): each module has its own migration folder
(`db/migration/<module>`) and its own Flyway history table
(`flyway_schema_history_<module>`), versioned independently of other modules. A module's
first migration creates its own schema and nothing else. SQL always schema-qualifies table
names (`inventory.stock`), never relies on `search_path`. One application database role for
all schemas; the boundary is a convention enforced in code (Spring Modulith), not by
PostgreSQL grants. Spring Modulith's `event_publication` table is infrastructure, not a
module's data, and its root migration (`db/migration/__root`) arrives with #7.

Spring Modulith's module-specific Flyway support customizes only the migration **location**
and the history **table name** per module; it does not set a `schemas`/`defaultSchema` per
module (read directly from `SpringModulithFlywayMigrationStrategy` at tag `2.1.2`). With
`spring.flyway.schemas` left unset, every history table — the root's `flyway_schema_history`
and each module's `flyway_schema_history_<module>` — lands in the one schema Flyway defaults
to, `public`. Putting a module's history table inside that module's own schema would need
custom Flyway configuration beyond what this framework version offers; not done here.

## Consequences

- Module boundaries are visible in `information_schema.schemata`.
- No version collisions between modules: each has its own migration sequence and history
  table.
- The boundary in SQL is a convention, not enforced by PostgreSQL: one shared role can still
  query any schema; only code review and Spring Modulith's build-time check stop a cross-
  module query.
- Migrations run in the order of the module dependency graph (Spring Modulith); if the order
  between two modules' migrations ever matters, a dependency boundary has already leaked.
- A module's **business tables** live in its own schema, but its **history table**
  (`flyway_schema_history_<module>`) lives in `public`, alongside the root's and every other
  module's. Extracting a module later moves its schema, but its migration history is a
  separate, named table that must be moved (or re-baselined) on its own — it does not travel
  with the schema automatically.

## Revisit when

A module is extracted as a deployable, or a cross-schema access slips through review (then:
one role per module).
