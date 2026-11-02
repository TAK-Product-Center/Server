package tak.server.oidc;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.bbn.marti.config.Oauth;
import com.bbn.marti.oauth.AuthCookieUtils;

/** Exact WebTAK scope gate tests for threat model T22. */
public class WebtakScopeGateTest {

    private static Oauth configured() {
        Oauth oauth = new Oauth();
        oauth.setWebtakScope("webtak");
        oauth.setScopeClaim("scope");
        return oauth;
    }

    private static Map<String, Object> scope(Object value) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("scope", value);
        return claims;
    }

    @Test
    public void t22NoConfiguredWebtakScopeLeavesTheLegacyGateOpen() {
        assertTrue(AuthCookieUtils.userHasWebtakAccess(new Oauth(), Collections.emptyMap()));
    }

    @Test
    public void t22ConfiguredScopeWithMissingClaimDenies() {
        assertFalse(AuthCookieUtils.userHasWebtakAccess(configured(), Collections.emptyMap()));
        assertFalse(AuthCookieUtils.userHasWebtakAccess(configured(), (Map<String, Object>) null));
    }

    @Test
    public void t22SpaceSeparatedStringRequiresAnExactScopeToken() {
        assertTrue(AuthCookieUtils.userHasWebtakAccess(configured(), scope("openid webtak")));
        assertFalse(AuthCookieUtils.userHasWebtakAccess(configured(), scope("openid not-webtak profile")));
    }

    @Test
    public void t22ListClaimRequiresAnExactScopeElement() {
        assertTrue(AuthCookieUtils.userHasWebtakAccess(configured(), scope(Arrays.asList("openid", "webtak"))));
        assertFalse(AuthCookieUtils.userHasWebtakAccess(configured(), scope(Arrays.asList("openid", "not-webtak"))));
    }

    @Test
    public void t22CommaSeparatedStringUsesWholeScopeTokens() {
        assertTrue(AuthCookieUtils.userHasWebtakAccess(configured(), scope("openid,webtak,profile")));
        assertFalse(AuthCookieUtils.userHasWebtakAccess(configured(), scope("openid,not-webtak,profile")));
    }
}
