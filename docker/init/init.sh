#!/bin/bash
#
# Bootstrap and maintenance for the minimal-footprint TAK Server container.
#
# The runtime image is distroless: no shell, no openssl, no editors. Everything that needs a shell happens here, and
# it happens through named subcommands so that nothing depends on an operator overriding the entrypoint and losing
# the environment the upstream cert scripts require.
#
#   init            bootstrap: certificates, then CoreConfig.xml. Generates certificates whose PEM is absent.
#   certs           certificates only. Run this before the IdP exists, so the IdP can be issued a certificate.
#   config          render CoreConfig.xml only. Refuses to overwrite an existing one unless FORCE=1.
#   update-oidc     FULL CoreConfig.xml regeneration from the template and current environment; backs up old XML.
#   make-cert T N   upstream makeCert.sh with the environment it needs. T is server, client or ca.
#   revoke N [CA]   revoke certificate N and regenerate the CRL of its issuing CA. CA is root or intermediate
#                   (auto-detected from the certificate issuer when omitted).
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
		val=$(cat "$file") || die "cannot read $file_var"
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
	CAPASS=$(read_secret CERT_PASS)
	export CAPASS
	[ -n "$CAPASS" ] || { CAPASS=atakatak; export CAPASS; }
	export PASS="$CAPASS"
}

fix_ownership() {
	chown -R "$TAK_UID:$TAK_GID" "$TAK_DIR"
	if [ -d "$CERTS_DIR/files" ]; then chmod 700 "$CERTS_DIR/files"; fi
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

# Upstream probes provider support with an expected failing grep and does not enable errexit.
# Convert only that known probe to a conditional, then run every real operation with -e/pipefail.
# Supply "n" to the CA promotion prompt: maintenance must never replace the root implicitly.
run_cert_script() (
	local script="$1" tmp
	shift
	cd "$CERTS_DIR"
	tmp=$(mktemp "$CERTS_DIR/.strict-cert.XXXXXX")
	trap 'rm -f "$tmp"' EXIT
	awk '
	/^openssl list -providers / {
	    print "if " $0 "; then :; else"; probe++; next
	}
	/^if \[ \$\? -ne 0 \] ; then/ { conditional++; next }
	{ print }
	END { if (probe != 1 || conditional != 1) exit 1 }
	' "$script" > "$tmp"
	bash -e -o pipefail "$tmp" "$@" <<< n
)

validate_cert_name() {
	[[ "$1" =~ ^[A-Za-z0-9][A-Za-z0-9_.-]*$ ]] || die "certificate name must be a base name, not a path"
}

verify_issued_cert() {
	local type="$1" name="$2" stem="$2" f
	if [ "$type" = ca ]; then stem="$name-signing"; fi
	for f in "$name.pem" "$name.key" "$stem.p12" "$stem.jks"; do
		[ -s "$CERTS_DIR/files/$f" ] || die "issuance did not produce $f"
	done
	openssl x509 -in "$CERTS_DIR/files/$name.pem" -noout >/dev/null
	# Upstream uses legacy PKCS12 encryption under OpenSSL 3.
	openssl pkcs12 -legacy -in "$CERTS_DIR/files/$stem.p12" -passin env:PASS -noout >/dev/null
	keytool -list -keystore "$CERTS_DIR/files/$stem.jks" -storepass "$PASS" -alias "$name" >/dev/null
	if [ "$type" = ca ]; then
		keytool -list -keystore "$CERTS_DIR/files/truststore-$name.jks" -storepass "$CAPASS" >/dev/null
	fi
}

############################################################################### certificates
cmd_certs() {
	mkdir -p "$CERTS_DIR"
	export_cert_env

	if [ ! -f "$CERTS_DIR/files/ca.pem" ]; then
		log "generating the certificate authority"
		cp "$SRC_DIR"/certs/* "$CERTS_DIR/"
		chmod +x "$CERTS_DIR"/*.sh
		run_cert_script makeRootCa.sh --ca-name "${CA_NAME:-TAK-CONTAINER-CA}"
	else
		log "certificate authority already present"
		# an interrupted bootstrap can leave the scripts missing; restore them without touching key material
		for f in "$SRC_DIR"/certs/*; do
			[ -f "$CERTS_DIR/$(basename "$f")" ] || cp "$f" "$CERTS_DIR/"
		done
		chmod +x "$CERTS_DIR"/*.sh
	fi

	# need_cert checks PEM existence only. Missing companion stores with an existing PEM
	# require operator recovery; verify blocks startup but does not repair those stores.
	need_cert() { [ -f "$CERTS_DIR/files/$1.pem" ] || return 0; return 1; }
	if need_cert "${SERVER_CN:-takserver}"; then
		log "issuing server certificate ${SERVER_CN:-takserver}"
		validate_cert_name "${SERVER_CN:-takserver}"
		run_cert_script makeCert.sh server "${SERVER_CN:-takserver}"
		verify_issued_cert server "${SERVER_CN:-takserver}"
	fi
	if need_cert intermediate-ca; then
		log "issuing the intermediate signing CA"
		validate_cert_name "intermediate-ca"
		run_cert_script makeCert.sh ca "intermediate-ca"
		verify_issued_cert ca "intermediate-ca"
	fi
	if need_cert "${ADMIN_CN:-admin}"; then
		log "issuing admin client certificate ${ADMIN_CN:-admin}"
		validate_cert_name "${ADMIN_CN:-admin}"
		run_cert_script makeCert.sh client "${ADMIN_CN:-admin}"
		verify_issued_cert client "${ADMIN_CN:-admin}"
	fi
	for extra in ${EXTRA_SERVER_CERTS:-}; do
		if need_cert "$extra"; then
			log "issuing extra server certificate $extra"
			validate_cert_name "$extra"
			run_cert_script makeCert.sh server "$extra"
			verify_issued_cert server "$extra"
		fi
		[ -f "$CERTS_DIR/files/$extra.nopass.key" ] || \
			openssl rsa -in "$CERTS_DIR/files/$extra.key" -passin "pass:$CAPASS" \
			            -out "$CERTS_DIR/files/$extra.nopass.key"
	done

	# CoreConfig names the server store takserver.jks whatever the CN is
	cp -f "$CERTS_DIR/files/${SERVER_CN:-takserver}.jks" "$CERTS_DIR/files/takserver.jks"
	cp -f "$CERTS_DIR/files/${SERVER_CN:-takserver}.p12" "$CERTS_DIR/files/takserver.p12"

	# Rebuild from isolated indexes even if upstream generated an initial CRL using its shared config.
	# A legacy shared index must be migrated before this can proceed.
	regen_crl root
	regen_crl intermediate-ca
	refresh_crl_bundle

	export_for_other_containers
	mkdir -p "$TAK_DIR/logs"
	fix_ownership
	log "certificates ready"
}

# Never reuse the shared upstream index for a different issuer. Old nonempty shared
# indexes cannot be split safely by subject alone; require an audited migration first.
ca_config() ( # root or intermediate-ca -> config path relative to certs/files
	set -e
	local ca="$1" index="crl-index-$1.txt" cfg="crl-$1.cfg"
	(
		cd "$CERTS_DIR/files"
		if [ ! -f "$index" ]; then
			[ ! -s crl_index.txt ] || die "legacy shared revocation index is nonempty; migrate verified issuer-specific records to $index before maintenance"
			: > "$index"
		fi
		printf 'unique_subject = no\n' > "$index.attr"
		awk -v db="$index" '
		/^[[:space:]]*database[[:space:]]*=/ { print "database = $dir/" db; n++; next }
		{ print }
		END { if (n != 1) exit 1 }
		' ../config.cfg > "$cfg.tmp"
		mv "$cfg.tmp" "$cfg"
	)
	printf '%s' "$cfg"
)

verify_crl() {
    local result
    result=$(openssl crl -in "$1" -noout -verify -CAfile "$2" 2>&1) || die "CRL verification command failed"
    # Some OpenSSL releases print a verification failure without returning failure.
    [ "$result" = "verify OK" ] || die "CRL signature verification failed: $result"
}

regen_crl() { # root or intermediate-ca
	local ca="$1" keyfile certfile out cfg
	case "$ca" in
		root) keyfile=ca-do-not-share.key; certfile=ca.pem; out=ca.crl ;;
		intermediate-ca) keyfile=intermediate-ca.key; certfile=intermediate-ca.pem; out=intermediate-ca.crl ;;
		*) die "unsupported CRL issuer $ca" ;;
	esac
	cfg=$(ca_config "$ca")
	(
		cd "$CERTS_DIR/files"
		openssl ca -config "$cfg" -gencrl -keyfile "$keyfile" -passin env:CAPASS -cert "$certfile" -out "$out.tmp"
		verify_crl "$out.tmp" "$certfile"
		mv "$out.tmp" "$out"
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
	for f in /opt/idp-certs/*.key; do [ ! -e "$f" ] || chmod 640 "$f"; done
	for f in /opt/idp-certs/*.pem /opt/idp-certs/*.jks; do [ ! -e "$f" ] || chmod 644 "$f"; done
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

	# The first configured CRL is the root/intermediate bundle required by the web connector.
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
	log "FULL regeneration of CoreConfig.xml from the template and current environment; unrelated manual XML edits are replaced (backup saved)"
	render_config
	fix_ownership
	log "CoreConfig.xml updated. The config microservice reads it at start and distributes it over Ignite, so run:"
	log "  docker compose up -d --force-recreate takserver-config takserver-messaging takserver-api"
}

############################################################################### maintenance
cmd_make_cert() {
	[ $# -eq 2 ] || die "usage: make-cert <server|client|ca> <name>"
	case "$1" in server|client|ca) ;; *) die "unsupported certificate type $1" ;; esac
	validate_cert_name "$2"
	export_cert_env
	run_cert_script makeCert.sh "$1" "$2"
	verify_issued_cert "$1" "$2"
	fix_ownership
	log "issued $1 certificate $2"
}

cmd_revoke() {
	[ $# -ge 1 ] && [ $# -le 2 ] || die "usage: revoke <certificate-name> [root|intermediate]"
	local name="$1" which="${2:-}" issuer subject candidate matches=0 detected=""
	local cakey cacert crl cfg serial crl_text
	validate_cert_name "$name"
	export_cert_env
	issuer=$(openssl x509 -in "$CERTS_DIR/files/$name.pem" -noout -issuer -nameopt RFC2253)
	issuer=${issuer#issuer=}
	for candidate in root intermediate; do
		if [ "$candidate" = root ]; then cacert=ca; else cacert=intermediate-ca; fi
		subject=$(openssl x509 -in "$CERTS_DIR/files/$cacert.pem" -noout -subject -nameopt RFC2253)
		if [ "$issuer" = "${subject#subject=}" ]; then
			detected="$candidate"; matches=$((matches+1))
		fi
	done
	[ "$matches" = 1 ] || die "certificate issuer does not identify exactly one configured CA"
	[ -z "$which" ] || [ "$which" = "$detected" ] || die "requested CA $which mismatches certificate issuer $detected"
	which="$detected"
	case "$which" in
		root) cakey=ca-do-not-share; cacert=ca; crl=ca.crl; candidate=root ;;
		intermediate) cakey=intermediate-ca; cacert=intermediate-ca; crl=intermediate-ca.crl; candidate=intermediate-ca ;;
	esac
	# Check the signature too: a matching issuer DN alone is insufficient.
	openssl verify -no_check_time -partial_chain -trusted "$CERTS_DIR/files/$cacert.pem" "$CERTS_DIR/files/$name.pem"
	serial=$(openssl x509 -in "$CERTS_DIR/files/$name.pem" -noout -serial)
	serial=${serial#serial=}
	cfg=$(ca_config "$candidate")
	(
		cd "$CERTS_DIR/files"
		openssl ca -config "$cfg" -revoke "$name.pem" -keyfile "$cakey.key" -passin env:CAPASS -cert "$cacert.pem"
	)
	regen_crl "$candidate"
	verify_crl "$CERTS_DIR/files/$crl" "$CERTS_DIR/files/$cacert.pem"
	crl_text=$(openssl crl -in "$CERTS_DIR/files/$crl" -noout -text)
	printf '%s\n' "$crl_text" | awk -v wanted="$serial" '
	function norm(s) { s=toupper(s); gsub(/:/,"",s); sub(/^0+/,"",s); return s }
	/Serial Number:/ { if (norm($NF) == norm(wanted)) found=1 }
	END { exit !found }
	' || die "requested serial $serial absent from $crl"
	refresh_crl_bundle
	fix_ownership
	log "revoked $name serial $serial against the $which CA; verified signed CRL $crl"
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
