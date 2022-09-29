package com.bbn.marti.test.shared.engines.verification;

import com.bbn.marti.takcl.TestExceptions;
import com.bbn.marti.takcl.connectivity.missions.MissionModels;
import com.bbn.marti.test.shared.TestConnectivityState;
import com.bbn.marti.test.shared.data.GroupProfiles;
import com.bbn.marti.test.shared.data.GroupSetProfiles;
import com.bbn.marti.test.shared.data.connections.AbstractConnection;
import com.bbn.marti.test.shared.data.connections.MutableConnection;
import com.bbn.marti.test.shared.data.protocols.ProtocolProfiles;
import com.bbn.marti.test.shared.data.servers.AbstractServerProfile;
import com.bbn.marti.test.shared.data.users.AbstractUser;
import com.bbn.marti.test.shared.data.users.MutableUser;
import com.bbn.marti.test.shared.engines.ActionEngine;
import com.bbn.marti.test.shared.engines.EngineHelper;
import com.bbn.marti.test.shared.engines.EngineInterface;
import com.bbn.marti.test.shared.engines.UserIdentificationData;
import com.bbn.marti.test.shared.engines.state.ConnectionState;
import com.bbn.marti.test.shared.engines.state.MissionState;
import com.bbn.marti.test.shared.engines.state.StateEngine;
import com.bbn.marti.test.shared.engines.state.UserState;
import com.bbn.marti.tests.Assert;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.bbn.marti.takcl.connectivity.missions.MissionModels.*;
import static com.bbn.marti.tests.Assert.SampleObjects;

/**
 * Used to verify test results
 * Created on 10/26/15.
 */
public class VerificationEngine implements EngineInterface {

	public static VerificationData data = VerificationData.instance;


	private AtomicInteger eventIndex = new AtomicInteger(0);

	public VerificationEngine() {
	}

//    private UserState.VerificationEngineUserStateInterface asUser(AbstractUser user) {
//        return (UserState.VerificationEngineUserStateInterface) environmentState.getState(user);
//    }

//    private UserState.VerificationEngineUserStateInterface asUser(UserState user) {
//        return (UserState.VerificationEngineUserStateInterface) user;
//    }

	private void setSenderExpectations(boolean doAuthIfNecessary, @NotNull AbstractUser sendingUser) {
		if (sendingUser.doValidation()) {
			TreeSet<AbstractUser> latestSASenderList = EngineHelper.computeExpectedLatestSASendersOnConnect(
					doAuthIfNecessary, StateEngine.data.getState(sendingUser));

			TestConnectivityState expectedConnectivityState = EngineHelper.computeExpectedUserSignOnResult(doAuthIfNecessary, sendingUser);

			// TODO Missions: Add checks that mission data is sent to fresh users
			data.setUserExpectations(sendingUser, latestSASenderList, expectedConnectivityState);
		}
	}

	@Override
	public void offlineAddUsersAndConnectionsIfNecessary(@NotNull AbstractUser... users) {
		data.engineIterationDataClear();

	}

	@Override
	public void offlineEnableLatestSA(boolean enabled, @NotNull AbstractServerProfile... servers) {
		data.engineIterationDataClear();

	}

	@Override
	public void connectClientAndVerify(boolean doAuthIfNecessary, @NotNull AbstractUser user) {
		data.validateAllUserExpectations("latestSA");
		data.engineIterationDataClear();
	}

	@Override
	public void disconnectClientAndVerify(@NotNull AbstractUser disconnectingUser) {
		if (disconnectingUser.doValidation()) {
			UserState disconnectingState = StateEngine.data.getState(disconnectingUser);
			TreeSet<UserState> userList = new TreeSet<>();
			userList.add(disconnectingState);

			Map<AbstractUser, TreeSet<AbstractUser>> latestSAClientMap;

			latestSAClientMap = EngineHelper.computeDisconnectionLatestSACount(userList);

			for (UserState user : StateEngine.data.getUserStates()) {
				if (user == disconnectingState) {
					// TODO Missions: Do disconnect notifications propagate to mission updates, or no?
					data.setUserExpectations(disconnectingUser, new TreeSet<AbstractUser>(),
							TestConnectivityState.Disconnected);
				} else {
					TreeSet<AbstractUser> userEventList = latestSAClientMap.get(user.getProfile());
//                    UserState.VerificationPrepEngineUserStateInterface clientState =
//                            (UserState.VerificationPrepEngineUserStateInterface) client;
					// TODO Missions: Do disconnect notifications propagate to mission updates, or no?
					data.setUserExpectations(user.getProfile(),
							(userEventList == null ? new TreeSet<AbstractUser>() : userEventList),
							user.getConnectivityState());
				}
			}
		}


		data.validateAllUserExpectations("UserDisconnected");
		data.engineIterationDataClear();
	}

	@Override
	public void offlineFederateServers(boolean useV1Federation, boolean useV2Federation, @NotNull AbstractServerProfile... serversToFederate) {
		data.engineIterationDataClear();

	}

	@Override
	public void offlineAddOutboundFederateConnection(boolean useV2Federation, @NotNull AbstractServerProfile sourceServer, @NotNull AbstractServerProfile targetServer) {
		data.engineIterationDataClear();

	}

	@Override
	public void offlineAddFederate(@NotNull AbstractServerProfile federatedServer, @NotNull AbstractServerProfile federate) {
		data.engineIterationDataClear();

	}

	@Override
	public void offlineAddOutboundFederateGroup(@NotNull AbstractServerProfile federatedServer, @NotNull AbstractServerProfile federate, @NotNull String outboundGroupIdentifier) {
		data.engineIterationDataClear();

	}

	@Override
	public void offlineAddInboundFederateGroup(@NotNull AbstractServerProfile federatedServer, @NotNull AbstractServerProfile federate, @NotNull String inboundGroupIdentifier) {
		data.engineIterationDataClear();

	}

	@Override
	public void onlineAddUser(@NotNull AbstractUser user) {
		data.engineIterationDataClear();

	}

	@Override
	public void onlineRemoveUsers(@NotNull AbstractServerProfile server, @NotNull MutableUser... users) {
		data.engineIterationDataClear();

	}

	@Override
	public void onlineAddUsersToGroup(@NotNull AbstractServerProfile server, @NotNull GroupProfiles group, @NotNull MutableUser... users) {
		data.engineIterationDataClear();

	}

	@Override
	public void onlineRemoveUsersFromGroup(@NotNull AbstractServerProfile server, @NotNull GroupProfiles group, @NotNull MutableUser... users) {
		data.engineIterationDataClear();

	}

	@Override
	public void onlineUpdateUserPassword(@NotNull AbstractServerProfile server, @NotNull MutableUser user, @NotNull String userPassword) {
		Assert.assertFalse("User credentials should no longer be valid!", user.isUserCredentialsValid());
	}

	@Override
	public void updateLocalUserPassowrd(@NotNull MutableUser user) {
		Assert.assertTrue("User credentials should be valid!", user.isUserCredentialsValid());
		data.engineIterationDataClear();
	}

	@Override
	public void onlineAddInputToGroup(@NotNull MutableConnection input, @NotNull GroupProfiles group) {
		data.engineIterationDataClear();

	}

	@Override
	public void onlineRemoveInputFromGroup(@NotNull MutableConnection input, @NotNull GroupProfiles group) {
		data.engineIterationDataClear();

	}

	@Override
	public void onlineRemoveInputAndVerify(@NotNull AbstractConnection input) {
		ConnectionState connectionState = StateEngine.data.getState(input);

		Map<AbstractUser, TreeSet<AbstractUser>> latestSAClientMap;

		// Get the LatestSA that will be sent upon the disconnection of all users associated with the input
		latestSAClientMap = EngineHelper.computeDisconnectionLatestSACount(connectionState.getUserStates());

		// Go through all clients, and set their disconnection notification messages, and if they are on the input and
		// can be disconnected, set the state change

		for (UserState userState : StateEngine.data.getUserStates()) {

			TreeSet<AbstractUser> userEventList = latestSAClientMap.get(userState.getProfile());


			if (userEventList == null) {
				userEventList = new TreeSet<>();
			}

			TestConnectivityState expectedState;
			TreeSet<AbstractUser> expectedUserEventList;


			if (userState.getConnectionState() == connectionState &&
					input.getProtocol().canConnect() &&
					userState.getConnectivityState() != TestConnectivityState.Disconnected &&
					input.getProtocol().clientConnectionSeveredWithInputRemoval()) {
				expectedState = TestConnectivityState.Disconnected;
				expectedUserEventList = userEventList;
			} else {
				expectedState = userState.getConnectivityState();
				expectedUserEventList = new TreeSet<>();
			}

			// TODO: Missions: How does Input Removal impact missions, if at all?
			data.setUserExpectations(userState.getProfile(), expectedUserEventList, expectedState);
		}


		ConnectionState state = StateEngine.data.getState(input);
		ProtocolProfiles protocol = state.getProfile().getProtocol();

		// As of 01/13/2016, Clients are no longer disconnected when an input is removed. This was necessary to resolve a significant memory leak. Similar check is also commented out in UnifiedExpectantClient CanCurrentlyReceive and CanCurrentlySend checks
		if (protocol.canConnect() && protocol.clientConnectionSeveredWithInputRemoval()) {
			for (UserState userState : StateEngine.data.getUserStates()) {
				if (ActionEngine.data.getState(userState.getProfile()).isConnected()) {
					throw new RuntimeException("Client '" + toString() + "' is still connected even though the server has turned off the connection!");
				}
			}
		}

		data.validateAllUserExpectations("InputRemoved");
		data.engineIterationDataClear();
	}
	
	@Override
	public void onlineRemoveDataFeedAndVerify(@NotNull AbstractConnection dataFeed) {
		onlineRemoveInputAndVerify(dataFeed);
	}

	@Override
	public void attemptSendFromUserAndVerify(@NotNull AbstractUser sendingUser, @NotNull AbstractUser... targetUsers) {
		attemptSendFromUserAndVerify(DefaultSendingUserIdentification, sendingUser, DefaultReceivingUserIdentification, targetUsers);
		data.engineIterationDataClear();
	}

	@Override
	public void attemptSendFromUserAndVerify(@NotNull AbstractUser sendingUser, @NotNull String missionName) {
		attemptSendFromUserAndVerify(DefaultSendingUserIdentification, sendingUser, DefaultReceivingUserIdentification, missionName);
	}

	@Override
	public void attemptSendFromUserAndVerify(@NotNull UserIdentificationData senderIdentification, @NotNull AbstractUser sendingUser, @NotNull AbstractUser... targetUsers) {
		attemptSendFromUserAndVerify(senderIdentification, sendingUser, DefaultReceivingUserIdentification, targetUsers);
		data.engineIterationDataClear();
	}

	@Override
	public void attemptSendFromUserAndVerify(@NotNull UserIdentificationData senderIdentification, @NotNull AbstractUser sendingUser, @NotNull UserIdentificationData recipientIdentification, @NotNull AbstractUser... targetUsers) {
		attemptSendFromUserAndVerify(senderIdentification, sendingUser, recipientIdentification, null, targetUsers);
	}

	@Override
	public void attemptSendFromUserAndVerify(@NotNull UserIdentificationData senderIdentification, @NotNull AbstractUser sendingUser, @NotNull UserIdentificationData recipientIdentification, @Nullable String missionName, @NotNull AbstractUser... targetUsers) {
		// TODO Missions: Add Mission Validation
		if (sendingUser.doValidation()) {
			if (missionName == null) {
				Map<AbstractUser, TreeSet<AbstractUser>> outcomeMap = EngineHelper.computeExpectedRecipientsOfUserSend(
						StateEngine.data.getState(sendingUser), false, false,
						recipientIdentification, StateEngine.data.getUserStates(new TreeSet<>(Arrays.asList(targetUsers))));

				for (UserState userState : StateEngine.data.getUserStates()) {
					// TODO Missions: Add Mission flow verification within EngineHelper. Include mission, check recipients, ad to outcomes
					data.setUserExpectations(userState.getProfile(), outcomeMap.get(userState.getProfile()), userState.getConnectivityState());
				}

				String label = missionName == null ? "" : "Mission";
				if (targetUsers.length > 0) {
					label = label + "PointToPoint";
				}
				if (label.equals("")) {
					label = "Sent";
				}
				data.validateAllUserExpectations(label);
			}
		}
		data.engineIterationDataClear();
	}

	@Override
	public void authenticateAndVerifyClient(@NotNull AbstractUser user) {
		// TODO Missions: Add expectations?
		setSenderExpectations(true, user);
		if (user.isUserCredentialsValid()) {
			data.validateAllUserExpectations("latestSA");
		}
		data.engineIterationDataClear();
	}

	@Override
	public void offlineAddSubscriptionFromInputToServer(@NotNull AbstractConnection targetInput, @NotNull AbstractServerProfile serverProvidingSubscription) {
		data.engineIterationDataClear();
	}
	
	@Override
	public void offlineAddSubscriptionFromDataFeedToServer(@NotNull AbstractConnection targetInput, @NotNull AbstractServerProfile serverProvidingSubscription) {
		data.engineIterationDataClear();
	}

	@Override
	public void onlineAddInput(@NotNull AbstractConnection input) {
		data.engineIterationDataClear();
	}
	
	@Override
	public void onlineAddDataFeed(@NotNull AbstractConnection dataFeed) {
		data.engineIterationDataClear();
	}

	@Override
	public void startServer(@NotNull AbstractServerProfile server, @NotNull String sessionIdentifier) {
		data.engineIterationDataClear();
	}

	@Override
	public void startServerWithStartupValidation(@NotNull AbstractServerProfile server, @NotNull String sessionIdentifier, boolean enablePluginManager, boolean enableRetentionService) {
		data.engineIterationDataClear();
	}

	@Override
	public void stopServers(@NotNull AbstractServerProfile... servers) {

		data.engineIterationDataClear();
	}

	@Override
	public void engineFactoryReset() {

		data.engineIterationDataClear();
	}

	@Override
	public void connectClientAndSendMessage(boolean doAuthIfNecessary, @NotNull AbstractUser user, @NotNull AbstractUser... targetUsers) {
		connectClientAndSendMessage(doAuthIfNecessary, UserIdentificationData.UID_AND_CALLSIGN, user, targetUsers);
		data.engineIterationDataClear();
	}


	@Override
	public void connectClientAndSendMessage(boolean doAuthIfNecessary, @NotNull UserIdentificationData providedSenderData, @NotNull AbstractUser sendingUser, @NotNull AbstractUser... targetUsers) {
		connectClientAndSendMessage(doAuthIfNecessary, providedSenderData, sendingUser, DefaultReceivingUserIdentification, targetUsers);
		data.engineIterationDataClear();
	}

	@Override
	public void connectClientAndSendMessage(boolean doAuthIfNecessary, @NotNull UserIdentificationData providedSenderData, @NotNull AbstractUser sendingUser, @NotNull UserIdentificationData providedRecipientData, @NotNull AbstractUser... targetUsers) {
		// TODO Missions: Incorporate expected sends into EngineHelper, and add inclusion of mission to initial send message
		setSenderExpectations(doAuthIfNecessary, sendingUser);

		if (sendingUser.doValidation()) {

			Map<AbstractUser, TreeSet<AbstractUser>> receiveMap = EngineHelper.computeExpectedRecipientsOfUserSend(
					StateEngine.data.getState(sendingUser), true,
					doAuthIfNecessary, providedRecipientData, StateEngine.data.getUserStates(new TreeSet<>(Arrays.asList(targetUsers))));

			for (AbstractUser loopUser : StateEngine.data.getUsers()) {
				if (sendingUser != loopUser) {

					UserState loopUserState = StateEngine.data.getState(loopUser);

					TreeSet<AbstractUser> receiveList = receiveMap.get(loopUser);

					if (receiveList == null) {
						receiveList = new TreeSet<>();
					}

					// TODO Missions: Add Mission Verification
					data.setUserExpectations(loopUser, receiveList, loopUserState.getConnectivityState());
				}
			}
		}

		if (targetUsers == null || targetUsers.length == 0) {
			data.validateAllUserExpectations("ClientConnectAndSend");
		} else {
			data.validateAllUserExpectations("ClientConnectAndSendPointToPoint");
		}
		data.engineIterationDataClear();
	}

	@Override
	public String fileAdd(@NotNull AbstractUser user, @NotNull String name, @NotNull byte[] fileData) {
		// TODO: Consider data that has already been added will be given an existing hash?
		ActionEngine.ActionClient client = ActionEngine.data.getState(user);
		ResponseWrapper rawResponse = client.getCallResponse();
		EnterpriseSyncUploadResponse response = Assert.getEnterpriseSyncUploadResponse(200, rawResponse);
		String hash = response.Hash;
		client.stateEngineData = hash;
		Assert.assertNotNull("Response hash is null!", hash);
		return hash;
	}

	@Override
	public void fileDelete(@NotNull AbstractUser user, @NotNull String hash) {
		ResponseWrapper rawResponse = ActionEngine.data.getState(user).getCallResponse();
		Assert.assertCallReturnCode(200, rawResponse);
	}

	@Override
	public void fileDownload(@NotNull AbstractUser user, @NotNull String hash) {
		ResponseWrapper rawResponse = ActionEngine.data.getState(user).getCallResponse();

		if (StateEngine.data.enterpriseSyncDataDeleted(hash)) {
			Assert.assertCallReturnCode(404, rawResponse);

		} else {
			byte[] responseBody = Assert.getByteResponseData(200, rawResponse);
			byte[] expectedContents = StateEngine.data.getEnterpriseSyncData(hash);
			Assert.assertEquals("The received data is longer than the expected data!", responseBody.length, expectedContents.length);
			Assert.assertArrayEquals("Data arrays do not match!", responseBody, expectedContents);
		}
	}

	@Override
	public void missionDetailsGet(@NotNull AbstractUser user) {
		// NO SPECIFIC PERMISSIONS
		// TODO Missions: Add verification against existing data
		innerMissionGet(null, user, null);
	}

	@Override
	public void missionDetailsGetByName(@NotNull String missionName, @NotNull AbstractUser user) {
		// NO SPECIFIC PERMISSIONS
		// TODO Missions: Add verification against existing data
		innerMissionGet(missionName, user, null);
	}

	@Override
	public void missionAddResource(@NotNull AbstractUser user, @NotNull String missionName, @NotNull String dataUploadHash) {
		MissionUserPermission permission = MissionUserPermission.MISSION_WRITE;

		ActionEngine.ActionClient client = ActionEngine.data.getState(user);
		ResponseWrapper rawResponse = client.getCallResponse();
		ResponseWrapper validationResponse = ActionEngine.data.getState(user).getVerificationCallResponse();

		if (StateEngine.data.userHasMissionPermission(user, missionName, permission)) {
			client.stateEngineData_userHadPermissions = true;
			Mission returnedMission = Assert.getSingleApiSetVerificationData(200, SampleObjects.Mission, rawResponse);

			client.stateEngineData = returnedMission;

			TreeSet<MissionContentDataContainer> contentsList = returnedMission.getContents();
			Assert.assertTrue("The updated mission has no mission contents!", contentsList != null && contentsList.size() > 0);
			Optional<MissionContentDataContainer> result = contentsList.stream().filter(x -> x.getDataAsMissionContent().getContentHash().equals(dataUploadHash)).findFirst();
			Assert.assertTrue("The updated mission does not contain any contents with the expected hash!", result.isPresent());

			// TODO Missions: Add verification of other known file data
			// TODO Missions: Account for muiltiple types of mission contents
			// TODO Missions: Add verification aganist previous data?

			Mission retrievedMission = Assert.getSingleApiSetResponseData(200, SampleObjects.Mission, validationResponse);

			HashMap<Pattern, Object> exceptions = null;
			if (TestExceptions.MISSION_IGNORE_GROUPS_MISSING_IN_ADD_REMOVE_RESPONSES || TestExceptions.MISSION_IGNORE_ADD_RESOURCE_RESPONSE_MISSING_UID_OJBECTS) {
				exceptions = new HashMap<>();
				if (TestExceptions.MISSION_IGNORE_GROUPS_MISSING_IN_ADD_REMOVE_RESPONSES) {
					exceptions.put(MISSION_GROUPS_PATTERN, new TreeSet<>());
				}
				if (TestExceptions.MISSION_IGNORE_ADD_RESOURCE_RESPONSE_MISSING_UID_OJBECTS) {
					exceptions.put(MISSION_UIDS_PATTERN, new TreeSet<>());
				}
			}
			Assert.assertTrue("The retrieved mission is different than the returned mission!", returnedMission.assertMatchesExpectation(retrievedMission, exceptions));

		} else {
			client.stateEngineData_userHadPermissions = false;
			Assert.assertCallReturnCode(405, rawResponse);
		}
	}

	@Override
	public void missionRemoveResource(@NotNull AbstractUser user, @NotNull String missionName, @NotNull String dataUploadHash) {
		MissionUserPermission permission = MissionUserPermission.MISSION_WRITE;
		ActionEngine.ActionClient client = ActionEngine.data.getState(user);
		ResponseWrapper rawResponse = client.getCallResponse();

		if (StateEngine.data.userHasMissionPermission(user, missionName, permission)) {
			client.stateEngineData_userHadPermissions = true;
			Mission returnedMission = Assert.getSingleApiSetResponseData(200, SampleObjects.Mission, rawResponse);
			TreeSet<MissionContentDataContainer> contentsList = returnedMission.getContents();

			if (contentsList != null && contentsList.size() > 0) {
				for (MissionContentDataContainer dataContainer : contentsList) {
					Assert.assertNotEquals("The removed data with hash '" + dataUploadHash + "' is still part of the mission!",
							dataUploadHash, dataContainer.hashCode());
				}
			}
		} else {
			client.stateEngineData_userHadPermissions = false;
			Assert.assertCallReturnCode(405, rawResponse);
		}
	}

	/**
	 * @param missionName If null, all missions are assumed to have been fetched
	 * @param user        The user fetching the mission
	 */
	private void innerMissionGet(@Nullable String missionName, @NotNull AbstractUser user, @Nullable TreeSet<String> expectedGroupOverride) {
		// TODO Missions: Add comparison to known mission content
		// TODO Missions: Add checks for negative cases

		UserState userState = StateEngine.data.getState(user);
		ActionEngine.data.getState(user).stateEngineData_userHadPermissions = true;

		// Get the expected values for relevant mission data
		TreeMap<UserState, Set<MissionState>> expectedReceivedMissions;

		if (missionName == null) {
			expectedReceivedMissions = EngineHelper.computeMissionMembership(StateEngine.data.getMissionStates(), userState);
		} else {
			TreeSet<MissionState> expectedMissionStates = new TreeSet<>();
			expectedMissionStates.add(StateEngine.data.getMissionState(missionName));
			expectedReceivedMissions = EngineHelper.computeMissionMembership(expectedMissionStates, userState);
		}

		ResponseWrapper rawResponse = ActionEngine.data.getState(user).getCallResponse();

		if (missionName != null && StateEngine.data.hasMissionState(missionName) && StateEngine.data.getMissionState(missionName).hasBeenDeleted()) {
			Assert.assertEquals("The deleted mission should return a 410 return code!", 410, rawResponse.responseCode);

		} else if (missionName != null && expectedReceivedMissions.get(userState).stream().noneMatch(x -> x.getMissionName().equals(missionName))) {
			Assert.assertNotNull("TAKCL Error! Raw response should not be null!", rawResponse);
			Assert.assertEquals("Expected 404 response code since the user is not a mission member!", 404, rawResponse.responseCode);

		} else {
			Set<Mission> missions = Assert.getApiSetResponseData(200, SampleObjects.Mission, rawResponse);
			TreeMap<String, Mission> missionMap = new TreeMap<>();
			for (Mission mission : missions) {
				missionMap.put(mission.getUniqueStableName(), mission);
			}

			TreeMap<AbstractUser, ActionEngine.ActionClient> changedClients = ActionEngine.data.getAllChangedClients();

			// TODO Missions: Do better accounting of individual missions when removing users as changed clients. Also test multiple mission subscriptions
			for (UserState expectedState : expectedReceivedMissions.keySet()) {
				AbstractUser expectedUser = expectedState.getProfile();

				if (changedClients.containsKey(expectedUser)) {
					ActionEngine.ActionClient client = changedClients.get(expectedUser);

					for (MissionState expectedMissionState : expectedReceivedMissions.get(expectedState)) {
						if (expectedMissionState.hasBeenDeleted()) {
							continue;
						}
						String expectedMissionName = expectedMissionState.getMissionName();
						TreeSet<String> retrievedMissionNames = new TreeSet<>(missionMap.keySet());
						if (retrievedMissionNames.contains(expectedMissionState.getMissionName())) {
							// Mission members can only see the groups they are a part of
							HashMap<Pattern, Object> exceptions = new HashMap<>();

							// TODO: Missions: Add roles details
							if (expectedGroupOverride == null) {
								exceptions.put(MISSION_GROUPS_PATTERN, expectedMissionState.getGroupSet().getIntersectingGroupNames(client.getProfile().getActualGroupSetAccess()));
							} else {
								exceptions.put(MISSION_GROUPS_PATTERN, expectedGroupOverride);
							}
							exceptions.put(MISSION_UID_TIMESTAMPS_PATTERN, EXCEPTION_NOT_NULL);

							expectedMissionState.assertMissionMatchesExpectations(missionMap.get(expectedMissionName), exceptions);
							changedClients.remove(expectedUser);
						} else {
							Assert.fail("The user '" + expectedUser + "' has not received any data for the mission '" + expectedMissionName + "'!");
						}
					}
				} else if (expectedReceivedMissions.get(expectedState).size() != 0) {
					Assert.fail("The user '" + expectedUser + "' has not received any data!");
				}
			}

			if (changedClients.size() > 0) {
				String changedUsers = changedClients.keySet().stream().map(AbstractUser::getConsistentUniqueReadableIdentifier).collect(Collectors.joining());
				Assert.fail("Users " + changedUsers + " shouldn't have received missions!");
			}
		}

//
//		TreeSet<UserState> userStates = StateEngine.data.getUserStates(new TreeSet<>(Arrays.asList(fetchingUsers)));
//		TreeSet<MissionState> missionStates;
//
//
//		// Get all the mission states
//		if (missionName == null) {
//			missionStates = StateEngine.data.getMissionStates();
//		} else {
//			missionStates = new TreeSet<>();
//			missionStates.add(StateEngine.data.getMissionState(missionName));
//		}
//
//		// Determine which users should have received updates
//		TreeMap<MissionState, Set<UserState>> missionMemberships = EngineHelper.computeMissionMembership(missionStates, userStates);
//
//		for (MissionState missionState : missionStates) {
//			for (UserState userState : missionMemberships.get(missionState)) {
//				AbstractUser user = userState.getProfile();
//				ActionEngine.ActionClient client = ActionEngine.data.getState(user);
//				Mission clientMission;
//
//				// Get the expected retrieved mission, fall back to null otherwise
//				if (missionName == null) {
//					clientMission = client.getRetrievedMissions().getOrDefault(missionState.getMissionName(), null);
//				} else {
//					clientMission = client.getRetrievedMission();
//				}
//
//				if (clientMission == null) {
//					// If it's null, add it to the missing missions
//					if (!missingExpectedUserMissions.containsKey(user)) {
//						missingExpectedUserMissions.put(user, new TreeSet<>());
//					}
//					missingExpectedUserMissions.get(user).add(user.getConsistentUniqueReadableIdentifier() + " missing mission data for " + missionState.getMissionName());
//				} else {
//					// Otherwise, add it to the received expected missions if it is equal or non-matching missions if not equal
//					String notEqualErr = null;
//					try {
//						if (!clientMission.equals(missionState.getMission())) {
//							notEqualErr = "";
//						}
//					} catch (AssertionError e) {
//						notEqualErr = e.getMessage();
//					}
//
//					if (notEqualErr != null) {
//						if (!receivedNonMatchingUserMissions.containsKey(user)) {
//							receivedNonMatchingUserMissions.put(user, new TreeSet<>());
//						}
//						receivedExpectedUserMissions.get(user).add("\n\tMission Data does not match for mission " +
//								missionState.getMissionName() + ": " + notEqualErr);
//
//					} else {
//						if (!receivedExpectedUserMissions.containsKey(user)) {
//							receivedExpectedUserMissions.put(user, new TreeSet<>());
//						}
//						receivedExpectedUserMissions.get(user).add(user.getConsistentUniqueReadableIdentifier() + " <<<<<< " + missionState.getMissionName());
//					}
//				}
//
////				Mission clientMission = missionName == null ? client.getRetrievedMissions().get(missionState.getMissionName()) : client.getRetrievedMission();
////				Assert.assertTrue(clientMission.equals(missionState.getMission()));
////				userStates.remove(userState);
//			}
//
//			//
//			if (userStates.size() > 0) {
//				for (UserState userState : userStates) {
//					AbstractUser user = userState.getProfile();
//					ActionEngine.ActionClient client = ActionEngine.data.getState(user);
//
//
////					TreeMap<AbstractUser, TreeSet<String>> receivedExpectedUserMissions = new TreeMap<>();
////					TreeMap<AbstractUser, TreeSet<String>> receivedNonMatchingUserMissions = new TreeMap<>();
//
//
//					// Get the missions received by the client
//					TreeSet<String> receivedMissions = new TreeSet<>(client.getRetrievedMissions().keySet());
//					if (client.getRetrievedMission() != null) {
//						receivedMissions.add(client.getRetrievedMission().getName());
//					}
//
//					Set<String> presentMissions = new HashSet<>();
//					presentMissions.addAll(receivedExpectedUserMissions.get(user));
//					presentMissions.addAll(receivedNonMatchingUserMissions.get(user));
//
//					if (!receivedUnexpectedUserMissions.containsKey(user)) {
//						receivedUnexpectedUserMissions.put(user, new TreeSet<>());
//					}
//					receivedUnexpectedUserMissions.get(user).add(
//							user.getConsistentUniqueReadableIdentifier() + " incorrectly received data " +
//									"from the following missions: [" + String.join(",", missionNames) + "]");
//				}
//			}
//		}
//
//		for (Asbtra)
//
//			TreeMap<AbstractUser, TreeSet<String>> absentUserMissions = new TreeMap<>();
//		TreeMap<AbstractUser, TreeSet<String>> unexpectedUserMissions = new TreeMap<>();
//		TreeMap<AbstractUser, TreeSet<String>> expectedUserMissions = new TreeMap<>();
//		TreeMap<AbstractUser, TreeSet<String>> nonMatchingUserMissions = new TreeMap<>();

		data.engineIterationDataClear();
	}

	@Override
	public void missionAdd(@NotNull AbstractUser apiUser, @NotNull String missionName, @Nullable GroupSetProfiles groupProfile, @Nullable MissionModels.MissionUserRole userRole) {
		// NO DEFAULT PERMISSIONS

		ActionEngine.ActionClient client = ActionEngine.data.getState(apiUser);
		ResponseWrapper rawResponse = client.getCallResponse();


		if (StateEngine.data.getState(apiUser).isAdmin()) {
			client.stateEngineData_userHadPermissions = true;

			// Update vs create
			int expectedResult = StateEngine.data.hasMissionState(missionName) ? 200 : 201;
			Mission ownerCreatedMission = Assert.getSingleApiSetResponseData(expectedResult, SampleObjects.Mission, rawResponse);
			client.stateEngineData = ownerCreatedMission;


			// Ensure mission was created
			Assert.assertEquals("The created mission name does not match the received mission name!", missionName, ownerCreatedMission.getUniqueStableName());
			Assert.assertEquals("The created mission groups do no match the received mission groups!", groupProfile.groupSet, ownerCreatedMission.getGroups());

			// TODO Missions: Clear Data
			// TODO Missions: Incorporate fetching data through a REST API and valiate response data
		} else {
			client.stateEngineData_userHadPermissions = false;
			Assert.assertCallReturnCode(403, rawResponse);
		}
	}

	@Override
	public void missionSubscribe(@NotNull AbstractUser missionOwner, @NotNull String missionName, @NotNull AbstractUser user) {
		// NO DEFAULT PERMISSIONS

		ActionEngine.ActionClient client = ActionEngine.data.getState(missionOwner);
		client.stateEngineData_userHadPermissions = true;
		ResponseWrapper rawResponse = client.getCallResponse();
		SubscriptionData data = Assert.getApiSingleResponseData(201, SampleObjects.ReceivedSubscriptionData, rawResponse);
		client.stateEngineData = data;
		Assert.assertNotNull("Token should not be null!", data.getToken());
		Assert.assertNotNull("createTime should not be null!", data.getCreateTime());
		Assert.assertEquals("The clientId should match!", user.getCotUid(), data.getClientUid());
	}

	@Override
	public void missionDelete(@NotNull AbstractUser user, @NotNull String missionName) {
		MissionUserPermission permission = MissionUserPermission.MISSION_WRITE;
		ActionEngine.ActionClient client = ActionEngine.data.getState(user);
		ResponseWrapper rawResponse = client.getCallResponse();
		ResponseWrapper verificationResponse = client.getVerificationCallResponse();

		if (StateEngine.data.userHasMissionPermission(user, missionName, permission)) {
			client.stateEngineData_userHadPermissions = true;

			Mission returnedMission = Assert.getSingleApiSetResponseData(200, SampleObjects.Mission, rawResponse);

			Assert.assertNotNull("The mission deletion did not return the deleted mission!", returnedMission);
			Assert.assertNull("The client was able to retrieve the mission after deleting it!", verificationResponse.body);
		} else {
			client.stateEngineData_userHadPermissions = false;
			throw new RuntimeException("The negative scenario for this case is not yet properly handled!");
		}
	}

	@Override
	public void missionDeepDelete(@NotNull AbstractUser user, @NotNull String missionName) {
		MissionUserPermission permission = MissionUserPermission.MISSION_DELETE;

		ActionEngine.ActionClient client = ActionEngine.data.getState(user);
		ResponseWrapper rawResponse = client.getCallResponse();
		ResponseWrapper verificationResponse = client.getVerificationCallResponse();

		if (StateEngine.data.userHasMissionPermission(user, missionName, permission)) {
			client.stateEngineData_userHadPermissions = true;

			Mission returnedMission = Assert.getSingleApiSetResponseData(200, SampleObjects.Mission, rawResponse);
			Assert.assertNotNull("The mission deletion did not return the deleted mission!", returnedMission);
			Assert.assertNull("The client was able to retrieve the mission after deleting it!", verificationResponse.body);
		} else {
			client.stateEngineData_userHadPermissions = false;
		}
	}

	@Override
	public void missionSetUserRole(@NotNull AbstractUser apiUser, @NotNull String missionName, @NotNull AbstractUser user, @Nullable MissionUserRole userRole) {
		MissionUserPermission permission = MissionUserPermission.MISSION_SET_ROLE;

		ActionEngine.ActionClient client = ActionEngine.data.getState(apiUser);
		ResponseWrapper rawResponse = client.getCallResponse();
		ResponseWrapper verificationResponse = client.getVerificationCallResponse();


		if (StateEngine.data.userHasMissionPermission(apiUser, missionName, permission) && !StateEngine.data.missionDefaultRoleInUse(missionName)) {
			client.stateEngineData_userHadPermissions = true;

			Assert.assertCallReturnCode(200, rawResponse);
			Set<SubscriptionData> subscriptions = Assert.getApiSetVerificationData(200, SampleObjects.ReceivedSubscriptionData, client.getVerificationCallResponse());
			Optional<SubscriptionData> optSub = subscriptions.stream().filter(x -> user.getCotUid().equals(x.getClientUid())).findAny();
			Assert.assertTrue("The user role could not be found in the verification data!", optSub.isPresent());
			SubscriptionData sub = optSub.get();
			Assert.assertEquals("The role does not match!", userRole.name(), sub.getRole().name());
			client.stateEngineData = sub;
			//		ActionEngine.ActionClient client = ActionEngine.data.getState(user);
			//		Integer returnCode = client.getCallReturnCode();
			//		Assert.assertNotNull("Return code wasn't set! Likely framework issue!", returnCode);
			//		int returnCodeInt = returnCode;
			//		Assert.assertEquals("Call return code was not 200!", 200, returnCodeInt);
		} else {
			client.stateEngineData_userHadPermissions = false;
			if (StateEngine.data.missionDefaultRoleInUse(missionName)) {
				Assert.assertCallReturnCode(400, rawResponse);
			} else {
				Assert.assertCallReturnCode(405, rawResponse);
			}
		}
	}

	@Override
	public void missionGetChanges(@NotNull AbstractUser user, @NotNull String missionName) {
		MissionUserPermission permission = MissionUserPermission.MISSION_READ;

		ActionEngine.ActionClient client = ActionEngine.data.getState(user);
		ResponseWrapper rawResponse = client.getCallResponse();

		if (StateEngine.data.hasMissionState(missionName) && StateEngine.data.getMissionState(missionName).hasBeenDeleted()) {
			Assert.assertEquals("The deleted mission should return a 410 return code!", 410, rawResponse.responseCode);
		} else if (StateEngine.data.userHasMissionPermission(user, missionName, permission)) {
			client.stateEngineData_userHadPermissions = true;

			List<MissionChange> actual = Assert.getApiListResponseData(200, SampleObjects.MissionChange, rawResponse);
			HashMap<Pattern, Object> exceptions = new HashMap<>();
			// Cannot predict the server time
			exceptions.put(MISSIONCHANGE_SERVERTIME_PATTERN, EXCEPTION_NOT_NULL);
			exceptions.put(MISSIONCHANGE_TIMESTAMP_PATTERN, EXCEPTION_NOT_NULL);

			StateEngine.data.getMissionState(missionName).assertMissionChangesMatchExpectations(actual, exceptions);

			// TODO Missions: A way of determining changes needs to be implemented for this to really work properly...


//		TreeSet<Mission> missions = response.getMissions();
//		Assert.assertNotNull("No missions were in the response!", missions);
//		Assert.assertTrue("The mission list in the response is empty!", missions.size() != 0);
//		Assert.assertEquals("More than one mission was in the response!", 1, missions.size());
			// TODO Missions: Perform enhanced comparison of the mission changes against the existing mission state!
		} else {
			client.stateEngineData_userHadPermissions = false;
			throw new RuntimeException("The negative scenario for this case is not yet properly handled!");
		}
	}

	@Override
	public void missionSetKeywords(@NotNull AbstractUser user, @NotNull String missionName, @NotNull String... keywords) {
		MissionUserPermission permission = MissionUserPermission.MISSION_WRITE;

		ActionEngine.ActionClient client = ActionEngine.data.getState(user);
		ResponseWrapper rawResponse = client.getCallResponse();

		if (StateEngine.data.userHasMissionPermission(user, missionName, permission)) {
			client.stateEngineData_userHadPermissions = true;


//		ResponseWrapper wrapper = client.getVerificationCallResponse();
			Set<Mission> data = Assert.getApiSetResponseData(200, SampleObjects.Mission, rawResponse);
//		TreeSet<MissionChange> missionChanges = data.getMissionChanges();
			Assert.assertNotNull("No set of missions returned!", data);
			Optional<Mission> match = data.stream().filter(x -> missionName.equals(x.getUniqueStableName())).findAny();
			Assert.assertTrue("No matching mission was found!", match.isPresent());
			Mission mission = match.get();
			Assert.assertEquals("Provided keyword lists do not match!", mission.getKeywords(), new TreeSet<>(Arrays.asList(keywords)));
		} else {
			client.stateEngineData_userHadPermissions = false;
			Assert.assertCallReturnCode(405, rawResponse);
		}
	}

	@Override
	public void missionClearKeywords(@NotNull AbstractUser user, @NotNull String missionName) {
		MissionUserPermission permission = MissionUserPermission.MISSION_WRITE;

		ActionEngine.ActionClient client = ActionEngine.data.getState(user);
		ResponseWrapper rawResponse = client.getCallResponse();
		ResponseWrapper verificationResponse = client.getVerificationCallResponse();


		if (StateEngine.data.userHasMissionPermission(user, missionName, permission)) {
			client.stateEngineData_userHadPermissions = true;


			Assert.assertCallReturnCode(200, rawResponse);
			Mission verificationMission = Assert.getSingleApiSetVerificationData(200, SampleObjects.Mission, verificationResponse);
			Assert.assertEmpty("Mission still has keywords after they have been cleared!", verificationMission.getKeywords());


//		Integer returnCode = client.getCallReturnCode();
//		Assert.assertNotNull("Return code wasn't set! Likely framework issue!", returnCode);
//		int returnCodeInt = returnCode;
//		Assert.assertEquals("Call return code was not 200!", 200, returnCodeInt);
//		System.err.println("TODO: Add further mission clear keywords verification!");
		} else {
			client.stateEngineData_userHadPermissions = false;
			Assert.assertCallReturnCode(405, rawResponse);
		}
	}

	@Override
	public void missionSetPassword(@NotNull AbstractUser apiUser, @NotNull String missionName, @NotNull String password) {
		MissionUserPermission permission = MissionUserPermission.MISSION_SET_PASSWORD;

		ActionEngine.ActionClient client = ActionEngine.data.getState(apiUser);
		ResponseWrapper rawResponse = client.getCallResponse();
		ResponseWrapper verificationResponse = client.getVerificationCallResponse();

		if (StateEngine.data.userHasMissionPermission(apiUser, missionName, permission)) {
			client.stateEngineData_userHadPermissions = true;


			Assert.assertCallReturnCode(200, rawResponse);
			Mission verificationMission = Assert.getSingleApiSetVerificationData(200, SampleObjects.Mission, verificationResponse);
			Assert.assertTrue("The mission does not appear to be password protected!", verificationMission.isPasswordProtected());
			client.stateEngineData = verificationMission;
		} else {
			client.stateEngineData_userHadPermissions = false;
			Assert.assertCallReturnCode(405, rawResponse);
		}
	}
}
