#!/bin/bash
#
# Provisions the lab realm "tak" in the compose Keycloak, using the same realm shape phase 1 proved:
# clients tak-web (confidential, PKCE S256), atak-civ (public, PKCE S256), probe (direct access grants only);
# client scope "groups" with a group membership mapper and an audience mapper; groups tak_ops and tak_admin;
# users alice (tak_ops), carol (tak_admin + tak_ops), bob (no groups).
#
# Differences from the host version in the phase 1 request directory: kcadm runs inside the idp container, the
# redirect URI is the container hostname on the 8446 connector (8443 demands a client certificate), and the
# truststore is the JKS the init container exported.
#
# Writes the client secrets to ../.env.secrets (mode 600) for the e2e run.
set -euo pipefail
cd "$(dirname "$0")/.."

TRUST="/opt/idp-certs/truststore-root.jks"
KCOPTS="-Djavax.net.ssl.trustStore=$TRUST -Djavax.net.ssl.trustStoreType=JKS -Djavax.net.ssl.trustStorePassword=${CERT_PASS:-atakatak}"
KCURL="${KCURL:-https://idp:8843}"
REDIRECT_URI="${REDIRECT_URI:-https://takserver-config:8446/login/redirect}"

kc() { docker compose exec -T -e KC_OPTS="$KCOPTS" idp /opt/keycloak/bin/kcadm.sh "$@"; }

kc config credentials --server "$KCURL" --realm master \
  --user "${KC_ADMIN:-admin}" --password "${KC_ADMIN_PASSWORD:?set KC_ADMIN_PASSWORD}"

kc delete realms/tak 2>/dev/null && echo "deleted existing realm tak" || true
kc create realms -s realm=tak -s enabled=true -s displayName="TAK OIDC container lab" \
  -s accessTokenLifespan=3600 -s ssoSessionIdleTimeout=3600
echo "realm tak created"

kc create groups -r tak -s name=tak_ops
kc create groups -r tak -s name=tak_admin
OPS_ID=$(kc get groups -r tak -q search=tak_ops --fields id,name | jq -r '.[]|select(.name=="tak_ops").id')
ADM_ID=$(kc get groups -r tak -q search=tak_admin --fields id,name | jq -r '.[]|select(.name=="tak_admin").id')
echo "groups: tak_ops=$OPS_ID tak_admin=$ADM_ID"

SCOPE_ID=$(kc create client-scopes -r tak -i -s name=groups -s protocol=openid-connect \
  -s 'attributes."include.in.token.scope"=true' -s 'attributes."display.on.consent.screen"=false')
kc create "client-scopes/$SCOPE_ID/protocol-mappers/models" -r tak \
  -s name=groups -s protocol=openid-connect -s protocolMapper=oidc-group-membership-mapper \
  -s 'config."claim.name"=groups' -s 'config."full.path"=false' \
  -s 'config."id.token.claim"=true' -s 'config."access.token.claim"=true' -s 'config."userinfo.token.claim"=true'
kc create "client-scopes/$SCOPE_ID/protocol-mappers/models" -r tak \
  -s name=aud-tak-web -s protocol=openid-connect -s protocolMapper=oidc-audience-mapper \
  -s 'config."included.client.audience"=tak-web' -s 'config."id.token.claim"=true' \
  -s 'config."access.token.claim"=true' -s 'config."introspection.token.claim"=true'
echo "client scope groups=$SCOPE_ID"

TAKWEB_ID=$(kc create clients -r tak -i -s clientId=tak-web -s protocol=openid-connect -s enabled=true \
  -s publicClient=false -s standardFlowEnabled=true -s implicitFlowEnabled=false \
  -s directAccessGrantsEnabled=false -s serviceAccountsEnabled=false \
  -s "redirectUris=[\"$REDIRECT_URI\"]" -s 'webOrigins=["+"]' \
  -s 'attributes."pkce.code.challenge.method"=S256' -s 'attributes."post.logout.redirect.uris"=+')
kc update "clients/$TAKWEB_ID/default-client-scopes/$SCOPE_ID" -r tak
TAKWEB_SECRET=$(kc get "clients/$TAKWEB_ID/client-secret" -r tak | jq -r .value)

ATAK_ID=$(kc create clients -r tak -i -s clientId=atak-civ -s protocol=openid-connect -s enabled=true \
  -s publicClient=true -s standardFlowEnabled=true -s implicitFlowEnabled=false -s directAccessGrantsEnabled=false \
  -s 'redirectUris=["com.atakmap.app.civ:/oidc/callback","http://127.0.0.1/*"]' \
  -s 'attributes."pkce.code.challenge.method"=S256')
kc update "clients/$ATAK_ID/default-client-scopes/$SCOPE_ID" -r tak

PROBE_ID=$(kc create clients -r tak -i -s clientId=probe -s protocol=openid-connect -s enabled=true \
  -s publicClient=false -s standardFlowEnabled=false -s directAccessGrantsEnabled=true -s serviceAccountsEnabled=false)
kc update "clients/$PROBE_ID/default-client-scopes/$SCOPE_ID" -r tak
PROBE_SECRET=$(kc get "clients/$PROBE_ID/client-secret" -r tak | jq -r .value)
echo "clients: tak-web=$TAKWEB_ID atak-civ=$ATAK_ID probe=$PROBE_ID"

mkuser() {
  local u=$1; shift
  local uid
  uid=$(kc create users -r tak -i -s "username=$u" -s enabled=true -s emailVerified=true -s "email=$u@tak.local" \
        -s "firstName=$u" -s lastName=Lab)
  kc set-password -r tak --username "$u" --new-password "$u"
  for g in "$@"; do kc update "users/$uid/groups/$g" -r tak -s realm=tak -s "userId=$uid" -s "groupId=$g" -n; done
  echo "user $u=$uid groups=[$*]"
}
mkuser alice "$OPS_ID"
mkuser carol "$ADM_ID" "$OPS_ID"
mkuser bob

umask 077
cat > .env.secrets <<EOS
TAKWEB_CLIENT_SECRET=$TAKWEB_SECRET
PROBE_CLIENT_SECRET=$PROBE_SECRET
EOS
echo "secrets written to $(pwd)/.env.secrets"
