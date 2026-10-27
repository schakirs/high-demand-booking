# 0013 — Publier les événements par une transactional outbox

> Traduction française de `docs/decisions/0013-transactional-outbox.md`, qui fait foi.

- **Statut** : Acceptée, amendée par 0020 (l’outbox est le registre Spring Modulith)
- **Date** : 2026-10-09 (date du document de référence ; date d’origine de la décision non consignée)
- **Liens** : D-010, D-011, D-016, D-020, D-024

## Contexte

Les changements d’état (blocage créé, expiré, confirmé) doivent être publiés vers les autres
modules. Écrire dans la base puis dans un broker est une double écriture : un crash entre les
deux perd ou invente un événement.

## Options envisagées

1. **Double écriture : base puis broker** — simple ; perd des événements en cas de panne.
2. **Transactional outbox** — l’événement est écrit dans la même transaction que l’état, puis
   relayé.

## Décision

Les événements sont publiés par une transactional outbox.

## Conséquences

- Pas de double écriture entre la base et un broker.
- Livraison au moins une fois : les consommateurs doivent être idempotents.
- Un relais doit être exploité ; avec 0020, le registre de publication d’événements de Spring
  Modulith joue ce rôle.
- Un équivalent sera à trouver si l’inventaire passe sur Redis (D-016, étape 3).

## À réviser quand

L’inventaire passe sur Redis, ou les événements doivent sortir du monolithe par un broker.
