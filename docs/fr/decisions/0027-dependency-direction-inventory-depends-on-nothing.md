# 0027 — Sens des dépendances : l’inventaire ne dépend d’aucun module

> Traduction française de `docs/decisions/0027-dependency-direction-inventory-depends-on-nothing.md`, qui fait foi.

- **Statut** : Acceptée
- **Date** : 2026-10-25, révisée le 2026-10-26 (revue de fondation)
- **Liens** : D-004, D-009, D-013, D-014, D-016, D-020, D-024, D-028 ; remplace le flux 0
  d’origine, où l’inventaire écoutait `EventCreated`

## Contexte

À l’origine, l’inventaire écoutait `EventCreated` pour créer son stock, tandis que
`GET /v1/events/{id}` renvoyait `remaining_seats`, que seul l’inventaire connaît. Spring
Modulith compte l’écoute du type d’événement d’un autre module comme une dépendance :
`inventory → event` (listener) plus `event → inventory` (lecture des places restantes)
formaient un cycle, et le test de vérification échoue sur les cycles (D-020).

L’inventaire doit être une capacité générique (D-004) : il doit connaître les places et les
blocages, pas ce qu’est un « événement ». Il a aussi besoin, par événement, de la capacité, de
l’heure d’ouverture (D-028) et de la durée de blocage (R5, `hold_duration_seconds` dans le
contrat).

La revue de fondation (2026-10-26) a aussi constaté que les places restantes ne sont pas
nécessaires pour prouver l’itération 1 (des blocages corrects sous concurrence) : les clients
savent déjà s’ils doivent réessayer grâce aux types de problème 409 (D-029), et le test de charge
vérifie directement la base. Les places restantes sont une lecture volatile et volumineuse qui
relève du chemin de lecture (D-009, D-014).

## Options envisagées

1. **Événement sert les détails, l’inventaire expose la disponibilité** — pas de cycle en
   lecture ; change le contrat, le client fait deux appels, et l’inventaire écoute toujours
   `EventCreated`.
2. **Événement garde une projection des places restantes alimentée par les événements de
   l’inventaire** — cohérent avec le CQRS (D-009) ; le cycle demeure.
3. **Reporter les places restantes ; l’inventaire écoute `EventCreated`** — plus de cycle une
   fois l’arête de lecture supprimée, pas d’événement auto-consommé ; l’inventaire connaît les
   types du module Événement (amende D-004) et doit changer quand `EventCreated` change.
4. **Reporter les places restantes ; Événement pilote l’inventaire par un appel** — Événement
   publie `EventCreated` et le consomme lui-même, après commit, pour appeler
   `inventory.createStock(...)` ; l’inventaire reste générique.

## Décision

Option 4. Les places restantes sont retirées de `GET /v1/events/{id}` dans l’itération 1
(contrat 1.2.0) et reportées au chemin de lecture. L’inventaire ne dépend d’aucun module.
Événement publie `EventCreated` et l’écoute lui-même (listener après commit, rejoué par le
registre de publication d’événements de Modulith) pour appeler
`inventory.createStock(eventId, capacity, opensAt, holdDuration)`, qui est idempotent. Graphe
des modules : `event → inventory` (création du stock seulement), `booking → inventory`.

## Conséquences

- L’inventaire reste générique et réutilisable : il ne change que pour ses propres besoins,
  jamais parce que le module Événement change. Graphe acyclique vérifié à chaque build.
- La création du stock est hors de la transaction de l’événement : elle repose sur le registre
  (au moins une fois), sans transaction distribuée. `createStock` doit être idempotent : une
  publication rejouée ne doit ni créer un deuxième stock ni réinitialiser les compteurs.
- La durée de blocage et l’heure d’ouverture sont copiées dans le stock ; l’immuabilité des
  données de l’événement dans l’itération 1 garde les deux copies cohérentes (D-028).
- Il existe une courte fenêtre où l’événement existe mais pas son stock. Un blocage reçoit alors
  `UNKNOWN` de l’inventaire et Réservation renvoie `404 /problems/event-not-found`. Pour que
  cette fenêtre reste avant l’ouverture de la vente, `sale_start_time` doit être postérieur à
  `now()` plus une marge paramétrable, supérieure au délai de relance du registre.
- Un stock jamais créé (publication bloquée dans le registre) rendrait une vente impossible : une
  métrique et une alerte sur les publications d’événements incomplètes sont obligatoires.
- Un événement auto-consommé se lit moins facilement qu’un appel direct ; il doit être documenté
  dans le module Événement.
- Les clients ne voient pas les places restantes dans l’itération 1.

## À réviser quand

- Les places restantes sont conçues avec le chemin de lecture (salle d’attente et Vue de
  position).
- Des publications `EventCreated` incomplètes sont observées en dehors de pannes passagères.
- Un deuxième consommateur de l’inventaire a besoin que l’inventaire réagisse à ses événements
  (l’inventaire aurait alors besoin de son propre contrat d’événements entrants), ou
  l’inventaire est extrait en déployable.
