/**
 * Generation du resume de fin de run.
 *
 * Volontairement sans dependance externe (pas de https://jslib.k6.io/...) :
 * la CI ne doit pas casser parce qu'un CDN est indisponible.
 *
 * Produit trois sorties :
 *  - stdout        : resume lisible dans les logs du job
 *  - summary.json  : donnees brutes k6, archivees comme artefact
 *  - summary.md    : tableau injecte dans $GITHUB_STEP_SUMMARY
 */

function ms(value) {
    if (value === undefined || value === null || isNaN(value)) {
        return 'n/a';
    }
    return `${value.toFixed(1)} ms`;
}

function pct(value) {
    if (value === undefined || value === null || isNaN(value)) {
        return 'n/a';
    }
    return `${(value * 100).toFixed(2)} %`;
}

function metric(data, name) {
    return (data.metrics && data.metrics[name]) || null;
}

function values(data, name) {
    const m = metric(data, name);
    return (m && m.values) || {};
}

/** Liste tous les seuils du run avec leur statut. */
function thresholds(data) {
    const out = [];
    const metrics = data.metrics || {};
    for (const name of Object.keys(metrics)) {
        const th = metrics[name].thresholds;
        if (!th) {
            continue;
        }
        for (const expr of Object.keys(th)) {
            out.push({ metric: name, expr, ok: th[expr].ok !== false });
        }
    }
    return out;
}

/** Parcourt recursivement les groupes pour retrouver les checks en echec. */
function failedChecks(group, acc) {
    acc = acc || [];
    if (!group) {
        return acc;
    }
    for (const c of group.checks || []) {
        if (c.fails > 0) {
            acc.push({ name: c.name, fails: c.fails, passes: c.passes });
        }
    }
    for (const g of group.groups || []) {
        failedChecks(g, acc);
    }
    return acc;
}

function collect(data, scenario) {
    const dur = values(data, 'http_req_duration');
    const reqs = values(data, 'http_reqs');
    const failed = values(data, 'http_req_failed');
    const checks = values(data, 'checks');
    const iters = values(data, 'iterations');
    const vus = values(data, 'vus_max');

    return {
        scenario,
        requests: reqs.count || 0,
        rps: reqs.rate || 0,
        iterations: iters.count || 0,
        vusMax: vus.max || 0,
        errorRate: failed.rate,
        checkRate: checks.rate,
        avg: dur.avg,
        p95: dur['p(95)'],
        p99: dur['p(99)'],
        max: dur.max,
        thresholds: thresholds(data),
        failedChecks: failedChecks(data.root_group),
    };
}

function toText(s) {
    const lines = [
        '',
        `=== Resume k6 — scenario "${s.scenario}" ===`,
        `  requetes      : ${s.requests} (${s.rps.toFixed(1)}/s)`,
        `  iterations    : ${s.iterations}  |  VUs max : ${s.vusMax}`,
        `  erreurs HTTP  : ${pct(s.errorRate)}`,
        `  checks OK     : ${pct(s.checkRate)}`,
        `  latence       : avg ${ms(s.avg)} | p95 ${ms(s.p95)} | p99 ${ms(s.p99)} | max ${ms(s.max)}`,
        '',
    ];
    for (const t of s.thresholds) {
        lines.push(`  ${t.ok ? '[OK]  ' : '[KO]  '}${t.metric}: ${t.expr}`);
    }
    for (const c of s.failedChecks) {
        lines.push(`  [KO]  check "${c.name}" — ${c.fails} echec(s) / ${c.fails + c.passes}`);
    }
    lines.push('');
    return lines.join('\n');
}

function toMarkdown(s) {
    const failedThresholds = s.thresholds.filter((t) => !t.ok);
    const ok = failedThresholds.length === 0 && s.failedChecks.length === 0;

    const lines = [
        `## ${ok ? '✅' : '❌'} k6 — scénario \`${s.scenario}\``,
        '',
        '| Métrique | Valeur |',
        '| --- | --- |',
        `| Requêtes | ${s.requests} (${s.rps.toFixed(1)}/s) |`,
        `| Itérations | ${s.iterations} |`,
        `| VUs max | ${s.vusMax} |`,
        `| Erreurs HTTP | ${pct(s.errorRate)} |`,
        `| Checks réussis | ${pct(s.checkRate)} |`,
        `| Latence moyenne | ${ms(s.avg)} |`,
        `| Latence p95 | ${ms(s.p95)} |`,
        `| Latence p99 | ${ms(s.p99)} |`,
        `| Latence max | ${ms(s.max)} |`,
        '',
        '### Seuils',
        '',
    ];

    for (const t of s.thresholds) {
        lines.push(`- ${t.ok ? '✅' : '❌'} \`${t.metric}\` : \`${t.expr}\``);
    }

    if (s.failedChecks.length > 0) {
        lines.push('', '### Checks en échec', '');
        for (const c of s.failedChecks) {
            lines.push(`- ❌ ${c.name} — ${c.fails} échec(s) sur ${c.fails + c.passes}`);
        }
    }

    lines.push('');
    return lines.join('\n');
}

/**
 * A utiliser dans chaque scenario :
 *   export function handleSummary(data) { return makeSummary(data, 'load'); }
 */
export function makeSummary(data, scenario) {
    const s = collect(data, scenario);
    const dir = __ENV.K6_OUT_DIR || 'results';
    const out = {};
    out.stdout = toText(s);
    out[`${dir}/${scenario}-summary.json`] = JSON.stringify(data, null, 2);
    out[`${dir}/${scenario}-summary.md`] = toMarkdown(s);
    return out;
}
