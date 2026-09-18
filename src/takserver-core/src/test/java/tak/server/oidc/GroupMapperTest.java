package tak.server.oidc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;

import com.bbn.marti.remote.groups.Direction;
import com.bbn.marti.remote.groups.Group;

import tak.server.oidc.GroupMappingRule.Access;
import tak.server.oidc.MappingResult.Decision;

/**
 * Pure unit tests for {@link GroupMapper}. No Spring context, no CoreConfig.
 *
 * Threat model rows exercised: T9 (deny when nothing maps), T10 (admin never from a mapping).
 */
public class GroupMapperTest {

    private static Group in(String name) {
        return new Group(name, Direction.IN);
    }

    private static Group out(String name) {
        return new Group(name, Direction.OUT);
    }

    private static Set<Group> groups(Group... gs) {
        return new TreeSet<>(Arrays.asList(gs));
    }

    private static final List<GroupMappingRule> RULES = Arrays.asList(
            new GroupMappingRule("tak_ops", "Operations", Access.BOTH),
            new GroupMappingRule("tak_ops", "Common", Access.OUT),
            new GroupMappingRule("tak_admin", "TAK_Admin", Access.BOTH),
            new GroupMappingRule("tak_readers", "Intel", Access.OUT),
            new GroupMappingRule("tak_writers", "Intel", Access.IN));

    private static GroupMapper mappingMode() {
        return new GroupMapper(RULES, null, false, null);
    }

    // ---- mapping mode ---------------------------------------------------------------------------

    @Test
    public void mappingModeMapsExactValuesToAllTheirGroups() {
        MappingResult r = mappingMode().map(Arrays.asList("tak_ops"), false);
        assertEquals(Decision.ALLOW, r.getDecision());
        assertEquals(groups(in("Operations"), out("Operations"), out("Common")), r.getGroups());
        assertEquals("mapping", r.getReason());
    }

    @Test
    public void mappingModeHonoursAccessDirection() {
        assertEquals(groups(out("Intel")), mappingMode().map(Arrays.asList("tak_readers"), false).getGroups());
        assertEquals(groups(in("Intel")), mappingMode().map(Arrays.asList("tak_writers"), false).getGroups());
        assertEquals(groups(in("Intel"), out("Intel")),
                mappingMode().map(Arrays.asList("tak_readers", "tak_writers"), false).getGroups());
    }

    @Test
    public void mappingModeIgnoresUnmappedValuesAndIsCaseSensitive() {
        MappingResult r = mappingMode().map(Arrays.asList("TAK_OPS", "Operations", "something-else"), false);
        assertEquals(Decision.DENY_NO_MAPPING, r.getDecision());
        assertTrue(r.getGroups().isEmpty());
    }

    @Test
    public void mappingModeNeverPassesAClaimValueThroughAsAGroupName() {
        // "Operations" is a real TAK group name but not a configured claim value: it must not map (T9).
        MappingResult r = mappingMode().map(Arrays.asList("Operations"), false);
        assertEquals(Decision.DENY_NO_MAPPING, r.getDecision());
    }

    @Test
    public void emptyOrAbsentClaimDeniesWithoutDefaultGroup() {
        assertEquals(Decision.DENY_NO_MAPPING, mappingMode().map(null, false).getDecision());
        assertEquals(Decision.DENY_NO_MAPPING, mappingMode().map(Collections.emptyList(), false).getDecision());
        assertEquals(Decision.DENY_NO_MAPPING, mappingMode().map("", false).getDecision());
        assertEquals("no-mapping", mappingMode().map(null, false).getReason());
    }

    @Test
    public void defaultGroupAppliesOnlyWhenNothingMaps() {
        GroupMapper m = new GroupMapper(RULES, "Guests", false, null);
        MappingResult none = m.map(Arrays.asList("unknown"), false);
        assertEquals(Decision.ALLOW, none.getDecision());
        assertEquals(groups(in("Guests"), out("Guests")), none.getGroups());
        assertEquals("default-group", none.getReason());

        MappingResult some = m.map(Arrays.asList("tak_admin"), false);
        assertEquals(groups(in("TAK_Admin"), out("TAK_Admin")), some.getGroups());
        assertEquals("mapping", some.getReason());
    }

    @Test
    public void adminIsPassedThroughAndNeverDerivedFromRules() {
        // The tak_admin rule maps to a group named TAK_Admin; that must not set the admin flag (T10).
        assertFalse(mappingMode().map(Arrays.asList("tak_admin"), false).isAdmin());
        assertTrue(mappingMode().map(Arrays.asList("tak_ops"), true).isAdmin());
        // A denied identity is never admin, whatever the claim said.
        assertFalse(mappingMode().map(Arrays.asList("nothing"), true).isAdmin());
    }

    @Test
    public void stringClaimIsSplitOnWhitespaceOrCommas() {
        Set<Group> expected = groups(in("Operations"), out("Operations"), out("Common"), in("TAK_Admin"), out("TAK_Admin"));
        assertEquals(expected, mappingMode().map("tak_ops tak_admin", false).getGroups());
        assertEquals(expected, mappingMode().map("tak_ops,tak_admin", false).getGroups());
        assertEquals(expected, mappingMode().map("  tak_ops ,\n tak_admin  ", false).getGroups());
    }

    @Test
    public void unsupportedClaimTypesAreTreatedAsAbsent() {
        assertEquals(Decision.DENY_NO_MAPPING, mappingMode().map(Integer.valueOf(42), false).getDecision());
        assertEquals(Decision.DENY_NO_MAPPING, mappingMode().map(Boolean.TRUE, false).getDecision());
    }

    @Test
    public void nullEntriesAndBlanksInsideAnArrayAreIgnored() {
        List<Object> claim = new ArrayList<>();
        claim.add(null);
        claim.add("  ");
        claim.add("tak_admin");
        assertEquals(groups(in("TAK_Admin"), out("TAK_Admin")), mappingMode().map(claim, false).getGroups());
    }

    @Test
    public void mappedGroupsAreAlwaysASubsetOfConfiguredTargets() {
        // Property: whatever the IdP sends, mapping mode can only ever produce configured target groups.
        Set<Group> targets = new HashSet<>();
        for (GroupMappingRule rule : RULES) {
            targets.add(in(rule.getGroup()));
            targets.add(out(rule.getGroup()));
        }
        String[] vocabulary = {"tak_ops", "tak_admin", "tak_readers", "tak_writers", "Operations", "TAK_Admin",
                "ANON", "", "tak_ops_READ", "x", "Common_WRITE"};
        Random random = new Random(20260918L);
        for (int i = 0; i < 2000; i++) {
            List<String> claim = new ArrayList<>();
            int n = random.nextInt(6);
            for (int j = 0; j < n; j++) {
                claim.add(vocabulary[random.nextInt(vocabulary.length)]);
            }
            MappingResult r = mappingMode().map(claim, false);
            assertTrue("result groups must be configured targets: " + r.getGroups(), targets.containsAll(r.getGroups()));
            assertEquals(r.getGroups().isEmpty(), r.getDecision() == Decision.DENY_NO_MAPPING);
        }
    }

    // ---- strict mode without rules ---------------------------------------------------------------

    @Test
    public void noRulesAndNoLegacyMeansDenyUnlessDefault() {
        GroupMapper strict = new GroupMapper(Collections.emptyList(), null, false, null);
        assertEquals(Decision.DENY_NO_MAPPING, strict.map(Arrays.asList("Operations"), false).getDecision());

        GroupMapper withDefault = new GroupMapper(Collections.emptyList(), "Guests", false, null);
        MappingResult r = withDefault.map(Arrays.asList("Operations"), false);
        assertEquals(groups(in("Guests"), out("Guests")), r.getGroups());
    }

    // ---- legacy pass-through ---------------------------------------------------------------------

    private static GroupMapper legacy(String prefix, String regex, String readOnly) {
        return new GroupMapper(Collections.emptyList(), null, true,
                new GroupMapper.LegacySettings(prefix, regex, readOnly, "_READ", "_WRITE"));
    }

    @Test
    public void legacyPassthroughUsesClaimValuesAsGroupNames() {
        MappingResult r = legacy("", null, null).map(Arrays.asList("Operations", "Intel"), false);
        assertEquals(groups(in("Operations"), out("Operations"), in("Intel"), out("Intel")), r.getGroups());
        assertEquals("legacy", r.getReason());
    }

    @Test
    public void legacyPassthroughAppliesReadWriteSuffixes() {
        MappingResult r = legacy("", null, null).map(Arrays.asList("Intel_READ", "Ops_WRITE"), false);
        assertEquals(groups(out("Intel"), in("Ops")), r.getGroups());
    }

    @Test
    public void legacyPassthroughAppliesPrefixFilterCaseInsensitively() {
        MappingResult r = legacy("TAK_", null, null).map(Arrays.asList("tak_Ops", "Other"), false);
        assertEquals(groups(in("tak_Ops"), out("tak_Ops")), r.getGroups());
    }

    @Test
    public void legacyPassthroughExtractsWithRegex() {
        MappingResult r = legacy("", "CN=(.*?)(?:,|$)", null)
                .map(Arrays.asList("CN=Operations,OU=TAK,DC=example"), false);
        assertEquals(groups(in("Operations"), out("Operations")), r.getGroups());
    }

    @Test
    public void legacyReadOnlyGroupStripsWriteAccessFromEverything() {
        MappingResult r = legacy("", null, "readonly").map(Arrays.asList("readonly", "Operations"), false);
        assertEquals(groups(out("Operations")), r.getGroups());
    }

    @Test
    public void legacyPassthroughIsIgnoredWhenRulesExist() {
        GroupMapper m = new GroupMapper(RULES, null, true, GroupMapper.LegacySettings.defaults());
        assertEquals(Decision.DENY_NO_MAPPING, m.map(Arrays.asList("Operations"), false).getDecision());
    }

    @Test
    public void legacyEmptyClaimStillDenies() {
        // The old code fell through to anonymous assignment here. Phase 1 denies (T9).
        assertEquals(Decision.DENY_NO_MAPPING, legacy("", null, null).map(Collections.emptyList(), false).getDecision());
    }

    // ---- rule construction -----------------------------------------------------------------------

    @Test(expected = IllegalArgumentException.class)
    public void ruleRejectsEmptyClaimValue() {
        new GroupMappingRule("", "Operations", Access.BOTH);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ruleRejectsEmptyGroup() {
        new GroupMappingRule("tak_ops", "", Access.BOTH);
    }
}
