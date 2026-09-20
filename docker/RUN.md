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

`RELEASE-MANIFEST.md` binds these bytes to the acceptance run: it carries the image digests, the file hashes of the
compose file, the init files and the documentation, and the acceptance transcript the digests were produced against.

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
Build contexts exclude `.env`, `.env.*`, `certs/files/` and any key material (`docker/Dockerfile.dockerignore`).

## 3. First start

`docker compose up -d` runs, in dependency order:

1. `db`, to healthy.
2. `takinit` (`certs`): the CA, the server certificate, the `intermediate-ca` online signer, an `admin` client
   certificate, the two CRLs, and any `EXTRA_SERVER_CERTS`. Each artifact is checked individually, so an interrupted
   bootstrap repairs itself; existing key material is never regenerated.
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
upstream's scripts require. Never override its entrypoint; use the subcommands.

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

`CoreConfig.xml` configures both CRLs, `certs/files/ca.crl` and `certs/files/intermediate-ca.crl`, and both exist
from bootstrap. Online enrolment signs with the intermediate CA, so that is the default:

```
docker compose run --rm takinit revoke alice                 # intermediate CA, the online signer
docker compose run --rm takinit revoke admin root            # certificates the root CA issued directly
docker compose up -d --force-recreate takserver-config takserver-messaging takserver-api
```

The subcommand passes base names to upstream's `revokeCert.sh` the way that script expects, checks that the CRL was
actually rewritten and that it contains a revoked serial, then repairs ownership. The recreate is required: TAK reads
the CRLs when it builds the TLS context. Evidence that a revoked certificate is then rejected on 8089 and 8443 is in
`qa/container/06-revocation.txt`.

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
# 1. fetch the chain
openssl s_client -showcerts -connect idp.example.com:443 </dev/null 2>/dev/null \
  | openssl crl2pkcs7 -nocrl -certfile /dev/stdin | openssl pkcs7 -print_certs -out idp-chain.pem
# 2. build a JKS from it, inside the init image, which has keytool
docker compose cp idp-chain.pem takinit:/tmp/idp-chain.pem
docker compose run --rm --no-deps --entrypoint keytool takinit \
  -importcert -noprompt -trustcacerts -alias idp -file /tmp/idp-chain.pem \
  -keystore /opt/tak/certs/files/idp-truststore.jks -storepass "$CERT_PASS"
# 3. point CoreConfig at it and re-render
OIDC_TRUSTSTORE_FILE=certs/files/idp-truststore.jks docker compose run --rm takconfig update-oidc
docker compose up -d --force-recreate takserver-config takserver-messaging takserver-api
```

## 6. Acceptance lab (never outside a lab)

One command, cold, from a clean engine:

```
./qa/lab-up.sh --clean
```

It does the whole ordering the dependencies actually require: certificates first (the IdP needs one from this CA),
then the database and Keycloak, then realm provisioning (which is what produces the confidential client secret),
then `CoreConfig.xml` rendered with that secret, then schema and the three TAK processes, then `verify`, then the
29-check acceptance suite with the correct container DNS URLs, audiences and the probe secret. It waits for real
readiness at every step and exits non-zero with logs if anything stalls. It needs `docker compose`, `jq` and `curl`
on the host; everything else runs in containers, and the runner image is in the tarball.

`qa/run-e2e-container.sh` differs from the phase 1 host script in exactly two lines, both about which connector a
probe talks to, never about what it asserts: a `TAK_CERT` variable, and check 8d using it. Diff it against
`requests/2026-09-18-tak-oidc-sso-phase1/qa/e2e/run-e2e-v02.sh`.

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
| compressed content (`docker image ls` CONTENT SIZE) | see `qa/container/04-image-size-and-layers.txt` | | | |
| unpacked layer sum (`docker history`) | | | | |
| local storage (`docker image ls` DISK USAGE) | | | | |

The exact current numbers are in `qa/container/04-image-size-and-layers.txt` and `RELEASE-MANIFEST.md`; they are
reproduced there rather than here so that this file cannot drift from the measurement. Note that content size,
unpacked layer sum and local storage are three different quantities and the earlier revision of this document
conflated them.

SBOM (syft, CycloneDX JSON, in `delivery/`) with per-image counts in `qa/container/05-sbom-summary.txt`.
**The two distroless images contain no shell, no package manager and no editor**, and their SBOMs show only the
distroless base packages plus Java archives. The init and qa images are alpine and do contain `busybox`, `bash` and
`apk-tools`; that is the point of them, and they are one-shot containers that are not running while TAK serves.

### Hardening, per service

| Service | User | rootfs | Capabilities | no-new-privileges | Writable paths |
|---|---|---|---|---|---|
| `takserver-config/messaging/api` | 65532 nonroot | read-only | all dropped | yes | `/opt/tak` volume, tmpfs `/tmp` |
| `schemamanager` | 65532 nonroot | read-only | all dropped | yes | tmpfs `/tmp` (volume is `:ro`) |
| `takinit` / `takconfig` | 0 (it chowns the volume) | writable | all dropped, then CHOWN, DAC_OVERRIDE, FOWNER, FSETID | yes | `/opt/tak`, `/opt/idp-certs`, tmpfs `/tmp` |
| `db` | postgres image default | writable | all dropped, then CHOWN, DAC_READ_SEARCH, FOWNER, SETGID, SETUID | yes | `pgdata` volume |
| `idp`, `qa` | image default | writable | image default | no | lab only, not delivered configuration |

`/tmp` is a tmpfs and is writable in the TAK containers; the `/opt/tak` volume is the only writable **persistent**
path. Effective runtime evidence (user, capability sets, mounts, security options, read-only flag) is archived in
`qa/container/07-hardening-inspect.txt`, taken with `docker inspect` from the running clean-start stack. A `healthy`
status proves none of this and is not offered as proof of it.

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
   (section 4), which is tested. Deferred fix: a sidecar or an API change.
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
