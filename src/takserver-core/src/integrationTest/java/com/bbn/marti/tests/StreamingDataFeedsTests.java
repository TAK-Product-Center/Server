package com.bbn.marti.tests;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import com.bbn.marti.test.shared.AbstractTestClass;
import com.bbn.marti.test.shared.TestManipulatorInterface;
import com.bbn.marti.test.shared.data.connections.AbstractConnection;
import com.bbn.marti.test.shared.data.connections.ConnectionFilter;
import com.bbn.marti.test.shared.data.generated.ImmutableConnections;
import com.bbn.marti.test.shared.data.protocols.ProtocolProfiles;
import com.bbn.marti.test.shared.data.servers.ImmutableServerProfiles;
import com.bbn.marti.test.shared.data.users.AbstractUser;
import com.bbn.marti.test.shared.data.users.UserFilter;

public class StreamingDataFeedsTests extends AbstractTestClass {

	private static final String className = "StreamingDataFeedsTests";

	@Test(timeout = 14000000)
	public void dataFeedRemoveAddTest() {
		randomize = true;
		randomSeed = 348057624308987L;
		dataFeedRemoveAddTest(null);
	}
	

	public void dataFeedRemoveAddTest(TestManipulatorInterface manipulator) {
		try {
			String sessionIdentifier = initTestMethod();

			// add all the inputs with their initial users

//            List<GenConnections> dataFeedList = ServerProfiles.SERVER_0.getInputs();
			List<AbstractConnection> dataFeedListDefault = new ArrayList(
					ImmutableConnections.valuesFiltered(new ConnectionFilter()
							.addServerProfile(ImmutableServerProfiles.SERVER_0)
							.addConnectionTypes(ProtocolProfiles.ConnectionType.DATAFEED)));
			
			List<AbstractConnection> dataFeedList = new LinkedList<>();
			for (AbstractConnection connection : dataFeedListDefault) {
				switch (connection.getProtocol()) {

					case INPUT_TCP:
					case INPUT_UDP:
					case INPUT_MCAST:
					case INPUT_STCP:
					case INPUT_TLS:
					case INPUT_SSL:
					case SUBSCRIPTION_TCP:
					case SUBSCRIPTION_UDP:
					case SUBSCRIPTION_MCAST:
					case DATAFEED_TCP:
					case DATAFEED_UDP:
					case DATAFEED_MCAST:
						break;

					case SUBSCRIPTION_STCP:
					case SUBSCRIPTION_TLS:
					case SUBSCRIPTION_SSL:
					case DATAFEED_STCP:
					case DATAFEED_TLS:
					case DATAFEED_SSL:
						dataFeedList.add(connection);
				}
			}

			List<AbstractUser> userList = new ArrayList<>(2 * dataFeedList.size());
			List<AbstractUser> sendableUserList = new LinkedList<>();
			List<AbstractUser> connectableUserList = new LinkedList<>();

			randomize(dataFeedList);

			for (AbstractConnection dataFeed : dataFeedList) {
				AbstractUser[] dataFeedUsers = dataFeed.getUsers(new UserFilter().setUserActivityIsValidated(true)).toArray(new AbstractUser[0]);
				AbstractUser u0 = dataFeedUsers[0];
				AbstractUser u1 = dataFeedUsers[1];
				userList.add(u0);
				userList.add(u1);
				engine.offlineAddUsersAndConnectionsIfNecessary(u0);
				engine.offlineAddUsersAndConnectionsIfNecessary(u1);
				if (dataFeed.getProtocol().canConnect()) {
					connectableUserList.add(u0);
					connectableUserList.add(u1);
				}
				if (dataFeed.getProtocol().canSend()) {
					sendableUserList.add(u0);
					sendableUserList.add(u1);
				}
			}

			if (manipulator != null) {
				manipulator.preTestRunPreServerStart(engine);
			}

			engine.startServer(ImmutableServerProfiles.SERVER_0, sessionIdentifier);

			if (manipulator != null) {
				manipulator.preTestRunPostServerStart(engine);
			}

			randomize(connectableUserList);

			engine.connectClientsAndVerify(true, connectableUserList.toArray(new AbstractUser[0]));

			randomize(sendableUserList);

			for (AbstractUser user : sendableUserList) {
				engine.attemptSendFromUserAndVerify(user);
			}

			randomize(dataFeedList);

			for (AbstractConnection dataFeed : dataFeedList) {
				engine.onlineAddDataFeed(dataFeed);

				AbstractUser[] inputUsers = dataFeed.getUsers(new UserFilter().setUserActivityIsValidated(true)).toArray(new AbstractUser[0]);
				AbstractUser u0 = inputUsers[0];
				AbstractUser u1 = inputUsers[1];

				if (connectableUserList.contains(u0)) {
					engine.connectClientAndVerify(true, u0);
				}

				if (connectableUserList.contains(u1)) {
					engine.connectClientAndVerify(true, u1);
				}

				List<AbstractUser> looplist = engine.getUsersThatCanSend();
				randomize(looplist);

				for (AbstractUser user : looplist) {
					engine.attemptSendFromUserAndVerify(user);
				}
			}

			randomize(dataFeedList);

			for (AbstractConnection dataFeed : dataFeedList) {
				engine.onlineRemoveDataFeedAndVerify(dataFeed);

				List<AbstractUser> loopList = engine.getUsersThatCanSend();
				randomize(loopList);

				for (AbstractUser user : loopList) {
					engine.attemptSendFromUserAndVerify(user);
				}
			}

		} finally {
			if (manipulator != null) {
				manipulator.postTestRunPreServerStop(engine);
			}
			engine.stopServers(ImmutableServerProfiles.SERVER_0);
			if (manipulator != null) {
				manipulator.postTestRunPostServerStop(engine);
			}
		}
	}

}
