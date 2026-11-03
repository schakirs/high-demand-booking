# 0038 — Simulated identity mechanism: ES256, a hand-built decoder, fail-closed URL rules

- **Status**: Accepted
- **Date**: 2026-11-03
- **Related**: R3, D-007, D-030 (amends: fills in the mechanism), D-027 (amends: new allowed
  dependency `user → web`), D-037

## Context

D-030 decided that identity is simulated: an offline script signs tokens for test users and an
administrator, and Spring Security validates them with a local key. It left the mechanism open.
The `user` module (#6) needs that mechanism, plus the authorization rules for every contract
operation, and a runnable token generator.

## Options considered

1. **Boot's own `spring.security.oauth2.resourceserver.jwt.public-key-location` auto-configuration**
   — zero code; rejected: its `NimbusJwtDecoder.withPublicKey(RSAPublicKey)` only accepts an
   RSA key, and tokens here are signed with ES256 (an EC key).
2. **RS256 instead, to use that auto-configured property** — rejected: it trades a deliberate
   algorithm for one chosen only to fit the tool, and still needs custom claim validators
   (`iss`, `aud`, UUID `sub`) regardless.
3. **A hand-built `JwtDecoder`, ES256** (chosen) — chosen as asymmetric, specifically: the
   application holds the public key only, so it can verify a token but never mint one, the same
   trust model a real identity provider will expose via JWKS later (D-007). A symmetric secret
   (HS256) would have to live in the app, letting it also forge tokens — defeating the point of
   identity being *simulated*, not owned. Construction details (JWK source, key selector, claim
   validator chain) live as code comments in `JwtDecoderConfig`, not here.
4. **`@PreAuthorize` per controller method** — rejected (D-037): an `AccessDeniedException`
   raised inside MVC falls through to `TechnicalProblemHandler`'s generic handler, becoming a
   `500 about:blank` instead of a `403`.
5. **Fail-closed URL rules in a `SecurityFilterChain`** (chosen) — one explicit rule per contract
   operation (method + exact path), `anyRequest().denyAll()` for everything else.

## Decision

Option 3 + option 5.

**Token validation.** The `user` module builds its own `JwtDecoder` from the EC public key PEM
at `hdb.security.jwt.public-key-location` (no default: the application does not start without
it). Validated: `exp` required, `nbf`/`exp` with a 60 s clock skew
(`hdb.security.jwt.clock-skew`, no default either), `iss == hdb-token-script`, `aud` contains
`hdb-core`, `sub` present and a parseable UUID. `scope` needs no custom converter: Spring's
default converter already maps it to `SCOPE_*` authorities. Token time is checked against the
JVM clock, not `now()` from PostgreSQL: D-028 is about business time, and a token's validity
window is not business data.

**Authorization.** `SecurityConfig`'s `SecurityFilterChain` lists every contract operation by
method and exact path (`POST /v1/events` → `SCOPE_admin`; the other four → authenticated), the
two actuator `GET`s → public, `anyRequest().denyAll()` otherwise. Stateless sessions, CSRF
disabled, no form login, no HTTP Basic, no default user. The `WWW-Authenticate` header (RFC
6750 §3) is set directly by `JwtAuthenticationEntryPoint`/`JwtAccessDeniedHandler`, not by
delegating to Spring Security's `BearerToken*` classes, which also append `error_description`
and `resource_metadata` — a leak of the rejection reason the response body is deliberately
generic about: exactly `Bearer` with no token, `Bearer error="invalid_token"` with a rejected
one, `Bearer error="insufficient_scope"` on 403. Spring Security 7's RFC 9728
protected-resource-metadata filter — added unconditionally alongside the JWT resource server,
and reachable before authorization ever runs — is removed from the built filter chain: it is
not in the contract (CLAUDE.md rule 8).

**Caller id and module boundary.** A controller implementing a generated API interface cannot
take an `@AuthenticationPrincipal Jwt` parameter: the signature is fixed by the generator.
`web`'s `Caller.id()` reads it off the security context instead, failing hard if called without
authentication. `user` depends on `web` (`@ApplicationModule(allowedDependencies = "web")`,
amending D-027's graph); nothing depends on `user`; every class besides the module marker is
package-private.

**Token generator.** `TestTokens` (`backend/core/src/test/.../user/testsupport`, never
`src/main`) generates ES256 key pairs and signs tokens. Run from the command line, it persists
the key pair under `.local/identity/` (`private.pem` restricted to the owner) and writes
`load-tests/tokens.json`. User ids are deterministic (`UUID.nameUUIDFromBytes`) so a k6 run can
be replayed.

## Consequences

- No contract change beyond the `WWW-Authenticate` header declaration (1.4.0).
- Authorization rules are contract-driven only by discipline, not by generation: a new
  operation needs its own rule added by hand; a test parses `contracts/openapi.yaml` and fails
  if one is missing (an admin token must get neither 401 nor 403 on every operation).
- Tokens expire; regenerate before each run (`tokens.ttl`, default `PT1H`).
- Only `GET` is exposed on the two actuator paths; no RFC 9728 metadata endpoint.
- Every full-context test needs a public key to start, via a shared `SecurityTestSupport` base.
- The decoder is hand-built: switching algorithms is a new ADR, not a configuration change.

## Revisit when

- A real identity provider replaces the token script (D-007, D-030): the custom property and
  decoder are replaced by the provider's own resource-server configuration.
- A second admin-only or role-gated operation needs a role other than `admin`.
- Authorization rules need to be generated from the contract rather than hand-maintained.
