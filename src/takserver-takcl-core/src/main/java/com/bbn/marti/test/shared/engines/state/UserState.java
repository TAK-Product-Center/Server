package com.bbn.marti.test.shared.engines.state;

import com.bbn.marti.config.AuthType;
import com.bbn.marti.test.shared.TestConnectivityState;
import com.bbn.marti.test.shared.data.connections.AbstractConnection;
import com.bbn.marti.test.shared.data.servers.AbstractServerProfile;
import com.bbn.marti.test.shared.data.users.AbstractUser;
import com.bbn.marti.test.shared.engines.ActionEngine;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created on 2/1/18.
 */
public class UserState implements Comparable<UserState> {
	private final AbstractConnection connection;
	private final AbstractServerProfile server;
	private final AbstractUser profile;
	private final AtomicBoolean isDeployed;
	private TestConnectivityState connectivityState;
	private String latestSA;
	private boolean isAdmin;

	UserState(AbstractUser profile) {
		this.profile = profile;
		this.connection = profile.getConnection();
		this.server = profile.getServer();
		this.isDeployed = new AtomicBoolean(false);
		this.connectivityState = ActionEngine.data.getState(profile).getConnectivityState();
	}

	public TestConnectivityState getConnectivityState() {
		return connectivityState;
	}

	public void updateConnectivityState(TestConnectivityState connectivityState) {
		this.connectivityState = connectivityState;
	}

	public AbstractUser getProfile() {
		return profile;
	}

	public boolean isActiveInDeployment() {
        return isDeployed.get();
	}


	public boolean isAdmin() {
		return isAdmin;
	}

	public void overrideAdminStatus(boolean isAdmin) {
		this.isAdmin = isAdmin;
	}

	void setActiveInDeployment() {
		isDeployed.set(true);
	}

	void setInactiveInDeployment() {
		isDeployed.set(false);
	}

	public ConnectionState getConnectionState() {
		return StateEngine.data.getState(connection);
	}

	public ServerState getServerState() {
		return StateEngine.data.getState(server);
	}

	@Override
	public int compareTo(UserState o) {
		if (o == null) {
			return 1;
		} else {
			return (profile.getConsistentUniqueReadableIdentifier().compareTo(o.getProfile().getConsistentUniqueReadableIdentifier()));
		}
	}

	final synchronized boolean canCurrentlySend() {
		// TODO: check isConnected?
		if (!isDeployed.get()) {
			return false;
		}
		if (connection.getProtocol().clientConnectionSeveredWithInputRemoval() &&
				!getConnectionState().isActiveInDeployment()) {
			return false;
		}

		switch (getConnectivityState()) {
			case Disconnected:
			case ConnectedUnauthenticated:
			case ReceiveOnly:
				return false;

			case ConnectedAuthenticatedIfNecessary:
			case SendOnly:
				return true;

			default:
				throw new RuntimeException("Unhandled Connectivity State \'" + getConnectivityState().toString() + "'!");
		}
	}

	/**
	 * Determines if the user can receive data based on the {@link TestConnectivityState}
	 *
	 * @return True if the user does not need authentication or has authenticated
	 */
	public final boolean isCurrentlyAvailable() {
		return (canCurrentlySend() || canCurrentlyReceive());
	}

	// TODO: Add concept of connection flow to clearer differentiate between user behavior on input disconnection and user behavior with no input

	final synchronized boolean canCurrentlyReceive() {
		if (!isDeployed.get()) {
			return false;
		}
		// TODO: Check isConnected?
		if (connection.getProtocol().clientConnectionSeveredWithInputRemoval() &&
				!getConnectionState().isActiveInDeployment()) {
			return false;
		}

		switch (getConnectivityState()) {
			case ConnectedAuthenticatedIfNecessary:
			case ReceiveOnly:
				return true;

			case Disconnected:
			case ConnectedUnauthenticated:
			case SendOnly:
				return false;

			default:
				throw new RuntimeException("Unhandled Connectivity State \'" + getConnectivityState().toString() + "'!");
		}
	}


	public final boolean canSendInFuture(boolean willConnectIfNecessary, boolean willAuthIfNecessary) {
		if (getConnectivityState() == TestConnectivityState.ReceiveOnly) {
			return false;
		} else if (getConnectivityState() == TestConnectivityState.SendOnly && getConnectionState().isActiveInDeployment()) {
			return true;
		}

		boolean canCurrentlySend = canCurrentlySend();

		if (canCurrentlySend) {
			return true;
		} else {
			return willBeConnectedInFuture(willConnectIfNecessary, willAuthIfNecessary);
		}
	}

	public synchronized final boolean willBeConnectedInFuture(boolean willConnectIfNecessary, boolean willAuthIfNecessary) {
		if (!canPotentiallyConnect()) {
			return false;
		}

		if ((isConnected() && isDeployed.get()) || willConnectIfNecessary) {
			if (needsAuthentication()) {
				if (willAuthIfNecessary) {
					return true;
				} else {
					return false;
				}
			} else {
				return true;
			}
		} else {
			return false;
		}
	}

	public synchronized final boolean canReceiveInFuture(boolean willConnectIfNecessary, boolean willAuthIfNecessary) {
		if (getConnectivityState() == TestConnectivityState.SendOnly) {
			return false;
		} else if (getConnectivityState() == TestConnectivityState.ReceiveOnly && getConnectionState().isActiveInDeployment()) {
			return true;
		}

		boolean canCurrentlyReceive = canCurrentlyReceive();

		if (canCurrentlyReceive && isDeployed.get()) {
			return true;
		} else {
			return willBeConnectedInFuture(willConnectIfNecessary, willAuthIfNecessary);
		}
	}

	protected synchronized final boolean needsAuthentication() {
		return connection.getAuthType() != AuthType.ANONYMOUS &&
				getConnectivityState() != TestConnectivityState.ConnectedAuthenticatedIfNecessary;
	}

	private synchronized boolean isConnected() {
		return getConnectivityState() == TestConnectivityState.ConnectedAuthenticatedIfNecessary ||
				getConnectivityState() == TestConnectivityState.ConnectedUnauthenticated;
	}


	final boolean canPotentiallyConnect() {
		return connection.getProtocol().canConnect() && getConnectionState().isActiveInDeployment();
	}

	public String toString() {
		return profile.getConsistentUniqueReadableIdentifier();
	}

	public synchronized String getLatestSA() {
		return latestSA;
	}

	synchronized void updateLatestSA(@Nullable String latestSA) {
		this.latestSA = latestSA;
	}
}
