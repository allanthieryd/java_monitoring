# MaDemo — Plateforme backend de jeu compétitif

Backend Spring Boot simulant une plateforme de jeu compétitif : profils joueurs, matchmaking, économie virtuelle, contenu communautaire (UGC), modération et observabilité complète.

## Stack technique

- Java 21 + Spring Boot 3.4.5
- MySQL 8 (persistance), H2 (tests)
- Spring Security + JWT (authentification stateless)
- Bucket4j (rate limiting)
- Micrometer + Prometheus + Grafana (observabilité)
- Apache JMeter (tests de charge)
- Docker Compose (stack complète)

---

## 1. Lancement

Depuis le dossier `MaDemo/` (là où se trouve `docker-compose.yml`) :

```bash
docker compose up --build
```

| Service    | URL                          |
|------------|------------------------------|
| API        | http://localhost:8081        |
| Prometheus | http://localhost:9090        |
| Grafana    | http://localhost:3005        |
| MySQL      | localhost:3307               |

> L'application Spring Boot tourne dans le conteneur `mademo-app`. Il n'est pas nécessaire de la lancer séparément depuis IntelliJ sauf pour du débogage.

---

## 2. Comptes disponibles

| Rôle  | Username | Password |
|-------|----------|----------|
| USER  | allan    | 7895     |
| ADMIN | admin    | admin123 |
| Grafana | admin  | admin123 |

---

## 3. Authentification

Tous les endpoints `/api/v1/**` requièrent un token JWT. Le flux est :

### 3.1 Obtenir un token

```bash
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"allan","password":"7895"}'
```

Réponse :
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "allan",
  "roles": ["ROLE_USER"]
}
```

### 3.2 Utiliser le token

Ajouter le header `Authorization: Bearer <token>` à chaque requête :

```bash
curl http://localhost:8081/api/v1/profils \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

Le token expire après 24h (`jwt.expiration=86400000` ms).

---

## 4. Endpoints

Tous les endpoints sont préfixés `/api/v1/`. Les actions de modération (`resolve`, `reject`) sont réservées au rôle `ADMIN`.

### Profils

| Méthode | Endpoint              | Description                        |
|---------|-----------------------|------------------------------------|
| GET     | `/api/v1/profils`     | Liste tous les profils             |
| GET     | `/api/v1/profils/{id}`| Détail d'un profil                 |
| POST    | `/api/v1/profils`     | Créer un profil                    |
| PUT     | `/api/v1/profils/{id}`| Mettre à jour un profil            |
| DELETE  | `/api/v1/profils/{id}`| Supprimer un profil                |

```json
POST /api/v1/profils
{ "username": "player1", "email": "player1@mail.com" }
```

### Matchmaking

| Méthode | Endpoint                        | Description                        |
|---------|---------------------------------|------------------------------------|
| GET     | `/api/v1/matches`               | Liste des 50 derniers matchs       |
| GET     | `/api/v1/matches/{id}`          | Détail d'un match                  |
| POST    | `/api/v1/matches`               | Créer un match                     |
| POST    | `/api/v1/matches/{id}/complete` | Clôturer un match (déclare vainqueur) |

```json
POST /api/v1/matches
{ "playerOneId": 1, "playerTwoId": 2 }

POST /api/v1/matches/{id}/complete
{ "winnerId": 1 }
```

La clôture met à jour les rankPoints : +25 pour le vainqueur, -20 pour le perdant (min 0).

### Économie

| Méthode | Endpoint                        | Description                        |
|---------|---------------------------------|------------------------------------|
| GET     | `/api/v1/economy/{profilId}/transactions` | Historique du wallet    |
| POST    | `/api/v1/economy/{profilId}/credit`       | Créditer un compte      |
| POST    | `/api/v1/economy/{profilId}/debit`        | Débiter un compte       |

```json
POST /api/v1/economy/1/credit
{ "amount": 100.00, "description": "Récompense victoire" }
```

### UGC (User Generated Content)

| Méthode | Endpoint                        | Description                        |
|---------|---------------------------------|------------------------------------|
| GET     | `/api/v1/ugc`                   | Liste les contenus publiés         |
| POST    | `/api/v1/ugc`                   | Créer un contenu (statut DRAFT)    |
| POST    | `/api/v1/ugc/{id}/publish`      | Publier un contenu                 |
| POST    | `/api/v1/ugc/{id}/archive`      | Archiver un contenu                |

```json
POST /api/v1/ugc
{ "title": "Mon guide", "body": "Contenu...", "authorId": 1 }
```

### Modération

| Méthode | Endpoint                                    | Description                        |
|---------|---------------------------------------------|------------------------------------|
| GET     | `/api/v1/moderation/reports`                | Liste tous les signalements        |
| POST    | `/api/v1/moderation/reports`                | Créer un signalement               |
| POST    | `/api/v1/moderation/reports/{id}/resolve`   | Résoudre (ADMIN uniquement)        |
| POST    | `/api/v1/moderation/reports/{id}/reject`    | Rejeter (ADMIN uniquement)         |

```json
POST /api/v1/moderation/reports
{ "contentId": 1, "reporterId": 2, "reason": "Contenu inapproprié" }
```

> Un contenu signalé 3 fois ou plus est automatiquement archivé (règle métier via événement domaine).

### Leaderboard

| Méthode | Endpoint                  | Description                          |
|---------|---------------------------|--------------------------------------|
| GET     | `/api/v1/leaderboard/top` | Top 10 joueurs par rankPoints        |

### Monitoring

| Endpoint               | Description                         | Auth requise |
|------------------------|-------------------------------------|--------------|
| `/actuator/health`     | Santé de l'application              | Non          |
| `/actuator/prometheus` | Métriques Prometheus                | Non          |
| `/actuator/metrics`    | Métriques Micrometer                | Non          |

---

## 5. Rate Limiting

Le filtre de rate limiting est appliqué par IP :

| Cible                  | Limite       |
|------------------------|--------------|
| `/api/v1/auth/**`      | 10 req/min   |
| Tous les autres `/api/v1/**` | 100 req/min |

Dépassement → HTTP `429 Too Many Requests` :
```json
{
  "status": 429,
  "error": "Too Many Requests",
  "message": "Limite de requetes atteinte. Reessayez dans une minute."
}
```

Ce comportement est vérifié automatiquement par le plan JMeter `mademo-ratelimit-test.jmx` (cf. section 9).

---

## 6. Observabilité (Grafana)

Ouvrir http://localhost:3005 (admin / admin123) → dashboard **MaDemo Monitoring**.

Panels disponibles :
- Débit API (req/s) et latence p95
- Erreurs 5xx/s
- Compteurs de matchs créés / complétés
- Parties en cours (jauge temps réel)
- Taux d'abandon, taux de complétion (métriques UX joueur)
- Durée des matchs (p50 / p95 / p99)

Pour alimenter les métriques, créer quelques profils, matchs et les clôturer via l'API ou Postman.

---

## 7. Collection Postman

Importer le fichier :
```
postman/MaDemo-Platform.postman_collection.json
```

Variables préconfigurées : `baseUrl`, `username`, `password`, `adminUsername`, `adminPassword`. La collection inclut un script de login automatique qui stocke le token dans la variable `token`.

---

## 8. Tests

```bash
./mvnw test
```

Les tests tournent sur H2 en mémoire (profil `test`). Couverture :
- Tests unitaires services : `MatchmakingServiceTest`, `EconomyServiceTest`, `ProfilServiceTest`
- Tests de contrat contrôleurs : `ProfilControllerTest`, `MatchControllerTest`, `AuthControllerTest`

---

## 9. Tests de charge (JMeter)

La stack doit tourner. Le service `jmeter` est sous le profil `load-test` : il ne démarre pas avec `docker compose up`, on le lance à la demande.

```bash
# Test de charge : 20 joueurs simultanés sur le parcours complet
docker compose --profile load-test run --rm jmeter

# Validation du rate limiting : prouve le 429 au-delà de 100 req/min
PLAN=mademo-ratelimit-test.jmx docker compose --profile load-test run --rm jmeter

# Charge plus élevée
docker compose --profile load-test run --rm jmeter -Jthreads=50 -Jloops=20
```

Rapport HTML généré à chaque exécution :
```bash
open MaDemo/jmeter/results/mademo-load-test-*/html-report/index.html
```

Pendant un run, l'effet est visible en direct dans Grafana — c'est l'intérêt de brancher JMeter sur une stack déjà instrumentée.

Résultats de référence (20 joueurs × 10 itérations, 1220 requêtes, 0 erreur) : latence moyenne de 5 à 11 ms sur les endpoints API, 88 ms sur `/auth/login` (coût de BCrypt).

> Détail des scénarios, des paramètres et des choix de conception : [`MaDemo/jmeter/README.md`](MaDemo/jmeter/README.md).

---

## 10. Variables d'environnement

Les valeurs par défaut fonctionnent pour un lancement local. En production, surcharger :

| Variable                  | Défaut                                  | Description              |
|---------------------------|-----------------------------------------|--------------------------|
| `SPRING_DATASOURCE_URL`   | `jdbc:mysql://localhost:3307/...`       | URL MySQL                |
| `SPRING_DATASOURCE_USERNAME` | `root`                               | Utilisateur MySQL        |
| `SPRING_DATASOURCE_PASSWORD` | `root`                               | Mot de passe MySQL       |
| `JWT_SECRET`              | clé embarquée (dev uniquement)          | Secret HS256 (min 32 car)|
