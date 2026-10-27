# 0023 — Les blocages sont idempotents par clé métier (événement, utilisateur)

> Traduction française de `docs/decisions/0023-hold-idempotent-by-business-key.md`, qui fait foi.

- **Statut** : Acceptée
- **Date** : 2026-10-09 (date du document de référence ; date d’origine de la décision non consignée)
- **Liens** : R3, R4 ; amende 0003 (mécanisme pour les blocages)

## Contexte

D-003 exige que chaque opération résiste aux retries. Pour les blocages, R3 impose déjà au plus
un blocage actif ou une réservation par (événement, utilisateur), au moyen d’un index unique. Une
requête de blocage rejouée ne doit pas prendre une deuxième place.

## Options envisagées

1. **Clé d’idempotence client** (mécanisme de D-003) — générique ; redondante avec l’index
   unique.
2. **Clé métier (événement, utilisateur)** — une requête rejouée reçoit le blocage actif
   existant.

## Décision

Les blocages sont idempotents par la clé métier (événement, utilisateur) plutôt que par une clé
d’idempotence. Une requête rejouée reçoit le blocage actif existant. D-003 reste le principe ;
le mécanisme est choisi par opération.

## Conséquences

- Pas de stockage de clés pour les blocages.
- Une deuxième requête du même utilisateur, retry ou autre onglet, reçoit le même blocage.

## À réviser quand

Plusieurs places par utilisateur sont introduites : une clé d’idempotence redevient nécessaire.
