# 0025 — OpenAPI 3.1.1, snake_case, ISO 8601 UTC ; lecture d’un blocage

> Traduction française de `docs/decisions/0025-openapi-conventions.md`, qui fait foi.

- **Statut** : Acceptée
- **Date** : 2026-10-09 (date du document de référence ; date d’origine de la décision non consignée)
- **Liens** : D-021, D-028

## Contexte

Le contrat (0021) a besoin d’une version et de conventions. Un client qui recharge la page doit
pouvoir restaurer son compte à rebours, ce que le premier contrat ne permettait pas.

## Options envisagées

Non consignées au moment de la décision.

## Décision

OpenAPI 3.1.1, propriétés en snake_case, dates en ISO 8601 UTC. `GET /v1/holds/{holdId}` est
ajouté.

## Conséquences

- Une version récente au support outillé mature.
- Le client peut restaurer son compte à rebours.
- La configuration Jackson doit être explicite (snake_case, UTC).
- Le support de la 3.1 par le générateur doit être vérifié (ticket 5).

## À réviser quand

Le support de la 3.1 par le générateur se révèle insuffisant.
