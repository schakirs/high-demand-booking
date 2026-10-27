# 0031 — Expiration des blocages par un balayage périodique de l’inventaire

> Traduction française de `docs/decisions/0031-hold-expiry-by-inventory-sweep.md`, qui fait foi.

- **Statut** : Acceptée
- **Date** : 2026-10-26
- **Liens** : R5, R9, R12, D-010, D-012, D-016, D-028 ; clôt la question ouverte « balayage
  périodique ou messages différés »

## Contexte

Chaque blocage doit être expiré dès que `expires_at <= now()` : sa place est libérée et
`HoldExpired` est publié (flux C). Dans le scénario de référence, les blocages expirent à
jusqu’à ~40 000 instants différents par vente. D-012 confie au Planificateur le « quand » des
actions temporelles, mais cette décision visait quelques moments métier par événement
(ouverture, clôture). La justesse ne dépend jamais du balayage : chaque contrôle (confirmation,
lecture, nouveau blocage) traite déjà un blocage comme expiré dès que `expires_at <= now()`
(0028) ; le balayage ne fait que libérer la place.

## Options envisagées

1. **Le Planificateur publie un message différé par blocage** — suit D-012 à la lettre ; un
   message par blocage, un broker ou une file à délai, et un chemin d’expiration hors du
   propriétaire de la donnée.
2. **L’inventaire balaie périodiquement les blocages expirés** — lots avec
   `FOR UPDATE SKIP LOCKED`, une mise à jour conditionnelle par blocage ; pas d’infrastructure
   supplémentaire ; une place libérée peut revenir avec jusqu’à un intervalle de balayage de
   retard.

## Décision

Option 2, au-delà de l’itération 1. L’inventaire expire ses propres blocages par un balayage
périodique. Le Planificateur garde les moments métier (ouverture de la salle d’attente,
ouverture de la vente, clôture de la vente).

## Conséquences

- L’expiration reste chez le propriétaire de la donnée, atomique avec le réincrément du compteur.
- Sûr avec plusieurs instances (`SKIP LOCKED`, mise à jour conditionnelle).
- Une place libérée devient disponible avec jusqu’à un intervalle de balayage de retard, ce qui
  prolonge l’état « temporairement indisponible » (0029).
- La taille des lots et l’intervalle doivent être réglés et observés.

## À réviser quand

Le retard du balayage se voit dans la disponibilité des places en fin de vente, le coût du
balayage se voit dans la latence des blocages, ou l’inventaire passe sur Redis (D-016, étape 3),
où l’expiration des clés pourrait le remplacer.
