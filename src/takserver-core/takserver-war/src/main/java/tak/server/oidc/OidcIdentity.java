package tak.server.oidc;

import java.net.URI;
import java.util.Collections;
import java.util.Locale;
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
     * The stable TAK-side key for this identity: {@code sub} qualified by the whole issuer (R-04). Two IdPs
     * with the same subject, or one user whose email changes, never collide.
     *
     * Form: {@code <sub>@<host>[:<port>][_<path segment>[_<path segment>...]]}. The issuer path is kept because
     * real issuers carry one (IDCommand: {@code https://host/idp}; Keycloak: {@code https://host/realms/x}) and two
     * issuers on one host must not collapse into one identity. An underscore separates host from path: a hostname
     * cannot contain one, so the split is unambiguous. Every character is inside TAK's {@code MartiSafeString}
     * set and legal in a certificate CN, which is where this string ends up on the ATAK path.
     */
    public String getIdentityKey() {
        URI uri = URI.create(issuer.trim());
        StringBuilder key = new StringBuilder(subject).append('@');
        String host = uri.getHost() == null ? uri.getRawAuthority() : uri.getHost();
        key.append(host == null ? "" : host.toLowerCase(Locale.ENGLISH));
        if (uri.getPort() >= 0) {
            key.append(':').append(uri.getPort());
        }
        String path = uri.getPath() == null ? "" : uri.getPath();
        for (String segment : path.split("/")) {
            if (!segment.isEmpty()) {
                key.append('_').append(segment);
            }
        }
        return key.toString();
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
