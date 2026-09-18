import exec from 'k6/execution';

/** URL de base de l'API sous test (sans slash final). */
export const BASE_URL = (__ENV.BASE_URL || 'http://localhost:8080').replace(/\/+$/, '');
export const API = `${BASE_URL}/api/v1`;

/** Comptes definis dans SecurityConfig (InMemoryUserDetailsManager). */
export const USER = {
    username: __ENV.K6_USER || 'allan',
    password: __ENV.K6_PASSWORD || '7895',
};

export const ADMIN = {
    username: __ENV.K6_ADMIN || 'admin',
    password: __ENV.K6_ADMIN_PASSWORD || 'admin123',
};

/**
 * RateLimitFilter compte 100 req/min par IP cliente (10 sur /auth).
 * En envoyant un X-Forwarded-For different par VU, chaque VU obtient son
 * propre bucket : on mesure l'application, pas le rate limiter.
 *
 * SPOOF_CLIENT_IP=false pour desactiver (ex. test dedie du rate limiting).
 */
const SPOOF_CLIENT_IP = (__ENV.SPOOF_CLIENT_IP || 'true') !== 'false';

function clientIp() {
    const id = exec.vu.idInTest || 0;
    return `10.0.${Math.floor(id / 254) % 254}.${(id % 254) + 1}`;
}

/** En-tetes JSON, avec Authorization si un token est fourni. */
export function headers(token) {
    const h = { 'Content-Type': 'application/json' };
    if (token) {
        h.Authorization = `Bearer ${token}`;
    }
    if (SPOOF_CLIENT_IP) {
        h['X-Forwarded-For'] = clientIp();
    }
    return h;
}

/**
 * Percentiles calcules pour le resume de fin de run.
 * Sans p(99) explicite ici, k6 ne le remonte pas dans handleSummary et le
 * resume affiche "n/a" meme quand un seuil porte dessus.
 */
export const TREND_STATS = ['avg', 'min', 'med', 'p(95)', 'p(99)', 'max'];

/** Seuils communs : un run qui les depasse fait echouer la CI. */
export const THRESHOLDS = {
    smoke: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<1000'],
        checks: ['rate==1.0'],
    },
    load: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<1000', 'p(99)<2000'],
        checks: ['rate>0.99'],
    },
    stress: {
        // Sous stress on tolere une degradation, mais pas des erreurs massives.
        http_req_failed: ['rate<0.05'],
        http_req_duration: ['p(95)<3000'],
    },
};
