package tak.server.oidc;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.base.Strings;

import com.bbn.marti.config.Oauth;
import com.bbn.marti.config.OidcGroupAccess;
import com.bbn.marti.config.OidcIdentityKey;
import com.bbn.marti.remote.config.CoreConfigFacade;

/**
 * The set of external OIDC providers TAK Server trusts, built from {@code <auth><oauth>} in CoreConfig, plus
 * the server-wide mapping settings that apply to every one of them.
 *
 * Built lazily on first use and rebuilt after {@code oAuthConfigurationsCacheSeconds} (the same operator
 * setting {@code OAuthApi} already honours), so a discovery document change or a JWKS rotation is picked up
 * without a restart. A provider whose discovery fails is logged and left out: tokens from it are then unknown
 * and rejected. Nothing here falls open.
 *
 * With no {@code <openIdDiscoveryConfiguration>} elements the registry is empty and every caller takes its
 * pre-Phase-1 path unchanged (R-01).
 */
public final class OidcProviderRegistry {

    private static final Logger logger = LoggerFactory.getLogger(OidcProviderRegistry.class);

    /** Outstanding browser authorization requests: 10 minutes to finish a login, bounded (T7). */
    private static final Duration REQUEST_TTL = Duration.ofMinutes(10);
    private static final int REQUEST_MAX = 10_000;

    private static final Object LOCK = new Object();
    private static volatile LoadingCache<String, Generation> cache;
    private static final AuthorizationRequestStore REQUESTS = new AuthorizationRequestStore(REQUEST_TTL, REQUEST_MAX);

    /** One resolved snapshot of the configuration. Immutable. */
    static final class Generation {
        final List<OidcProvider> providers;
        final GroupMapper mapper;
        final String groupsClaim;
        final String usernameClaim;
        final String adminClaimName;
        final String adminClaimValue;
        final OidcIdentityKey identityKey;
        final Oauth oauth;

        Generation(List<OidcProvider> providers, GroupMapper mapper, Oauth oauth) {
            this.providers = Collections.unmodifiableList(providers);
            this.mapper = mapper;
            this.oauth = oauth;
            this.groupsClaim = oauth == null ? "groups" : oauth.getGroupsClaim();
            this.usernameClaim = oauth == null ? null : oauth.getUsernameClaim();
            this.adminClaimName = oauth == null ? null : oauth.getAdminTokenClaimName();
            this.adminClaimValue = oauth == null ? null : oauth.getAdminTokenClaimValue();
            this.identityKey = oauth == null || oauth.getIdentityKey() == null
                    ? OidcIdentityKey.ISSUER_SUBJECT : oauth.getIdentityKey();
        }
    }

    private final Generation gen;

    private OidcProviderRegistry(Generation gen) {
        this.gen = gen;
    }

    /**
     * The current registry. The first call builds it (one discovery fetch per provider). After
     * {@code oAuthConfigurationsCacheSeconds} the next call still returns the current generation and triggers a
     * rebuild in the background (Caffeine refreshAfterWrite), so authentication never waits on discovery once
     * the server is up (review finding 15). A rebuild that fails keeps the previous generation.
     */
    public static OidcProviderRegistry getInstance() {
        LoadingCache<String, Generation> c = cache;
        if (c == null) {
            synchronized (LOCK) {
                if (cache == null) {
                    long seconds = 300;
                    try {
                        seconds = CoreConfigFacade.getInstance().getRemoteConfiguration().getBuffer().getQueue()
                                .getOAuthConfigurationsCacheSeconds();
                    } catch (Exception e) {
                        logger.debug("using default oidc registry cache lifetime", e);
                    }
                    cache = Caffeine.newBuilder().refreshAfterWrite(seconds, TimeUnit.SECONDS).build(key -> build());
                }
                c = cache;
            }
        }
        return new OidcProviderRegistry(c.get("oidc"));
    }

    /** Drops the cached generation so the next call rebuilds. For tests and config reloads. */
    public static void reset() {
        synchronized (LOCK) {
            cache = null;
        }
    }

    /** Package-private: build from an explicit Oauth block, no CoreConfig. Discovery is supplied by the caller. */
    static Generation build(Oauth oauth, List<OidcProvider> providers) {
        return new Generation(providers, mapperFor(oauth), oauth);
    }

    private static Generation build() {
        Oauth oauth = null;
        try {
            oauth = CoreConfigFacade.getInstance().getRemoteConfiguration().getAuth().getOauth();
        } catch (Exception e) {
            logger.debug("no oauth configuration", e);
        }
        List<OidcProvider> providers = new ArrayList<>();
        if (oauth != null && oauth.getOpenIdDiscoveryConfiguration() != null) {
            int skewSeconds = oauth.getExternalAuthClockSkewSeconds();
            Duration jwksTtl = null;
            try {
                jwksTtl = Duration.ofSeconds(CoreConfigFacade.getInstance().getRemoteConfiguration().getBuffer()
                        .getQueue().getOAuthPublicKeyCacheSeconds());
            } catch (Exception e) {
                logger.debug("using the default JWKS cache lifetime", e);
            }
            for (Oauth.OpenIdDiscoveryConfiguration cfg : oauth.getOpenIdDiscoveryConfiguration()) {
                try {
                    // A bootstrap client for discovery: trust settings come from the same config block.
                    OidcProviderConfig bootstrap = bootstrapTrust(cfg);
                    OidcProviderConfig resolved = OidcDiscoveryClient.discover(cfg, OidcHttp.restTemplate(bootstrap), skewSeconds);
                    providers.add(new OidcProvider(resolved, OidcHttp.okHttpClient(resolved), jwksTtl));
                    logger.info("oidc provider '{}' registered: issuer={} audiences={} pkce={} skew={}s", resolved.getName(),
                            resolved.getIssuer(), resolved.getAudiences(), resolved.isUsePkce(), skewSeconds);
                } catch (Exception e) {
                    logger.error("oidc provider '{}' NOT registered: {}", cfg.getName(), e.getMessage());
                }
            }
        }
        return new Generation(providers, mapperFor(oauth), oauth);
    }

    /**
     * Trust for the discovery fetch itself, before the document is known. The URLs are placeholders that only
     * have to satisfy construction; the only fields used are the truststore and trustAllCerts.
     */
    private static OidcProviderConfig bootstrapTrust(Oauth.OpenIdDiscoveryConfiguration cfg) {
        String u = OidcProviderConfig.https(cfg.getName(), "configurationUri", cfg.getConfigurationUri());
        return new OidcProviderConfig.Builder()
                .name(cfg.getName())
                .issuer(u).authorizationEndpoint(u).tokenEndpoint(u).jwksUri(u)
                .webClientId(Strings.isNullOrEmpty(cfg.getClientId()) ? "bootstrap" : cfg.getClientId())
                .redirectUri(Strings.isNullOrEmpty(cfg.getRedirectUri()) ? "https://bootstrap.invalid/" : cfg.getRedirectUri())
                .truststore(cfg.getTruststore(), cfg.getTruststoreFile(), cfg.getTruststorePass())
                .trustAllCerts(cfg.isTrustAllCerts())
                .build();
    }

    static GroupMapper mapperFor(Oauth oauth) {
        List<GroupMappingRule> rules = new ArrayList<>();
        String defaultGroup = null;
        boolean legacyPassthrough = false;
        GroupMapper.LegacySettings legacy = GroupMapper.LegacySettings.defaults();
        if (oauth != null) {
            if (oauth.getGroupMapping() != null) {
                for (Oauth.GroupMapping m : oauth.getGroupMapping()) {
                    rules.add(new GroupMappingRule(m.getClaimValue(), m.getGroup(), access(m.getAccess())));
                }
            }
            defaultGroup = oauth.getDefaultGroup();
            legacyPassthrough = oauth.isLegacyGroupPassthrough();
            legacy = new GroupMapper.LegacySettings(oauth.getGroupprefix(), oauth.getGroupNameExtractorRegex(),
                    oauth.getReadOnlyGroup(), oauth.getReadGroupSuffix(), oauth.getWriteGroupSuffix());
        }
        // Deny by default (R-09): with no groupMapping and no defaultGroup an identity gets nothing. Pass-through
        // by name is opt-in through legacyGroupPassthrough for installations that relied on it before Phase 1
        // (review finding 3: deleting the last mapping must not widen access).
        return new GroupMapper(rules, defaultGroup, legacyPassthrough, legacy);
    }

    private static GroupMappingRule.Access access(OidcGroupAccess a) {
        if (a == null) {
            return GroupMappingRule.Access.BOTH;
        }
        switch (a) {
            case IN:
                return GroupMappingRule.Access.IN;
            case OUT:
                return GroupMappingRule.Access.OUT;
            default:
                return GroupMappingRule.Access.BOTH;
        }
    }

    // ---- lookups ------------------------------------------------------------------------------

    public boolean isEmpty() {
        return gen.providers.isEmpty();
    }

    public List<OidcProvider> providers() {
        return gen.providers;
    }

    /** The provider whose issuer equals {@code iss} exactly, or null. Exact string equality: no normalisation. */
    public OidcProvider byIssuer(String iss) {
        if (iss == null) {
            return null;
        }
        for (OidcProvider p : gen.providers) {
            if (p.getIssuer().equals(iss)) {
                return p;
            }
        }
        return null;
    }

    public OidcProvider byName(String name) {
        if (name == null) {
            return null;
        }
        for (OidcProvider p : gen.providers) {
            if (p.getName().equals(name)) {
                return p;
            }
        }
        return null;
    }

    /** The single configured provider, or the first when several are configured (Phase 1 supports one). */
    public OidcProvider first() {
        return gen.providers.isEmpty() ? null : gen.providers.get(0);
    }

    /**
     * The provider that will verify {@code token}, chosen by its unverified {@code iss}. Null when the token is
     * not a JWT, has no issuer, or names an issuer that is not registered: such a token is not an external
     * OIDC token and takes the pre-existing path.
     */
    public OidcProvider providerFor(String token) {
        return byIssuer(OidcTokenValidator.peekIssuer(token));
    }

    public GroupMapper groupMapper() {
        return gen.mapper;
    }

    public String groupsClaim() {
        return Strings.isNullOrEmpty(gen.groupsClaim) ? "groups" : gen.groupsClaim;
    }

    public OidcIdentityKey identityKeyMode() {
        return gen.identityKey;
    }

    public AuthorizationRequestStore requestStore() {
        return REQUESTS;
    }

    /**
     * The TAK username for an identity. {@code issuerSubject} (default): the identity key. {@code usernameClaim}:
     * the pre-existing resolution, kept for installations that depend on it: the configured claim, then
     * {@code email}, then {@code sub}. Never a random UUID.
     */
    public String takUsername(OidcIdentity identity) {
        if (gen.identityKey == OidcIdentityKey.ISSUER_SUBJECT) {
            return identity.getIdentityKey();
        }
        if (!Strings.isNullOrEmpty(gen.usernameClaim)) {
            String v = identity.getStringClaim(gen.usernameClaim, null);
            if (!Strings.isNullOrEmpty(v)) {
                return v;
            }
        }
        String email = identity.getStringClaim("email", null);
        if (!Strings.isNullOrEmpty(email)) {
            return email;
        }
        return identity.getSubject();
    }

    /** The explicit admin mapping (R-07): only {@code adminTokenClaimName}/{@code adminTokenClaimValue}. */
    public boolean hasAdminClaim(OidcIdentity identity) {
        if (Strings.isNullOrEmpty(gen.adminClaimName) || Strings.isNullOrEmpty(gen.adminClaimValue)) {
            return false;
        }
        Object v = identity.getClaim(gen.adminClaimName);
        if (v instanceof java.util.Collection<?>) {
            for (Object o : (java.util.Collection<?>) v) {
                if (o != null && gen.adminClaimValue.equals(o.toString())) {
                    return true;
                }
            }
            return false;
        }
        return v instanceof String && gen.adminClaimValue.equals(v);
    }

    /** Display name for the UI and audit: the username claim, then preferred_username, then the key. */
    public String displayName(OidcIdentity identity) {
        String claim = Strings.isNullOrEmpty(gen.usernameClaim) ? "preferred_username" : gen.usernameClaim;
        String v = identity.getStringClaim(claim, null);
        if (Strings.isNullOrEmpty(v)) {
            v = identity.getStringClaim("preferred_username", null);
        }
        return Strings.isNullOrEmpty(v) ? identity.getIdentityKey() : v;
    }

    @Override
    public String toString() {
        List<String> names = new ArrayList<>();
        for (OidcProvider p : gen.providers) {
            names.add(p.getName().toLowerCase(Locale.ENGLISH));
        }
        return "OidcProviderRegistry" + names;
    }
}
