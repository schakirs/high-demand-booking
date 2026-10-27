# 0005 — The waiting room is always active in the MVP

- **Status**: Accepted
- **Date**: 2026-10-09 (date of the reference document; original decision date not recorded)
- **Related**: R1, R2, R2b, R9, D-006

## Context

The waiting room and the queue protect the system under load, but they also guarantee the
order of access (R2). Most events will never face a peak.

## Options considered

1. **Queue only for high-demand events** — no extra step for quiet events; two paths to build
   and test, and a decision to make per event.
2. **Queue always active** — one path for every event.

## Decision

The waiting room and queue are always active in the MVP.

## Consequences

- A single path, which guarantees ordering and not only load handling.
- For low-demand events the step is instant but useless.
- Disabling the queue is listed after the MVP.

## Revisit when

Candidate signal (not in the reference): the queue step measurably degrades the experience or
the cost of low-demand events.
