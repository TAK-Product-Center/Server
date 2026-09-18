package tak.server.oidc;

import java.util.Collections;
import java.util.Map;

/**
 * A verified OIDC identity: what the token proved, and nothing the token did not.
 *
 * The identity key is {@code issuer + subject} (R-04). Display claims are carried for the UI and the audit
 * event and are never used to select a TAK user. The raw token is deliberately not retained here.
 */
public final class OidcIdentity {

    private final String providerName;
    private final String issuer;
    private final String subject;
    private final Map<String, Object> claims;

    public OidcIdentity(String providerName, String issuer, String subject, Map<String, Object> claims) {
        if (issuer == null || issuer.isEmpty()) {
            throw new IllegalArgumentException("issuer required");
        }
        if (subject == null || subject.isEmpty()) {
            throw new IllegalArgumentException("subject required");
        }
        this.providerName = providerName;
        this.issuer = issuer;
        this.subject = subject;
        this.claims = claims == null ? Collections.emptyMap() : Collections.unmodifiableMap(claims);
    }

    public String getProviderName() {
        return providerName;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getSubject() {
        return subject;
    }

    /**
     * The stable TAK-side key for this identity: {@code sub} qualified by the issuer host. Two IdPs
     * with the same subject, or one user whose email changes, never collide. Safe for use as a TAK
     * username: no whitespace, no characters outside the issuer host and the subject.
     */
    public String getIdentityKey() {
        String host = issuer.replaceFirst("^[a-zA-Z][a-zA-Z0-9+.-]*://", "");
        int slash = host.indexOf('/');
        if (slash >= 0) {
            host = host.substring(0, slash);
        }
        return subject + "@" + host;
    }

    /** A claim by name, or null. Callers must treat values as untrusted display data. */
    public Object getClaim(String name) {
        return claims.get(name);
    }

    /** A string claim by name, or the fallback when absent or not a string. */
    public String getStringClaim(String name, String fallback) {
        Object v = claims.get(name);
        return v instanceof String ? (String) v : fallback;
    }

    public Map<String, Object> getClaims() {
        return claims;
    }

    @Override
    public String toString() {
        // No claims in toString: it ends up in logs.
        return "OidcIdentity[" + getIdentityKey() + "]";
    }
}
