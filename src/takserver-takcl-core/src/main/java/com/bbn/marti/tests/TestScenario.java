package com.bbn.marti.tests;

import com.bbn.marti.takcl.TestLogger;
import com.bbn.marti.test.shared.AbstractTestClass;
import com.bbn.marti.test.shared.data.connections.MutableConnection;
import com.bbn.marti.test.shared.data.generated.ImmutableConnections;
import com.bbn.marti.test.shared.data.servers.ImmutableServerProfiles;
import com.bbn.marti.test.shared.data.servers.MutableServerProfile;
import com.bbn.marti.test.shared.data.users.AbstractUser;
import com.bbn.marti.test.shared.data.users.BaseUsers;
import com.bbn.marti.test.shared.data.users.MutableUser;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Created on 10/26/15.
 */
public class TestScenario extends AbstractTestClass {

	private static final String className = "TestScenario";

	@Test
	public void testScenario() {
		String sessionIdentifier = initTestMethod();

		double sendChance = 1.0;
		double disconnectChance = 0.004;
		double connectChance = 0.02;
		int userCount = 36;
		int intervalMS = 1000;

		List<MutableUser> onlineUserList = new ArrayList<>();
		List<MutableUser> offlineUserList = new ArrayList<>();

		long seed = 1820165335122579573L;
		Random r = new Random(seed);

		MutableServerProfile server = ImmutableServerProfiles.SERVER_0.getMutableInstance();
		MutableConnection connection = server.generateConnection(ImmutableConnections.s0_ssl);

		for (int i = 0; i < userCount / 2; i++) {
			MutableUser u = connection.generateConnectionUser(BaseUsers.anonuser, true, Integer.toString(i));
			engine.offlineAddUsersAndConnectionsIfNecessary(u);
            onlineUserList.add(u);
		}

		for (int i = 0; i < userCount / 2; i++) {
			MutableUser u = connection.generateConnectionUser(BaseUsers.anonuser, true, Integer.toString(i + userCount / 2));
			engine.offlineAddUsersAndConnectionsIfNecessary(u);
			offlineUserList.add(u);
		}

		engine.offlineEnableLatestSA(true, server);

		engine.startServer(ImmutableServerProfiles.SERVER_0, sessionIdentifier);

		engine.connectClientsAndVerify(true, onlineUserList.toArray(new AbstractUser[0]));

		LinkedList<MutableUser> onlineChangeUser = new LinkedList<>();


		while (true) {
			randomize(onlineUserList);
			randomize(offlineUserList);

			Iterator<MutableUser> onlineIterator = onlineUserList.iterator();
			Iterator<MutableUser> offlineIterator = offlineUserList.iterator();

			onlineChangeUser.clear();

			while (onlineIterator.hasNext() && offlineIterator.hasNext()) {
				double d;

				MutableUser onlineUser = null;
				MutableUser offlineUser = null;

				if (onlineIterator.hasNext()) {
					onlineUser = onlineIterator.next();
				}
				if (offlineIterator.hasNext()) {
					offlineUser = offlineIterator.next();
				}

				if (onlineUser != null) {
					d = r.nextDouble() % 1;
					if (d <= disconnectChance) {
						engine.disconnectClientAndVerify(onlineUser);
						onlineChangeUser.add(onlineUser);
					} else {
						d = r.nextDouble() % 1;
						if (d <= sendChance) {
							engine.attemptSendFromUserAndVerify(onlineUser);
						}
					}
				}

				if (offlineUser != null) {
					d = r.nextDouble() % 1;
					if (d < connectChance) {
						engine.connectClientAndVerify(true, offlineUser);
						onlineChangeUser.add(offlineUser);
					}
				}

				try {
					Thread.sleep(intervalMS);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}

			for (MutableUser mut : onlineChangeUser) {
				if (onlineUserList.contains(mut)) {
					onlineUserList.remove(mut);
					offlineUserList.add(mut);
				} else if (offlineUserList.contains(mut)) {
					offlineUserList.remove(mut);
					onlineUserList.add(mut);
				}
			}
		}
	}

}
