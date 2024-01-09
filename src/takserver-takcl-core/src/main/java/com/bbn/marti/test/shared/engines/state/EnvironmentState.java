package com.bbn.marti.test.shared.engines.state;

import com.bbn.marti.test.shared.data.GroupSetProfiles;
import com.bbn.marti.test.shared.data.connections.AbstractConnection;
import com.bbn.marti.test.shared.data.servers.AbstractServerProfile;
import com.bbn.marti.test.shared.data.users.AbstractUser;
import com.bbn.marti.test.shared.engines.ActionEngine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import static com.bbn.marti.takcl.connectivity.missions.MissionModels.Mission;
import static com.bbn.marti.takcl.connectivity.missions.MissionModels.MissionUserPermission;
import static com.bbn.marti.takcl.connectivity.missions.MissionModels.MissionUserRole;

/**
 * Created on 1/19/18.
 */
public class EnvironmentState {

	private final TreeMap<String, MissionState> missionNameStateMap = new TreeMap<>();

	private final TreeMap<String, ServerState> serverIdentifierStateMap = new TreeMap<>();
	private final TreeMap<String, UserState> userIdentifierStateMap = new TreeMap<>();
	private final TreeMap<String, ConnectionState> connectionIdentifierStateMap = new TreeMap<>();
	private final TreeMap<String, byte[]> enterpriseSyncFileData = new TreeMap<>();
	private final HashSet<String> enterpriseSyncDeletedHashes = new HashSet<>();

	static EnvironmentState instance = new EnvironmentState();

	private EnvironmentState() {
	}

	static void warn(String string) {
		System.err.println(string);
	}

	public synchronized ConnectionState getState(AbstractConnection connection) {
		String connectionIdentifier = connection.getConsistentUniqueReadableIdentifier();
		ConnectionState cs;
		synchronized (connectionIdentifierStateMap) {
			cs = connectionIdentifierStateMap.get(connectionIdentifier);
			if (cs == null) {
				cs = new ConnectionState(connection);
				connectionIdentifierStateMap.put(connectionIdentifier, cs);
			}
		}
		return cs;
	}

	public synchronized ServerState getState(AbstractServerProfile serverProfile) {
		ServerState ss;
		synchronized (serverIdentifierStateMap) {
			ss = serverIdentifierStateMap.get(serverProfile.getConsistentUniqueReadableIdentifier());
			if (ss == null) {
				ss = new ServerState(serverProfile);
				serverIdentifierStateMap.put(serverProfile.getConsistentUniqueReadableIdentifier(), ss);
			}
		}
		return ss;
	}

	public synchronized UserState getState(AbstractUser user) {
		String userIdentifier = user.getConsistentUniqueReadableIdentifier();
		UserState us;
		synchronized (userIdentifierStateMap) {
			us = userIdentifierStateMap.get(userIdentifier);
			if (us == null) {
				us = new UserState(user);
				userIdentifierStateMap.put(userIdentifier, us);
			}
		}
		return us;
	}

	synchronized void factoryReset() {
		serverIdentifierStateMap.clear();
		userIdentifierStateMap.clear();
		connectionIdentifierStateMap.clear();
		missionNameStateMap.clear();
	}

	public synchronized TreeSet<UserState> getUserStates(AbstractServerProfile server) {
		TreeSet<UserState> userStates = new TreeSet<>();
		for (UserState state : userIdentifierStateMap.values()) {
			if (state.getProfile().getServer().equals(server)) {
				userStates.add(state);
			}
		}
		return userStates;
	}

	public synchronized TreeSet<UserState> getUserStates(AbstractConnection connection) {
		TreeSet<UserState> userStates = new TreeSet<>();
		for (UserState state : userIdentifierStateMap.values()) {
			if (state.getProfile().getConnection().equals(connection)) {
				userStates.add(state);
			}
		}
		return userStates;
	}


	public synchronized TreeSet<AbstractUser> getUsers(AbstractServerProfile server) {
		TreeSet<AbstractUser> users = new TreeSet<>();
		for (UserState state : userIdentifierStateMap.values()) {
			if (state.getProfile().getServer().equals(server)) {
				users.add(state.getProfile());
			}
		}
		return users;
	}


	public synchronized TreeSet<AbstractConnection> getConnections(AbstractServerProfile profile) {
		TreeSet<AbstractConnection> rval = new TreeSet<>();

		synchronized (connectionIdentifierStateMap) {
			for (ConnectionState state : connectionIdentifierStateMap.values()) {
				if (state.getProfile().getServer().equals(profile)) {
					rval.add(state.profile);
				}
			}
		}
		return rval;
	}

	public synchronized TreeSet<ConnectionState> getConnectionStates(AbstractServerProfile profile) {
		TreeSet<ConnectionState> rval = new TreeSet<>();

		synchronized (connectionIdentifierStateMap) {
			for (ConnectionState state : connectionIdentifierStateMap.values()) {
				if (state.getProfile().getServer().equals(profile)) {
					rval.add(state);
				}
			}
		}
		return rval;
	}

	public synchronized TreeSet<AbstractUser> getUsers(AbstractConnection connection) {
		TreeSet<AbstractUser> users = new TreeSet<>();
		for (UserState state : userIdentifierStateMap.values()) {
			if (state.getProfile().getConnection().equals(connection)) {
				users.add(state.getProfile());
			}
		}
		return users;
	}

	public TreeSet<UserState> getUserStates(@NotNull TreeSet<AbstractUser> users) {
		TreeSet<UserState> rval = new TreeSet<>();
		for (AbstractUser u : users) {
			rval.add(getState(u));
		}
		return rval;
	}

	public synchronized TreeSet<AbstractUser> getUsers() {
		TreeSet<AbstractUser> userSet = new TreeSet<>();

		for (UserState state : userIdentifierStateMap.values()) {
			userSet.add(state.getProfile());
		}
		return userSet;
	}

	public UserState getUserState(@NotNull String userIdentifier) {
		return userIdentifierStateMap.get(userIdentifier);
	}

	public synchronized List<AbstractUser> getUsersThatCanCurrentlySend() {
		List<AbstractUser> sendUsers = new LinkedList<>();

		for (UserState userState : getUserStates()) {

			if (userState.canCurrentlySend()) {
				sendUsers.add(userState.getProfile());
			}

		}
		return sendUsers;
	}

	public synchronized TreeSet<UserState> getUserStates() {
		return new TreeSet<>(userIdentifierStateMap.values());
	}

	public synchronized TreeSet<ServerState> getServerStates() {
		return new TreeSet<>(serverIdentifierStateMap.values());
	}

	public synchronized TreeSet<ConnectionState> getConnectionStates() {
		return new TreeSet<>(connectionIdentifierStateMap.values());
	}

	protected synchronized void updateKnownCallsignAndUidState() {
		for (UserState recipientState : getUserStates()) {

			AbstractUser recipientUser = recipientState.getProfile();

			ActionEngine.ActionClient recipientClientState = ActionEngine.data.getState(recipientUser);

			TreeSet<String> senderUids = recipientClientState.getReceivedSenderUids();
			TreeSet<String> senderCallsigns = recipientClientState.getReceivedSenderCallsigns();

			for (UserState potentialSenderState : getUserStates()) {
				AbstractUser potentialUser = potentialSenderState.getProfile();
				if (senderUids.contains(potentialUser.getCotUid())) {
					getState(recipientUser.getServer()).setUserUidKnown(potentialUser);
				}
				if (senderCallsigns.contains(potentialUser.getCotCallsign())) {
					getState(recipientUser.getServer()).setUserCallsignknown(potentialUser);
				}
			}
		}
	}

	public synchronized MissionState getMissionState(String missionName) {
		return missionNameStateMap.getOrDefault(missionName, null);
	}

	public synchronized boolean hasMissionState(String missionName) {
		return missionNameStateMap.containsKey(missionName);
	}


	public synchronized TreeSet<MissionState> getMissionStates() {
		return new TreeSet<>(missionNameStateMap.values());
	}

	protected synchronized void addMission(@NotNull AbstractUser missionOwner, @NotNull String missionName,
	                                    @NotNull GroupSetProfiles groupSetProfile, @NotNull Mission mission,
	                                       @Nullable MissionUserRole defaultMissionRole, @Nullable String password) {
		// TODO Missions: This seems to be the only way to modify groups. Only updating the existing mission if the groups have changed
		// Due to permissions changing what a user can see in the mission, replacing the mission wholesale is something I am hesitant to do...
		if (missionNameStateMap.containsKey(missionName)) {
			missionNameStateMap.get(missionName).setGroupSetProfile(groupSetProfile);;
		} else {
			MissionState missionState =  new MissionState(missionOwner, missionName, groupSetProfile, mission, defaultMissionRole, password);
			missionNameStateMap.put(missionName, missionState);
		}
	}

	protected synchronized void removeMission(@NotNull String missionName) {
		MissionState state = missionNameStateMap.get(missionName);
		state.setDeleted();
	}

	public boolean enterpriseSyncDataDeleted(@NotNull String hash) {
		return enterpriseSyncDeletedHashes.contains(hash);
	}

	@Nullable
	public byte[] getEnterpriseSyncData(@NotNull String hash) {
		if (enterpriseSyncFileData.containsKey(hash)) {
			byte[] data = enterpriseSyncFileData.get(hash);
			return Arrays.copyOf(data, data.length);
		} else {
			return null;
		}
	}

	protected synchronized void fileAdd(@NotNull String hash, @NotNull byte[] data) {
		enterpriseSyncFileData.put(hash, data);
	}

	protected synchronized void fileDelete(@NotNull String hash) {
		enterpriseSyncFileData.remove(hash);
		enterpriseSyncDeletedHashes.add(hash);
	}

	protected synchronized void updateState() {
		System.out.println("--- ActionEngine.data.getAllClients().size: " + ActionEngine.data.getAllClients().size());

		for (ActionEngine.ActionClient client : ActionEngine.data.getAllClients()) {
			
			System.out.println("\t--- EnvironmentState updateState client: " + client + ". Will updateConnectivityState userstate to: "+client.getConnectivityState());

			UserState user = getState(client.getProfile());
			user.updateLatestSA(client.getLatestSA());

//            TestConnectivityState newUserState = client.getConnectivityState();
//            TestConnectivityState oldUserState = user.getConnectivityState();
//            ConnectionState connectionState = user.getConnectionState();
//            boolean isConnectionActiveOnServer = user.getConnectionState().isActiveInDeployment();
//
//            if (newUserState == TestConnectivityState.Disconnected) {
//                user.updateConnectivityState(newUserState);
//
//                switch (newUserState) {
//                    case Disconnected:
//                        user.updateConnectivityState(newUserState);
//                        break;
//
//                    case ConnectedAuthenticatedIfNecessary:
//                        if (connectionState.isActiveInDeployment()) {
//                            user.updateConnectivityState(newUserState);
//                        } else {
//                            user.updateConnectivityState(TestConnectivityState.ConnectedAuthenticatedIfNecessaryConnectionRemoved);
//                        }
//                        break;
//
//                    case ConnectedUnauthenticated:
//                        if (!isConnectionActiveOnServer) {
//                            throw new RuntimeException("Validation of Connected Authing clients that have not authenticated yet on a recently disconnected input is not yet supported!");
//                        } else {
//                            user.updateConnectivityState(newUserState);
//                        }
//                        break;
//
//                    case ConnectedCannotAuthenticate:
//                        if (!isConnectionActiveOnServer) {
//                            throw new RuntimeException("Validation of Connected Authing clients that cannot authenticate on a recently disconnected input is not yet supported!");
//                        } else {
//                            user.updateConnectivityState(newUserState);
//                        }
//                        break;
//
//                    case ConnectedAuthenticatedIfNecessaryConnectionRemoved:
//                    case SendOnly:
//                    case ReceiveOnly:
//                        user.updateConnectivityState(newUserState);
//                        break;
//                }
//            }

			user.updateConnectivityState(client.getConnectivityState());
		}
	}

	public boolean userHasMissionPermission(@NotNull AbstractUser apiUser, @NotNull String missionName, @Nullable MissionUserPermission permission) {
		UserState userState = getState(apiUser);
		return userState.isAdmin() || (permission != null && getMissionState(missionName).getUserRole(apiUser).hasPermission(permission));
	}

	public boolean missionDefaultRoleInUse(@NotNull String missionName) {
		return getMissionState(missionName).isDefaultRoleInUse();
	}
}
