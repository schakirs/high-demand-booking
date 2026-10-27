# 0029 — Distinguer « complet » de « temporairement indisponible » dès l’itération 1

> Traduction française de `docs/decisions/0029-distinguish-sold-out-from-temporarily-unavailable.md`, qui fait foi.

- **Statut** : Acceptée
- **Date** : 2026-10-25
- **Liens** : R10, R12, D-002, D-016, D-021, D-025

## Contexte

R10 exige « plus de place pour l’instant » tant que des blocages sont actifs, et « complet »
seulement quand toutes les places sont réservées. Le contrat de l’itération 1 renvoyait
`/problems/event-sold-out` dans les deux cas. Sans salle d’attente, réessayer est le seul
recours du client : il doit donc savoir si cela vaut la peine. La distinction doit être calculée
dans la transaction du blocage, sans lire la table des blocages, sur une ligne qui encaisse déjà
~500 écritures/s.

## Options envisagées

1. **Garder un seul problème `event-sold-out` dans l’itération 1** — pas de changement de
   contrat ; viole R10 et pousse les clients à réessayer à l’aveugle ou à abandonner à tort.
2. **Compter les blocages confirmés quand le décrément échoue** — exact ; lit la table des
   blocages sur le chemin chaud au pire moment.
3. **Maintenir un compteur `confirmed` sur la ligne du stock** — mis à jour dans la
   transaction de confirmation ; une colonne de plus lue sur la même ligne quand le décrément
   échoue.

## Décision

Option 3. Un compteur `confirmed` vit sur la ligne du stock et il est mis à jour dans la
transaction de confirmation. Quand le décrément échoue : `confirmed = capacity` →
`/problems/event-sold-out` (définitif) ; sinon `/problems/seats-temporarily-unavailable` avec
`Retry-After: 30`. Un ticket de changement de contrat est créé.

## Conséquences

- R10 est respectée dès l’itération 1 ; les clients distinguent « réessayer plus tard » de
  « abandonner ».
- La confirmation écrit désormais aussi la ligne du stock (contention sur la même ligne, au
  rythme des confirmations).
- Limites acceptées : un blocage expiré pas encore balayé prolonge l’état « temporaire » ;
  `Retry-After` n’est qu’une indication, la vraie protection viendra du rate limiting et de la
  salle d’attente.
- De nouvelles contraintes en base deviennent possibles et doivent être ajoutées :
  `confirmed <= capacity`, `available + confirmed <= capacity`.
- Changement de contrat (nouveau type de problème, en-tête `Retry-After`) au titre de D-021.

## À réviser quand

La salle d’attente rend la distinction visible ailleurs, les retries après `Retry-After`
apparaissent comme un pic de charge mesurable dans k6, ou le retard du balayage rend l’état
temporaire notablement long.
