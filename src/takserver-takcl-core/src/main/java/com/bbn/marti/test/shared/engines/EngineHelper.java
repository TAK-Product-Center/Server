package com.bbn.marti.test.shared.engines;

import com.bbn.marti.takcl.TestExceptions;
import com.bbn.marti.test.shared.CotGenerator;
import com.bbn.marti.test.shared.TestConnectivityState;
import com.bbn.marti.test.shared.data.GroupSetProfiles;
import com.bbn.marti.test.shared.data.servers.AbstractServerProfile;
import com.bbn.marti.test.shared.data.users.AbstractUser;
import com.bbn.marti.test.shared.engines.state.*;
import com.bbn.marti.tests.Assert;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created on 2/2/18.
 */
public class EngineHelper {

	private EngineHelper() {
	}

	// TODO: Add in support for connections from disconnected users
	public static TestConnectivityState computeExpectedUserSignOnResult(boolean doAuthIfNecessary, @NotNull AbstractUser user) {
		if (!user.isUserCredentialsValid()) {
			if (doAuthIfNecessary) {
				return TestConnectivityState.Disconnected;
			} else {
				return TestConnectivityState.ConnectedUnauthenticated;
			}
		}

		ConnectionState connection = StateEngine.data.getState(user).getConnectionState();

		TestConnectivityState expectedConnectivityState;

		if (!connection.isActiveInDeployment()) {
			expectedConnectivityState = TestConnectivityState.Disconnected;
		} else if (connection.getProfile().requiresAuthentication()) {
			if (doAuthIfNecessary) {
				if (user.isUserCredentialsValid()) {
					if (user.getActualAnonAccess() || user.getActualGroupSetAccess() != GroupSetProfiles.Set_None) {
						expectedConnectivityState = TestConnectivityState.ConnectedAuthenticatedIfNecessary;
					} else {
						expectedConnectivityState = TestConnectivityState.Disconnected;
					}
				} else {
					expectedConnectivityState = TestConnectivityState.Disconnected;
				}
			} else {
				expectedConnectivityState = TestConnectivityState.ConnectedUnauthenticated;
			}
		} else {
			expectedConnectivityState = TestConnectivityState.ConnectedAuthenticatedIfNecessary;
		}

		return expectedConnectivityState;
	}

	public static TreeMap<UserState, Set<MissionState>> computeMissionMembership(@NotNull Set<MissionState> missionStates, @NotNull UserState... users) {
		TreeMap<UserState, Set<MissionState>> results = new TreeMap<>();

		for (UserState user : users) {
			results.put(user, new TreeSet<>(missionStates.stream().filter(x ->
					user.getProfile().getActualGroupSetAccess().intersects(x.getGroupSet())).collect(Collectors.toSet())));
		}
		return results;
	}

	/**
	 * Determines which users a connecting client is expected to get a latest SA message from
	 *
	 * @param willDoAuthIfNecessary Whether or not the user will authenticate if necessary
	 * @param connectingUser        The profile of the connecting user
	 * @return The set of users the connecting client should receive latest SA from
	 */
	public static TreeSet<AbstractUser> computeExpectedLatestSASendersOnConnect(boolean willDoAuthIfNecessary, @NotNull UserState connectingUser) {
		// TODO Missions: Add Mission check
		Map<AbstractUser, TreeSet<AbstractUser>> outcomeMap = new HashMap<>();
		TreeSet<AbstractUser> latestSaSenders = new TreeSet<>();

		// If latestSA is enabled on the connecting user's server
		if (connectingUser.getServerState().isLatestSaEnabled()) {

			// For every known user
			for (UserState saUser : StateEngine.data.getUserStates()) {
				AbstractUser user = saUser.getProfile();
				// That is not the current user, validation should be done one, and has valid credentials
				if (saUser != connectingUser && user.doValidation() && user.isUserCredentialsValid()) {
					// If they can send to the connecting user and they have a latest SA value
					boolean isReachable = computeGeneralReachability(saUser, false, false, connectingUser, true, willDoAuthIfNecessary, false);
					if (isReachable && saUser.getLatestSA() != null) {
						// Add them as a sender
						latestSaSenders.add(saUser.getProfile());
					}
				}
			}
		}
		return latestSaSenders;
	}


	private static boolean computeGeneralReachability(@NotNull UserState source,
	                                                  boolean sourceWillConnectIfNecessary,
	                                                  boolean sourceWillAuthIfNecessary,
	                                                  @NotNull UserState target,
	                                                  boolean targetWillConnectIfNecessary,
	                                                  boolean targetWillAuthIfNecessary,
	                                                  boolean canSendToSelf) {
		// TODO Missions: Add reachability checks

		if (source == target && !canSendToSelf) {
			return false;
		}

		boolean sourceCanSend = source.canSendInFuture(sourceWillConnectIfNecessary, sourceWillAuthIfNecessary);
		boolean targetCanReceive = target.canReceiveInFuture(targetWillConnectIfNecessary, targetWillAuthIfNecessary);

		boolean sameServer = source.getServerState().equals(target.getServerState());

		if (sourceCanSend) {
			if (targetCanReceive) {

				if (sameServer) {
					return computeLocalReachability(source, target);

				} else {
					return isFederatedDirectionallyReachable(source, target) ||
							isSubscriptionDirectionallyReachable(source, target);
				}
			}
		}

		return false;
	}

//    public static boolean canReceiveInFuture(UserState user, boolean willConnectIfNecessary, boolean willAuthIfNecessary) {
//        if (user.getConnectivityState() == TestConnectivityState.SendOnly) {
//            return false;
//        } else if (user.getConnectivityState() == TestConnectivityState.ReceiveOnly) {
//            return true;
//        }
//
//        boolean canCurrentlyReceive = canCurrentlyReceive();
//
//        if (canCurrentlyReceive) {
//            return true;
//        } else {
//            return willBeConnectedInFuture(willConnectIfNecessary, willAuthIfNecessary);
//        }
//    }
//    
//    private static boolean canSendInFuture(UserState user, boolean willConnectIfNecessary, boolean willAuthIfNecessary) {
//            if (user.getConnectivityState() == TestConnectivityState.ReceiveOnly) {
//                return false;
//            } else if (user.getConnectivityState() == TestConnectivityState.SendOnly) {
//                return true;
//            }
//
//            boolean canCurrentlySend = canCurrentlySend();
//
//            if (canCurrentlySend) {
//                return true;
//            } else {
//                return willBeConnectedInFuture(willConnectIfNecessary, willAuthIfNecessary);
//            }
//    }
//
//    public static boolean willBeConnectedInFuture(UserState user, boolean willConnectIfNecessary, boolean willAuthIfNecessary) {
//        if (!canPotentiallyConnect()) {
//            return false;
//        }
//
//        if (isConnected() || willConnectIfNecessary) {
//            if (needsAuthentication()) {
//                if (willAuthIfNecessary) {
//                    return true;
//                } else {
//                    return false;
//                }
//            } else {
//                return true;
//            }
//        } else {
//            return false;
//        }
//    }

	/**
	 * Determines the reachability between two users on the same server
	 *
	 * @param source The user sending a message
	 * @param target The user potentially receiving the message
	 * @return Whether or not the message is expected to be received
	 */
	private static boolean computeLocalReachability(@NotNull UserState source,
	                                                @NotNull UserState target) {
		if (source.getServerState() != target.getServerState()) {
			String msg = "Cannot compute reachability for the users '" +
					source.getProfile().getConsistentUniqueReadableIdentifier() + "' and '" +
					target.getProfile().getConsistentUniqueReadableIdentifier() + "' on different servers!";
			Assert.fail(msg);
			throw new RuntimeException(msg);
		}

		// Through their input group assignments and auth-based assignments, do they share group access locally?
		boolean localGroupSetAccess = source.getProfile().getActualGroupSetAccess().intersects(target.getProfile().getActualGroupSetAccess());

		// Through their input group assignments and auth-based assignments, do they share anon access locally?
		boolean localAnonAccess = source.getProfile().getActualAnonAccess() && target.getProfile().getActualAnonAccess();

		return (localGroupSetAccess || localAnonAccess);
	}


	private static boolean hasIntersection(Set<String> setA, Set<String> setB) {
		for (String value : setA) {
			if (setB.contains(value)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isFederatedDirectionallyReachable(@NotNull UserState sourceUser, @NotNull UserState targetUser) {
		ServerState sourceServerState = sourceUser.getServerState();
		ServerState targetServerState = targetUser.getServerState();

		AbstractServerProfile sourceServer = sourceServerState.getProfile();
		AbstractServerProfile targetServer = targetServerState.getProfile();

		// If the source server is federated
		return sourceServerState.federation.isFederated() &&
				// And the target server is federated
				targetServerState.federation.isFederated() &&
				// And one of them has a connection to the other
				(sourceServerState.federation.isOutgoingConnection(targetServer) || targetServerState.federation.isOutgoingConnection(sourceServer)) &&
				// And both users are members of some groups
				targetUser.getProfile().getActualGroupSetAccess().groupSet != null && sourceUser.getProfile().getActualGroupSetAccess().groupSet != null &&
				// And the source user's groups intersect with the source server to target server outbound federate groups
				hasIntersection(sourceUser.getProfile().getActualGroupSetAccess().groupSet, sourceServerState.federation.getFederateState(targetServer).getOutboundGroups()) &&
				// And the target user's groups intersect with the source server to target server inbound federate groups
				hasIntersection(targetUser.getProfile().getActualGroupSetAccess().groupSet, targetServerState.federation.getFederateState(sourceServer).getInboundGroups());
	}


	private static boolean isSubscriptionDirectionallyReachable(@NotNull UserState sourceUser, @NotNull UserState targetUser) {
		ServerState sourceServer = sourceUser.getServerState();
		ServerState targetServer = targetUser.getServerState();

		return (sourceServer.isServerSubscriptionTarget(targetServer.getProfile()) &&
				sourceUser.getProfile().getActualAnonAccess() && targetUser.getProfile().getActualAnonAccess());
	}


	public static Map<AbstractUser, TreeSet<AbstractUser>> computeExpectedRecipientsOfUserSend(
			@NotNull UserState sourceUser, boolean willConnectIfNecessary, boolean willAuthIfNecessary,
			@NotNull UserIdentificationData targetUserIdentificationData, @NotNull TreeSet<UserState> targetUsers) {
		// TODO Missions: Add Missions reachability check

		Map<AbstractUser, TreeSet<AbstractUser>> outcomeMap = new HashMap<>();

		if (targetUsers.size() == 0) {
			for (UserState possibleRecipient : StateEngine.data.getUserStates()) {
				boolean generalReachability = computeGeneralReachability(sourceUser, willConnectIfNecessary, willAuthIfNecessary, possibleRecipient, false, false, false);

				if (!outcomeMap.containsKey(possibleRecipient.getProfile())) {
					outcomeMap.put(possibleRecipient.getProfile(), new TreeSet<AbstractUser>());
				}

				if (generalReachability) {
					outcomeMap.get(possibleRecipient.getProfile()).add(sourceUser.getProfile());
				}
			}

		} else {
			for (UserState targetUser : targetUsers) {
				boolean generalReachability = computeGeneralReachability(sourceUser, willConnectIfNecessary, willAuthIfNecessary, targetUser, false, false, true);

				if (generalReachability &&
						(targetUserIdentificationData == UserIdentificationData.UID ||
								targetUserIdentificationData == UserIdentificationData.UID_AND_CALLSIGN) &&
						sourceUser.getServerState().isUserUidKnown(targetUser)) {
					if (!outcomeMap.containsKey(targetUser.getProfile())) {
						outcomeMap.put(targetUser.getProfile(), new TreeSet<AbstractUser>());
					}
					outcomeMap.get(targetUser.getProfile()).add(sourceUser.getProfile());
				}
				if (generalReachability &&
						(targetUserIdentificationData == UserIdentificationData.CALLSIGN ||
								targetUserIdentificationData == UserIdentificationData.UID_AND_CALLSIGN) &&
						sourceUser.getServerState().isUserCallsignKnown(targetUser)) {
					if (!outcomeMap.containsKey(targetUser.getProfile())) {
						outcomeMap.put(targetUser.getProfile(), new TreeSet<AbstractUser>());
					}
					outcomeMap.get(targetUser.getProfile()).add(sourceUser.getProfile());
				}
			}

			for (UserState us : StateEngine.data.getUserStates()) {
				if (!outcomeMap.containsKey(us.getProfile())) {
					outcomeMap.put(us.getProfile(), new TreeSet<AbstractUser>());
				}
			}
		}
		return outcomeMap;
	}

	public static Map<AbstractUser, TreeSet<AbstractUser>> computeDisconnectionLatestSACount
			(@NotNull TreeSet<UserState> disconnectingUsers) {

		Map<AbstractUser, TreeSet<AbstractUser>> outcomeMap = new HashMap<>();

//        List<AbstractUser> disconnectingUserList = Arrays.asList(userList);


		for (UserState saClient : StateEngine.data.getUserStates()) {
			if (saClient.getServerState().isLatestSaEnabled()) {
				TreeSet<AbstractUser> receiveList = new TreeSet<>();
				outcomeMap.put(saClient.getProfile(), receiveList);

				for (UserState disconnectingUser : disconnectingUsers) {
					if (saClient != disconnectingUser && disconnectingUser.getLatestSA() != null & !disconnectingUsers.contains(saClient)) {
						boolean isReachable = computeGeneralReachability(disconnectingUser, false, false, saClient, false, false, false);
						if (isReachable) {
							receiveList.add(disconnectingUser.getProfile());
						}
					}
				}
			}
		}
		return outcomeMap;
	}

	public static TreeSet<AbstractUser> extractSendersWithCallsigns(List<String> receivedMessages) {
		TreeSet<AbstractUser> rval = new TreeSet<>();
		for (String msg : receivedMessages) {
			String callsign = CotGenerator.parseCallsign(msg);
			for (UserState candidate : StateEngine.data.getUserStates()) {
				if (candidate.getProfile().getCotCallsign().equals(callsign)) {
					rval.add(candidate.getProfile());
				}
			}
		}
		return rval;
	}

	public static TreeSet<AbstractUser> extractSendersWithUids(List<String> receivedMessages) {
		TreeSet<AbstractUser> rval = new TreeSet<>();
		for (String msg : receivedMessages) {
			String uid = CotGenerator.parseClientUID(msg);
			for (UserState candidate : StateEngine.data.getUserStates()) {
				if (candidate.getProfile().getCotUid().equals(uid)) {
					rval.add(candidate.getProfile());
				}
			}
		}
		return rval;
	}
}
