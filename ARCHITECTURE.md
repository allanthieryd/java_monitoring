# Architecture technique — MaDemo

Ce document justifie les choix d'architecture et de technologies retenus pour le projet.

---

## 1. Vue d'ensemble

```
┌─────────────────────────────────────────────────────────────┐
│                        Client HTTP                          │
│               (Postman / navigateur / curl)                 │
└────────────────────────┬────────────────────────────────────┘
                         │ HTTP/REST
┌────────────────────────▼────────────────────────────────────┐
│                    Spring Boot 3.4.5                        │
│                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │RateLimitFilter│  │JwtAuthFilter │  │   Controllers    │  │
│  │  (Bucket4j)  │  │ (Spring Sec) │  │  /api/v1/**      │  │
│  └──────┬───────┘  └──────┬───────┘  └────────┬─────────┘  │
│         └─────────────────┴──────────────┬─────┘            │
│                                          │                   │
│                               ┌──────────▼──────────┐       │
│                               │      Services        │       │
│                               │ (logique métier +    │       │
│                               │  publication events) │       │
│                               └──────────┬───────────┘       │
│                   ┌───────────────────────┤                  │
│                   │                       │                  │
│         ┌─────────▼──────┐    ┌──────────▼──────────┐       │
│         │  Repositories  │    │   Event Listeners   │       │
│         │  (Spring Data) │    │  (async + sync)     │       │
│         └─────────┬──────┘    └─────────────────────┘       │
│                   │                                          │
└───────────────────┼──────────────────────────────────────────┘
                    │ JDBC / Hibernate
             ┌──────▼──────┐
             │   MySQL 8   │
             └─────────────┘
```

---

## 2. Organisation en domaines

Le code est structuré par domaine métier plutôt que par couche technique. Chaque domaine possède ses entités, son service et son contrôleur.

| Domaine     | Responsabilité                                           |
|-------------|----------------------------------------------------------|
| Identité    | Gestion des profils joueurs (`Profil`)                   |
| Matchmaking | Création et clôture des parties, calcul du rang          |
| Économie    | Wallet virtuel, transactions crédit/débit                |
| UGC         | Contenu communautaire (cycle de vie : draft → publié → archivé) |
| Modération  | Signalements, résolution admin, auto-archivage           |
| Leaderboard | Classement en lecture seule (top 10 par rankPoints)      |

Ce découpage facilite la séparation des responsabilités et rend chaque domaine testable indépendamment.

---

## 3. Sécurité

### JWT stateless — pourquoi pas les sessions HTTP ?

Spring Security propose par défaut une authentification par session (cookie `JSESSIONID`). Ce mode a été remplacé par JWT pour plusieurs raisons :

- **Scalabilité** : une session serveur nécessite un état partagé entre instances (sticky sessions ou Redis). Un token JWT est auto-portant et vérifié localement.
- **API REST** : le protocole HTTP est stateless par nature ; les sessions introduisent un état artificiel qui complique le déploiement.
- **Interopérabilité** : un client mobile ou une SPA peut stocker et transmettre un Bearer token sans gestion de cookie.

Le token est signé avec HMAC-SHA256 (`jjwt 0.12.6`), expire après 24h et contient le username et les rôles.

### Rôles

Deux rôles sont définis en mémoire (démo) :

- `ROLE_USER` : accès à tous les endpoints `/api/v1/**`
- `ROLE_ADMIN` : accès supplémentaire aux actions de modération (`resolve`, `reject`)

En production, les utilisateurs seraient stockés en base et les mots de passe hachés via BCrypt (déjà en place).

---

## 4. Rate Limiting

### Choix : Bucket4j (token bucket en mémoire)

Le rate limiting est appliqué par IP via un filtre Servlet (`OncePerRequestFilter`) placé avant la chaîne Spring Security.

Deux fenêtres distinctes :
- `/api/v1/auth/**` : 10 req/min — protection contre le brute-force de mots de passe
- Reste de l'API : 100 req/min — protection contre le scraping et les abus

**Pourquoi Bucket4j plutôt qu'un reverse proxy ?** Pour un projet monolithique, ajouter un gateway (Kong, Nginx) uniquement pour du rate limiting serait disproportionné. Bucket4j s'intègre directement comme filtre et ne nécessite aucune infrastructure supplémentaire.

**Limitation connue et évolution prévue** : le `ConcurrentHashMap` utilisé pour stocker les buckets est local au processus. Dans un déploiement multi-instances, les compteurs ne seraient pas partagés. La solution serait `bucket4j-redis` (Bucket4j + backend Redis), sans changer le code applicatif.

---

## 5. Événements domaine

### Découplage via Spring ApplicationEvent

Les services publient des événements métier via `ApplicationEventPublisher` au lieu d'appeler directement d'autres services. Exemple : quand un match se termine, `MatchmakingService` publie `MatchCompletedEvent` sans savoir ce qui en sera fait.

Événements implémentés :

| Événement                      | Publié par          | Consommé par                               |
|--------------------------------|---------------------|--------------------------------------------|
| `MatchCreatedEvent`            | MatchmakingService  | DomainEventLogger (async)                  |
| `MatchCompletedEvent`          | MatchmakingService  | DomainEventLogger (async)                  |
| `UgcPublishedEvent`            | UgcService          | DomainEventLogger (async)                  |
| `ModerationReportCreatedEvent` | ModerationService   | DomainEventLogger + ModerationEventListener |
| `WalletTransactionEvent`       | EconomyService      | DomainEventLogger (async)                  |

`ModerationEventListener` est synchrone et transactionnel : il auto-archive un contenu après 3 signalements dans la même transaction que le signalement.

**Pourquoi pas Kafka ici ?** Spring Events couvre la communication intra-service au sein d'un même processus. Kafka serait pertinent si les domaines étaient déployés en microservices distincts ou si la durabilité des messages entre redémarrages était requise.

---

## 6. Logging asynchrone

### Logback AsyncAppender + Virtual Threads (Java 21)

Les logs sont découplés des threads applicatifs via deux mécanismes complémentaires :

1. **Logback `AsyncAppender`** : les appels `log.info(...)` déposent un message dans une queue (512 slots) et rendent la main immédiatement. Un thread dédié écrit sur la console.
2. **`@Async` sur les listeners** : `DomainEventLogger` tourne sur un `TaskExecutor` basé sur `Executors.newVirtualThreadPerTaskExecutor()` (Java 21 Project Loom). Chaque invocation obtient un Virtual Thread léger sans bloquer un thread de plateforme.

**Impact** : sur des endpoints à forte charge, le logging synchrone peut ajouter plusieurs millisecondes de latence. L'approche async élimine ce coût du chemin critique HTTP.

---

## 7. Métriques et observabilité

### Micrometer + Prometheus + Grafana

- Micrometer est l'abstraction standard de Spring Boot pour les métriques (comparable à SLF4J pour les logs).
- Prometheus suit le modèle pull : il scrape `/actuator/prometheus` toutes les 15s, sans couplage réseau entre l'application et le monitoring.
- Grafana consomme Prometheus via une datasource provisionnée automatiquement au démarrage (fichier `grafana/provisioning/`).

### Métriques métier exposées

En plus des métriques techniques Spring (latence HTTP, pool de connexions, JVM), des métriques orientées expérience joueur ont été ajoutées via `PlayerExperienceMetrics` (`MeterBinder`) :

| Métrique                   | Type                | Formule                        | Interprétation                       |
|----------------------------|---------------------|--------------------------------|--------------------------------------|
| `match_abandonment_rate`   | Gauge               | CANCELED / total × 100        | Joueurs qui quittent les parties     |
| `match_completion_rate`    | Gauge               | COMPLETED / total × 100       | Santé du flux de jeu bout en bout    |
| `match_open_ratio`         | Gauge               | OPEN / total × 100            | Charge en temps réel                 |
| `match_duration_seconds`   | DistributionSummary | Durée entre startedAt / endedAt (p50/p95/p99) | Expérience joueur perçue |

**Distinction métriques infra vs métriques UX** : les métriques CPU/RAM/IOPS mesurent l'état de la machine. Les métriques ci-dessus mesurent ce que ressent le joueur — deux angles complémentaires pour diagnostiquer un incident.

---

## 8. Tests

### Stratégie

| Type | Outil | Scope |
|------|-------|-------|
| Tests unitaires services | JUnit 5 + Mockito | Logique métier isolée des I/O |
| Tests de contrat contrôleurs | `@WebMvcTest` + `@MockBean` | Sérialisation JSON, codes HTTP, sécurité |
| Test de démarrage | `@SpringBootTest` | Contexte Spring complet sur H2 |

### H2 pour les tests

La base H2 in-memory (profil `test`) permet des tests rapides sans Docker. Les migrations Hibernate (`ddl-auto=update`) s'appliquent automatiquement sur H2 au démarrage.

### `@WebMvcTest` + `@WithMockUser`

`@WebMvcTest` ne charge que la couche web (pas de JPA, pas de MySQL). `@WithMockUser` injecte un utilisateur authentifié dans le SecurityContext pour tester les endpoints protégés sans passer par le flux JWT complet. `spring-security-test` est déclaré explicitement en `<scope>test</scope>` dans le `pom.xml` car Spring Boot ne le résout pas automatiquement avec `@WebMvcTest`.

---

## 9. Ce qui serait fait en production

| Sujet | Solution dev actuelle | Évolution prod |
|-------|-----------------------|----------------|
| Utilisateurs | InMemoryUserDetailsManager | Table `users` + `UserDetailsService` JPA |
| Rate limiting multi-instances | ConcurrentHashMap local | `bucket4j-redis` (backend Redis distribué) |
| Refresh token | Non implémenté | Endpoint `/auth/refresh` + expiration courte (15min) |
| Communication inter-services | Spring Events (in-process) | Apache Kafka (persistance, multi-consumers) |
| Secret JWT | Propriété application | Variable d'environnement injectée par Vault / K8s Secret |
| Logs | Console async | Fichiers rotatifs + agrégation (ELK / Loki) |
