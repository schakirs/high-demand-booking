# 0001 — L’apprentissage prime sur tout objectif commercial

> Traduction française de `docs/decisions/0001-learning-over-commercial-goals.md`, qui fait foi.

- **Statut** : Acceptée
- **Date** : 2026-10-09 (date du document de référence ; date d’origine de la décision non consignée)
- **Liens** : toutes les décisions

## Contexte

Le projet est un support d’apprentissage de l’architecture logicielle, construit autour d’un
système de réservation soumis à une demande massive et simultanée. Beaucoup de choix pourraient
être tranchés par « ce dont une vraie entreprise aurait besoin maintenant » ; ce critère
supprimerait souvent les problèmes mêmes qui valent d’être étudiés.
Il s’agit de l’objectif du projet et non d’un choix entre alternatives : aucune option n’a été
envisagée.

## Décision

L’apprentissage prime sur tout objectif commercial. Chaque complexité ajoutée doit être liée à
un objectif d’apprentissage.

## Conséquences

- Le système est volontairement plus complexe que le besoin réel (par exemple un monolithe
  modulaire avec trois déployables, une outbox, un inventaire conçu par étapes).
- Chaque ADR doit nommer l’objectif d’apprentissage derrière la complexité qu’elle ajoute.
- Certaines décisions seraient différentes pour un produit en production ; elles ne doivent pas
  être recopiées telles quelles.

## À réviser quand

Signal candidat (absent de la référence) : le projet doit servir de vrais utilisateurs ou de
vraies ventes.
