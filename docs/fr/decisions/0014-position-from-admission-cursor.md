# 0014 — Position = rang − curseur d’admission, servi par cache et CDN

> Traduction française de `docs/decisions/0014-position-from-admission-cursor.md`, qui fait foi.

- **Statut** : Acceptée
- **Date** : 2026-10-09 (date du document de référence ; date d’origine de la décision non consignée)
- **Liens** : R2b, R13, D-006, D-009

## Contexte

Les lectures de position atteignent ~60 000 req/s avec R13. Après le tirage, le rang d’un
utilisateur ne change plus ; seul le curseur d’admission de l’événement avance.

## Options envisagées

1. **Calculer la position de chaque utilisateur à chaque lecture** — exact ; une requête par
   lecture au pic.
2. **Position = rang fixe − un curseur d’admission par événement**, le curseur étant servi par
   cache et CDN.

## Décision

La position est le rang de l’utilisateur moins le curseur d’admission de l’événement. Il y a un
seul curseur par événement, servi par cache et CDN.

## Conséquences

- La plupart des lectures sont absorbées par le cache et le CDN.
- La position est pessimiste : les utilisateurs qui ont quitté la file ne sont pas déduits.
- Le curseur a un léger délai de propagation.

## À réviser quand

La détection des abandons est conçue (question ouverte) et la position pessimiste devient
trompeuse.
