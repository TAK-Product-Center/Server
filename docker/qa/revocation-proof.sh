#!/bin/bash
#
# Proves that revocation actually revokes: a client certificate is accepted on 8089 and 8443, then revoked with the
# init image's maintenance subcommand, then rejected on both after the TAK containers pick up the new CRL.
#
# Two certificates are exercised, because TAK has two issuers:
#   revoketest-root  issued by the root CA with makeCert.sh, revoked against ca.crl
#   revoketest-enrol issued by the ONLINE SIGNER (intermediate-ca) through /Marti/api/tls/signClient/v2, which is
#                    what every OIDC enrolment produces, revoked against intermediate-ca.crl
#
# Run after the lab is up: ./qa/revocation-proof.sh
set -uo pipefail
cd "$(dirname "$0")/.."
export PATH=/opt/homebrew/bin:$PATH
set -a; . ./.env; . ./.env.secrets; set +a

CP="${CERT_PASS:-atakatak}"
say() { printf '\n=== %s  %s\n' "$(date -u +%FT%TZ)" "$*"; }
inqa() { docker compose exec -T qa "$@"; }

say "0. issue a client certificate from the root CA"
docker compose run --rm takinit make-cert client revoketest-root

say "0b. enrol a certificate through the online signer, the way an OIDC client does"
inqa bash -c '
set -e
TOK=$(curl -sk -X POST "https://idp:8843/realms/tak/protocol/openid-connect/token" \
  -d grant_type=password -d client_id=probe -d client_secret="'"$PROBE_CLIENT_SECRET"'" \
  -d username=alice -d password=alice -d scope="openid profile groups" \
  | python3 -c "import json,sys;print(json.load(sys.stdin)[\"access_token\"])")
IDENT=$(curl -sk -H "Authorization: Bearer $TOK" https://takserver-config:8446/Marti/api/tls/config \
  | python3 -c "import re,sys;m=re.search(r\"<identity>([^<]+)</identity>\",sys.stdin.read());print(m.group(1))")
echo "enrolment identity: $IDENT"
openssl req -new -newkey rsa:2048 -nodes -keyout /tmp/enrol.key -out /tmp/enrol.csr \
  -subj "/CN=$IDENT/O='"${CERT_ORGANIZATION:-Lastwall}"'/OU='"${CERT_ORGANIZATIONAL_UNIT:-TAKOIDC}"'" >/dev/null 2>&1
B64=$(openssl req -in /tmp/enrol.csr -outform DER | base64 | tr -d "\n")
curl -sk -H "Authorization: Bearer $TOK" -H "Content-Type: text/plain" -H "Accept: application/json" \
  --data-binary "$B64" "https://takserver-config:8446/Marti/api/tls/signClient/v2?clientUid=revproof&version=2" \
  > /tmp/signed.json
python3 - <<PY
import json,base64
j=json.load(open("/tmp/signed.json"))
open("/tmp/enrol.der","wb").write(base64.b64decode(j["signedCert"]))
PY
openssl x509 -inform DER -in /tmp/enrol.der -out /tmp/enrol.pem
echo "issued by: $(openssl x509 -in /tmp/enrol.pem -noout -issuer)"
echo "subject  : $(openssl x509 -in /tmp/enrol.pem -noout -subject)"
echo "serial   : $(openssl x509 -in /tmp/enrol.pem -noout -serial)"
cp /tmp/enrol.pem /tmp/enrol.key /qa/ 2>/dev/null || true
'
# the revoke subcommand works on files in the certs volume, so put the enrolled certificate there under a name
docker compose cp qa:/qa/enrol.pem ./qa/revoketest-enrol.pem
docker compose cp ./qa/revoketest-enrol.pem takinit:/tmp/revoketest-enrol.pem 2>/dev/null || true
docker compose run --rm --no-deps --entrypoint /bin/bash -v "$PWD/qa:/hostqa" takinit -lc \
  'cp /hostqa/revoketest-enrol.pem /opt/tak/certs/files/revoketest-enrol.pem && chown 65532:65532 /opt/tak/certs/files/revoketest-enrol.pem && echo stored'

probe() { # $1 label, $2 cert base in the qa container
	say "PROBE $1"
	inqa bash -c '
	set +e
	C=$1
	echo "--- 8443 (client certificate web connector)"
	curl -sk --cert /qa/'"$2"'.pem --key /qa/'"$2"'.key -o /dev/null \
	     -w "    http=%{http_code} curl_exit=%{exitcode}\n" https://takserver-config:8443/Marti/api/version
	echo "    curl exit: $?"
	echo "--- 8089 (TLS input)"
	echo | openssl s_client -connect takserver-config:8089 -cert /qa/'"$2"'.pem -key /qa/'"$2"'.key \
	     -cert_chain /qa/intermediate-ca.pem -CAfile /qa/ca.pem -verify_return_error 2>&1 | grep -E "Verify return code|alert|handshake|Cipher is|SSL handshake has read" | head -5
	' _ "$1"
}

# make both certificates, their keys and the CA chain available to the runner. Always go through the init image:
# `docker compose cp takinit:` only works while a takinit container exists, and a partial copy (pem without key)
# produced a proof that failed BEFORE revocation on 2026-09-20 and therefore proved nothing.
docker compose run --rm --no-deps --entrypoint /bin/bash -v "$PWD/qa:/hostqa" takinit -lc \
  'cd /opt/tak/certs/files && cp revoketest-root.pem ca.pem intermediate-ca.pem /hostqa/ && openssl pkey -in revoketest-root.key -passin pass:"${CAPASS:-atakatak}" -out /hostqa/revoketest-root.key && chmod 644 /hostqa/revoketest-root.* /hostqa/ca.pem /hostqa/intermediate-ca.pem && echo exported'
for f in revoketest-root.pem revoketest-root.key ca.pem intermediate-ca.pem revoketest-enrol.pem; do
	docker compose cp "./qa/$f" "qa:/qa/$f"
done
docker compose cp ./qa/enrol.key qa:/qa/revoketest-enrol.key 2>/dev/null || true
# TAK's trust anchor is the root; an intermediate-issued leaf must be presented WITH the intermediate, exactly as the
# acceptance suite's check 8d does (leaf + ca chain), otherwise the handshake fails for a reason that is not revocation.
inqa bash -c 'cat /qa/revoketest-enrol.pem /qa/intermediate-ca.pem > /qa/revoketest-enrol-chain.pem; cp /qa/revoketest-enrol-chain.pem /qa/revoketest-enrol.pem; chmod 600 /qa/*.key; echo chain built'

say "1. BEFORE revocation"
probe "root-CA certificate BEFORE" revoketest-root
probe "enrolment certificate BEFORE" revoketest-enrol

say "2. revoke both"
docker compose run --rm takinit revoke revoketest-root root
docker compose run --rm takinit revoke revoketest-enrol intermediate

say "3. recreate the TAK containers so the TLS context rebuilds with the new CRLs"
docker compose up -d --force-recreate takserver-config takserver-messaging takserver-api
n=0; until docker compose ps --format '{{.Service}} {{.Status}}' | grep -q 'takserver-api.*(healthy)'; do
	sleep 15; n=$((n+1)); [ $n -lt 240 ] || { echo "api did not come back"; exit 1; }
	[ $((n % 8)) -eq 0 ] && echo "  waiting, $((n*15))s"
done

say "4. AFTER revocation"
probe "root-CA certificate AFTER" revoketest-root
probe "enrolment certificate AFTER" revoketest-enrol

say "4b. server-side evidence: revocation rejections in the TAK logs (messaging = 8089, api = 8443)"
docker compose run --rm --no-deps --entrypoint /bin/bash takinit -lc \
  'grep -h -i "rejected for revoked\|CertificateRevokedException\|revocation status" /opt/tak/logs/takserver-messaging.log /opt/tak/logs/takserver-api.log 2>/dev/null | tail -8'

say "5. CRL contents"
docker compose run --rm --no-deps --entrypoint /bin/bash takinit -lc \
  'for c in ca intermediate-ca; do echo "--- $c.crl"; openssl crl -in /opt/tak/certs/files/$c.crl -noout -text | grep -A3 "Revoked Certificate" | head -20; done'
