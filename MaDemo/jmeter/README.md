# Tests de charge — Apache JMeter

Deux plans de test complémentaires, exécutés dans un conteneur JMeter intégré à la stack Docker Compose.

| Plan | Question posée | Verdict |
|------|----------------|---------|
| `mademo-load-test.jmx` | Comment se comporte l'API sous N joueurs concurrents ? | Temps de réponse, débit, taux d'erreur |
| `mademo-ratelimit-test.jmx` | Le rate limiter applique-t-il vraiment 100 req/min/IP ? | Assertion : 100 × `200` puis `429` |

---

## 1. Lancer les tests

La stack doit tourner (`docker compose up -d`). Le service `jmeter` est sous le profil `load-test` : il ne démarre pas avec la stack, on le lance à la demande.

```bash
# Test de charge, paramètres par défaut (20 joueurs × 10 itérations)
docker compose --profile load-test run --rm jmeter

# Validation du rate limiting
PLAN=mademo-ratelimit-test.jmx docker compose --profile load-test run --rm jmeter

# Charge plus élevée
docker compose --profile load-test run --rm jmeter -Jthreads=50 -Jloops=20
```

Le conteneur sort en code non nul si une assertion échoue : utilisable tel quel comme étape de CI.

### Paramètres (`-J`)

| Paramètre | Défaut | Rôle |
|-----------|--------|------|
| `threads` | 20 | Joueurs simultanés |
| `loops` | 10 | Itérations par joueur |
| `rampup` | 20 | Montée en charge (s) |
| `thinktime.base` | 700 | Temps de réflexion minimum (ms) |
| `thinktime.range` | 600 | Amplitude aléatoire du think time (ms) |
| `requests` | 120 | Requêtes envoyées (plan rate limit) |
| `apilimit` | 100 | Limite attendue (plan rate limit) |

---

## 2. Lire les résultats

Chaque exécution écrit dans un dossier horodaté (JMeter refuse d'écrire un rapport dans un répertoire non vide, et on garde ainsi l'historique pour comparer avant/après une optimisation) :

```
jmeter/results/mademo-load-test-20260717-114030/
├── html-report/index.html   ← rapport complet (ouvrir dans un navigateur)
├── results.jtl              ← données brutes (CSV)
└── jmeter.log               ← log d'exécution
```

```bash
open jmeter/results/mademo-load-test-*/html-report/index.html
```

Le dossier `results/` est ignoré par Git.

**Pendant un run, l'effet est visible dans Grafana** (http://localhost:3005) : c'est tout l'intérêt de brancher JMeter sur une stack déjà instrumentée. Les métriques UX (`match_completion_rate`, `match_duration_seconds`) réagissent en direct à la charge injectée.

---

## 3. Scénario du test de charge

Chaque thread simule **un joueur** :

```
01 POST /auth/login              ─ une seule fois, le JWT est réutilisé
02 POST /profils (joueur A)      ┐
03 POST /profils (joueur B)      │
04 POST /matches                 ├─ répété `loops` fois
05 POST /matches/{id}/complete   │
06 GET  /leaderboard/top         │
07 GET  /profils/{id}            ┘
```

Le parcours est réaliste : il crée deux joueurs, les fait s'affronter, clôture le match (ce qui déclenche le recalcul de rang et la publication de `MatchCompletedEvent`), puis consulte le classement.

---

## 4. Décisions de conception

### 4.1 Une IP simulée par joueur — sinon on ne mesure pas l'API

`RateLimitFilter` limite à **100 req/min par IP**. Sans précaution, les N threads partiraient de la même IP, partageraient un seul bucket, et le test mesurerait le rate limiter au lieu de l'API.

Le filtre résout l'IP via `X-Forwarded-For` (comportement standard derrière un load balancer). Chaque thread s'attribue donc une IP distincte, dérivée de son numéro de thread :

```groovy
String ip = "10." + seed + "." + (n.intdiv(256) % 256) + "." + (n % 256);
vars.put("CLIENT_IP", ip);
```

Aucune modification applicative n'est nécessaire : on simule simplement N clients derrière un proxy, ce qui est le déploiement réel visé.

L'octet de tête dérive de la minute courante : les buckets Bucket4j vivent en mémoire côté serveur et ne se rechargent qu'au bout d'une minute. Sans ça, relancer le test aussitôt hériterait des tokens déjà consommés et produirait des `429` parasites.

> **Piège Groovy** : `intdiv()` et non `/`. En Groovy, `/` renvoie un `BigDecimal` auquel `%` ne s'applique pas. Un `/` ici fait échouer le script **silencieusement** : `CLIENT_IP` reste vide, JMeter envoie le littéral `${CLIENT_IP}` en header, le filtre l'accepte comme une IP, et tous les threads retombent dans un seul bucket. Le test tourne, produit un rapport, et mesure la mauvaise chose.

### 4.2 Un login par joueur, pas par itération

Le JWT est valable 24h : un joueur réel se logue une fois. Le test fait pareil, et pour une raison mesurée :

| | Temps moyen |
|---|---|
| `POST /auth/login` (BCrypt) | **88 ms** |
| Endpoints API | 5 – 11 ms |

Se reloguer à chaque itération ferait de BCrypt ~90 % du temps mesuré et masquerait la performance réelle de l'API.

### 4.3 Think time — on monte la charge en ajoutant des joueurs

Un thread sans pause envoie plusieurs centaines de req/min et dépasserait seul son budget de 100 req/min. Le think time (~1 s entre deux actions) maintient chaque joueur simulé sous ce plafond, autour de 60 req/min — ce qui correspond aussi au rythme d'un humain.

**Conséquence : pour augmenter la charge, augmenter `threads`, pas la cadence.** Chaque thread supplémentaire est un joueur de plus, avec sa propre IP et son propre budget.

### 4.4 `GET /profils/{id}` plutôt que `GET /profils`

`GET /api/v1/profils` renvoie la table entière sans pagination. Comme le test crée des profils à chaque itération, cet endpoint ralentirait au fil des exécutions et ferait dériver les résultats. La lecture par id est bornée et reste comparable d'un run à l'autre.

---

## 5. Résultats de référence

Stack Docker locale, 20 joueurs × 10 itérations, 1220 requêtes :

| Endpoint | n | moy | p95 | max |
|----------|---|-----|-----|-----|
| `POST /auth/login` | 20 | 88 ms | 126 ms | 126 ms |
| `POST /profils` (A) | 200 | 9 ms | 17 ms | 23 ms |
| `POST /profils` (B) | 200 | 9 ms | 18 ms | 21 ms |
| `POST /matches` | 200 | 9 ms | 18 ms | 25 ms |
| `POST /matches/{id}/complete` | 200 | 11 ms | 20 ms | 40 ms |
| `GET /leaderboard/top` | 200 | 6 ms | 11 ms | 36 ms |
| `GET /profils/{id}` | 200 | 5 ms | 9 ms | 16 ms |

**0 erreur, ~15 req/s.** Le débit est plafonné par le think time et le nombre de joueurs, pas par l'application : ce run mesure la latence à charge nominale, il ne cherche pas le point de rupture.

À retenir : `complete` est l'endpoint le plus lent des écritures (11 ms), ce qui est cohérent — il écrit trois entités (match + deux profils) dans la même transaction. Le logging asynchrone et les `@Async` listeners tiennent leur promesse : la publication de `MatchCompletedEvent` n'apparaît pas dans la latence HTTP.

---

## 6. Constats et pistes

| Constat | Piste |
|---------|-------|
| `GET /api/v1/profils` renvoie la table entière sans pagination | Ajouter `Pageable` — dégradation garantie en production |
| Le pool Hikari est à 10 connexions | Au-delà de ~50 joueurs actifs, c'est le premier plafond à surveiller (`hikaricp_connections_pending` dans Grafana) |
| Les buckets de rate limiting sont locaux au processus | Déjà documenté dans `ARCHITECTURE.md` : `bucket4j-redis` en multi-instances |
| Le test tourne depuis une seule machine | Pour chercher le point de rupture réel, passer en mode distribué JMeter ou retirer le think time avec des IP par itération |

---

## 7. Ouvrir les plans dans l'IHM JMeter

Les `.jmx` sont des fichiers JMeter standards, éditables dans l'IHM (JMeter 5.6.3) :

```bash
brew install jmeter
jmeter -t jmeter/mademo-load-test.jmx
```

L'IHM cible `localhost:8081` par défaut (le port publié sur l'hôte). Le conteneur, lui, cible `app:8080` : il est sur le réseau Compose et joint l'application par son nom de service.

> Ne pas lancer un test de charge depuis l'IHM : elle consomme énormément de ressources et fausse les mesures. L'IHM sert à construire et déboguer le plan, le mode non-GUI (`-n`, celui du conteneur) à l'exécuter.
