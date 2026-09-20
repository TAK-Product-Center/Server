#!/bin/bash
#
# Bootstrap and maintenance for the minimal-footprint TAK Server container.
#
# The runtime image is distroless: no shell, no openssl, no editors. Everything that needs a shell happens here, and
# it happens through named subcommands so that nothing depends on an operator overriding the entrypoint and losing
# the environment the upstream cert scripts require.
#
#   init            bootstrap: certificates, then CoreConfig.xml. Default. Idempotent and self-repairing.
#   certs           certificates only. Run this before the IdP exists, so the IdP can be issued a certificate.
#   config          render CoreConfig.xml only. Refuses to overwrite an existing one unless FORCE=1.
#   update-oidc     rewrite the <oauth> block of the persisted CoreConfig.xml from the current environment.
#   make-cert T N   upstream makeCert.sh with the environment it needs. T is server, client or ca.
#   revoke N [CA]   revoke certificate N and regenerate the CRL of its issuing CA. CA is root or intermediate
#                   (default intermediate, which is what online enrollment signs with).
#   verify          check the complete artifact set and the permissions, exit non zero on anything missing.
#
set -euo pipefail
umask 077

TAK_DIR="${TAK_DIR:-/opt/tak}"
SRC_DIR="${SRC_DIR:-/opt/tak-init}"
CERTS_DIR="$TAK_DIR/certs"
TAK_UID="${TAK_UID:-65532}"
TAK_GID="${TAK_GID:-65532}"

log()  { printf '[init] %s\n' "$*"; }
die()  { printf '[init] ERROR: %s\n' "$*" >&2; exit 1; }

# A _FILE variant wins over the plain variable, so secrets can arrive as a mounted file instead of an environment
# entry visible in docker inspect. docker compose secrets land in /run/secrets/<name>.
read_secret() { # $1 variable name -> echoes the value
	local name="$1" file_var="${1}_FILE" file val
	eval "file=\${$file_var:-}"
	if [ -n "$file" ]; then
		[ -r "$file" ] || die "$file_var points at $file which is not readable"
		val=$(cat "$file")
	else
		eval "val=\${$name:-}"
	fi
	printf '%s' "$val"
}

export_cert_env() {
	export COUNTRY="${CERT_COUNTRY:-US}"
	export STATE="${CERT_STATE:-State}"
	export CITY="${CERT_CITY:-City}"
	export ORGANIZATION="${CERT_ORGANIZATION:-TAK}"
	export ORGANIZATIONAL_UNIT="${CERT_ORGANIZATIONAL_UNIT:-TAKOIDC}"
	export CAPASS="$(read_secret CERT_PASS)"
	[ -n "$CAPASS" ] || { CAPASS=atakatak; export CAPASS; }
	export PASS="$CAPASS"
}

fix_ownership() {
	chown -R "$TAK_UID:$TAK_GID" "$TAK_DIR"
	chmod 700 "$CERTS_DIR/files" 2>/dev/null || true
	[ -f "$TAK_DIR/CoreConfig.xml" ] && chmod 600 "$TAK_DIR/CoreConfig.xml"
	return 0
}

# Tomcat (8443, the web connector) is handed ONLY the first <crl> entry (ServerConfiguration.java:346
# crls.get(0)), and reads every CRL inside that one file (SSLUtilBase.generateCRLs). The messaging path
# (SSLConfig.initTrust) loads one CRL per entry. So the first entry is a PEM bundle of both CRLs, root first,
# and the intermediate stays as a second entry for messaging. Regenerate the bundle whenever either source changes.
refresh_crl_bundle() {
	( cd "$CERTS_DIR/files" && cat ca.crl intermediate-ca.crl > ca-chain.crl.tmp && mv -f ca-chain.crl.tmp ca-chain.crl )
}

xml_escape() {
	printf '%s' "$1" | sed -e 's/&/\&amp;/g' -e 's/</\&lt;/g' -e 's/>/\&gt;/g' -e 's/"/\&quot;/g'
}

subst() { # $1 placeholder token, $2 raw value -> substituted into $RENDER, XML escaped
	local value
	value=$(xml_escape "$2" | sed -e 's/[\\|&]/\\&/g')
	sed -i "s|$1|$value|g" "$RENDER"
}

############################################################################### certificates
cmd_certs() {
	mkdir -p "$CERTS_DIR"
	export_cert_env

	if [ ! -f "$CERTS_DIR/files/ca.pem" ]; then
		log "generating the certificate authority"
		cp "$SRC_DIR"/certs/* "$CERTS_DIR/"
		chmod +x "$CERTS_DIR"/*.sh
		( cd "$CERTS_DIR" && ./makeRootCa.sh --ca-name "${CA_NAME:-TAK-CONTAINER-CA}" )
	else
		log "certificate authority already present"
		# an interrupted bootstrap can leave the scripts missing; restore them without touching key material
		for f in "$SRC_DIR"/certs/*; do
			[ -f "$CERTS_DIR/$(basename "$f")" ] || cp "$f" "$CERTS_DIR/"
		done
		chmod +x "$CERTS_DIR"/*.sh
	fi

	# Each of these is checked individually, so an interrupted bootstrap repairs itself instead of being skipped
	# because ca.pem happened to exist.
	need_cert() { [ -f "$CERTS_DIR/files/$1.pem" ] || return 0; return 1; }
	if need_cert "${SERVER_CN:-takserver}"; then
		log "issuing server certificate ${SERVER_CN:-takserver}"
		( cd "$CERTS_DIR" && ./makeCert.sh server "${SERVER_CN:-takserver}" )
	fi
	if need_cert intermediate-ca; then
		log "issuing the intermediate signing CA"
		( cd "$CERTS_DIR" && ./makeCert.sh ca intermediate-ca )
	fi
	if need_cert "${ADMIN_CN:-admin}"; then
		log "issuing admin client certificate ${ADMIN_CN:-admin}"
		( cd "$CERTS_DIR" && ./makeCert.sh client "${ADMIN_CN:-admin}" )
	fi
	for extra in ${EXTRA_SERVER_CERTS:-}; do
		if need_cert "$extra"; then
			log "issuing extra server certificate $extra"
			( cd "$CERTS_DIR" && ./makeCert.sh server "$extra" )
		fi
		[ -f "$CERTS_DIR/files/$extra.nopass.key" ] || \
			openssl rsa -in "$CERTS_DIR/files/$extra.key" -passin "pass:$CAPASS" \
			            -out "$CERTS_DIR/files/$extra.nopass.key"
	done

	# CoreConfig names the server store takserver.jks whatever the CN is
	cp -f "$CERTS_DIR/files/${SERVER_CN:-takserver}.jks" "$CERTS_DIR/files/takserver.jks"
	cp -f "$CERTS_DIR/files/${SERVER_CN:-takserver}.p12" "$CERTS_DIR/files/takserver.p12"

	# CoreConfig references both CRLs, so both have to exist before TAK starts, even when empty. makeRootCa.sh and
	# makeCert.sh normally write them; these calls repair an interrupted bootstrap.
	[ -s "$CERTS_DIR/files/ca.crl" ]              || regen_crl root
	[ -s "$CERTS_DIR/files/intermediate-ca.crl" ] || regen_crl intermediate-ca
	refresh_crl_bundle

	export_for_other_containers
	mkdir -p "$TAK_DIR/logs"
	fix_ownership
	log "certificates ready"
}

# config.cfg sets dir = . and database = $dir/crl_index.txt, and upstream's scripts always run openssl from
# certs/files with -config ../config.cfg. Doing it from certs/ instead is what produces
# "Problem with index file: ./crl_index.txt". Same working directory as upstream, same relative config path.
regen_crl() { # $1: root, or the base name of the issuing CA (intermediate-ca)
	local ca="$1" keyfile certfile out
	if [ "$ca" = ca ] || [ "$ca" = root ]; then keyfile=ca-do-not-share.key; certfile=ca.pem; out=ca.crl
	else keyfile="$ca.key"; certfile="$ca.pem"; out="$ca.crl"; fi
	(
		cd "$CERTS_DIR/files"
		touch crl_index.txt crl_index.txt.attr
		grep -q unique_subject crl_index.txt.attr || echo "unique_subject = no" >> crl_index.txt.attr
		openssl ca -config ../config.cfg -gencrl -keyfile "$keyfile" -key "$CAPASS" -cert "$certfile" -out "$out"
	)
	log "regenerated certs/files/$out"
}

export_for_other_containers() {
	[ -d /opt/idp-certs ] || return 0
	for extra in ${EXTRA_SERVER_CERTS:-}; do
		# makeCert.sh already writes leaf + CA into <name>.pem. Appending ca.pem again produces a chain with a
		# duplicate root, which Keycloak rejects with java.security.KeyStoreException: Certificate chain is not valid.
		[ -f "$CERTS_DIR/files/$extra.pem" ] && cp -f "$CERTS_DIR/files/$extra.pem" "/opt/idp-certs/$extra.pem"
		[ -f "$CERTS_DIR/files/$extra.nopass.key" ] && cp -f "$CERTS_DIR/files/$extra.nopass.key" "/opt/idp-certs/$extra.nopass.key"
	done
	cp -f "$CERTS_DIR/files/ca.pem" /opt/idp-certs/ca.pem
	cp -f "$CERTS_DIR/files/truststore-root.jks" /opt/idp-certs/truststore-root.jks
	chown -R "${IDP_UID:-1000}:${IDP_GID:-0}" /opt/idp-certs
	chmod 640 /opt/idp-certs/*.key 2>/dev/null || true
	chmod 644 /opt/idp-certs/*.pem /opt/idp-certs/*.jks 2>/dev/null || true
}

############################################################################### CoreConfig.xml
render_config() { # writes $TAK_DIR/CoreConfig.xml
	local secret db_pass cert_pass
	db_pass="$(read_secret DB_PASSWORD)"
	[ -n "$db_pass" ] || die "DB_PASSWORD (or DB_PASSWORD_FILE) is required"
	cert_pass="$(read_secret CERT_PASS)"; [ -n "$cert_pass" ] || cert_pass=atakatak

	RENDER="$TAK_DIR/CoreConfig.xml.tmp"
	rm -f "$RENDER"
	cp "$SRC_DIR/CoreConfig.template.xml" "$RENDER"
	chmod 600 "$RENDER"

	subst __DB_URL__                     "${DB_URL:-jdbc:postgresql://db:5432/cot}"
	subst __DB_USERNAME__                "${DB_USERNAME:-martiuser}"
	subst __DB_PASSWORD__                "$db_pass"
	subst __DB_POOL__                    "${DB_POOL:-16}"
	subst __TOMCAT_MAX_POOL__            "${TOMCAT_MAX_POOL:-64}"
	subst __CERT_PASS__                  "$cert_pass"
	subst __CERT_ORGANIZATION__          "${CERT_ORGANIZATION:-TAK}"
	subst __CERT_ORGANIZATIONAL_UNIT__   "${CERT_ORGANIZATIONAL_UNIT:-TAKOIDC}"

	# CRL enforcement is generated and maintained unconditionally (both CRLs always exist and the revoke
	# subcommand always updates them) but it is only CONFIGURED when CRL_ENABLED=true. Turning it on in this lab
	# made TAK reject valid enrolment certificates on the 8443 client-certificate connector; see the named gap in
	# RUN.md section 9. The files are ready, so enabling it is a one-line change once that is understood.
	if [ "${CRL_ENABLED:-true}" != "true" ]; then
		sed -i "/<!--CRL-START-->/,/<!--CRL-END-->/d" "$RENDER"
	fi

	if [ "${OIDC_ENABLED:-false}" = "true" ]; then
		secret="$(read_secret OIDC_CLIENT_SECRET)"
		# Finding 1. An empty secret renders a configuration whose authorization code exchange always fails with
		# HTTP 500, and nothing later repairs it. Refuse instead.
		[ -n "$secret" ] || die "OIDC_ENABLED=true but OIDC_CLIENT_SECRET (or OIDC_CLIENT_SECRET_FILE) is empty. Provision the confidential client first, then run this with its secret. For a public client, set OIDC_CLIENT_SECRET=public and use a public client id."
		[ "$secret" = public ] && secret=""
		log "OIDC provider ${OIDC_NAME:-oidc} at ${OIDC_ISSUER:-} (client ${OIDC_CLIENT_ID:-}, secret supplied)"
		subst __OIDC_NAME__               "${OIDC_NAME:-oidc}"
		subst __OIDC_CLIENT_ID__          "${OIDC_CLIENT_ID:?OIDC_CLIENT_ID is required when OIDC_ENABLED=true}"
		subst __OIDC_CLIENT_SECRET__      "$secret"
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
		subst __OIDC_TRUSTSTORE_PASS__    "${OIDC_TRUSTSTORE_PASS:-$cert_pass}"

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
			rm -f /tmp/mappings.xml
		else
			sed -i "/<!--OIDC-GROUPMAPPING-->/d" "$RENDER"
		fi
	else
		log "OIDC disabled, removing the oauth block"
		sed -i "/<!--OIDC-START-->/,/<!--OIDC-END-->/d" "$RENDER"
	fi

	mv "$RENDER" "$TAK_DIR/CoreConfig.xml"
	chmod 600 "$TAK_DIR/CoreConfig.xml"
}

cmd_config() {
	if [ -f "$TAK_DIR/CoreConfig.xml" ] && [ "${FORCE:-0}" != 1 ]; then
		log "CoreConfig.xml already present, leaving it untouched (FORCE=1 to overwrite)"
	else
		[ -f "$TAK_DIR/CoreConfig.xml" ] && cp -f "$TAK_DIR/CoreConfig.xml" "$TAK_DIR/CoreConfig.xml.bak.$(date -u +%Y%m%dT%H%M%SZ)"
		log "rendering CoreConfig.xml"
		render_config
	fi
	[ -f "$TAK_DIR/TAKIgniteConfig.xml" ]        || cp "$SRC_DIR/TAKIgniteConfig.xml" "$TAK_DIR/TAKIgniteConfig.xml"
	[ -f "$TAK_DIR/UserAuthenticationFile.xml" ] || cp "$SRC_DIR/UserAuthenticationFile.xml" "$TAK_DIR/UserAuthenticationFile.xml"
	mkdir -p "$TAK_DIR/logs"
	fix_ownership
}

# Finding 1. The explicit update path. It rewrites the persisted file, which is the file the config microservice
# reads at start, so the three TAK containers have to be recreated afterwards; this prints the command.
cmd_update_oidc() {
	[ -f "$TAK_DIR/CoreConfig.xml" ] || die "no CoreConfig.xml to update; run 'config' first"
	cp -f "$TAK_DIR/CoreConfig.xml" "$TAK_DIR/CoreConfig.xml.bak.$(date -u +%Y%m%dT%H%M%SZ)"
	log "rewriting CoreConfig.xml from the current environment"
	render_config
	fix_ownership
	log "CoreConfig.xml updated. The config microservice reads it at start and distributes it over Ignite, so run:"
	log "  docker compose up -d --force-recreate takserver-config takserver-messaging takserver-api"
}

############################################################################### maintenance
cmd_make_cert() {
	[ $# -ge 2 ] || die "usage: make-cert <server|client|ca> <name>"
	export_cert_env
	( cd "$CERTS_DIR" && ./makeCert.sh "$1" "$2" )
	[ -f "$CERTS_DIR/files/$2.pem" ] || die "makeCert.sh did not produce files/$2.pem"
	fix_ownership
	log "issued $1 certificate $2"
}

# Finding 2. revokeCert.sh takes base names, not paths, changes into files/ itself and appends the extensions. It
# also needs PASS from cert-metadata.sh. Both are handled here, and the CA defaults to the intermediate, which is
# what online enrollment signs with.
cmd_revoke() {
	[ $# -ge 1 ] || die "usage: revoke <certificate-name> [root|intermediate]"
	local name="$1" which="${2:-intermediate}" cakey cacert crl
	export_cert_env
	case "$which" in
		root) cakey=ca-do-not-share; cacert=ca; crl="$CERTS_DIR/files/ca.crl" ;;
		intermediate) cakey=intermediate-ca; cacert=intermediate-ca; crl="$CERTS_DIR/files/intermediate-ca.crl" ;;
		*) die "second argument must be root or intermediate" ;;
	esac
	[ -f "$CERTS_DIR/files/$name.pem" ] || die "files/$name.pem does not exist"
	[ -f "$CERTS_DIR/files/$cakey.key" ] || die "files/$cakey.key does not exist"
	( cd "$CERTS_DIR" && ./revokeCert.sh "$name" "$cakey" "$cacert" )
	[ -s "$crl" ] || die "revokeCert.sh did not write $crl"
	openssl crl -in "$crl" -noout -text | grep -q "Serial Number" || die "$crl has no revoked entry"
	refresh_crl_bundle
	fix_ownership
	log "revoked $name against the $which CA; CRL now:"
	openssl crl -in "$crl" -noout -text | sed -n '1,40p'
	log "TAK reads the CRL when the TLS context is built, so run:"
	log "  docker compose up -d --force-recreate takserver-config takserver-messaging takserver-api"
}

# Finding 8. A complete artifact check, not the presence of one file.
cmd_verify() {
	local missing=0 f
	for f in CoreConfig.xml TAKIgniteConfig.xml UserAuthenticationFile.xml; do
		[ -f "$TAK_DIR/$f" ] || { echo "MISSING $TAK_DIR/$f"; missing=1; }
	done
	for f in ca.pem ca-do-not-share.key truststore-root.jks fed-truststore.jks takserver.jks takserver.p12 \
	         intermediate-ca.pem intermediate-ca.key intermediate-ca-signing.jks ca.crl intermediate-ca.crl ca-chain.crl \
	         "${ADMIN_CN:-admin}.pem" "${ADMIN_CN:-admin}.p12" "${SERVER_CN:-takserver}.pem"; do
		[ -f "$CERTS_DIR/files/$f" ] || { echo "MISSING $CERTS_DIR/files/$f"; missing=1; }
	done
	for extra in ${EXTRA_SERVER_CERTS:-}; do
		[ -f "$CERTS_DIR/files/$extra.pem" ] || { echo "MISSING $CERTS_DIR/files/$extra.pem"; missing=1; }
	done
	if [ -f "$TAK_DIR/CoreConfig.xml" ]; then
		grep -q '__[A-Z_]*__' "$TAK_DIR/CoreConfig.xml" && { echo "UNRENDERED placeholder in CoreConfig.xml"; missing=1; }
		if [ "${OIDC_ENABLED:-false}" = true ] && grep -q 'secret=""' "$TAK_DIR/CoreConfig.xml"; then
			echo "EMPTY OIDC client secret in CoreConfig.xml"; missing=1
		fi
	fi
	echo "CoreConfig.xml mode: $(stat -c '%a %U:%G' "$TAK_DIR/CoreConfig.xml" 2>/dev/null || echo absent)"
	echo "certs/files mode:    $(stat -c '%a %U:%G' "$CERTS_DIR/files" 2>/dev/null || echo absent)"
	[ "$missing" = 0 ] || die "artifact set incomplete"
	log "artifact set complete"
}

############################################################################### dispatch
cmd="${1:-init}"; shift || true
case "$cmd" in
	init)        cmd_certs; cmd_config; cmd_verify ;;
	certs)       cmd_certs ;;
	config)      cmd_config; cmd_verify ;;
	update-oidc) cmd_update_oidc; cmd_verify ;;
	make-cert)   cmd_make_cert "$@" ;;
	revoke)      cmd_revoke "$@" ;;
	verify)      cmd_verify ;;
	*)           die "unknown subcommand '$cmd'. One of: init certs config update-oidc make-cert revoke verify" ;;
esac
