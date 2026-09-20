# Running the minimal-footprint TAK Server container

TAK Server 5.7-RELEASE-14 from branch `lastwall/oidc-phase1`, OIDC SSO Phase 1 included. Four images, all built from
`docker/Dockerfile` in this repository and all pinned to base image digests.

| Image | Base | Runs | In the tarball |
|---|---|---|---|
| `takserver-oidc` | `gcr.io/distroless/java17-debian12:nonroot` | the three TAK processes (config, messaging, api) | yes |
| `takserver-oidc-schemamanager` | same distroless | the flyway migrations, once | yes |
| `takserver-oidc-init` | `alpine:3.22` + bash, openssl, headless JRE | certificates, CoreConfig.xml, and all certificate maintenance | yes |
| `takserver-oidc-qa` | `alpine:3.22` + bash, curl, python3, openssl | the acceptance runner, lab only | yes |

Read `DESIGN-NOTES.md` before changing the topology or the base image. Section 9 of this file lists every known gap.

## 1. Load and verify

```
shasum -a 256 -c SHA256SUMS
docker load -i takserver-oidc.oci.tar
docker image ls | grep takserver-oidc
```

`RELEASE-MANIFEST.md` records artifact identities and historical acceptance transcripts. It does not establish
exact-byte acceptance of the current files: see the tested-versus-delivered init image note in section 9.

## 2. Configure

```
cp .env.example .env
$EDITOR .env
```

The values that matter:

- `DB_PASSWORD` - the postgres password. No default.
- `BIND_ADDR` - **defaults to `127.0.0.1`**. Publishing 8089/8443/8446 on other interfaces is a deliberate act; see
  section 7.
- `SERVER_CN` - common name of the server certificate. It has to be the hostname clients and the IdP use.
- `CERT_PASS` - keystore password, `atakatak` by default. That is upstream's default in
  `src/takserver-core/scripts/certs/cert-metadata.sh` and what upstream's own documentation and data packages assume;
  it is not a protocol requirement. Change it, and change it in the data packages you hand to clients.
- `OIDC_ENABLED=true` plus `OIDC_CLIENT_ID`, `OIDC_CLIENT_SECRET`, `OIDC_ISSUER`, `OIDC_CONFIGURATION_URI`,
  `OIDC_REDIRECT_URI`, `OIDC_AUDIENCE`, `OIDC_ANDROID_CLIENT_ID`, `OIDC_GROUP_MAPPINGS`.

**Secrets.** `DB_PASSWORD`, `CERT_PASS` and `OIDC_CLIENT_SECRET` each have a `_FILE` variant that takes precedence:
`DB_PASSWORD_FILE`, `CERT_PASS_FILE`, `OIDC_CLIENT_SECRET_FILE`. Point them at a mounted file, for example a compose
secret at `/run/secrets/oidc_client_secret`, and the value never appears in `docker inspect`. The rendered
`CoreConfig.xml` contains the database password and the client secret in clear text; the init container writes it
`0600` owned by uid 65532 with `umask 077`, and `certs/files` is `0700`. To rotate: change the value (or the file),
run `docker compose run --rm takconfig update-oidc` for the client secret, then recreate the three TAK containers.
**Despite its name, `update-oidc` regenerates ALL of `CoreConfig.xml` from the template and current environment.**
It saves a timestamped backup but replaces manual connector, auth, repository and other XML edits. The init image
has no python3 XML editor; review the full generated configuration and restore any required customizations before
recreating the TAK containers. Keep every required setting available during regeneration, including DB credentials.
Build contexts exclude `.env`, `.env.*`, `certs/files/` and any key material (`docker/Dockerfile.dockerignore`).

## 3. First start

`docker compose up -d` runs, in dependency order:

1. `db`, to healthy.
2. `takinit` (`certs`): the CA, the server certificate, the `intermediate-ca` online signer, an `admin` client
   certificate, the two CRLs, and any `EXTRA_SERVER_CERTS`. `need_cert` checks only whether each PEM exists.
   A missing PEM triggers issuance; an existing PEM with missing JKS/P12/signing stores requires operator recovery.
   Startup verification checks artifact existence, not key matching or store usability. Restore missing companions
   from a known-good backup or repair that certificate deliberately; do not delete the CA or database to recover.
3. `takconfig` (`config`): renders `CoreConfig.xml`, then `verify` fails the start if any artifact is missing, any
   template token survived, or the OIDC client secret is empty.
4. `schemamanager`: flyway migrations, then exits.
5. `takserver-config`, `takserver-messaging`, `takserver-api`.

**`takconfig` refuses to render an OIDC configuration with an empty client secret.** That is deliberate: such a
configuration boots, serves the discovery document and then fails every authorization code exchange with HTTP 500.
If your IdP does not exist yet, use the ordering in section 6, or set `OIDC_ENABLED=false` and enable it later with
`update-oidc`.

Useful commands:

```
docker compose logs -f takserver-api
docker compose run --rm --no-deps takconfig verify
docker compose exec takserver-api java -cp /opt/takserver/health Health tcp:127.0.0.1:8446
```

Log files live in the `takdata` volume at `/opt/tak/logs/takserver-{config,messaging,api}.log`. OIDC audit lines carry
the `tak.server.oidc.audit` logger and an `outcome=` field.

## 4. Certificates and the admin enrolment

All certificate work goes through the init image, which has bash, openssl and keytool and which exports the variables
upstream's scripts require. Use the named subcommands for issuance and revocation. The explicit truststore
import below uses its own strict shell and ownership repair.

```
docker compose run --rm takinit make-cert client alice      # issue a client certificate
docker compose run --rm takinit make-cert server host.example.com
docker compose run --rm --no-deps takconfig verify          # check the whole artifact set
```

Get the admin certificate out and mark it as an administrator (the messaging process must be running, UserManager
talks to it over Ignite):

```
docker compose cp takserver-api:/opt/tak/certs/files/admin.p12 ./admin.p12     # password: $CERT_PASS
docker compose cp takserver-api:/opt/tak/certs/files/ca.pem    ./ca.pem
docker compose exec takserver-messaging java -jar /opt/takserver/UserManager.jar certmod -A certs/files/admin.pem
```

Import `admin.p12` into the browser and open `https://<host>:8443`.

### Revocation

CRL enforcement is ON by default (`CRL_ENABLED=true`). `CoreConfig.xml` lists two `<crl>` entries: first a PEM bundle
`certs/files/ca-chain.crl` (root CRL then intermediate CRL), then `certs/files/intermediate-ca.crl`. The bundle exists
because of an upstream TAK defect: `ServerConfiguration` hands the web connector (8443) only the FIRST `<crl>` entry
(`crls.get(0)`), while Tomcat reads every CRL inside that one file; the messaging connector (8089) loads one CRL per
entry. With the two CRLs listed separately, every certificate issued by the intermediate CA failed on 8443 with an
undetermined revocation status. The init container regenerates the bundle at bootstrap and after every revocation.
Online enrolment signs with the intermediate CA. `revoke` derives the issuer from the named certificate; an
optional CA argument must match:

```
docker compose run --rm takinit revoke alice                 # auto-detects root for the make-cert example above
docker compose run --rm takinit revoke admin root            # certificates the root CA issued directly
docker compose up -d --force-recreate takserver-config takserver-messaging takserver-api
```

The subcommand compares the certificate issuer with each CA subject, verifies the certificate signature, and
uses `crl-index-root.txt` or `crl-index-intermediate-ca.txt` through a copied OpenSSL configuration. It verifies the
CRL signature and the exact requested serial before reporting success. A nonempty legacy shared `crl_index.txt`
blocks creation of a missing issuer index: migrate its records only after checking each original certificate's
issuer and signature. Do not copy the shared index to both CAs or discard revoked records. The recreate is required:
TAK reads the CRLs when it builds the TLS context.

Historical evidence: `qa/container/09-acceptance-crl-enforced.txt` reports 29/29 after re-rendering existing
configuration and recreating the TAK trio, **not a fresh-volume start**. `qa/container/06-revocation.txt` shows both
issuers accepted on 8443 before and rejected afterward, with fresh 8089 rejection lines. Its client-side handshake
output does not establish pre-revocation 8089 application acceptance. The revised `qa/revocation-proof.sh` requires
HTTP 200 before and non-200 after for both certificates, plus fresh 8089 rejection lines carrying each CN or serial.
An anonymous rejection line fails this gate, including the historical root rejection with an empty subject.
These revised maintenance and assertion paths have not been run in containers in this fix round.

The admin API's own revoke endpoint still cannot regenerate the CRL from inside the distroless runtime, because it
shells out to `revokeCert.sh`. Use the command above; see gap 3 in section 9.

## 5. OIDC

With `OIDC_ENABLED=true` the rendered `CoreConfig.xml` carries one `openIdDiscoveryConfiguration` inside `<oauth>`:
`enabled="true"` is the Phase 1 switch, `usePkce="true"`, `identityKey="issuerSubject"`, and the group mappings from
`OIDC_GROUP_MAPPINGS` (`claim:group[:access]` rows, comma separated; an unmapped claim value gets no groups).

**Use 8446 for browser SSO, everywhere.** 8443 is the `https` connector and demands a client certificate, which a
first-time SSO browser does not have. 8446 is the `cert_https` connector with `clientAuth="false"`. So
`OIDC_REDIRECT_URI=https://<host>:8446/login/redirect`, the IdP's registered redirect URI is the same string, and
the login entry point is `https://<host>:8446/login/auth`. After enrolment a client has a certificate and uses 8443
and 8089 normally.

What the configuration gets you:

- `GET https://<host>:8446/login/.well-known/openid-configuration` advertises the issuer, the android client id, the
  scopes as a JSON array and the android redirect style. This is what ATAK reads.
- Browser SSO on 8446 with PKCE S256 and a nonce, single-use authorization requests.
- Bearer: access tokens and ID tokens accepted on the Marti API when `aud`/`azp` matches a configured audience.
- Certificate enrolment with a bearer assertion, `POST /Marti/api/tls/signClient/v2`.

**IdP truststore.** `OIDC_TRUSTSTORE_TYPE` (default `JKS`), `OIDC_TRUSTSTORE_FILE` (default
`certs/files/truststore-root.jks`, relative to `/opt/tak`) and `OIDC_TRUSTSTORE_PASS` (default `CERT_PASS`). The
default only trusts IdP certificates issued by this TAK CA. For an external IdP, import its chain into a store the
container can read:

```
# 1. Obtain idp-chain.pem from your IdP administrator and verify its CA fingerprints out of band.
# It must contain the PEM CA certificates you intend to trust, not just an unverified server leaf.
# 2. Build a JKS in ONE container with the chain bind-mounted into that same container.
# Replace an existing store only as a deliberate trust rotation; this recipe refuses to overwrite it.
docker compose run --rm --no-deps \
  -v "$PWD/idp-chain.pem:/tmp/idp-chain.pem:ro" --entrypoint /bin/bash takinit -s <<'IDPSTORE'
  set -euo pipefail
  umask 077
  store=/opt/tak/certs/files/idp-truststore.jks
  [ ! -e "$store" ] || { echo "truststore already exists" >&2; exit 1; }
  pass=${CERT_PASS:-atakatak}
  if [ -n "${CERT_PASS_FILE:-}" ]; then pass=$(cat "$CERT_PASS_FILE"); fi
  work=$(mktemp -d)
  trap 'rm -rf "$work"' EXIT
  awk -v dir="$work" '/-----BEGIN CERTIFICATE-----/ { n++; file=dir "/ca-" n ".pem" }
    file { print > file }
    /-----END CERTIFICATE-----/ { close(file); file="" }
    END { if (n == 0 || file != "") exit 1 }' /tmp/idp-chain.pem
  for cert in "$work"/ca-*.pem; do
    keytool -importcert -noprompt -trustcacerts -storetype JKS \
      -alias "$(basename "$cert" .pem)" -file "$cert" \
      -keystore "$work/idp.jks" -storepass "$pass"
  done
  keytool -list -storetype JKS -keystore "$work/idp.jks" -storepass "$pass"
  cp "$work/idp.jks" "$store"
  chown 65532:65532 "$store"
  chmod 600 "$store"
IDPSTORE
# 3. Set OIDC_TRUSTSTORE_TYPE=JKS and OIDC_TRUSTSTORE_FILE=certs/files/idp-truststore.jks
# in .env; set OIDC_TRUSTSTORE_PASS to that store password. Then FULLY regenerate (see above):
docker compose run --rm takconfig update-oidc
docker compose up -d --force-recreate takserver-config takserver-messaging takserver-api
```

## 6. Acceptance lab (never outside a lab)

After loading the images, run from this directory (host tools: Docker Compose, jq and curl):

```
./qa/lab-up.sh --clean
```

If `.env` is absent, the launcher copies `.env.example` with private permissions. Whether `.env` is new or already
copied, it exports lab overrides for this run: OIDC and CRLs enabled, issuer/discovery at `https://idp:8843/realms/tak`,
redirect at `https://takserver-config:8446/login/redirect`, audiences `tak-web,atak-civ,probe`, lab group mappings,
and the TAK-root JKS. It provisions and passes the confidential secret automatically. Existing `.env` files are not
rewritten with these overrides. The example passwords and users are lab-only. `--clean` deletes the existing lab
volumes, CA and database. The revised launcher targets the 29-check suite; this fix round did not run cold acceptance.

It does the whole ordering the dependencies actually require: certificates first (the IdP needs one from this CA),
then the database and Keycloak, then realm provisioning (which is what produces the confidential client secret),
then `CoreConfig.xml` rendered with that secret, then schema and the three TAK processes, then `verify`, then the
29-check acceptance suite with container DNS URLs, audiences and the probe secret. It waits for Compose
health checks (log/TCP checks with the limitations below) and exits non-zero with logs if anything stalls. It needs `docker compose`, `jq` and `curl`
on the host; everything else runs in containers, and the runner image is in the tarball.

`qa/run-e2e-container.sh` uses 8446 for the SSO/bearer path and a separate `TAK_CERT` connector for check 8d.
Its replay assertion accepts only HTTP 400/401/403; HTTP 500 fails. QA runs `sleep infinity` in the lab and the
launcher executes the suite inside it. Stop it with `docker compose --profile acceptance stop qa` when finished.

## 7. Ports, and where they are published

| Port | Connector | Client certificate | Published by default |
|---|---|---|---|
| 8089 | `stdssl` TLS input | required | `127.0.0.1` only |
| 8443 | `https` web connector | required | `127.0.0.1` only |
| 8446 | `cert_https` web connector | **not required** | `127.0.0.1` only |

8446 is the authentication surface: it serves `/login/auth`, `/login/redirect`, the discovery document and bearer
API calls, and it accepts connections with no client certificate. Set `BIND_ADDR` only when you have decided what
sits in front of it. The database and the Ignite ports are not published to the host at all.

## 8. Stopping, upgrading, removing

```
docker compose down                     # keeps the volumes: certificates, configuration, database survive
docker compose down -v                  # destroys the CA, the configuration and the database
docker compose up -d --force-recreate takserver-config takserver-messaging takserver-api
```

The last one matters twice: `takserver-config` owns the network namespace the other two join, so they have to be
recreated together; and it is how any `CoreConfig.xml` change (including `update-oidc` and a revocation) takes
effect, because the config microservice reads the file at start and distributes it over Ignite.

## 9. Footprint, hardening, and the gaps

### Footprint, measured three ways because they are three different numbers

| | `takserver-oidc` | `schemamanager` | `init` | `qa` |
|---|---|---|---|---|
| compressed layer blobs (runtime: archive inspection; others: engine content display) | 390,366,742 B | 141 MB | 67.1 MB | 22.1 MB |
| uncompressed layer tar data (archive inspection) | 561,752,576 B | not measured | not measured | not measured |
| local storage (`docker image ls` DISK USAGE, shared base layers counted per image) | 957 MB | 438 MB | 263 MB | 82.8 MB |

Delivered tarball (`docker save`, all four images, shared layers once): 534,628,352 bytes. Measured 2026-09-20T11:14Z,
`qa/container/04b-image-size-after-fixes.txt`.

These are historical measurements of the reviewed archive, not measurements of images rebuilt with these fixes.
The runtime compressed/uncompressed totals were corrected by hostile review 2. Uncompressed layer tar bytes include
tar metadata and overwritten layer entries; they are not the merged root filesystem size. Engine `.Size` was not a
valid unpacked-size measurement on this engine. The manifest generator measures archive blobs and decompressed tar
streams separately; local storage is an engine-specific quantity and shared layers must not be summed as unique usage.

SBOM (syft, CycloneDX JSON, in `delivery/`) with per-image counts in `qa/container/05-sbom-summary.txt`.
**The two distroless images contain no shell, no package manager and no editor**, and their SBOMs show only the
distroless base packages plus Java archives. The init and qa images are alpine and do contain `busybox`, `bash` and
`apk-tools`. Init/config are one-shot commands, including maintenance runs while TAK serves; QA remains running
with `sleep infinity` in the acceptance lab until stopped.

### Hardening, per service

| Service | User | rootfs | Capabilities | no-new-privileges | Writable paths |
|---|---|---|---|---|---|
| `takserver-config/messaging/api` | 65532 nonroot | read-only | all dropped | yes | `/opt/tak` volume, tmpfs `/tmp` |
| `schemamanager` | 65532 nonroot | read-only | all dropped | yes | tmpfs `/tmp` (volume is `:ro`) |
| `takinit` / `takconfig` | 0 (it chowns the volume) | writable | all dropped, then CHOWN, DAC_OVERRIDE, FOWNER, FSETID | yes | `/opt/tak`, `/opt/idp-certs`, tmpfs `/tmp` |
| `db` | postgres image default | writable | all dropped, then CHOWN, DAC_READ_SEARCH, FOWNER, SETGID, SETUID | yes | `pgdata` volume |
| `idp`, `qa` | image default | writable | image default | no | acceptance profile in delivered Compose |

`/tmp` is a tmpfs and is writable in the TAK containers; the `/opt/tak` volume is the only writable **persistent**
path. Engine-configuration evidence (configured user, capabilities, mounts, security options, read-only flag) is archived in
`qa/container/07-hardening-inspect.txt`, taken with `docker inspect` from the running clean-start stack. A `healthy`
status proves none of this. This is not an inspection of every existing application process's effective UID/capabilities.

### Named gaps

1. **WebTAK is not here and cannot be built from this repository.** `find src -iname "*webtak*"` on
   `lastwall/oidc-phase1` at c0851d5 returns nothing. WebTAK is distributed as a separate component by the TAK
   Product Center. If it is in scope, it has to be layered in from a TPC distribution and re-tested.
2. **`takserver-tool-ui` is excluded from the war.** `takserver-core/build.gradle:117` wires
   `bootWar.dependsOn ":takserver-tool-ui:deployToCore"`, and that React build fails with
   `[eslint] package.json > eslint-config-react-app/jest#overrides[0]: Environment key "jest/globals" is unknown`.
   Transcript: `qa/container/00-build-attempt-with-toolui-FAILED.txt`. It deploys to `Marti/static/{css,js}`, which
   already carries checked-in content. *Not verified here: whether a host build fails identically. The evidence is a
   container build only.* The Angular 19 metrics dashboard **is** built and is in the image.
3. **The admin API's revoke endpoint cannot regenerate the CRL.** It shells out to `revokeCert.sh`, which needs bash
   and openssl, and the failure is swallowed by the calling code. Use `docker compose run --rm takinit revoke`
   (section 4). The transcript covers the earlier helper; the revised helper still needs a container run.
   Deferred fix for the admin endpoint: a sidecar or an API change.
4. **The whole PKI lives on the shared `takdata` volume**, owned by the runtime uid, so every TAK process can read
   the root CA key and the intermediate signing key. `0700` on `certs/files` does not isolate processes that share a
   uid. Deferred: split the root CA and the exported admin key out of the runtime mounts, keep only the online
   signer and the server keystore, and mount static trust material read-only.
5. **Health checks prove liveness, not correctness.** The config check greps a persistent log file, so a startup
   line from a previous run would satisfy it; the other two are TCP connects. A stack can be `healthy` with broken
   SSO, which is exactly what happened in the first acceptance round. Deferred: current-instance readiness and a
   dependency check.
6. **The acceptance suite's negative tokens keep the original signature** for the audience, issuer and expiry
   mutations, so those rejections do not independently prove the claim validators; requests use `curl -k`, so client
   side chain and hostname verification is untested; and there is no real browser session. Deferred: re-signed
   negative tokens, a trusted-CA run without `-k`, and a browser test.
7. **`UserManager.jar` (57.9 MB) is in the runtime image** for one maintenance command. Deferred: a maintenance
   target. The removal recipe is not one line, because the builder copies it unconditionally.
8. **Federation, clustering, retention and the plugin manager** are not configured or started. See `DESIGN-NOTES.md`.
9. **Kubernetes translation is stated as design analysis, not a tested claim.** No Pod spec was applied anywhere.
10. **Upstream loads only the first `<crl>` entry on the web connector** (`ServerConfiguration.java:346`, `crls.get(0)`).
    Worked around with the PEM bundle described in section 4; the upstream fix would be to hand Tomcat every entry. Worth a
    merge request of its own.
11. **`takserver-messaging.log` still prints `TLS enabled, but no certificate revocation lists, and OSCP is not enabled`
    at each start** (six times over the day's runs) even though the 8089 input demonstrably enforces the CRLs
    (`06-revocation.txt`, section 4b). The line comes from a second `SSLConfig` instance built by the services-deployment
    worker, most likely the federation or a secondary input context that carries no `<crl>`. Not diagnosed further; the
    enforcement evidence is on the connector that matters. Deferred: identify the context and either configure it or
    silence the warning.

12. **Tested versus delivered init image.** Historical acceptance used init identity prefix `83c5cd9f`; the reviewed
    delivery contained `ead53999`. The CRL-default flip was low functional risk under Compose because Compose
    supplies `CRL_ENABLED`, but those are different bytes. The CRL-on transcript reused existing volumes. These
    source fixes have not been rebuilt, exported or accepted in containers; a fresh-volume run with CRLs enabled
    against the exact rebuilt archive, immutable image identities and sanitized configuration remains outstanding.
