package com.bbn.marti.test.shared.data.connections;

import com.bbn.marti.config.AuthType;
import com.bbn.marti.config.Network;
import com.bbn.marti.config.Subscription;
import com.bbn.marti.test.shared.data.GroupSetProfiles;
import com.bbn.marti.test.shared.data.protocols.ProtocolProfiles;
import com.bbn.marti.test.shared.data.servers.AbstractServerProfile;
import com.bbn.marti.test.shared.data.users.AbstractUser;
import com.bbn.marti.test.shared.data.users.UserFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Set;

/**
 * Created on 3/4/16.
 */
public abstract class AbstractConnection implements Comparable<Object>, Comparator<Object> {

	/**
	 * Gets the type of connection
	 *
	 * @return The connection type
	 */
	public final ProtocolProfiles.ConnectionType getConnectionType() {
		return getProtocol().getConnectionType();
	}

	/**
	 * Gets the resultant anon access of this connection
	 *
	 * @return The resultant anon access
	 */
	public final boolean getAnonAccess() {
		return getAnonAccess(getAuthType(), getGroupSet(), getRawAnonAccessFlag());
	}


	public static boolean getAnonAccess(@NotNull AuthType authType, @NotNull GroupSetProfiles groupSet, @Nullable Boolean isAnon) {
		if (authType == AuthType.ANONYMOUS) {
			if (isAnon == null) {
				return (groupSet == GroupSetProfiles.Set_None);

			} else {
				return isAnon;
			}

		} else {
			return false;
		}
	}

	/**
	 * Generates a {@link com.bbn.marti.config.Network.Input}  input for this connection. This connection must be a proper input type or this will throw an exception!
	 *
	 * @return A usable
	 */
	public final Network.Input getConfigInput() {
		if (getConnectionType() != ProtocolProfiles.ConnectionType.INPUT) {
			throw new RuntimeException(("Cannot generate an input for for connection '" + getConsistentUniqueReadableIdentifier() + "' because it is not an input type!"));
		}
		Network.Input input = new Network.Input();
		input.setName(this.getConsistentUniqueReadableIdentifier());
		input.setProtocol(getProtocol().getValue());
		input.setPort(getPort());
		input.setAuth(getAuthType());
		input.setGroup(getMCastGroup());
		input.setAnongroup(getRawAnonAccessFlag());
		Integer networkVersion = getProtocol().getCoreNetworkVersion();
		if (networkVersion != null) {
			input.setCoreVersion(networkVersion);
		}
		if (getGroupSet().groupSet != null) {
			input.getFiltergroup().addAll(getGroupSet().groupSet);
		}

		return input;
	}


	/**
	 * Generates a {@link com.bbn.marti.config.Subscription.Static} for this connection. This connection must be a proper static subscription type or this will throw an exception!
	 *
	 * @return
	 */
	public final Subscription.Static getStaticSubscription() {
		if (getConnectionType() != ProtocolProfiles.ConnectionType.SUBSCRIPTION) {
			throw new RuntimeException(("Cannot generate a static subscription for connection '" + getConsistentUniqueReadableIdentifier() + "' because it is not a subscription type!"));
		}

		Subscription.Static subscription = new Subscription.Static();
		subscription.setProtocol(getProtocol().getValue());

		if (getProtocol() == ProtocolProfiles.SUBSCRIPTION_MCAST) {
			subscription.setAddress(getMCastGroup());
		} else {
			subscription.setAddress("127.0.0.1");
		}

		subscription.setName(getConsistentUniqueReadableIdentifier());
		subscription.setPort(getPort());
		subscription.setFederated(false);

		return subscription;
	}


	/**
	 * Generates a matching {@link com.bbn.marti.config.Subscription.Static} for this input. This connection must be a proper input type or this will throw an exception!
	 *
	 * @return
	 */
	public final Subscription.Static generateMatchingStaticSubscription() {
		AbstractServerProfile server = getServer();

		Subscription.Static subscription = new Subscription.Static();

		subscription.setProtocol(getProtocol().getValue());

		if (getProtocol() == ProtocolProfiles.INPUT_MCAST) {
			subscription.setAddress(getMCastGroup());
		} else {
			subscription.setAddress("127.0.0.1");
		}

		subscription.setName(getConsistentUniqueReadableIdentifier() + "-subcription");
		subscription.setPort(getPort());
		subscription.setFederated(false);

		return subscription;
	}


	/**
	 * Indicates if the connection requires authentication
	 *
	 * @return Whether or not it requires authentication
	 */
	public final boolean requiresAuthentication() {
		return requiresAuthentication(getAuthType());
	}

	public static boolean requiresAuthentication(@NotNull AuthType authType) {
		return (authType != AuthType.ANONYMOUS);
	}

	/**
	 * Produces a multi-line string with much of the information in this file displayed.
	 *
	 * @return A display string of this object
	 */
	@NotNull
	public final String displayString() {
		return "{ getConsistentUniqueReadableIdentifier : \"" + getConsistentUniqueReadableIdentifier() + "\"" +
				", type: \"" + getProtocol().getConnectionType().name() + "\"" +
				", protocol : \"" + getProtocol().getValue() + "\"" +
				", port : \"" + getPort() + "\"" +
				", auth : \"" + getAuthType().value() + "\"" +
				", anon : " + (getRawAnonAccessFlag() == null ? "undefined" : ("\"" + getRawAnonAccessFlag().toString() + "\"")) +
				", filtergroups : " + getGroupSet().displayString() +
				", group : " + (getMCastGroup() == null ? "undefined" : ("\"" + getMCastGroup() + "\"")) +
				" }";
	}


	/**
	 * Should be the same as {@link AbstractConnection#getConsistentUniqueReadableIdentifier()}
	 *
	 * @return The user identifier
	 */
	public final String toString() {
		return getConsistentUniqueReadableIdentifier();
	}


	/**
	 * Gets the protocol in use
	 *
	 * @return The protocol in use
	 */
	public abstract ProtocolProfiles getProtocol();


	/**
	 * Gets the port used by this connection
	 *
	 * @return The port number
	 */
	public abstract int getPort();


	/**
	 * Gets the authentication type in use
	 *
	 * @return the authentication type
	 */
	public abstract AuthType getAuthType();


	/**
	 * Gets the group set associated with the connection
	 *
	 * @return The group set
	 */
	public abstract GroupSetProfiles getGroupSet();


	/**
	 * Gets the mcast group associated with the connection, if any
	 *
	 * @return the mcat group
	 */
	public abstract String getMCastGroup();


	/**
	 * This is meant to be a unique identifier to be used for tracking purposes.
	 * It has a few properties:
	 * <p>
	 * - Mutable instances of users shall inherit it
	 * - It shall be consistent through the life of the item, unless intentionally overridden to test edge cases
	 *
	 * @return the connection identifier
	 */
	public abstract String getConsistentUniqueReadableIdentifier();


	/**
	 * Gets a variant of the getConsistentUniqueReadableIdentifier that if the connection has been modified contains information also containing that.
	 * That being the case, the getConsistentUniqueReadableIdentifier is not static and cannot be used as a unique identifier for the connection
	 *
	 * @return The getConsistentUniqueReadableIdentifier indicating the connection's identifier and current status
	 */
	public abstract String getDynamicName();


	/**
	 * Gets the server associated with the connection
	 *
	 * @return The server
	 */
	public abstract AbstractServerProfile getServer();


	/**
	 * Gets the value of the isAnon flag (could be null/unset)
	 *
	 * @return The value of the anon flag, if any
	 */
	public abstract Boolean getRawAnonAccessFlag();


	/**
	 * Gets the users associated with the connection
	 *
	 * @param filter
	 * @return The set of users
	 */
	public abstract Set<AbstractUser> getUsers(@Nullable UserFilter filter);

	@Override
	public final int compareTo(Object o) {

		if (o == null) {
			return 1;
		} else {
			if (o instanceof AbstractConnection) {
				return (this.getConsistentUniqueReadableIdentifier().compareTo(((AbstractConnection) o).getConsistentUniqueReadableIdentifier()));
			} else if (o instanceof String) {
				return (this.getConsistentUniqueReadableIdentifier().compareTo((String) o));
			} else {
				return -1;
			}
		}
	}

	@Override
	public final int compare(Object o1, Object o2) {
		if (o1 == null && o2 == null) {
			return 0;
		} else if (o1 == null) {
			return -1;
		} else if (o2 == null) {
			return 1;
		} else if (o1 instanceof AbstractConnection) {
			return ((AbstractConnection) o1).compareTo(o2);
		} else if (o2 instanceof AbstractConnection) {
			return ((AbstractConnection) o2).compareTo(o1);
		} else {
			return -1;
		}
	}

	@Override
	public final boolean equals(Object obj) {
		return (compareTo(obj) == 0);
	}
}
