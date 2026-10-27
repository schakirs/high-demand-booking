# 0002 — L’intégrité avant la disponibilité sur le chemin de réservation

> Traduction française de `docs/decisions/0002-integrity-over-availability.md`, qui fait foi.

- **Statut** : Acceptée
- **Date** : 2026-10-09 (date du document de référence ; date d’origine de la décision non consignée)
- **Liens** : R3, R12, D-010, D-016

## Contexte

Une place survendue est irréversible : quelqu’un détient un billet qui n’existe pas. Une période
d’indisponibilité est récupérable : les utilisateurs attendent, réessaient ou savent où ils en
sont. Dans le scénario de référence (40 000 places, 1,5 million d’utilisateurs connectés), le
chemin de réservation sera poussé au-delà de ses limites : le système a besoin d’une règle sur ce
qu’il sacrifie en premier.

## Options envisagées

1. **Privilégier la disponibilité** — continuer à accepter des blocages sous stress et réparer
   les conflits ensuite ; écartée, car une survente ne se répare pas.
2. **Privilégier l’intégrité** — ralentir ou refuser quand la justesse est menacée.

## Décision

L’intégrité passe avant la disponibilité sur le chemin de réservation : le système ralentit ou
refuse plutôt que de risquer une survente.

## Conséquences

- L’invariant bloquées + réservées <= capacité (R12) n’est jamais échangé contre du débit.
- Les utilisateurs peuvent recevoir des refus ou des attentes qu’un système plus permissif
  éviterait.
- Tout mécanisme du chemin de réservation (compteurs, conversions, retries) doit échouer de façon
  sûre (fail closed).

## À réviser quand

Signal candidat (absent de la référence) : un cas d’usage apparaît où la survente peut être
compensée (par exemple du surbooking avec une solution de repli garantie).
