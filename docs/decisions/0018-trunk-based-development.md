# 0018 — Trunk-based development, one artefact promoted across environments

- **Status**: Accepted
- **Date**: 2026-10-09 (date of the reference document; original decision date not recorded)
- **Related**: D-019

## Context

One developer, with Claude Code producing much of the code. Long-lived branches drift, and
rebuilding per environment means deploying something other than what was tested.

## Options considered

1. **Long-lived branches (e.g. develop/release)** — familiar; more divergence and merges.
2. **Trunk-based: one main branch, short branches per ticket, merge by pull request.**

## Decision

Trunk-based development: one main branch, short branches per ticket, merged through pull
requests. The same artefact is promoted from one environment to the next.

## Consequences

- Less divergence; what is deployed is what was tested.
- The main branch must always stay deployable.

## Revisit when

Candidate signal (not in the reference): several contributors work in parallel and main
breaks regularly.
