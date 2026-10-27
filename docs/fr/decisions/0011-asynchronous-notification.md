# 0011 — Notification asynchrone, hors du chemin critique, avec retries

> Traduction française de `docs/decisions/0011-asynchronous-notification.md`, qui fait foi.

- **Statut** : Acceptée
- **Date** : 2026-10-09 (date du document de référence ; date d’origine de la décision non consignée)
- **Liens** : D-013, D-017

## Contexte

Après la confirmation d’une réservation, l’utilisateur reçoit un email ou une notification push.
Le service de notification n’est pas critique ; la réservation l’est.

## Options envisagées

1. **Notification synchrone dans le flux de réservation** — immédiate ; un échec ou un
   ralentissement de la notification ferait échouer ou ralentir la réservation.
2. **Notification asynchrone avec retries.**

## Décision

La notification est asynchrone, hors du chemin critique, avec retries.

## Conséquences

- Un composant non critique ne peut jamais faire échouer un composant critique.
- L’email de confirmation est différé.
- La notification est la première candidate à une extraction pédagogique (0017).

## À réviser quand

Signal candidat (absent de la référence) : les utilisateurs ont besoin de la notification comme
seule preuve de réservation, dans un délai strict.
