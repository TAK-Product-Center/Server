package tak.server.oidc;

import java.net.URI;
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
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.SignedJWT;

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
    }

    /** The only algorithms ever accepted. No HMAC family, so a public key can never be used as a secret. */
    static final Set<SignatureAlgorithm> ASYMMETRIC = Collections.unmodifiableSet(EnumSet.of(
            SignatureAlgorithm.RS256, SignatureAlgorithm.RS384, SignatureAlgorithm.RS512,
            SignatureAlgorithm.PS256, SignatureAlgorithm.PS384, SignatureAlgorithm.PS512,
            SignatureAlgorithm.ES256, SignatureAlgorithm.ES384, SignatureAlgorithm.ES512));

    private static final String AUD_ERROR_MARKER = "aud/azp";

    private final Settings settings;
    private final JwtDecoder decoder;

    /** Production constructor: JWKS fetched with a default RestTemplate (caller supplies a truststore-aware one). */
    public OidcTokenValidator(Settings settings) {
        this(settings, new RestTemplate());
    }

    /**
     * @param rest the HTTP client used to fetch the JWKS. Production passes one whose SSL context trusts the
     *             tactical CA; tests pass a stub that serves a canned JWKS.
     */
    public OidcTokenValidator(Settings settings, RestOperations rest) {
        this.settings = settings;
        NimbusJwtDecoder nimbus = NimbusJwtDecoder.withJwkSetUri(settings.jwksUri)
                .jwsAlgorithms(algs -> {
                    algs.clear();
                    algs.addAll(settings.algorithms);
                })
                .restOperations(rest)
                .build();
        nimbus.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(settings.clockSkew),
                new JwtIssuerValidator(settings.issuer),
                new AudienceValidator(settings.audiences)));
        this.decoder = nimbus;
    }

    public Settings getSettings() {
        return settings;
    }

    /**
     * @return the identity the token proves
     * @throws OidcValidationException with a stable reason when the token is rejected for any cause
     */
    public OidcIdentity validate(String token) throws OidcValidationException {
        if (token == null || token.isEmpty()) {
            throw new OidcValidationException(Reason.MALFORMED, "empty token");
        }
        rejectUnpermittedAlgorithm(token);
        Jwt jwt;
        try {
            jwt = decoder.decode(token);
        } catch (JwtValidationException e) {
            throw new OidcValidationException(classifyValidation(e), describe(e), e);
        } catch (BadJwtException e) {
            throw new OidcValidationException(classifyBad(e), scrub(e.getMessage()), e);
        } catch (JwtException e) {
            throw new OidcValidationException(classifyOther(e), scrub(e.getMessage()), e);
        }

        String sub = jwt.getSubject();
        if (sub == null || sub.isEmpty()) {
            throw new OidcValidationException(Reason.CLAIMS, "sub missing");
        }
        Map<String, Object> claims = new HashMap<>(jwt.getClaims());
        return new OidcIdentity(settings.providerName, settings.issuer, sub, claims);
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
            if (d.contains("expired")) {
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
        if (m.contains("Unsupported JWS algorithm") || m.contains("Unsupported algorithm")
                || m.contains("algorithm not accepted")) {
            return Reason.ALG;
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
        // not from the token. Fail closed and say so.
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
