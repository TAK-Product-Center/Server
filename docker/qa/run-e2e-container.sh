#!/usr/bin/env bash
# Container copy of qa/e2e/run-e2e-v02.sh from the phase 1 request directory. TWO lines differ, both about WHICH
# connector a probe talks to, never about what it asserts:
#   1. a new TAK_CERT variable, default https://localhost:8443, the connector that demands a client certificate;
#   2. check 8d now uses $TAK_CERT instead of the hard coded https://localhost:8443, which inside the runner
#      container would point at the runner itself.
# The container run sets TAK=8446 (the connector the IdP redirect URI is registered on, clientAuth=false) and
# TAK_CERT=8443, so check 8d is still a client certificate connection on the client certificate connector.
# No check was removed, relaxed or reordered; diff it against the phase 1 original.
# End-to-end acceptance probes for TAK OIDC SSO Phase 1 against a LOCAL TAK Server + Keycloak.
#
# Preconditions (qa/runtime/RUNTIME.md): Keycloak realm "tak" on $KC, TAK Server on $TAK (8443 web, 8446 bearer),
# CoreConfig with the keycloak discovery provider, groupMapping tak_ops -> Operations, tak_admin -> Operations,
# adminTokenClaimName=groups adminTokenClaimValue=tak_admin. Users alice (tak_ops), carol (tak_admin+tak_ops),
# bob (no groups). A confidential client "probe" with Direct Access Grants mints bearer tokens for the API probes.
#
# Every probe prints PASS/FAIL with the HTTP evidence and appends to $OUT. The script never prints a raw token.
set -u
KC="${KC:-https://localhost:8843}"
REALM="${REALM:-tak}"
TAK="${TAK:-https://localhost:8443}"
TAK_BEARER="${TAK_BEARER:-https://localhost:8446}"
# The connector that demands a client certificate. Check 8d must use it; in the container the browser flow lives on
# 8446 because that is where the IdP redirect URI is registered, so $TAK and $TAK_CERT are different connectors.
TAK_CERT="${TAK_CERT:-https://localhost:8443}"
PROBE_CLIENT="${PROBE_CLIENT:-probe}"
PROBE_SECRET="${PROBE_SECRET:?set PROBE_SECRET (Keycloak client 'probe' secret)}"
TAK_LOG="${TAK_LOG:-}"            # path to the TAK server log that carries tak.server.oidc.audit lines
OUT="${OUT:-$(dirname "$0")/e2e-results-$(date -u +%Y%m%dT%H%M%SZ).md}"
if [ -n "${KEEP_WORK:-}" ]; then WORK="$KEEP_WORK"; mkdir -p "$WORK"; else WORK="$(mktemp -d)"; fi
pass=0; fail=0

say() { printf '%s\n' "$*" | tee -a "$OUT"; }
result() { # $1 name, $2 ok(0/1), $3 evidence
  if [ "$2" = 0 ]; then pass=$((pass+1)); say "PASS  $1  ($3)"; else fail=$((fail+1)); say "FAIL  $1  ($3)"; fi
}
mint() { # $1 user $2 password -> prints access token (never logged)
  curl -sk -X POST "$KC/realms/$REALM/protocol/openid-connect/token" \
    -d grant_type=password -d client_id="$PROBE_CLIENT" -d client_secret="$PROBE_SECRET" \
    -d username="$1" -d password="$2" -d scope="openid profile groups" | python3 -c 'import json,sys; print(json.load(sys.stdin).get("access_token",""))'
}
mint_id() { # ID token instead of access token
  curl -sk -X POST "$KC/realms/$REALM/protocol/openid-connect/token" \
    -d grant_type=password -d client_id="$PROBE_CLIENT" -d client_secret="$PROBE_SECRET" \
    -d username="$1" -d password="$2" -d scope="openid profile groups" | python3 -c 'import json,sys; print(json.load(sys.stdin).get("id_token",""))'
}
claims() { python3 -c 'import sys,base64,json; t=sys.argv[1].split(".")[1]; t+="="*(-len(t)%4); print(json.dumps(json.loads(base64.urlsafe_b64decode(t)),sort_keys=True))' "$1"; }
bearer_code() { # $1 token $2 path -> http code
  curl -sk -o "$WORK/body" -w '%{http_code}' -H "Authorization: Bearer $1" "$TAK_BEARER$2"
}
audit_tail() { [ -n "$TAK_LOG" ] && grep -h "outcome=" "$TAK_LOG" | tail -n 3 | sed 's/^/    audit: /' | tee -a "$OUT" >/dev/null; return 0; }

say "# E2E run $(date -u +%FT%TZ) KC=$KC TAK=$TAK"

# ---- 0. IdP is what we think it is -------------------------------------------------------------------
disc=$(curl -sk "$KC/realms/$REALM/.well-known/openid-configuration")
iss=$(printf '%s' "$disc" | python3 -c 'import json,sys; print(json.load(sys.stdin)["issuer"])')
pkce=$(printf '%s' "$disc" | python3 -c 'import json,sys; print("S256" in json.load(sys.stdin).get("code_challenge_methods_supported",[]))')
result "0a IdP discovery reachable, issuer=$iss" $([ -n "$iss" ] && echo 0 || echo 1) "issuer=$iss pkce_s256=$pkce"

# ---- 1. TAK advertises the provider to clients (AN-10) ------------------------------------------------
wk=$(curl -sk "$TAK_BEARER/login/.well-known/openid-configuration")
wk_iss=$(printf '%s' "$wk" | python3 -c 'import json,sys; d=json.load(sys.stdin); print(d.get("issuer",""), d.get("android_client_id",""), d.get("scopes",""))' 2>/dev/null)
result "1  /login/.well-known carries issuer, android_client_id, scopes" $([ "$(printf '%s' "$wk_iss" | awk '{print $1}')" = "$iss" ] && echo 0 || echo 1) "$wk_iss"
# v0.2 (review v02 D2): scopes must be a JSON array and android_redirect one of scheme|loopback
wk_v02=$(printf '%s' "$wk" | python3 -c 'import json,sys; d=json.load(sys.stdin); s=d.get("scopes"); r=d.get("android_redirect"); print("list" if isinstance(s,list) else type(s).__name__, json.dumps(s), r)' 2>/dev/null)
result "1b /login/.well-known scopes is a JSON array (D2)" $([ "$(printf '%s' "$wk_v02" | awk '{print $1}')" = list ] && echo 0 || echo 1) "scopes=$(printf '%s' "$wk_v02" | awk '{$1="";$NF="";print}' | xargs)"
ar=$(printf '%s' "$wk_v02" | awk '{print $NF}')
result "1c /login/.well-known android_redirect is scheme|loopback (D2)" $([ "$ar" = scheme ] || [ "$ar" = loopback ] && echo 0 || echo 1) "android_redirect=$ar"

# ---- 2. Bearer path: mapped user is allowed with mapped groups (test 2/5, T9 positive) -------------------
alice=$(mint alice alice)
[ -n "$alice" ] || { say "FATAL could not mint alice"; exit 2; }
say "    alice claims: $(claims "$alice" | python3 -c 'import json,sys; d=json.load(sys.stdin); print({k:d.get(k) for k in ("iss","aud","azp","groups","preferred_username","sub")})')"
code=$(bearer_code "$alice" /Marti/api/tls/config)
result "2a alice Bearer -> /Marti/api/tls/config" $([ "$code" = 200 ] && echo 0 || echo 1) "http=$code"
ident=$(python3 -c 'import re,sys; s=open(sys.argv[1]).read(); m=re.search(r"identity[\"=>]+([^<\"]+)",s); print(m.group(1) if m else "")' "$WORK/body" 2>/dev/null)
result "2b tls/config carries identity (iss+sub key)" $([ -n "$ident" ] && echo 0 || echo 1) "identity=$ident"
code=$(bearer_code "$alice" /Marti/api/groups/all)
groups=$(python3 -c 'import json,sys; d=json.load(open(sys.argv[1])); print(sorted({g.get("name") for g in d.get("data",[])}))' "$WORK/body" 2>/dev/null)
result "2c alice groups are exactly the mapped set" $([ "$groups" = "['Operations']" ] && echo 0 || echo 1) "http=$code groups=$groups"
audit_tail

# ---- 3. Deny by default (test 6, T9) ----------------------------------------------------------------------
bob=$(mint bob bob)
code=$(bearer_code "$bob" /Marti/api/tls/config)
result "3  bob (no groups) is denied" $([ "$code" = 401 ] || [ "$code" = 403 ] && echo 0 || echo 1) "http=$code"
audit_tail

# ---- 4. Admin only via explicit mapping (test 3, T10) -------------------------------------------------------
carol=$(mint carol carol)
code=$(bearer_code "$carol" /Marti/api/inputs)   # an admin-only endpoint; 200 for admin
result "4a carol (tak_admin claim) reaches an admin endpoint" $([ "$code" = 200 ] && echo 0 || echo 1) "http=$code"
code=$(bearer_code "$alice" /Marti/api/inputs)
result "4b alice (no admin claim) is refused the admin endpoint" $([ "$code" = 403 ] || [ "$code" = 401 ] && echo 0 || echo 1) "http=$code"

# ---- 5. Planted bad tokens (test 9, T1 T2 T3 T4 T5 T8) -----------------------------------------------------
py_tamper() { python3 - "$1" "$2" <<'PY'
import sys,base64,json
tok,mode=sys.argv[1],sys.argv[2]
h,p,s=tok.split(".")
def dec(x): x+="="*(-len(x)%4); return json.loads(base64.urlsafe_b64decode(x))
def enc(d): return base64.urlsafe_b64encode(json.dumps(d,separators=(",",":")).encode()).decode().rstrip("=")
hd,pl=dec(h),dec(p)
if mode=="aud": pl["aud"]="someone-else"; pl.pop("azp",None)
if mode=="iss": pl["iss"]="https://evil.example/realms/tak"
if mode=="exp": pl["exp"]=pl["iat"]-3600
if mode=="none": hd["alg"]="none"; print(enc(hd)+"."+enc(pl)+"."); sys.exit()
if mode=="hs256": hd["alg"]="HS256"
if mode=="kid": hd["kid"]="no-such-key"
if mode=="sig": s=s[:-6]+("AAAAAA" if not s.endswith("AAAAAA") else "BBBBBB")
print(enc(hd)+"."+enc(pl)+"."+s)
PY
}
for mode in aud iss exp none hs256 kid sig; do
  bad=$(py_tamper "$alice" "$mode")
  code=$(bearer_code "$bad" /Marti/api/tls/config)
  result "5  planted $mode token is rejected" $([ "$code" = 401 ] && echo 0 || echo 1) "http=$code"
done
audit_tail
if [ -n "$TAK_LOG" ]; then
  leak=$(grep -c -F "$(printf '%s' "$alice" | cut -c1-40)" "$TAK_LOG" || true)
  result "5z no token substring in the server log (T8)" $([ "$leak" = 0 ] && echo 0 || echo 1) "hits=$leak"
fi

# ---- 6. ID token as the bearer assertion (IDCommand shape: ID token carries the claims) ---------------------
aid=$(mint_id alice alice)
code=$(bearer_code "$aid" /Marti/api/tls/config)
result "6  alice ID token accepted as bearer (aud=probe must be a configured audience for this to pass)" $([ "$code" = 200 ] && echo 0 || echo 1) "http=$code"

# ---- 7. Browser flow with PKCE, nonce, consumed state (tests 2, T6, T7) -------------------------------------
jar="$WORK/jar"; rm -f "$jar"
auth=$(curl -sk -c "$jar" -o /dev/null -w '%{redirect_url}' "$TAK/login/auth")
has_pkce=$(printf '%s' "$auth" | grep -c 'code_challenge=' || true)
has_nonce=$(printf '%s' "$auth" | grep -c 'nonce=' || true)
result "7a /login/auth redirects to the IdP with code_challenge and nonce" $([ "$has_pkce" = 1 ] && [ "$has_nonce" = 1 ] && echo 0 || echo 1) "challenge=$has_pkce nonce=$has_nonce"
# Keycloak login form: GET the authorization URL, POST the form action with credentials, follow to TAK.
form=$(curl -sk -c "$jar" -b "$jar" "$auth")
action=$(printf '%s' "$form" | python3 -c 'import re,sys,html; m=re.search(r"action=\"([^\"]+)\"",sys.stdin.read()); print(html.unescape(m.group(1)) if m else "")')
cb=$(curl -sk -c "$jar" -b "$jar" -o /dev/null -w '%{redirect_url}' -X POST "$action" -d username=alice -d password=alice -d credentialId=)
# The registered redirect URI must be on the connector browsers use (8446 in the lab, clientAuth=false); the
# callback is followed exactly as the IdP issued it.
has_code=$(printf '%s' "$cb" | grep -c 'code=' || true)
result "7b IdP login redirects back to TAK with a code" $([ "$has_code" = 1 ] && echo 0 || echo 1) "callback=$(printf '%s' "$cb" | sed 's/code=[^&]*/code=REDACTED/')"
code=$(curl -sk -c "$jar" -b "$jar" -o "$WORK/cb1" -w '%{http_code}' "$cb")
result "7c first callback completes (cookie set)" $([ "$code" = 200 ] || [ "$code" = 302 ] && echo 0 || echo 1) "http=$code"
code=$(curl -sk -b "$jar" -o "$WORK/body" -w '%{http_code}' "$TAK/Marti/api/groups/all")
groups=$(python3 -c 'import json,sys; d=json.load(open(sys.argv[1])); print(sorted({g.get("name") for g in d.get("data",[])}))' "$WORK/body" 2>/dev/null)
result "7d session cookie reaches TAK with the mapped groups" $([ "$groups" = "['Operations']" ] && echo 0 || echo 1) "http=$code groups=$groups"
code=$(curl -sk -c "$WORK/jar2" -b "$jar" -o /dev/null -w '%{http_code}' "$cb")
result "7e replayed callback is rejected (T7)" $([ "$code" = 500 ] || [ "$code" = 400 ] || [ "$code" = 401 ] && echo 0 || echo 1) "http=$code"
audit_tail

# ---- 8. Bearer enrollment then a certificate connection (test 4 server half, T21, decision 4) ---------------
CSR_O="${CSR_O:-Lastwall}"; CSR_OU="${CSR_OU:-TAKOIDC}"     # must equal the configured certificateSigning nameEntries
code=$(bearer_code "$alice" /Marti/api/tls/config)
ident=$(python3 -c 'import re,sys; s=open(sys.argv[1]).read(); m=re.search(r"<identity>([^<]+)</identity>",s); print(m.group(1) if m else "")' "$WORK/body" 2>/dev/null)
result "8a tls/config carries identity" $([ "$code" = 200 ] && [ -n "$ident" ] && echo 0 || echo 1) "http=$code identity=$ident"
openssl req -new -newkey rsa:2048 -nodes -keyout "$WORK/e.key" -out "$WORK/e.csr" -subj "/CN=$ident/O=$CSR_O/OU=$CSR_OU" >/dev/null 2>&1
csr_b64=$(openssl req -in "$WORK/e.csr" -outform DER 2>/dev/null | base64 | tr -d '\n')
code=$(curl -sk -o "$WORK/signed.json" -w '%{http_code}' -H "Authorization: Bearer $alice" -H 'Content-Type: text/plain' -H 'Accept: application/json' --data-binary "$csr_b64" "$TAK_BEARER/Marti/api/tls/signClient/v2?clientUid=e2e-$$&version=2")
result "8b signClient/v2 with the bearer assertion issues a certificate" $([ "$code" = 200 ] && echo 0 || echo 1) "http=$code"
python3 - "$WORK" <<'PY'
import json,base64,sys
w=sys.argv[1]; j=json.load(open(w+"/signed.json"))
open(w+"/e.der","wb").write(base64.b64decode(j["signedCert"]))
for k in ("ca0","ca1"):
    if k in j: open(w+"/"+k+".der","wb").write(base64.b64decode(j[k]))
PY
openssl x509 -inform DER -in "$WORK/e.der" -out "$WORK/e.pem" 2>/dev/null
cn=$(openssl x509 -in "$WORK/e.pem" -noout -subject 2>/dev/null)
result "8c issued certificate CN is the identity key" $(printf '%s' "$cn" | grep -q "$ident" && echo 0 || echo 1) "$cn"
cat "$WORK/e.pem" > "$WORK/chain.pem"; for k in ca0 ca1; do [ -f "$WORK/$k.der" ] && openssl x509 -inform DER -in "$WORK/$k.der" >> "$WORK/chain.pem" 2>/dev/null; done
code=$(curl -sk --cert "$WORK/chain.pem" --key "$WORK/e.key" -o "$WORK/body" -w '%{http_code}' "$TAK_CERT/Marti/api/groups/all")
groups=$(python3 -c 'import json,sys; d=json.load(open(sys.argv[1])); print(sorted({g.get("name") for g in d.get("data",[])}))' "$WORK/body" 2>/dev/null)
result "8d certificate connection on 8443 carries exactly the mapped groups" $([ "$code" = 200 ] && [ "$groups" = "['Operations']" ] && echo 0 || echo 1) "http=$code groups=$groups"
if [ -n "$TAK_LOG" ]; then
  certaudit=$(grep -c "surface=CERT.*user=$ident" "$TAK_LOG" || true)
  result "8e audit line with surface=CERT for that identity" $([ "$certaudit" -ge 1 ] && echo 0 || echo 1) "lines=$certaudit"
fi

say ""
say "TOTAL pass=$pass fail=$fail  (results: $OUT)"
[ -n "${KEEP_WORK:-}" ] || rm -rf "$WORK"
[ "$fail" = 0 ]
