import { group, sleep } from 'k6';
import { THRESHOLDS, USER, ADMIN } from './lib/config.js';
import { makeSummary } from './lib/summary.js';
import {
    login,
    createProfil,
    getProfil,
    listProfils,
    deleteProfil,
    createMatch,
    completeMatch,
    getMatch,
    listMatches,
    creditWallet,
    listWalletTransactions,
    createUgc,
    publishUgc,
    listUgc,
    reportContent,
    listReports,
    resolveReport,
    leaderboard,
    health,
    prometheusScrape,
} from './lib/api.js';

/**
 * Smoke test : 1 VU, 1 iteration.
 *
 * Verifie que chaque parcours fonctionnel de l'API repond correctement
 * apres un deploiement. C'est le scenario execute sur chaque PR : rapide,
 * deterministe, et il echoue au moindre check rouge (checks: rate==1.0).
 */
export const options = {
    vus: 1,
    iterations: 1,
    thresholds: THRESHOLDS.smoke,
};

export default function () {
    const run = `${Date.now()}`;
    let token;
    let adminToken;
    let playerOne;
    let playerTwo;

    group('actuator', function () {
        health();
        prometheusScrape();
    });

    group('auth', function () {
        token = login(USER);
        adminToken = login(ADMIN);
    });

    group('profils', function () {
        playerOne = createProfil(token, `${run}-p1`);
        playerTwo = createProfil(token, `${run}-p2`);
        getProfil(token, playerOne.id);
        listProfils(token);
    });

    group('matchmaking', function () {
        const created = createMatch(token, playerOne.id, playerTwo.id);
        const matchId = created.json('id');
        getMatch(token, matchId);
        completeMatch(token, matchId, playerOne.id);
        listMatches(token);
    });

    group('economy', function () {
        creditWallet(token, playerOne.id, 250.5, 'recompense k6 smoke');
        listWalletTransactions(token, playerOne.id);
    });

    group('ugc + moderation', function () {
        const ugcId = createUgc(token, playerOne.id, run).json('id');
        publishUgc(token, ugcId);
        listUgc(token);

        const reportId = reportContent(token, ugcId, playerTwo.id, 'contenu signale par k6').json('id');
        listReports(token);
        // /resolve est reserve au role ADMIN.
        resolveReport(adminToken, reportId);
    });

    group('leaderboard', function () {
        leaderboard(token);
    });

    group('nettoyage', function () {
        deleteProfil(token, playerOne.id);
        deleteProfil(token, playerTwo.id);
    });

    sleep(1);
}

export function handleSummary(data) {
    return makeSummary(data, 'smoke');
}
