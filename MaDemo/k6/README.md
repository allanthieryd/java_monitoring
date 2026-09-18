# Tests de charge k6

Campagnes de performance exécutées par le workflow [`ci.yml`](../../.github/workflows/ci.yml)
et rejouables en local contre la stack `docker-compose`.

## Scénarios

| Fichier | Profil | Durée | Exécution |
| --- | --- | --- | --- |
| `smoke.js` | 1 VU, 1 itération, tous les endpoints | ~10 s | chaque PR et chaque push |
| `load.js` | palier de 10 VUs, mix 70 % lecture / 30 % écriture | ~2 min | push sur `main`, ou manuel |
| `stress.js` | montée jusqu'à 60 VUs, saturation volontaire | ~3 min | manuel uniquement |

`smoke.js` est un test fonctionnel déguisé : il échoue au moindre check rouge
(`checks: rate==1.0`). `load.js` et `stress.js` sont des tests de performance :
ils échouent sur dépassement des seuils de latence ou du taux d'erreur, définis
dans [`lib/config.js`](lib/config.js).

## Lancer en local

```bash
# 1. Démarrer la stack (app + MySQL + Prometheus + Grafana)
docker compose up -d --build

# 2. Jouer un scénario
k6 run k6/smoke.js
k6 run k6/load.js
k6 run --env VUS=30 --env DURATION=2m k6/load.js
```

Chaque run écrit `results/<scénario>-summary.json` (données brutes k6) et
`results/<scénario>-summary.md` (tableau repris dans le résumé du job GitHub).

## Variables d'environnement

| Variable | Défaut | Rôle |
| --- | --- | --- |
| `BASE_URL` | `http://localhost:8080` | cible du test |
| `VUS` | `10` (`60` en stress) | utilisateurs virtuels |
| `DURATION` | `1m` | durée du palier de charge (`load.js`) |
| `PROFIL_POOL` | `10` | profils créés dans `setup()` |
| `K6_USER` / `K6_PASSWORD` | `allan` / `7895` | compte `ROLE_USER` |
| `K6_ADMIN` / `K6_ADMIN_PASSWORD` | `admin` / `admin123` | compte `ROLE_ADMIN` |
| `K6_OUT_DIR` | `results` | dossier des résumés |
| `SPOOF_CLIENT_IP` | `true` | voir ci-dessous |

## Rate limiting

`RateLimitFilter` autorise 100 req/min par IP cliente (10 sur `/api/v1/auth/**`).
Sans précaution, un test de charge mesure donc le rate limiter et non
l'application. Deux parades, combinables :

- **`SPOOF_CLIENT_IP=true`** (défaut) : chaque VU envoie un `X-Forwarded-For`
  distinct et obtient son propre bucket. Fonctionne sans toucher à l'app —
  c'est ce qui rend les scripts utilisables contre n'importe quel déploiement.
- **`RATELIMIT_ENABLED=false`** côté application : désactive complètement le
  filtre. C'est ce que fait la CI, pour des mesures non biaisées.

Les seuils sont configurables via `ratelimit.enabled`,
`ratelimit.api-per-minute` et `ratelimit.auth-per-minute`
(voir [`application.properties`](../src/main/resources/application.properties)).

Pour tester le rate limiter *lui-même*, lancer avec `SPOOF_CLIENT_IP=false`
contre une application où le filtre est actif : les 429 attendus feront alors
échouer le seuil `http_req_failed`, ce qui est le comportement voulu pour ce
cas de figure — c'est un test à écrire à part, pas un scénario de charge.

## Observer pendant le run

La stack expose les métriques pendant le test :

- Prometheus : `http://localhost:9090`
- Grafana : `http://localhost:3000` (dashboards provisionnés dans `grafana/dashboards`)
- Endpoint brut : `http://localhost:8080/actuator/prometheus`

`stress.js` est fait pour ça : le pic met en évidence la saturation du pool
Hikari (`spring.datasource.hikari.maximum-pool-size=10`) et la montée des
percentiles `http_server_requests`.

> Les ports ci-dessus supposent une publication de ports dans `docker-compose.yml`
> (le fichier actuel n'en déclare pas). En local, ajouter les mappings
> `8080:8080`, `9090:9090` et `3000:3000` ou utiliser `docker compose port`.
