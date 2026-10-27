# 0022 — Stack technique

> Traduction française de `docs/decisions/0022-technology-stack.md`, qui fait foi.

- **Statut** : Acceptée
- **Date** : 2026-10-09 (date du document de référence ; date d’origine de la décision non consignée)
- **Liens** : D-016, D-020, D-026, D-028

## Contexte

La stack doit rester supportée pendant toute la durée du projet, et familière là où la nouveauté
n’apporte rien à apprendre (12 ans de Java et de Spring Boot).

## Options envisagées

Non consignées au moment de la décision.

## Décision

Java 25 (LTS), Spring Boot 4.1.x et la version compatible de Spring Modulith, Maven, Flyway,
PostgreSQL 18, avec la même version majeure en local, en CI et dans le cloud.

## Conséquences

- Versions supportées ; outils familiers.
- Les migrations Flyway vont uniquement en avant.
- Avec les threads virtuels, le pool de connexions devient la limite et doit être protégé
  explicitement.

## À réviser quand

Signal candidat (absent de la référence) : une version arrive en fin de support, ou le pool de
connexions limite le débit lors des tests de charge.
