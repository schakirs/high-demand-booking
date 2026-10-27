# 0006 — Tirage au sort à l’ouverture de la vente, puis FIFO

> Traduction française de `docs/decisions/0006-lottery-at-opening-then-fifo.md`, qui fait foi.

- **Statut** : Acceptée
- **Date** : 2026-10-09 (date du document de référence ; date d’origine de la décision non consignée)
- **Liens** : R1, R2, D-005, D-012

## Contexte

La salle d’attente ouvre 30 minutes avant la vente (R1). Avec un ordre d’arrivée pur, les rangs
dépendraient des millisecondes et de la latence réseau, et tout le monde se connecterait à la
dernière seconde, ce qui concentrerait le pic.

## Options envisagées

1. **FIFO pur par heure d’arrivée** — simple ; récompense la chance réseau et concentre les
   connexions.
2. **Tirage au sort parmi les présents à l’ouverture, puis FIFO pour les arrivants suivants.**

## Décision

À l’ouverture de la vente, les utilisateurs présents dans la salle d’attente reçoivent un rang
tiré au sort ; les arrivants suivants sont placés en FIFO derrière eux.

## Conséquences

- L’ordre ne dépend plus des millisecondes ; les connexions s’étalent sur les 30 minutes.
- Arriver très tôt ne donne aucun avantage.
- Le tirage doit être atomique à l’ouverture, sur un ensemble potentiellement très grand
  (question ouverte).

## À réviser quand

Signal candidat (absent de la référence) : le tirage ne peut pas s’exécuter atomiquement dans la
fenêtre d’ouverture du scénario de référence.
