package com.bbn.marti.tests;

import com.bbn.marti.test.shared.AbstractTestClass;
import com.bbn.marti.test.shared.TestManipulatorInterface;
import com.bbn.marti.test.shared.data.connections.AbstractConnection;
import com.bbn.marti.test.shared.data.connections.ConnectionFilter;
import com.bbn.marti.test.shared.data.generated.ImmutableConnections;
import com.bbn.marti.test.shared.data.protocols.ProtocolProfiles;
import com.bbn.marti.test.shared.data.servers.ImmutableServerProfiles;
import com.bbn.marti.test.shared.data.users.AbstractUser;
import com.bbn.marti.test.shared.data.users.UserFilter;
import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created on 10/26/15.
 */
public class InputTests extends AbstractTestClass {

	private static final String className = "InputTests";

	@Test(timeout = 14000000)
	public void inputRemoveAddTest() {
		randomize = true;
		randomSeed = 348057624308987L;
		inputRemoveAddTest(null);
	}

	public void inputRemoveAddTest(TestManipulatorInterface manipulator) {
		try {
			String sessionIdentifier = initTestMethod();

			// add all the inputs with their initial users

//            List<GenConnections> inputList = ServerProfiles.SERVER_0.getInputs();
			List<AbstractConnection> inputListDefault = new ArrayList(
					ImmutableConnections.valuesFiltered(new ConnectionFilter()
							.addServerProfile(ImmutableServerProfiles.SERVER_0)
							.addConnectionTypes(ProtocolProfiles.ConnectionType.INPUT)));

			List<AbstractConnection> inputList = new LinkedList<>();
			for (AbstractConnection connection : inputListDefault) {
				switch (connection.getProtocol()) {

					case INPUT_TCP:
					case INPUT_UDP:
					case INPUT_MCAST:
					case SUBSCRIPTION_TCP:
					case SUBSCRIPTION_UDP:
					case SUBSCRIPTION_MCAST:
						break;

					case INPUT_STCP:
					case INPUT_TLS:
					case INPUT_SSL:
					case SUBSCRIPTION_STCP:
					case SUBSCRIPTION_TLS:
					case SUBSCRIPTION_SSL:
						inputList.add(connection);
				}
			}

			List<AbstractUser> userList = new ArrayList<>(2 * inputList.size());
			List<AbstractUser> sendableUserList = new LinkedList<>();
			List<AbstractUser> connectableUserList = new LinkedList<>();

			randomize(inputList);

			for (AbstractConnection input : inputList) {
				AbstractUser[] inputUsers = input.getUsers(new UserFilter().setUserActivityIsValidated(true)).toArray(new AbstractUser[0]);
				AbstractUser u0 = inputUsers[0];
				AbstractUser u1 = inputUsers[1];
				userList.add(u0);
				userList.add(u1);
				engine.offlineAddUsersAndConnectionsIfNecessary(u0);
				engine.offlineAddUsersAndConnectionsIfNecessary(u1);
				if (input.getProtocol().canConnect()) {
					connectableUserList.add(u0);
					connectableUserList.add(u1);
				}
				if (input.getProtocol().canSend()) {
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

			randomize(inputList);

			for (AbstractConnection input : inputList) {
				engine.onlineRemoveInputAndVerify(input);

				List<AbstractUser> loopList = engine.getUsersThatCanSend();
				randomize(loopList);

				for (AbstractUser user : loopList) {
					engine.attemptSendFromUserAndVerify(user);
				}
			}

			randomize(inputList);

			for (AbstractConnection input : inputList) {
				engine.onlineAddInput(input);

				AbstractUser[] inputUsers = input.getUsers(new UserFilter().setUserActivityIsValidated(true)).toArray(new AbstractUser[0]);
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
