package tak.server.oidc;

import java.io.IOException;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestOperations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.bbn.marti.config.Oauth;

/**
 * Resolves an {@code <openIdDiscoveryConfiguration>} into an {@link OidcProviderConfig} by fetching the
 * provider's discovery document (OpenID Connect Discovery 1.0, section 4).
 *
 * Replaces the discovery half of {@code OAuthUtils.processTrustedAuthServerConfig}, which required the optional
 * {@code scopes_supported}, discarded the discovered issuer, and converted every JWKS key as RSA so an EC key
 * aborted discovery. Here: only the four required members are required; the issuer is kept and, when the
 * operator pinned one, must match (T2); keys are Nimbus's problem, fetched lazily by the validator.
 *
 * Pure apart from the one HTTP GET: {@link #parse} is a static function of the document text so it can be
 * tested with planted-bad documents.
 */
public final class OidcDiscoveryClient {

    private static final ObjectMapper JSON = new ObjectMapper();

    private OidcDiscoveryClient() {
    }

    /**
     * @param cfg  the operator's configuration
     * @param rest the HTTP client to use (truststore-aware in production)
     * @throws IllegalArgumentException when the document or the configuration is unusable; the message names the
     *                                  field and never contains a secret
     */
    public static OidcProviderConfig discover(Oauth.OpenIdDiscoveryConfiguration cfg, RestOperations rest) {
        return discover(cfg, rest, 60);
    }

    /** @param clockSkewSeconds the server-wide externalAuthClockSkewSeconds, applied to every token from this provider */
    public static OidcProviderConfig discover(Oauth.OpenIdDiscoveryConfiguration cfg, RestOperations rest, int clockSkewSeconds) {
        String uri = OidcProviderConfig.https(cfg.getName(), "configurationUri", cfg.getConfigurationUri());
        ResponseEntity<String> response;
        try {
            response = rest.exchange(uri, HttpMethod.GET, null, String.class);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("oidc provider '" + cfg.getName() + "': discovery fetch failed: "
                    + e.getClass().getSimpleName(), e);
        }
        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new IllegalArgumentException("oidc provider '" + cfg.getName() + "': discovery returned HTTP "
                    + response.getStatusCode().value());
        }
        return parse(cfg, response.getBody(), clockSkewSeconds);
    }

    /** Builds the provider from configuration plus a discovery document. Static so tests can plant defects. */
    public static OidcProviderConfig parse(Oauth.OpenIdDiscoveryConfiguration cfg, String discoveryJson) {
        return parse(cfg, discoveryJson, 60);
    }

    public static OidcProviderConfig parse(Oauth.OpenIdDiscoveryConfiguration cfg, String discoveryJson, int clockSkewSeconds) {
        JsonNode doc;
        try {
            doc = JSON.readTree(discoveryJson);
        } catch (IOException e) {
            throw new IllegalArgumentException("oidc provider '" + cfg.getName() + "': discovery document is not JSON", e);
        }
        if (doc == null || !doc.isObject()) {
            throw new IllegalArgumentException("oidc provider '" + cfg.getName() + "': discovery document is not a JSON object");
        }

        String discoveredIssuer = requiredText(cfg.getName(), doc, "issuer");
        String configuredIssuer = cfg.getIssuer() == null ? null : cfg.getIssuer().trim();
        if (configuredIssuer != null && !configuredIssuer.isEmpty() && !configuredIssuer.equals(discoveredIssuer)) {
            throw new IllegalArgumentException("oidc provider '" + cfg.getName() + "': configured issuer '"
                    + configuredIssuer + "' does not match the discovery document's issuer '" + discoveredIssuer + "' (T2)");
        }

        return new OidcProviderConfig.Builder()
                .name(cfg.getName())
                .issuer(discoveredIssuer)
                .authorizationEndpoint(requiredText(cfg.getName(), doc, "authorization_endpoint"))
                .tokenEndpoint(requiredText(cfg.getName(), doc, "token_endpoint"))
                .jwksUri(requiredText(cfg.getName(), doc, "jwks_uri"))
                .userinfoEndpoint(optionalText(doc, "userinfo_endpoint"))
                .endSessionEndpoint(optionalText(doc, "end_session_endpoint"))
                .webClientId(cfg.getClientId())
                .webClientSecret(cfg.getSecret())
                .redirectUri(cfg.getRedirectUri())
                .androidClientId(cfg.getAndroidClientId())
                .audience(cfg.getAudience())
                .scopes(cfg.getScopes())
                .androidRedirect(cfg.getAndroidRedirect())
                .usePkce(cfg.isUsePkce())
                .clockSkewSeconds(clockSkewSeconds)
                .truststore(cfg.getTruststore(), cfg.getTruststoreFile(), cfg.getTruststorePass())
                .trustAllCerts(cfg.isTrustAllCerts())
                .accessTokenName(cfg.getAccessTokenName())
                .refreshTokenName(cfg.getRefreshTokenName())
                .build();
    }

    /** True when the discovery document advertises S256 PKCE. Informational: TAK sends PKCE regardless. */
    public static boolean advertisesPkceS256(String discoveryJson) {
        try {
            JsonNode doc = JSON.readTree(discoveryJson);
            JsonNode methods = doc == null ? null : doc.get("code_challenge_methods_supported");
            if (methods != null && methods.isArray()) {
                for (JsonNode m : methods) {
                    if ("S256".equals(m.asText())) {
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            // not JSON: not advertised
        }
        return false;
    }

    private static String requiredText(String name, JsonNode doc, String field) {
        JsonNode n = doc.get(field);
        if (n == null || !n.isTextual() || n.asText().trim().isEmpty()) {
            throw new IllegalArgumentException("oidc provider '" + name + "': discovery document lacks " + field);
        }
        return n.asText().trim();
    }

    private static String optionalText(JsonNode doc, String field) {
        JsonNode n = doc.get(field);
        return (n == null || !n.isTextual() || n.asText().trim().isEmpty()) ? null : n.asText().trim();
    }
}
