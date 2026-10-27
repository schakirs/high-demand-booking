# 0008 — Découpler l’expiration visible de la libération réelle (période de grâce)

> Traduction française de `docs/decisions/0008-grace-period-before-release.md`, qui fait foi.

- **Statut** : Acceptée
- **Date** : 2026-10-09 (date du document de référence ; date d’origine de la décision non consignée)
- **Liens** : R5, R6, R7, R8, D-010

## Contexte

Un blocage dure 5 minutes (R5). Un paiement peut être en cours quand le compte à rebours se
termine ; si la place est libérée à cet instant, le paiement aboutit pour une place qui n’existe
plus et doit être remboursé (R8). Les paiements tardifs ne peuvent pas être éliminés, mais ils
peuvent être rendus rares.

## Options envisagées

1. **Libérer à l’expiration et seulement traiter les paiements tardifs** (remboursement
   automatique, R8) — simple ; rend les paiements tardifs fréquents.
2. **Garder la place bloquée pendant une période de grâce si un paiement est en cours.**

## Décision

L’expiration visible et la libération réelle sont découplées : si un paiement est en cours à
l’expiration, la place reste bloquée 1 minute de plus (paramétrable, R7).

## Conséquences

- Les paiements tardifs deviennent plus rares au lieu d’être seulement traités.
- Une place peut rester immobilisée une minute de plus.
- R8 (remboursement automatique et idempotent) reste nécessaire pour les paiements au-delà de la
  période de grâce.

## À réviser quand

Signal candidat (absent de la référence) : le taux de paiements tardifs après la grâce reste
significatif, ou les périodes de grâce retardent visiblement la disponibilité des places en fin
de vente.
