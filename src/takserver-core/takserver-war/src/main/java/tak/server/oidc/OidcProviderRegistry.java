package tak.server.oidc;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.CacheLoader;
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
 * without a restart.
 *
 * Two views, kept apart on purpose (review v02 H1/F-01):
 * <ul>
 * <li><b>configured</b>: an {@code <openIdDiscoveryConfiguration enabled="true">} exists in CoreConfig. Derived
 * from configuration alone, never from the network. Callers that decide <em>whether</em> a credential is an OIDC
 * credential, or whether the legacy login flow may run, branch on this view.</li>
 * <li><b>live</b>: the providers whose discovery succeeded, or whose last good resolution was retained across a
 * failed refresh. Callers that <em>verify</em> a credential use this view.</li>
 * </ul>
 * A provider that is configured but not live is "unavailable": everything that names it is refused. Nothing here
 * falls open, and an unavailable provider is never silently equivalent to an absent one.
 *
 * With no {@code <openIdDiscoveryConfiguration enabled="true">} the registry is not configured and every caller
 * takes its pre-Phase-1 path unchanged (R-01).
 *
 * One {@code OidcProviderRegistry} instance wraps exactly one generation. Read {@link #getInstance()} once per
 * decision and use that instance for every lookup in the decision (review v02 F-10).
 */
public final class OidcProviderRegistry {

    private static final Logger logger = LoggerFactory.getLogger(OidcProviderRegistry.class);

    /** Outstanding browser authorization requests: 10 minutes to finish a login, bounded (T7). */
    private static final Duration REQUEST_TTL = Duration.ofMinutes(10);
    private static final int REQUEST_MAX = 10_000;

    /** While a configured provider is unavailable, discovery is retried in the background this often. */
    public static final long RETRY_SECONDS = 60;

    private static final Object LOCK = new Object();
    private static volatile LoadingCache<String, Generation> cache;
    private static volatile long nextRetryAtMillis;
    private static final AuthorizationRequestStore REQUESTS = new AuthorizationRequestStore(REQUEST_TTL, REQUEST_MAX);

    /** How one enabled configuration element becomes a live provider. Production does discovery; tests stub it. */
    interface Discovery {
        OidcProvider discover(Oauth.OpenIdDiscoveryConfiguration cfg) throws Exception;
    }

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
        /** Names of the enabled elements (the configured view). */
        final Set<String> configuredNames;
        /** Pinned {@code issuer} attributes of the enabled elements plus the issuers of the live providers. */
        final Set<String> configuredIssuers;
        /** Enabled elements with no live provider in this generation. */
        final List<String> unavailable;

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
            Set<String> names = new LinkedHashSet<>();
            Set<String> issuers = new LinkedHashSet<>();
            List<String> missing = new ArrayList<>();
            for (Oauth.OpenIdDiscoveryConfiguration cfg : phase1Entries(oauth)) {
                names.add(cfg.getName());
                if (!Strings.isNullOrEmpty(cfg.getIssuer())) {
                    issuers.add(cfg.getIssuer().trim());
                }
                if (find(cfg.getName()) == null) {
                    missing.add(cfg.getName());
                }
            }
            for (OidcProvider p : this.providers) {
                issuers.add(p.getIssuer());
            }
            this.configuredNames = Collections.unmodifiableSet(names);
            this.configuredIssuers = Collections.unmodifiableSet(issuers);
            this.unavailable = Collections.unmodifiableList(missing);
        }

        OidcProvider find(String name) {
            for (OidcProvider p : providers) {
                if (p.getName().equals(name)) {
                    return p;
                }
            }
            return null;
        }

        boolean configured() {
            return !configuredNames.isEmpty() || !providers.isEmpty();
        }

        boolean degraded() {
            return !unavailable.isEmpty();
        }
    }

    private final Generation gen;

    OidcProviderRegistry(Generation gen) {
        this.gen = gen;
    }

    /**
     * The current registry. The first call builds it (one discovery fetch per provider). After
     * {@code oAuthConfigurationsCacheSeconds} the next call still returns the current generation and triggers a
     * rebuild in the background (Caffeine refreshAfterWrite), so authentication never waits on discovery once
     * the server is up (review finding 15).
     *
     * Availability semantics (review v02 H1/F-01): a refresh that cannot resolve an enabled provider keeps that
     * provider's last good resolution; a refresh that can build nothing throws inside the loader so Caffeine keeps
     * the previous generation; a cold start that can build nothing publishes a generation that is configured but
     * has no live provider, and discovery is retried in the background every {@link #RETRY_SECONDS} until it
     * succeeds. The four-period expiry still applies, so a replacement configuration that never resolves cannot
     * keep old grants alive indefinitely (review round two, 19, 22).
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
                    cache = newCache(seconds);
                }
                c = cache;
            }
        }
        Generation g = c.get("oidc");
        if (g.degraded()) {
            long now = System.currentTimeMillis();
            if (now >= nextRetryAtMillis) {
                nextRetryAtMillis = now + RETRY_SECONDS * 1000;
                // Asynchronous: the caller keeps this generation; a successful rebuild replaces it.
                c.refresh("oidc");
            }
        }
        return new OidcProviderRegistry(g);
    }

    private static LoadingCache<String, Generation> newCache(long seconds) {
        // Refresh no more often than once a minute (a zero or tiny value must not disable the registry or
        // hammer discovery); a generation that cannot be refreshed dies after four periods.
        long refresh = Math.max(60, seconds);
        return Caffeine.newBuilder()
                .refreshAfterWrite(refresh, TimeUnit.SECONDS)
                .expireAfterWrite(refresh * 4, TimeUnit.SECONDS)
                .build(new CacheLoader<String, Generation>() {
                    @Override
                    public Generation load(String key) {
                        return build(null);
                    }

                    @Override
                    public Generation reload(String key, Generation previous) {
                        return build(previous);
                    }
                });
    }

    /** Drops the cached generation so the next call rebuilds. For tests and config reloads. */
    public static void reset() {
        synchronized (LOCK) {
            cache = null;
            nextRetryAtMillis = 0;
        }
    }

    /** Package-private, tests only: publish {@code gen} as the current generation without discovery. */
    static void install(Generation gen) {
        synchronized (LOCK) {
            LoadingCache<String, Generation> c = newCache(RETRY_SECONDS);
            c.put("oidc", gen);
            cache = c;
            nextRetryAtMillis = Long.MAX_VALUE;
        }
    }

    /** Package-private: build from an explicit Oauth block, no CoreConfig. Discovery is supplied by the caller. */
    static Generation build(Oauth oauth, List<OidcProvider> providers) {
        return new Generation(providers, safeMapper(oauth), oauth);
    }

    private static Generation build(Generation previous) {
        Oauth oauth = null;
        try {
            oauth = CoreConfigFacade.getInstance().getRemoteConfiguration().getAuth().getOauth();
        } catch (Exception e) {
            logger.debug("no oauth configuration", e);
        }
        int skewSeconds = oauth == null ? 60 : oauth.getExternalAuthClockSkewSeconds();
        Duration jwksTtl = null;
        try {
            // Nimbus needs the lifetime to exceed refresh-ahead plus fetch timeout (40 s); clamp to a minute.
            long ttl = CoreConfigFacade.getInstance().getRemoteConfiguration().getBuffer()
                    .getQueue().getOAuthPublicKeyCacheSeconds();
            jwksTtl = Duration.ofSeconds(Math.max(60, ttl));
        } catch (Exception e) {
            logger.debug("using the default JWKS cache lifetime", e);
        }
        return build(oauth, previous, productionDiscovery(skewSeconds, jwksTtl));
    }

    /** Production discovery: bootstrap trust from the element, fetch the document, build the provider. */
    static Discovery productionDiscovery(int skewSeconds, Duration jwksTtl) {
        return cfg -> {
            // A bootstrap client for discovery: trust settings come from the same config block.
            OidcProviderConfig bootstrap = bootstrapTrust(cfg);
            OidcProviderConfig resolved = OidcDiscoveryClient.discover(cfg, OidcHttp.restTemplate(bootstrap), skewSeconds);
            OidcProvider provider = new OidcProvider(resolved, OidcHttp.okHttpClient(resolved), jwksTtl);
            logger.info("oidc provider '{}' registered: issuer={} audiences={} pkce={} skew={}s", resolved.getName(),
                    resolved.getIssuer(), resolved.getAudiences(), resolved.isUsePkce(), skewSeconds);
            return provider;
        };
    }

    /**
     * Package-private: one generation from an Oauth block, the previous generation (null on cold start) and a
     * discovery function. Per enabled element: a successful discovery is the new provider; a failed discovery
     * keeps the previous generation's provider of the same name when there is one, otherwise the element is
     * unavailable. Each failure is one ERROR line. When nothing could be built and there is a previous generation,
     * this throws so the Caffeine loader keeps that generation (review v02 H1/F-01).
     *
     * A malformed {@code <groupMapping>} does not abort the build: the generation is published with a deny-all
     * mapper so the error is confined to OIDC identities instead of every authenticator (review v02 F-03).
     */
    static Generation build(Oauth oauth, Generation previous, Discovery discovery) {
        GroupMapper mapper = safeMapper(oauth);
        List<OidcProvider> providers = new ArrayList<>();
        List<Oauth.OpenIdDiscoveryConfiguration> enabled = phase1Entries(oauth);
        for (Oauth.OpenIdDiscoveryConfiguration cfg : enabled) {
            try {
                if (Strings.isNullOrEmpty(cfg.getIssuer()) || cfg.getIssuer().trim().isEmpty()) {
                    // Without a pinned issuer a certificate enrolled with this provider cannot be recognised while
                    // the provider is unreachable, and the certificate path could not fail closed (review v02 H1).
                    throw new IllegalArgumentException("issuer is required on an enabled=\"true\" provider");
                }
                refuseUsernameClaimOnEnabled(oauth);
                providers.add(discovery.discover(cfg));
            } catch (Exception e) {
                OidcProvider last = previous == null ? null : previous.find(cfg.getName());
                if (last != null) {
                    providers.add(last);
                    logger.error("oidc provider '{}' refresh failed: {}; keeping the previous resolution (issuer={})",
                            cfg.getName(), e.getMessage(), last.getIssuer());
                } else {
                    logger.error("oidc provider '{}' NOT registered, unavailable until discovery succeeds: {}",
                            cfg.getName(), e.getMessage());
                }
            }
        }
        if (!enabled.isEmpty() && providers.isEmpty() && previous != null) {
            throw new IllegalStateException("no enabled oidc provider could be resolved; keeping the previous generation");
        }
        return new Generation(providers, mapper, oauth);
    }

    /**
     * {@code identityKey="usernameClaim"} on {@code <oauth>} is refused for every {@code enabled="true"} provider
     * (review v02 M2, Astra rank 5): R-04 requires the TAK user to be keyed on iss plus sub, and a display claim
     * would let one IdP account impersonate another. Elements without {@code enabled="true"} never reach this
     * check, so legacy installations keep the mode.
     */
    static void refuseUsernameClaimOnEnabled(Oauth oauth) {
        if (oauth != null && oauth.getIdentityKey() == OidcIdentityKey.USERNAME_CLAIM) {
            throw new IllegalArgumentException("identityKey=\"usernameClaim\" on <oauth> is refused on an enabled=\"true\""
                    + " provider (M2): R-04 requires the TAK user to be keyed on iss plus sub; set"
                    + " identityKey=\"issuerSubject\" or remove the attribute");
        }
    }

    /** The mapper for {@code oauth}, or a deny-all mapper when the configuration is malformed (review v02 F-03). */
    static GroupMapper safeMapper(Oauth oauth) {
        try {
            return mapperFor(oauth);
        } catch (IllegalArgumentException e) {
            logger.error("oidc <groupMapping> is invalid ({}): every OIDC identity is denied until it is fixed", e.getMessage());
            return new GroupMapper(Collections.emptyList(), null, false, GroupMapper.LegacySettings.defaults());
        }
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

    /**
     * The discovery elements Phase 1 owns: those with {@code enabled="true"} (R-01). Every other element keeps its
     * pre-Phase-1 behaviour in every caller (registry, JwtUtils, OAuthApi), so an unchanged configuration is
     * regression-equivalent to upstream.
     */
    static List<Oauth.OpenIdDiscoveryConfiguration> phase1Entries(Oauth oauth) {
        List<Oauth.OpenIdDiscoveryConfiguration> out = new ArrayList<>();
        if (oauth == null || oauth.getOpenIdDiscoveryConfiguration() == null) {
            return out;
        }
        for (Oauth.OpenIdDiscoveryConfiguration cfg : oauth.getOpenIdDiscoveryConfiguration()) {
            if (cfg.isEnabled()) {
                out.add(cfg);
            } else {
                logger.info("oidc provider '{}' has no enabled=\"true\": pre-existing behaviour applies", cfg.getName());
            }
        }
        return out;
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

    // ---- configured view (from CoreConfig, independent of discovery) ----------------------------

    /** True when an {@code enabled="true"} discovery element exists, whether or not it resolved. */
    public boolean isConfigured() {
        return gen.configured();
    }

    /** Configured but nothing live: every OIDC credential is refused and the legacy flows must not run. */
    public boolean isUnavailable() {
        return gen.configured() && gen.providers.isEmpty();
    }

    /** Names of the enabled elements that have no live provider in this generation. */
    public List<String> unavailableProviders() {
        return gen.unavailable;
    }

    /** The pinned issuers of the enabled elements plus the issuers of the live providers. Exact strings. */
    public Set<String> configuredIssuers() {
        return gen.configuredIssuers;
    }

    /** Whether {@code iss} names a configured provider, live or not. Exact string equality. */
    public boolean isConfiguredIssuer(String iss) {
        return iss != null && gen.configuredIssuers.contains(iss);
    }

    // ---- live view -----------------------------------------------------------------------------

    /** No live provider. Not the same as {@link #isConfigured()} being false; see the class comment. */
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
