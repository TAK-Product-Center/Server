package tak.server.oidc;

import org.springframework.web.client.RestOperations;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import okhttp3.OkHttpClient;

/**
 * A registered external OIDC provider: its resolved configuration, the HTTP client that trusts it, and the
 * validator bound to its issuer and key set. One instance per provider per registry generation.
 */
public final class OidcProvider {

    private final OidcProviderConfig config;
    private final RestOperations rest;
    private final OkHttpClient http;
    private final OidcTokenValidator validator;

    /** Production: one OkHttp client built from the provider's trust settings, used for JWKS and the token call. */
    OidcProvider(OidcProviderConfig config, OkHttpClient http, java.time.Duration jwksCacheTtl) {
        this.config = config;
        this.http = http;
        this.rest = new RestTemplate(new OkHttp3ClientHttpRequestFactory(http));
        this.validator = new OidcTokenValidator(new OidcTokenValidator.Settings(config.getName(), config.getIssuer(),
                config.getJwksUri(), config.getAudiences(), config.getClockSkew(), null, jwksCacheTtl), rest);
    }

    /** Tests: a stubbed RestOperations for the JWKS; no token endpoint client. */
    OidcProvider(OidcProviderConfig config, RestOperations rest) {
        this.config = config;
        this.http = null;
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

    /** The Spring client for JWKS and discovery (same trust as the token endpoint). */
    public RestOperations getRest() {
        return rest;
    }

    /** The OkHttp client for the token endpoint (null only in tests). Same trust as JWKS and discovery. */
    public OkHttpClient getHttp() {
        return http;
    }

    public OidcTokenValidator getValidator() {
        return validator;
    }

    /** @see OidcTokenValidator#validate(String, boolean) */
    public OidcIdentity validate(String token, boolean ignoreTime) throws OidcValidationException {
        return validator.validate(token, ignoreTime);
    }
}
