package tak.server.oidc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.function.Consumer;

import org.junit.Test;

/** Threat model T18/T24 and requirements R-02/R-15/R-16. */
public class OidcProviderConfigTest {

    private static OidcProviderConfig.Builder validBuilder() {
        return new OidcProviderConfig.Builder()
                .name("local-idp")
                .issuer("https://idp.example/realms/tak")
                .authorizationEndpoint("https://idp.example/authorize")
                .tokenEndpoint("https://idp.example/token")
                .jwksUri("https://idp.example/jwks")
                .userinfoEndpoint("https://idp.example/userinfo")
                .endSessionEndpoint("https://idp.example/logout")
                .webClientId("tak-web")
                .webClientSecret("web-secret-value")
                .redirectUri("https://tak.example/login/redirect")
                .androidClientId("atak-civ");
    }

    private static void assertHttpRefused(String field, Consumer<OidcProviderConfig.Builder> change) {
        OidcProviderConfig.Builder builder = validBuilder();
        change.accept(builder);
        try {
            builder.build();
            fail("http " + field + " must be refused");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(field));
            assertTrue(e.getMessage(), e.getMessage().contains("T24"));
        }
    }

    @Test
    public void t24HttpIssuerRefusesConstruction() {
        assertHttpRefused("issuer", b -> b.issuer("http://idp.example/realms/tak"));
    }

    @Test
    public void t24HttpAuthorizationEndpointRefusesConstruction() {
        assertHttpRefused("authorization_endpoint", b -> b.authorizationEndpoint("http://idp.example/authorize"));
    }

    @Test
    public void t24HttpTokenEndpointRefusesConstruction() {
        assertHttpRefused("token_endpoint", b -> b.tokenEndpoint("http://idp.example/token"));
    }

    @Test
    public void t24HttpJwksUriRefusesConstruction() {
        assertHttpRefused("jwks_uri", b -> b.jwksUri("http://idp.example/jwks"));
    }

    @Test
    public void t24HttpUserinfoEndpointRefusesConstruction() {
        assertHttpRefused("userinfo_endpoint", b -> b.userinfoEndpoint("http://idp.example/userinfo"));
    }

    @Test
    public void t24HttpEndSessionEndpointRefusesConstruction() {
        assertHttpRefused("end_session_endpoint", b -> b.endSessionEndpoint("http://idp.example/logout"));
    }

    @Test
    public void t18AudienceThatResolvesToNothingRefusesConstruction() {
        try {
            validBuilder().audience(" , , ").build();
            fail("an explicitly empty audience list must be refused");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("audience"));
            assertTrue(e.getMessage(), e.getMessage().contains("T18"));
        }
    }

    @Test
    public void r15AudienceDefaultsToWebAndAndroidClientIds() {
        assertEquals(new LinkedHashSet<>(Arrays.asList("tak-web", "atak-civ")),
                validBuilder().build().getAudiences());
    }

    @Test
    public void r15CommaSeparatedAudienceIsTrimmedAndDeduplicated() {
        assertEquals(new LinkedHashSet<>(Arrays.asList("one", "two", "three")),
                validBuilder().audience(" one, two,one, ,three ").build().getAudiences());
    }

    @Test
    public void r02ScopesMustContainOpenid() {
        try {
            validBuilder().scopes("profile email groups").build();
            fail("openid is required");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("scopes"));
            assertTrue(e.getMessage(), e.getMessage().contains("openid"));
        }
    }

    @Test
    public void t18ClockSkewAcceptsOnlyZeroThroughThreeHundredSeconds() {
        assertEquals(Duration.ZERO, validBuilder().clockSkewSeconds(0).build().getClockSkew());
        assertEquals(Duration.ofSeconds(300), validBuilder().clockSkewSeconds(300).build().getClockSkew());

        for (int seconds : new int[] {-1, 301}) {
            try {
                validBuilder().clockSkewSeconds(seconds).build();
                fail("clock skew " + seconds + " must be refused");
            } catch (IllegalArgumentException e) {
                assertTrue(e.getMessage(), e.getMessage().contains("externalAuthClockSkewSeconds"));
            }
        }
    }

    @Test
    public void t18TruststoreTypeAndFileMustBeSetTogether() {
        for (OidcProviderConfig.Builder builder : Arrays.asList(
                validBuilder().truststore("JKS", null, "trust-secret-value"),
                validBuilder().truststore(null, "/etc/tak/idp.jks", "trust-secret-value"))) {
            try {
                builder.build();
                fail("a half-configured truststore must be refused");
            } catch (IllegalArgumentException e) {
                assertTrue(e.getMessage(), e.getMessage().contains("truststore"));
                assertTrue(e.getMessage(), e.getMessage().contains("truststoreFile"));
            }
        }
    }

    @Test
    public void r16ToStringContainsNoClientOrTruststoreSecret() {
        OidcProviderConfig config = validBuilder()
                .truststore("JKS", "/etc/tak/idp.jks", "trust-secret-value")
                .build();
        assertFalse(config.toString(), config.toString().contains("web-secret-value"));
        assertFalse(config.toString(), config.toString().contains("trust-secret-value"));
    }
}
