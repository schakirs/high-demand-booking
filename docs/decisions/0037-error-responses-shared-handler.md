# 0037 — Error responses: shared technical handler, business problems per module

- **Status**: Accepted
- **Date**: 2026-11-02
- **Related**: D-002, D-021, D-025, D-027

## Context

Errors use RFC 9457 Problem Details (D-021). Validation errors, malformed requests, and the
500 fallback are identical across every module; business problems (`event-not-found`,
`event-sold-out`, …) are specific to the module that raises them.

## Options considered

1. **One handler per API module** — duplicates the technical-error logic; drifts.
2. **One global handler that knows every module's business exceptions** — couples the shared
   module to all of them, defeating the point of a module boundary.
3. **Shared technical handler + per-module business handlers** (chosen) — a minimal `web`
   module owns validation errors, the 500 fallback, and the problem factory; each module adds
   its own `@RestControllerAdvice` for its business problems, ordered ahead of the shared one.
4. **Unknown request properties**: tolerant reader (ignore) vs **rejected** (chosen, D-002) —
   a typo such as `hold_duration_secs` must not silently fall back to a default. The same
   reasoning extends to an explicit JSON `null` on an optional field typed as non-nullable: it
   is rejected, not applied over the field's default.

## Decision

`com.schakir.hdb.core.web` depends on no other module
(`@ApplicationModule(allowedDependencies = {})`); `event` and `booking` may depend on it;
`inventory` gets the same empty `allowedDependencies` (D-027 enforced by declaration, not by a
dedicated test): the existing `ModularityTests.verifiesModuleBoundaries` test then rejects any
dependency `inventory` might grow, not only one on `web`.

Every problem response — business or technical — is built with `ProblemDetailFactory` and
never returned as a raw `ProblemDetail` body: Spring MVC fills a null `instance` with the
request URI otherwise, which must never happen while no correlation id exists (#13).
`TechnicalProblemHandler` (`@RestControllerAdvice`, lowest precedence) overrides
`ResponseEntityExceptionHandler.createResponseEntity`, the single point every one of its
default exception handlers funnels through: this is what lets an exception this project never
explicitly classifies (a missing request parameter, a raw `ResponseStatusException`, …) still
come out as a valid problem response, with `type` defaulted from the status
(`/problems/validation-error` for 400, `about:blank` otherwise) instead of silently leaking the
request path.

`detail` names the field and the rule, never the raw input or a Java class/exception name.
Jackson's global `default-property-inclusion: non_null` (CLAUDE.md) keeps every optional,
absent field — `instance` included — out of the body entirely, rather than present as `null`.

## Consequences

- One more module every API module depends on, kept deliberately tiny.
- Handler precedence is explicit (`@Order`); a module's business handler always wins over the
  technical fallback.
- Security errors (#6) must reuse `ProblemDetailFactory`.
- Every future business-problem handler must go through the same factory; returning a
  `ProblemDetail` directly from any `@RestControllerAdvice` method in this codebase is a bug,
  not a style choice.

## Revisit when

- `web` starts collecting business logic (it must not: that is the signal to split it back
  out into the owning module).
- A client needs to tolerate unknown fields for contract evolution (today: rejected, D-002).
- Spring Framework changes `HttpEntityMethodProcessor`'s instance-from-URI behavior (then the
  `createResponseEntity` override may no longer be needed).
