# 0036 — Contract-first toolchain: generated interfaces per module and Redocly lint

- **Status**: Accepted
- **Date**: 2026-11-02
- **Related**: D-021, D-025, D-027, D-032

## Context

D-021 needs the contract and the generated code to stay in sync; D-025 asked for the
generator's OpenAPI 3.1 support to be verified against the real contract, not assumed. Two
modules expose endpoints (`event`, `booking`), each owning a different tag.

## Options considered

1. **One generated package for the whole contract** — shared models for every tag; breaks
   module boundaries (D-027: `booking` must never depend on `event`).
2. **Generation per tag, into the module that owns the resource** (chosen) — one
   `openapi-generator-maven-plugin` execution per tag, output under that module's own
   sub-package.
3. **Generated code committed** vs **regenerated on build** (chosen) — `target/` is already
   git-ignored; the cost is that generated changes never show in a PR diff.
4. **Lint with the generator's own `validateSpec`** (structural only, weak on 3.1) vs
   **Spectral** vs **Redocly CLI** (chosen) — configurable `rule/` assertions cover the five
   project-specific rules without writing a JavaScript plugin; Spectral would need one for at
   least the path-prefix and security-implies-401 checks.

## Decision

`openapi-generator-maven-plugin`, generator `spring`, two executions in
`backend/core/pom.xml` (tag `Events` → `event.api`, tag `Holds` → `booking.api`), bound before
compilation. `frontend-maven-plugin` downloads a pinned Node LTS and runs `@redocly/cli`
against `contracts/openapi.yaml` in the same build phase, before generation, so a broken
contract fails early. Exact versions, generator options and the reasoning behind each one are
commented in the poms, not duplicated here.

### OpenAPI 3.1 support, verified against the real contract and the real generated output

- `examples` arrays, `additionalProperties: false`, `format: uri-reference`: not reflected in
  generated code, each for a reason specific to this contract (no Swagger annotations
  requested; no code-level enforcement generated for either, by design — the project's own
  Jackson configuration is the actual enforcement).
- **`$ref` with a sibling `description` (`createHold`'s 404): wrong.** The generated Javadoc
  uses only the referenced component's own description, silently dropping the sibling
  override text — the one real 3.1 gap found.
- `default: 300`, the two success codes of `createHold`, and `minimum`/`maximum`/`minLength`/
  `maxLength`: all generated correctly.
- `Location` headers: never modeled in the interface signature in `interfaceOnly` mode,
  regardless of 3.0/3.1; the controller sets them (later ticket).

## Consequences

- An endpoint lives in the module that owns its resource; a tag maps to one module.
- Node enters the build, pinned and never committed.
- The IDE needs a build before the generated types exist.
- Generated changes never show in a PR diff; the generator and Redocly versions are not
  watched by Dependabot yet.
- Every business-problem handler must build its response through `ProblemDetailFactory`
  (ADR 0037), including the one schema the generator never produces a model for
  (`ProblemDetails`, schema-mapped to Spring's own `ProblemDetail`).

## Revisit when

- The sibling-`description`-on-`$ref` gap above carries real information clients need.
- A resource changes owner module.
- Node in the build becomes a problem (time, supply chain).
