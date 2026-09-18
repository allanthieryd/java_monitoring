import { sleep } from 'k6';
import { THRESHOLDS, TREND_STATS } from './lib/config.js';
import { makeSummary } from './lib/summary.js';
import {
    seed,
    cleanup,
    getProfil,
    listMatches,
    createMatch,
    completeMatch,
    creditWallet,
    leaderboard,
} from './lib/api.js';

/**
 * Stress / spike test : on pousse jusqu'a trouver le point de rupture.
 *
 * Sert surtout de generateur de trafic pour les dashboards Grafana :
 * le pic met en evidence la saturation du pool Hikari
 * (spring.datasource.hikari.maximum-pool-size=10), la montee des
 * percentiles http_server_requests et le comportement des compteurs metier.
 *
 * Non execute automatiquement sur les PR : lancement manuel
 * (workflow_dispatch) ou en local contre la stack docker-compose.
 */
const PEAK_VUS = parseInt(__ENV.VUS || '60', 10);

export const options = {
    scenarios: {
        spike: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: Math.ceil(PEAK_VUS / 4) },
                { duration: '30s', target: Math.ceil(PEAK_VUS / 2) },
                { duration: '1m', target: PEAK_VUS },
                { duration: '30s', target: PEAK_VUS },
                { duration: '30s', target: 0 },
            ],
            gracefulRampDown: '20s',
        },
    },
    thresholds: THRESHOLDS.stress,
    summaryTrendStats: TREND_STATS,
};

export function setup() {
    return seed(parseInt(__ENV.PROFIL_POOL || '20', 10));
}

export default function (data) {
    const { token, profilIds } = data;
    const a = profilIds[__VU % profilIds.length];
    const b = profilIds[(__VU + 1) % profilIds.length];

    leaderboard(token);
    getProfil(token, a);
    listMatches(token);

    const matchId = createMatch(token, a, b).json('id');
    if (matchId) {
        completeMatch(token, matchId, a);
    }
    creditWallet(token, a, 5, 'stress k6');

    // Pas de sleep long : on cherche volontairement la saturation.
    sleep(0.2);
}

export function teardown(data) {
    cleanup(data);
}

export function handleSummary(data) {
    return makeSummary(data, 'stress');
}
