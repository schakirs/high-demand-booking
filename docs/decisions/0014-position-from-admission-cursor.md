# 0014 — Position = rank − admission cursor, served by cache and CDN

- **Status**: Accepted
- **Date**: 2026-10-09 (date of the reference document; original decision date not recorded)
- **Related**: R2b, R13, D-006, D-009

## Context

Position reads reach ~60,000 req/s with R13. After the lottery, a user's rank never changes;
only the admission cursor of the event moves.

## Options considered

1. **Compute each user's position on every read** — exact; one query per read at peak.
2. **Position = fixed rank − one admission cursor per event**, the cursor served by cache and
   CDN.

## Decision

The position is the user's rank minus the event's admission cursor. There is one cursor per
event, served by cache and CDN.

## Consequences

- Most reads are absorbed by the cache and the CDN.
- The position is pessimistic: users who left the queue are not deducted.
- The cursor has a small propagation delay.

## Revisit when

Abandonment detection is designed (open question) and the pessimistic position becomes
misleading.
