package com.bbn.marti.test.shared.engines.state;

import com.bbn.marti.takcl.TestExceptions;
import com.bbn.marti.takcl.connectivity.missions.MissionModels;
import com.bbn.marti.test.shared.TestConnectivityState;
import com.bbn.marti.test.shared.data.GroupProfiles;
import com.bbn.marti.test.shared.data.GroupSetProfiles;
import com.bbn.marti.test.shared.data.connections.AbstractConnection;
import com.bbn.marti.test.shared.data.connections.MutableConnection;
import com.bbn.marti.test.shared.data.servers.AbstractServerProfile;
import com.bbn.marti.test.shared.data.users.AbstractUser;
import com.bbn.marti.test.shared.data.users.MutableUser;
import com.bbn.marti.test.shared.engines.ActionEngine;
import com.bbn.marti.test.shared.engines.EngineInterface;
import com.bbn.marti.test.shared.engines.UserIdentificationData;
import org.dom4j.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.TreeSet;

import static com.bbn.marti.takcl.connectivity.missions.MissionModels.*;

/**
 * Created on 9/7/16.
 */
public class StateEngine implements EngineInterface {

	public static final EnvironmentState data = EnvironmentState.instance;

	public StateEngine() {
	}

	@Override
	public void offlineAddUsersAndConnectionsIfNecessary(@NotNull AbstractUser... users) {
		for (AbstractUser user : users) {
			UserState us = data.getState(user);
			us.getServerState().setActiveInDeployment();
			((ConnectionState) us.getConnectionState()).setActiveInDeployment();
			onlineAddUser(user);
		}
		data.updateState();
	}

	@Override
	public void offlineEnableLatestSA(boolean enabled, @NotNull AbstractServerProfile... servers) {
		for (AbstractServerProfile server : servers) {
			data.getState(server).setLatestSaEnabled(enabled);
		}
		data.updateState();
	}

	@Override
	public void connectClientAndVerify(boolean doAuthIfNecessary, @NotNull AbstractUser users) {
//        data.clearTestIterationData();
		data.updateState();
	}

	@Override
	public void disconnectClientAndVerify(@NotNull AbstractUser disconnectingUser) {
//        data.clearTestIterationData();
		data.updateState();
	}

	@Override
	public void onlineRemoveInputAndVerify(@NotNull AbstractConnection input) {
		// TODO: Validate validate validate!! Check. Maybe add a thread for delayed checking?
		// As of 01/13/2016, Clients are no longer disconnected when an input is removed. This was necessary to resolve a significant memory leak. Similar check is also commented out in UnifiedExpectantClient CanCurrentlyReceive and CanCurrentlySend checks
//        if (!value && state.getConnection().getProtocol().canConnect() && client.isConnected() && state.getConnection().getProtocol().clientConnectionSeveredWithInputRemoval()) {
//            throw new RuntimeException("Client '" + toString() + "' is still connected even though the server has turned off the connection!");
//        }
		data.getState(input).setInactiveInDeployment();
//        data.clearTestIterationData();
		data.updateState();
	}
	
	@Override
	public void onlineRemoveDataFeedAndVerify(@NotNull AbstractConnection dataFeed) {
		// TODO: Validate validate validate!! Check. Maybe add a thread for delayed checking?
		data.getState(dataFeed).setInactiveInDeployment();
//        data.clearTestIterationData();
		data.updateState();
	}

	@Override
	public void attemptSendFromUserAndVerify(@NotNull AbstractUser sendingUser, @NotNull AbstractUser... targetUsers) {
		attemptSendFromUserAndVerify(UserIdentificationData.UID_AND_CALLSIGN, sendingUser, targetUsers);
	}

	@Override
	public void attemptSendFromUserAndVerify(@NotNull AbstractUser sendingUser, @NotNull String missionName) {
		attemptSendFromUserAndVerify(DefaultSendingUserIdentification, sendingUser, DefaultReceivingUserIdentification, missionName);

	}

	@Override
	public void attemptSendFromUserAndVerify(@NotNull UserIdentificationData senderIdentification, @NotNull AbstractUser sendingUser, @NotNull AbstractUser... targetUsers) {
		attemptSendFromUserAndVerify(senderIdentification, sendingUser, UserIdentificationData.UID_AND_CALLSIGN, targetUsers);
	}

	@Override
	public void attemptSendFromUserAndVerify(@NotNull UserIdentificationData senderIdentification, @NotNull AbstractUser sendingUser, @NotNull UserIdentificationData recipientIdentification, @NotNull AbstractUser... targetUsers) {
		attemptSendFromUserAndVerify(senderIdentification, sendingUser, recipientIdentification, null, targetUsers);
	}

	@Override
	public void attemptSendFromUserAndVerify(@NotNull UserIdentificationData senderIdentification, @NotNull AbstractUser sendingUser, @NotNull UserIdentificationData recipientIdentification, @Nullable String missionName, @NotNull AbstractUser... targetUsers) {
		if (missionName != null) {
			MissionState missionState = data.getMissionState(missionName);

			if (missionState.isSubscriber(sendingUser)) {
				Document sentCotMessage = ActionEngine.data.getState(sendingUser).getSentCotMessage();
				missionState.addSentCotMessage(sentCotMessage);
			}
		}
		data.updateKnownCallsignAndUidState();
		data.updateState();
	}

	@Override
	public void authenticateAndVerifyClient(@NotNull AbstractUser users) {
//        data.clearTestIterationData();
		data.updateState();
	}

	@Override
	public void onlineAddInput(@NotNull AbstractConnection input) {
		// TODO: Validate validate validate!! Check
		// As of 01/13/2016, Clients are no longer disconnected when an input is removed. This was necessary to resolve a significant memory leak. Similar check is also commented out in UnifiedExpectantClient CanCurrentlyReceive and CanCurrentlySend checks
//        if (!value && state.getConnection().getProtocol().canConnect() && client.isConnected() && state.getConnection().getProtocol().clientConnectionSeveredWithInputRemoval()) {
//            throw new RuntimeException("Client '" + toString() + "' is still connected even though the server has turned off the connection!");
//        }
		data.getState(input).setActiveInDeployment();
		data.updateState();
	}
	
	@Override
	public void onlineAddDataFeed(@NotNull AbstractConnection dataFeed) {
		// TODO: Validate validate validate!! Check
		data.getState(dataFeed).setActiveInDeployment();
		data.updateState();
	}

	@Override
	public void startServer(@NotNull AbstractServerProfile server, @NotNull String sessionIdentifier) {
		data.updateState();
	}

	@Override
	public void startServerWithStartupValidation(@NotNull AbstractServerProfile server, @NotNull String sessionIdentifier, boolean enablePluginManager, boolean enableRetentionService) {
		data.updateState();
	}

	@Override
	public void stopServers(@NotNull AbstractServerProfile... servers) {
//        data.resetState();
//        data.clearLatestSA(servers);
//        data.updateState();
		HashSet<AbstractServerProfile> serverSet = new HashSet<>(Arrays.asList(servers));
		for (ActionEngine.ActionClient client : ActionEngine.data.getAllClients()) {
			if (serverSet.contains(client.getProfile().getServer())) {
				UserState user = data.getState(client.getProfile());
				user.updateLatestSA(client.getLatestSA());
				user.updateConnectivityState(TestConnectivityState.Disconnected);
			}
		}
	}

	@Override
	public void engineFactoryReset() {
		data.factoryReset();
	}

	@Override
	public void connectClientAndSendMessage(boolean doAuthIfNecessary, @NotNull AbstractUser user, @NotNull AbstractUser... targetUsers) {
		connectClientAndSendMessage(doAuthIfNecessary, UserIdentificationData.UID_AND_CALLSIGN, user, targetUsers);
		data.updateState();
	}

	@Override
	public void connectClientAndSendMessage(boolean doAuthIfNecessary, @NotNull UserIdentificationData providedSenderData, @NotNull AbstractUser sendingUser, @NotNull AbstractUser... targetUsers) {
		connectClientAndSendMessage(doAuthIfNecessary, providedSenderData, sendingUser, UserIdentificationData.UID_AND_CALLSIGN, targetUsers);
		data.updateState();
	}

	@Override
	public void connectClientAndSendMessage(boolean doAuthIfNecessary, @NotNull UserIdentificationData providedSenderData, @NotNull AbstractUser sendingUser, @NotNull UserIdentificationData providedRecipientData, @NotNull AbstractUser... targetUsers) {
		data.updateKnownCallsignAndUidState();
//        data.clearTestIterationData();
	}

	@Override
	public void offlineAddSubscriptionFromInputToServer(@NotNull AbstractConnection targetInput, @NotNull AbstractServerProfile serverProvidingSubscription) {
		data.getState(serverProvidingSubscription).addConnectionSubscriptionTarget(targetInput);
		data.updateState();
	}
	
	@Override
	public void offlineAddSubscriptionFromDataFeedToServer(@NotNull AbstractConnection targetInput, @NotNull AbstractServerProfile serverProvidingSubscription) {
		data.getState(serverProvidingSubscription).addConnectionSubscriptionTarget(targetInput);
		data.updateState();
	}

	@Override
	public void offlineFederateServers(boolean useV1Federation, boolean useV2Federation, @NotNull AbstractServerProfile... serversToFederate) {
		for (AbstractServerProfile server : serversToFederate) {
			data.getState(server).federation.setFederated();
		}
		data.updateState();
	}

	@Override
	public void offlineAddOutboundFederateConnection(boolean useV2Federation, @NotNull AbstractServerProfile federatedServer, @NotNull AbstractServerProfile targetServer) {
		data.getState(federatedServer).federation.addOutgoingConnection(targetServer);
		data.updateState();
	}

	@Override
	public void offlineAddFederate(@NotNull AbstractServerProfile federatedServer, @NotNull AbstractServerProfile federate) {
		data.getState(federatedServer).federation.addFederate(federate);
		data.updateState();
	}

	@Override
	public void offlineAddOutboundFederateGroup(@NotNull AbstractServerProfile federatedServer, @NotNull AbstractServerProfile federate, @NotNull String outboundGroupIdentifier) {
		data.getState(federatedServer).federation.getFederateState(federate).addOutboundGroup(outboundGroupIdentifier);
		data.updateState();
	}

	@Override
	public void offlineAddInboundFederateGroup(@NotNull AbstractServerProfile federatedServer, @NotNull AbstractServerProfile federate, @NotNull String inboundGroupIdentifier) {
		data.getState(federatedServer).federation.getFederateState(federate).addInboundGroup(inboundGroupIdentifier);
		data.updateState();
	}

	@Override
	public void onlineAddUser(@NotNull AbstractUser user) {
		data.getState(user).setActiveInDeployment();
		data.updateState();
	}

	@Override
	public void onlineRemoveUsers(@NotNull AbstractServerProfile server, @NotNull MutableUser... users) {
		for (AbstractUser user : users) {
			data.getState(user).setInactiveInDeployment();
		}
		data.updateState();
	}

	@Override
	public void onlineAddUsersToGroup(@NotNull AbstractServerProfile server, @NotNull GroupProfiles group, @NotNull MutableUser... users) {
		data.updateState();

	}

	@Override
	public void onlineRemoveUsersFromGroup(@NotNull AbstractServerProfile server, @NotNull GroupProfiles group, @NotNull MutableUser... users) {
		data.updateState();

	}

	@Override
	public void onlineUpdateUserPassword(@NotNull AbstractServerProfile server, @NotNull MutableUser user, @NotNull String userPassword) {
		data.updateState();

	}

	@Override
	public void updateLocalUserPassowrd(@NotNull MutableUser user) {
		data.updateState();

	}

	@Override
	public void onlineAddInputToGroup(@NotNull MutableConnection input, @NotNull GroupProfiles group) {
		data.updateState();

	}

	@Override
	public void onlineRemoveInputFromGroup(@NotNull MutableConnection input, @NotNull GroupProfiles group) {
		data.updateState();
	}

	@Override
	public String fileAdd(@NotNull AbstractUser user, @NotNull String name, @NotNull byte[] fileData) {
		ActionEngine.ActionClient client = ActionEngine.data.getState(user);
		String hash = (String) client.stateEngineData;
		data.fileAdd(hash, fileData);
		return hash;
	}

	@Override
	public void fileDelete(@NotNull AbstractUser user, @NotNull String hash) {
		data.fileDelete(hash);
	}

	@Override
	public void fileDownload(@NotNull AbstractUser user, @NotNull String hash) {
		// No-op
	}

	@Override
	public void missionDetailsGetByName(@NotNull String missionName, @NotNull AbstractUser user) {
		// No-op
	}

	@Override
	public void missionAddResource(@NotNull AbstractUser missionOwner, @NotNull String missionName, @NotNull String dataUploadHash) {
		ActionEngine.ActionClient client = ActionEngine.data.getState(missionOwner);
		if (!client.stateEngineData_userHadPermissions) {
			return;
		}

		MissionState missionState = data.getMissionState(missionName);

		boolean dataAdded = false;
		for (MissionContentDataContainer container : ((Mission) client.stateEngineData).getContents()) {
			if (container.getDataAsMissionContent().getContentHash().equals(dataUploadHash)) {
				missionState.addMissionResource(container);
				dataAdded = true;
				break;
			}
		}
		if (!dataAdded) {
			throw new RuntimeException("The details added to the mission were not properly added to the state engine!");
		}
	}

	@Override
	public void missionRemoveResource(@NotNull AbstractUser user, @NotNull String missionName, @NotNull String dataUploadHash) {
		if (!ActionEngine.data.getState(user).stateEngineData_userHadPermissions) {
			return;
		}
		data.getMissionState(missionName).removeMissionResource(user, dataUploadHash);
	}

	@Override
	public void missionDetailsGet(@NotNull AbstractUser user) {
		// No-op
	}

	@Override
	public void missionAdd(@NotNull AbstractUser apiUser, @NotNull String missionName, @Nullable GroupSetProfiles groupSetProfile, @Nullable MissionModels.MissionUserRole defaultUserRole) {
		ActionEngine.ActionClient client = ActionEngine.data.getState(apiUser);
		if (!client.stateEngineData_userHadPermissions) {
			return;
		}
		Mission mission = (Mission) client.stateEngineData;
		if (TestExceptions.MISSION_IGNORE_GROUPS_MISSING_IN_ADD_REMOVE_RESPONSES) {
			mission.overrideGroups(new TreeSet<>(groupSetProfile.groupSet));
		}
		// TODO Missions: Add default password tests
		data.addMission(apiUser, missionName, groupSetProfile, mission, defaultUserRole, null);
	}

	@Override
	public void missionSubscribe(@NotNull AbstractUser missionOwner, @NotNull String missionName, @NotNull AbstractUser user) {
		ActionEngine.ActionClient client = ActionEngine.data.getState(missionOwner);
		if (!client.stateEngineData_userHadPermissions) {
			return;
		}
		SubscriptionData receivedSubscriptionData = (SubscriptionData) client.stateEngineData;
		data.getMissionState(missionName).addUserSubscription(user, receivedSubscriptionData);
	}

	@Override
	public void missionDelete(@NotNull AbstractUser user, @NotNull String missionName) {
		if (!ActionEngine.data.getState(user).stateEngineData_userHadPermissions) {
			return;
		}
		data.removeMission(missionName);
	}

	@Override
	public void missionDeepDelete(@NotNull AbstractUser user, @NotNull String missionName) {
		if (!ActionEngine.data.getState(user).stateEngineData_userHadPermissions) {
			return;
		}
		data.removeMission(missionName);
	}

	@Override
	public void missionSetUserRole(@NotNull AbstractUser missionOwner, @NotNull String missionName, @NotNull AbstractUser user, @Nullable MissionUserRole userRole) {
		ActionEngine.ActionClient client = ActionEngine.data.getState(missionOwner);
		if (!client.stateEngineData_userHadPermissions) {
			return;
		}
		data.getMissionState(missionName).updateUserSubscriptionRole(user, userRole);
	}

	@Override
	public void missionGetChanges(@NotNull AbstractUser user, @NotNull String missionName) {
		// TODO Mission: Can we somehow use this to seed better data for validation?
		// No-op
	}

	@Override
	public void missionSetKeywords(@NotNull AbstractUser user, @NotNull String missionName, @NotNull String... keywords) {
		if (!ActionEngine.data.getState(user).stateEngineData_userHadPermissions) {
			return;
		}
		data.getMissionState(missionName).setMissionKeywords(keywords);
	}

	@Override
	public void missionClearKeywords(@NotNull AbstractUser user, @NotNull String missionName) {
		if (!ActionEngine.data.getState(user).stateEngineData_userHadPermissions) {
			return;
		}
		data.getMissionState(missionName).clearMissionKeywords();
	}

	@Override
	public void missionSetPassword(@NotNull AbstractUser apiUser, @NotNull String missionName, @NotNull String password) {
		if (!ActionEngine.data.getState(apiUser).stateEngineData_userHadPermissions) {
			return;
		}
		data.getMissionState(missionName).setPassword(password);
	}
}
