# Running the minimal-footprint TAK Server container

TAK Server 5.7-RELEASE-14 from branch `lastwall/oidc-phase1`, OIDC SSO Phase 1 included.
Three images, all built from `docker/Dockerfile` in this repository:

| Image | Base | Runs |
|---|---|---|
| `takserver-oidc` | `gcr.io/distroless/java17-debian12:nonroot` | the three TAK processes (config, messaging, api) |
| `takserver-oidc-schemamanager` | same distroless base | the flyway migrations, once |
| `takserver-oidc-init` | `alpine:3.22` + bash, openssl, headless JRE | the CA, the certificates and CoreConfig.xml, once |

The runtime image has no shell, no package manager, no editors and no netcat. It runs as uid 65532 with a read-only
root filesystem and all capabilities dropped. Read `DESIGN-NOTES.md` before changing any of that: two behaviours
depend on it (certificate revocation, and the fact that the three TAK processes share one network namespace).

## 1. Load the images

```
docker load -i takserver-oidc.oci.tar
docker image ls | grep takserver-oidc
```

Verify the tarball first:

```
shasum -a 256 -c SHA256SUMS
```

## 2. Configure

```
cp .env.example .env
$EDITOR .env        # DB_PASSWORD is mandatory; set the OIDC block if you want SSO
```

The values that matter:

- `DB_PASSWORD` - the postgres password. No default on purpose.
- `SERVER_CN` - the common name of the server certificate. It has to be the hostname clients and the IdP use.
  Default `takserver-config`, which is the compose service name.
- `CERT_PASS` - keystore password, `atakatak` by default because that is what every TAK client expects.
- `OIDC_ENABLED=true` plus `OIDC_ISSUER`, `OIDC_CONFIGURATION_URI`, `OIDC_CLIENT_ID`, `OIDC_CLIENT_SECRET`,
  `OIDC_REDIRECT_URI`, `OIDC_AUDIENCE`, `OIDC_ANDROID_CLIENT_ID`, `OIDC_GROUP_MAPPINGS`.
- `OIDC_REDIRECT_URI` must point at the **8446** connector. 8443 requires a client certificate, which a browser doing
  first-time SSO does not have yet. `https://<host>:8446/login/redirect`.
- `OIDC_GROUP_MAPPINGS` is `claim:group[:access]` rows separated by commas, for example
  `tak_ops:Operations:both,tak_admin:Operations`. A claim value with no mapping gets no groups: deny by default.
- `OIDC_TRUSTSTORE_FILE` is the JKS that holds the IdP's chain, relative to `/opt/tak`. If your IdP has a public
  certificate, point this at a truststore you build; if it is signed by the TAK CA, the default
  `certs/files/truststore-root.jks` already trusts it.

## 3. First start

```
docker compose up -d
docker compose ps
```

What happens, in order:

1. `db` starts and becomes healthy.
2. `takinit` runs once. It generates the CA (`makeRootCa.sh`), the server certificate, an `admin` client certificate
   and the `intermediate-ca` signing certificate (`makeCert.sh`), renders `CoreConfig.xml` from the template, writes
   `TAKIgniteConfig.xml` and an empty `UserAuthenticationFile.xml`, and hands the `takdata` volume to uid 65532.
   It is idempotent: if a CA or a `CoreConfig.xml` already exists it leaves them alone, so restarting never rotates
   keys and never discards an edited configuration.
3. `schemamanager` applies the flyway migrations and exits.
4. `takserver-config`, then `takserver-messaging`, then `takserver-api`.

`takserver-api` healthy means 8443 and 8446 are listening. First boot takes a couple of minutes.

Logs:

```
docker compose logs -f takserver-api            # console, through the consolelog profile
docker compose exec takserver-api java -cp /opt/takserver/health Health tcp:127.0.0.1:8446   # readiness by hand
```

The rolling log files are inside the `takdata` volume at `/opt/tak/logs/takserver-{config,messaging,api}.log`.
The OIDC audit lines carry `tak.server.oidc.audit` and an `outcome=` field.

## 4. Certificates, and the admin enrolment

Everything the init container produced is in the `takdata` volume under `/opt/tak/certs/files`. To get the admin
client certificate out:

```
docker compose cp takserver-api:/opt/tak/certs/files/admin.p12 ./admin.p12     # password: $CERT_PASS
docker compose cp takserver-api:/opt/tak/certs/files/ca.pem    ./ca.pem
```

Import `admin.p12` into the browser, then mark that certificate as an administrator. The messaging process has to be
running, because `UserManager` talks to it over Ignite:

```
docker compose exec takserver-messaging java -jar /opt/takserver/UserManager.jar certmod -A certs/files/admin.pem
```

Then browse to `https://<host>:8443` and pick the admin certificate.

To issue more client certificates, run the upstream script in the init image, which has bash and openssl:

```
docker compose run --rm --entrypoint /bin/bash takinit -lc \
  'cd /opt/tak/certs && ./makeCert.sh client <name>'
```

**Revocation is a known gap in the distroless runtime.** The admin API's revoke endpoint shells out to
`revokeCert.sh`, which needs bash and openssl; in the runtime image that call fails and the failure is swallowed, so
the database record is marked revoked while the CRL file is not regenerated. Do the CRL step explicitly:

```
docker compose run --rm --entrypoint /bin/bash takinit -lc \
  'cd /opt/tak/certs && ./revokeCert.sh files/<name>.pem files/ca-do-not-share.key files/ca.pem'
```

## 5. OIDC

With `OIDC_ENABLED=true` the rendered `CoreConfig.xml` carries one `openIdDiscoveryConfiguration` inside `<oauth>`:
`enabled="true"` is the Phase 1 switch, `usePkce="true"`, `identityKey="issuerSubject"`, and the group mappings from
`OIDC_GROUP_MAPPINGS`. What that gets you:

- `GET https://<host>:8446/login/.well-known/openid-configuration` advertises the issuer, the android client id, the
  scopes as a JSON array and the android redirect style, which is what ATAK reads.
- Browser SSO: `https://<host>:8443/login/auth` redirects to the IdP with `code_challenge` and `nonce`; the callback
  returns to 8446 and the authorization request is single use.
- Bearer: an access token or an ID token from the IdP is accepted on the Marti API when its `aud`/`azp` is one of the
  configured audiences, and it maps to exactly the groups your claim mappings allow.
- Certificate enrolment with a bearer assertion: `POST /Marti/api/tls/signClient/v2`.

The IdP has to be reachable from the container and its chain has to be in `OIDC_TRUSTSTORE_FILE`.

## 6. Acceptance profile (lab only)

```
docker compose --profile acceptance up -d
./qa/keycloak-realm-container.sh                  # provisions realm tak: clients, groups, alice/bob/carol
docker compose exec -T qa bash /qa/run-e2e-container.sh
```

This starts a Keycloak with a certificate issued by the same CA, and a small runner container that executes the
phase 1 acceptance script from inside the compose network, so the IdP issuer hostname is the one TAK is configured
with. Never enable this profile outside a lab.

## 7. Stopping, upgrading, removing

```
docker compose down                     # keeps the volumes, so certificates and configuration survive
docker compose down -v                  # destroys the CA, the configuration and the database
docker compose up -d --force-recreate takserver-config takserver-messaging takserver-api
```

The last one matters: `takserver-config` owns the network namespace the other two join, so they have to be recreated
together with it.

## 8. Kubernetes

The three TAK services are one Pod: same network namespace, same volume, one init container. A Pod spec is a direct
translation of the compose file, with `takinit` and `schemamanager` as `initContainers` and the three TAK processes as
containers. The images need no change.

## 9. What is in the image, and what is not

Measured on the delivered `linux/amd64` image, not estimated:

```
docker image inspect takserver-oidc:5.7-RELEASE-14 --format '{{.Size}}'   ->  390388451 bytes (390 MB), 39 layers
```

The three largest layers are the war (276 MB), the distroless JRE (193 MB) and `UserManager.jar` (57.9 MB).
`UserManager.jar` is only used for the admin enrolment step in section 4; if you do not want it in the runtime image,
build with `--build-arg GRADLE_TASKS` that omits `:takserver-usermanager:shadowJar` and drop the COPY line.

SBOM (syft, CycloneDX JSON, in `delivery/`): **463 real packages** in the runtime image, being **25 Debian packages**
(the entire distroless base) and **438 Java archives** (the war's own dependencies). The schemamanager image has 104,
the init image has 30. There is no shell, no package manager and no editor in any of those lists.

### Named gaps, stated rather than discovered later

1. **WebTAK is not here, and cannot be built from this repository.** `find src -iname "*webtak*"` returns nothing on
   `lastwall/oidc-phase1` at c0851d5, nor on its upstream merge base. WebTAK is distributed as a separate component by
   the TAK Product Center. No container change produces it. If WebTAK is part of the target, it has to come from a TPC
   distribution and be layered in; tell us and we will add the layer and a second acceptance pass.
2. **`takserver-tool-ui` is excluded from the war.** `takserver-core/build.gradle:117` wires
   `bootWar.dependsOn ":takserver-tool-ui:deployToCore"`, and that React build fails on this branch with
   `[eslint] package.json > eslint-config-react-app/jest#overrides[0]: Environment key "jest/globals" is unknown`.
   That is upstream toolchain rot, not a container problem, and it fails the same way in a host build. It deploys to
   `Marti/static/{css,js}`, which already carries checked-in content in the war, so the admin pages and the SSO login
   page are present. **The Angular 19 metrics dashboard IS built and IS in this image**
   (`:takserver-core:deployMetricsDashboard`, output at `Marti/metrics/browser/`).
3. **Certificate revocation does not regenerate the CRL from inside the runtime image.** See section 4 and
   `DESIGN-NOTES.md`.
4. **Federation, clustering, the retention service and the plugin manager are not configured or started.**
   See `DESIGN-NOTES.md` section 4.
