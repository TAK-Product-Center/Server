package tak.server.oidc;

import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * One external OIDC provider, fully resolved: what the operator configured under
 * {@code <openIdDiscoveryConfiguration>} merged with what the provider's discovery document said.
 *
 * Immutable. Construction refuses the states the threat model names as operator misconfiguration (T18, T24):
 * a non-https endpoint, an empty audience list, a discovered issuer that differs from the configured one. A
 * provider that fails construction is never registered, so the failure is loud at startup and fails closed.
 */
public final class OidcProviderConfig {

    private final String name;
    private final String issuer;
    private final String authorizationEndpoint;
    private final String tokenEndpoint;
    private final String jwksUri;
    private final String userinfoEndpoint;     // nullable
    private final String endSessionEndpoint;   // nullable
    private final String webClientId;
    private final String webClientSecret;      // nullable: a public web client is not supported, but a secret
                                               // may be absent when only the ATAK path is in use
    private final String redirectUri;
    private final String androidClientId;      // nullable
    private final Set<String> audiences;
    private final String scopes;
    private final boolean usePkce;
    private final Duration clockSkew;
    private final String truststoreType;       // nullable
    private final String truststoreFile;       // nullable
    private final String truststorePassword;   // nullable
    private final boolean trustAllCerts;
    private final String accessTokenName;
    private final String refreshTokenName;

    public static final class Builder {
        private String name;
        private String issuer;
        private String authorizationEndpoint;
        private String tokenEndpoint;
        private String jwksUri;
        private String userinfoEndpoint;
        private String endSessionEndpoint;
        private String webClientId;
        private String webClientSecret;
        private String redirectUri;
        private String androidClientId;
        private String audience;
        private String scopes = "openid profile";
        private boolean usePkce = true;
        private int clockSkewSeconds = 60;
        private String truststoreType;
        private String truststoreFile;
        private String truststorePassword;
        private boolean trustAllCerts;
        private String accessTokenName = "access_token";
        private String refreshTokenName = "refresh_token";

        public Builder name(String v) { this.name = v; return this; }
        public Builder issuer(String v) { this.issuer = v; return this; }
        public Builder authorizationEndpoint(String v) { this.authorizationEndpoint = v; return this; }
        public Builder tokenEndpoint(String v) { this.tokenEndpoint = v; return this; }
        public Builder jwksUri(String v) { this.jwksUri = v; return this; }
        public Builder userinfoEndpoint(String v) { this.userinfoEndpoint = v; return this; }
        public Builder endSessionEndpoint(String v) { this.endSessionEndpoint = v; return this; }
        public Builder webClientId(String v) { this.webClientId = v; return this; }
        public Builder webClientSecret(String v) { this.webClientSecret = v; return this; }
        public Builder redirectUri(String v) { this.redirectUri = v; return this; }
        public Builder androidClientId(String v) { this.androidClientId = v; return this; }
        /** Comma-separated accepted aud/azp values; null or empty means clientId plus androidClientId. */
        public Builder audience(String v) { this.audience = v; return this; }
        public Builder scopes(String v) { if (v != null && !v.trim().isEmpty()) { this.scopes = v.trim(); } return this; }
        public Builder usePkce(boolean v) { this.usePkce = v; return this; }
        public Builder clockSkewSeconds(int v) { this.clockSkewSeconds = v; return this; }
        public Builder truststore(String type, String file, String password) {
            this.truststoreType = type; this.truststoreFile = file; this.truststorePassword = password; return this;
        }
        public Builder trustAllCerts(boolean v) { this.trustAllCerts = v; return this; }
        public Builder accessTokenName(String v) { if (v != null && !v.isEmpty()) { this.accessTokenName = v; } return this; }
        public Builder refreshTokenName(String v) { if (v != null && !v.isEmpty()) { this.refreshTokenName = v; } return this; }

        public OidcProviderConfig build() {
            return new OidcProviderConfig(this);
        }
    }

    private OidcProviderConfig(Builder b) {
        this.name = required("name", b.name);
        this.issuer = https(this.name, "issuer", b.issuer);
        this.authorizationEndpoint = https(this.name, "authorization_endpoint", b.authorizationEndpoint);
        this.tokenEndpoint = https(this.name, "token_endpoint", b.tokenEndpoint);
        this.jwksUri = https(this.name, "jwks_uri", b.jwksUri);
        this.userinfoEndpoint = optionalHttps(this.name, "userinfo_endpoint", b.userinfoEndpoint);
        this.endSessionEndpoint = optionalHttps(this.name, "end_session_endpoint", b.endSessionEndpoint);
        this.webClientId = required("clientId", b.webClientId);
        this.webClientSecret = blankToNull(b.webClientSecret);
        this.redirectUri = required("redirectUri", b.redirectUri);
        this.androidClientId = blankToNull(b.androidClientId);

        Set<String> auds = new LinkedHashSet<>();
        if (b.audience != null && !b.audience.trim().isEmpty()) {
            for (String a : b.audience.split(",")) {
                if (!a.trim().isEmpty()) {
                    auds.add(a.trim());
                }
            }
        } else {
            auds.add(this.webClientId);
            if (this.androidClientId != null) {
                auds.add(this.androidClientId);
            }
        }
        if (auds.isEmpty()) {
            throw new IllegalArgumentException("oidc provider '" + this.name + "': audience resolves to nothing (T18)");
        }
        this.audiences = Collections.unmodifiableSet(auds);
        this.scopes = b.scopes;
        if (!this.scopes.contains("openid")) {
            throw new IllegalArgumentException("oidc provider '" + this.name + "': scopes must include openid");
        }
        this.usePkce = b.usePkce;
        if (b.clockSkewSeconds < 0 || b.clockSkewSeconds > 300) {
            throw new IllegalArgumentException("oidc provider '" + this.name
                    + "': externalAuthClockSkewSeconds must be between 0 and 300");
        }
        this.clockSkew = Duration.ofSeconds(b.clockSkewSeconds);
        this.truststoreType = blankToNull(b.truststoreType);
        this.truststoreFile = blankToNull(b.truststoreFile);
        this.truststorePassword = b.truststorePassword;
        if ((this.truststoreFile == null) != (this.truststoreType == null)) {
            throw new IllegalArgumentException("oidc provider '" + this.name
                    + "': truststore and truststoreFile must be set together");
        }
        this.trustAllCerts = b.trustAllCerts;
        this.accessTokenName = b.accessTokenName;
        this.refreshTokenName = b.refreshTokenName;
    }

    private static String required(String field, String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("oidc provider: " + field + " is required");
        }
        return value.trim();
    }

    private static String blankToNull(String v) {
        return (v == null || v.trim().isEmpty()) ? null : v.trim();
    }

    private static String optionalHttps(String name, String field, String value) {
        return blankToNull(value) == null ? null : https(name, field, value);
    }

    /** Every IdP URL must be https (T24). A plaintext endpoint is a startup error, not a warning. */
    static String https(String name, String field, String value) {
        String v = required(field, value);
        URI uri;
        try {
            uri = URI.create(v);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("oidc provider '" + name + "': " + field + " is not a valid URI", e);
        }
        if (uri.getScheme() == null || !"https".equals(uri.getScheme().toLowerCase(Locale.ENGLISH))
                || uri.getHost() == null) {
            throw new IllegalArgumentException("oidc provider '" + name + "': " + field + " must be an https URL (T24): " + v);
        }
        return v;
    }

    public String getName() { return name; }
    public String getIssuer() { return issuer; }
    public String getAuthorizationEndpoint() { return authorizationEndpoint; }
    public String getTokenEndpoint() { return tokenEndpoint; }
    public String getJwksUri() { return jwksUri; }
    public String getUserinfoEndpoint() { return userinfoEndpoint; }
    public String getEndSessionEndpoint() { return endSessionEndpoint; }
    public String getWebClientId() { return webClientId; }
    public String getWebClientSecret() { return webClientSecret; }
    public String getRedirectUri() { return redirectUri; }
    public String getAndroidClientId() { return androidClientId; }
    public Set<String> getAudiences() { return audiences; }
    public String getScopes() { return scopes; }
    public boolean isUsePkce() { return usePkce; }
    public Duration getClockSkew() { return clockSkew; }
    public String getTruststoreType() { return truststoreType; }
    public String getTruststoreFile() { return truststoreFile; }
    public String getTruststorePassword() { return truststorePassword; }
    public boolean isTrustAllCerts() { return trustAllCerts; }
    public String getAccessTokenName() { return accessTokenName; }
    public String getRefreshTokenName() { return refreshTokenName; }

    /** The validator settings this provider implies. */
    public OidcTokenValidator.Settings validatorSettings() {
        return new OidcTokenValidator.Settings(name, issuer, jwksUri, audiences, clockSkew, null);
    }

    @Override
    public String toString() {
        // No secret, no truststore password.
        return "OidcProviderConfig[" + name + " issuer=" + issuer + " aud=" + audiences + " pkce=" + usePkce + "]";
    }
}
