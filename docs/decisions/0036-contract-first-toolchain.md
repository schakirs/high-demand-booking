# 0036 — Contract-first toolchain: generated interfaces per module and Redocly lint

- **Status**: Accepted
- **Date**: 2026-11-02
- **Related**: D-021, D-025, D-027, D-032

## Context

D-021 needs the contract and the generated code to stay in sync; D-025 asked for the
generator's OpenAPI 3.1 support to be verified, not assumed, once a real generator ran
against the real contract. Two modules expose endpoints (`event`, `booking`), each owning a
different tag.

## Options considered

1. **One generated package for the whole contract** — a single execution, shared models for
   every tag; breaks module boundaries (every module would depend on the shared package, or
   the package would need to be an open module) and does not fit D-027 (`booking` never
   depends on `event`).
2. **Generation per tag, into the module that owns the resource** (chosen) — one
   `openapi-generator-maven-plugin` execution per tag, filtered by `apisToGenerate` /
   `modelsToGenerate`, output under that module's own sub-package (`<module>.api`,
   `<module>.api.model`).
3. **Lint by the generator's own `validateSpec`** — structural only, weak on 3.1-specific
   constructs. vs **Spectral** vs **Redocly CLI** (chosen) — configurable `rule/` assertions
   cover the five project-specific rules (snake_case properties, problem+json on every 4xx,
   problem `type` pattern in examples, `/v1/` path prefix, 401 on every operation) without a
   JavaScript plugin.
4. **Generated code committed** vs **regenerated on build** (chosen) — `target/` is already
   git-ignored; a contract change regenerates automatically, at the cost of generated changes
   never showing in a PR diff.

## Decision

`openapi-generator-maven-plugin` 7.27.0, generator `spring`, two executions in
`backend/core/pom.xml` (tag `Events` → `event.api`, tag `Holds` → `booking.api`), bound to
`generate-sources`, before any compilation. `frontend-maven-plugin` 2.0.2 in the `backend`
aggregator pom downloads a pinned Node LTS (v26.13.0) into `contracts/node` and runs
`@redocly/cli` 2.72.1 (pinned, `package-lock.json` committed) against `contracts/openapi.yaml`
in the same `generate-sources` phase, before generation, so a broken contract fails the build
early.

### OpenAPI 3.1 support, verified against the real contract and the real generated output

- `examples` arrays (3.1 plural form): not reflected in generated code — expected, since
  `annotationLibrary=none` strips all Swagger-derived metadata including examples; this is a
  consequence of that choice, not a 3.1 gap.
- `$ref` with a sibling `description` (`createHold`'s 404): **wrong**. The generated Javadoc
  uses only the referenced component's own description ("Event not found."), silently
  dropping the override text the contract declares next to the `$ref`. Confirmed by direct
  comparison between the contract and the generated `HoldsApi.java`.
- `additionalProperties: false`: ignored — no code-level enforcement is generated. Expected:
  the project's own global Jackson configuration (`fail-on-unknown-properties: true`, ticket
  #5) is the actual enforcement, proven by a test.
- `format: uri-reference`: not exercised by the generated output in this contract — the only
  fields using it (`ProblemDetails.type` / `.instance`) are schema-mapped to Spring's own
  `ProblemDetail` (see below) rather than generated as a model.
- `default: 300` on `hold_duration_seconds`: generated correctly
  (`private Integer holdDurationSeconds = 300;`).
- The two success codes of `createHold` (`201`/`200`): generated correctly — a single
  `ResponseEntity<Hold>` return type, both documented in the Javadoc.
- `Location` headers: not modeled in the interface signature in `interfaceOnly` mode (true
  regardless of 3.0/3.1); adding the header to the response is the controller implementation's
  responsibility (later ticket).
- `minimum` / `maximum` / `minLength` / `maxLength`: generated correctly as Bean Validation
  annotations (`@Min`, `@Max`, `@Size`).

### Generator options

`interfaceOnly`, `skipDefaultInterface` (interface only, no default method bodies);
`useSpringBoot4` (implies `useJakartaEe`), `useJackson3` (only allowed with `useSpringBoot4`);
`useBeanValidation`; `openApiNullable=false`; `annotationLibrary=none`;
`documentationProvider=none`; `dateLibrary=java8`; `generateApiTests`, `generateModelTests`,
`generateApiDocumentation`, `generateModelDocumentation`, `generateSupportingFiles` all
`false` (minimal footprint — none of this was asked for, and `documentationProvider=none`
already removes the one thing most of it would have documented).

**`useTags=true` is required**, not optional: without it, the `spring` generator groups
operations by the first path segment of the operation's path, not by the OpenAPI `tag`.
Every operation in this contract starts with `/v1/...`, so without `useTags`, all five
operations (`Events` and `Holds` alike) would land in a single generated class, defeating the
per-module split entirely. Verified against the generator's `SpringCodegen` source for 7.27.0.

**`date-time` → `java.time.Instant`**: no `dateLibrary` value maps directly to `Instant`
(`java8` maps to `OffsetDateTime`); the documented mechanism is a `typeMapping`
(`DateTime=Instant`) plus a matching `importMapping` (`Instant=java.time.Instant`) at the
plugin level, confirmed against the real output.

**The `ProblemDetails` schema is schema-mapped, not excluded.** Point 1 of this ticket's plan
said not to generate a `ProblemDetails` model (errors use Spring's own `ProblemDetail`).
Simply leaving `ProblemDetails` out of `modelsToGenerate` is not enough: the generated API
interfaces still `import ...model.ProblemDetails` for the `4xx` responses that reference it,
and that import does not resolve, which fails the build. The fix is a `schemaMapping`
(`ProblemDetails=org.springframework.http.ProblemDetail`): every reference to the schema is
redirected to the existing Spring type instead, which also happens to be exactly the type
`ProblemDetailFactory` builds (ADR 0037).

**`frontend-maven-plugin`'s executions need `inherited=false`.** Declared in the aggregator
pom's `<plugins>` (not `<pluginManagement>`), they are otherwise inherited by every child
module (`core`, and later deployables) and re-run there, with `${project.basedir}` resolving
against the child's own directory instead of `backend/`, breaking the `workingDirectory`
path. `inherited=false` confines the executions to the aggregator itself.

## Consequences

- An endpoint lives in the module that owns its resource; a tag maps to one module.
- Node enters the Maven build (pinned, downloaded fresh, never installed globally).
- The IDE needs a build before the generated types exist.
- Generated changes never show in a PR diff.
- The generator and Redocly versions are not watched by Dependabot yet.
- `ResponseEntity<ProblemDetail>` cannot be returned directly from any handler without
  triggering Spring's own request-URI auto-fill of `instance` (ADR 0037) — every module's
  business-problem handler must go through `ProblemDetailFactory.asResponseBody`, not just the
  shared technical handler.

## Revisit when

- A contract feature cannot be generated correctly (the sibling-`description`-on-`$ref` gap
  above is a candidate if it ever carries real information clients need).
- A resource changes owner module.
- Node in the build becomes a problem (time, supply chain).
