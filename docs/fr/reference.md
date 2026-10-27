# High-Demand Booking Platform — Référence projet

Oct 9, 2026 · révisé le 26 oct. 2026 (revue de fondation) · @Sami

> Traduction française de `reference.md`, qui fait foi (règle des langues, 26 oct. 2026).

## 1. Problème

Nous recevons des demandes de réservation massives et simultanées pour des ressources limitées, sur une période très courte. Le système doit absorber ce pic sans jamais vendre plus que la capacité, car une survente est irréversible, alors qu'une attente reste acceptable si l'utilisateur sait où il en est.

**Scénario de référence** : concert de 40 000 places, ouverture de la vente samedi à 10h00, 1,5 million de personnes connectées.

### Risques identifiés

- **Intégrité** : survente, double attribution, incohérence des données.
- **Charge** : saturation des threads et de la base ; l'effet F5 amplifie le pic.
- **Requêtes rejouées** : timeout ambigu, retries clients, réseau mobile.
- **Places fantômes** : places bloquées par des utilisateurs qui ne paient jamais.
- **Effet domino** : un événement saturé ne doit pas dégrader les autres.
- **Trafic malveillant** : bots et revendeurs (hors MVP).

### Critères de succès

- **Zéro survente** : à tout instant, places bloquées + réservées ≤ capacité. Vérifié automatiquement après chaque test de charge.
- **Visibilité** : la position est rafraîchie selon R13.
- **Réactivité** : blocage d'une place confirmé en moins de 100 ms au p99, hors paiement.
- **Durabilité** : une réservation confirmée survit à un crash.

Principe directeur : perdre parce que la demande dépasse l'offre est normal ; perdre injustement ou sans le savoir est un échec du système.

## 2. Acteurs

Acteurs qui interagissent avec le système, y compris non humains et malveillants (hors MVP).

| Acteur | Veut | Peut | Échec pour lui |
| --- | --- | --- | --- |
| Participant | Obtenir une place | Consulter, entrer dans la file, bloquer, payer | Perdre injustement ou sans le savoir |
| Organisateur | Vendre ses places sans incident | Créer un événement, fixer capacité et fenêtre de vente, voir ses statistiques (après MVP) | Survente, vente effondrée, perte de crédibilité |
| Administrateur | Des ventes sans incident | MVP : tableau de bord en lecture seule. Après MVP : pause, ralentissement | Être aveugle ou impuissant pendant un incident |
| Planificateur | — | Publier les événements temporels : ouverture, clôture | Déclenchement manqué ou en double |
| Prestataire de paiement (simulé) | — | Traiter un paiement, envoyer un webhook | — |
| Service de notification | — | Envoyer email ou push | — |
| Revendeur, bots (hors MVP) | Accaparer des places | Comptes multiples, onglets multiples, automatisation | — |

Une file d'attente seule n'arrête pas un revendeur : il obtient une position par compte. Le blocage par IP pénalise des utilisateurs légitimes qui partagent une adresse.

## 3. Périmètre du MVP

Le MVP couvre un parcours complet en placement libre, une place par utilisateur, avec paiement simulé.

### Parcours participant

1. Consulte la liste des événements en vente.
2. Ouvre la page d'un événement et demande une place.
3. Entre dans la salle d'attente, puis dans la file ; voit sa position, les places restantes et ses chances.
4. Est admis : une place est bloquée, un compte à rebours de 5 minutes démarre.
5. Paie (paiement simulé).
6. Reçoit la confirmation de réservation.

### Chemins malheureux

- **Plus de place pour l'instant** : l'utilisateur reste informé ; il n'est renvoyé que lorsque l'événement est définitivement complet.
- **Blocage expiré avant paiement** : la place est libérée ; l'utilisateur est informé et peut revenir en fin de file.
- **Paiement échoué** : relance possible dans le temps restant, sans prolongation.
- **Paiement tardif** : période de grâce, puis remboursement automatique.

### Fonctionnalités du MVP

- Catalogue : liste et détail des événements.
- Salle d'attente et file, toujours actives (D-005) ; position, places restantes, chances.
- Blocage temporaire, compte à rebours, expiration automatique.
- Paiement simulé réaliste : latence variable, échecs, timeouts, webhooks asynchrones parfois en double.
- Confirmation affichée et notification simulée, asynchrone.
- Idempotence des opérations de file, de réservation et de paiement.
- Identité minimale : utilisateurs de test, jeton signé (D-007).
- Admin : création d'événement par API, tableau de bord en lecture seule.
- Observabilité : métriques et logs du déroulement d'une vente.
- Test de charge reproductible, avec vérification automatique de l'invariant.

### Après le MVP

Plusieurs places par utilisateur · modification d'un événement (dates immuables dans l'itération 1, D-028) · actions admin en direct · interface organisateur et analytics · désactivation de la file · places numérotées, catégories, prix · vrai prestataire de paiement · protection contre les bots et revendeurs · vraie authentification.

### Hors projet pour l'instant

Revente entre particuliers · remboursements à la demande · multi-tenant.

## 4. Règles métier

Chaque règle doit pouvoir devenir un test automatisé.

| Id | Sujet | Règle |
| --- | --- | --- |
| R1 | Salle d'attente | Ouverte 30 min avant la vente (paramétrable), sans limite de capacité. Avant son ouverture et après la clôture de la vente : consultation seulement. |
| R2 | Équité | À l'ouverture de la vente, les présents reçoivent un rang tiré au sort ; les arrivants suivants sont placés en FIFO derrière eux. |
| R2b | Information | Chaque participant voit sa position, les places restantes et une estimation de ses chances ; il peut quitter la file à tout moment. |
| R3 | Unicité | Un utilisateur a au plus un blocage actif OU une réservation par événement, y compris sous requêtes concurrentes. |
| R4 | Idempotence de la file | Entrer dans la file est idempotent par (utilisateur, événement) : une nouvelle demande renvoie la position existante. |
| R5 | Blocage | À l'admission, l'Inventaire bloque une place pendant 5 min (paramétrable), avec compte à rebours. |
| R6 | Relance | Après un échec de paiement, relance possible dans le temps restant, sans prolongation. |
| R7 | Grâce | Si un paiement est en cours à l'expiration, la place reste bloquée 1 min de plus (paramétrable). |
| R8 | Paiement tardif | Un paiement confirmé après libération de la place déclenche un remboursement automatique et idempotent. |
| R9 | Admission | Le participant suivant est admis si une place est disponible ET si le débit d'admission, calé sur le débit mesuré de l'Inventaire, n'est pas dépassé. Une place redevient disponible quand un blocage expire. |
| R10 | Complet | « Plus de place pour l'instant » tant que des blocages sont actifs ; « complet » seulement quand toutes les places sont réservées. |
| R11 | Capacité | Augmentation autorisée pendant la vente ; réduction non supportée dans le MVP (plus tard : limitée aux places ni bloquées ni réservées). |
| R12 | Invariant | À tout instant, places bloquées + réservées ≤ capacité. |
| R13 | Rafraîchissement | Intervalle imposé par le serveur selon l'attente estimée : < 2 min → 5 s ; 2 à 15 min → 15 s ; > 15 min → 60 s. |
| R14 | Panne paiement : détection | Détection automatique par circuit breaker sur la passerelle de paiement. |
| R15 | Panne paiement : admissions | Pendant la panne, les admissions sont suspendues. |
| R16 | Panne paiement : blocages | Les comptes à rebours actifs sont gelés pendant la panne (exception assumée à R6). |
| R17 | Panne paiement : reprise | Participants et administrateur sont informés ; admissions et comptes à rebours repartent automatiquement à la reprise. |

R7 couvre un paiement lent côté utilisateur ; R16 couvre une panne de notre fait.

## 5. Estimations de charge

Les lectures de position dominent la charge d'un facteur 100 environ, mais le chemin critique est un problème de contention, pas de volume.

| Opération | Pic (scénario de référence) | Nature du problème |
| --- | --- | --- |
| Consultation de la position, fixe à 10 s | \~150 000 req/s | Volume, tolérance à l'imprécision |
| Consultation de la position, avec R13 | \~60 000 req/s, en majorité absorbées par le cache et le CDN (D-014) | Volume |
| Entrées dans la salle (pire cas : 750 000 en 2 min) | \~6 000 req/s | Volume, en écriture |
| Blocages, admission à 500/s | 500 écritures/s sur un même compteur | Contention, cohérence |
| Paiements | \~130 req/s | Faible |

Durée de la vente, sans limite de blocages : \~98 % des places vendues en 12 min, traîne jusqu'à 25 à 30 min (30 % d'expirations par vague). Première vague de 40 000 admissions à 500/s : \~80 s.

Le débit de 500 blocages/s est une hypothèse à mesurer par le test de charge.

## 6. Architecture

Trois déployables (D-017) : un monolithe modulaire, la Vue de position et le simulateur de paiement.

&#91;embedded content: architecture du MVP · 3 déployables\]

Les lectures de position contournent le monolithe ; seul le curseur d'admission leur parvient. L'Inventaire, en surbrillance, détient les blocages et les réservations.

### Communications entre composants

| Interaction | Mode | Pourquoi |
| --- | --- | --- |
| Admission → Inventaire : bloquer une place | Synchrone | L'admission a besoin du résultat pour continuer. Orchestration entre Admission, Réservation et Inventaire à concevoir (section 8) |
| Inventaire → Réservation : blocage créé, confirmé, expiré | Asynchrone, via outbox (D-013) | Réservation doit seulement le savoir, de façon fiable ; aucun consommateur dans l'itération 1 (D-024) |
| Réservation → Paiement : lancer un paiement | Synchrone, idempotent (D-015) | Il faut l'URL de paiement immédiatement |
| Paiement → Réservation : résultat | Asynchrone : webhook acquitté, puis traité | Le prestataire répond plus tard, parfois en double |
| Réservation → Notification | Asynchrone (D-011) | Hors du chemin critique |
| Admission → Vue de position : curseur | Asynchrone (D-014) | La lecture tolère un léger retard |
| Planificateur → File : vente ouverte | Asynchrone, idempotent (D-012) | Un déclenchement en double reste sans effet |

## 7. Journal des décisions

Les décisions sont réparties en cinq tableaux, de la plus récente à la plus ancienne. Ce premier tableau regroupe les décisions d'architecture. Chaque décision a son ADR dans `docs/decisions/`. Une ADR acceptée n'est pas réécrite : un changement fait l'objet d'une nouvelle ADR qui l'amende (changement partiel) ou la remplace (changement total). Cette règle s'applique à partir du premier commit des ADR (ticket 1).

| Id | Décision | Pourquoi | Conséquences acceptées |
| --- | --- | --- | --- |
| D-017 | Monolithe modulaire + deux déployables séparés : Vue de position et simulateur de paiement | Le chemin de lecture exige des ressources de calcul séparées ; le simulateur doit reproduire un vrai appel réseau. Les autres isolations se règlent par bulkheads | Un déploiement redémarre tous les modules. Première extraction pédagogique : Notification. Signal réel : correctif nécessaire pendant une vente |
| D-016 | Inventaire en trois étapes : Postgres mise à jour conditionnelle + CHECK (MVP) → une ligne par place + SKIP LOCKED → Redis atomique | La file limite le débit d'arrivée ; l'approche 1 suffit avec une durabilité forte | Débit plafonné par la base. Signaux : places numérotées ou première vague > 2 min (vers 2) ; capacité dépassée par plusieurs ventes (vers 3). Contrat stable, implémentations interchangeables |
| D-015 | Appels au paiement idempotents (même clé au retry), réconciliation, webhooks acquittés puis traités de façon idempotente | Un timeout ne dit pas si le paiement a été créé | Logique de réconciliation à maintenir |
| D-014 | Position = rang − curseur d'admission ; un seul curseur par événement, servi par cache et CDN | Le rang est fixe après le tirage ; seul le curseur change | Position pessimiste (abandons non déduits) ; légère latence de propagation |
| D-013 | Publication des événements par transactional outbox | Évite la double écriture base + broker | Livraison au moins une fois, consommateurs idempotents ; relais fourni par le registre Spring Modulith (D-020) ; équivalent à trouver si l'Inventaire passe sur Redis |
| D-012 | Le Planificateur publie le « quand » ; chaque composant exécute le « quoi » de façon idempotente | Concentre la gestion du temps et des instances multiples | Un composant de plus ; il déclenche les moments métier, les expirations restent au module propriétaire de la donnée (D-031) |
| D-011 | Notification asynchrone, hors du chemin critique, avec retries | Un composant non critique ne doit jamais faire échouer un composant critique | Confirmation par email différée |
| D-010 | Le blocage appartient à l'Inventaire ; conversion atomique « blocage → réservation si encore actif » | Une seule source de vérité ; la course expiration / paiement est tranchée par une opération | Réservation dépend des événements de l'Inventaire, son statut peut être brièvement en retard |
| D-009 | Séparation du chemin de lecture (position) et du chemin de réservation (CQRS) | \~150 000 lectures/s (rafraîchissement fixe à 10 s ; \~60 000 avec R13) tolérantes à l'erreur contre quelques milliers d'écritures/s intolérantes | Position légèrement décalée ; deux modèles à synchroniser |
| D-008 | Expiration visible et libération réelle découplées (période de grâce) | Réduire la fréquence du paiement tardif plutôt que seulement le traiter | Place immobilisée 1 min de plus |
| D-007 | Identité minimale : utilisateurs de test et jeton signé | L'authentification n'est pas un objectif d'apprentissage et gênerait les tests de charge | Composant à remplacer avant toute exposition réelle |
| D-006 | Tirage au sort à l'ouverture, puis FIFO | L'ordre ne dépend plus des millisecondes ; les connexions s'étalent | Arriver très tôt ne donne pas d'avantage ; tirage atomique à l'ouverture |
| D-005 | File d'attente toujours active dans le MVP | La file garantit l'ordre, pas seulement la tenue en charge ; un seul chemin | Étape instantanée mais inutile pour les événements peu demandés |
| D-004 | Langage du domaine concret (Event, Hold, Booking) ; seul l'inventaire est générique | Généraliser sans deuxième cas d'usage, c'est deviner | Renommages possibles plus tard |
| D-003 | Le serveur suppose que les retries arriveront : clé d'idempotence générée par le client | Le client n'est pas contrôlable | Clés à stocker |
| D-002 | Intégrité avant disponibilité sur le chemin de réservation | Une indisponibilité est récupérable, une survente ne l'est pas | Le système ralentit ou refuse plutôt que de risquer une survente |
| D-001 | L'apprentissage prime sur tout objectif commercial | Objectif du projet | Complexité supérieure au besoin réel, chaque ajout lié à un objectif d'apprentissage |

### Décisions de fonctionnement

Décisions sur la façon de travailler et la stack, prises avant la première ligne de code.

| Id | Décision | Pourquoi | Conséquences acceptées |
| --- | --- | --- | --- |
| D-022 | Stack : Java 25 (LTS), Spring Boot 4.1.x et la version de Spring Modulith compatible, Maven, Flyway, Postgres 18 (même version majeure en local, en CI et dans le cloud) | Versions supportées ; outils familiers là où la nouveauté n'apporte rien | Migrations Flyway en avant uniquement ; threads virtuels : le pool de connexions devient la limite, à protéger explicitement |
| D-021 | API contract-first : contrat OpenAPI écrit et validé avant l'implémentation ; erreurs au format Problem Details (RFC 9457) ; version dans l'URL (/v1) | L'API est conçue avant d'être codée ; Claude Code implémente un contrat validé | Contrat et code à garder synchronisés |
| D-020 | Spring Modulith pour faire respecter les frontières de modules, et son registre d'événements comme outbox | Frontières vérifiées automatiquement à chaque build ; outbox fournie (D-013) | Une dépendance de plus et ses conventions |
| D-019 | Kanban sur GitHub Issues + Projects, limite de 2 tickets en cours ; numéro de ticket dans chaque branche et commit | Travail en solo sans sprints ; tickets liés aux commits et pull requests | Discipline de mise à jour du tableau |
| D-018 | Trunk-based development : une branche principale, branches courtes par ticket, fusion par pull request ; le même artefact est promu d'un environnement à l'autre | Moins de divergence, ce qui est déployé est ce qui a été testé | La branche principale doit toujours rester déployable |

### Décisions de l'itération 1

Décisions issues de la revue des flux.

| Id | Décision | Pourquoi | Conséquences acceptées |
| --- | --- | --- | --- |
| D-024 | L'API de réservation reste exposée par Réservation, façade et futur orchestrateur ; son état est entièrement alimenté par les événements de l'Inventaire (HoldCreated, HoldConfirmed, HoldExpired) | Le contrat public reste stable quand l'Admission et le paiement arriveront ; l'Inventaire reste une capacité interne et générique (D-004) ; plus de double écriture dans le flux A | Dans l'itération 1, Réservation est une façade sans état : toutes ses opérations, lectures comprises, appellent l'Inventaire ; la projection est reportée à l'arrivée du paiement |
| D-023 | Blocage idempotent par clé métier (événement, utilisateur) plutôt que par clé d'idempotence ; une requête rejouée reçoit le blocage actif existant | L'index unique sur (événement, utilisateur) garantit déjà l'unicité ; une clé technique serait redondante | Une deuxième requête du même utilisateur, retry ou autre onglet, reçoit le même blocage. La clé redeviendra nécessaire avec plusieurs places par utilisateur. D-003 reste le principe, le mécanisme est choisi par opération |

### Décisions de fondation

Décisions qui closent la phase de fondation, issues de la revue des tickets de l'itération 1 et de la revue de fondation.

| Id | Décision | Pourquoi | Conséquences acceptées |
| --- | --- | --- | --- |
| D-031 | Expiration des blocages par balayage périodique de l'Inventaire, au-delà de l'itération 1. Le Planificateur garde les moments métier (ouverture de la salle, ouverture et clôture de la vente) | Jusqu'à \~40 000 expirations par vente, sur une donnée que possède l'Inventaire ; la justesse ne dépend pas du balayage (tout contrôle applique `expires_at <= now()`) | Une place libérée revient avec au plus un intervalle de balayage de retard ; taille de lot et intervalle à régler |
| D-030 | Identité simulée : le contrat garde le schéma Bearer ; un script génère des jetons signés pour N utilisateurs de test et un administrateur ; Spring Security les valide avec une clé locale | R3 et les tests de charge exigent des identités distinctes issues du jeton, sans faire de l'authentification un objectif (D-007) | L'émission de jetons n'existe jamais dans le profil de production ; composant à remplacer avant toute exposition réelle |
| D-029 | R10 dès l'itération 1 : compteur `confirmed` sur la ligne du stock, mis à jour dans la transaction de confirmation. Échec du décrément : `confirmed = capacity` → `/problems/event-sold-out` (définitif) ; sinon `/problems/seats-temporarily-unavailable` avec `Retry-After: 30` | Sans file, le retry est le seul recours du client : il doit savoir s'il vaut la peine de réessayer | Un blocage expiré non balayé prolonge l'état « temporaire » ; `Retry-After` n'est qu'une indication, la vraie protection viendra du rate limiting et de la file ; modification du contrat |
| D-028 | Ouverture de la vente : l'Inventaire stocke `opens_at` et refuse tout blocage tant que `now() < opens_at`, dans sa mise à jour conditionnelle. Dates d'un événement immuables dans l'itération 1 (pas de report). Source de vérité du temps : l'horloge PostgreSQL (`now()`, début de transaction ; `clock_timestamp()` seulement si un besoin apparaît) | Contrôle atomique avec le décrément, sans appel synchrone ; une seule horloge pour toutes les instances | `opens_at` existe dans deux modules, cohérent grâce à l'immuabilité ; tests : on déplace la donnée, pas le temps ; à partir de l'itération 2, défense en profondeur derrière l'Admission ; toute comparaison de temps utilise l'horloge de la base |
| D-027 | Sens des dépendances : l'Inventaire, composant générique (D-004), ne dépend d'aucun module. `remaining_seats` est retiré de `GET /v1/events/{id}` dans l'itération 1 (contrat 1.2.0) et reporté au chemin de lecture. Événement publie `EventCreated` et l'écoute lui-même (listener après commit, rejoué par le registre Modulith) pour appeler `inventory.createStock(eventId, capacity, opensAt, holdDuration)`, idempotent. Sans stock : un blocage reçoit UNKNOWN puis `404 event-not-found` ; `sale_start_time` doit dépasser `now()` d'une marge paramétrable | L'Inventaire reste générique : il ne change que pour ses propres besoins ; plus de cycle Événement ↔ Inventaire | Graphe : `event → inventory` (création du stock), `booking → inventory` ; fenêtre sans stock, toujours avant l'ouverture ; métrique et alerte sur les publications incomplètes du registre ; événement auto-consommé à documenter ; pas de places restantes visibles dans l'itération 1 |

### Décisions de setup

| Id | Décision | Pourquoi | Conséquences acceptées |
| --- | --- | --- | --- |
| D-026 | Itération 1 entièrement en local et en CI ; outillage : k6 pour la charge, Prometheus + Grafana pour l'observabilité locale, google-java-format via Spotless imposé en CI ; décisions en un fichier ADR par décision dans `docs/decisions/` | L'objectif de l'itération se prouve sans cloud ; outils gratuits et standards ; format stable malgré le code généré par l'IA ; ADR lisibles par un recruteur | Pas de mesure en environnement cloud pendant l'itération 1 ; plus de fichiers à maintenir |
| D-025 | Contrat OpenAPI 3.1.1, propriétés en snake\_case, dates ISO 8601 UTC ; ajout de `GET /v1/holds/{holdId}` | Version récente au support outillé mature ; le client doit pouvoir restaurer son compte à rebours | Configuration Jackson explicite ; support 3.1 du générateur à vérifier |

## 7b. Itération 1 : flux entre modules

Périmètre : un événement créé par l'administrateur, des utilisateurs de test avec jeton, pas de file ni de paiement. Un utilisateur bloque une place, puis la confirme avant expiration.

### Flux 0 : création de l'événement (D-027)

1. Admin → Événement : `POST /v1/events` avec capacité, ouverture de la vente et durée de blocage · synchrone. L'ouverture doit dépasser `now()` d'une marge paramétrable, supérieure au délai de relance du registre. Une transaction : insertion de l'événement et `EventCreated` dans l'outbox.
2. Événement → Événement : un listener après commit du module Événement reçoit `EventCreated`, rejoué par le registre Modulith en cas d'échec · asynchrone.
3. Événement → Inventaire : `createStock(événement, capacité, opensAt, duréeBlocage)` · synchrone, idempotent. L'Inventaire crée la ligne de stock (événement, capacité, places disponibles, confirmées = 0, `opens_at`, durée de blocage).
4. Fenêtre sans stock (quelques millisecondes, toujours avant l'ouverture grâce à la marge) : un blocage reçoit UNKNOWN de l'Inventaire et Réservation renvoie `404 event-not-found` (D-027). `GET /v1/events/{id}` est servi par Événement seul, sans places restantes. Une publication restée incomplète déclenche une alerte.
5. Les dates de l'événement sont immuables dans l'itération 1 (D-028).

### Flux A : bloquer une place

1. Client → Réservation : `POST /v1/events/{id}/holds` avec le jeton · synchrone.
2. Réservation → Inventaire : `bloquer(événement, utilisateur)` · synchrone, en mémoire.
3. Inventaire, une transaction : si l'utilisateur a un blocage actif non expiré, il est renvoyé tel quel ; s'il a un blocage actif mais expiré, celui-ci est d'abord expiré (mise à jour conditionnelle, réincrément) ; sinon, décrément conditionnel du compteur (`available > 0` et `now() >= opens_at`, D-028), insertion du blocage (`expires_at = now()` + durée de blocage du stock), `HoldCreated` dans l'outbox. Si le décrément échoue, la ligne de stock tranche : vente non ouverte, complet (`confirmed = capacity`) ou temporairement indisponible (D-029).
4. Réponse : `201` nouveau blocage · `200` blocage existant · `404` événement inconnu ou stock pas encore créé · `409` complet, définitif · `409` places temporairement indisponibles, avec `Retry-After: 30` · `409` déjà réservé · `409` vente non ouverte.
5. `HoldCreated` est publié ; aucun consommateur dans l'itération 1 (D-024).

### Flux B : confirmer

1. Client → Réservation : `POST /v1/holds/{id}/confirmations` · synchrone.
2. Réservation → Inventaire : `confirmer(idBlocage, utilisateur)`.
3. Inventaire, une transaction : mise à jour conditionnelle (actif, non expiré selon `now()`, même utilisateur), incrément du compteur `confirmed` de la ligne de stock (D-029), `HoldConfirmed` dans l'outbox.
4. Réponse : `200` confirmé, y compris en cas de double clic · `409` expiré · `404` introuvable ou appartenant à un autre utilisateur.
5. `HoldConfirmed` est publié ; aucun consommateur dans l'itération 1.
6. Lecture : `GET /v1/holds/{id}` → Réservation → `lireBlocage(idBlocage, utilisateur)` de l'Inventaire ; statut `EXPIRED` dès que `expires_at <= now()`, sans retard après une confirmation.

### Flux C : expirer

1. Scheduler de l'Inventaire : sélection d'un lot de blocages actifs expirés (`FOR UPDATE SKIP LOCKED`).
2. Par blocage, une transaction : mise à jour conditionnelle « actif → expiré » ; si elle réussit, réincrément du compteur et `HoldExpired` dans l'outbox.
3. `HoldExpired` est publié ; aucun consommateur dans l'itération 1. Le balayage reste à l'Inventaire au-delà de l'itération 1 (D-031).

### Interface publique de l'Inventaire

| Élément | Signature |
| --- | --- |
| Opération | `createStock(événement, capacité, opensAt, duréeBlocage)` → idempotent (D-027) |
| Opération | `bloquer(événement, utilisateur)` → Bloqué {id, expiration, nouveau ou existant} · Complet · TemporairementIndisponible · DéjàRéservé · NonOuvert · Inconnu |
| Opération | `confirmer(idBlocage, utilisateur)` → Confirmé · Expiré · Introuvable |
| Opération | `lireBlocage(idBlocage, utilisateur)` → Blocage {id, événement, statut, expiration, confirmation} · Introuvable |
| Événements publiés | `HoldCreated`, `HoldConfirmed`, `HoldExpired` |
| Événement écouté | aucun (D-027) |

Dépendances (D-027) : Événement → Inventaire (création du stock) et Réservation → Inventaire, par appels. Personne n'écoute les événements de l'Inventaire dans l'itération 1. Le module Utilisateur valide seulement les jetons simulés (D-030) ; l'utilisateur courant vient du contexte de sécurité. L'Inventaire ne dépend d'aucun module. Aucun cycle. Pas d'opération publique de libération.

## 8. Questions ouvertes

- [ ] Combien de temps conserver les clés d'idempotence ?
- [ ] Que faire d'un retry reçu pendant que la requête initiale est encore en cours ?
- [x] Quelle source de vérité pour l'heure entre plusieurs instances du Planificateur ? → l'horloge PostgreSQL (D-028).
- [ ] Comment exécuter le tirage au sort atomiquement sur un grand nombre d'entrées ?
- [ ] Comment détecter les abandons dans la file pour affiner la position et les chances ?
- [x] Comment implémenter les expirations de blocage : balayage périodique ou messages différés ? → balayage par l'Inventaire (D-031).
- [ ] Quel broker pour les événements entre modules et vers la Vue de position ?
- [ ] Débit réel de l'Inventaire Postgres, à mesurer (hypothèse : 500 blocages/s).
- [ ] Places restantes : qui les sert, à quel débit et avec quelle fraîcheur, sur le chemin de lecture (reporté par D-027) ?
- [ ] Clôture de la vente et événement passé (`event_date`) : quel contrôle, et où (R1) ?
- [ ] Orchestration à l'itération 2 : l'Admission appelle-t-elle Réservation ou l'Inventaire ?
- [ ] Démarrage manuel de la vente par l'administrateur : besoin réel (démo, test, incident) ? Lié aux actions admin en direct, après le MVP.
