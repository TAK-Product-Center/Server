package tak.server.oidc;

import java.util.Date;
import java.util.function.Predicate;

/**
 * The two enrollment behaviour changes of Phase 1 apply to OIDC-issued tokens only (review v02 F-05). Legacy TAK
 * tokens and legacy authServer tokens keep upstream behaviour. Pure, so it is unit-testable without Ignite.
 */
public final class OidcTokenGate {

    private OidcTokenGate() {}

    /** True when the registry recognises {@code token} as issued by a live Phase 1 provider. */
    public static boolean isOidcToken(String token, Predicate<String> recognisedByRegistry) {
        return token != null && !token.isEmpty() && recognisedByRegistry.test(token);
    }

    /** The token a new certificate record keeps: the caller's token when it is OIDC-issued, otherwise null. */
    public static String tokenToStore(String token, Predicate<String> recognisedByRegistry) {
        return isOidcToken(token, recognisedByRegistry) ? token : null;
    }

    /**
     * Certificate notAfter under useTokenExpiration. A readable exp always wins. An unreadable exp refuses signing
     * for an OIDC token (the operator asked for the certificate to die with the assertion) and keeps the upstream
     * default lifetime for a legacy token.
     */
    public static Date notAfter(Long expSeconds, boolean oidcToken, Date defaultNotAfter) {
        if (expSeconds != null) {
            return new Date(expSeconds * 1000);
        }
        if (oidcToken) {
            throw new IllegalStateException("useTokenExpiration is set and the assertion's exp cannot be read");
        }
        return defaultNotAfter;
    }
}
