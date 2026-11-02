# syntax=docker/dockerfile:1.9
#
# Minimal-footprint TAK Server image built from this branch.
#
#   docker buildx build -f docker/Dockerfile --platform linux/amd64 --target takserver -t takserver-oidc:<ver> .
#
# Three targets share one build:
#   takserver      distroless java17, nonroot, the war only. No shell, no package manager, no editors.
#   schemamanager  distroless java17, nonroot, the flyway uber jar only. Runs once as a compose init service.
#   takinit        alpine + bash + openssl. Generates the CA and renders CoreConfig.xml into the takdata volume.
#
# Upstream's Dockerfile.takserver is eclipse-temurin:17-jammy plus emacs, vim, net-tools and netcat, started through
# a shell script. None of that is in here.

ARG JDK_IMAGE=eclipse-temurin:17-jdk@sha256:bc033b57e11b773c3043babfd664e7a5ef110805548b921cbfc3e8c67a0725d6
ARG RUNTIME_IMAGE=gcr.io/distroless/java17-debian12:nonroot@sha256:06484c2a9dcc9070aeafbc0fe752cb9f73bc0cea5c311f6a516e9010061998ad
ARG ALPINE_IMAGE=alpine:3.22@sha256:5291449c3df73caf6ed85e649dec1b9e818b39a5d8c871e97afc13e9cd5e8fa8

########################################################################################################## build
# Runs on the builder's own architecture: the war, the uber jars and the healthcheck class are all architecture
# independent, so there is no emulation cost for a cross-architecture target image.
FROM --platform=$BUILDPLATFORM ${JDK_IMAGE} AS build

# takserver-protobuf:applyGrpcPatch shells out to patch(1); the temurin base does not carry it.
RUN --mount=type=cache,target=/var/cache/apt,sharing=locked \
    --mount=type=cache,target=/var/lib/apt/lists,sharing=locked \
    apt-get update && apt-get install -y --no-install-recommends patch

ENV GRADLE_USER_HOME=/gradle-home
WORKDIR /repo
COPY . /repo

# GRADLE_TASKS / GRADLE_EXCLUDES decide how much of the front end is built.
#   default         : bootWar, which pulls in :takserver-core:deployMetricsDashboard, so the gradle node plugin
#                     downloads node 22.13.1 and builds the Angular 19 metrics dashboard into Marti/metrics/browser.
#   with tool-ui    : drop the -x :takserver-tool-ui:deployToCore below. takserver-core/build.gradle:117 wires
#                     bootWar.dependsOn ":takserver-tool-ui:deployToCore", so it is in the graph unless excluded.
#                     It FAILS on this branch, and not because of this image: its React build runs
#                     eslint-config-react-app, which aborts with 'Environment key "jest/globals" is unknown'.
#                     Upstream toolchain rot (node 18.12.1, react-scripts). takserver-tool-ui deploys to
#                     Marti/static/{css,js}, which already carries checked-in content in the war.
#   api-only        : GRADLE_EXCLUDES="-x test -x :takserver-core:deployMetricsDashboard -x bundleAngularWebApp
#                     -x webappAngularNpmInstall", which builds in about 5 minutes and has no npm step.
# Tests are always excluded here: they run separately (qa/gradle in the request directory, 130 tests).
ARG GRADLE_TASKS=":takserver-core:bootWar :takserver-schemamanager:shadowJar :takserver-usermanager:shadowJar"
ARG GRADLE_EXCLUDES="-x test -x :takserver-tool-ui:deployToCore"
RUN --mount=type=cache,target=/gradle-home,sharing=locked \
    set -eux; \
    cd /repo/src; \
    ./gradlew --no-daemon --console=plain ${GRADLE_TASKS} ${GRADLE_EXCLUDES}; \
    mkdir -p /out/health; \
    cp takserver-core/build/libs/takserver-core-*.war   /out/takserver.war; \
    cp takserver-schemamanager/build/libs/schemamanager-*-uber.jar /out/SchemaManager.jar; \
    cp takserver-usermanager/build/libs/UserManager-*-all.jar      /out/UserManager.jar; \
    javac -d /out/health /repo/docker/health/Health.java; \
    ( cd /repo && java -version 2>&1; ./src/gradlew --version 2>/dev/null | head -20 ) > /out/BUILD-VERSIONS.txt || true; \
    ls -l /out

########################################################################################################## takinit
# bash, openssl and a headless JRE for keytool. The JRE is here and not in the runtime image on purpose: upstream's
# cert scripts are run verbatim, so the keystores are byte-for-byte the shape TAK expects, including the trusted
# certificate entries that only keytool -import -trustcacerts produces. See docker/DESIGN-NOTES.md for why an
# openssl-only PKCS12 variant was rejected.
FROM ${ALPINE_IMAGE} AS takinit
ARG APK_BASH_VERSION
ARG APK_OPENSSL_VERSION
RUN apk add --no-cache "bash${APK_BASH_VERSION:+=$APK_BASH_VERSION}" "openssl${APK_OPENSSL_VERSION:+=$APK_OPENSSL_VERSION}" openjdk17-jre-headless
COPY docker/init/                         /opt/tak-init/
COPY src/takserver-core/scripts/certs/    /opt/tak-init/certs/
RUN chmod +x /opt/tak-init/init.sh /opt/tak-init/certs/*.sh
ENTRYPOINT ["/opt/tak-init/init.sh"]

########################################################################################################## schemamanager
FROM ${RUNTIME_IMAGE} AS schemamanager
COPY --from=build /out/SchemaManager.jar /opt/takserver/SchemaManager.jar
WORKDIR /opt/tak
USER nonroot:nonroot
ENTRYPOINT ["java", "-Xmx256m", "-jar", "/opt/takserver/SchemaManager.jar"]
CMD ["upgrade"]

########################################################################################################## takserver
FROM ${RUNTIME_IMAGE} AS takserver

# Exactly upstream setenv.sh, minus the shell. JDK_JAVA_OPTIONS is prepended to every java invocation by the launcher,
# so compose only has to pass the heap and the Spring profile.
ENV IGNITE_HOME=/opt/tak
ENV JDK_JAVA_OPTIONS="-Dloader.path=WEB-INF/lib-provided,WEB-INF/lib,WEB-INF/classes -Dio.netty.tmpdir=/opt/tak -Djava.io.tmpdir=/opt/tak -Dio.netty.native.workdir=/opt/tak -Djava.net.preferIPv4Stack=true -Djava.security.egd=file:/dev/./urandom -DIGNITE_UPDATE_NOTIFIER=false -DIGNITE_QUIET=true -Djdk.tls.client.protocols=TLSv1.2 -XX:-UsePerfData --add-opens=java.base/sun.security.pkcs=ALL-UNNAMED --add-opens=java.base/sun.security.pkcs10=ALL-UNNAMED --add-opens=java.base/sun.security.util=ALL-UNNAMED --add-opens=java.base/sun.security.x509=ALL-UNNAMED --add-opens=java.base/sun.security.tools.keytool=ALL-UNNAMED --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED --add-opens=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.management/com.sun.jmx.mbeanserver=ALL-UNNAMED --add-opens=jdk.internal.jvmstat/sun.jvmstat.monitor=ALL-UNNAMED --add-opens=java.base/sun.reflect.generics.reflectiveObjects=ALL-UNNAMED --add-opens=jdk.management/com.sun.management.internal=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.util.concurrent=ALL-UNNAMED --add-opens=java.base/java.util.concurrent.locks=ALL-UNNAMED --add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.lang.invoke=ALL-UNNAMED --add-opens=java.base/java.math=ALL-UNNAMED --add-opens=java.sql/java.sql=ALL-UNNAMED --add-opens=java.base/javax.net.ssl=ALL-UNNAMED --add-opens=java.base/java.net=ALL-UNNAMED --add-opens=jdk.unsupported/sun.misc=ALL-UNNAMED --add-opens=java.base/java.lang.ref=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.security=ALL-UNNAMED --add-opens=java.base/java.security.cert=ALL-UNNAMED --add-opens=java.base/sun.security.provider.certpath=ALL-UNNAMED --add-opens=java.base/sun.security.rsa=ALL-UNNAMED --add-opens=java.base/sun.security.ssl=ALL-UNNAMED --add-opens=java.base/sun.security.validator=ALL-UNNAMED --add-opens=java.base/sun.security.x500=ALL-UNNAMED --add-opens=jdk.crypto.cryptoki/sun.security.pkcs11=ALL-UNNAMED --add-opens=java.base/sun.security.pkcs12=ALL-UNNAMED --add-opens=java.base/sun.security.provider=ALL-UNNAMED --add-opens=java.base/javax.security.auth.x500=ALL-UNNAMED"

COPY --from=build /out/takserver.war    /opt/takserver/takserver.war
COPY --from=build /out/UserManager.jar  /opt/takserver/UserManager.jar
COPY --from=build /out/health/          /opt/takserver/health/

# /opt/tak is the working directory and the only writable path. It is a named volume in compose, seeded by takinit.
WORKDIR /opt/tak
USER nonroot:nonroot
EXPOSE 8089 8443 8446

ENTRYPOINT ["java"]
CMD ["-Dspring.profiles.active=config", "-jar", "/opt/takserver/takserver.war"]

########################################################################################################## qa
# Test-only. Runs the phase 1 end-to-end acceptance script from inside the compose network, so the Keycloak issuer
# hostname is the same one TAK is configured with. Never part of a delivery.
FROM ${ALPINE_IMAGE} AS qa
RUN apk add --no-cache bash curl python3 openssl
WORKDIR /qa
ENTRYPOINT []
CMD ["sleep", "infinity"]
