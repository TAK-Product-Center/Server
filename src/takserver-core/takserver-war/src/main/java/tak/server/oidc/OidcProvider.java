package tak.server.oidc;

import org.springframework.web.client.RestOperations;

/**
 * A registered external OIDC provider: its resolved configuration, the HTTP client that trusts it, and the
 * validator bound to its issuer and key set. One instance per provider per registry generation.
 */
public final class OidcProvider {

    private final OidcProviderConfig config;
    private final RestOperations rest;
    private final OidcTokenValidator validator;

    OidcProvider(OidcProviderConfig config, RestOperations rest) {
        this.config = config;
        this.rest = rest;
        this.validator = new OidcTokenValidator(config.validatorSettings(), rest);
    }

    public OidcProviderConfig getConfig() {
        return config;
    }

    public String getName() {
        return config.getName();
    }

    public String getIssuer() {
        return config.getIssuer();
    }

    /** The HTTP client for this provider's token endpoint: same trust as discovery and JWKS. */
    public RestOperations getRest() {
        return rest;
    }

    public OidcTokenValidator getValidator() {
        return validator;
    }

    /** @see OidcTokenValidator#validate(String, boolean) */
    public OidcIdentity validate(String token, boolean ignoreTime) throws OidcValidationException {
        return validator.validate(token, ignoreTime);
    }
}
