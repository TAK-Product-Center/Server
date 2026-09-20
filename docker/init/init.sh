#!/bin/bash
#
# One-shot initialiser for the minimal-footprint TAK Server container.
#
# The runtime image is distroless: no shell, no openssl, no editors. Everything that needs a shell happens here, once,
# into the takdata volume, and then this container exits. It is idempotent: an existing CA or CoreConfig.xml is left
# alone, so restarting the stack never rotates keys or overwrites operator edits.
#
set -euo pipefail

TAK_DIR="${TAK_DIR:-/opt/tak}"
SRC_DIR="${SRC_DIR:-/opt/tak-init}"
TAK_UID="${TAK_UID:-65532}"
TAK_GID="${TAK_GID:-65532}"

log() { printf '[init] %s\n' "$*"; }

xml_escape() {
	printf '%s' "$1" | sed -e 's/&/\&amp;/g' -e 's/</\&lt;/g' -e 's/>/\&gt;/g' -e 's/"/\&quot;/g'
}

subst() { # $1 placeholder token, $2 raw value -> substituted into $RENDER, XML escaped
	local value
	value=$(xml_escape "$2" | sed -e 's/[\\|&]/\\&/g')
	sed -i "s|$1|$value|g" "$RENDER"
}

############################################################################### certificates
if [ -f "$TAK_DIR/certs/files/ca.pem" ]; then
	log "CA already present in the volume, leaving certificates untouched"
else
	log "generating the certificate authority and server/client certificates"
	mkdir -p "$TAK_DIR/certs"
	cp "$SRC_DIR"/certs/* "$TAK_DIR/certs/"
	chmod +x "$TAK_DIR"/certs/*.sh
	cd "$TAK_DIR/certs"

	export COUNTRY="${CERT_COUNTRY:-US}"
	export STATE="${CERT_STATE:-State}"
	export CITY="${CERT_CITY:-City}"
	export ORGANIZATION="${CERT_ORGANIZATION:-TAK}"
	export ORGANIZATIONAL_UNIT="${CERT_ORGANIZATIONAL_UNIT:-TAKOIDC}"
	export CAPASS="${CERT_PASS:-atakatak}"
	export PASS="${CERT_PASS:-atakatak}"

	./makeRootCa.sh --ca-name "${CA_NAME:-TAK-CONTAINER-CA}"
	./makeCert.sh server "${SERVER_CN:-takserver}"
	./makeCert.sh ca intermediate-ca
	./makeCert.sh client "${ADMIN_CN:-admin}"
	for extra in ${EXTRA_SERVER_CERTS:-}; do
		log "extra server certificate: $extra"
		./makeCert.sh server "$extra"
		# unencrypted key for components that cannot take a passphrase (Keycloak --https-certificate-key-file)
		openssl rsa -in "files/$extra.key" -passin "pass:${CERT_PASS:-atakatak}" -out "files/$extra.nopass.key"
	done

	# the server certificate is referenced as takserver.jks by CoreConfig, whatever its CN is
	cp "files/${SERVER_CN:-takserver}.jks" files/takserver.jks
	cp "files/${SERVER_CN:-takserver}.p12" files/takserver.p12
	cd - >/dev/null
fi

############################################################################### export for other containers
# Anything that is not TAK (the lab IdP, for instance) gets its certificate through this directory, never through the
# takdata volume, which stays 700 and owned by uid 65532.
if [ -d /opt/idp-certs ]; then
	for extra in ${EXTRA_SERVER_CERTS:-}; do
		if [ -f "$TAK_DIR/certs/files/$extra.pem" ] && [ ! -f "/opt/idp-certs/$extra.pem" ]; then
			# makeCert.sh already writes leaf + CA into <name>.pem. Appending ca.pem again produces a chain with a
			# duplicate root, which Keycloak rejects with java.security.KeyStoreException: Certificate chain is not valid.
			cp "$TAK_DIR/certs/files/$extra.pem" "/opt/idp-certs/$extra.pem"
			cp "$TAK_DIR/certs/files/$extra.nopass.key" "/opt/idp-certs/$extra.nopass.key"
			log "exported $extra certificate to /opt/idp-certs"
		fi
	done
	cp -f "$TAK_DIR/certs/files/ca.pem" /opt/idp-certs/ca.pem
	cp -f "$TAK_DIR/certs/files/truststore-root.jks" /opt/idp-certs/truststore-root.jks
	chown -R "${IDP_UID:-1000}:${IDP_GID:-0}" /opt/idp-certs
	chmod 640 /opt/idp-certs/*.key 2>/dev/null || true
	chmod 644 /opt/idp-certs/*.pem 2>/dev/null || true
fi

############################################################################### CoreConfig.xml
if [ -f "$TAK_DIR/CoreConfig.xml" ]; then
	log "CoreConfig.xml already present, leaving it untouched"
else
	log "rendering CoreConfig.xml"
	RENDER="$TAK_DIR/CoreConfig.xml.tmp"
	cp "$SRC_DIR/CoreConfig.template.xml" "$RENDER"

	subst __DB_URL__                     "${DB_URL:-jdbc:postgresql://db:5432/cot}"
	subst __DB_USERNAME__                "${DB_USERNAME:-martiuser}"
	subst __DB_PASSWORD__                "${DB_PASSWORD:?DB_PASSWORD is required}"
	subst __DB_POOL__                    "${DB_POOL:-16}"
	subst __TOMCAT_MAX_POOL__            "${TOMCAT_MAX_POOL:-64}"
	subst __CERT_PASS__                  "${CERT_PASS:-atakatak}"
	subst __CERT_ORGANIZATION__          "${CERT_ORGANIZATION:-TAK}"
	subst __CERT_ORGANIZATIONAL_UNIT__   "${CERT_ORGANIZATIONAL_UNIT:-TAKOIDC}"

	if [ "${OIDC_ENABLED:-false}" = "true" ]; then
		log "OIDC provider: ${OIDC_NAME:-oidc} at ${OIDC_ISSUER:-}"
		subst __OIDC_NAME__               "${OIDC_NAME:-oidc}"
		subst __OIDC_CLIENT_ID__          "${OIDC_CLIENT_ID:?OIDC_CLIENT_ID is required when OIDC_ENABLED=true}"
		subst __OIDC_CLIENT_SECRET__      "${OIDC_CLIENT_SECRET:-}"
		subst __OIDC_REDIRECT_URI__       "${OIDC_REDIRECT_URI:?OIDC_REDIRECT_URI is required}"
		subst __OIDC_CONFIGURATION_URI__  "${OIDC_CONFIGURATION_URI:?OIDC_CONFIGURATION_URI is required}"
		subst __OIDC_ISSUER__             "${OIDC_ISSUER:?OIDC_ISSUER is required}"
		subst __OIDC_AUDIENCE__           "${OIDC_AUDIENCE:-}"
		subst __OIDC_ANDROID_CLIENT_ID__  "${OIDC_ANDROID_CLIENT_ID:-}"
		subst __OIDC_SCOPES__             "${OIDC_SCOPES:-openid profile groups}"
		subst __OIDC_GROUPS_CLAIM__       "${OIDC_GROUPS_CLAIM:-groups}"
		subst __OIDC_USERNAME_CLAIM__     "${OIDC_USERNAME_CLAIM:-preferred_username}"
		subst __OIDC_ADMIN_CLAIM_NAME__   "${OIDC_ADMIN_CLAIM_NAME:-groups}"
		subst __OIDC_ADMIN_CLAIM_VALUE__  "${OIDC_ADMIN_CLAIM_VALUE:-tak_admin}"
		subst __OIDC_IDENTITY_KEY__       "${OIDC_IDENTITY_KEY:-issuerSubject}"
		subst __OIDC_USE_TAK_LOGIN_PAGE__ "${OIDC_USE_TAK_LOGIN_PAGE:-true}"
		subst __OIDC_TRUSTSTORE_TYPE__    "${OIDC_TRUSTSTORE_TYPE:-JKS}"
		subst __OIDC_TRUSTSTORE_FILE__    "${OIDC_TRUSTSTORE_FILE:-certs/files/truststore-root.jks}"
		subst __OIDC_TRUSTSTORE_PASS__    "${OIDC_TRUSTSTORE_PASS:-${CERT_PASS:-atakatak}}"

		# OIDC_GROUP_MAPPINGS="claim:group[:access],claim:group[:access],..."
		mappings=""
		IFS=',' read -ra rows <<< "${OIDC_GROUP_MAPPINGS:-}"
		for row in "${rows[@]:-}"; do
			[ -n "$row" ] || continue
			IFS=':' read -r claim group access <<< "$row"
			if [ -n "${access:-}" ]; then
				mappings+="            <groupMapping claimValue=\"$(xml_escape "$claim")\" group=\"$(xml_escape "$group")\" access=\"$(xml_escape "$access")\"/>\n"
			else
				mappings+="            <groupMapping claimValue=\"$(xml_escape "$claim")\" group=\"$(xml_escape "$group")\"/>\n"
			fi
		done
		if [ -n "$mappings" ]; then
			printf '%b' "$mappings" > /tmp/mappings.xml
			sed -i -e "/<!--OIDC-GROUPMAPPING-->/{r /tmp/mappings.xml" -e "d}" "$RENDER"
		else
			sed -i "/<!--OIDC-GROUPMAPPING-->/d" "$RENDER"
		fi
	else
		log "OIDC disabled, removing the oauth block"
		sed -i "/<!--OIDC-START-->/,/<!--OIDC-END-->/d" "$RENDER"
	fi

	mv "$RENDER" "$TAK_DIR/CoreConfig.xml"
fi

############################################################################### supporting files
[ -f "$TAK_DIR/TAKIgniteConfig.xml" ]      || cp "$SRC_DIR/TAKIgniteConfig.xml" "$TAK_DIR/TAKIgniteConfig.xml"
[ -f "$TAK_DIR/UserAuthenticationFile.xml" ] || cp "$SRC_DIR/UserAuthenticationFile.xml" "$TAK_DIR/UserAuthenticationFile.xml"
mkdir -p "$TAK_DIR/logs"

chown -R "$TAK_UID:$TAK_GID" "$TAK_DIR"
chmod 700 "$TAK_DIR/certs/files"
log "takdata volume ready, owned by $TAK_UID:$TAK_GID"
