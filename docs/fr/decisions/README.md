# Architecture Decision Records

> Traduction française de `docs/decisions/README.md`, qui fait foi.

Un fichier par décision (D-026). L’ADR `00NN` consigne la décision `D-0NN` de la référence du
projet. Modèle : `docs/decisions/0000-template.md` (non traduit). Les ADR 0001 à 0026 ont été
converties depuis la référence le 2026-10-25 : les options ne sont listées que lorsque la
justification consignée nomme l’alternative qu’elle écarte ; « Non consignées » signifie que la
référence ne garde aucune alternative. « Signal candidat » dans *À réviser quand* marque un
signal ajouté pendant la conversion.

Une ADR acceptée n’est pas réécrite. Un changement est une nouvelle ADR qui amende (changement
partiel) ou remplace (changement total) une ADR antérieure ; seule la ligne Statut de l’ADR
antérieure est mise à jour. Cette règle s’applique à partir du premier commit des ADR
(ticket 1). L’anglais fait foi ; les traductions françaises sont dans `docs/fr/decisions/`.

| ADR | Titre | Statut |
|---|---|---|
| [0001](0001-learning-over-commercial-goals.md) | L’apprentissage prime sur tout objectif commercial | Acceptée |
| [0002](0002-integrity-over-availability.md) | L’intégrité avant la disponibilité sur le chemin de réservation | Acceptée |
| [0003](0003-client-generated-idempotency-keys.md) | Supposer les retries : clés d’idempotence générées par le client | Acceptée, amendée par 0023 (les blocages utilisent une clé métier) |
| [0004](0004-concrete-domain-language.md) | Langage du domaine concret ; seul l’inventaire est générique | Acceptée |
| [0005](0005-waiting-room-always-on.md) | La salle d’attente est toujours active dans le MVP | Acceptée |
| [0006](0006-lottery-at-opening-then-fifo.md) | Tirage au sort à l’ouverture de la vente, puis FIFO | Acceptée |
| [0007](0007-minimal-identity.md) | Identité minimale : utilisateurs de test et jetons signés | Acceptée, amendée par 0030 (mécanisme) |
| [0008](0008-grace-period-before-release.md) | Découpler l’expiration visible de la libération réelle (période de grâce) | Acceptée |
| [0009](0009-separate-read-and-booking-paths.md) | Séparer le chemin de lecture de la position du chemin de réservation (CQRS) | Acceptée |
| [0010](0010-hold-owned-by-inventory.md) | Les blocages appartiennent à l’inventaire ; conversion atomique blocage → réservation | Acceptée |
| [0011](0011-asynchronous-notification.md) | Notification asynchrone, hors du chemin critique, avec retries | Acceptée |
| [0012](0012-scheduler-publishes-when.md) | Le Planificateur publie le « quand » ; les composants exécutent le « quoi » | Acceptée |
| [0013](0013-transactional-outbox.md) | Publier les événements par une transactional outbox | Acceptée, amendée par 0020 (l’outbox est le registre Spring Modulith) |
| [0014](0014-position-from-admission-cursor.md) | Position = rang − curseur d’admission, servi par cache et CDN | Acceptée |
| [0015](0015-idempotent-payment-calls.md) | Appels au paiement idempotents, réconciliation, webhooks idempotents | Acceptée |
| [0016](0016-inventory-in-three-stages.md) | Inventaire en trois étapes, en commençant par une mise à jour conditionnelle PostgreSQL | Acceptée |
| [0017](0017-modular-monolith-plus-two-deployables.md) | Monolithe modulaire plus deux déployables séparés | Acceptée |
| [0018](0018-trunk-based-development.md) | Trunk-based development, un seul artefact promu d’un environnement à l’autre | Acceptée |
| [0019](0019-kanban-on-github.md) | Kanban sur GitHub Issues et Projects, limite de 2 tickets en cours | Acceptée |
| [0020](0020-spring-modulith.md) | Spring Modulith pour les frontières de modules et comme outbox | Acceptée |
| [0021](0021-contract-first-api.md) | API contract-first, erreurs Problem Details, version dans l’URL | Acceptée |
| [0022](0022-technology-stack.md) | Stack technique | Acceptée |
| [0023](0023-hold-idempotent-by-business-key.md) | Les blocages sont idempotents par clé métier (événement, utilisateur) | Acceptée |
| [0024](0024-booking-api-as-projection.md) | Réservation expose l’API ; son état sera alimenté par les événements de l’inventaire | Acceptée |
| [0025](0025-openapi-conventions.md) | OpenAPI 3.1.1, snake_case, ISO 8601 UTC ; lecture d’un blocage | Acceptée |
| [0026](0026-iteration-1-local-and-tooling.md) | Itération 1 en local et en CI seulement ; outillage ; une ADR par décision | Acceptée |
| [0027](0027-dependency-direction-inventory-depends-on-nothing.md) | Sens des dépendances : l’inventaire ne dépend d’aucun module | Acceptée |
| [0028](0028-sale-opening-enforced-by-inventory.md) | Ouverture de la vente contrôlée par l’inventaire, horloge PostgreSQL comme source du temps | Acceptée |
| [0029](0029-distinguish-sold-out-from-temporarily-unavailable.md) | Distinguer « complet » de « temporairement indisponible » dès l’itération 1 | Acceptée |
| [0030](0030-simulated-identity.md) | Identité simulée par jetons bearer signés localement | Acceptée |
| [0031](0031-hold-expiry-by-inventory-sweep.md) | Expiration des blocages par un balayage périodique de l’inventaire | Acceptée |
