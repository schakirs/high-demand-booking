# 0030 — Simulated identity with locally signed bearer tokens

- **Status**: Accepted
- **Date**: 2026-10-25
- **Related**: R3, D-021; amends 0007 (mechanism)

## Context

Authentication is not a learning goal (D-007), but R3 (one active hold or booking per user and
event) and the load tests need many distinct, trustworthy user identities, plus an admin role
for event creation. The contract already declares a bearer scheme and has no token endpoint.

## Options considered

1. **User id in a request header** — trivial; changes the contract and lets any client pick its
   identity, which weakens R3 tests.
2. **Token issuance endpoint in the application** — convenient for k6; adds an endpoint that
   could leak into production.
3. **Offline script issuing signed tokens, validated by Spring Security** — no contract change,
   no issuance code in the application.

## Decision

Option 3. The contract keeps the bearer scheme. A script generates signed tokens for N test
users and one administrator; Spring Security validates them with a local key. Token issuance
never exists in the production profile.

## Consequences

- R3 and k6 work with real, distinct identities taken from the token.
- No contract change; the security layer is the one a real provider would plug into.
- Key management is local and simplistic; this component must be replaced before any real
  exposure (D-007).
- Tokens must be regenerated when they expire or the key changes.

## Revisit when

The application is exposed beyond local and CI environments, or a real identity provider is
introduced.
