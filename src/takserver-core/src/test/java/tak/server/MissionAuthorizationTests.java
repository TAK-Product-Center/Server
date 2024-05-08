package tak.server;

import static org.junit.Assert.assertEquals;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.oxm.XmlMappingException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.bbn.marti.jwt.JwtUtils;
import com.bbn.marti.sync.model.MissionPermission;
import com.bbn.marti.sync.model.MissionRole;
import com.bbn.marti.sync.service.MissionTokenUtils;

import io.jsonwebtoken.Claims;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TakServerTestApplicationConfig.class})
public class MissionAuthorizationTests {
    
    // Using random mission GUID
    private static final UUID missionGuid = UUID.randomUUID();
    
    private static final String missionName = "testMission";

    @Test
    public void testMissionToken() throws XmlMappingException, NoSuchAlgorithmException, InvalidKeySpecException {

        String tokenId = UUID.randomUUID().toString();

        JwtUtils.getInstanceGenerateKeys();

        String token = MissionTokenUtils
                .getInstance(JwtUtils.getInstance().getPrivateKey())
                .createMissionToken(
                        tokenId, missionName, MissionTokenUtils.TokenType.SUBSCRIPTION,-1, "MissionAuthorizationTests", missionGuid);

        Claims claims = MissionTokenUtils
                .getInstance(JwtUtils.getInstance().getPrivateKey())
                .decodeMissionToken(token);

        assertEquals(claims.getId(), tokenId);
    }

    @Test
    public void testMissionRoles() {
        MissionRole owner = new MissionRole(MissionRole.Role.MISSION_OWNER);
        owner.getMissionPermissions().add(new MissionPermission(MissionPermission.Permission.MISSION_READ));
        owner.getMissionPermissions().add(new MissionPermission(MissionPermission.Permission.MISSION_WRITE));
        assertEquals(owner.hasPermission(MissionPermission.Permission.MISSION_READ), true);
        assertEquals(owner.hasPermission(MissionPermission.Permission.MISSION_WRITE), true);

        MissionRole readOnlySubscriber = new MissionRole(MissionRole.Role.MISSION_READONLY_SUBSCRIBER);
        readOnlySubscriber.getMissionPermissions().add(new MissionPermission(MissionPermission.Permission.MISSION_READ));
        assertEquals(readOnlySubscriber.hasPermission(MissionPermission.Permission.MISSION_READ), true);
        assertEquals(readOnlySubscriber.hasPermission(MissionPermission.Permission.MISSION_WRITE), false);
    }
}

