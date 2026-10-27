# 0018 — Trunk-based development, un seul artefact promu d’un environnement à l’autre

> Traduction française de `docs/decisions/0018-trunk-based-development.md`, qui fait foi.

- **Statut** : Acceptée
- **Date** : 2026-10-09 (date du document de référence ; date d’origine de la décision non consignée)
- **Liens** : D-019

## Contexte

Un seul développeur, avec Claude Code qui produit une grande partie du code. Les branches longues
divergent, et reconstruire par environnement revient à déployer autre chose que ce qui a été
testé.

## Options envisagées

1. **Branches longues (par exemple develop/release)** — familières ; plus de divergence et de
   fusions.
2. **Trunk-based : une branche principale, des branches courtes par ticket, fusion par pull
   request.**

## Décision

Trunk-based development : une branche principale, des branches courtes par ticket, fusionnées
par pull request. Le même artefact est promu d’un environnement au suivant.

## Conséquences

- Moins de divergence ; ce qui est déployé est ce qui a été testé.
- La branche principale doit toujours rester déployable.

## À réviser quand

Signal candidat (absent de la référence) : plusieurs contributeurs travaillent en parallèle et la
branche principale casse régulièrement.
