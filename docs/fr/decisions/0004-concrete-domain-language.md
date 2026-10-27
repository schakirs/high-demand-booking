# 0004 — Langage du domaine concret ; seul l’inventaire est générique

> Traduction française de `docs/decisions/0004-concrete-domain-language.md`, qui fait foi.

- **Statut** : Acceptée
- **Date** : 2026-10-09 (date du document de référence ; date d’origine de la décision non consignée)
- **Liens** : D-024, D-027

## Contexte

Le système vend des places pour des événements. Il est tentant de modéliser dès le départ une
plateforme générique de « réservation de ressources », mais il n’existe qu’un seul cas d’usage.
L’inventaire (places, blocages, compteurs) est la seule partie dont le comportement ne dépend
pas de ce qui est vendu.

## Options envisagées

1. **Modèle générique partout** (ressource, créneau, réservation) — réutilisable en théorie ;
   écartée : généraliser sans deuxième cas d’usage, c’est deviner.
2. **Langage concret (Event, Hold, Booking), inventaire seul générique.**

## Décision

Le domaine utilise un langage concret (Event, Hold, Booking). Seul l’inventaire est générique.

## Conséquences

- Le code et les documents se lisent comme le métier qu’ils servent.
- L’inventaire reste réutilisable et ne doit pas dépendre des concepts d’événement (garanti par
  D-027).
- Des renommages pourront être nécessaires si un deuxième cas d’usage apparaît.

## À réviser quand

Un deuxième cas d’usage (un autre type de ressource à vendre) est réellement prévu.
