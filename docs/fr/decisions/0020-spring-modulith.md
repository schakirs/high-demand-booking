# 0020 — Spring Modulith pour les frontières de modules et comme outbox

> Traduction française de `docs/decisions/0020-spring-modulith.md`, qui fait foi.

- **Statut** : Acceptée
- **Date** : 2026-10-09 (date du document de référence ; date d’origine de la décision non consignée)
- **Liens** : D-017, D-027 ; amende 0013 (le registre est l’outbox)

## Contexte

Un monolithe modulaire ne reste modulaire que si ses frontières sont vérifiées. L’outbox (0013) a
besoin d’une table de stockage et d’un relais.

## Options envisagées

1. **Conventions plus une outbox et un relais écrits à la main** — pas de dépendance
   supplémentaire ; frontières non vérifiées et une outbox à construire et à exploiter.
2. **Spring Modulith** — vérification des frontières à chaque build, et son registre de
   publication d’événements comme outbox.

## Décision

Spring Modulith fait respecter les frontières de modules, et son registre de publication
d’événements sert d’outbox (0013).

## Conséquences

- Les frontières sont vérifiées automatiquement à chaque build ; les cycles font échouer le build
  (voir 0027).
- L’outbox est fournie.
- Une dépendance de plus, avec ses conventions (par exemple, écouter le type d’événement d’un
  autre module compte comme une dépendance).

## À réviser quand

Signal candidat (absent de la référence) : un module est extrait en déployable, ou le rejeu au
moins une fois du registre ne suffit plus (publications incomplètes en production).
