# 0015 — Appels au paiement idempotents, réconciliation, webhooks idempotents

> Traduction française de `docs/decisions/0015-idempotent-payment-calls.md`, qui fait foi.

- **Statut** : Acceptée
- **Date** : 2026-10-09 (date du document de référence ; date d’origine de la décision non consignée)
- **Liens** : R6, R8, D-003

## Contexte

Un timeout sur un appel de paiement ne dit pas si le paiement a été créé. Le prestataire répond
plus tard par des webhooks, qui peuvent arriver plusieurs fois.

## Options envisagées

Non consignées au moment de la décision.

## Décision

Les appels au paiement sont idempotents (même clé au retry), les paiements sont réconciliés, et
les webhooks sont d’abord acquittés puis traités de façon idempotente.

## Conséquences

- Un appel rejoué ne crée jamais un deuxième paiement ; un webhook en double reste sans effet.
- La logique de réconciliation doit être maintenue.

## À réviser quand

Signal candidat (absent de la référence) : le vrai prestataire de paiement remplace le simulateur
et offre des garanties d’idempotence différentes.
