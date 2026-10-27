# 0010 — Les blocages appartiennent à l’inventaire ; conversion atomique blocage → réservation

> Traduction française de `docs/decisions/0010-hold-owned-by-inventory.md`, qui fait foi.

- **Statut** : Acceptée
- **Date** : 2026-10-09 (date du document de référence ; date d’origine de la décision non consignée)
- **Liens** : R3, R5, R7, R8, R12, D-002, D-024, D-029

## Contexte

Un blocage peut expirer à l’instant même où son paiement réussit. Si l’état du blocage vivait à
plusieurs endroits, la course expiration / paiement devrait être arbitrée entre eux, avec le
risque de libérer une place en cours de réservation.

## Options envisagées

1. **État du blocage partagé entre réservation et inventaire** — chaque module possède une
   partie du cycle de vie ; la course doit être tranchée entre deux sources de vérité.
2. **L’inventaire possède le blocage ; la conversion est une seule opération atomique.**

## Décision

Le blocage appartient à l’inventaire. Convertir un blocage en réservation est une seule
opération atomique, « blocage → réservation si encore actif ».

## Conséquences

- Une seule source de vérité pour les places et les blocages ; la course expiration / paiement
  est tranchée par une opération.
- La réservation dépend des événements de l’inventaire ; son statut peut être brièvement en
  retard (voir 0024).

## À réviser quand

Signal candidat (absent de la référence) : l’inventaire passe sur un stockage où la conversion
atomique n’est plus disponible (D-016, étape 3).
