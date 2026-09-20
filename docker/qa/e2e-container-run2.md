# E2E run 2026-09-20T08:05:07Z KC=https://idp:8843 TAK=https://takserver-config:8446
PASS  0a IdP discovery reachable, issuer=https://idp:8843/realms/tak  (issuer=https://idp:8843/realms/tak pkce_s256=True)
PASS  1  /login/.well-known carries issuer, android_client_id, scopes  (https://idp:8843/realms/tak atak-civ ['openid', 'profile', 'groups'])
PASS  1b /login/.well-known scopes is a JSON array (D2)  (scopes=[openid, profile, groups])
PASS  1c /login/.well-known android_redirect is scheme|loopback (D2)  (android_redirect=scheme)
    alice claims: {'iss': 'https://idp:8843/realms/tak', 'aud': ['tak-web', 'account'], 'azp': 'probe', 'groups': ['tak_ops'], 'preferred_username': 'alice', 'sub': '3f471a0a-e584-4608-9bd9-5401248de565'}
PASS  2a alice Bearer -> /Marti/api/tls/config  (http=200)
PASS  2b tls/config carries identity (iss+sub key)  (identity=oidc-b5bf2b3fb22b9e7e2a03387d727a0d7597e2726c)
PASS  2c alice groups are exactly the mapped set  (http=200 groups=['Operations'])
    audit: 2026-09-20-08:02:56.448 [https-jsse-nio-8446-exec-10] WARN tak.server.oidc.audit - outcome=DENY surface=BEARER provider=keycloak iss=https://idp:8843/realms/tak sub=- user=- port=8446 groups=[] admin=false reason=alg
    audit: 2026-09-20-08:03:00.038 [https-jsse-nio-8446-exec-7] WARN tak.server.oidc.audit - outcome=DENY surface=BEARER provider=keycloak iss=https://idp:8843/realms/tak sub=- user=- port=8446 groups=[] admin=false reason=kid
    audit: 2026-09-20-08:04:19.359 [https-jsse-nio-8443-exec-5] INFO tak.server.oidc.audit - outcome=ALLOW surface=CERT provider=keycloak iss=https://idp:8843/realms/tak sub=3f471a0a-e584-4608-9bd9-5401248de565 user=oidc-b5bf2b3fb22b9e7e2a03387d727a0d7597e2726c port=8443 groups=[Operations:IN,Operations:OUT] admin=false reason=mapping
PASS  3  bob (no groups) is denied  (http=401)
    audit: 2026-09-20-08:03:00.038 [https-jsse-nio-8446-exec-7] WARN tak.server.oidc.audit - outcome=DENY surface=BEARER provider=keycloak iss=https://idp:8843/realms/tak sub=- user=- port=8446 groups=[] admin=false reason=kid
    audit: 2026-09-20-08:04:19.359 [https-jsse-nio-8443-exec-5] INFO tak.server.oidc.audit - outcome=ALLOW surface=CERT provider=keycloak iss=https://idp:8843/realms/tak sub=3f471a0a-e584-4608-9bd9-5401248de565 user=oidc-b5bf2b3fb22b9e7e2a03387d727a0d7597e2726c port=8443 groups=[Operations:IN,Operations:OUT] admin=false reason=mapping
    audit: 2026-09-20-08:05:25.553 [https-jsse-nio-8446-exec-7] WARN tak.server.oidc.audit - outcome=DENY surface=BEARER provider=keycloak iss=https://idp:8843/realms/tak sub=a84837cc-2105-496e-9555-06a7e2d7d0f1 user=oidc-df6f8b95641ac3837f9a2bb945d6a2247c40b3ed port=8446 groups=[] admin=false reason=no-mapping
PASS  4a carol (tak_admin claim) reaches an admin endpoint  (http=200)
PASS  4b alice (no admin claim) is refused the admin endpoint  (http=403)
PASS  5  planted aud token is rejected  (http=401)
PASS  5  planted iss token is rejected  (http=401)
PASS  5  planted exp token is rejected  (http=401)
PASS  5  planted none token is rejected  (http=401)
PASS  5  planted hs256 token is rejected  (http=401)
PASS  5  planted kid token is rejected  (http=401)
PASS  5  planted sig token is rejected  (http=401)
    audit: 2026-09-20-08:05:25.553 [https-jsse-nio-8446-exec-7] WARN tak.server.oidc.audit - outcome=DENY surface=BEARER provider=keycloak iss=https://idp:8843/realms/tak sub=a84837cc-2105-496e-9555-06a7e2d7d0f1 user=oidc-df6f8b95641ac3837f9a2bb945d6a2247c40b3ed port=8446 groups=[] admin=false reason=no-mapping
    audit: 2026-09-20-08:05:31.025 [https-jsse-nio-8446-exec-10] INFO tak.server.oidc.audit - outcome=ALLOW surface=BEARER provider=keycloak iss=https://idp:8843/realms/tak sub=11711732-b279-4bc3-88e1-5ebbaebc9fc7 user=oidc-14381cedc5c11d1fcfe51060e1dadcd87053ecf7 port=8446 groups=[Operations:IN,Operations:OUT] admin=true reason=mapping
    audit: 2026-09-20-08:05:51.277 [https-jsse-nio-8446-exec-8] WARN tak.server.oidc.audit - outcome=DENY surface=BEARER provider=keycloak iss=https://idp:8843/realms/tak sub=- user=- port=8446 groups=[] admin=false reason=kid
PASS  5z no token substring in the server log (T8)  (hits=0)
PASS  6  alice ID token accepted as bearer (aud=probe must be a configured audience for this to pass)  (http=200)
PASS  7a /login/auth redirects to the IdP with code_challenge and nonce  (challenge=1 nonce=1)
PASS  7b IdP login redirects back to TAK with a code  (callback=https://takserver-config:8446/login/redirect?state=uvG5mh65gDXlJ1jnJtQ2qma6Odec4MjFOWRVxEyCZ3Q&session_state=c3db18c4-856a-efb6-e1f4-c647d1ca91b6&iss=https%3A%2F%2Fidp%3A8843%2Frealms%2Ftak&code=REDACTED)
FAIL  7c first callback completes (cookie set)  (http=500)
FAIL  7d session cookie reaches TAK with the mapped groups  (http=403 groups=)
PASS  7e replayed callback is rejected (T7)  (http=400)
    audit: 2026-09-20-08:05:31.025 [https-jsse-nio-8446-exec-10] INFO tak.server.oidc.audit - outcome=ALLOW surface=BEARER provider=keycloak iss=https://idp:8843/realms/tak sub=11711732-b279-4bc3-88e1-5ebbaebc9fc7 user=oidc-14381cedc5c11d1fcfe51060e1dadcd87053ecf7 port=8446 groups=[Operations:IN,Operations:OUT] admin=true reason=mapping
    audit: 2026-09-20-08:05:51.277 [https-jsse-nio-8446-exec-8] WARN tak.server.oidc.audit - outcome=DENY surface=BEARER provider=keycloak iss=https://idp:8843/realms/tak sub=- user=- port=8446 groups=[] admin=false reason=kid
    audit: 2026-09-20-08:06:30.119 [https-jsse-nio-8446-exec-6] WARN tak.server.oidc.audit - outcome=DENY surface=BROWSER provider=keycloak iss=https://idp:8843/realms/tak sub=- user=- port=8446 groups=[] admin=false reason=exchange
PASS  8a tls/config carries identity  (http=200 identity=oidc-b5bf2b3fb22b9e7e2a03387d727a0d7597e2726c)
PASS  8b signClient/v2 with the bearer assertion issues a certificate  (http=200)
PASS  8c issued certificate CN is the identity key  (subject=CN=oidc-b5bf2b3fb22b9e7e2a03387d727a0d7597e2726c, O=Lastwall, OU=TAKOIDC)
PASS  8d certificate connection on 8443 carries exactly the mapped groups  (http=200 groups=['Operations'])
PASS  8e audit line with surface=CERT for that identity  (lines=2)

TOTAL pass=27 fail=2  (results: /qa/e2e-container-run2.md)
