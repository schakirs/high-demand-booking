# 0016 — Inventaire en trois étapes, en commençant par une mise à jour conditionnelle PostgreSQL

> Traduction française de `docs/decisions/0016-inventory-in-three-stages.md`, qui fait foi.

- **Statut** : Acceptée
- **Date** : 2026-10-09 (date du document de référence ; date d’origine de la décision non consignée)
- **Liens** : R3, R9, R12, D-002, D-010, D-028, D-029

## Contexte

Bloquer une place est un problème de contention, pas de volume : à un débit d’admission de
500/s, chaque blocage écrit le même compteur de stock. Le scénario de référence (40 000 places)
demande une première vague d’environ 80 s à ce débit. La salle d’attente plafonne le débit
d’arrivée sur le chemin des blocages (R9) : l’inventaire n’a donc pas à absorber le pic brut.
L’intégrité passe en premier (D-002) : l’invariant bloquées + réservées <= capacité (R12) doit
tenir à tout instant, avec une durabilité forte. Le chiffre de 500 blocages/s est une hypothèse
à mesurer par le test de charge.

## Options envisagées

Les trois approches sont consignées comme des étapes successives plutôt que comme des options
concurrentes :

1. **Mise à jour conditionnelle PostgreSQL sur un compteur, plus une contrainte CHECK** — une
   ligne par événement, `available = available - 1 WHERE available > 0` ; simple, durable,
   atomique ; débit plafonné par une seule ligne chaude.
2. **Une ligne par place, réclamée avec `SELECT ... FOR UPDATE SKIP LOCKED`** — répartit la
   contention sur plusieurs lignes et supporte les places numérotées ; plus de lignes et des
   requêtes plus complexes.
3. **Opérations atomiques dans Redis** — débit le plus élevé ; durabilité plus faible et une
   deuxième source de vérité à réconcilier.

## Décision

Commencer par l’étape 1 pour le MVP ; passer à l’étape 2, puis 3, seulement quand leur signal
apparaît. Le contrat de l’inventaire reste stable pour que les implémentations soient
interchangeables.

## Conséquences

- Durabilité forte et implémentation simple et testable pour le MVP.
- Le débit est plafonné par la ligne en base ; ce plafond doit être mesuré.
- Les implémentations doivent rester derrière un contrat stable.
- Passer sur Redis supprime `now()` comme horloge partagée (0028) et demande un équivalent de
  l’outbox (0013).

## À réviser quand

- Des places numérotées sont introduites, ou la première vague dépasse 2 minutes → étape 2.
- Plusieurs ventes simultanées dépassent ce que la base peut absorber → étape 3.
