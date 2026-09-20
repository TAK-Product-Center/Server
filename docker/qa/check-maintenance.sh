#!/bin/bash
# Local regression checks for issuer isolation and failure propagation. No Docker or network access.
set -euo pipefail
cd "$(dirname "$0")/.."
PACKAGE=$PWD
WORK=$(mktemp -d "$PACKAGE/qa/.maintenance-check.XXXXXX")
trap 'rm -rf "$WORK"' EXIT
export TAK_DIR="$WORK/tak" CERT_PASS=test-password TAK_UID="$(id -u)" TAK_GID="$(id -g)"
mkdir -p "$TAK_DIR/certs/files"
F="$TAK_DIR/certs/files"
cat > "$TAK_DIR/certs/config.cfg" <<'CFG'
default_crl_days = 30
[ca]
default_ca = CA_default
[CA_default]
dir = .
database = $dir/crl_index.txt
default_md = sha256
CFG
: > "$F/crl_index.txt"
cat > "$WORK/ca.ext" <<'CFG'
basicConstraints=critical,CA:TRUE
keyUsage=critical,keyCertSign,cRLSign
CFG
openssl req -x509 -newkey rsa:2048 -nodes -subj /CN=test-root -days 1 \
  -keyout "$F/ca-do-not-share.key" -out "$F/ca.pem" \
  -addext basicConstraints=critical,CA:TRUE -addext keyUsage=critical,keyCertSign,cRLSign > "$WORK/setup.log" 2>&1
openssl req -new -newkey rsa:2048 -nodes -subj /CN=test-intermediate \
  -keyout "$F/intermediate-ca.key" -out "$WORK/intermediate.csr" >> "$WORK/setup.log" 2>&1
openssl x509 -req -in "$WORK/intermediate.csr" -CA "$F/ca.pem" -CAkey "$F/ca-do-not-share.key" \
  -set_serial 10 -days 1 -extfile "$WORK/ca.ext" -out "$F/intermediate-ca.pem" >> "$WORK/setup.log" 2>&1
issue() {
  local name=$1 ca=$2 key=$3 serial=$4
  openssl req -new -newkey rsa:2048 -nodes -subj "/CN=$name" -keyout "$F/$name.key" \
    -out "$WORK/$name.csr" >> "$WORK/setup.log" 2>&1
  openssl x509 -req -in "$WORK/$name.csr" -CA "$F/$ca.pem" -CAkey "$F/$key.key" \
    -set_serial "$serial" -days 1 -out "$F/$name.pem" >> "$WORK/setup.log" 2>&1
}
issue root-client ca ca-do-not-share 101
issue intermediate-client intermediate-ca intermediate-ca 202
# Colliding serials across issuers must not be copied into both indexes.
issue same-serial intermediate-ca intermediate-ca 101
# Load definitions only in separate bash invocations to preserve real errexit behavior.
sed '/^############################################################################### dispatch/,$d' init/init.sh > "$WORK/functions.sh"
bash -c 'source "$1"; export_cert_env; regen_crl root; regen_crl intermediate-ca; refresh_crl_bundle' _ "$WORK/functions.sh" > "$WORK/bootstrap.log" 2>&1
if bash init/init.sh revoke root-client intermediate > "$WORK/mismatch.log" 2>&1; then
  echo 'FAIL: issuer mismatch succeeded'; exit 1
fi
[ ! -s "$F/crl-index-root.txt" ] && [ ! -s "$F/crl-index-intermediate-ca.txt" ]
bash init/init.sh revoke root-client > "$WORK/root.log" 2>&1
[ ! -s "$F/crl-index-intermediate-ca.txt" ]
bash init/init.sh revoke intermediate-client > "$WORK/intermediate.log" 2>&1
awk -F '\t' '$4 != "65" { exit 1 } END { if (NR != 1) exit 1 }' "$F/crl-index-root.txt"
awk -F '\t' '$4 != "CA" { exit 1 } END { if (NR != 1) exit 1 }' "$F/crl-index-intermediate-ca.txt"
bash init/init.sh revoke same-serial > "$WORK/collision.log" 2>&1
[ "$(wc -l < "$F/crl-index-root.txt" | tr -d ' ')" = 1 ]
[ "$(wc -l < "$F/crl-index-intermediate-ca.txt" | tr -d ' ')" = 2 ]
if bash -c 'source "$1"; verify_crl "$2/ca.crl" "$2/intermediate-ca.pem"' _ "$WORK/functions.sh" "$F" > "$WORK/wrong-signature.log" 2>&1; then
  echo 'FAIL: wrong CRL signer accepted'; exit 1
fi
# Upstream-style scripts must stop at keytool and PKCS12 failures even with a PEM already present.
for failure in keytool pkcs12; do
  cat > "$TAK_DIR/certs/makeCert.sh" <<'SCRIPT'
#!/bin/bash
openssl list -providers 2>&1 | grep "\(invalid command\|unknown option\)" >/dev/null
if [ $? -ne 0 ] ; then
  :
fi
SCRIPT
  if [ "$failure" = keytool ]; then
    printf 'keytool() { echo "injected keytool exception" >&2; return 29; }; keytool -importkeystore\n' >> "$TAK_DIR/certs/makeCert.sh"
  else
    printf 'openssl() { echo "injected pkcs12 failure" >&2; return 30; }; openssl pkcs12\n' >> "$TAK_DIR/certs/makeCert.sh"
  fi
  printf 'echo SHOULD_NOT_REACH\n' >> "$TAK_DIR/certs/makeCert.sh"
  if bash init/init.sh make-cert client root-client > "$WORK/$failure.log" 2>&1; then
    echo "FAIL: $failure error succeeded"; exit 1
  fi
  if grep -Eq 'SHOULD_NOT_REACH|issued client certificate' "$WORK/$failure.log"; then
    echo "FAIL: continued after $failure error"; exit 1
  fi
done
# A nonempty shared index must not silently seed a new issuer database.
mkdir -p "$WORK/legacy/certs/files"
printf 'legacy revoked entry\n' > "$WORK/legacy/certs/files/crl_index.txt"
if TAK_DIR="$WORK/legacy" bash -c 'source "$1"; cfg=$(ca_config root); echo SHOULD_NOT_REACH' _ "$WORK/functions.sh" > "$WORK/legacy.log" 2>&1; then
  echo 'FAIL: legacy mixed index accepted'; exit 1
fi
printf 'PASS: issuer mismatch, auto-selection, isolated indexes, serial collision, CRL signer, keytool/PKCS12 failures, legacy-index refusal\n'
