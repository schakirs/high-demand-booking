# 0019 — Kanban sur GitHub Issues et Projects, limite de 2 tickets en cours

> Traduction française de `docs/decisions/0019-kanban-on-github.md`, qui fait foi.

- **Statut** : Acceptée
- **Date** : 2026-10-09 (date du document de référence ; date d’origine de la décision non consignée)
- **Liens** : D-018

## Contexte

Travail en solo, sans sprints. Chaque changement doit être traçable du ticket au commit puis à
la pull request.

## Options envisagées

1. **Sprints (Scrum)** — rythme et cérémonies de planification ; peu de valeur pour une seule
   personne.
2. **Kanban sur GitHub Issues + Projects avec une limite de travail en cours.**

## Décision

Kanban sur GitHub Issues et Projects, avec une limite de 2 tickets en cours. Le numéro de ticket
figure dans chaque branche et chaque commit.

## Conséquences

- Les tickets sont liés aux commits et aux pull requests.
- Le tableau doit être tenu à jour.

## À réviser quand

Signal candidat (absent de la référence) : des tickets attendent régulièrement en cours au-delà
de la limite, ou le tableau ne reflète plus la réalité.
