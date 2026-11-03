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
   — zero code; rejected: it builds its `JwtDecoder` through
   `NimbusJwtDecoder.withPublicKey(RSAPublicKey)`, which only accepts an RSA key. Tokens here are
   signed with ES256 (an EC, P-256 key), so this property cannot be used at all, not even as a
   starting point.
2. **RS256 instead, to use the auto-configured property** — keeps the zero-code path; rejected: it
   trades a deliberate, already-decided algorithm for one chosen only to fit the tool, and still
   needs custom claim validators (`iss`, `aud`, `sub`-is-UUID, required `exp`) regardless.
3. **A hand-built `JwtDecoder`** (chosen) — a Nimbus `JWKSource` holding the one EC public key, a
   `JWSVerificationKeySelector` pinned to ES256, and a `DelegatingOAuth2TokenValidator` for the
   claims. A custom property (`hdb.security.jwt.public-key-location`) replaces Boot's, since
   Boot's only exists for the RSA path.
4. **`@PreAuthorize` per controller method** — rejected (D-037): an `AccessDeniedException` raised
   inside MVC is not caught by any `@RestControllerAdvice` for business problems and falls through
   to `TechnicalProblemHandler`'s `Exception.class` handler, becoming a `500 about:blank` instead
   of a `403`.
5. **Fail-closed URL rules in a `SecurityFilterChain`** (chosen) — one explicit rule per contract
   operation (method + exact path), `anyRequest().denyAll()` for everything else. Authorization
   runs before Spring MVC reads the request body, so it is also the mechanism behind D-037's
   "security errors must reuse `ProblemDetailFactory`" note: a 401/403 is decided before a 400
   ever could be.

## Decision

Option 3 + option 5.

**Token validation.** The `user` module builds its own `JwtDecoder`: an `ECKey` built from the PEM
at `hdb.security.jwt.public-key-location` (no default — the application does not start without
it), wrapped in an `ImmutableJWKSet`, with a `JWSVerificationKeySelector` pinned to
`JWSAlgorithm.ES256` (this is what rejects every other `alg`, including `none`, by construction,
not by an explicit check). A `DelegatingOAuth2TokenValidator` adds: `exp` required (Spring's own
`JwtTimestampValidator` checks it only if present), a 60 s clock skew
(`hdb.security.jwt.clock-skew`, set explicitly in `application.yml`, not left at a default read
from code), `iss == hdb-token-script`, `aud` contains `hdb-core`, and `sub` present and a
parseable UUID. The `scope` claim needs no custom converter: Spring's default
`JwtGrantedAuthoritiesConverter` already maps it to `SCOPE_*` authorities.

Token time (`exp`, `nbf`) is checked against the JVM clock, not `now()` from PostgreSQL: D-028 is
about business time (sale windows, hold expiry), and a token's validity window is not business
data, so D-028 does not apply here.

**Authorization.** `SecurityConfig`'s `SecurityFilterChain` lists every contract operation by
HTTP method and exact path (`POST /v1/events` → `SCOPE_admin`; the other four → authenticated),
`GET /actuator/health` and `GET /actuator/prometheus` (those two exact paths only) → public, and
`anyRequest().denyAll()`. Stateless sessions, CSRF disabled, no form login, no HTTP Basic, no
default user. `BearerTokenAuthenticationEntryPoint`/`BearerTokenAccessDeniedHandler` compute the
`WWW-Authenticate` header (RFC 6750 §3); the `user` module wraps both to also write the
`/problems/unauthorized` / `/problems/forbidden` body through `ProblemDetailFactory` (a new,
minimal `writeTo` overload, since this runs outside MVC dispatch), reusing the same
type/`instance`-from-correlation-id logic `toResponseEntity` already uses, never
`response.reset()` (that would erase the header the delegate just set).

**Module boundary.** `user` depends on `web` (`@ApplicationModule(allowedDependencies = "web")`,
amending D-027's graph); nothing depends on `user`. Every class inside it besides the module
marker is package-private: nothing outside the package needs to be public for Spring to find it.

**Token generator.** `TestTokens` (`backend/core/src/test/java/.../user/testsupport`, never
`src/main`) generates ES256 key pairs and signs tokens. Called directly from tests with an
ephemeral, in-memory key pair; run from the command line
(`./mvnw -pl core test-compile exec:java@tokens -Dtokens.users=... -Dtokens.ttl=...`, bound to no
build phase) it persists the key pair under `.local/identity/` (reused across runs unless
rotated) and writes `load-tests/tokens.json`. User ids are deterministic
(`UUID.nameUUIDFromBytes("user-" + i)`) so a k6 run can be replayed.

## Consequences

- No contract change beyond the `WWW-Authenticate` header declaration (1.4.0); the bearer scheme
  itself was already there (D-030).
- The decoder is hand-built, so an upgrade to Boot's own property-based configuration is not
  possible while ES256 is the chosen algorithm; switching algorithms is a new ADR, not a config
  change.
- Authorization rules are contract-driven only by discipline, not by generation: a new contract
  operation needs its own rule added by hand (a test parses `contracts/openapi.yaml` to catch a
  forgotten one, but it only proves authentication is required, not which role).
- `/test-only/**` (src/test scaffolding, #5) is exempted from the filter chain so the shared
  technical-error-handling tests keep running without a token; it is never reachable in
  production.
- Every full-context test now needs a public key to start: tests that boot `CoreApplication`
  provide one through a shared test base (`SecurityTestSupport`), an ephemeral key registered via
  `@DynamicPropertySource`.

## Revisit when

- A real identity provider replaces the token script (D-007, D-030): the custom property and
  decoder are replaced by the provider's own resource-server configuration.
- A second admin-only or role-gated operation needs a role other than `admin`.
- Authorization rules need to be generated from the contract rather than hand-maintained.
