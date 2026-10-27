# 0021 — API contract-first, erreurs Problem Details, version dans l’URL

> Traduction française de `docs/decisions/0021-contract-first-api.md`, qui fait foi.

- **Statut** : Acceptée
- **Date** : 2026-10-09 (date du document de référence ; date d’origine de la décision non consignée)
- **Liens** : D-025, D-029

## Contexte

L’API est conçue avant d’être codée, et Claude Code l’implémente. Un contrat validé donne à
l’implémentation une cible fixe et permet aux revues de se concentrer sur le comportement.

## Options envisagées

1. **Code-first, contrat généré depuis le code** — démarrage plus rapide ; l’API prend la forme
   de l’implémentation.
2. **Contract-first** — le contrat OpenAPI est écrit et validé avant l’implémentation.

## Décision

L’API est contract-first : le contrat OpenAPI est écrit et validé avant l’implémentation. Les
erreurs utilisent Problem Details (RFC 9457). La version est dans l’URL (`/v1`).

## Conséquences

- Claude Code implémente un contrat validé.
- Contrat et code doivent rester synchronisés (interfaces générées, ticket 5).
- Tout changement de contrat est un changement explicite (par exemple 0029).

## À réviser quand

Signal candidat (absent de la référence) : les changements de contrat deviennent si fréquents
que l’étape de validation ralentit chaque ticket.
