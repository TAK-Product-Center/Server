package tak.server.oidc;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

    /** Prefix of every OIDC identity key; reserves the namespace so no file, LDAP or cert user can collide. */
    public static final String KEY_PREFIX = "oidc-";

    /**
     * The stable TAK-side key for this identity (R-04): {@code oidc-} followed by 40 lower-case hex characters of
     * SHA-256 over the exact issuer and exact subject, length-delimited.
     *
     * Why a digest and not {@code sub@issuer}: the issuer is a URL with a path (IDCommand: {@code /idp}; Keycloak:
     * {@code /realms/x}) and the subject is free text, and this key becomes a TAK username, a certificate CN (64
     * characters at most), a group-cache key that TAK lower-cases, and an audit field. A readable encoding either
     * collides (two issuers whose paths differ only in the separator; two subjects differing only in case) or
     * leaks characters TAK's validators refuse. The digest is exact, bounded (45 characters), case-stable and
     * inside {@code MartiSafeString}. The audit line carries issuer and subject in clear beside it, and the
     * display name (preferred_username) is what people see.
     */
    public String getIdentityKey() {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] issuerBytes = issuer.getBytes(StandardCharsets.UTF_8);
            byte[] subjectBytes = subject.getBytes(StandardCharsets.UTF_8);
            sha256.update(Integer.toString(issuerBytes.length).getBytes(StandardCharsets.US_ASCII));
            sha256.update((byte) ':');
            sha256.update(issuerBytes);
            sha256.update((byte) ':');
            sha256.update(subjectBytes);
            byte[] digest = sha256.digest();
            StringBuilder hex = new StringBuilder(KEY_PREFIX);
            for (int i = 0; i < 20; i++) {
                hex.append(Character.forDigit((digest[i] >> 4) & 0xF, 16)).append(Character.forDigit(digest[i] & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
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
