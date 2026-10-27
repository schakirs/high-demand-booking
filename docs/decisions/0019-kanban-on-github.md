# 0019 — Kanban on GitHub Issues and Projects, WIP limit 2

- **Status**: Accepted
- **Date**: 2026-10-09 (date of the reference document; original decision date not recorded)
- **Related**: D-018

## Context

Solo work, without sprints. Every change should be traceable from ticket to commit to pull
request.

## Options considered

1. **Sprints (Scrum)** — rhythm and planning ceremonies; little value for a single person.
2. **Kanban on GitHub Issues + Projects with a WIP limit.**

## Decision

Kanban on GitHub Issues and Projects, with a limit of 2 tickets in progress. The ticket number
appears in every branch and commit.

## Consequences

- Tickets are linked to commits and pull requests.
- The board must be kept up to date.

## Revisit when

Candidate signal (not in the reference): tickets regularly wait in progress beyond the WIP
limit, or the board no longer reflects reality.
