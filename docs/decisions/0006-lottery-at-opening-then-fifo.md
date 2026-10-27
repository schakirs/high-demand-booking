# 0006 — Lottery at sale opening, then FIFO

- **Status**: Accepted
- **Date**: 2026-10-09 (date of the reference document; original decision date not recorded)
- **Related**: R1, R2, D-005, D-012

## Context

The waiting room opens 30 minutes before the sale (R1). With a pure arrival order, ranks would
depend on milliseconds and network latency, and everyone would connect at the last second,
concentrating the peak.

## Options considered

1. **Pure FIFO by arrival time** — simple; rewards network luck and concentrates connections.
2. **Lottery among those present at opening, then FIFO for later arrivals.**

## Decision

At sale opening, users present in the waiting room receive a random rank; later arrivals are
queued in FIFO order behind them.

## Consequences

- Order no longer depends on milliseconds; connections spread over the 30 minutes.
- Arriving very early gives no advantage.
- The lottery must be atomic at opening, over a potentially very large set (open question).

## Revisit when

Candidate signal (not in the reference): the lottery cannot complete atomically within the
opening window for the reference scenario.
