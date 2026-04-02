package tak.server;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.ignite.Ignite;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.bbn.marti.remote.groups.Direction;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.util.RemoteUtil;

import tak.server.ignite.cache.IgniteCacheHolder;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { TakServerTestApplicationConfig.class })
public class IgniteCacheTests {

	private static final Logger logger = LoggerFactory.getLogger(IgniteCacheTests.class);

	@Autowired
	private Ignite ignite;

	@Test
	public void getAllLatestSAsForGroupVector() {
		assertNotNull("ignite instance", ignite);

		// Create two groups g1,g2
		Group in1 = new Group("g1", Direction.IN);
		in1.setBitpos(0);
		Group out1 = new Group("g1", Direction.OUT);
		out1.setBitpos(0);

		Group in2 = new Group("g2", Direction.IN);
		in2.setBitpos(1);
		Group out2 = new Group("g2", Direction.OUT);
		out2.setBitpos(1);

		Set<Group> groups1 = new HashSet<>();
		groups1.add(in1);
		groups1.add(out1);

		Set<Group> groups2 = new HashSet<>();
		groups2.add(in2);
		groups2.add(out2);

		Set<Group> groups3 = new HashSet<>();
		groups3.add(in1);
		groups3.add(out1);
		groups3.add(in2);
		groups3.add(out2);

		String vector1 = RemoteUtil.getInstance()
				.bitVectorToString(RemoteUtil.getInstance().getBitVectorForGroups(groups1));
		String vector2 = RemoteUtil.getInstance()
				.bitVectorToString(RemoteUtil.getInstance().getBitVectorForGroups(groups2));
		String vector3 = RemoteUtil.getInstance()
				.bitVectorToString(RemoteUtil.getInstance().getBitVectorForGroups(groups3));

		// make 3 users
		String id1 = "id1";
		String cot1 = "cot1";

		String id2 = "id2";
		String cot2 = "cot2";

		String id3 = "id3";
		String cot3 = "cot3";

		// user 1 has group g1
		IgniteCacheHolder.getIgniteLatestSAConnectionUidCache().put(id1, cot1);
		IgniteCacheHolder.getIgniteUserOutboundGroupCache().put(id1, vector1);

		// user 2 has group g2
		IgniteCacheHolder.getIgniteLatestSAConnectionUidCache().put(id2, cot2);
		IgniteCacheHolder.getIgniteUserOutboundGroupCache().put(id2, vector2);

		// user 3 has group g1,g2
		IgniteCacheHolder.getIgniteLatestSAConnectionUidCache().put(id3, cot3);
		IgniteCacheHolder.getIgniteUserOutboundGroupCache().put(id3, vector3);

		// should only have cot1
		Collection<String> latestSAs1 = IgniteCacheHolder.getAllLatestSAsForGroupVector(vector1);
		System.out.println("latestSAs1 " + String.join(", ", latestSAs1));
		assertTrue(latestSAs1.contains(cot1));
		assertFalse(latestSAs1.contains(cot2));
		assertTrue(latestSAs1.contains(cot3));

		// should only have cot2
		Collection<String> latestSAs2 = IgniteCacheHolder.getAllLatestSAsForGroupVector(vector2);
		System.out.println("latestSAs2 " + String.join(", ", latestSAs2));
		assertFalse(latestSAs2.contains(cot1));
		assertTrue(latestSAs2.contains(cot2));
		assertTrue(latestSAs2.contains(cot3));

		// should have cot1, cot2, and cot3
		Collection<String> latestSAs3 = IgniteCacheHolder.getAllLatestSAsForGroupVector(vector3);
		System.out.println("latestSAs3 " + String.join(", ", latestSAs3));
		assertTrue(latestSAs3.contains(cot1));
		assertTrue(latestSAs3.contains(cot2));
		assertTrue(latestSAs3.contains(cot3));
	}
}
