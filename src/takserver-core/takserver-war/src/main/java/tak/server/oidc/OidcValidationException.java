package tak.server.oidc;

/**
 * A token was rejected. Carries a stable reason code for the audit event and never the token itself.
 */
public final class OidcValidationException extends Exception {

    private static final long serialVersionUID = 1L;

    public enum Reason {
        /** Issuer claim missing or not the configured issuer. */
        ISS,
        /** Neither aud nor azp names a configured audience. */
        AUD,
        /** Token expired (with the configured clock skew). */
        EXP,
        /** Token not yet valid. */
        NBF,
        /** Signature did not verify against the provider's published keys. */
        SIG,
        /** Algorithm not permitted: none, an HMAC algorithm, or not the algorithm the key is for. */
        ALG,
        /** No published key matches the token's key id, even after one refetch. */
        KID,
        /** Not a JWT, or not a JWS. */
        MALFORMED,
        /** The provider's JWKS could not be fetched (network, TLS, or a bad document). */
        JWKS,
        /** A required identity claim (sub) is missing. */
        CLAIMS,
        /** The ID token's nonce is missing or is not the one issued with the authorization request (T6). */
        NONCE,
        /** The browser callback's state is unknown, expired, or already consumed (T7). */
        STATE,
        /** The code exchange with the token endpoint failed or returned no usable ID token. */
        EXCHANGE
    }

    private final Reason reason;

    public OidcValidationException(Reason reason, String detail) {
        super(reason + (detail == null || detail.isEmpty() ? "" : ": " + detail));
        this.reason = reason;
    }

    public OidcValidationException(Reason reason, String detail, Throwable cause) {
        super(reason + (detail == null || detail.isEmpty() ? "" : ": " + detail), cause);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
