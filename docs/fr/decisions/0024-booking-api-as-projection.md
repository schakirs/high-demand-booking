# 0024 — Réservation expose l’API ; son état sera alimenté par les événements de l’inventaire

> Traduction française de `docs/decisions/0024-booking-api-as-projection.md`, qui fait foi.

- **Statut** : Acceptée
- **Date** : 2026-10-09 (date du document de référence ; date d’origine de la décision non consignée)
- **Liens** : D-004, D-009, D-010, D-013, D-027 ; révisée le 2026-10-26 (revue de fondation)

## Contexte

L’API publique de réservation doit rester stable quand l’Admission et le paiement arriveront.
L’inventaire est une capacité interne et générique (D-004). Dans la première version du flux A,
Réservation et Inventaire écrivaient tous deux l’état d’un même blocage (double écriture).

## Options envisagées

1. **L’inventaire expose directement l’API des blocages** — moins de sauts ; fait entrer
   l’inventaire générique dans le contrat public.
2. **Réservation écrit son propre état à côté de l’inventaire** — Réservation possède ses
   données ; double écriture dans le flux A.
3. **Réservation est la façade ; son état est une projection des événements de l’inventaire.**

## Décision

L’API de réservation reste exposée par Réservation, façade et futur orchestrateur. Réservation
n’écrit jamais elle-même l’état d’un blocage : quand elle garde un état, il est entièrement
alimenté par les événements de l’inventaire (HoldCreated, HoldConfirmed, HoldExpired). Dans
l’itération 1, elle n’en garde aucun.

## Conséquences

- Le contrat public reste stable quand l’Admission et le paiement arriveront.
- L’inventaire reste interne et générique.
- Plus de double écriture dans le flux A.
- Itération 1 (revue de fondation, 2026-10-26) : Réservation est une façade sans état. Chaque
  endpoint de blocage, `GET /v1/holds/{id}` compris, appelle l’inventaire (`hold`,
  `confirm`, `getHold`), de sorte qu’une lecture juste après une confirmation n’est jamais en
  retard. La projection est reportée jusqu’à ce que le paiement en ait besoin ; l’inventaire
  publie quand même ses événements, sans consommateur pour l’instant.

## À réviser quand

L’Admission et le paiement arrivent et Réservation commence à orchestrer (elle gagne alors son
propre état).
