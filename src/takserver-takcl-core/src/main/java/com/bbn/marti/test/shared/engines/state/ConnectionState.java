package com.bbn.marti.test.shared.engines.state;

import com.bbn.marti.test.shared.data.connections.AbstractConnection;
import com.bbn.marti.test.shared.data.users.AbstractUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created on 2/1/18.
 */
public class ConnectionState implements Comparable<ConnectionState> {

	final AbstractConnection profile;

	private final AtomicBoolean isDeployed;

	@Override
	public int compareTo(@NotNull ConnectionState o) throws NullPointerException, ClassCastException {
		return profile.getConsistentUniqueReadableIdentifier().compareTo(o.getProfile().getConsistentUniqueReadableIdentifier());
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		return (obj != null) && (obj instanceof ConnectionState) && (compareTo((ConnectionState) obj) == 0);
	}

	ConnectionState(AbstractConnection connectionProfile) {
		profile = connectionProfile;
		isDeployed = new AtomicBoolean(false);
	}

	public AbstractConnection getProfile() {
		return profile;
	}

	public boolean isActiveInDeployment() {
		return isDeployed.get();
	}

	void setActiveInDeployment() {
		isDeployed.set(true);
	}

	void setInactiveInDeployment() {
		isDeployed.set(false);
	}

	public ServerState getServerState() {
		return StateEngine.data.getState(profile.getServer());
	}

	public TreeSet<UserState> getUserStates() {
		return new TreeSet<>(StateEngine.data.getUserStates(this.profile));
	}

	public TreeSet<AbstractUser> getUsers() {
		return StateEngine.data.getUsers(profile);
	}

	public String toString() {
		return profile.getConsistentUniqueReadableIdentifier();
	}

}
