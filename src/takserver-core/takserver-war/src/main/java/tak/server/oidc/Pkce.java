package tak.server.oidc;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * Proof Key for Code Exchange, RFC 7636, S256 only.
 *
 * Used by the browser flow in {@code OAuthApi} (verifier kept server-side) and, as the reference
 * implementation, by the ATAK client. Plain method is deliberately not implemented.
 */
public final class Pkce {

    /** RFC 7636 section 4.1: 43 to 128 characters from the unreserved set. */
    private static final Pattern VERIFIER = Pattern.compile("^[A-Za-z0-9\\-._~]{43,128}$");

    private static final SecureRandom RANDOM = new SecureRandom();

    private Pkce() {
    }

    /** A 43-character verifier: 32 random bytes, base64url without padding. */
    public static String newVerifier() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** {@code BASE64URL(SHA256(ASCII(verifier)))}, RFC 7636 section 4.2. */
    public static String challengeS256(String verifier) {
        if (verifier == null || !VERIFIER.matcher(verifier).matches()) {
            throw new IllegalArgumentException("code_verifier must be 43-128 unreserved characters");
        }
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] digest = sha256.digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    /** True when the verifier is well formed. */
    public static boolean isValidVerifier(String verifier) {
        return verifier != null && VERIFIER.matcher(verifier).matches();
    }
}
