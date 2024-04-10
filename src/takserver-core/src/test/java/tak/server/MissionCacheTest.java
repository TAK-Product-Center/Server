package tak.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.UUID;

import org.apache.ignite.Ignite;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.bbn.marti.service.SubscriptionStore;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TakServerTestApplicationConfig.class})
public class MissionCacheTest {
//	private static final String MISSION1 = "mission1";
//	private static final String MISSION2 = "mission2";
//	private static final String MISSION3 = "mission3";
	
	// replacing these names with random UUIDs - TODO: check if failures
	private static final UUID MISSION1 = UUID.randomUUID();
	private static final UUID MISSION2 = UUID.randomUUID();
	private static final UUID MISSION3 = UUID.randomUUID();

	private static final String UID1 = "uid1";
	private static final String UID2 = "uid2";
	private static final String UID3 = "uid3";

	@Autowired
	Ignite ignite;
	
	@Autowired
	SubscriptionStore subscriptionStore;
	
	@Test
	public void uidToMission() {
		subscriptionStore.putUidToMission(MISSION1, UID1);
		subscriptionStore.putUidToMission(MISSION1, UID1);
		subscriptionStore.putUidToMission(MISSION1, UID2);

		
		Collection<String> uids = subscriptionStore.getUidsByMission(MISSION1);
		
		assertEquals(2, uids.size());
		assertTrue(uids.contains(UID1));
		assertTrue(uids.contains(UID2));
		assertTrue(!uids.contains(UID3));	
		
		subscriptionStore.removeUidByMission(MISSION1, UID1);
		uids = subscriptionStore.getUidsByMission(MISSION1);
		
		assertEquals(1, uids.size());
		assertTrue(!uids.contains(UID1));
		assertTrue(uids.contains(UID2));
		assertTrue(!uids.contains(UID3));
	}
	
	@Test
	public void missionToUid() {
		subscriptionStore.putMissionToUid(UID1, MISSION1);
		subscriptionStore.putMissionToUid(UID1, MISSION1);
		subscriptionStore.putMissionToUid(UID1, MISSION2);
		
		Collection<UUID> missions = subscriptionStore.getMissionsByUid(UID1);
		
		assertEquals(2, missions.size());
		assertTrue(missions.contains(MISSION1));
		assertTrue(missions.contains(MISSION2));
		assertTrue(!missions.contains(MISSION3));	
		
		subscriptionStore.removeMissionByUid(UID1, MISSION1);
		missions = subscriptionStore.getMissionsByUid(UID1);
		
		assertEquals(1, missions.size());
		assertTrue(!missions.contains(MISSION1));
		assertTrue(missions.contains(MISSION2));
		assertTrue(!missions.contains(MISSION3));	
	}
	
	@Test
	public void uidToMissionContents() {
		subscriptionStore.putUidToMissionContents(MISSION1, UID1);
		subscriptionStore.putUidToMissionContents(MISSION1, UID1);
		subscriptionStore.putUidToMissionContents(MISSION1, UID2);

		
		Collection<String> uids = subscriptionStore.getUidsByMissionContents(MISSION1);
		
		assertEquals(2, uids.size());
		assertTrue(uids.contains(UID1));
		assertTrue(uids.contains(UID2));
		assertTrue(!uids.contains(UID3));	
		
		subscriptionStore.removeUidByMissionContents(MISSION1, UID1);
		uids = subscriptionStore.getUidsByMissionContents(MISSION1);
		
		assertEquals(1, uids.size());
		assertTrue(!uids.contains(UID1));
		assertTrue(uids.contains(UID2));
		assertTrue(!uids.contains(UID3));
	}
	
	@Test
	public void missionToUidContents() {
		subscriptionStore.putMissionToContentsUid(UID1, MISSION1);
		subscriptionStore.putMissionToContentsUid(UID1, MISSION1);
		subscriptionStore.putMissionToContentsUid(UID1, MISSION2);

		
		Collection<UUID> missions = subscriptionStore.getMissionsByContentsUid(UID1);
		
		assertEquals(2, missions.size());
		assertTrue(missions.contains(MISSION1));
		assertTrue(missions.contains(MISSION2));
		assertTrue(!missions.contains(MISSION3));	
		
		subscriptionStore.removeMissionByContentsUid(UID1, MISSION1);
		missions = subscriptionStore.getMissionsByContentsUid(UID1);
		
		assertEquals(1, missions.size());
		assertTrue(!missions.contains(MISSION1));
		assertTrue(missions.contains(MISSION2));
		assertTrue(!missions.contains(MISSION3));	
	}
	
	
}
