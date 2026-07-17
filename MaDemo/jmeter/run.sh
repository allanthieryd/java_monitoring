#!/bin/sh
# Lance un plan JMeter en mode non-GUI et genere un rapport HTML.
#
# Chaque execution ecrit dans son propre dossier horodate : JMeter refuse de
# generer un rapport dans un repertoire non vide, et on garde ainsi l'historique
# des runs pour comparer avant/apres une optimisation.
#
# Variables d'environnement :
#   PLAN         plan a executer (defaut mademo-load-test.jmx)
#   TARGET_HOST  hote de l'API vu depuis le conteneur (defaut app)
#   TARGET_PORT  port de l'API vu depuis le conteneur (defaut 8080)
#
# Tout argument supplementaire est passe tel quel a JMeter :
#   docker compose --profile load-test run --rm jmeter -Jthreads=50 -Jloops=20
set -eu

PLAN="${PLAN:-mademo-load-test.jmx}"
TARGET_HOST="${TARGET_HOST:-app}"
TARGET_PORT="${TARGET_PORT:-8080}"

if [ ! -f "/jmeter/${PLAN}" ]; then
    echo "Plan introuvable : /jmeter/${PLAN}" >&2
    echo "Plans disponibles :" >&2
    ls -1 /jmeter/*.jmx 2>/dev/null >&2 || echo "  (aucun)" >&2
    exit 1
fi

RUN_NAME="$(basename "${PLAN}" .jmx)-$(date +%Y%m%d-%H%M%S)"
OUT_DIR="/jmeter/results/${RUN_NAME}"
mkdir -p "${OUT_DIR}"

echo "Plan     : ${PLAN}"
echo "Cible    : http://${TARGET_HOST}:${TARGET_PORT}"
echo "Resultats: jmeter/results/${RUN_NAME}/"
echo

# -n : mode non-GUI  |  -e -o : rapport HTML en fin de run
# Le code de sortie de JMeter est non nul si une assertion echoue, ce qui rend
# le conteneur utilisable tel quel comme etape de CI.
exec jmeter -n \
    -t "/jmeter/${PLAN}" \
    -Jhost="${TARGET_HOST}" \
    -Jport="${TARGET_PORT}" \
    -l "${OUT_DIR}/results.jtl" \
    -j "${OUT_DIR}/jmeter.log" \
    -e -o "${OUT_DIR}/html-report" \
    "$@"
