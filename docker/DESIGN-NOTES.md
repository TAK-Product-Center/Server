# Design notes for the minimal-footprint TAK Server container

Design analysis and historical build observations from `lastwall/oidc-phase1` (source baseline `c0851d5`).
This is not a claim of current runtime verification; RUN.md records the remaining evidence limits.

## 1. Why three containers and one network namespace

`IgniteConfigurationHolder` uses the single `igniteHost` attribute from `TAKIgniteConfig.xml` for three different
things: the discovery ip-finder address (`ipFinder.setAddresses(...)`, line 339), the communication SPI local bind
(`tcpSpiConf.setLocalAddress(...)`, line 355) and the Ignite local host (`standaloneConf.setLocalHost(...)`, line 362).
One value cannot be both "my own address" and "the other container's address", so the three TAK processes cannot sit in
three separate network namespaces without changing TAK source.

They therefore share one namespace, the way the containers of a Kubernetes Pod do:
`network_mode: "service:takserver-config"`. The config container owns the namespace and publishes 8089, 8443 and 8446.

Two consequences an operator has to know:

- **Startup order.** `takserver-messaging` depends on `takserver-config` with `service_started`, not `service_healthy`.
  Messaging is the Ignite server and config is an Ignite client, so gating messaging on config's readiness deadlocks
  the stack. `takserver-api` waits for messaging to be healthy (port 8089 listening).
- **Replacement.** If the namespace owner is recreated, recreate the other two in the same operation
  (`docker compose up -d --force-recreate takserver-config takserver-messaging takserver-api`), otherwise they hold a
  dead namespace.

Ignite's own working directories do not collide: the profile name is part of the path
(`<work>/config-tmp-work`, `messaging-tmp-work`, `api-tmp-work`), so one shared `/opt/tak` volume is safe. This is the
same arrangement upstream's `configureInDocker.sh` uses inside one container.

## 2. Why the runtime image has no shell

`gcr.io/distroless/java17-debian12:nonroot`, uid 65532, read-only root filesystem, `cap_drop: ALL`,
`no-new-privileges`, tmpfs `/tmp`. Upstream's `Dockerfile.takserver` is `eclipse-temurin:17-jammy` plus emacs, vim,
net-tools and netcat; none of that is in here, and there is no package manager to add it back.

What that costs, precisely:

- **The healthcheck has to be Java.** `docker/health/Health.java` is compiled in the build stage and shipped as a
  class file. It does TCP connects and log greps, and it is the only thing the HEALTHCHECK runs.
- **Certificate revocation through the admin API cannot regenerate the CRL.**
  `CertManagerAdminApi` (takserver-war, around line 285) runs `./revokeCert.sh` with `/opt/tak/certs` as the working
  directory, and `revokeCert.sh` needs `/bin/bash` and `openssl`. In this image that call fails, and the failure is
  swallowed by the surrounding catch, so the database record is marked revoked and the CRL file is not updated.
  **Maintenance path:** `docker compose run --rm takinit revoke <cert> [root|intermediate]` establishes the cert
  environment, validates the issuer and certificate signature, uses a per-issuer revocation index, and checks the
  requested serial in that CA's signed CRL. Recreate all three TAK containers afterward; see RUN.md section 4.
  The revised helper has not been run in a container during this fix round.
- **`configureInDocker.sh` is not used at all.** The three JVM invocations it performs are three compose services.

The war lives at `/opt/takserver/takserver.war` and the working directory is `/opt/tak`, the writable persistent volume; `/tmp` is also writable tmpfs.
That is safe with this war: it is launched through Spring Boot's `PropertiesLauncher`, relative `loader.path` entries
are resolved inside the archive as well as against `loader.home`, and libraries that need unpacking go to
`java.io.tmpdir`, which is set to `/opt/tak`. TAK's own relative paths (`CoreConfig.xml`, `certs/files/...`, `logs/...`,
Tomcat's base directory) all resolve against the working directory, so they land in the volume.

## 3. Why the keystores are JKS and the init image carries a JRE

An earlier iteration used PKCS12 everywhere so the init image could be alpine plus bash plus openssl with no JRE.
That was wrong, for two independent reasons:

1. **A plain `openssl pkcs12 -export -nokeys` truststore loads as zero entries in Java 17.** Java requires the
   PKCS12 `trustedKeyUsage` bag attribute before it will treat a certificate entry as trusted; OpenSSL only writes it
   with `-jdktrust`. Upstream's `makeRootCa.sh` line 60 gets this right by using
   `keytool -import -trustcacerts`, which is not merely a format conversion.
2. **Parts of TAK require the `.jks` extension.** `SecurityAuthenticationApi` (takserver-war) rejects TLS, signing and
   Microsoft CA store filenames that do not end in `.jks` with HTTP 400, `FederationConfigApi` does the same for the
   federation truststore, and `CoreConfigFacade` rewrites the enrollment signing configuration with `keystore="JKS"`
   when it is saved. Hand-written PKCS12 paths work at boot and then fail the first time an administrator saves those
   settings from the web UI.

The init image carries `openjdk17-jre-headless` for keytool. The wrapper makes upstream's expected-failure provider
probe conditional, then runs certificate scripts with `bash -e -o pipefail` and checks issued stores before success.
Init/config are one-shot commands that can also run for maintenance while TAK serves. The lab QA service remains
running with `sleep infinity`. Both images contribute to deployment footprint even though they are separate from runtime.

Bootstrap recovery is limited: `need_cert` checks PEM existence only. Missing companion stores with an existing PEM
need operator recovery; startup's artifact-existence gate is not a key/store consistency check.

`update-oidc` is full `CoreConfig.xml` regeneration, not an OAuth-only edit. The init image has no python3 XML editor.
A timestamped backup is saved, but unrelated manual XML changes are replaced; review and restore custom settings
before restarting. The `09-*` transcripts ran on an earlier init image and are historical; the release evidence is the fresh-volume
CRL-on run (`11-*`), the assertive revocation proof (`12-*`) and the hardening inspection (`13-*`), all executed on the
exact image ids that were then saved into the delivered archive (see RELEASE-MANIFEST.md).
Runtime archive measurements are 390,366,742 B compressed layer blobs and 561,752,576 B uncompressed layer tar data;
engine `.Size` was incorrectly labeled unpacked in earlier documentation. Hardening inspection is engine configuration
evidence, not a complete inspection of existing processes' effective privileges.

## 4. What is deliberately not in the configuration

- **Federation.** The `<federation>` element is omitted, so 9000/9001 are neither configured nor published. If it is
  needed, add the element and the `fed_https` connector on 8444; `makeRootCa.sh` already produced `fed-truststore.jks`.
- **Clustering.** No `<cluster>` element, no NATS, no Kubernetes ip-finder.
- **Plugins, retention and the plugin manager.** Upstream's `configureInDocker.sh` also starts
  `takserver-retention.jar` and `takserver-pm.jar`. Neither is part of the OIDC delivery and neither is in this image.
