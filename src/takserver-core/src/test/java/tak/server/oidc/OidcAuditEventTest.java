package tak.server.oidc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;

import com.bbn.marti.remote.groups.Direction;
import com.bbn.marti.remote.groups.Group;

import tak.server.oidc.OidcAuditEvent.Surface;
import tak.server.oidc.OidcValidationException.Reason;

/** Threat model T20 and requirements R-16/R-17. */
public class OidcAuditEventTest {

    @Test
    public void t20FixedSchemaSanitisesEveryFreeTextFieldAndSortsGroups() {
        Collection<Group> groups = Arrays.asList(
                new Group("Zulu Group", Direction.OUT),
                new Group("Alpha=Group", Direction.IN));

        assertEquals(
                "outcome=ALLOW surface=BEARER provider=pro_vider_one"
                        + " iss=https://idp/_realm__x_ sub=sub__x_ user=a_b_c port=8443"
                        + " groups=[Alpha_Group:IN,Zulu_Group:OUT] admin=true reason=bad__reason",
                OidcAuditEvent.format("ALLOW", Surface.BEARER, "pro vider=one",
                        "https://idp/[realm],x\n", "sub [x]", "a=b,c", 8443, groups, true,
                        "bad\n reason"));
    }

    @Test
    public void t20NullFreeTextFieldsPrintDash() {
        assertEquals(
                "outcome=DENY surface=UNKNOWN provider=- iss=- sub=- user=- port=-1"
                        + " groups=[] admin=false reason=-",
                OidcAuditEvent.format("DENY", null, null, null, null, null, -1, null, false, null));
    }

    @Test
    public void t20PublicApiCannotNameATokenCodeOrHeaderAndItsParameterListsArePinned() throws Exception {
        Method allow = OidcAuditEvent.class.getDeclaredMethod("allow", Surface.class, OidcIdentity.class,
                String.class, int.class, Collection.class, boolean.class, String.class);
        Method deny = OidcAuditEvent.class.getDeclaredMethod("deny", Surface.class, String.class, String.class,
                String.class, int.class, Reason.class);
        Method denyNoMapping = OidcAuditEvent.class.getDeclaredMethod("denyNoMapping", Surface.class,
                OidcIdentity.class, String.class, int.class);

        assertTrue(Modifier.isPublic(allow.getModifiers()) && Modifier.isStatic(allow.getModifiers()));
        assertTrue(Modifier.isPublic(deny.getModifiers()) && Modifier.isStatic(deny.getModifiers()));
        assertTrue(Modifier.isPublic(denyNoMapping.getModifiers()) && Modifier.isStatic(denyNoMapping.getModifiers()));

        int publicMethods = 0;
        for (Method method : OidcAuditEvent.class.getDeclaredMethods()) {
            if (!Modifier.isPublic(method.getModifiers())) {
                continue;
            }
            publicMethods++;
            for (Parameter parameter : method.getParameters()) {
                String name = parameter.getName().toLowerCase(java.util.Locale.ENGLISH);
                assertFalse(method + " exposes a token-like parameter", name.contains("token"));
                assertFalse(method + " exposes a code-like parameter", name.contains("code"));
                assertFalse(method + " exposes a header-like parameter", name.contains("header"));
            }
        }
        assertEquals("only the three pinned logging entry points are public", 3, publicMethods);
    }
}
