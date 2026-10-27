# 0009 — Séparer le chemin de lecture de la position du chemin de réservation (CQRS)

> Traduction française de `docs/decisions/0009-separate-read-and-booking-paths.md`, qui fait foi.

- **Statut** : Acceptée
- **Date** : 2026-10-09 (date du document de référence ; date d’origine de la décision non consignée)
- **Liens** : R2b, R13, D-014, D-017

## Contexte

Les lectures de position dominent la charge d’un facteur 100 environ : ~150 000 req/s avec un
rafraîchissement fixe à 10 s, ~60 000 req/s avec R13, et elles tolèrent une légère imprécision.
Les écritures de réservation se comptent en milliers par seconde et ne tolèrent aucune erreur.
Les servir depuis le même modèle laisserait le volume de lecture menacer le chemin d’écriture.

## Options envisagées

1. **Un seul modèle pour les lectures et les écritures** — pas de synchronisation ; les lectures
   disputent les mêmes ressources au chemin critique.
2. **Chemin de lecture (position) séparé du chemin de réservation.**

## Décision

Le chemin de lecture de la position est séparé du chemin de réservation (CQRS).

## Conséquences

- Les lectures peuvent être dimensionnées et mises en cache indépendamment (D-014, D-017).
- La position affichée peut être légèrement en retard sur la réalité.
- Deux modèles doivent être synchronisés.

## À réviser quand

Signal candidat (absent de la référence) : le volume de lecture mesuré est assez faible pour que
le modèle de réservation le serve sans affecter la latence des blocages.
