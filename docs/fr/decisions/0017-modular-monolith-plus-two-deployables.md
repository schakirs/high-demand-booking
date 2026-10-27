# 0017 — Monolithe modulaire plus deux déployables séparés

> Traduction française de `docs/decisions/0017-modular-monolith-plus-two-deployables.md`, qui fait foi.

- **Statut** : Acceptée
- **Date** : 2026-10-09 (date du document de référence ; date d’origine de la décision non consignée)
- **Liens** : D-009, D-011, D-014, D-020

## Contexte

Le chemin de lecture de la position a besoin de ressources de calcul séparées de celles du
chemin de réservation (D-009). Le simulateur de paiement doit se comporter comme un vrai appel
réseau (latence, timeouts, webhooks en double). Les autres modules peuvent être isolés dans un
même processus avec des bulkheads.

## Options envisagées

1. **Un seul déployable pour tout** — le plus simple ; le chemin de lecture concurrence la
   réservation, et un simulateur dans le même processus ne reproduit pas un vrai appel réseau.
2. **Monolithe modulaire + Vue de position + simulateur de paiement**, en déployables séparés.
3. **Un service par module** — isolation maximale ; complexité distribuée sans objectif
   d’apprentissage qui l’exige maintenant.

## Décision

Un monolithe modulaire plus deux déployables séparés : la Vue de position et le simulateur de
paiement. Les autres isolations sont traitées par des bulkheads.

## Conséquences

- Trois déployables à exploiter au lieu de nombreux.
- Un déploiement redémarre tous les modules du monolithe.
- La première extraction pédagogique sera la Notification.

## À réviser quand

Un correctif doit être déployé pendant une vente (le redémarrage de tous les modules devient un
vrai problème).
