package com.bbn.marti.test.shared.engines.state;

import com.bbn.marti.test.shared.data.connections.AbstractConnection;
import com.bbn.marti.test.shared.data.servers.AbstractServerProfile;
import com.bbn.marti.test.shared.data.users.AbstractUser;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created on 2/1/18.
 */
public class ServerState implements Comparable<ServerState> {
	private final AbstractServerProfile profile;
	private final AtomicBoolean isDeployed;

	// THe subscription targets this server has
	private final TreeSet<AbstractConnection> serverSubscriptionTargets = new TreeSet<>();

	// The users with UIDs the server can specifically route to
	private final TreeSet<AbstractUser> knownUserUids = new TreeSet<>();

	// The users with callsigns the server can specifically route to
	private final TreeSet<AbstractUser> knownUserCallsigns = new TreeSet<>();

	private UserAuthenticationFileState userAuthenticationFileState;

	@Override
	public int compareTo(ServerState o) {
		if (o == null) {
			return 1;
		} else {
			return (profile.getConsistentUniqueReadableIdentifier().compareTo(o.getProfile().getConsistentUniqueReadableIdentifier()));
		}
	}

	// todo: default?
	// Whether or not latest SA is enabled
	private boolean latestSaEnabled;

	public final FederationState federation;


	ServerState(AbstractServerProfile profile) {
		this.profile = profile;
		this.federation = new FederationState(profile);
		this.isDeployed = new AtomicBoolean(false);
		this.userAuthenticationFileState = new UserAuthenticationFileState(profile);
	}

	public AbstractServerProfile getProfile() {
		return profile;
	}

	public boolean isActiveInDeployment() {
		return isDeployed.get();
	}

	void setActiveInDeployment() {
		isDeployed.set(true);

	}

	public UserAuthenticationFileState getUserAuthenticationFileState() {
		return userAuthenticationFileState;
	}

	void setInactiveInDeployment() {
		isDeployed.set(false);
	}

	public boolean isLatestSaEnabled() {
		return latestSaEnabled;
	}

	void setLatestSaEnabled(boolean value) {
		latestSaEnabled = value;
	}

	void setUserUidKnown(AbstractUser user) {
		knownUserUids.add(user);
	}

	public boolean isUserUidKnown(UserState user) {
		return knownUserUids.contains(user.getProfile());
	}

	void setUserCallsignknown(AbstractUser user) {
		knownUserCallsigns.add(user);
	}

	public boolean isUserCallsignKnown(UserState user) {
		return knownUserCallsigns.contains(user.getProfile());
	}

	void addConnectionSubscriptionTarget(AbstractConnection connection) {
		serverSubscriptionTargets.add(connection);
	}

	public TreeSet<ConnectionState> getConnectionStates() {
		return new TreeSet<ConnectionState>(StateEngine.data.getConnectionStates(profile));
	}

	public TreeSet<UserState> getUserStates() {
		return new TreeSet<UserState>(StateEngine.data.getUserStates(profile));
	}


	public TreeSet<AbstractConnection> getConnections() {
		return StateEngine.data.getConnections(profile);
	}

	public TreeSet<AbstractUser> getUsers() {
		return StateEngine.data.getUsers(profile);

	}

	boolean isConnectionSubscriptionTarget(AbstractConnection connection) {
		return serverSubscriptionTargets.contains(connection);
	}

	public boolean isServerSubscriptionTarget(AbstractServerProfile server) {
		for (AbstractConnection c : serverSubscriptionTargets) {
			if (c.getServer().equals(server)) {
				return true;
			}
		}
		return false;
	}

	public static class FederationState {

		private final AbstractServerProfile hostServer;


		// Is it federated?
		private boolean federated;

		// The servers this server has a federated outbound connection to
		private final TreeSet<AbstractServerProfile> federatedOutboundConnections = new TreeSet<>();

		// The state of the federate's this server is connected to
		private final TreeMap<AbstractServerProfile, FederationState.FederateState> federateStates = new TreeMap<>();


		FederationState(AbstractServerProfile hostServer) {
			this.hostServer = hostServer;
		}


		public boolean isOutgoingConnection(AbstractServerProfile targetServer) {
			return federatedOutboundConnections.contains(targetServer);

		}

		void addOutgoingConnection(AbstractServerProfile targetServer) {
			federatedOutboundConnections.add(targetServer);

		}

		public FederateState getFederateState(AbstractServerProfile federate) {
			if (!federateStates.keySet().contains(federate)) {
				EnvironmentState.warn("The ServerState for '" + hostServer.getConsistentUniqueReadableIdentifier() + "' does not contain the federate '" + federate.getConsistentUniqueReadableIdentifier() + "'!");
			}
			return federateStates.get(federate);
		}

		void setFederated() {
			this.federated = true;
		}

		public boolean isFederated() {
			return this.federated;
		}

		public boolean isFederate(AbstractServerProfile federateProfile) {
			return federateStates.containsKey(federateProfile);
		}

		void addFederate(AbstractServerProfile federateProfile) {
			federateStates.put(federateProfile, new FederationState.FederateState(hostServer, federateProfile));
		}

		/**
		 * The state of a federate
		 */
		public static class FederateState {
			private final AbstractServerProfile federatedServer;
			private final AbstractServerProfile federateServer;
			private final Set<String> outboundGroups = new HashSet<>();
			private final Set<String> inboundGroups = new HashSet<>();

			FederateState(AbstractServerProfile federatedServer, AbstractServerProfile federateServer) {
				this.federatedServer = federatedServer;
				this.federateServer = federateServer;
			}

			public boolean isOutboundGroup(String groupIdentifier) {
				return outboundGroups.contains(groupIdentifier);
			}

			void addOutboundGroup(String groupIdentifier) {
				if (isOutboundGroup(groupIdentifier)) {
					EnvironmentState.warn("The ServerState for '" + federatedServer + "' with federate '" + federateServer + "' already contains an outbound group '" + groupIdentifier + "'!");
				} else {
					outboundGroups.add(groupIdentifier);
				}
			}


			public boolean isInboundGroup(String groupIdentifier) {
				return inboundGroups.contains(groupIdentifier);
			}

			void addInboundGroup(String groupIdentifier) {
				if (isInboundGroup(groupIdentifier)) {
					EnvironmentState.warn("The ServerState for '" + federatedServer + "' with federate '" + federateServer + "' already contains an inbound group '" + groupIdentifier + "'!");
				} else {
					inboundGroups.add(groupIdentifier);
				}
			}

			public TreeSet<String> getOutboundGroups() {
				return new TreeSet<>(outboundGroups);
			}

			public TreeSet<String> getInboundGroups() {
				return new TreeSet<>(inboundGroups);
			}
		}
	}


	public String toString() {
		return profile.getConsistentUniqueReadableIdentifier();
	}

}
