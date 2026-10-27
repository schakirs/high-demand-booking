# 0005 — La salle d’attente est toujours active dans le MVP

> Traduction française de `docs/decisions/0005-waiting-room-always-on.md`, qui fait foi.

- **Statut** : Acceptée
- **Date** : 2026-10-09 (date du document de référence ; date d’origine de la décision non consignée)
- **Liens** : R1, R2, R2b, R9, D-006

## Contexte

La salle d’attente et la file protègent le système sous charge, mais elles garantissent aussi
l’ordre d’accès (R2). La plupart des événements ne connaîtront jamais de pic.

## Options envisagées

1. **File seulement pour les événements très demandés** — pas d’étape supplémentaire pour les
   événements calmes ; deux chemins à construire et à tester, et une décision à prendre par
   événement.
2. **File toujours active** — un seul chemin pour tous les événements.

## Décision

La salle d’attente et la file sont toujours actives dans le MVP.

## Conséquences

- Un seul chemin, qui garantit l’ordre et pas seulement la tenue en charge.
- Pour les événements peu demandés, l’étape est instantanée mais inutile.
- La désactivation de la file est prévue après le MVP.

## À réviser quand

Signal candidat (absent de la référence) : l’étape de file dégrade de façon mesurable
l’expérience ou le coût des événements peu demandés.
