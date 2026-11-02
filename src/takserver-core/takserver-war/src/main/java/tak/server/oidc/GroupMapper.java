package tak.server.oidc;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import com.bbn.marti.remote.groups.Direction;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.util.GroupNameExtractor;

/**
 * Maps the value of an OIDC groups claim to TAK groups.
 *
 * Pure: no Spring, no CoreConfig, no GroupManager. Returns unhydrated {@link Group}s (name and direction);
 * the caller hydrates them. Two modes:
 *
 * <ul>
 *   <li><b>Mapping mode</b> (one or more {@link GroupMappingRule}s configured): a claim value maps only
 *       through an exact-match rule. Nothing else maps. This is the deny-by-default behaviour the Phase 1
 *       requirements ask for (R-08, R-09).</li>
 *   <li><b>Legacy pass-through</b> (no rules, and the operator has kept the pre-existing behaviour): a
 *       claim value is treated as a TAK group name after the prefix filter, the optional regex extraction,
 *       the readOnlyGroup check and the _READ/_WRITE suffix handling. This reproduces what
 *       {@code OAuthAuthenticator} did before Phase 1 so an unchanged upstream configuration behaves
 *       identically.</li>
 * </ul>
 *
 * When neither mode yields a group, {@code defaultGroup} applies if configured, otherwise the result is
 * DENY. There is no anonymous fallback and no directory lookup here, by design.
 *
 * Admin is never derived from a group mapping; the caller decides it from the explicit admin claim and
 * passes it through so the result is one object.
 */
public final class GroupMapper {

    /** Legacy pass-through settings, mirroring the existing {@code <oauth>} attributes. */
    public static final class LegacySettings {
        private final String groupPrefix;
        private final String groupNameExtractorRegex;
        private final String readOnlyGroup;
        private final String readSuffix;
        private final String writeSuffix;

        public LegacySettings(String groupPrefix, String groupNameExtractorRegex, String readOnlyGroup,
                String readSuffix, String writeSuffix) {
            this.groupPrefix = groupPrefix == null ? "" : groupPrefix.toLowerCase(Locale.ENGLISH);
            this.groupNameExtractorRegex = groupNameExtractorRegex;
            this.readOnlyGroup = readOnlyGroup;
            this.readSuffix = readSuffix == null ? "_READ" : readSuffix;
            this.writeSuffix = writeSuffix == null ? "_WRITE" : writeSuffix;
        }

        public static LegacySettings defaults() {
            return new LegacySettings("", null, null, "_READ", "_WRITE");
        }
    }

    private final List<GroupMappingRule> rules;
    private final String defaultGroup;
    private final boolean legacyPassthrough;
    private final LegacySettings legacy;

    /**
     * @param rules             configured mappings; empty means no mapping mode
     * @param defaultGroup      TAK group granted when nothing else maps; null or empty means deny
     * @param legacyPassthrough true only when the operator has not adopted mapping and the pre-existing
     *                          name pass-through must be preserved; ignored when {@code rules} is non-empty
     * @param legacy            pass-through settings; may be null when {@code legacyPassthrough} is false
     */
    public GroupMapper(List<GroupMappingRule> rules, String defaultGroup, boolean legacyPassthrough,
            LegacySettings legacy) {
        this.rules = rules == null ? Collections.emptyList() : List.copyOf(rules);
        this.defaultGroup = (defaultGroup == null || defaultGroup.isEmpty()) ? null : defaultGroup;
        this.legacyPassthrough = this.rules.isEmpty() && legacyPassthrough;
        this.legacy = legacy == null ? LegacySettings.defaults() : legacy;
    }

    /**
     * @param groupsClaim the raw claim value: a JSON array (any {@link Collection}), a single string
     *                    (whitespace or comma separated), or null when absent
     * @param adminClaim  whether the explicit admin claim matched; passed through, never computed here
     */
    public MappingResult map(Object groupsClaim, boolean adminClaim) {
        Set<String> claimValues = normalise(groupsClaim);

        Set<Group> groups = new TreeSet<>();
        String reason;

        if (!rules.isEmpty()) {
            reason = "mapping";
            for (String value : claimValues) {
                for (GroupMappingRule rule : rules) {
                    if (rule.getClaimValue().equals(value)) {
                        if (rule.grants(Direction.IN)) {
                            groups.add(new Group(rule.getGroup(), Direction.IN));
                        }
                        if (rule.grants(Direction.OUT)) {
                            groups.add(new Group(rule.getGroup(), Direction.OUT));
                        }
                    }
                }
            }
        } else if (legacyPassthrough) {
            reason = "legacy";
            groups.addAll(legacyGroups(claimValues));
        } else {
            reason = "no-rules";
        }

        if (groups.isEmpty()) {
            if (defaultGroup == null) {
                return MappingResult.denyNoMapping();
            }
            groups.add(new Group(defaultGroup, Direction.IN));
            groups.add(new Group(defaultGroup, Direction.OUT));
            reason = "default-group";
        }

        return MappingResult.allow(groups, adminClaim, reason);
    }

    /**
     * Accepts the forms the requirements name: an array claim or a string claim. Order is preserved so
     * that the audit event lists values as the IdP sent them; duplicates collapse.
     */
    static Set<String> normalise(Object groupsClaim) {
        Set<String> out = new LinkedHashSet<>();
        if (groupsClaim == null) {
            return out;
        }
        if (groupsClaim instanceof Collection<?>) {
            for (Object o : (Collection<?>) groupsClaim) {
                if (o != null) {
                    String s = o.toString().trim();
                    if (!s.isEmpty()) {
                        out.add(s);
                    }
                }
            }
            return out;
        }
        if (groupsClaim instanceof String) {
            for (String s : ((String) groupsClaim).split("[\\s,]+")) {
                String t = s.trim();
                if (!t.isEmpty()) {
                    out.add(t);
                }
            }
            return out;
        }
        // Any other JSON type (number, boolean, object) is not a groups claim. Treat as absent.
        return out;
    }

    /**
     * The pre-Phase-1 behaviour of {@code OAuthAuthenticator}: prefix filter, optional regex extraction,
     * readOnlyGroup, then _READ/_WRITE suffix handling as in {@code LdapAuthenticator.groupNamesToGroups}.
     */
    private Set<Group> legacyGroups(Set<String> claimValues) {
        Set<String> names = new TreeSet<>();
        for (String name : claimValues) {
            if (name.toLowerCase(Locale.ENGLISH).startsWith(legacy.groupPrefix)) {
                names.add(name);
            }
        }

        if (legacy.groupNameExtractorRegex != null && !legacy.groupNameExtractorRegex.isEmpty()) {
            GroupNameExtractor extractor = new GroupNameExtractor(legacy.groupNameExtractorRegex);
            Set<String> extracted = new TreeSet<>();
            for (String name : names) {
                extracted.add(extractor.extractGroupName(name));
            }
            names = extracted;
        }

        boolean readOnly = false;
        if (legacy.readOnlyGroup != null && !legacy.readOnlyGroup.isEmpty() && names.remove(legacy.readOnlyGroup)) {
            readOnly = true;
        }

        Set<Group> groups = new TreeSet<>();
        for (String name : names) {
            boolean grantRead = true;
            boolean grantWrite = true;
            if (name.endsWith(legacy.readSuffix)) {
                grantWrite = false;
                name = name.substring(0, name.indexOf(legacy.readSuffix));
            } else if (name.endsWith(legacy.writeSuffix)) {
                grantRead = false;
                name = name.substring(0, name.indexOf(legacy.writeSuffix));
            }
            if (name.isEmpty()) {
                continue;
            }
            if (grantWrite && !readOnly) {
                groups.add(new Group(name, Direction.IN));
            }
            if (grantRead) {
                groups.add(new Group(name, Direction.OUT));
            }
        }
        return groups;
    }
}
