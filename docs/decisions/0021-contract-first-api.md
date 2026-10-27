# 0021 — Contract-first API, Problem Details errors, version in the URL

- **Status**: Accepted
- **Date**: 2026-10-09 (date of the reference document; original decision date not recorded)
- **Related**: D-025, D-029

## Context

The API is designed before it is coded, and Claude Code implements it. A validated contract
gives the implementation a fixed target and lets reviews focus on behaviour.

## Options considered

1. **Code-first, contract generated from the code** — faster to start; the API is shaped by
   the implementation.
2. **Contract-first** — the OpenAPI contract is written and validated before implementation.

## Decision

The API is contract-first: the OpenAPI contract is written and validated before
implementation. Errors use Problem Details (RFC 9457). The version is in the URL (`/v1`).

## Consequences

- Claude Code implements a validated contract.
- Contract and code must be kept in sync (generated interfaces, ticket 5).
- Every contract change is an explicit change (e.g. 0029).

## Revisit when

Candidate signal (not in the reference): contract changes become so frequent that the
validation step slows every ticket.
