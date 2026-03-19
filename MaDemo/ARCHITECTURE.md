# Architecture Back-End - MaDemo

## 1. Vision

Architecture microservice orientee monitoring:

- Service metier principal: MaDemo API
- Brique de surveillance isolee: Prometheus + Grafana
- Communication non intrusive via endpoint pull `/actuator/prometheus`

Cette separation garantit qu'une panne du monitoring ne bloque pas l'API metier.

## 2. Domaines metier

- Identite et acces: Basic Auth (utilisateurs en memoire)
- Joueurs et profils: creation, lecture, suppression des profils
- Observabilite: metriques techniques et metriques metier

## 3. Composants

- `ProfilController`: API REST
- `ProfilService`: regles metier, transactions, metriques custom
- `ProfilRepository`: acces donnees JPA
- `ApiExceptionHandler`: gestion uniforme des erreurs
- `SecurityConfig`: frontiere de securite des flux

## 4. Donnees

- SGBD: MySQL 8.4
- Table principale: `profil(id, nom)`
- Ecriture transactionnelle via service Spring `@Transactional`

## 5. API et securisation

- `/api/**`: authentification obligatoire
- `/actuator/**`: ouvert pour scraping de metriques
- Validation entree: `name` obligatoire, longueur max 100
- Erreurs standardisees en JSON

## 6. Observabilite

- Micrometer + Actuator pour instrumentation
- Prometheus pour collecte periodique
- Grafana pour visualisation et exploitation
- Metrique metier: `profil_created_total`

## 7. Scalabilite et resilience

- Base de donnees et API separentes en conteneurs
- Monitoring decouple de l'execution metier
- Possibilite d'ajouter replication app + load balancer
- Collecte pull limitee en frequence via `scrape_interval`

## 8. Flux principal

1. Client appelle API (`/api/profils`) avec auth.
2. API valide puis persiste via JPA.
3. Actuator expose les metriques de l'API.
4. Prometheus scrape les metriques.
5. Grafana interroge Prometheus et affiche les dashboards.

## 9. Hypotheses et limites

- Authentification simple (Basic Auth en memoire) pour contexte pedagogique.
- Un seul service metier dans cette version.
- Extension naturelle: externaliser IAM, alerting avance, bus d'evenements.
