import http from 'k6/http';
import { check, fail } from 'k6';
import { ADMIN, API, BASE_URL, USER, headers } from './config.js';

/**
 * Wrappers autour des endpoints de MaDemo.
 *
 * Chaque appel est tague avec `name` pour que les URLs contenant un id
 * (/matches/42) soient agregees dans une seule ligne de metriques.
 */

function opts(token, name) {
    return { headers: headers(token), tags: { name } };
}

export function login(creds) {
    const res = http.post(`${API}/auth/login`, JSON.stringify(creds), opts(null, 'POST /auth/login'));
    check(res, { 'login -> 200': (r) => r.status === 200 });
    if (res.status !== 200) {
        fail(`login ${creds.username} a echoue: ${res.status} ${res.body}`);
    }
    return res.json('token');
}

export function createProfil(token, suffix) {
    const body = {
        name: `k6-player-${suffix}`,
        email: `k6-${suffix}@loadtest.local`,
        rankPoints: 1000,
        credits: 1000.0,
    };
    const res = http.post(`${API}/profils`, JSON.stringify(body), opts(token, 'POST /profils'));
    check(res, { 'create profil -> 201': (r) => r.status === 201 });
    if (res.status !== 201) {
        fail(`creation profil impossible: ${res.status} ${res.body}`);
    }
    return res.json();
}

export function getProfil(token, id) {
    const res = http.get(`${API}/profils/${id}`, opts(token, 'GET /profils/{id}'));
    check(res, { 'get profil -> 200': (r) => r.status === 200 });
    return res;
}

export function listProfils(token) {
    const res = http.get(`${API}/profils`, opts(token, 'GET /profils'));
    check(res, { 'list profils -> 200': (r) => r.status === 200 });
    return res;
}

export function deleteProfil(token, id) {
    return http.del(`${API}/profils/${id}`, null, opts(token, 'DELETE /profils/{id}'));
}

export function createMatch(token, playerOneId, playerTwoId) {
    const body = { playerOneId, playerTwoId };
    const res = http.post(`${API}/matches`, JSON.stringify(body), opts(token, 'POST /matches'));
    check(res, { 'create match -> 201': (r) => r.status === 201 });
    return res;
}

export function completeMatch(token, matchId, winnerId) {
    const res = http.post(
        `${API}/matches/${matchId}/complete`,
        JSON.stringify({ winnerId }),
        opts(token, 'POST /matches/{id}/complete'),
    );
    check(res, { 'complete match -> 200': (r) => r.status === 200 });
    return res;
}

export function listMatches(token) {
    const res = http.get(`${API}/matches`, opts(token, 'GET /matches'));
    check(res, { 'list matches -> 200': (r) => r.status === 200 });
    return res;
}

export function getMatch(token, id) {
    const res = http.get(`${API}/matches/${id}`, opts(token, 'GET /matches/{id}'));
    check(res, { 'get match -> 200': (r) => r.status === 200 });
    return res;
}

export function creditWallet(token, profilId, amount, reason) {
    const body = { profilId, type: 'CREDIT', amount, reason };
    const res = http.post(`${API}/economy/transactions`, JSON.stringify(body), opts(token, 'POST /economy/transactions'));
    check(res, { 'credit wallet -> 201': (r) => r.status === 201 });
    return res;
}

export function listWalletTransactions(token, profilId) {
    const res = http.get(
        `${API}/economy/wallet/${profilId}/transactions`,
        opts(token, 'GET /economy/wallet/{id}/transactions'),
    );
    check(res, { 'list transactions -> 200': (r) => r.status === 200 });
    return res;
}

export function createUgc(token, authorProfilId, suffix) {
    const body = {
        authorProfilId,
        title: `Map communautaire ${suffix}`,
        body: `Contenu genere par le test de charge k6 (${suffix}).`,
    };
    const res = http.post(`${API}/ugc`, JSON.stringify(body), opts(token, 'POST /ugc'));
    check(res, { 'create ugc -> 201': (r) => r.status === 201 });
    return res;
}

export function publishUgc(token, id) {
    const res = http.post(`${API}/ugc/${id}/publish`, null, opts(token, 'POST /ugc/{id}/publish'));
    check(res, { 'publish ugc -> 200': (r) => r.status === 200 });
    return res;
}

export function listUgc(token) {
    const res = http.get(`${API}/ugc`, opts(token, 'GET /ugc'));
    check(res, { 'list ugc -> 200': (r) => r.status === 200 });
    return res;
}

export function reportContent(token, contentId, reporterProfilId, reason) {
    const body = { contentId, reporterProfilId, reason };
    const res = http.post(`${API}/moderation/reports`, JSON.stringify(body), opts(token, 'POST /moderation/reports'));
    check(res, { 'create report -> 201': (r) => r.status === 201 });
    return res;
}

export function listReports(token) {
    const res = http.get(`${API}/moderation/reports`, opts(token, 'GET /moderation/reports'));
    check(res, { 'list reports -> 200': (r) => r.status === 200 });
    return res;
}

export function resolveReport(adminToken, id) {
    const res = http.post(
        `${API}/moderation/reports/${id}/resolve`,
        null,
        opts(adminToken, 'POST /moderation/reports/{id}/resolve'),
    );
    check(res, { 'resolve report -> 200': (r) => r.status === 200 });
    return res;
}

export function leaderboard(token) {
    const res = http.get(`${API}/leaderboard/top`, opts(token, 'GET /leaderboard/top'));
    check(res, { 'leaderboard -> 200': (r) => r.status === 200 });
    return res;
}

export function health() {
    const res = http.get(`${BASE_URL}/actuator/health`, { tags: { name: 'GET /actuator/health' } });
    check(res, {
        'health -> 200': (r) => r.status === 200,
        'health = UP': (r) => r.json('status') === 'UP',
    });
    return res;
}

/**
 * Attention aux noms : le client Prometheus retire les suffixes reserves
 * (_total, _created, _count, _sum...) avant de reexposer la metrique.
 * Le compteur declare `match_created_total` dans MatchmakingService ressort
 * donc sous le nom `match_total` — ce sont bien les noms exposes qu'on
 * verifie ici, pas ceux du code Java.
 */
export function prometheusScrape() {
    const res = http.get(`${BASE_URL}/actuator/prometheus`, { tags: { name: 'GET /actuator/prometheus' } });
    check(res, {
        'prometheus -> 200': (r) => r.status === 200,
        'expose match_total': (r) => r.body.indexOf('match_total') !== -1,
        'expose match_completed_total': (r) => r.body.indexOf('match_completed_total') !== -1,
    });
    return res;
}

/**
 * Cree un jeu de profils reutilisables par les VUs, dans setup().
 * Retourne { token, adminToken, profilIds }.
 */
export function seed(profilCount) {
    const token = login(USER);
    const adminToken = login(ADMIN);
    const run = `${Date.now()}`;
    const profilIds = [];
    for (let i = 0; i < profilCount; i++) {
        profilIds.push(createProfil(token, `${run}-${i}`).id);
    }
    return { token, adminToken, profilIds, run };
}

/** Supprime les profils crees par seed() — appele depuis teardown(). */
export function cleanup(data) {
    if (!data || !data.profilIds) {
        return;
    }
    for (const id of data.profilIds) {
        deleteProfil(data.token, id);
    }
}
