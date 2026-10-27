# 0026 — Itération 1 en local et en CI seulement ; outillage ; une ADR par décision

> Traduction française de `docs/decisions/0026-iteration-1-local-and-tooling.md`, qui fait foi.

- **Statut** : Acceptée
- **Date** : 2026-10-09 (date du document de référence ; date d’origine de la décision non consignée)
- **Liens** : D-018, D-019, D-022

## Contexte

L’itération 1 doit prouver que les blocages restent corrects sous concurrence ; cette preuve n’a
pas besoin du cloud. L’outillage doit être gratuit et standard. Une grande partie du code est
générée par l’IA : le formatage doit donc être imposé mécaniquement. Les décisions doivent être
lisibles par un lecteur extérieur (par exemple un recruteur).

## Options envisagées

1. **Déployer dans le cloud dès l’itération 1** — mesures réalistes tôt ; coût et effort de mise
   en place avant que le cœur soit prouvé.
2. **Local et CI seulement.**
3. **Décisions gardées seulement dans le journal de la référence** — un seul endroit ; difficile
   à lire et à lier.
4. **Un fichier ADR par décision.**

## Décision

L’itération 1 s’exécute entièrement en local et en CI. Outillage : k6 pour la charge, Prometheus
et Grafana pour l’observabilité locale, google-java-format via Spotless imposé en CI. Les
décisions sont consignées à raison d’un fichier ADR par décision dans `docs/decisions/`.

## Conséquences

- Outils gratuits et standards ; formatage stable malgré le code généré ; ADR lisibles.
- Pas de mesure en environnement cloud pendant l’itération 1.
- Plus de fichiers à maintenir.

## À réviser quand

Un environnement cloud est prévu, ou les mesures locales ne permettent pas de répondre à une
question qu’une itération doit trancher.
