# 0037 — Error responses: shared technical handler, business problems per module

- **Status**: Accepted
- **Date**: 2026-11-02
- **Related**: D-002, D-021, D-025, D-027

## Context

Errors use RFC 9457 Problem Details (D-021). Validation errors, malformed requests, and the
500 fallback are identical across every module; business problems (`event-not-found`,
`event-sold-out`, …) are specific to the module that raises them. No controller exists yet
(this ticket generates interfaces only): the handler needs to be ready for the modules that
will use it (#7, #9, #10, #12), and for the security filter chain (#6), without any of them
existing yet.

## Options considered

1. **One handler per API module** — duplicates the technical-error logic; drifts.
2. **One global handler that knows every module's business exceptions** — couples the shared
   module to all of them, defeating the point of a module boundary.
3. **Shared technical handler + per-module business handlers** (chosen) — a minimal `web`
   module owns validation errors, the 500 fallback, and the problem factory; each module adds
   its own `@RestControllerAdvice` for its business problems, ordered ahead of the shared one.
4. **Unknown request properties**: tolerant reader (ignore) vs **rejected** (chosen, D-002) —
   a typo such as `hold_duration_secs` must not silently fall back to a default.

## Decision

`com.schakir.hdb.core.web` depends on no other module
(`@ApplicationModule(allowedDependencies = {})`); `event` and `booking` may depend on it;
`inventory` never does. Rather than add a dedicated test for that last rule, `inventory` gets
the same `allowedDependencies = {}` annotation (D-027 enforced by declaration): the existing
`ModularityTests.verifiesModuleBoundaries` test now rejects *any* dependency `inventory`
might grow, not only one on `web`.

`TechnicalProblemHandler` (`@RestControllerAdvice`, `@Order(Ordered.LOWEST_PRECEDENCE)`, so a
module's own business handler is tried first) returns `application/problem+json` for: Bean
Validation failures, malformed JSON, a missing or wrong-typed body, an unknown JSON property,
and a non-UUID path parameter, all as `400 /problems/validation-error` with `detail` naming
the field and the rule, never the raw input or a Java class/exception name; any other
unhandled exception as `500 about:blank`, logged at ERROR with its stack trace, nothing
technical in the body; other undeclared Spring MVC errors (unknown route, 405, 406, 415) keep
their status with `about:blank`.

`ProblemDetailFactory` builds the `ProblemDetail` (validates the kebab-case name, sets
`instance` from the MDC correlation id — key defined in `web`, generation and propagation is
#13, not this ticket — when present) and is usable from the security filter chain for 401/403
(#6, out of scope here).

### A real pitfall this ticket's testing surfaced, binding on every module's business handler

Spring MVC's `HttpEntityMethodProcessor` (and `RequestResponseBodyMethodProcessor`) special-
case any `ResponseEntity`/`HttpEntity` body that is a `ProblemDetail`: if its `instance` is
null, they set it to the current request's URI before writing the response — unconditionally,
independent of Jackson, independent of any `@RestControllerAdvice` logic, and independent of
whether the body came from `ResponseEntityExceptionHandler`'s own machinery or a handler's own
directly-returned `ResponseEntity`. This directly conflicts with "`instance` holds the
correlation id when present, and is omitted otherwise": the correlation id does not exist yet
(#13), so most responses today would get the request path as a spurious `instance` instead of
nothing.

The fix is `ProblemDetailFactory.asResponseBody(ProblemDetail)`: it converts the
`ProblemDetail` into a plain, ordered `Map<String, Object>` with absent fields dropped (never
sent as JSON `null`, which would also violate the contract's `type: string` schema for a field
like `instance`), and that map — not the `ProblemDetail` itself — becomes the response body.
Because a `Map` does not match `instanceof ProblemDetail`, Spring's auto-fill never triggers.
The same conversion also has to restore the `application/problem+json` content type explicitly
(`ResponseEntity.contentType(...)`): that, too, was tied to the same `instanceof ProblemDetail`
check and is lost once the body stops being one. Every future business-problem handler
(#7, #9, #10, #12) must go through this same method, not hand `ProblemDetail` to
`ResponseEntity.body(...)` directly.

## Consequences

- One more module every API module depends on, kept deliberately tiny.
- Handler precedence is explicit (`@Order`); a module's business handler always wins over the
  technical fallback.
- Security errors (#6) must reuse `ProblemDetailFactory`, including `asResponseBody`.
- Undeclared Spring MVC errors use `about:blank`.
- Every module's future business-problem handler needs the same `asResponseBody` conversion;
  returning `ProblemDetail` directly from any `@RestControllerAdvice` method in this codebase
  is a bug, not a style choice.

## Revisit when

- `web` starts collecting business logic (it must not: that is the signal to split it back
  out into the owning module).
- A client needs to tolerate unknown fields for contract evolution (today: rejected, D-002).
- Spring Framework changes the `HttpEntityMethodProcessor` behavior above (then the
  `asResponseBody` workaround can likely be dropped).
