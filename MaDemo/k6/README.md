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
#    RATELIMIT_ENABLED=false est indispensable pour load et stress, voir plus bas
RATELIMIT_ENABLED=false docker compose up -d --build

# 2. Créer le dossier de sortie — k6 ne le crée pas lui-même
mkdir -p results

# 3. Jouer un scénario
k6 run k6/smoke.js
k6 run k6/load.js
k6 run --env VUS=30 --env DURATION=2m k6/load.js
```

Chaque run écrit `results/<scénario>-summary.json` (données brutes k6) et
`results/<scénario>-summary.md` (tableau repris dans le résumé du job GitHub).
Sans le `mkdir`, le run se déroule normalement mais k6 termine sur une erreur
`could not open 'results/...'` et n'écrit aucun résumé.

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
l'application. Deux parades, à ne pas confondre :

- **`RATELIMIT_ENABLED=false`** côté application : désactive complètement le
  filtre. **Obligatoire pour `load.js` et `stress.js`** — c'est ce que fait la
  CI. Sans cela, mesuré en local : 67 % de 429 dès 5 VUs.
- **`SPOOF_CLIENT_IP=true`** (défaut) : chaque VU envoie un `X-Forwarded-For`
  distinct et obtient son propre bucket. Cela répartit la charge sur N buckets
  mais **ne supprime pas la limite** : un VU qui dépasse 100 req/min prend
  quand même des 429. Suffisant pour `smoke.js` (23 requêtes au total),
  insuffisant dès que le débit monte.

Les seuils sont configurables via `ratelimit.enabled`,
`ratelimit.api-per-minute` et `ratelimit.auth-per-minute`
(voir [`application.properties`](../src/main/resources/application.properties)).

Pour tester le rate limiter *lui-même*, lancer avec `SPOOF_CLIENT_IP=false`
contre une application où le filtre est actif : les 429 attendus feront alors
échouer le seuil `http_req_failed`, ce qui est le comportement voulu pour ce
cas de figure — c'est un test à écrire à part, pas un scénario de charge.

## Noms des métriques Prometheus

Le client Prometheus retire les suffixes réservés (`_total`, `_created`,
`_count`, `_sum`…) avant de réexposer une métrique. Le compteur déclaré
`match_created_total` dans `MatchmakingService` est donc exposé sous le nom
**`match_total`** : `_total` puis `_created` sont retirés, puis `_total` est
rajouté à l'exposition.

Les checks de `prometheusScrape()` portent sur les noms **exposés**, pas sur
ceux du code Java. À vérifier avec `curl -s localhost:8080/actuator/prometheus`
avant d'écrire une requête PromQL ou un panneau Grafana.

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
