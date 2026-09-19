# OIDC single sign-on for TAK Server (Phase 1)

TAK Server can authenticate WebTAK, the web admin UI and ATAK Civ against a standards-compliant OpenID Connect
provider on the same host or tactical LAN, with no route to the Internet. OIDC is a second, optional
authentication path: LDAP, file and certificate authentication are unchanged, and nothing in this document is
active until an `<openIdDiscoveryConfiguration>` element carries `enabled="true"` (R-01: optional, off by default).

Each section states BUILT (code and tests exist on this branch) or PENDING (not yet proven at that terminus).

## 1. Configuration (BUILT)

Everything lives under `<auth><oauth>` in CoreConfig.xml. Only the attributes below are new; every other
`<oauth>` attribute keeps its meaning.

```xml
<auth default="file" x509groups="true" x509addAnonymous="false">
  <oauth groupsClaim="groups"
         usernameClaim="preferred_username"
         adminTokenClaimName="groups" adminTokenClaimValue="tak_admin"
         identityKey="issuerSubject"
         externalAuthClockSkewSeconds="60"
         legacyGroupPassthrough="false"
         useTakServerLoginPage="true">
    <openIdDiscoveryConfiguration
        name="idp"
        enabled="true"
        configurationUri="https://idp.tactical.local/idp/.well-known/openid-configuration"
        issuer="https://idp.tactical.local/idp"
        clientId="e5130a25-9a34-481d-b330-d02487a3105b"
        secret="the web client secret"
        redirectUri="https://tak.tactical.local:8443/login/redirect"
        androidClientId="7d1f0c34-2b6e-4a1e-9f0e-0c7b2a5d6e81"
        audience="e5130a25-9a34-481d-b330-d02487a3105b,7d1f0c34-2b6e-4a1e-9f0e-0c7b2a5d6e81"
        scopes="openid profile groups"
        usePkce="true"
        truststore="JKS" truststoreFile="certs/files/idp-truststore.jks" truststorePass="atakatak"/>
    <groupMapping claimValue="tak_ops"   group="Operations" access="both"/>
    <groupMapping claimValue="tak_ro"    group="Operations" access="out"/>
    <groupMapping claimValue="tak_admin" group="Operations"/>
  </oauth>
</auth>
```

| Attribute | Meaning | Default |
|---|---|---|
| `enabled` | The Phase 1 switch. `false` (default): the element behaves exactly as before this change. `true`: everything in this document applies to it | `false` |
| `configurationUri` | The provider's discovery document. Must be `https`. IDCommand publishes it under the issuer path: `https://<host>/idp/.well-known/openid-configuration`. Keycloak: `https://<host>/realms/<realm>/.well-known/openid-configuration` | required |
| `issuer` | Expected `iss`. Required on an `enabled="true"` element: the discovery document's `issuer` must equal it exactly, every token's `iss` must equal it exactly, and it is how TAK recognises an OIDC-enrolled certificate while the provider is unreachable (section 2). An enabled element without it is refused at startup | required when enabled |
| `clientId`, `secret` | The confidential web client TAK uses for the browser flow | required |
| `redirectUri` | Registered at the IdP exactly as written, including the port | required |
| `androidClientId` | The public client ATAK uses (Authorization Code with PKCE, no secret). Published to clients on `/login/.well-known/openid-configuration` | none |
| `audience` | Comma-separated values; a token's `aud` must contain one of them (`azp` alone does not count). Keycloak operators add an audience mapper; IDCommand's ID token carries the client id | `clientId` plus `androidClientId` |
| `scopes` | Space-delimited tokens requested at the authorization endpoint; the exact token `openid` is required (`not-openid` is refused). Include the scope that carries your groups claim if the IdP needs one. Published to ATAK as a JSON array of the same tokens | `openid profile` |
| `androidRedirect` | Redirect shape ATAK uses, published as `android_redirect`: `scheme` (private-use URI `com.atakmap.app.civ:/oidc/callback`) or `loopback` (RFC 8252 section 7.3 listener on `127.0.0.1`). Any other value refuses registration naming the field. IDCommand's Admin Portal accepts only loopback for a public client: its Allowed Redirect URIs field says "HTTPS is required" and "http:// scheme can only be used with loopback/localhost addresses" (`spec/IDCOMMAND-FACTS.md`), so an IDCommand deployment sets `androidRedirect="loopback"` | `scheme` |
| `usePkce` | Send `code_challenge` (S256) on the browser flow | `true` |
| `truststore`, `truststoreFile`, `truststorePass` | The CA that signed the IdP's TLS certificate. Mirrors `ldapsTruststoreFile`. Unset means the JVM default trust store | unset |
| `trustAllCerts` | Pre-existing attribute. Refused on an `enabled="true"` element: startup logs an error naming the field and the provider is unavailable until it is removed (review v02 M1). A lab IdP with a private CA sets `truststore` and `truststoreFile` to that CA instead; the flag keeps its meaning on elements without `enabled="true"` | `false` |
| `<groupMapping claimValue group access>` | One value of the groups claim maps to one TAK group; `access` is `in`, `out` or `both`. Repeat the element to map one value to several groups | none |
| `defaultGroup` (on `<oauth>`) | TAK group granted when nothing maps. Unset means deny. Never an administrative group | unset |
| `legacyGroupPassthrough` (on `<oauth>`) | Only for installations that used an external IdP before this change and relied on claim values being TAK group names | `false` |
| `identityKey` (on `<oauth>`) | `issuerSubject` (recommended) keys the TAK user on `iss` + `sub`. `usernameClaim` keeps the pre-existing resolution (username claim, then email, then sub) for elements without `enabled="true"` only; with any `enabled="true"` element it is refused at registration, the log names the field and the provider stays unavailable, because R-04 requires `iss` + `sub` (review v02 M2) | `issuerSubject` |
| `externalAuthClockSkewSeconds` (on `<oauth>`) | Tolerance on `exp` and `nbf`, 0 to 300 | `60` |
| `adminTokenClaimName`, `adminTokenClaimValue` | Pre-existing. The only way an OIDC user becomes `ROLE_ADMIN` | none |
| `groupsClaim`, `usernameClaim`, `scopeClaim`, `webtakScope` | Pre-existing | |

Startup refuses, with the field named in the log, any non-`https` endpoint (configured or discovered), an
empty audience, a missing or mismatching pinned issuer, `trustAllCerts="true"`, a `truststore` without a
`truststoreFile`, and a clock skew outside 0 to 300.

Fail closed, for every surface including certificates: an enabled provider whose discovery has not succeeded is
"configured but unavailable", never "not configured". While it is unavailable, bearer tokens and browser logins
that name it are rejected, `/login/auth` and `/login/.well-known/openid-configuration` answer `503` with
`Retry-After` (the legacy flow never runs in its place), and a certificate enrolled through it is refused with an
audit line `outcome=DENY surface=CERT reason=provider-unavailable` rather than routed to the file, LDAP or
anonymous paths. The log carries one `NOT registered` line per failed provider and discovery is retried in the
background every 60 s. A provider that resolved once keeps its last good resolution across a failed refresh. The
other authenticators (file, LDAP, legacy certificates, legacy `authServer`) are unaffected throughout. A malformed
`<groupMapping>` (for example `group=""`) is logged and denies every OIDC identity; it does not stop the other
authenticators.

## 2. What the server does (BUILT, unit tested; integration PENDING until section 8 says otherwise)

Browser (`/login/auth` and `/login/redirect`): TAK generates `state`, `nonce` and a PKCE verifier, keeps them
server-side for ten minutes, and redirects to the IdP. On the callback it consumes the state exactly once (a
replayed callback is rejected before any network call), exchanges the code with `code_verifier`, validates the
ID token (signature against the IdP's JWKS, algorithm, `iss`, `aud` = the web client, `exp`, `nonce`), and sets
the session cookie. The cookie credential is the access token when it is a JWT from the same issuer, otherwise
the ID token. Every later request re-validates that credential.

Bearer (`Authorization: Bearer`, port 8446, and the session cookie): a JWT whose `iss` is a registered provider is
validated by that provider only: signature against its JWKS, permitted asymmetric algorithms only (`none` and HMAC
are refused before any key lookup), `exp` required and enforced, `iss` exact on the signed claim, `aud` containing a configured audience, `sub`
present. It never reaches the LDAP, file or anonymous fallbacks.

Identity: the TAK username is `oidc-` plus 40 hex characters of SHA-256 over the exact issuer and subject. It
is stable across email or display-name changes, distinct across issuers, safe as a certificate CN, and
distinct for subjects that differ only in case. `preferred_username` (or `usernameClaim`) is the display name.

Groups: the configured groups claim (array, or a space or comma separated string) is mapped through
`<groupMapping>` rules by exact match. Nothing else maps. An identity with no mapped group is denied unless
`defaultGroup` is set. `ROLE_ADMIN` comes only from `adminTokenClaimName`/`adminTokenClaimValue`.

WebTAK gate: when `webtakScope` is configured, the scope claim must be present and contain it as a whole token.

Certificates: a certificate issued on an OIDC enrollment (`signClient`, `signClient/v2` or `makeClientKeyStore`)
stores the enrollment assertion; on every certificate connection that assertion is re-verified (signature,
issuer, audience; expiry waived because the certificate is the credential) and mapped with the mapping rules in
force at connection time, so a mapping change applies to certificate clients without re-enrollment and a signing
key rotation at the IdP invalidates certificates enrolled under the old key until they re-enroll. This is the
replay-the-assertion semantics; a certificate-bound grant snapshot (schema change) is the alternative and is a
decision for the project owner. `useTokenExpiration="true"` on the CA config caps the certificate lifetime at the
assertion's `exp`; when that value cannot be read, signing is refused rather than defaulted.

R-13 caveat: the TAK transport is unchanged, but OIDC-enrolled devices now depend on the IdP being reachable from
TAK Server. Re-verifying the stored assertion needs the provider's JWKS (cached for `oAuthPublicKeyCacheSeconds`,
refreshed ahead of expiry), and a certificate is refused outright while its provider is configured but not
resolved. A prolonged IdP outage therefore denies OIDC-enrolled devices on 8089 and 8443; file, LDAP and
legacy-PKI certificates are not affected. Plan the IdP's availability with the data plane in mind, or keep a
legacy enrollment path for devices that must connect through an IdP outage.

Discovery: fetched at startup and re-fetched every `oAuthConfigurationsCacheSeconds` (minimum 60 s) in the
background. A refresh that fails for one provider keeps that provider's previous resolution; a refresh that can
resolve nothing keeps the whole previous generation; a cold start that can resolve nothing publishes a
"configured but unavailable" generation (section 1) and retries every 60 s. A generation that cannot be refreshed
expires after four periods, so a replacement configuration that never resolves cannot keep old grants alive
indefinitely. JWKS cached for
`oAuthPublicKeyCacheSeconds` (minimum 60 s) with refresh-ahead; an unknown key id triggers a refetch limited to
two per 30 s window per provider generation.

## 3. Audit (BUILT)

One line per decision on logger `tak.server.oidc.audit`, fixed schema, never a token, code or header:

```
outcome=ALLOW surface=BEARER provider=idp iss=https://idp.tactical.local/idp sub=7c1b... user=oidc-3f9a... port=8446 groups=[Operations:IN,Operations:OUT] admin=false reason=mapping
outcome=DENY  surface=BEARER provider=idp iss=https://idp.tactical.local/idp sub=9e2d... user=oidc-c0ff... port=8446 groups=[] admin=false reason=no-mapping
outcome=DENY  surface=BROWSER provider=idp iss=https://idp.tactical.local/idp sub=- user=- port=8443 groups=[] admin=false reason=nonce
```

`surface` is `BEARER` (API port or cookie), `BROWSER` (the callback) or `CERT` (a certificate connection
re-reading its enrollment assertion). `reason` on DENY: `iss`, `aud`, `exp`, `nbf`, `sig`, `alg`, `kid`,
`malformed`, `jwks`, `claims`, `nonce`, `state`, `exchange`, `no-mapping`. Route the logger to its own file in
`logback.xml` if you want a separate audit trail.

## 4. IDCommand (reference IdP)

In the Admin Portal, Applications, create two OIDC applications:

- Web: Client Type Confidential, Allowed Flow Types Authorization Code, Require PKCE on, Client Authentication
  Method Client Secret Post (TAK sends `client_secret` in the token request body, as upstream did), Allowed
  Redirect URI exactly `https://<tak-host>:8443/login/redirect`. Copy the client id (a UUID) and secret into
  `clientId` and `secret`.
- ATAK: Client Type Public, Authorization Code, Require PKCE on, Accept Plain Code Challenges off, Allowed
  Redirect URI as the ATAK build registers it (section 6). Copy the client id into `androidClientId`.

On both applications add an Attribute Mapping named `groups` with value source Groups, so the assertion carries
the user's IDCommand groups. `groupsClaim="groups"`. IDCommand's discovery document does not advertise
`code_challenge_methods_supported`; TAK sends PKCE regardless. The issuer carries a path
(`https://<host>/idp`); use it verbatim in `issuer` and `configurationUri`. IDCommand issues no refresh
tokens: a WebTAK session lasts until the assertion's `exp`, then the user signs in again.

Which token IDCommand's Attribute Mapping lands in (ID token, access token, userinfo) is not documented and
was not observable without a registered client: PENDING, one login against a tenant with the two applications
registered settles it. TAK accepts either the ID token or a JWT access token as the assertion, so the mapping
is configuration either way.

## 5. Keycloak (second IdP, used for the acceptance runs)

Realm with clients `tak-web` (confidential, standard flow, PKCE S256) and `atak-civ` (public, PKCE S256); a
client scope carrying a Group Membership mapper with claim name `groups`, full path off, added to the ID token
and access token; an Audience mapper adding `tak-web` (or set `audience` on the TAK side to include what
Keycloak puts in `aud`). The runtime scripts under the request's `qa/runtime/` build exactly this realm.

## 6. ATAK Civ (code BUILT on branch `lastwall/oidc-phase1` of atak-civ; APK build and device run PENDING)

The connection dialog gains "SSO (OIDC)". ATAK reads `issuer`, `android_client_id`, `scopes` and
`android_redirect` from `https://<tak>:8446/login/.well-known/openid-configuration`, opens the IdP in a Custom
Tab with PKCE S256, `state` and `nonce`, receives the redirect (private-use scheme
`com.atakmap.app.civ:/oidc/callback`, or an RFC 8252 loopback listener on 127.0.0.1 when `android_redirect` is
`loopback`), exchanges the code as a public client, checks the nonce, and enrolls through the existing bearer
enrollment path with the assertion; the CSR CN comes from the `identity` field of `/Marti/api/tls/config`.
No token is persisted; the issued certificate is the credential. Seven error states map to provider-neutral
messages. This box has no Android SDK, so the APK has not been built or run.

## 7. Legacy behaviour (R-01)

Without `enabled="true"` on a discovery element: identical to upstream, including an existing
`<openIdDiscoveryConfiguration>` (it keeps the pre-existing discovery, key pooling, name pass-through, anonymous
fallback and scope gate). With `enabled="true"`: tokens from that provider are checked for issuer and audience,
an identity with no mapped group is denied instead of being placed in the anonymous group, and the WebTAK scope
gate matches whole tokens and denies a missing claim. An installation that relied on claim values passing
through as TAK group names sets `legacyGroupPassthrough="true"` for the old mapping behaviour. Three upstream
defects on the legacy path are left as they are so the legacy path stays regression-equivalent; they are listed
for TAK Product Center separately (no issuer or audience check, anonymous fallback, substring scope match).

## 8. Evidence

| Claim | Evidence |
|---|---|
| Builds from upstream `5187abd` with Gradle 8.10.2, JDK 17 | `qa/gradle/02-compileJava.txt` |
| 110 unit tests in `tak.server.oidc` | `qa/gradle/05-test-oidc-review-fixes.txt` |
| The suite fails when the checks are removed | `qa/mutation-runs.md` (11 mutants), `qa/mutation-runs-2.md` (11 mutants, 10 killed, the hole closed) |
| Independent review of the branch | `qa/fleet/C-review-1.md` (17 findings, addressed in commit `eee2a66`), round two pending |
| Browser login, group mapping, deny, replay, planted tokens against a live IdP | PENDING: `qa/e2e/run-e2e.sh` against the local Keycloak runtime |
| Same flow against IDCommand | PENDING: needs two registered applications on a tenant |
| ATAK on a device | PENDING: needs an Android build |
