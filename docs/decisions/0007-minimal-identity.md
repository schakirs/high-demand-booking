# 0007 — Minimal identity: test users and signed tokens

- **Status**: Accepted, amended by 0030 (mechanism)
- **Date**: 2026-10-09 (date of the reference document; original decision date not recorded)
- **Related**: R3, D-001, D-030

## Context

Authentication is not a learning objective (D-001), and a real login flow would get in the
way of load tests that need thousands of users. The system still needs distinct identities
(R3: one active hold or booking per user and event).

## Options considered

1. **Real authentication** — production-like; costly, off-goal, and hinders load tests.
2. **Test users with signed tokens.**

## Decision

Identity is minimal: test users and signed tokens. The mechanism is specified in 0030.

## Consequences

- Load tests can create many identities cheaply.
- This component must be replaced before any real exposure.

## Revisit when

The application is exposed beyond local and CI environments.
