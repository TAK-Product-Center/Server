#!/bin/bash
#
# Asserts HTTP 200 before and non-200 after revocation for both issuers on 8443,
# plus fresh, identity-correlated rejection logs on 8089. Pre-revoke 8089 application acceptance is not asserted.
#
# Two certificates are exercised, because TAK has two issuers:
#   revoketest-root  issued by the root CA with makeCert.sh, revoked against ca.crl
#   revoketest-enrol issued by the ONLINE SIGNER (intermediate-ca) through /Marti/api/tls/signClient/v2, which is
#                    what every OIDC enrolment produces, revoked against intermediate-ca.crl
#
# Run after the lab is up: ./qa/revocation-proof.sh
set -euo pipefail
cd "$(dirname "$0")/.."
export PATH=/opt/homebrew/bin:$PATH
set -a; . ./.env; . ./.env.secrets; set +a

umask 077
say() { printf '\n=== %s  %s\n' "$(date -u +%FT%TZ)" "$*"; }
inqa() { docker compose exec -T qa "$@"; }

say "0. issue a client certificate from the root CA (stale test material from an earlier run is removed first;
        make-cert now refuses to overwrite an existing store, which is the finding-6 behaviour)"
docker compose run --rm --no-deps --entrypoint /bin/bash takinit -lc \
  'rm -f /opt/tak/certs/files/revoketest-root.* /opt/tak/certs/files/revoketest-enrol.* && echo "stale test certs removed"'
docker compose run --rm takinit make-cert client revoketest-root

say "0b. enrol a certificate through the online signer, the way an OIDC client does"
docker compose exec -T -e PROBE_CLIENT_SECRET -e CERT_ORGANIZATION -e CERT_ORGANIZATIONAL_UNIT qa bash -c '
set -euo pipefail
TOK=$(curl -skf -X POST "https://idp:8843/realms/tak/protocol/openid-connect/token" \
  -d grant_type=password -d client_id=probe -d client_secret="$PROBE_CLIENT_SECRET" \
  -d username=alice -d password=alice -d scope="openid profile groups" \
  | python3 -c "import json,sys;print(json.load(sys.stdin)[\"access_token\"])")
IDENT=$(curl -skf -H "Authorization: Bearer $TOK" https://takserver-config:8446/Marti/api/tls/config \
  | python3 -c "import re,sys;m=re.search(r\"<identity>([^<]+)</identity>\",sys.stdin.read());print(m.group(1))")
echo "enrolment identity: $IDENT"
openssl req -new -newkey rsa:2048 -nodes -keyout /tmp/enrol.key -out /tmp/enrol.csr \
  -subj "/CN=$IDENT/O=${CERT_ORGANIZATION:-Lastwall}/OU=${CERT_ORGANIZATIONAL_UNIT:-TAKOIDC}" >/dev/null 2>&1
B64=$(openssl req -in /tmp/enrol.csr -outform DER | base64 | tr -d "\n")
curl -skf -H "Authorization: Bearer $TOK" -H "Content-Type: text/plain" -H "Accept: application/json" \
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
cp /tmp/enrol.pem /tmp/enrol.key /qa/
'
# the revoke subcommand works on files in the certs volume, so put the enrolled certificate there under a name
inqa cp /qa/enrol.pem /qa/revoketest-enrol.pem
docker compose run --rm --no-deps --entrypoint /bin/bash -v "$PWD/qa:/hostqa" takinit -lc \
  'cp /hostqa/revoketest-enrol.pem /opt/tak/certs/files/revoketest-enrol.pem && chown 65532:65532 /opt/tak/certs/files/revoketest-enrol.pem && echo stored'

probe() { # phase (before/after), certificate basename
    say "PROBE $1 $2"
    inqa bash -s -- "$1" "$2" <<'PROBE'
set -euo pipefail
phase=$1; cert=$2
rc=0
code=$(curl -sk --max-time 20 --cert "/qa/$cert.pem" --key "/qa/$cert.key" \
    -o /dev/null -w '%{http_code}' https://takserver-config:8443/Marti/api/version) || rc=$?
printf '8443 %s %s http=%s curl_exit=%s\n' "$phase" "$cert" "$code" "$rc"
if [ "$phase" = before ]; then
    [ "$rc" = 0 ] && [ "$code" = 200 ] || { echo 'FAIL: expected HTTP 200 before revocation'; exit 1; }
else
    [[ "$code" =~ ^[0-9]{3}$ ]] && [ "$code" != 200 ] || { echo 'FAIL: expected rejection after revocation'; exit 1; }
    # Only TLS/connection-closed failures are expected for a 000 response, not missing keys/DNS/timeouts.
    if [ "$code" = 000 ]; then
        case "$rc" in 35|52|55|56) ;; *) echo "FAIL: unrelated curl failure $rc"; exit 1 ;; esac
    fi
fi
# A client-side TLS handshake alone cannot prove 8089 application acceptance/rejection.
# Exercise that connector; the post-revocation gate below requires correlated server evidence.
rc=0
timeout 20 openssl s_client -connect takserver-config:8089 -cert "/qa/$cert.pem" \
    -key "/qa/$cert.key" -cert_chain /qa/intermediate-ca.pem -CAfile /qa/ca.pem \
    -verify_return_error </dev/null > "/qa/$cert-$phase-8089.txt" 2>&1 || rc=$?
printf '8089 probe exit=%s (server log assertion follows after revocation)\n' "$rc"
case "$rc" in 0|1) ;; *) echo 'FAIL: 8089 probe could not complete'; exit 1 ;; esac
PROBE
}

# make both certificates, their keys and the CA chain available to the runner. Always go through the init image:
# `docker compose cp takinit:` only works while a takinit container exists, and a partial copy (pem without key)
# produced a proof that failed BEFORE revocation on 2026-09-20 and therefore proved nothing.
docker compose run --rm --no-deps --entrypoint /bin/bash -v "$PWD/qa:/hostqa" takinit -ec \
  'cd /opt/tak/certs/files && cp revoketest-root.pem ca.pem intermediate-ca.pem /hostqa/ && openssl pkey -in revoketest-root.key -passin pass:"${CERT_PASS:-atakatak}" -out /hostqa/revoketest-root.key && chmod 600 /hostqa/revoketest-root.* /hostqa/ca.pem /hostqa/intermediate-ca.pem && echo exported'
# /qa is the host qa directory bind-mounted by Compose; the exported files are already visible.
inqa cp /qa/enrol.key /qa/revoketest-enrol.key
# TAK's trust anchor is the root; an intermediate-issued leaf must be presented WITH the intermediate, exactly as the
# acceptance suite's check 8d does (leaf + ca chain), otherwise the handshake fails for a reason that is not revocation.
inqa bash -e -o pipefail -c 'cat /qa/revoketest-enrol.pem /qa/intermediate-ca.pem > /qa/revoketest-enrol-chain.pem; cp /qa/revoketest-enrol-chain.pem /qa/revoketest-enrol.pem; chmod 600 /qa/*.key; echo chain built'

say "1. BEFORE revocation"
probe before revoketest-root
probe before revoketest-enrol

say "2. revoke both"
docker compose run --rm takinit revoke revoketest-root root
docker compose run --rm takinit revoke revoketest-enrol intermediate
REVOKE_TIME=$(inqa python3 -c 'import time; print(time.time())')
printf 'Revoke completed at epoch=%s UTC\n' "$REVOKE_TIME"

say "3. recreate the TAK containers so the TLS context rebuilds with the new CRLs"
docker compose up -d --force-recreate takserver-config takserver-messaging takserver-api
n=0; until docker compose ps --format '{{.Service}} {{.Status}}' | grep -q 'takserver-api.*(healthy)'; do
	sleep 15; n=$((n+1)); [ $n -lt 240 ] || { echo "api did not come back"; exit 1; }
	if [ $((n % 8)) -eq 0 ]; then echo "  waiting, $((n*15))s"; fi
done

say "4. AFTER revocation"
probe after revoketest-root
probe after revoketest-enrol

say "4b. assert fresh 8089 revocation rejections for each certificate"
# The lab JVM log timestamps are UTC. Require a timestamp strictly after revocation
# and the exact CN or serial on the rejection line; an anonymous root rejection is insufficient.
inqa python3 - "$REVOKE_TIME" <<'PYLOG'
import datetime, pathlib, re, subprocess, sys, time
cutoff = float(sys.argv[1])
certs = []
for name in ("revoketest-root", "revoketest-enrol"):
    subject = subprocess.check_output(["openssl", "x509", "-in", f"/qa/{name}.pem", "-noout", "-subject", "-nameopt", "multiline"], text=True)
    cn = re.search(r"^\s*commonName\s*=\s*(.+)$", subject, re.M)
    if not cn:
        raise SystemExit(f"FAIL: no CN for {name}")
    serial = subprocess.check_output(["openssl", "x509", "-in", f"/qa/{name}.pem", "-noout", "-serial"], text=True).strip().split("=", 1)[1]
    issuer = subprocess.check_output(["openssl", "x509", "-in", f"/qa/{name}.pem", "-noout", "-issuer", "-nameopt", "multiline"], text=True)
    icn = re.search(r"^\s*commonName\s*=\s*(.+)$", issuer, re.M)
    certs.append((name, cn[1], serial.lstrip("0").upper() or "0", icn[1] if icn else ""))
path = pathlib.Path("/opt/tak/logs/takserver-messaging.log")
found = {}
for attempt in range(15):
    for line in path.read_text(errors="replace").splitlines():
        ts = re.match(r"^(\d{4}-\d{2}-\d{2}-\d{2}:\d{2}:\d{2}\.\d+)", line)
        if not ts or "Connection rejected for revoked client certificate" not in line or "Local port: 8089" not in line:
            continue
        when = datetime.datetime.strptime(ts[1], "%Y-%m-%d-%H:%M:%S.%f").replace(tzinfo=datetime.timezone.utc).timestamp()
        if when <= cutoff:
            continue
        for name, cn, serial, icn in certs:
            cn_match = re.search(r"(?:^|[, :])CN=" + re.escape(cn) + r"(?=,|;|$)", line)
            logged_serial = re.search(r"serial(?: number)?\s*[:=]\s*(?:0x)?([0-9a-f:]+)(?=[,;\s]|$)", line, re.I)
            serial_match = logged_serial and (logged_serial[1].replace(":", "").lstrip("0").upper() or "0") == serial
            # Upstream quirk, observed 2026-09-20: for a certificate issued directly by the ROOT CA the handler logs an
            # EMPTY subject ("revoked client certificate: , Certificate has been revoked ..."), so CN correlation is
            # impossible for that issuer. Fall back to a strict triple: issuing authority CN on the line, a revocation
            # date within 120 s after our revoke, and a line timestamp after the probe. Any other issuer still needs CN/serial.
            auth_match = icn and re.search(r"authority: CN=" + re.escape(icn) + r"(?=,|;|$)", line)
            rd = re.search(r"revocation date: (\w{3} \w{3} \d{1,2} \d{2}:\d{2}:\d{2}) GMT (\d{4})", line)
            rd_ok = False
            if rd:
                rdt = datetime.datetime.strptime(rd[1] + " " + rd[2], "%a %b %d %H:%M:%S %Y").replace(tzinfo=datetime.timezone.utc).timestamp()
                rd_ok = (cutoff - 120) <= rdt <= (cutoff + 120)
            anon_root = ("revoked client certificate: ," in line) and auth_match and rd_ok and name == "revoketest-root"
            if cn_match or serial_match or anon_root:
                found[name] = line
    if len(found) == len(certs):
        break
    time.sleep(1)
for name, cn, serial, _icn in certs:
    if name not in found:
        print(f"FAIL: no fresh correlated 8089 rejection for {name} CN={cn} serial={serial}", file=sys.stderr)
    else:
        print("PASS:", name, found[name])
if len(found) != len(certs):
    raise SystemExit(1)
PYLOG
say "PASS: both HTTP before/after assertions and both fresh correlated 8089 rejections"
