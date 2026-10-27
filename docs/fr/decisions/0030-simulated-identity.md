# 0030 — Identité simulée par jetons bearer signés localement

> Traduction française de `docs/decisions/0030-simulated-identity.md`, qui fait foi.

- **Statut** : Acceptée
- **Date** : 2026-10-25
- **Liens** : R3, D-021 ; amende 0007 (mécanisme)

## Contexte

L’authentification n’est pas un objectif d’apprentissage (D-007), mais R3 (un blocage actif ou
une réservation par utilisateur et événement) et les tests de charge ont besoin de nombreuses
identités distinctes et fiables, plus un rôle administrateur pour créer les événements. Le
contrat déclare déjà un schéma bearer et n’a pas d’endpoint d’émission de jetons.

## Options envisagées

1. **Identifiant utilisateur dans un en-tête de requête** — trivial ; change le contrat et
   laisse n’importe quel client choisir son identité, ce qui affaiblit les tests de R3.
2. **Endpoint d’émission de jetons dans l’application** — pratique pour k6 ; ajoute un endpoint
   qui pourrait fuiter en production.
3. **Script hors ligne qui émet des jetons signés, validés par Spring Security** — pas de
   changement de contrat, pas de code d’émission dans l’application.

## Décision

Option 3. Le contrat garde le schéma bearer. Un script génère des jetons signés pour N
utilisateurs de test et un administrateur ; Spring Security les valide avec une clé locale.
L’émission de jetons n’existe jamais dans le profil de production.

## Conséquences

- R3 et k6 fonctionnent avec de vraies identités distinctes, issues du jeton.
- Pas de changement de contrat ; la couche de sécurité est celle sur laquelle un vrai
  fournisseur viendrait se brancher.
- La gestion des clés est locale et simpliste ; ce composant doit être remplacé avant toute
  exposition réelle (D-007).
- Les jetons doivent être régénérés quand ils expirent ou que la clé change.

## À réviser quand

L’application est exposée au-delà des environnements local et CI, ou un vrai fournisseur
d’identité est introduit.
