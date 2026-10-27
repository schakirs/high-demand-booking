# 0003 — Supposer les retries : clés d’idempotence générées par le client

> Traduction française de `docs/decisions/0003-client-generated-idempotency-keys.md`, qui fait foi.

- **Statut** : Acceptée, amendée par 0023 (les blocages utilisent une clé métier)
- **Date** : 2026-10-09 (date du document de référence ; date d’origine de la décision non consignée)
- **Liens** : R4, R8, D-015, D-023

## Contexte

Les requêtes rejouées sont un risque de premier ordre : timeouts ambigus, retries clients et
réseaux mobiles instables font qu’une même requête peut atteindre le serveur plusieurs fois. Le
client n’est pas sous notre contrôle : le serveur ne peut pas compter sur lui pour éviter les
doublons. L’entrée en file (R4), les blocages et les paiements (R8, D-015) doivent tous produire
un seul effet par intention de l’utilisateur.
Aucune alternative n’a été envisagée : ignorer les retries contredirait l’objectif d’intégrité
(D-002).

## Décision

Le serveur suppose que les retries arriveront. Chaque opération non idempotente accepte une clé
d’idempotence générée par le client ; une requête rejouée avec la même clé renvoie le résultat
initial. C’est le principe ; le mécanisme est choisi par opération (0023 utilise la clé métier
(événement, utilisateur) pour les blocages).

## Conséquences

- Les retries sont sans risque, quoi que fasse le client.
- Les clés doivent être stockées, avec une durée de conservation encore à définir (question
  ouverte).
- Le comportement face à un retry reçu pendant que la requête initiale est encore en cours reste
  à définir (question ouverte).
- Avec plusieurs places par utilisateur, les blocages auront de nouveau besoin d’une clé
  technique (0023).

## À réviser quand

Non consigné. Signal candidat : le stockage des clés d’idempotence se voit dans la latence
d’écriture ou le coût de stockage lors des tests de charge.
