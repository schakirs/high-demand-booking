# 0007 — Identité minimale : utilisateurs de test et jetons signés

> Traduction française de `docs/decisions/0007-minimal-identity.md`, qui fait foi.

- **Statut** : Acceptée, amendée par 0030 (mécanisme)
- **Date** : 2026-10-09 (date du document de référence ; date d’origine de la décision non consignée)
- **Liens** : R3, D-001, D-030

## Contexte

L’authentification n’est pas un objectif d’apprentissage (D-001), et un vrai parcours de
connexion gênerait les tests de charge, qui ont besoin de milliers d’utilisateurs. Le système a
tout de même besoin d’identités distinctes (R3 : au plus un blocage actif ou une réservation par
utilisateur et événement).

## Options envisagées

1. **Vraie authentification** — proche de la production ; coûteuse, hors objectif, et gênante
   pour les tests de charge.
2. **Utilisateurs de test avec jetons signés.**

## Décision

L’identité est minimale : utilisateurs de test et jetons signés. Le mécanisme est précisé dans
0030.

## Conséquences

- Les tests de charge peuvent créer beaucoup d’identités à peu de frais.
- Ce composant doit être remplacé avant toute exposition réelle.

## À réviser quand

L’application est exposée au-delà des environnements local et CI.
