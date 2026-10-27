# 0012 — Le Planificateur publie le « quand » ; les composants exécutent le « quoi »

> Traduction française de `docs/decisions/0012-scheduler-publishes-when.md`, qui fait foi.

- **Statut** : Acceptée
- **Date** : 2026-10-09 (date du document de référence ; date d’origine de la décision non consignée)
- **Liens** : R1, R2, D-006, D-028

## Contexte

Plusieurs moments rythment une vente : ouverture de la salle d’attente, ouverture de la vente
(tirage au sort), clôture de la vente. Avec plusieurs instances, si chaque composant planifiait
ses propres minuteries, on risquerait des déclenchements manqués ou en double et des horloges
incohérentes.

## Options envisagées

1. **Chaque composant planifie ses propres actions temporelles** — pas de composant
   supplémentaire ; gestion du temps et des instances multiples dupliquée partout.
2. **Un Planificateur publie les événements temporels ; chaque composant réagit de façon
   idempotente.**

## Décision

Le Planificateur publie le « quand » ; chaque composant exécute le « quoi » de façon idempotente.

## Conséquences

- La gestion du temps et des instances multiples est concentrée à un seul endroit.
- Un déclenchement en double reste sans effet.
- Un composant de plus.
- Périmètre (revue de fondation, 2026-10-26) : le Planificateur déclenche les moments métier
  (ouverture de la salle d’attente, ouverture de la vente, clôture de la vente). L’expiration
  des données qu’un composant possède, comme les blocages, reste à ce composant (0031). Dans
  l’itération 1, il n’y a pas de Planificateur : l’ouverture de la vente est contrôlée par
  l’inventaire avec l’horloge de la base (0028).

## À réviser quand

Signal candidat (absent de la référence) : un déclenchement manqué ou en double est observé, ou
les contrôles temporels passent entièrement dans les composants propriétaires des données.
