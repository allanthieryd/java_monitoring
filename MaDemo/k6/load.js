import { group, sleep } from 'k6';
import { THRESHOLDS } from './lib/config.js';
import { makeSummary } from './lib/summary.js';
import {
    seed,
    cleanup,
    getProfil,
    listProfils,
    createMatch,
    completeMatch,
    listMatches,
    creditWallet,
    listWalletTransactions,
    createUgc,
    publishUgc,
    leaderboard,
} from './lib/api.js';

/**
 * Load test : charge nominale soutenue.
 *
 * Melange realiste ~70 % lecture / 30 % ecriture sur un pool de profils
 * cree une seule fois dans setup(). Duree ~2 min, dimensionne pour tourner
 * sur un runner GitHub gratuit (2 vCPU) sans saturer le CPU du runner
 * lui-meme — ce qui fausserait les mesures.
 *
 * Reglable : VUS, DURATION, PROFIL_POOL.
 */
const VUS = parseInt(__ENV.VUS || '10', 10);
const DURATION = __ENV.DURATION || '1m';
const PROFIL_POOL = parseInt(__ENV.PROFIL_POOL || '10', 10);

export const options = {
    scenarios: {
        nominal: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: VUS },
                { duration: DURATION, target: VUS },
                { duration: '15s', target: 0 },
            ],
            gracefulRampDown: '15s',
        },
    },
    thresholds: THRESHOLDS.load,
};

export function setup() {
    return seed(PROFIL_POOL);
}

export default function (data) {
    const { token, profilIds } = data;
    const a = profilIds[Math.floor(Math.random() * profilIds.length)];
    let b = profilIds[Math.floor(Math.random() * profilIds.length)];
    if (a === b) {
        // createMatch refuse un joueur contre lui-meme.
        b = profilIds[(profilIds.indexOf(a) + 1) % profilIds.length];
    }

    group('lecture', function () {
        leaderboard(token);
        listProfils(token);
        getProfil(token, a);
        listMatches(token);
        listWalletTransactions(token, a);
    });

    group('ecriture', function () {
        const matchId = createMatch(token, a, b).json('id');
        completeMatch(token, matchId, a);
        creditWallet(token, a, 10, 'gain de match k6');

        const ugcId = createUgc(token, a, `${data.run}-${__ITER}`).json('id');
        publishUgc(token, ugcId);
    });

    sleep(1);
}

export function teardown(data) {
    cleanup(data);
}

export function handleSummary(data) {
    return makeSummary(data, 'load');
}
