# MaDemo - Plateforme backend complete

Ce projet implemente une plateforme backend de tracking de jeu avec:

- Gestion des profils joueurs (identite, points de classement, credits)
- Matchmaking et cloture des matchs (mise a jour du rank)
- Economie virtuelle (credit/debit + historique wallet)
- Contenu communautaire UGC (creation, publication, archivage)
- Moderation (signalements, resolution/rejet admin)
- Leaderboard des meilleurs joueurs
- Observabilite (Actuator, Micrometer, Prometheus, Grafana)

## 1. Lancement

Depuis le dossier du projet:

```bash
docker compose up --build
```

Services:

- API: http://localhost:8081
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3005
- MySQL: localhost:3307

## 2. Comptes

- User API: allan / 7895
- Admin API: admin / admin123
- Grafana: admin / admin123

## 3. Endpoints principaux

- Profils: /api/profils
- Matchmaking: /api/matches
- Economie: /api/economy
- UGC: /api/ugc
- Moderation: /api/moderation/reports
- Leaderboard: /api/leaderboard/top
- Monitoring: /actuator/health, /actuator/prometheus

## 4. Collection Postman

Collection prete a importer:

- postman/MaDemo-Platform.postman_collection.json

Variables deja configurees dans la collection:

- baseUrl = http://localhost:8081
- username/password = allan/7895
- adminUsername/adminPassword = admin/admin123

## 5. Tests

```bash
sh mvnw test
```

Les tests utilisent H2 en memoire avec le profil test.
