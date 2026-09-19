package tak.server.oidc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.bbn.marti.config.Oauth;
import com.bbn.marti.config.OidcGroupAccess;
import com.bbn.marti.config.OidcIdentityKey;
import com.bbn.marti.remote.groups.Direction;
import com.bbn.marti.remote.groups.Group;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import tak.server.oidc.MappingResult.Decision;

/** Registry routing and policy tests for T2/T10 and requirements R-04/R-07/R-08/R-09. */
public class OidcProviderRegistryTest {

    private static final String ISSUER = "https://idp.example/realms/tak";
    private static final String JWKS_URI = "https://idp.example/realms/tak/certs";
    private static RSAKey providerKey;
    private static String jwksJson;

    @BeforeClass
    public static void keys() throws Exception {
        providerKey = new RSAKeyGenerator(2048).keyID("registry-k1").keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256).generate();
        jwksJson = new JWKSet(providerKey.toPublicJWK()).toString();
    }

    static final class JwksStub extends RestTemplate {
        @Override
        @SuppressWarnings("unchecked")
        public <T> ResponseEntity<T> exchange(RequestEntity<?> request, Class<T> responseType) {
            assertEquals(JWKS_URI, request.getUrl().toString());
            return (ResponseEntity<T>) ResponseEntity.ok(jwksJson);
        }
    }

    private static OidcProvider provider() {
        OidcProviderConfig config = new OidcProviderConfig.Builder()
                .name("test-idp")
                .issuer(ISSUER)
                .authorizationEndpoint("https://idp.example/authorize")
                .tokenEndpoint("https://idp.example/token")
                .jwksUri(JWKS_URI)
                .webClientId("tak-web")
                .redirectUri("https://tak.example/login/redirect")
                .build();
        return new OidcProvider(config, new JwksStub());
    }

    private static OidcProviderRegistry registry(Oauth oauth, List<OidcProvider> providers) throws Exception {
        OidcProviderRegistry.Generation generation = OidcProviderRegistry.build(oauth, providers);
        Constructor<OidcProviderRegistry> constructor = OidcProviderRegistry.class
                .getDeclaredConstructor(OidcProviderRegistry.Generation.class);
        constructor.setAccessible(true);
        return constructor.newInstance(generation);
    }

    private static OidcProviderRegistry registry(Oauth oauth) throws Exception {
        return registry(oauth, Collections.emptyList());
    }

    private static OidcIdentity identity(String subject, Map<String, Object> claims) {
        return new OidcIdentity("test-idp", ISSUER, subject, claims);
    }

    private static String tokenWithIssuer(String issuer) throws Exception {
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("registry-k1").build(),
                new JWTClaimsSet.Builder().issuer(issuer).subject("alice").build());
        jwt.sign(new RSASSASigner(providerKey));
        return jwt.serialize();
    }

    private static Oauth.GroupMapping mapping(String claim, String group, OidcGroupAccess access) {
        Oauth.GroupMapping mapping = new Oauth.GroupMapping();
        mapping.setClaimValue(claim);
        mapping.setGroup(group);
        mapping.setAccess(access);
        return mapping;
    }

    private static Set<Group> groups(Group... groups) {
        return new TreeSet<>(Arrays.asList(groups));
    }

    @Test
    public void t2ByIssuerUsesExactMatchAndDoesNotNormalizeTrailingSlash() throws Exception {
        OidcProvider provider = provider();
        OidcProviderRegistry registry = registry(new Oauth(), Collections.singletonList(provider));
        assertSame(provider, registry.byIssuer(ISSUER));
        assertNull(registry.byIssuer(ISSUER + "/"));
    }

    @Test
    public void t2ProviderForUnknownIssuerOrNonJwtReturnsNull() throws Exception {
        OidcProviderRegistry registry = registry(new Oauth(), Collections.singletonList(provider()));
        assertNull(registry.providerFor(tokenWithIssuer("https://unknown.example/realms/tak")));
        assertNull(registry.providerFor("not-a-jwt"));
    }

    @Test
    public void r04IssuerSubjectModeUsesTheStableIdentityKey() throws Exception {
        Oauth oauth = new Oauth();
        oauth.setIdentityKey(OidcIdentityKey.ISSUER_SUBJECT);
        OidcIdentity identity = identity("alice", Collections.singletonMap("preferred_username", "display-name"));
        assertEquals(identity.getIdentityKey(), registry(oauth).takUsername(identity));
    }

    @Test
    public void r04UsernameClaimModeFallsBackToEmailThenSubjectNeverUuid() throws Exception {
        Oauth oauth = new Oauth();
        oauth.setIdentityKey(OidcIdentityKey.USERNAME_CLAIM);
        oauth.setUsernameClaim("preferred_username");
        OidcProviderRegistry registry = registry(oauth);

        Map<String, Object> preferred = new HashMap<>();
        preferred.put("preferred_username", "alice-name");
        preferred.put("email", "alice@example.test");
        assertEquals("alice-name", registry.takUsername(identity("subject-one", preferred)));

        assertEquals("alice@example.test", registry.takUsername(
                identity("subject-two", Collections.singletonMap("email", "alice@example.test"))));
        assertEquals("subject-three", registry.takUsername(identity("subject-three", Collections.emptyMap())));
    }

    @Test
    public void t10AdminComesOnlyFromConfiguredStringOrListClaimNeverGroups() throws Exception {
        Oauth oauth = new Oauth();
        oauth.setAdminTokenClaimName("roles");
        oauth.setAdminTokenClaimValue("tak-admin");
        OidcProviderRegistry registry = registry(oauth);

        assertTrue(registry.hasAdminClaim(identity("one", Collections.singletonMap("roles", "tak-admin"))));
        assertTrue(registry.hasAdminClaim(identity("two",
                Collections.singletonMap("roles", Arrays.asList("user", "tak-admin")))));
        assertFalse(registry.hasAdminClaim(identity("three",
                Collections.singletonMap("groups", Arrays.asList("tak-admin", "TAK_Admin")))));
        assertFalse(registry.hasAdminClaim(identity("four", Collections.singletonMap("roles", "user"))));
    }

    @Test
    public void r08MapperBuildsInOutAndBothRulesFromJaxbConfiguration() throws Exception {
        Oauth oauth = new Oauth();
        oauth.getGroupMapping().add(mapping("writers", "WriteOnly", OidcGroupAccess.IN));
        oauth.getGroupMapping().add(mapping("readers", "ReadOnly", OidcGroupAccess.OUT));
        oauth.getGroupMapping().add(mapping("operators", "Operations", OidcGroupAccess.BOTH));

        MappingResult result = registry(oauth).groupMapper()
                .map(Arrays.asList("writers", "readers", "operators"), false);
        assertEquals(Decision.ALLOW, result.getDecision());
        assertEquals(groups(
                new Group("WriteOnly", Direction.IN),
                new Group("ReadOnly", Direction.OUT),
                new Group("Operations", Direction.IN),
                new Group("Operations", Direction.OUT)), result.getGroups());
    }

    @Test
    public void r09MapperUsesExplicitDefaultGroupWhenRulesMapNothing() throws Exception {
        Oauth oauth = new Oauth();
        oauth.getGroupMapping().add(mapping("operators", "Operations", OidcGroupAccess.BOTH));
        oauth.setDefaultGroup("Guests");

        MappingResult result = registry(oauth).groupMapper().map(Collections.singletonList("unknown"), false);
        assertEquals(Decision.ALLOW, result.getDecision());
        assertEquals(groups(new Group("Guests", Direction.IN), new Group("Guests", Direction.OUT)),
                result.getGroups());
        assertEquals("default-group", result.getReason());
    }

    /** R-09 (review finding 3): no mapping rules and no default group is a denial, not pass-through by name. */
    @Test
    public void r09NoMappingRulesDenyByDefault() throws Exception {
        MappingResult result = registry(new Oauth()).groupMapper()
                .map(new ArrayList<>(Collections.singletonList("Operations")), false);
        assertEquals(Decision.DENY_NO_MAPPING, result.getDecision());
        assertTrue(result.getGroups().isEmpty());
    }

    /** Pass-through by name survives only behind the explicit legacyGroupPassthrough switch. */
    @Test
    public void r01LegacyPassThroughIsOptIn() throws Exception {
        Oauth oauth = new Oauth();
        oauth.setLegacyGroupPassthrough(true);
        MappingResult result = registry(oauth).groupMapper()
                .map(new ArrayList<>(Collections.singletonList("Operations")), false);
        assertEquals(Decision.ALLOW, result.getDecision());
        assertEquals(groups(new Group("Operations", Direction.IN), new Group("Operations", Direction.OUT)),
                result.getGroups());
        assertEquals("legacy", result.getReason());
    }
}
