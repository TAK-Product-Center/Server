#!/bin/bash
#
# Cold-start launcher for the acceptance lab. Run it from a clean engine; it does every step in the order the
# dependencies actually require, and it fails loudly instead of leaving a half-configured server.
#
#   ./qa/lab-up.sh            bring the lab up and run the acceptance suite
#   ./qa/lab-up.sh --clean    docker compose down -v first, so the CA, the configuration and the database are new
#   ./qa/lab-up.sh --no-test  set the lab up, do not run the suite
#
# Requires: docker compose, jq and curl on the host. Everything else runs in containers.
set -euo pipefail
cd "$(dirname "$0")/.."
export PATH=/opt/homebrew/bin:$PATH

CLEAN=0; RUNTEST=1
for a in "$@"; do
	case "$a" in
		--clean) CLEAN=1 ;;
		--no-test) RUNTEST=0 ;;
		*) echo "unknown option $a" >&2; exit 2 ;;
	esac
done

step() { printf '\n=== %s  %s\n' "$(date -u +%FT%TZ)" "$*"; }
need() { command -v "$1" >/dev/null || { echo "missing required host tool: $1" >&2; exit 2; }; }
need docker; need jq; need curl

set -a; . ./.env; set +a
: "${DB_PASSWORD:?set DB_PASSWORD in .env}"
: "${KC_ADMIN_PASSWORD:?set KC_ADMIN_PASSWORD in .env}"
export EXTRA_SERVER_CERTS="${EXTRA_SERVER_CERTS:-idp}"

if [ "$CLEAN" = 1 ]; then
	step "down -v (destroys the CA, the configuration and the database)"
	docker compose --profile acceptance down -v --remove-orphans
fi

step "1/6 certificates (the IdP needs one from this CA before it can start)"
docker compose run --rm takinit certs

step "2/6 database and IdP"
docker compose up -d db
docker compose --profile acceptance up -d idp
n=0; until docker compose ps --format '{{.Service}} {{.Status}}' | grep -q 'idp.*(healthy)'; do
	sleep 5; n=$((n+1)); [ $n -lt 120 ] || { echo "idp never became healthy"; docker compose logs idp | tail -30; exit 1; }
done
echo "idp healthy"

step "3/6 realm provisioning, which is what produces the confidential client secret"
./qa/keycloak-realm-container.sh

step "4/6 CoreConfig.xml, rendered WITH that secret"
set -a; . ./.env.secrets; set +a
export OIDC_CLIENT_SECRET="$TAKWEB_CLIENT_SECRET"
[ -n "$OIDC_CLIENT_SECRET" ] || { echo "realm provisioning did not yield a client secret"; exit 1; }
FORCE=1 docker compose run --rm -e OIDC_CLIENT_SECRET -e FORCE=1 takconfig config

step "5/6 schema and the three TAK processes"
docker compose up -d takserver-api
n=0; until docker compose ps --format '{{.Service}} {{.Status}}' | grep -q 'takserver-api.*(healthy)'; do
	sleep 15; n=$((n+1))
	[ $n -lt 240 ] || { echo "takserver-api never became healthy"; docker compose logs takserver-api | tail -40; exit 1; }
	[ $((n % 8)) -eq 0 ] && echo "  still waiting, $((n*15))s"
done
echo "takserver-api healthy"
docker compose run --rm --no-deps takconfig verify

step "6/6 acceptance"
docker compose --profile acceptance up -d qa
if [ "$RUNTEST" = 1 ]; then
	docker compose exec -T \
	  -e KC=https://idp:8843 -e REALM=tak \
	  -e TAK=https://takserver-config:8446 \
	  -e TAK_BEARER=https://takserver-config:8446 \
	  -e TAK_CERT=https://takserver-config:8443 \
	  -e TAK_LOG=/opt/tak/logs/takserver-api.log \
	  -e PROBE_SECRET="$PROBE_CLIENT_SECRET" \
	  -e CSR_O="${CERT_ORGANIZATION:-Lastwall}" -e CSR_OU="${CERT_ORGANIZATIONAL_UNIT:-TAKOIDC}" \
	  -e OUT="/qa/${E2E_OUT:-e2e-container-clean-start.md}" \
	  qa bash /qa/run-e2e-container.sh
else
	echo "lab is up; --no-test was given"
fi
