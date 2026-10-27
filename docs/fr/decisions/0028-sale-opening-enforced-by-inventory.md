# 0028 — Ouverture de la vente contrôlée par l’inventaire, horloge PostgreSQL comme source du temps

> Traduction française de `docs/decisions/0028-sale-opening-enforced-by-inventory.md`, qui fait foi.

- **Statut** : Acceptée
- **Date** : 2026-10-25
- **Liens** : R1, D-002, D-010, D-012, D-016, D-027 ; clôt la question ouverte sur la source du
  temps entre instances

## Contexte

Un blocage doit être refusé avant l’ouverture de la vente (R1, `409 /problems/sale-not-open`).
Le stock est créé bien avant l’ouverture (D-027) : son existence ne suffit donc pas. Le contrôle
ne doit introduire ni course lecture-puis-écriture, ni appel synchrone sur le chemin des blocages
(cible p99 < 100 ms, ~500 blocages/s). Plusieurs instances applicatives tournent, avec des
horloges qui peuvent dériver. À partir de l’itération 2, l’Admission (alimentée par le
Planificateur, D-012) contrôlera l’accès aux blocages.

## Options envisagées

1. **L’inventaire stocke `opens_at` et le vérifie dans sa mise à jour conditionnelle** —
   atomique, sans appel supplémentaire ; duplique l’heure d’ouverture du module Événement ; un
   report doit être propagé.
2. **Réservation interroge le module Événement à chaque blocage** — une seule copie de la
   donnée ; ajoute un appel synchrone sur le chemin critique et un contrôle-puis-action entre
   deux modules.

## Décision

Option 1. L’inventaire stocke `opens_at` et refuse tout blocage tant que `now() < opens_at`,
dans sa mise à jour conditionnelle. Les dates d’un événement sont immuables dans l’itération 1 :
il n’y a pas d’opération de report ; la modification d’un événement est prévue après
l’itération 1. À partir de l’itération 2, ce contrôle devient une défense en profondeur derrière
l’Admission. La source de vérité du temps est l’horloge PostgreSQL (`now()`).

## Conséquences

- Le contrôle d’ouverture est atomique avec le décrément des places ; pas de course à l’instant
  de l’ouverture.
- Une seule horloge pour toutes les instances ; la dérive des horloges applicatives ne peut pas
  ouvrir une vente en avance.
- `opens_at` existe dans deux modules ; l’immuabilité garde les deux copies cohérentes dans
  l’itération 1. Quand la modification d’un événement arrivera, l’inventaire aura besoin d’une
  opération autorisée seulement avant l’ouverture (mise à jour conditionnelle sur
  `opens_at > now()`).
- `now()` est l’heure de début de la transaction : conservé car les transactions sont courtes ;
  `clock_timestamp()` seulement si un besoin apparaît.
- Toute comparaison de temps qui compte (expiration d’un blocage, confirmation, statut de
  `GET /v1/holds/{id}`) utilise l’horloge de la base. Les tests déplacent la donnée, pas le
  temps : des fixtures SQL placent `expires_at` dans le passé ; le balayage d’expiration est
  attendu avec Awaitility.

## À réviser quand

- La modification d’un événement est introduite : le report doit alors être conçu.
- Une transaction longue fait dériver `now()` de façon notable.
- L’inventaire passe sur Redis (D-016, étape 3), ce qui supprime `now()` comme horloge partagée.
- L’Admission rend le contrôle redondant et son coût se voit dans la latence des blocages.
