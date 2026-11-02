package tak.server.oidc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

import com.bbn.marti.config.Oauth;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** Provider-neutral discovery tests for T2/T24 and requirements R-03/R-05. */
public class OidcDiscoveryClientTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String KEYCLOAK = "{"
            + "\"issuer\":\"https://kc.local:8443/realms/tak\","
            + "\"authorization_endpoint\":\"https://kc.local:8443/realms/tak/protocol/openid-connect/auth\","
            + "\"token_endpoint\":\"https://kc.local:8443/realms/tak/protocol/openid-connect/token\","
            + "\"jwks_uri\":\"https://kc.local:8443/realms/tak/protocol/openid-connect/certs\","
            + "\"code_challenge_methods_supported\":[\"plain\",\"S256\"]"
            + "}";

    private static Oauth.OpenIdDiscoveryConfiguration config() {
        Oauth.OpenIdDiscoveryConfiguration cfg = new Oauth.OpenIdDiscoveryConfiguration();
        cfg.setName("fixture-idp");
        cfg.setClientId("tak-web");
        cfg.setSecret("client-secret-value");
        cfg.setRedirectUri("https://tak.local/login/redirect");
        cfg.setAndroidClientId("atak-civ");
        cfg.setScopes("openid profile groups");
        cfg.setUsePkce(Boolean.TRUE);
        return cfg;
    }

    private static String idCommandDocument() throws Exception {
        Path cursor = Paths.get("").toAbsolutePath();
        while (cursor != null) {
            Path candidate = cursor.resolve("requests/2026-09-18-tak-oidc-sso-phase1/inputs/"
                    + "idcommand-docs/openid-configuration.json");
            if (Files.isRegularFile(candidate)) {
                return Files.readString(candidate, StandardCharsets.UTF_8);
            }
            cursor = cursor.getParent();
        }
        throw new AssertionError("real IDCommand discovery fixture was not found from the Gradle working directory");
    }

    private static void assertRefused(String expectedField, String document) {
        try {
            OidcDiscoveryClient.parse(config(), document);
            fail("discovery document should refuse " + expectedField);
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(expectedField));
        }
    }

    @Test
    public void r03RealIdCommandDiscoveryDocumentParses() throws Exception {
        OidcProviderConfig parsed = OidcDiscoveryClient.parse(config(), idCommandDocument());
        assertEquals("https://lastwall.idcommand.lastwall.com/idp", parsed.getIssuer());
        assertEquals("https://lastwall.idcommand.lastwall.com/idp/oidc/authorize",
                parsed.getAuthorizationEndpoint());
        assertEquals("https://lastwall.idcommand.lastwall.com/idp/oidc/token", parsed.getTokenEndpoint());
        assertEquals("https://lastwall.idcommand.lastwall.com/idp/jwks.json", parsed.getJwksUri());
        assertEquals("https://lastwall.idcommand.lastwall.com/idp/oidc/userinfo", parsed.getUserinfoEndpoint());
        assertEquals("https://lastwall.idcommand.lastwall.com/idp/oidc/logout", parsed.getEndSessionEndpoint());
    }

    @Test
    public void r03KeycloakShapedDiscoveryDocumentParsesWithoutOptionalEndpoints() {
        OidcProviderConfig parsed = OidcDiscoveryClient.parse(config(), KEYCLOAK);
        assertEquals("https://kc.local:8443/realms/tak", parsed.getIssuer());
        assertNull(parsed.getUserinfoEndpoint());
        assertNull(parsed.getEndSessionEndpoint());
        assertTrue(OidcDiscoveryClient.advertisesPkceS256(KEYCLOAK));
    }

    @Test
    public void r03MissingRequiredDiscoveryFieldsRefuseAndNameEachField() throws Exception {
        for (String field : new String[] {"issuer", "authorization_endpoint", "token_endpoint", "jwks_uri"}) {
            ObjectNode document = (ObjectNode) JSON.readTree(KEYCLOAK);
            document.remove(field);
            assertRefused(field, document.toString());
        }
    }

    @Test
    public void t2ConfiguredIssuerMismatchRefusesDiscovery() {
        Oauth.OpenIdDiscoveryConfiguration cfg = config();
        cfg.setIssuer("https://other-idp.local/realms/tak");
        try {
            OidcDiscoveryClient.parse(cfg, KEYCLOAK);
            fail("configured issuer mismatch must be refused");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("issuer"));
            assertTrue(e.getMessage(), e.getMessage().contains("T2"));
        }
    }

    @Test
    public void r03NonJsonDiscoveryDocumentRefuses() {
        assertRefused("not JSON", "this is not json");
    }

    @Test
    public void t24HttpDiscoveryEndpointsAllRefuse() throws Exception {
        for (String field : new String[] {"issuer", "authorization_endpoint", "token_endpoint", "jwks_uri",
                "userinfo_endpoint", "end_session_endpoint"}) {
            ObjectNode document = (ObjectNode) JSON.readTree(KEYCLOAK);
            document.put(field, "http://idp.local/" + field);
            assertRefused(field, document.toString());
        }
    }

    @Test
    public void r02PkceAdvertisementDistinguishesTrueFalseAndAbsent() {
        assertTrue(OidcDiscoveryClient.advertisesPkceS256(KEYCLOAK));
        assertFalse(OidcDiscoveryClient.advertisesPkceS256(
                "{\"code_challenge_methods_supported\":[\"plain\"]}"));
        assertFalse(OidcDiscoveryClient.advertisesPkceS256("{}"));
    }

    /** Mutation run M4 survived the field-name check alone; the exact discovery message is pinned here. */
    @Test
    public void r03MissingJwksUriIsRefusedByDiscoveryItself() throws Exception {
        String doc = "{\"issuer\":\"https://idp.test/realms/tak\",\"authorization_endpoint\":\"https://idp.test/a\","
                + "\"token_endpoint\":\"https://idp.test/t\"}";
        Oauth.OpenIdDiscoveryConfiguration cfg = new Oauth.OpenIdDiscoveryConfiguration();
        cfg.setName("fixture-idp");
        cfg.setClientId("tak-web");
        cfg.setRedirectUri("https://tak.test/login/redirect");
        cfg.setConfigurationUri("https://idp.test/realms/tak/.well-known/openid-configuration");
        try {
            OidcDiscoveryClient.parse(cfg, doc);
            org.junit.Assert.fail("missing jwks_uri must be refused");
        } catch (IllegalArgumentException e) {
            assertEquals("oidc provider 'fixture-idp': discovery document lacks jwks_uri", e.getMessage());
        }
    }
}
