package com.bbn.marti.test.shared.engines;

import com.bbn.marti.takcl.TAKCLCore;
import com.bbn.marti.takcl.TestExceptions;
import com.bbn.marti.takcl.TestLogger;
import com.bbn.marti.takcl.connectivity.AbstractRunnableServer;
import com.bbn.marti.takcl.connectivity.missions.MissionModels;
import com.bbn.marti.test.shared.data.GroupProfiles;
import com.bbn.marti.test.shared.data.GroupSetProfiles;
import com.bbn.marti.test.shared.data.connections.AbstractConnection;
import com.bbn.marti.test.shared.data.connections.MutableConnection;
import com.bbn.marti.test.shared.data.servers.AbstractServerProfile;
import com.bbn.marti.test.shared.data.users.AbstractUser;
import com.bbn.marti.test.shared.data.users.MutableUser;
import com.bbn.marti.test.shared.engines.state.StateEngine;
import com.bbn.marti.test.shared.engines.verification.VerificationEngine;
import com.bbn.marti.tests.Assert;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * Used to manage the overall client/server testing
 * Created on 10/12/15.
 */
public class TestEngine implements EngineInterface, Callable<String> {

	private boolean latestSAEnabled = false;

	private final VerificationEngine verificationEngine;
	private final ActionEngine actionEngine;
	private final StateEngine stateEngine;

	public TestEngine(@NotNull AbstractServerProfile... servers) {
		stateEngine = new StateEngine();
		verificationEngine = new VerificationEngine();
		actionEngine = new ActionEngine(servers);
	}

	public synchronized void setSleepMultiplier(double multiplier) {
		actionEngine.setSleepMultiplier(multiplier);
	}

	public synchronized void setSendValidationDelayMultiplier(double multiplier) {
		actionEngine.setSendValidationDelayMultiplier(multiplier);
	}

	/**
	 * If set to true, all tests will pause when the server start or shutdown instruction is sent, allowing the user to
	 * manually turn the server on and off (useful for debugging)
	 *
	 * @param runMode Whether or not to force the user to manually start/stop the server
	 */
	public synchronized void setControlMode(AbstractRunnableServer.RUNMODE runMode) {
		actionEngine.setControlMode(runMode);
	}

	public synchronized void setRemoteDebuggee(@Nullable Integer serverIdentifier) {
		actionEngine.setRemoteDebuggee(serverIdentifier);
	}

	public synchronized void connectClientsAndVerify(boolean doAuthIfNecessary, @NotNull AbstractUser... users) {
		for (AbstractUser user : users) {
			connectClientAndVerify(doAuthIfNecessary, user);
		}
	}

	public synchronized void authenticateAndVerifyClients(@NotNull AbstractUser... users) {
		for (AbstractUser user : users) {
			authenticateAndVerifyClient(user);
		}
	}

	@Override
	/**
	 * Expectation that all clients will connect successfully
	 *
	 * @param doAuthIfNecessary
	 * @param users
	 */
	public synchronized void connectClientAndVerify(boolean doAuthIfNecessary, @NotNull AbstractUser users) {
		TestLogger.executeEngineCommand("connectClientAndVerify");

		Integer networkVersion = users.getConnection().getProtocol().getCoreNetworkVersion();
		if (TestExceptions.FORCE_IMMEDIATE_AUTH) {
			doAuthIfNecessary = true;
		}
		actionEngine.connectClientAndVerify(doAuthIfNecessary, users);
		verificationEngine.connectClientAndVerify(doAuthIfNecessary, users);
		stateEngine.connectClientAndVerify(doAuthIfNecessary, users);
	}

	@Override
	public void connectClientAndSendMessage(boolean doAuthIfNecessary, @NotNull AbstractUser user, @NotNull AbstractUser... targetUsers) {
		TestLogger.executeEngineCommand("connectClientAndSendMessage");
		actionEngine.connectClientAndSendMessage(doAuthIfNecessary, user, targetUsers);
		verificationEngine.connectClientAndSendMessage(doAuthIfNecessary, user, targetUsers);
		stateEngine.connectClientAndSendMessage(doAuthIfNecessary, user, targetUsers);
	}

	@Override
	public void connectClientAndSendMessage(boolean doAuthIfNecessary, @NotNull UserIdentificationData providedSenderData, @NotNull AbstractUser sendingUser, @NotNull AbstractUser... targetUsers) {
		TestLogger.executeEngineCommand("connectClientAndSendMessage");
		actionEngine.connectClientAndSendMessage(doAuthIfNecessary, providedSenderData, sendingUser, targetUsers);
		verificationEngine.connectClientAndSendMessage(doAuthIfNecessary, providedSenderData, sendingUser, targetUsers);
		stateEngine.connectClientAndSendMessage(doAuthIfNecessary, providedSenderData, sendingUser, targetUsers);
	}

	@Override
	public void connectClientAndSendMessage(boolean doAuthIfNecessary, @NotNull UserIdentificationData providedSenderData, @NotNull AbstractUser sendingUser, @NotNull UserIdentificationData providedRecipientData, @NotNull AbstractUser... targetUsers) {
		TestLogger.executeEngineCommand("connectClientAndSendMessage");
		actionEngine.connectClientAndSendMessage(doAuthIfNecessary, providedSenderData, sendingUser, providedRecipientData, targetUsers);
		verificationEngine.connectClientAndSendMessage(doAuthIfNecessary, providedSenderData, sendingUser, providedRecipientData, targetUsers);
		stateEngine.connectClientAndSendMessage(doAuthIfNecessary, providedSenderData, sendingUser, providedRecipientData, targetUsers);
	}

	@Override
	public void offlineAddSubscriptionFromInputToServer(@NotNull AbstractConnection targetInput, @NotNull AbstractServerProfile serverProvidingSubscription) {
		if (!TAKCLCore.useRunningServer) {
			TestLogger.executeEngineCommand("offlineAddSubscriptionFromInputToServer");
			actionEngine.offlineAddSubscriptionFromInputToServer(targetInput, serverProvidingSubscription);
			verificationEngine.offlineAddSubscriptionFromInputToServer(targetInput, serverProvidingSubscription);
			stateEngine.offlineAddSubscriptionFromInputToServer(targetInput, serverProvidingSubscription);
		}
	}
	
	@Override
	public void offlineAddSubscriptionFromDataFeedToServer(@NotNull AbstractConnection targetInput, @NotNull AbstractServerProfile serverProvidingSubscription) {
		if (!TAKCLCore.useRunningServer) {
			TestLogger.executeEngineCommand("offlineAddSubscriptionFromInputToServer");
			actionEngine.offlineAddSubscriptionFromDataFeedToServer(targetInput, serverProvidingSubscription);
			verificationEngine.offlineAddSubscriptionFromDataFeedToServer(targetInput, serverProvidingSubscription);
			stateEngine.offlineAddSubscriptionFromDataFeedToServer(targetInput, serverProvidingSubscription);
		}
	}

	@Override
	public void offlineFederateServers(boolean useV1Federation, boolean useV2Federation, @NotNull AbstractServerProfile... serversToFederate) {
		if (!TAKCLCore.useRunningServer) {
			for (int i = 0; i < serversToFederate.length; i++) {
				TestLogger.executeEngineCommand("offlineFederateServers");
			}
			actionEngine.offlineFederateServers(useV1Federation, useV2Federation, serversToFederate);
			verificationEngine.offlineFederateServers(useV1Federation, useV2Federation, serversToFederate);
			stateEngine.offlineFederateServers(useV1Federation, useV2Federation, serversToFederate);
		}
	}

	@Override
	public void offlineAddOutboundFederateConnection(boolean useV2Federation, @NotNull AbstractServerProfile sourceServer, @NotNull AbstractServerProfile targetServer) {
		if (!TAKCLCore.useRunningServer) {
			TestLogger.executeEngineCommand("offlineAddOutboundFederateConnection");
			actionEngine.offlineAddOutboundFederateConnection(useV2Federation, sourceServer, targetServer);
			verificationEngine.offlineAddOutboundFederateConnection(useV2Federation, sourceServer, targetServer);
			stateEngine.offlineAddOutboundFederateConnection(useV2Federation, sourceServer, targetServer);
		}
	}

	@Override
	public void offlineAddFederate(@NotNull AbstractServerProfile federatedServer, @NotNull AbstractServerProfile federate) {
		if (!TAKCLCore.useRunningServer) {
			TestLogger.executeEngineCommand("offlineAddFederate");
			actionEngine.offlineAddFederate(federatedServer, federate);
			verificationEngine.offlineAddFederate(federatedServer, federate);
			stateEngine.offlineAddFederate(federatedServer, federate);
		}
	}

	@Override
	public void offlineAddOutboundFederateGroup(@NotNull AbstractServerProfile federatedServer, @NotNull AbstractServerProfile federate, @NotNull String outboundGroupIdentifier) {
		if (!TAKCLCore.useRunningServer) {
			TestLogger.executeEngineCommand("offlineAddOutboundFederateGroup");
			actionEngine.offlineAddOutboundFederateGroup(federatedServer, federate, outboundGroupIdentifier);
			verificationEngine.offlineAddOutboundFederateGroup(federatedServer, federate, outboundGroupIdentifier);
			stateEngine.offlineAddOutboundFederateGroup(federatedServer, federate, outboundGroupIdentifier);
		}
	}

	@Override
	public void offlineAddInboundFederateGroup(@NotNull AbstractServerProfile federatedServer, @NotNull AbstractServerProfile federate, @NotNull String inboundGroupIdentifier) {
		if (!TAKCLCore.useRunningServer) {
			TestLogger.executeEngineCommand("offlineAddInboundFederateGroup");
			actionEngine.offlineAddInboundFederateGroup(federatedServer, federate, inboundGroupIdentifier);
			verificationEngine.offlineAddInboundFederateGroup(federatedServer, federate, inboundGroupIdentifier);
			stateEngine.offlineAddInboundFederateGroup(federatedServer, federate, inboundGroupIdentifier);
		}
	}

	@Override
	public synchronized void disconnectClientAndVerify(@NotNull AbstractUser disconnectingUser) {
		TestLogger.executeEngineCommand("disconnectClientAndVerify");
		actionEngine.disconnectClientAndVerify(disconnectingUser);
		verificationEngine.disconnectClientAndVerify(disconnectingUser);
		stateEngine.disconnectClientAndVerify(disconnectingUser);
	}

	@Override
	public synchronized void authenticateAndVerifyClient(@NotNull AbstractUser user) {
		Integer networkVersion = user.getConnection().getProtocol().getCoreNetworkVersion();
		if (!TestExceptions.FORCE_IMMEDIATE_AUTH) {
			TestLogger.executeEngineCommand("authenticateAndVerifyClient");
			actionEngine.authenticateAndVerifyClient(user);
			verificationEngine.authenticateAndVerifyClient(user);
			stateEngine.authenticateAndVerifyClient(user);
		}
	}

	@Override
	public synchronized void onlineRemoveInputAndVerify(@NotNull AbstractConnection input) {
		TestLogger.executeEngineCommand("onlineRemoveInputAndVerify");
		actionEngine.onlineRemoveInputAndVerify(input);
		verificationEngine.onlineRemoveInputAndVerify(input);
		stateEngine.onlineRemoveInputAndVerify(input);
	}
	
	@Override
	public synchronized void onlineRemoveDataFeedAndVerify(@NotNull AbstractConnection dataFeed) {
		TestLogger.executeEngineCommand("onlineRemoveDataFeedAndVerify");
		actionEngine.onlineRemoveDataFeedAndVerify(dataFeed);
		verificationEngine.onlineRemoveDataFeedAndVerify(dataFeed);
		stateEngine.onlineRemoveDataFeedAndVerify(dataFeed);
	}

	@Override
	public synchronized void onlineAddInput(@NotNull AbstractConnection input) {
		TestLogger.executeEngineCommand("onlineAddInput");
		actionEngine.onlineAddInput(input);
		verificationEngine.onlineAddInput(input);
		stateEngine.onlineAddInput(input);
	}

	@Override
	public synchronized void onlineAddDataFeed(@NotNull AbstractConnection input) {
		TestLogger.executeEngineCommand("onlineAddDataFeed");
		actionEngine.onlineAddDataFeed(input);
		verificationEngine.onlineAddDataFeed(input);
		stateEngine.onlineAddDataFeed(input);
	}
	

	@Override
	/**
	 * This will add the inputs if necessary and enable file auth if necessary
	 *
	 * @param user
	 * @param input
	 */
	public synchronized void offlineAddUsersAndConnectionsIfNecessary(@NotNull AbstractUser... users) {
		if (!TAKCLCore.useRunningServer) {
			TestLogger.executeEngineCommand("offlineAddUsersAndConnectionsIfNecessary");
			actionEngine.offlineAddUsersAndConnectionsIfNecessary(users);
			verificationEngine.offlineAddUsersAndConnectionsIfNecessary(users);
			stateEngine.offlineAddUsersAndConnectionsIfNecessary(users);
		}
	}


//    @Override
//    public synchronized void offlineEnableLatestSA(boolean enabled) {
//        System.out.println("Offline: LatestSA " + (enabled ? "enabled." : "disabled."));
//        actionEngine.offlineEnableLatestSA(enabled);
//        stateManager.setLatestSAEnabled(enabled);
////        stateManager.offlineEnableLatestSA(enabled);
//        verificationEngine.offlineEnableLatestSA(enabled);
//        verificationPrepEngine.offlineEnableLatestSA(enabled);
//    }


	@Override
	public void onlineAddUser(@NotNull AbstractUser user) {
		TestLogger.executeEngineCommand("onlineAddUser");
		actionEngine.onlineAddUser(user);
		verificationEngine.onlineAddUser(user);
		stateEngine.onlineAddUser(user);
	}

	@Override
	public void onlineRemoveUsers(@NotNull AbstractServerProfile server, @NotNull MutableUser... users) {
		TestLogger.executeEngineCommand("onlineRemoveUsers");
		actionEngine.onlineRemoveUsers(server, users);
		verificationEngine.onlineRemoveUsers(server, users);
		stateEngine.onlineRemoveUsers(server, users);
	}

	@Override
	public void onlineAddUsersToGroup(@NotNull AbstractServerProfile server, @NotNull GroupProfiles group, @NotNull MutableUser... users) {
		TestLogger.executeEngineCommand("onlineAddUsersToGroup");
		actionEngine.onlineAddUsersToGroup(server, group, users);
		verificationEngine.onlineAddUsersToGroup(server, group, users);
		stateEngine.onlineAddUsersToGroup(server, group, users);
	}

	@Override
	public void onlineRemoveUsersFromGroup(@NotNull AbstractServerProfile server, @NotNull GroupProfiles group, @NotNull MutableUser... users) {
		TestLogger.executeEngineCommand("onlineRemoveUsersFromGroup");
		actionEngine.onlineRemoveUsersFromGroup(server, group, users);
		verificationEngine.onlineRemoveUsersFromGroup(server, group, users);
		stateEngine.onlineRemoveUsersFromGroup(server, group, users);
	}

	@Override
	public void onlineUpdateUserPassword(@NotNull AbstractServerProfile server, @NotNull MutableUser user, @NotNull String userPassword) {
		TestLogger.executeEngineCommand("onlineUpdateUserPassword");
		actionEngine.onlineUpdateUserPassword(server, user, userPassword);
		verificationEngine.onlineUpdateUserPassword(server, user, userPassword);
		stateEngine.onlineUpdateUserPassword(server, user, userPassword);
	}

	@Override
	public void updateLocalUserPassowrd(@NotNull MutableUser user) {
		TestLogger.executeEngineCommand("updateLocalUserPassowrd");
		actionEngine.updateLocalUserPassowrd(user);
		verificationEngine.updateLocalUserPassowrd(user);
		stateEngine.updateLocalUserPassowrd(user);
	}

	@Override
	public void onlineAddInputToGroup(@NotNull MutableConnection input, @NotNull GroupProfiles group) {
		TestLogger.executeEngineCommand("onlineAddInputToGroup");
		actionEngine.onlineAddInputToGroup(input, group);
		verificationEngine.onlineAddInputToGroup(input, group);
		stateEngine.onlineAddInputToGroup(input, group);
	}

	@Override
	public void onlineRemoveInputFromGroup(@NotNull MutableConnection input, @NotNull GroupProfiles group) {
		TestLogger.executeEngineCommand("onlineRemoveInputFromGroup");
		actionEngine.onlineRemoveInputFromGroup(input, group);
		verificationEngine.onlineRemoveInputFromGroup(input, group);
		stateEngine.onlineRemoveInputFromGroup(input, group);
	}

	@Override
	public void attemptSendFromUserAndVerify(@NotNull AbstractUser sendingUser, @NotNull AbstractUser... targetUsers) {
		TestLogger.executeEngineCommand("attemptSendFromUserAndVerify");
		actionEngine.attemptSendFromUserAndVerify(sendingUser, targetUsers);
		verificationEngine.attemptSendFromUserAndVerify(sendingUser, targetUsers);
		stateEngine.attemptSendFromUserAndVerify(sendingUser, targetUsers);
	}

	@Override
	public void attemptSendFromUserAndVerify(@NotNull AbstractUser sendingUser, @NotNull String missionName) {
		TestLogger.executeEngineCommand("attemptSendFromUserAndVerify");
		actionEngine.attemptSendFromUserAndVerify(sendingUser, missionName);
		verificationEngine.attemptSendFromUserAndVerify(sendingUser, missionName);
		stateEngine.attemptSendFromUserAndVerify(sendingUser, missionName);
	}

	@Override
	public void attemptSendFromUserAndVerify(@NotNull UserIdentificationData senderIdentification, @NotNull AbstractUser sendingUser, @NotNull AbstractUser... targetUsers) {
		TestLogger.executeEngineCommand("attemptSendFromUserAndVerify");
		actionEngine.attemptSendFromUserAndVerify(senderIdentification, sendingUser, targetUsers);
		verificationEngine.attemptSendFromUserAndVerify(senderIdentification, sendingUser, targetUsers);
		stateEngine.attemptSendFromUserAndVerify(senderIdentification, sendingUser, targetUsers);
	}

	@Override
	public void attemptSendFromUserAndVerify(
			@NotNull UserIdentificationData senderIdentification, @NotNull AbstractUser sendingUser,
			@NotNull UserIdentificationData recipientIdentification, @NotNull AbstractUser... targetUsers) {
		TestLogger.executeEngineCommand("attemptSendFromUserAndVerify");
		actionEngine.attemptSendFromUserAndVerify(senderIdentification, sendingUser, recipientIdentification, targetUsers);
		verificationEngine.attemptSendFromUserAndVerify(senderIdentification, sendingUser, recipientIdentification, targetUsers);
		stateEngine.attemptSendFromUserAndVerify(senderIdentification, sendingUser, recipientIdentification, targetUsers);
	}

	@Override
	public void attemptSendFromUserAndVerify(
			@NotNull UserIdentificationData senderIdentification, @NotNull AbstractUser sendingUser,
			@NotNull UserIdentificationData recipientIdentification, @Nullable String missionName, @NotNull AbstractUser... targetUsers) {
		TestLogger.executeEngineCommand("attemptSendFromUserAndVerify");
		actionEngine.attemptSendFromUserAndVerify(senderIdentification, sendingUser, recipientIdentification, missionName, targetUsers);
		verificationEngine.attemptSendFromUserAndVerify(senderIdentification, sendingUser, recipientIdentification, missionName, targetUsers);
		stateEngine.attemptSendFromUserAndVerify(senderIdentification, sendingUser, recipientIdentification, missionName, targetUsers);
	}

	public List<AbstractUser> getUsersThatCanSend() {
		return StateEngine.data.getUsersThatCanCurrentlySend();
	}

	@Override
	public String call() throws Exception {
		wait();
		return null;
	}

	@Override
	public void offlineEnableLatestSA(boolean enabled, @NotNull AbstractServerProfile... servers) {
		if (!TAKCLCore.useRunningServer) {
			TestLogger.executeEngineCommand("offlineEnableLatestSA");
			System.out.println("Offline: LatestSA " + (enabled ? "enabled." : "disabled."));
			actionEngine.offlineEnableLatestSA(enabled, servers);
			verificationEngine.offlineEnableLatestSA(enabled, servers);
			stateEngine.offlineEnableLatestSA(enabled, servers);
		}
	}

	@Override
	public void startServer(@NotNull AbstractServerProfile server, @NotNull String sessionIdentifier) {
		TestLogger.executeEngineCommand("startServer");
		actionEngine.startServer(server, sessionIdentifier);
		verificationEngine.startServer(server, sessionIdentifier);
		stateEngine.startServer(server, sessionIdentifier);
	}

	@Override
	public void stopServers(@NotNull AbstractServerProfile... servers) {
		TestLogger.executeEngineCommand("stopServers");
		actionEngine.stopServers(servers);
		verificationEngine.stopServers(servers);
		stateEngine.stopServers(servers);
	}

	@Override
	public void engineFactoryReset() {
		actionEngine.engineFactoryReset();
		verificationEngine.engineFactoryReset();
		stateEngine.engineFactoryReset();
	}

	@Override
	public void missionDetailsGetByName(@Nullable String missionName, @NotNull AbstractUser user) {
		TestLogger.executeEngineCommand("missionDetailsGetByName");
		actionEngine.missionDetailsGetByName(missionName, user);
		verificationEngine.missionDetailsGetByName(missionName, user);
		stateEngine.missionDetailsGetByName(missionName, user);
	}

	@Override
	public void missionAddResource(@NotNull AbstractUser missionOwner, @NotNull String missionName, @NotNull String dataUploadHash) {
		TestLogger.executeEngineCommand("missionAddResource");
		actionEngine.missionAddResource(missionOwner, missionName, dataUploadHash);
		verificationEngine.missionAddResource(missionOwner, missionName, dataUploadHash);
		stateEngine.missionAddResource(missionOwner, missionName, dataUploadHash);

	}

	@Override
	public void missionRemoveResource(@NotNull AbstractUser user, @NotNull String missionName, @NotNull String dataUploadHash) {
		TestLogger.executeEngineCommand("missionRemoveResource");
		actionEngine.missionRemoveResource(user, missionName, dataUploadHash);
		verificationEngine.missionRemoveResource(user, missionName, dataUploadHash);
		stateEngine.missionRemoveResource(user, missionName, dataUploadHash);
	}

	@Override
	public void missionDetailsGet(@NotNull AbstractUser user) {
		TestLogger.executeEngineCommand("missionDetailsGet");
		actionEngine.missionDetailsGet(user);
		verificationEngine.missionDetailsGet(user);
		stateEngine.missionDetailsGet(user);
	}

	@Override
	public void missionAdd(@NotNull AbstractUser apiUser, @NotNull String missionName, @Nullable GroupSetProfiles groupSetProfiles, @Nullable MissionModels.MissionUserRole userRole) {
		TestLogger.executeEngineCommand("missionAdd");
		actionEngine.missionAdd(apiUser, missionName, groupSetProfiles, userRole);
		verificationEngine.missionAdd(apiUser, missionName, groupSetProfiles, userRole);
		stateEngine.missionAdd(apiUser, missionName, groupSetProfiles, userRole);
	}

	@Override
	public void missionSubscribe(@NotNull AbstractUser missionOwner, @NotNull String missionName, @NotNull AbstractUser user) {
		TestLogger.executeEngineCommand("missionSubscribe");
		actionEngine.missionSubscribe(missionOwner, missionName, user);
		verificationEngine.missionSubscribe(missionOwner, missionName, user);
		stateEngine.missionSubscribe(missionOwner, missionName, user);
	}

	@Override
	public void missionDelete(@NotNull AbstractUser user, @NotNull String missionName) {
		TestLogger.executeEngineCommand("missionDelete");
		actionEngine.missionDelete(user, missionName);
		verificationEngine.missionDelete(user, missionName);
		stateEngine.missionDelete(user, missionName);
	}

	@Override
	public void missionDeepDelete(@NotNull AbstractUser user, @NotNull String missionName) {
		TestLogger.executeEngineCommand("missionDeepDelete");
		actionEngine.missionDeepDelete(user, missionName);
		verificationEngine.missionDeepDelete(user, missionName);
		stateEngine.missionDeepDelete(user, missionName);
	}

	@Override
	public void missionSetUserRole(@NotNull AbstractUser apiUser, @NotNull String missionName, @NotNull AbstractUser targetUser, @Nullable MissionModels.MissionUserRole userRole) {
		TestLogger.executeEngineCommand("missionSetUserRole");
		actionEngine.missionSetUserRole(apiUser, missionName, targetUser, userRole);
		verificationEngine.missionSetUserRole(apiUser, missionName, targetUser, userRole);
		stateEngine.missionSetUserRole(apiUser, missionName, targetUser, userRole);
	}

	@Override
	public void missionGetChanges(@NotNull AbstractUser user, @NotNull String missionName) {
		TestLogger.executeEngineCommand("missionGetChanges");
		actionEngine.missionGetChanges(user, missionName);
		verificationEngine.missionGetChanges(user, missionName);
		stateEngine.missionGetChanges(user, missionName);
	}

	@Override
	public void missionSetKeywords(@NotNull AbstractUser user, @NotNull String missionName, @NotNull String... keywords) {
		TestLogger.executeEngineCommand("missionSetKeywords");
		actionEngine.missionSetKeywords(user, missionName, keywords);
		verificationEngine.missionSetKeywords(user, missionName, keywords);
		stateEngine.missionSetKeywords(user, missionName, keywords);
	}

	@Override
	public void missionClearKeywords(@NotNull AbstractUser user, @NotNull String missionName) {
		TestLogger.executeEngineCommand("missionClearKeywords");
		actionEngine.missionClearKeywords(user, missionName);
		verificationEngine.missionClearKeywords(user, missionName);
		stateEngine.missionClearKeywords(user, missionName);
	}

	@Override
	public String fileAdd(@NotNull AbstractUser user, @NotNull String name, @NotNull byte[] fileData) {
		TestLogger.executeEngineCommand("fileAdd");
		String result = actionEngine.fileAdd(user, name, fileData);
		String value = verificationEngine.fileAdd(user, name, fileData);
		Assert.assertEquals("Methods should return the same value for consistency!", result, value);
		value = stateEngine.fileAdd(user, name, fileData);
		Assert.assertEquals("Methods should return the same value for consistency!", result, value);
		return value;
	}

	@Override
	public void fileDelete(@NotNull AbstractUser user, @NotNull String hash) {
		TestLogger.executeEngineCommand("fileDelete");
		actionEngine.fileDelete(user, hash);
		verificationEngine.fileDelete(user, hash);
		stateEngine.fileDelete(user, hash);
	}

	@Override
	public void fileDownload(@NotNull AbstractUser user, @NotNull String hash) {
		TestLogger.executeEngineCommand("fileDownload");
		actionEngine.fileDownload(user, hash);
		verificationEngine.fileDownload(user, hash);
		stateEngine.fileDownload(user, hash);
	}

	@Override
	public void missionSetPassword(@NotNull AbstractUser apiUser, @NotNull String missionName, @NotNull String password) {
		TestLogger.executeEngineCommand("missionSetPassword");
		actionEngine.missionSetPassword(apiUser, missionName, password);
		verificationEngine.missionSetPassword(apiUser, missionName, password);
		stateEngine.missionSetPassword(apiUser, missionName, password);
	}
}
