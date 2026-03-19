# MaDemo - Microservice backend monitorable

Ce projet implemente un microservice Spring Boot oriente performance backend avec:

- API REST securisee (Basic Auth)
- Persistence MySQL
- Metriques Micrometer exposees en Prometheus
- Stack Docker Compose: App + MySQL + Prometheus + Grafana
- Dashboard Grafana provisionne automatiquement

## 1. Prerequis

- Java 21+
- Docker + Docker Compose
- Maven Wrapper (inclus)

## 2. Lancer avec Docker

Depuis le dossier du projet:

```bash
docker compose up --build
```

Services demarres:

- API: http://localhost:8081
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000
- MySQL: localhost:3307

Comptes utiles:

- API Basic Auth: allan / 7895
- Grafana: admin / admin123

## 3. Endpoints API

Base URL: http://localhost:8081

- POST /api/profils
- GET /api/profils
- GET /api/profils/{id}
- DELETE /api/profils/{id}

Exemple creation:

```bash
curl -u allan:7895 -X POST http://localhost:8081/api/profils \
  -H "Content-Type: application/json" \
  -d '{"name":"PlayerOne"}'
```

## 4. Endpoints observabilite

- Health: http://localhost:8081/actuator/health
- Prometheus: http://localhost:8081/actuator/prometheus
- Metrics: http://localhost:8081/actuator/metrics

Metriques metier ajoutees:

- profil_created_total
- api.profil.create (timer)
- api.profil.get (timer)
- api.profil.list (timer)
- api.profil.delete (timer)

## 5. Dashboard Grafana

Le dashboard est provisionne automatiquement au demarrage:

- Nom: MaDemo Monitoring
- Panels: debit API, latence p95, erreurs 5xx, compteur profils crees

## 6. Test local sans Docker

Configurer une base MySQL locale puis lancer:

```bash
sh mvnw spring-boot:run
```

Page de test simple:

- http://localhost:8080/profil.html

## 7. Tests

```bash
sh mvnw test
```

Les tests utilisent un profil test avec H2 en memoire.
