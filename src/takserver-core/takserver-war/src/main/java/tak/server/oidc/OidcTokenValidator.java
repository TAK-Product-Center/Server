package tak.server.oidc;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.DefaultJOSEObjectTypeVerifier;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.Resource;
import com.nimbusds.jose.util.ResourceRetriever;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;

import tak.server.oidc.OidcValidationException.Reason;

/**
 * Validates a JWT from one configured external OIDC provider and returns the identity it proves.
 *
 * Replaces, for external tokens, the try-every-key loop in {@code JwtUtils.parseClaims}. One provider per
 * instance: the key set, the issuer and the accepted audiences are bound together, so a token minted by another
 * issuer whose keys happen to be configured elsewhere can never verify here (threat model T2).
 *
 * Checks, in order: JWS structure and permitted algorithm family, signature against the provider's JWKS
 * (fetched from the configured {@code jwks_uri} only, cached, one bounded refetch on an unknown key id),
 * {@code exp}/{@code nbf} with clock skew, {@code iss} equality, {@code aud} or {@code azp} membership, and
 * presence of {@code sub}. Every rejection carries a stable {@link Reason} for the audit event; the token
 * itself is never included in the exception message.
 *
 * Built on {@code spring-security-oauth2-jose} and Nimbus, both already on the TAK Server classpath.
 */
public final class OidcTokenValidator {

    /** Everything the validator needs about one provider. Immutable; construction validates it. */
    public static final class Settings {
        private final String providerName;
        private final String issuer;
        private final String jwksUri;
        private final Set<String> audiences;
        private final Duration clockSkew;
        private final Set<SignatureAlgorithm> algorithms;
        private final Duration jwksCacheTtl;

        /** Default JWKS lifetime when the operator has not set oAuthPublicKeyCacheSeconds: 12 hours, as upstream. */
        public static final Duration DEFAULT_JWKS_TTL = Duration.ofHours(12);

        /**
         * @param providerName operator-facing name, used in audit events
         * @param issuer       exact {@code iss} value the provider publishes; must be https
         * @param jwksUri      the provider's {@code jwks_uri}; must be https
         * @param audiences    accepted {@code aud} (or {@code azp}) values; must not be empty
         * @param clockSkew    tolerance for {@code exp}/{@code nbf}; null means 60 seconds
         * @param algorithms   permitted JWS algorithms; null means every asymmetric algorithm. The type is
         *                     Spring's asymmetric-only enum, so HMAC cannot be named here; "none" is rejected
         *                     by the decoder.
         */
        public Settings(String providerName, String issuer, String jwksUri, Collection<String> audiences,
                Duration clockSkew, Collection<SignatureAlgorithm> algorithms) {
            this(providerName, issuer, jwksUri, audiences, clockSkew, algorithms, null);
        }

        /** @param jwksCacheTtl how long a fetched key set is served before a background refresh; null = 12 h */
        public Settings(String providerName, String issuer, String jwksUri, Collection<String> audiences,
                Duration clockSkew, Collection<SignatureAlgorithm> algorithms, Duration jwksCacheTtl) {
            this.jwksCacheTtl = (jwksCacheTtl == null || jwksCacheTtl.isZero() || jwksCacheTtl.isNegative())
                    ? DEFAULT_JWKS_TTL : jwksCacheTtl;
            this.providerName = (providerName == null || providerName.isEmpty()) ? "oidc" : providerName;
            this.issuer = requireHttps("issuer", issuer);
            this.jwksUri = requireHttps("jwksUri", jwksUri);
            if (audiences == null || audiences.isEmpty()) {
                throw new IllegalArgumentException("oidc provider '" + this.providerName
                        + "': at least one accepted audience is required");
            }
            Set<String> auds = new LinkedHashSet<>();
            for (String a : audiences) {
                if (a != null && !a.trim().isEmpty()) {
                    auds.add(a.trim());
                }
            }
            if (auds.isEmpty()) {
                throw new IllegalArgumentException("oidc provider '" + this.providerName
                        + "': at least one accepted audience is required");
            }
            this.audiences = Collections.unmodifiableSet(auds);
            this.clockSkew = clockSkew == null ? Duration.ofSeconds(60) : clockSkew;
            Set<SignatureAlgorithm> algs = EnumSet.noneOf(SignatureAlgorithm.class);
            if (algorithms == null || algorithms.isEmpty()) {
                algs.addAll(ASYMMETRIC);
            } else {
                for (SignatureAlgorithm a : algorithms) {
                    if (ASYMMETRIC.contains(a)) {
                        algs.add(a);
                    }
                }
                if (algs.isEmpty()) {
                    throw new IllegalArgumentException("oidc provider '" + this.providerName
                            + "': no permitted asymmetric signature algorithm");
                }
            }
            this.algorithms = Collections.unmodifiableSet(algs);
        }

        private static String requireHttps(String field, String value) {
            if (value == null || value.trim().isEmpty()) {
                throw new IllegalArgumentException("oidc " + field + " is required");
            }
            URI uri;
            try {
                uri = URI.create(value.trim());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("oidc " + field + " is not a valid URI", e);
            }
            if (uri.getScheme() == null || !uri.getScheme().toLowerCase(Locale.ENGLISH).equals("https")) {
                throw new IllegalArgumentException("oidc " + field + " must use https (T24): " + value);
            }
            return value.trim();
        }

        public String getProviderName() {
            return providerName;
        }

        public String getIssuer() {
            return issuer;
        }

        public String getJwksUri() {
            return jwksUri;
        }

        public Set<String> getAudiences() {
            return audiences;
        }

        public Duration getClockSkew() {
            return clockSkew;
        }

        public Set<SignatureAlgorithm> getAlgorithms() {
            return algorithms;
        }

        public Duration getJwksCacheTtl() {
            return jwksCacheTtl;
        }
    }

    /** The only algorithms ever accepted. No HMAC family, so a public key can never be used as a secret. */
    static final Set<SignatureAlgorithm> ASYMMETRIC = Collections.unmodifiableSet(EnumSet.of(
            SignatureAlgorithm.RS256, SignatureAlgorithm.RS384, SignatureAlgorithm.RS512,
            SignatureAlgorithm.PS256, SignatureAlgorithm.PS384, SignatureAlgorithm.PS512,
            SignatureAlgorithm.ES256, SignatureAlgorithm.ES384, SignatureAlgorithm.ES512));

    private static final String AUD_ERROR_MARKER = "aud/azp";

    private final Settings settings;
    private final JwtDecoder decoder;
    private final JwtDecoder decoderIgnoringTime;

    /** Production constructor: JWKS fetched with a default RestTemplate (caller supplies a truststore-aware one). */
    public OidcTokenValidator(Settings settings) {
        this(settings, new RestTemplate());
    }

    /**
     * @param rest the HTTP client used to fetch the JWKS. Production passes one whose SSL context trusts the
     *             tactical CA; tests pass a stub that serves a canned JWKS.
     */
    /** Minimum interval between two JWKS refreshes triggered by unknown key ids (T5, T19). */
    static final Duration JWKS_REFRESH_RATE_LIMIT = Duration.ofSeconds(30);
    /** Time allowed for one JWKS fetch. */
    static final Duration JWKS_FETCH_TIMEOUT = Duration.ofSeconds(10);

    public OidcTokenValidator(Settings settings, RestOperations rest) {
        this.settings = settings;
        // One key source for both decoders: one cache, one rate limiter, one fetch per rotation.
        JWKSource<SecurityContext> keys = keySource(settings, rest);
        this.decoder = build(settings, keys, true);
        this.decoderIgnoringTime = build(settings, keys, false);
    }

    /**
     * The provider's published keys, fetched from {@code jwks_uri} only. Cached for the configured lifetime,
     * refreshed early in the background, and, on an unknown key id, refetched at most once per
     * {@link #JWKS_REFRESH_RATE_LIMIT}: a stream of junk tokens with fresh {@code kid}s cannot turn TAK Server
     * into a request generator against the IdP (T19). A failed refresh does not serve stale keys forever:
     * outage tolerance is off, so after the cache lifetime a provider that has gone away fails closed.
     */
    private static JWKSource<SecurityContext> keySource(Settings settings, RestOperations rest) {
        URL url;
        try {
            url = new URI(settings.jwksUri).toURL();
        } catch (Exception e) {
            throw new IllegalArgumentException("oidc provider '" + settings.providerName + "': jwks_uri is not a URL", e);
        }
        return JWKSourceBuilder.create(url, new RestResourceRetriever(rest))
                .cache(settings.jwksCacheTtl.toMillis(), JWKS_FETCH_TIMEOUT.toMillis())
                .refreshAheadCache(true)
                .rateLimited(JWKS_REFRESH_RATE_LIMIT.toMillis())
                .retrying(false)
                .outageTolerant(false)
                .build();
    }

    private static JwtDecoder build(Settings settings, JWKSource<SecurityContext> keys, boolean enforceTime) {
        Set<JWSAlgorithm> algs = new LinkedHashSet<>();
        for (SignatureAlgorithm a : settings.algorithms) {
            algs.add(JWSAlgorithm.parse(a.getName()));
        }
        DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
        // "JWT", RFC 9068 "at+jwt", or no typ. Anything else (for example a logout token) is not an assertion.
        processor.setJWSTypeVerifier(new DefaultJOSEObjectTypeVerifier<>(
                JOSEObjectType.JWT, new JOSEObjectType("at+jwt"), null));
        processor.setJWSKeySelector(new JWSVerificationKeySelector<>(algs, keys));
        // Claims are checked by the Spring validators below, with stable reason codes; Nimbus's default
        // verifier would reject expired tokens with a message the reason classifier does not see.
        processor.setJWTClaimsSetVerifier((claims, context) -> { });
        NimbusJwtDecoder nimbus = new NimbusJwtDecoder(processor);

        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
        if (enforceTime) {
            validators.add(new RequiredExpiryValidator());
            validators.add(new JwtTimestampValidator(settings.clockSkew));
        }
        validators.add(new ExactIssuerValidator(settings.issuer));
        validators.add(new AudienceValidator(settings.audiences));
        nimbus.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validators));
        return nimbus;
    }

    /** Fetches the JWKS through the caller's HTTP client (truststore-aware in production, a stub in tests). */
    static final class RestResourceRetriever implements ResourceRetriever {
        private final RestOperations rest;

        RestResourceRetriever(RestOperations rest) {
            this.rest = rest;
        }

        @Override
        public Resource retrieveResource(URL url) throws IOException {
            try {
                ResponseEntity<String> response = rest.exchange(
                        RequestEntity.get(url.toURI()).header(HttpHeaders.ACCEPT, "application/json").build(),
                        String.class);
                if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                    throw new IOException("jwks fetch returned HTTP " + response.getStatusCode().value());
                }
                return new Resource(response.getBody(), response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE));
            } catch (URISyntaxException e) {
                throw new IOException("jwks_uri is not a URI", e);
            } catch (RuntimeException e) {
                // The client's message can carry a response body; the class name is enough for the audit trail.
                throw new IOException("jwks fetch failed: " + e.getClass().getSimpleName(), e);
            }
        }
    }

    /** {@code exp} must be present. Spring's timestamp validator skips a token that has none. */
    static final class RequiredExpiryValidator implements OAuth2TokenValidator<Jwt> {
        @Override
        public OAuth2TokenValidatorResult validate(Jwt jwt) {
            if (jwt.getExpiresAt() == null) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token",
                        "The exp claim is required and is missing", null));
            }
            return OAuth2TokenValidatorResult.success();
        }
    }

    /**
     * {@code iss} must equal the configured issuer as a string. Spring's {@code JwtIssuerValidator} compares
     * after converting through {@code java.net.URL}, which trims whitespace and lower-cases the scheme; the
     * routing in {@code OidcProviderRegistry} compares the raw claim, and validation must agree with it.
     */
    static final class ExactIssuerValidator implements OAuth2TokenValidator<Jwt> {
        private final String issuer;

        ExactIssuerValidator(String issuer) {
            this.issuer = issuer;
        }

        @Override
        public OAuth2TokenValidatorResult validate(Jwt jwt) {
            Object iss = jwt.getClaims().get(JwtClaimNames.ISS);
            String raw = iss instanceof URL ? iss.toString() : (iss == null ? null : String.valueOf(iss));
            if (raw == null || !raw.equals(issuer)) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token",
                        "The iss claim is not the configured issuer", null));
            }
            return OAuth2TokenValidatorResult.success();
        }
    }

    public Settings getSettings() {
        return settings;
    }

    /**
     * @return the identity the token proves
     * @throws OidcValidationException with a stable reason when the token is rejected for any cause
     */
    public OidcIdentity validate(String token) throws OidcValidationException {
        return validate(token, false);
    }

    /**
     * @param ignoreTime when true, {@code exp} and {@code nbf} are not enforced. Signature, algorithm, issuer,
     *                   audience and {@code sub} still are. Used only for the token a certificate client presented
     *                   at enrollment and TAK stored with the certificate: the certificate is the credential there
     *                   and the token is the group snapshot it was issued against (gap analysis decision 4). Never
     *                   for a token presented by a client.
     * @return the identity the token proves
     * @throws OidcValidationException with a stable reason when the token is rejected for any cause
     */
    public OidcIdentity validate(String token, boolean ignoreTime) throws OidcValidationException {
        if (token == null || token.isEmpty()) {
            throw new OidcValidationException(Reason.MALFORMED, "empty token");
        }
        rejectUnpermittedAlgorithm(token);
        Jwt jwt;
        try {
            jwt = (ignoreTime ? decoderIgnoringTime : decoder).decode(token);
        } catch (JwtValidationException e) {
            throw new OidcValidationException(classifyValidation(e), describe(e), e);
        } catch (BadJwtException e) {
            throw new OidcValidationException(classifyBad(e), scrub(e.getMessage()), e);
        } catch (JwtException e) {
            throw new OidcValidationException(classifyOther(e), scrub(e.getMessage()), e);
        }

        // Exact issuer equality on the raw claim. Spring converts iss through java.net.URL before its validators
        // see it (scheme lower-cased, whitespace trimmed), so the converted value can equal the configured issuer
        // when the signed string does not. The signature was verified over these same bytes, so re-reading the
        // raw claim after decode is sound.
        String rawIssuer;
        try {
            rawIssuer = JWTParser.parse(token).getJWTClaimsSet().getIssuer();
        } catch (java.text.ParseException e) {
            throw new OidcValidationException(Reason.MALFORMED, "claims not parseable");
        }
        if (rawIssuer == null || !rawIssuer.equals(settings.issuer)) {
            throw new OidcValidationException(Reason.ISS, "iss claim is not the configured issuer");
        }

        String sub = jwt.getSubject();
        if (sub == null || sub.isEmpty()) {
            throw new OidcValidationException(Reason.CLAIMS, "sub missing");
        }
        Map<String, Object> claims = new HashMap<>(jwt.getClaims());
        return new OidcIdentity(settings.providerName, settings.issuer, sub, claims);
    }

    /**
     * The unverified {@code iss} claim of a compact JWT, or null when the input is not a JWT or carries none.
     * For routing a token to the provider that will verify it, nothing else: the value is attacker-controlled
     * until {@link #validate} has run against that provider's keys.
     */
    public static String peekIssuer(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        try {
            return JWTParser.parse(token).getJWTClaimsSet().getIssuer();
        } catch (java.text.ParseException e) {
            return null;
        }
    }

    /**
     * Reads only the JOSE header and refuses anything that is not a JWS with a permitted asymmetric algorithm,
     * before the key source is consulted. Unsecured tokens and HMAC algorithms never cause a JWKS fetch (T4, T19).
     * The library message for a parse failure can echo the input, so a fixed detail is used instead (T8).
     */
    private void rejectUnpermittedAlgorithm(String token) throws OidcValidationException {
        JWT parsed;
        try {
            parsed = JWTParser.parse(token);
        } catch (java.text.ParseException e) {
            throw new OidcValidationException(Reason.MALFORMED, "not a JWT");
        }
        if (!(parsed instanceof SignedJWT)) {
            throw new OidcValidationException(Reason.ALG, "not a signed JWT");
        }
        String alg = ((SignedJWT) parsed).getHeader().getAlgorithm().getName();
        for (SignatureAlgorithm permitted : settings.algorithms) {
            if (permitted.getName().equals(alg)) {
                return;
            }
        }
        throw new OidcValidationException(Reason.ALG, "algorithm not permitted: " + alg);
    }

    // ---- reason classification -------------------------------------------------------------------

    private static Reason classifyValidation(JwtValidationException e) {
        // The first failing validator decides the reason; the order is timestamp, issuer, audience.
        for (OAuth2Error error : e.getErrors()) {
            String d = error.getDescription() == null ? "" : error.getDescription();
            if (d.contains("expired") || d.contains("exp claim")) {
                return Reason.EXP;
            }
            if (d.contains("used before")) {
                return Reason.NBF;
            }
            if (d.contains("iss claim")) {
                return Reason.ISS;
            }
            if (d.contains(AUD_ERROR_MARKER)) {
                return Reason.AUD;
            }
        }
        return Reason.SIG;
    }

    private static Reason classifyBad(BadJwtException e) {
        String m = e.getMessage() == null ? "" : e.getMessage();
        if (m.contains("Unsecured") || m.contains("plain")) {
            return Reason.ALG;
        }
        if (m.contains("Could not coerce") && m.contains("URI")) {
            // Spring could not turn the iss (or another URL-typed) claim into a URL: not our issuer.
            return Reason.ISS;
        }
        if (m.contains("Unsupported JWS algorithm") || m.contains("Unsupported algorithm")
                || m.contains("algorithm not accepted")) {
            return Reason.ALG;
        }
        if (m.contains("typ") && m.contains("not allowed")) {
            return Reason.MALFORMED;
        }
        if (m.contains("no matching key")) {
            // Nimbus reports an unknown kid and an alg/key mismatch with the same message; both mean the
            // provider published no key that can verify this token.
            return Reason.KID;
        }
        if (m.contains("Invalid signature")) {
            return Reason.SIG;
        }
        if (m.contains("Malformed") || m.contains("Invalid") || m.contains("serialization")) {
            return Reason.MALFORMED;
        }
        return Reason.SIG;
    }

    private static Reason classifyOther(JwtException e) {
        // A JwtException that is not a BadJwtException comes from the key source (fetch, TLS, bad document),
        // not from the token. Fail closed and say so. One special case: a refresh refused by the rate limiter
        // means "unknown key id, and no refetch allowed yet", which is the token's problem, not the IdP's.
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t.getClass().getSimpleName().contains("RateLimit")) {
                return Reason.KID;
            }
        }
        return Reason.JWKS;
    }

    private static String describe(JwtValidationException e) {
        List<String> parts = new ArrayList<>();
        for (OAuth2Error error : e.getErrors()) {
            parts.add(scrub(error.getDescription()));
        }
        return String.join("; ", parts);
    }

    /** Detail text for the audit event. Never contains the token: these messages come from the library
     *  and describe the check, not the input. Kept short and single-line. */
    private static String scrub(String message) {
        if (message == null) {
            return "";
        }
        String m = message.replace('\n', ' ').replace('\r', ' ');
        return m.length() > 200 ? m.substring(0, 200) : m;
    }

    // ---- audience -------------------------------------------------------------------------------

    /**
     * Accepts a token whose {@code aud} contains a configured audience, or whose {@code azp} is one
     * (OIDC Core 1.0 section 2: {@code azp} names the party the token was issued to). A token with neither
     * is rejected (T1).
     */
    static final class AudienceValidator implements OAuth2TokenValidator<Jwt> {
        private final Set<String> accepted;

        AudienceValidator(Set<String> accepted) {
            this.accepted = accepted;
        }

        @Override
        public OAuth2TokenValidatorResult validate(Jwt jwt) {
            List<String> aud = jwt.getAudience();
            if (aud != null) {
                for (String a : aud) {
                    if (accepted.contains(a)) {
                        return OAuth2TokenValidatorResult.success();
                    }
                }
            }
            Object azp = jwt.getClaim("azp");
            if (azp instanceof String && accepted.contains((String) azp)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token",
                    "The " + AUD_ERROR_MARKER + " claim does not name a configured audience", null));
        }
    }

    /** Claim name constants used by callers, so the strings live in one place. */
    public static final String CLAIM_SUB = JwtClaimNames.SUB;
    public static final String CLAIM_ISS = JwtClaimNames.ISS;
}
