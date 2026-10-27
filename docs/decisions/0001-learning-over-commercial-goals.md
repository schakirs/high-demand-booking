# 0001 — Learning comes before any commercial goal

- **Status**: Accepted
- **Date**: 2026-10-09 (date of the reference document; original decision date not recorded)
- **Related**: all decisions

## Context

The project is a learning vehicle for software architecture, built around a booking system
under massive simultaneous demand. Many choices could be settled by "what a real business
would need now"; that criterion would often remove the very problems worth studying.
This is the project's goal rather than a choice between alternatives, so no option was
considered.

## Decision

Learning takes precedence over any commercial objective. Every added piece of complexity must
be tied to a learning objective.

## Consequences

- The system is deliberately more complex than the real need (e.g. a modular monolith with
  three deployables, an outbox, an inventory designed in stages).
- Each ADR must name the learning objective behind any complexity it adds.
- Some decisions would be different for a production product; they must not be copied as-is.

## Revisit when

Candidate signal (not in the reference): the project is meant to serve real users or real
sales.
