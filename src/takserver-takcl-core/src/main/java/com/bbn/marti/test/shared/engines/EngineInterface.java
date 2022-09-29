package com.bbn.marti.test.shared.engines;

import com.bbn.marti.takcl.connectivity.missions.MissionModels;
import com.bbn.marti.test.shared.data.GroupProfiles;
import com.bbn.marti.test.shared.data.GroupSetProfiles;
import com.bbn.marti.test.shared.data.connections.AbstractConnection;
import com.bbn.marti.test.shared.data.connections.MutableConnection;
import com.bbn.marti.test.shared.data.servers.AbstractServerProfile;
import com.bbn.marti.test.shared.data.users.AbstractUser;
import com.bbn.marti.test.shared.data.users.MutableUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created on 10/26/15.
 */
public interface EngineInterface {
	UserIdentificationData DefaultSendingUserIdentification = UserIdentificationData.UID_AND_CALLSIGN;
	UserIdentificationData DefaultReceivingUserIdentification = UserIdentificationData.UID_AND_CALLSIGN;

	void offlineAddUsersAndConnectionsIfNecessary(@NotNull AbstractUser... users);

	void offlineEnableLatestSA(boolean enabled, @NotNull AbstractServerProfile... servers);

	void connectClientAndVerify(boolean doAuthIfNecessary, @NotNull AbstractUser user);

	void disconnectClientAndVerify(@NotNull AbstractUser disconnectingUser);

	void onlineRemoveInputAndVerify(@NotNull AbstractConnection input);
	
	void onlineRemoveDataFeedAndVerify(@NotNull AbstractConnection dataFeed);

	void attemptSendFromUserAndVerify(@NotNull AbstractUser sendingUser, @NotNull AbstractUser... targetUsers);

	void attemptSendFromUserAndVerify(@NotNull AbstractUser sendingUser, @NotNull String missionName);

	void attemptSendFromUserAndVerify(@NotNull UserIdentificationData senderIdentification, @NotNull AbstractUser sendingUser, @NotNull AbstractUser... targetUsers);

	void attemptSendFromUserAndVerify(@NotNull UserIdentificationData senderIdentification, @NotNull AbstractUser sendingUser, @NotNull UserIdentificationData recipientIdentification, @NotNull AbstractUser... targetUsers);

	void attemptSendFromUserAndVerify(@NotNull UserIdentificationData senderIdentification, @NotNull AbstractUser sendingUser, @NotNull UserIdentificationData recipientIdentification, @Nullable String missionName, @NotNull AbstractUser... targetUsers);

	void authenticateAndVerifyClient(@NotNull AbstractUser users);

	void onlineAddInput(@NotNull AbstractConnection input);
	
	void onlineAddDataFeed(@NotNull AbstractConnection dataFeed);

	void startServer(@NotNull AbstractServerProfile server, @NotNull String sessionIdentifier);

	void startServerWithStartupValidation(@NotNull AbstractServerProfile server, @NotNull String sessionIdentifier, boolean enablePluginManager, boolean enableRetentionService);

	void stopServers(@NotNull AbstractServerProfile... servers);

	void engineFactoryReset();

	void connectClientAndSendMessage(boolean doAuthIfNecessary, @NotNull AbstractUser user, @NotNull AbstractUser... targetUsers);

	void connectClientAndSendMessage(boolean doAuthIfNecessary, @NotNull UserIdentificationData providedSenderData, @NotNull AbstractUser sendingUser, @NotNull AbstractUser... targetUsers);

	void connectClientAndSendMessage(boolean doAuthIfNecessary, @NotNull UserIdentificationData providedSenderData, @NotNull AbstractUser sendingUser, @NotNull UserIdentificationData providedRecipientData, @NotNull AbstractUser... targetUsers);

	void offlineAddSubscriptionFromInputToServer(@NotNull AbstractConnection targetInput, @NotNull AbstractServerProfile serverProvidingSubscription);

	void offlineAddSubscriptionFromDataFeedToServer(AbstractConnection targetDataFeed, AbstractServerProfile serverProvidingSubscription);
	
	void offlineFederateServers(boolean useV1Federation, boolean useV2Federation, @NotNull AbstractServerProfile... serversToFederate);

	void offlineAddOutboundFederateConnection(boolean useV2Federation, @NotNull AbstractServerProfile sourceServer, @NotNull AbstractServerProfile targetServer);

	void offlineAddFederate(@NotNull AbstractServerProfile federatedServer, @NotNull AbstractServerProfile federate);

	void offlineAddOutboundFederateGroup(@NotNull AbstractServerProfile federatedServer, @NotNull AbstractServerProfile federate, @NotNull String outboundGroupIdentifier);

	void offlineAddInboundFederateGroup(@NotNull AbstractServerProfile federatedServer, @NotNull AbstractServerProfile federate, @NotNull String inboundGroupIdentifier);

	void onlineAddUser(@NotNull AbstractUser user);

	void onlineRemoveUsers(@NotNull AbstractServerProfile server, @NotNull MutableUser... users);

	void onlineAddUsersToGroup(@NotNull AbstractServerProfile server, @NotNull GroupProfiles group, @NotNull MutableUser... users);

	void onlineRemoveUsersFromGroup(@NotNull AbstractServerProfile server, @NotNull GroupProfiles group, @NotNull MutableUser... users);

	void onlineUpdateUserPassword(@NotNull AbstractServerProfile server, @NotNull MutableUser user, @NotNull String userPassword);

	void updateLocalUserPassowrd(@NotNull MutableUser user);

	void onlineAddInputToGroup(@NotNull MutableConnection input, @NotNull GroupProfiles group);

	void onlineRemoveInputFromGroup(@NotNull MutableConnection input, @NotNull GroupProfiles group);

	String fileAdd(@NotNull AbstractUser user, @NotNull String name, @NotNull byte[] data);

	void fileDelete(@NotNull AbstractUser user, @NotNull String hash);

	void fileDownload(@NotNull AbstractUser user, @NotNull String hash);

	void missionAdd(@NotNull AbstractUser apiUser, @NotNull String missionName, @Nullable GroupSetProfiles groupSetProfiles, @Nullable MissionModels.MissionUserRole defaultUserRole);

	void missionDetailsGet(@NotNull AbstractUser user);

	void missionDetailsGetByName(@NotNull String missionName, @NotNull AbstractUser user);

	void missionAddResource(@NotNull AbstractUser missionOwner, @NotNull String missionName, @NotNull String dataUploadHash);

	void missionRemoveResource(@NotNull AbstractUser user, @NotNull String missionName, @NotNull String dataUploadHash);

	void missionSubscribe(@NotNull AbstractUser missionOwner, @NotNull String missionName, @NotNull AbstractUser user);

	void missionDelete(@NotNull AbstractUser user, @NotNull String missionName);

	void missionDeepDelete(@NotNull AbstractUser user, @NotNull String missionName);

	void missionSetUserRole(@NotNull AbstractUser missionOwner, @NotNull String missionName, @NotNull AbstractUser user, @Nullable MissionModels.MissionUserRole userRole);

	void missionGetChanges(@NotNull AbstractUser user, @NotNull String missionName);

	void missionSetKeywords(@NotNull AbstractUser user, @NotNull String missionName, @NotNull String... keywords);

	void missionClearKeywords(@NotNull AbstractUser user, @NotNull String missionName);

	void missionSetPassword(@NotNull AbstractUser apiUser, @NotNull String missionName, @NotNull String password);
}
