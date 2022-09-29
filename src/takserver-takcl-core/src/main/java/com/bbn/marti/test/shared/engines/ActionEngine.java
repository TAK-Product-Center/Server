package com.bbn.marti.test.shared.engines;

import com.bbn.marti.config.AuthType;
import com.bbn.marti.takcl.AppModules.OfflineConfigModule;
import com.bbn.marti.takcl.AppModules.OnlineFileAuthModule;
import com.bbn.marti.takcl.SSLHelper;
import com.bbn.marti.takcl.TAKCLCore;
import com.bbn.marti.takcl.TestExceptions;
import com.bbn.marti.takcl.connectivity.AbstractRunnableServer;
import com.bbn.marti.takcl.connectivity.RunnableServerManager;
import com.bbn.marti.takcl.connectivity.implementations.UnifiedClient;
import com.bbn.marti.takcl.connectivity.interfaces.ClientResponseListener;
import com.bbn.marti.takcl.connectivity.missions.MissionModels;
import com.bbn.marti.test.shared.CotGenerator;
import com.bbn.marti.test.shared.TestConnectivityState;
import com.bbn.marti.test.shared.data.GroupProfiles;
import com.bbn.marti.test.shared.data.GroupSetProfiles;
import com.bbn.marti.test.shared.data.connections.AbstractConnection;
import com.bbn.marti.test.shared.data.connections.MutableConnection;
import com.bbn.marti.test.shared.data.servers.AbstractServerProfile;
import com.bbn.marti.test.shared.data.servers.ImmutableServerProfiles;
import com.bbn.marti.test.shared.data.users.AbstractUser;
import com.bbn.marti.test.shared.data.users.MutableUser;
import com.bbn.marti.test.shared.engines.state.StateEngine;
import com.bbn.marti.test.shared.engines.state.UserState;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Console;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static com.bbn.marti.takcl.connectivity.missions.MissionModels.*;

/**
 * Used to synchronize actions performed in relation to the server (both online and offline)
 * <p/>
 * Created on 10/9/15.
 */
public class ActionEngine implements EngineInterface {

	/**
	 * Do not ever add params without updating the {@link #clearIterationData()} or {@link #hasChanged()} methods!
	 */
	public static class ActionClient extends UnifiedClient implements Comparable<ActionClient> {

		private final Logger logger = LoggerFactory.getLogger(ActionClient.class);

		private ClientResponseListener innerListener = new ClientResponseListener() {
			@Override
			public void onMessageReceived(String response) {
				try {
//					dl.begin("ActionEngineClient Message Received");

					Document doc = DocumentHelper.parseText(response);

					String uid = CotGenerator.parseClientUID(doc);
					if (uid != null) {
						receivedSenderUids.add(uid);
					}
					String callsign = CotGenerator.parseCallsign(doc);
					if (callsign != null) {
						receivedSenderCallsigns.add(callsign);
					}

					receivedMessages.add(response);
//					dl.end("ActionEngineClient Message Received");

				} catch (DocumentException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			}

			@Override
			public void onMessageSent(String message) {

			}

			@Override
			public void onConnectivityStateChange(TestConnectivityState state) {

			}
		};

		//		private final Util.DurationLogger dl;
		private final CopyOnWriteArrayList<String> receivedMessages = new CopyOnWriteArrayList<>();
		private final ConcurrentSkipListSet<String> receivedSenderCallsigns = new ConcurrentSkipListSet<>();
		private final ConcurrentSkipListSet<String> receivedSenderUids = new ConcurrentSkipListSet<>();

		private Document sentCotMessage;
		private ResponseWrapper callResponse;
		private ResponseWrapper verificationCallResponse;

		public Object stateEngineData;
		public Boolean stateEngineData_userHadPermissions;

		public Document getSentCotMessage() {
			return sentCotMessage;
		}

		@Nullable
		public ResponseWrapper getCallResponse() {
			return callResponse;
		}

		@Nullable
		public ResponseWrapper getVerificationCallResponse() {
			return verificationCallResponse;
		}

		public ActionClient(@NotNull AbstractUser user) {
			super(user);
//			dl = new Util.DurationLogger(user.getConsistentUniqueReadableIdentifier(), logger);
			super.addListener(innerListener);
		}

		@Override
		public int compareTo(@NotNull ActionClient o) {
			if (o == null) {
				return 1;
			} else {
				return (getProfile().getConsistentUniqueReadableIdentifier().compareTo(o.getProfile().getConsistentUniqueReadableIdentifier()));
			}
		}

		public List<String> getRecievedMessages() {
			return new ArrayList<>(receivedMessages);
		}


		public TreeSet<String> getReceivedSenderCallsigns() {
			return new TreeSet<>(receivedSenderCallsigns);
		}

		public TreeSet<String> getReceivedSenderUids() {
			return new TreeSet<>(receivedSenderUids);
		}


		public synchronized boolean hasChanged() {
			return !(receivedMessages.size() == 0 &&
					receivedSenderCallsigns.size() == 0 &&
					receivedSenderUids.size() == 0 &&
					callResponse == null &&
					verificationCallResponse == null &&
					sentCotMessage == null &&
					stateEngineData == null &&
					stateEngineData_userHadPermissions == null
			);
		}

		protected synchronized void clearIterationData() {
			receivedMessages.clear();
			receivedSenderCallsigns.clear();
			receivedSenderUids.clear();
			callResponse = null;
			verificationCallResponse = null;
			sentCotMessage = null;
			stateEngineData = null;
			stateEngineData_userHadPermissions = null;
		}
	}


	public static AbstractRunnableServer getRunnableInstanceAndBuildIfnecessary(AbstractServerProfile server) {
		if (serverManager.serverInstanceExists(server)) {
			return serverManager.getServerInstance(server);
		} else {
			return serverManager.buildServerInstance(server);
		}
	}


//    public interface ActionClient extends Comparable {
////        TestConnectivityState getConnectivityState();
////        String getLatestSA();
////        List<String> getRecievedMessages();
////        AbstractUser getProfile();
//        
//        boolean isConnected();
//    }

	public static class ActionEngineData {

		private TreeMap<AbstractUser, ActionClient> clients = new TreeMap<>();

		private TreeMap<AbstractServerProfile, AbstractUser> serverUserCallsignKnownMap = new TreeMap<>();
		private TreeMap<AbstractServerProfile, AbstractUser> serverUserUidKnownMap = new TreeMap<>();

		private synchronized ActionClient getClient(AbstractUser user) {
			if (!clients.containsKey(user)) {
				clients.put(user, new ActionClient(user));
			}
			return clients.get(user);
		}

		public ActionClient getState(AbstractUser user) {
			return getClient(user);
		}

		private synchronized void engineIterationDataClear() {
			for (ActionClient client : clients.values()) {
				client.clearIterationData();
			}
		}

		public synchronized TreeSet<ActionClient> getAllClients() {
			return new TreeSet<>(clients.values());
		}

		public synchronized TreeMap<AbstractUser, ActionClient> getAllChangedClients() {
			return new TreeMap<>(clients.values().stream().filter(ActionClient::hasChanged)
					.collect(Collectors.toMap(ActionClient::getProfile, client -> client)));
		}

		synchronized void clear() {
			// TODO: Disconnect?
			for (ActionClient client : clients.values()) {
				try {
					client.disconnect();
				} catch (Exception e) {
					// Ignore
				}
				try {
					client.cleanup();
				} catch (Exception e) {
					// Ignore
				}
			}
			clients.clear();
		}
	}

	public static final ActionEngineData data = new ActionEngineData();

	private static final double SLEEP_ADDREMOVE_INPUT_BASE = 4000; // 2000;
	private static final double SLEEP_TIME_CONNECT_BASE = 1600; // 800; // 400;
	private static final double SLEEP_TIME_AUTHENTICATE_BASE = 1600; // 800; // 400;
	private static final double SLEEP_TIME_SEND_MESSAGE_BASE = 1600; // 1400; // 1000; // 400; // 200;
	private static final double SLEEP_TIME_DISCONNECT_BASE = 1600; // 800; // 400;
	private static final double SLEEP_TIME_SERVER_START_BASE = 120000;
	private static final double SLEEP_TIME_SERVER_STOP_BASE = 20000; // 8000; // 4000;
	private static final double SLEEP_TIME_GROUP_MANIPULATION_BASE = 2400; // 1200; // 600;

	private static double SLEEP_MULTIPLIER = TAKCLCore.sleepMultiplier;
	private static int SLEEP_ADDREMOVE_INPUT = (int) (SLEEP_ADDREMOVE_INPUT_BASE * SLEEP_MULTIPLIER);
	private static int SLEEP_TIME_CONNECT = (int) (SLEEP_TIME_CONNECT_BASE * SLEEP_MULTIPLIER);
	private static int SLEEP_TIME_AUTHENTICATE = (int) (SLEEP_TIME_AUTHENTICATE_BASE * SLEEP_MULTIPLIER);
	private static int SLEEP_TIME_SEND_MESSAGE = (int) (SLEEP_TIME_SEND_MESSAGE_BASE * SLEEP_MULTIPLIER);
	private static int SLEEP_TIME_DISCONNECT = (int) (SLEEP_TIME_DISCONNECT_BASE * SLEEP_MULTIPLIER);
	protected static int SLEEP_TIME_SERVER_START = (int) SLEEP_TIME_SERVER_START_BASE; //Ignored since this has a massive impact on test startup time and the default is excessively long.
	protected static int SLEEP_TIME_SERVER_STOP = (int) (SLEEP_TIME_SERVER_STOP_BASE * SLEEP_MULTIPLIER);
	private static int SLEEP_TIME_GROUP_MANIPULATION = (int) (SLEEP_TIME_GROUP_MANIPULATION_BASE * SLEEP_MULTIPLIER);

	public static AbstractRunnableServer.RUNMODE runMode = AbstractRunnableServer.RUNMODE.AUTOMATIC;

	public static int getServerStartTimeDelay() {
		return SLEEP_TIME_SERVER_START;
	}

	private static final AbstractServerProfile DEFAULT_SERVER = ImmutableServerProfiles.SERVER_0;

	private static final RunnableServerManager serverManager = RunnableServerManager.getInstance();

	public ActionEngine(AbstractServerProfile... servers) {
		if (servers == null) {
			servers = new AbstractServerProfile[]{DEFAULT_SERVER};
		}

		for (AbstractServerProfile serverIdentifier : servers) {
			serverManager.buildServerInstance(serverIdentifier);
		}
	}

	private static String toString(MutableUser... users) {
		return toString(Arrays.asList(users));
	}

	private static String toString(Collection<MutableUser> userList) {
		StringBuilder sb = new StringBuilder();
		for (MutableUser user : userList) {
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append(user.getConsistentUniqueReadableIdentifier());
		}
		return sb.toString();
	}

	public synchronized void setSleepMultiplier(double multiplier) {
		SLEEP_MULTIPLIER = multiplier * TAKCLCore.sleepMultiplier;

		SLEEP_ADDREMOVE_INPUT = (int) (SLEEP_ADDREMOVE_INPUT_BASE * SLEEP_MULTIPLIER);
		SLEEP_TIME_CONNECT = (int) (SLEEP_TIME_CONNECT_BASE * SLEEP_MULTIPLIER);
		SLEEP_TIME_AUTHENTICATE = (int) (SLEEP_TIME_AUTHENTICATE_BASE * SLEEP_MULTIPLIER);
		SLEEP_TIME_SEND_MESSAGE = (int) (SLEEP_TIME_SEND_MESSAGE_BASE * SLEEP_MULTIPLIER);
		SLEEP_TIME_DISCONNECT = (int) (SLEEP_TIME_DISCONNECT_BASE * SLEEP_MULTIPLIER);
//		SLEEP_TIME_SERVER_START = (int) (SLEEP_TIME_SERVER_START_BASE * SLEEP_MULTIPLIER);
		SLEEP_TIME_SERVER_STOP = (int) (SLEEP_TIME_SERVER_STOP_BASE * SLEEP_MULTIPLIER);
		SLEEP_TIME_GROUP_MANIPULATION = (int) (SLEEP_TIME_GROUP_MANIPULATION_BASE * SLEEP_MULTIPLIER);
	}

	public synchronized void setSendValidationDelayMultiplier(double multiplier) {
		SLEEP_TIME_SEND_MESSAGE = (int) (SLEEP_TIME_SEND_MESSAGE_BASE * multiplier);
	}

	/**
	 * If set to true, all tests will pause when the server start or shutdown instruction is sent, allowing the user to
	 * manually turn the server on and off (useful for debugging)
	 *
	 * @param runMode What run mode to run in
	 */
	public static synchronized void setControlMode(AbstractRunnableServer.RUNMODE runMode) {
		ActionEngine.runMode = runMode;
		AbstractRunnableServer.setControlMode(runMode);
	}

	public static synchronized void setRemoteDebuggee(@Nullable Integer serverIdentifier) {
		AbstractRunnableServer.setDebuggee(serverIdentifier);
	}

	public void sleep(int milliseconds) {
		if (runMode.sleepAutomatically) {
			try {
				Thread.sleep(milliseconds);
			} catch (InterruptedException e) {
				System.out.println(e.getMessage());
			}

		} else {
			Console console = System.console();
			console.readLine("Press enter to continue.");
		}
	}

	private void checkUserState(AbstractUser user) {
		if (!serverManager.serverInstanceExists(user.getServer())) {
			throw new RuntimeException("Cannot execute an action with user '" + user.getUserName() + "' because the user's server '" + user.getServer().getConsistentUniqueReadableIdentifier() + "' has not been set up!");
		}
	}


	@Override
	public synchronized void offlineEnableLatestSA(boolean enabled, @NotNull AbstractServerProfile... servers) {
		data.engineIterationDataClear();
		for (AbstractServerProfile server : servers) {
			getRunnableInstanceAndBuildIfnecessary(server).getOfflineConfigModule().latestSA(enabled);
		}
	}

	@Override
	public void connectClientAndVerify(boolean doAuthIfNecessary, @NotNull AbstractUser user) {
		data.engineIterationDataClear();
		checkUserState(user);
		connectClientAndSendData(doAuthIfNecessary, user, null);

		if (doAuthIfNecessary && user.getConnection().requiresAuthentication()) {
			System.out.println("Connected and attempted authentication: " + user);
		} else {
			System.out.println("Connected: " + user);
		}

		sleep((SLEEP_TIME_CONNECT + (doAuthIfNecessary ? SLEEP_TIME_AUTHENTICATE : 0)) / 2);
	}

	@Override
	public void disconnectClientAndVerify(@NotNull AbstractUser disconnectingUser) {
		data.engineIterationDataClear();
		checkUserState(disconnectingUser);
		ActionClient disconnectingClient = data.getClient(disconnectingUser);
		if (disconnectingClient == null) {
			throw new RuntimeException("User '" + disconnectingUser + "' does not exist in the test engine!");
		} else if (!disconnectingClient.isConnected()) {
			System.err.println("Client " + disconnectingClient + " is already disconnected!");
		} else {
			disconnectingClient.disconnect();
			System.out.println(disconnectingClient + " disconnected.");
		}

		System.out.println("Disconnected: " + disconnectingClient);

		// TODO: There is a delay to get around a hack to get around an intentional delay needed since removal of a user on the server may result in the removal of message routing information before the message has been handled.
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		sleep(SLEEP_TIME_DISCONNECT);
	}

	@Override
	public void onlineRemoveInputAndVerify(@NotNull AbstractConnection input) {
		data.engineIterationDataClear();
		double t0 = System.currentTimeMillis();
		serverManager.getServerInstance(input.getServer()).getOnlineInputModule().remove(input.getConsistentUniqueReadableIdentifier());
		double t1 = System.currentTimeMillis();
		double tTot = (t1 - t0) / 1000;
		System.out.println("Removing input \"" + input.getConsistentUniqueReadableIdentifier() + "\"(" + tTot + "s).");
		sleep(SLEEP_ADDREMOVE_INPUT);
	}
	
	@Override
	public void onlineRemoveDataFeedAndVerify(@NotNull AbstractConnection dataFeed) {
		data.engineIterationDataClear();
		double t0 = System.currentTimeMillis();
		serverManager.getServerInstance(dataFeed.getServer()).getOnlineInputModule().removeDataFeed(dataFeed.getConsistentUniqueReadableIdentifier());
		double t1 = System.currentTimeMillis();
		double tTot = (t1 - t0) / 1000;
		System.out.println("Removing data feed \"" + dataFeed.getConsistentUniqueReadableIdentifier() + "\"(" + tTot + "s).");
		sleep(SLEEP_ADDREMOVE_INPUT);
	}

	@Override
	public void attemptSendFromUserAndVerify(@NotNull AbstractUser sendingUser, @NotNull AbstractUser... targetUsers) {
		attemptSendFromUserAndVerify(DefaultSendingUserIdentification, sendingUser, targetUsers);
	}

	@Override
	public void attemptSendFromUserAndVerify(@NotNull AbstractUser sendingUser, @NotNull String missionName) {
		attemptSendFromUserAndVerify(DefaultSendingUserIdentification, sendingUser, DefaultReceivingUserIdentification, missionName);
	}

	@Override
	public void attemptSendFromUserAndVerify(@NotNull UserIdentificationData senderIdentification, @NotNull AbstractUser sendingUser, @NotNull AbstractUser... targetUsers) {
		attemptSendFromUserAndVerify(senderIdentification, sendingUser, DefaultReceivingUserIdentification, targetUsers);
	}

	@Override
	public void attemptSendFromUserAndVerify(@NotNull UserIdentificationData senderIdentification, @NotNull AbstractUser sendingUser, @NotNull UserIdentificationData recipientIdentification, @NotNull AbstractUser... targetUsers) {
		attemptSendFromUserAndVerify(senderIdentification, sendingUser, recipientIdentification, null, targetUsers);
	}

	@Override
	public void attemptSendFromUserAndVerify(@NotNull UserIdentificationData senderIdentification, @NotNull AbstractUser sendingUser, @NotNull UserIdentificationData recipientIdentification, @Nullable String missionName, @NotNull AbstractUser... targetUsers) {
		data.engineIterationDataClear();
		ActionClient sendingClient = data.getClient(sendingUser);

		Document sendMessage = CotGenerator.createLatestSAMessage(senderIdentification, sendingUser, recipientIdentification, false, missionName, targetUsers);
		sendingClient.sentCotMessage = sendMessage;

		sendingClient.sendMessage(sendMessage);

		String message = sendingClient + " attempted to send a message" +
				(senderIdentification == UserIdentificationData.UID_AND_CALLSIGN ? " with UID and Callsign " :
						senderIdentification == UserIdentificationData.UID ? " with UID " :
								senderIdentification == UserIdentificationData.CALLSIGN ? " with Callsign " :
										"");

		if (targetUsers.length > 0) {
			StringBuilder userListBuilder = new StringBuilder();

			for (AbstractUser user : targetUsers) {
				String recipientString = null;

				if (recipientIdentification == UserIdentificationData.UID_AND_CALLSIGN) {
					recipientString = "{uid=" + user.getCotUid() + ",callsign=" + user.getCotCallsign() + "}";
				} else if (recipientIdentification == UserIdentificationData.UID) {
					recipientString = "{uid=" + user.getCotUid() + "}";
				} else if (recipientIdentification == UserIdentificationData.CALLSIGN) {
					recipientString = "{callsign=" + user.getCotCallsign() + "}";
				}

				if (userListBuilder.length() > 0) {
					userListBuilder.append(",");
				}
				userListBuilder.append(recipientString);
			}
			message += " to [" + userListBuilder.toString() + "].";

		} else {
			message += ".";
		}

		System.out.println(message);

		sleep(SLEEP_TIME_SEND_MESSAGE);
	}

	@Override
	public void updateLocalUserPassowrd(@NotNull MutableUser user) {
		data.engineIterationDataClear();
		user.updatePassword();
		System.out.println(user.getConsistentUniqueReadableIdentifier() + " will now start using their current password.");
	}

	@Override
	public void onlineAddInputToGroup(@NotNull MutableConnection input, @NotNull GroupProfiles group) {
		data.engineIterationDataClear();
		System.out.println("Adding input '" + input + "' to group '" + group.name() + "'.");
		serverManager.getServerInstance(input.getServer()).getOnlineInputModule().addInputToGroup(input.getConsistentUniqueReadableIdentifier(), group.name());
		input.addToGroup(group);
		sleep(SLEEP_TIME_GROUP_MANIPULATION);
	}

	@Override
	public void onlineRemoveInputFromGroup(@NotNull MutableConnection input, @NotNull GroupProfiles group) {
		data.engineIterationDataClear();
		System.out.println("Removing input '" + input + "' from group '" + group.name() + "'.");
		serverManager.getServerInstance(input.getServer()).getOnlineInputModule().removeInputFromGroup(input.getConsistentUniqueReadableIdentifier(), group.name());
		input.removeFromGroup(group);
		sleep(SLEEP_TIME_GROUP_MANIPULATION);
	}

	@Override
	public void authenticateAndVerifyClient(@NotNull AbstractUser user) {
		data.engineIterationDataClear();
		UserState userState = StateEngine.data.getState(user);
		ActionClient client = data.getClient(user);
		if (client == null) {
			throw new RuntimeException("User '" + user + "' must be added and connected before authenticating!");
		} else if (!client.isConnected()) {
			throw new RuntimeException("User '" + client + "' must be connected before authenticating!");
		} else if (userState.isCurrentlyAvailable()) {
			System.err.println("User " + client.toString() + " is already connected and authed if necessary!");
		} else {
			client.authenticate();
			System.out.println(client + " authenticated.");
		}

		System.out.println("Authenticated: " + user);

		sleep(SLEEP_TIME_AUTHENTICATE);
	}

	@Override
	public void onlineAddInput(@NotNull AbstractConnection input) {
		data.engineIterationDataClear();
		double t0 = System.currentTimeMillis();
		serverManager.getServerInstance(input.getServer()).getOnlineInputModule().add(input);
		double t1 = System.currentTimeMillis();
		double tTot = (t1 - t0) / 1000;
		serverManager.getServerInstance(input.getServer()).getOnlineInputModule().add(input);
		System.out.println("Added input \"" + input.getConsistentUniqueReadableIdentifier() + "\"(" + tTot + "s).");
		sleep(SLEEP_ADDREMOVE_INPUT);

		for (GroupProfiles group : input.getGroupSet().getGroups()) {
			serverManager.getServerInstance(input.getServer()).getOnlineInputModule().addInputToGroup(input.getConsistentUniqueReadableIdentifier(), group.name());
			sleep(SLEEP_TIME_GROUP_MANIPULATION);
		}
	}
	
	@Override
	public void onlineAddDataFeed(@NotNull AbstractConnection dataFeed) {
		data.engineIterationDataClear();
		double t0 = System.currentTimeMillis();
		serverManager.getServerInstance(dataFeed.getServer()).getOnlineInputModule().addDataFeed(dataFeed);
		double t1 = System.currentTimeMillis();
		double tTot = (t1 - t0) / 1000;
		serverManager.getServerInstance(dataFeed.getServer()).getOnlineInputModule().addDataFeed(dataFeed);
		System.out.println("Added input \"" + dataFeed.getConsistentUniqueReadableIdentifier() + "\"(" + tTot + "s).");
		sleep(SLEEP_ADDREMOVE_INPUT);

		for (GroupProfiles group : dataFeed.getGroupSet().getGroups()) {
			serverManager.getServerInstance(dataFeed.getServer()).getOnlineInputModule().addInputToGroup(dataFeed.getConsistentUniqueReadableIdentifier(), group.name());
			sleep(SLEEP_TIME_GROUP_MANIPULATION);
		}
	}

	@Override
	public void startServer(@NotNull AbstractServerProfile server, @NotNull String sessionIdentifier) {
		data.engineIterationDataClear();
		getRunnableInstanceAndBuildIfnecessary(server).startServer(sessionIdentifier, SLEEP_TIME_SERVER_START);
		// The sleep is missing here because it is already done in the above startServer call
	}

	@Override
	public void stopServers(@NotNull AbstractServerProfile... servers) {
		data.engineIterationDataClear();
		for (AbstractServerProfile server : servers) {
			if (serverManager.serverInstanceExists(server)) {
				serverManager.getServerInstance(server).stopServer();
				sleep(SLEEP_TIME_SERVER_STOP);
			}
		}
	}

	@Override
	public void engineFactoryReset() {
		serverManager.destroyAllServers();
		sleep(SLEEP_TIME_SERVER_STOP);
		data.clear();
	}

	@Override
	public void connectClientAndSendMessage(boolean doAuthIfNecessary, @NotNull AbstractUser user, @NotNull AbstractUser... targetUsers) {
		connectClientAndSendMessage(doAuthIfNecessary, DefaultSendingUserIdentification, user, targetUsers);
	}

	@Override
	public void connectClientAndSendMessage(boolean doAuthIfNecessary, @NotNull UserIdentificationData providedSenderData, @NotNull AbstractUser sendingUser, @NotNull AbstractUser... targetUsers) {
		connectClientAndSendMessage(doAuthIfNecessary, providedSenderData, sendingUser, DefaultReceivingUserIdentification, targetUsers);
	}

	@Override
	public void connectClientAndSendMessage(boolean doAuthIfNecessary, @NotNull UserIdentificationData providedSenderData, @NotNull AbstractUser sendingUser, @NotNull UserIdentificationData providedRecipientData, @NotNull AbstractUser... targetUsers) {
		data.engineIterationDataClear();
		String message = CotGenerator.createLatestSAMessage(providedSenderData, sendingUser, providedRecipientData, false, null, targetUsers).asXML();

		connectClientAndSendData(doAuthIfNecessary, sendingUser, message);

		if (doAuthIfNecessary && sendingUser.getConnection().requiresAuthentication()) {
			System.out.println("Connected and Authenticated With Message: " + sendingUser);
		} else {
			System.out.println("Connected with message: " + sendingUser);
		}

		sleep(SLEEP_TIME_CONNECT + SLEEP_TIME_SEND_MESSAGE + (doAuthIfNecessary ? SLEEP_TIME_AUTHENTICATE : 0));
	}

	@Override
	public void offlineAddSubscriptionFromInputToServer(@NotNull AbstractConnection targetInput, @NotNull AbstractServerProfile serverProvidingSubscription) {
		data.engineIterationDataClear();
		AbstractServerProfile targetServer = targetInput.getServer();
		serverManager.getServerInstance(targetServer).getOfflineConfigModule().addInput(targetInput.getConfigInput());
		serverManager.getServerInstance(serverProvidingSubscription).getOfflineConfigModule().addStaticSubscription(targetInput.generateMatchingStaticSubscription());
		System.out.println("Static subsciption added to server '" + serverProvidingSubscription + "' for input '" + targetInput + "'.");
	}
	
	@Override
	public void offlineAddSubscriptionFromDataFeedToServer(@NotNull AbstractConnection targetDataFeed, @NotNull AbstractServerProfile serverProvidingSubscription) {
		data.engineIterationDataClear();
		AbstractServerProfile targetServer = targetDataFeed.getServer();
		serverManager.getServerInstance(targetServer).getOfflineConfigModule().addDataFeed(targetDataFeed.getConfigDataFeed());
		serverManager.getServerInstance(serverProvidingSubscription).getOfflineConfigModule().addStaticSubscription(targetDataFeed.generateMatchingStaticSubscription());
		System.out.println("Static subsciption added to server '" + serverProvidingSubscription + "' for data feed '" + targetDataFeed + "'.");
	}

	@Override
	public void offlineFederateServers(boolean useV1Federation, boolean useV2Federation, @NotNull AbstractServerProfile... serversToFederate) {
		data.engineIterationDataClear();
		for (AbstractServerProfile serverToFederate : serversToFederate) {
			getRunnableInstanceAndBuildIfnecessary(serverToFederate).getOfflineConfigModule().enableFederationServer(useV1Federation, useV2Federation);
		}
	}

	@Override
	public void offlineAddOutboundFederateConnection(boolean useV2Federation, @NotNull AbstractServerProfile sourceServer, @NotNull AbstractServerProfile targetServer) {
		data.engineIterationDataClear();
		if (StateEngine.data.getState(sourceServer).federation.isOutgoingConnection(targetServer)) {
			throw new RuntimeException("Outbound connection from '" + targetServer.getConsistentUniqueReadableIdentifier() + "' to '" + sourceServer.getConsistentUniqueReadableIdentifier() + "' already exists! Adding a reverse one will cause message duplication or other unforseen issues!");
		}

		serverManager.getServerInstance(sourceServer).getOfflineConfigModule().addFederationOutgoing(useV2Federation, targetServer);
	}

	@Override
	public void offlineAddFederate(@NotNull AbstractServerProfile federatedServer, @NotNull AbstractServerProfile federate) {
		data.engineIterationDataClear();
		serverManager.getServerInstance(federatedServer).getOfflineConfigModule().addFederate(federate);
	}

	@Override
	public void offlineAddOutboundFederateGroup(@NotNull AbstractServerProfile federatedServer, @NotNull AbstractServerProfile federate, @NotNull String outboundGroupIdentifier) {
		data.engineIterationDataClear();
		serverManager.getServerInstance(federatedServer).getOfflineConfigModule().addFederateOutboundGroup(federate, outboundGroupIdentifier);

	}

	@Override
	public void offlineAddInboundFederateGroup(@NotNull AbstractServerProfile federatedServer, @NotNull AbstractServerProfile federate, @NotNull String inboundGroupIdentifier) {
		data.engineIterationDataClear();
		serverManager.getServerInstance(federatedServer).getOfflineConfigModule().addFederateInboundGroup(federate, inboundGroupIdentifier);
	}

	@Override
	public void onlineAddUser(@NotNull AbstractUser user) {
		data.engineIterationDataClear();
		if (user.getConnection().getAuthType() == AuthType.FILE && user.getUserName() != null && user.getPassword() != null) {

			OnlineFileAuthModule module = serverManager.getServerInstance(user.getServer()).getOnlineFileAuthModule();
			module.addOrUpdateUser(user.getUserName(), user.getPassword());

			Path certPath = user.getCertPublicPemPath();
			if (certPath != null) {
				try {
					String fingerprint = SSLHelper.getUserFingerprintIfAvailable(user);
					if (fingerprint != null) {
						module.setUserFingerprint(user.getUserName(), fingerprint);
					}
				} catch (CertificateException | FileNotFoundException e) {
					throw new RuntimeException(e);
				}
			}

			for (GroupProfiles group : user.getDefinedGroupSet().getGroups()) {
				module.addUsersToGroup(group.name(), user.getUserName());
			}
		}

		System.out.println("Added user '" + user.getConsistentUniqueReadableIdentifier() + "'");
	}

	@Override
	public void onlineRemoveUsers(@NotNull AbstractServerProfile server, @NotNull MutableUser... users) {
		data.engineIterationDataClear();
		serverManager.getServerInstance(server).getOnlineFileAuthModule().removeUsers(getUserNames(users));

		// TODO: There is a delay to get around a hack to get around an intentional delay needed since removal of a user on the server may result in the removal of message routing information before the message has been handled.
		try {
			Thread.sleep(8000);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		System.out.println(toString(users) + " have been removed.");

		sleep(SLEEP_TIME_GROUP_MANIPULATION);
	}

	@Override
	public void onlineAddUsersToGroup(@NotNull AbstractServerProfile server, @NotNull GroupProfiles group, @NotNull MutableUser... users) {
		data.engineIterationDataClear();
		for (MutableUser user : users) {
			user.addToGroup(group);
		}

		serverManager.getServerInstance(server).getOnlineFileAuthModule().addUsersToGroup(group.name(), getUserNames(users));

		System.out.println(toString(users) + " have been added to the group '" + group.name() + "'");

		sleep(SLEEP_TIME_GROUP_MANIPULATION);
	}

	@Override
	public void onlineRemoveUsersFromGroup(@NotNull AbstractServerProfile server, @NotNull GroupProfiles group, @NotNull MutableUser... users) {
		data.engineIterationDataClear();
		for (MutableUser user : users) {
			user.removeFromGroup(group);
		}

		serverManager.getServerInstance(server).getOnlineFileAuthModule().removeUsersFromGroup(group.name(), getUserNames(users));

		System.out.println(toString(users) + " have been removed from the group '" + group.name() + "'");

		sleep(SLEEP_TIME_GROUP_MANIPULATION);
	}

	@Override
	public void onlineUpdateUserPassword(@NotNull AbstractServerProfile server, @NotNull MutableUser user, @NotNull String userPassword) {
		data.engineIterationDataClear();
		serverManager.getServerInstance(server).getOnlineFileAuthModule().addOrUpdateUser(user.getUserName(), userPassword);

		user.invalidatePassword(userPassword);

		System.out.println(user.getConsistentUniqueReadableIdentifier() + "'s server password has been changed and will continue using the old one.");

		sleep(SLEEP_TIME_GROUP_MANIPULATION);
	}

	@Override
	public synchronized void offlineAddUsersAndConnectionsIfNecessary(@NotNull AbstractUser... users) {
		data.engineIterationDataClear();
		for (AbstractUser user : users) {

			AbstractRunnableServer server = getRunnableInstanceAndBuildIfnecessary(user.getServer());
			OfflineConfigModule offlineConfigModule = server.getOfflineConfigModule();
			AbstractConnection connection = user.getConnection();


			offlineConfigModule.addConnectionIfNecessary(connection);

			String username = user.getUserName();
			String password = user.getPassword();
			try {
				String fingerprint = SSLHelper.getUserFingerprintIfAvailable(user);

				if (connection.getAuthType() == AuthType.FILE && (username != null && password != null) || fingerprint != null) {
					server.getOfflineFileAuthModule().addUpdateUser(username, password, fingerprint, user.getBaseGroupSetAccess().groupSet);
				}
			} catch (CertificateException | FileNotFoundException e) {
				throw new RuntimeException(e);
			}

//        if (connection.getProtocol().isTLS()) {
//            server.getOfflineConfigModule().setSSLSecuritySettings();
//        }

// TODO: Check for validity of getting input stuff from subscriptions
		}

	}

	/**
	 * Private so that actual tests cannot skip the latestSA verification. Skippable internally for when several clients are connected at once to minimize wait time
	 *
	 * @param doAuthIfNecessary Do auth if necessary?
	 * @param user              The user
	 * @param xmlData           The xml data to send, if any
	 */
	private synchronized void connectClientAndSendData(boolean doAuthIfNecessary, AbstractUser user, String xmlData) {
		UserState state = StateEngine.data.getState(user);
		ActionClient client = data.getClient(user);

//        Map<UserInterface, List<String>> latestSaCountMap = generatedExpectedLatestSASendersOnConnect(doAuthIfNecessary, user);
//        client.setExpectations(latestSaCountMap.get(user), false);

		if (client == null) {
			throw new RuntimeException("User '" + user + "' must be added to the test engine before connecting!");
		} else if (doAuthIfNecessary && state.isCurrentlyAvailable()) {
			System.err.println("User " + client.toString() + " is already connected and authed if necessary!");
		} else if (client.isConnected()) {
			System.err.println("User " + client.toString() + " is already connected!");
		} else {
			client.connect(doAuthIfNecessary, xmlData);
		}

//        if (verifyLatestSA) {
//            sleep(SLEEP_TIME_CONNECT + (doAuthIfNecessary ? SLEEP_TIME_AUTHENTICATE : 0));
//            client.checkAndClearExpectations("latestSA");
//        }
	}

	private String[] getUserNames(@NotNull MutableUser... users) {
		String[] userNames = new String[users.length];

		for (int i = 0; i < users.length; i++) {
			userNames[i] = users[i].getUserName();
		}
		return userNames;
	}


	public String fileAdd(@NotNull AbstractUser user, @NotNull String name, @NotNull byte[] fileData) {
		data.engineIterationDataClear();
		ActionClient client = data.getClient(user);
		ResponseWrapper<EnterpriseSyncUploadResponse> response = client.mission.fileUpload(name, fileData);
		client.callResponse = response;
		return response.body.Hash;
	}

	public void fileDelete(@NotNull AbstractUser user, @NotNull String hash) {
		data.engineIterationDataClear();
		ActionClient client = data.getClient(user);
		client.callResponse = client.mission.fileDelete(hash);
	}

	public void fileDownload(@NotNull AbstractUser user, @NotNull String hash) {
		data.engineIterationDataClear();
		ActionClient client = data.getClient(user);
		client.callResponse = client.mission.fileDownload(hash);
	}


	@Override
	public void missionDetailsGet(@NotNull AbstractUser user) {
		data.engineIterationDataClear();
		ActionClient client = data.getClient(user);
		client.callResponse = client.mission.getAllMissions();;
		client.callResponse.setMissionComparisonHintIfPossible(user.getConsistentUniqueReadableIdentifier() + "getMissionsResponse");
	}

	@Override
	public void missionDetailsGetByName(@NotNull String missionName, @NotNull AbstractUser user) {
		data.engineIterationDataClear();
			ActionClient client = data.getClient(user);
			client.callResponse = client.mission.getMissionByName(missionName, StateEngine.data.getMissionState(missionName).getPassword());
			client.callResponse.setMissionComparisonHintIfPossible(user.getConsistentUniqueReadableIdentifier() + "getMissionResponse");
	}

	@Override
	public void missionAddResource(@NotNull AbstractUser missionOwner, @NotNull String missionName, @NotNull String dataUploadHash) {
		data.engineIterationDataClear();
		String clientIdentifier = missionOwner.getConsistentUniqueReadableIdentifier();
		ActionClient client = data.getClient(missionOwner);

		client.callResponse = client.mission.addMissionContents(missionName,new PutMissionContents().addHashes(dataUploadHash), missionOwner.getCotUid());
		client.callResponse.setMissionComparisonHintIfPossible(clientIdentifier + "addResourceResponse");

		client.verificationCallResponse = client.mission.getMissionByName(missionName, StateEngine.data.getMissionState(missionName).getPassword());
		client.verificationCallResponse.setMissionComparisonHintIfPossible(clientIdentifier + "getMissionResponse");
	}

	@Override
	public void missionRemoveResource(@NotNull AbstractUser user, @NotNull String missionName, @NotNull String dataUploadHash) {
		data.engineIterationDataClear();
		ActionClient client = data.getClient(user);
		String subscriptionToken = StateEngine.data.getMissionState(missionName).getSubscriberToken(user);
		client.callResponse = client.mission.deleteMissionHash(missionName, dataUploadHash, user.getCotUid(), subscriptionToken);
		client.callResponse.setMissionComparisonHintIfPossible("_removeMissionResourceCallback");

		client.verificationCallResponse = client.mission.getMissionByName(missionName, StateEngine.data.getMissionState(missionName).getPassword());
		client.verificationCallResponse.setMissionComparisonHintIfPossible(user.getConsistentUniqueReadableIdentifier() + "_getMission");

		if (TestExceptions.MISSION_IGNORE_GROUPS_MISSING_IN_ADD_REMOVE_RESPONSES) {
			client.callResponse.overrideMissionGroupsIfPossible(client.verificationCallResponse);
		}
	}

	@Override
	public void missionAdd(@NotNull AbstractUser apiUser, @NotNull String missionName, @Nullable GroupSetProfiles groupProfile, @Nullable MissionModels.MissionUserRole userRole) {
		data.engineIterationDataClear();
		ActionClient ownerClient = data.getClient(apiUser);
		if (groupProfile != null && groupProfile.groupSet != null && groupProfile.groupSet.size() > 0) {
			String[] groups = groupProfile.groupSet.toArray(new String[0]);
			ownerClient.callResponse = ownerClient.mission.addMission(missionName, userRole, null, groups);
			System.err.println(gson.toJson(ownerClient.callResponse));
			ownerClient.callResponse.setMissionComparisonHintIfPossible(apiUser.getConsistentUniqueReadableIdentifier() + "_addMissionResponse");
			if (TestExceptions.MISSION_IGNORE_GROUPS_MISSING_IN_ADD_REMOVE_RESPONSES) {
				ownerClient.callResponse.overrideMissionGroupsIfPossible(groupProfile.groupSet);
			}
		} else {
			ownerClient.callResponse = ownerClient.mission.addMission(missionName, userRole, null);
			if (TestExceptions.MISSION_IGNORE_GROUPS_MISSING_IN_ADD_REMOVE_RESPONSES) {
				ownerClient.callResponse.overrideMissionGroupsIfPossible(GroupSetProfiles.Set_Anon.groupSet);
			}
		}

//		// TODO Missions: This needs to be tested via CoT as well!
//		for (ActionClient client : data.getAllClients()) {
//			String clientIdentifier = client.getProfile().getConsistentUniqueReadableIdentifier();
//			client.retrievedMission = client.mission.getMissionByName(missionName);
//			client.retrievedMission.setComparisonHint(clientIdentifier + "getMissionResponse");
//
//			if (client == ownerClient && TestExceptions.IGNORE_GROUPS_MISSING_IN_ADD_MISSION_RESPONSE) {
//				ownerClient.callReturnedMission.overrideGroups(ownerClient.retrievedMission.getGroups());
//			}
//
//			client.retrievedMissions.putAll(client.mission.getMissions());
//			for (Mission clientMission : client.retrievedMissions.values()) {
//				clientMission.setComparisonHint(clientIdentifier + "getMissionsResponse");
//			}
//		}
	}

	@Override
	public void missionSubscribe(@NotNull AbstractUser missionOwner, @NotNull String missionName, @NotNull AbstractUser user) {
		data.engineIterationDataClear();
		ActionClient ownerClient = data.getClient(missionOwner);
		ownerClient.callResponse =  ownerClient.mission.createMissionSubscription(missionName, user.getCotUid());
	}

	@Override
	public void missionDelete(@NotNull AbstractUser user, @NotNull String missionName) {
		data.engineIterationDataClear();
		ActionClient client = data.getClient(user);
		client.callResponse = client.mission.deleteMission(missionName);
		client.callResponse.setMissionComparisonHintIfPossible("_deleteMissionCallback");
		client.verificationCallResponse = client.mission.getMissionByName(missionName, StateEngine.data.getMissionState(missionName).getPassword());
		client.verificationCallResponse.setMissionComparisonHintIfPossible(user.getConsistentUniqueReadableIdentifier() + "_getMission");
	}

	@Override
	public void missionDeepDelete(@NotNull AbstractUser user, @NotNull String missionName) {
		data.engineIterationDataClear();
		ActionClient client = data.getClient(user);
		String subscriptionToken = StateEngine.data.getMissionState(missionName).getSubscriberToken(user);
		client.callResponse = client.mission.deepDeleteMission(missionName, subscriptionToken);
		client.callResponse.setMissionComparisonHintIfPossible("_deepDeleteMissionCallback");
		client.verificationCallResponse = client.mission.getMissionByName(missionName, StateEngine.data.getMissionState(missionName).getPassword());
		client.verificationCallResponse.setMissionComparisonHintIfPossible(user.getConsistentUniqueReadableIdentifier() + "_getMission");
	}

	@Override
	public void missionSetUserRole(@NotNull AbstractUser apiUser, @NotNull String missionName, @NotNull AbstractUser targetUser, @Nullable MissionUserRole userRole) {
		data.engineIterationDataClear();
		ActionClient apiClient = data.getClient(apiUser);
		SubscriptionData newSubscriptionData = StateEngine.data.getMissionState(missionName).cloneUserSubscriptionWithNewRole(targetUser, userRole);
		String subscriptionToken = StateEngine.data.getMissionState(missionName).getSubscriberToken(apiUser);
		apiClient.callResponse = apiClient.mission.setMissionSubscriptionRole(missionName, newSubscriptionData, subscriptionToken);
		apiClient.verificationCallResponse = apiClient.mission.getMissionSubscriptions(missionName);
	}

	@Override
	public void missionGetChanges(@NotNull AbstractUser user, @NotNull String missionName) {
		data.engineIterationDataClear();
		ActionClient client = data.getClient(user);
		ResponseWrapper callResponse = client.mission.getMissionChanges(missionName);
		System.err.println(gson.toJson(callResponse.body));
		client.callResponse = callResponse;
	}

	@Override
	public void missionSetKeywords(@NotNull AbstractUser user, @NotNull String missionName, @NotNull String... keywords) {
		data.engineIterationDataClear();
		ActionClient client = data.getClient(user);
		String subscriptionToken = StateEngine.data.getMissionState(missionName).getSubscriberToken(user);
		client.callResponse = client.mission.setMissionKeywords(missionName, subscriptionToken, keywords);
	}

	@Override
	public void missionClearKeywords(@NotNull AbstractUser user, @NotNull String missionName) {
		data.engineIterationDataClear();
		ActionClient client = data.getClient(user);
		String subscriptionToken = StateEngine.data.getMissionState(missionName).getSubscriberToken(user);
		client.callResponse = client.mission.clearMissionKeywords(missionName, subscriptionToken);
		client.verificationCallResponse = client.mission.getMissionByName(missionName, StateEngine.data.getMissionState(missionName).getPassword());
	}

	@Override
	public void missionSetPassword(@NotNull AbstractUser apiUser, @NotNull String missionName, @NotNull String password) {
		data.engineIterationDataClear();
		ActionClient client = data.getClient(apiUser);
		String subscriptionToken = StateEngine.data.getMissionState(missionName).getSubscriberToken(apiUser);
		client.callResponse = client.mission.setMissionPassword(missionName, password, apiUser.getCotUid(), subscriptionToken);
		client.verificationCallResponse = client.mission.getMissionByName(missionName, password);
	}
}
