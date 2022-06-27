package com.bbn.marti.test.shared.data.users;

import com.bbn.marti.config.AuthType;
import com.bbn.marti.test.shared.data.GroupSetProfiles;
import com.bbn.marti.test.shared.data.connections.AbstractConnection;
import com.bbn.marti.test.shared.data.servers.AbstractServerProfile;

import java.nio.file.Path;
import java.util.Comparator;

/**
 * Created on 3/7/16.
 */
public abstract class AbstractUser implements Comparable<Object>, Comparator<Object> {

	/**
	 * Gets the user's group set access considering their authentication type (which may not match the actual group set access
	 *
	 * @return The group set
	 */
	public final GroupSetProfiles getBaseGroupSetAccess() {
		if (getConnection().getAuthType() != AuthType.ANONYMOUS) {
			return getDefinedGroupSet();
		} else {
			return null;
		}
	}


	public String getCotCallsign() {
		return "[Callsign]" + getConsistentUniqueReadableIdentifier();
	}


	/**
	 * This is the same as {@link AbstractUser#getConsistentUniqueReadableIdentifier()}
	 *
	 * @return The user identifier
	 */
	public String getCotUid() {
		return getConsistentUniqueReadableIdentifier();
	}


	/**
	 * Gets the resultant anon group access for this user when all things are considered
	 *
	 * @return the anon access
	 */
	public final boolean getActualAnonAccess() {
		AbstractConnection connection = getConnection();
		Boolean inputIsAnon = connection.getAnonAccess();
		AuthType authType = connection.getAuthType();

		if (authType == AuthType.ANONYMOUS) {
			return inputIsAnon;
		} else {
			return false;
		}
	}


	/**
	 * Indicates if the user credentials are necessary and valid.
	 *
	 * @return If the credentials are valid and up to date
	 */
	public boolean isUserCredentialsValid() {
		return !getConnection().requiresAuthentication() || (getUserName() != null && getPassword() != null);
	}


	/**
	 * Gets the user's group set access, taking into account the users defined groups, the authentication type, and any groups that may be part of the related connection.
	 * This is the user's actual group set access
	 *
	 * @return THe group set
	 */
	public final GroupSetProfiles getActualGroupSetAccess() {
		AbstractConnection connection = getConnection();
		AuthType authType = connection.getAuthType();
		GroupSetProfiles userGroupSet = getBaseGroupSetAccess();
		GroupSetProfiles inputGroupSet = connection.getGroupSet();

		// Ignore anonuser connection state
		if (authType == AuthType.ANONYMOUS) {
			// Ignore user group set since it only be used with an authed connection
			return inputGroupSet;

		} else {
			// Ignore connection group set since it cannot be used with an unauthed connection
			return userGroupSet;

		}
	}

	@Override
	public final String toString() {
		return getConsistentUniqueReadableIdentifier();
	}


	@Override
	public final int compareTo(Object o) {

		if (o == null) {
			return 1;
		} else {
			if (o instanceof AbstractUser) {
				return (this.getConsistentUniqueReadableIdentifier().compareTo(((AbstractUser) o).getConsistentUniqueReadableIdentifier()));
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
		} else if (o1 instanceof AbstractUser) {
			return ((AbstractUser) o1).compareTo(o2);
		} else if (o2 instanceof AbstractUser) {
			return ((AbstractUser) o2).compareTo(o1);
		} else {
			return -1;
		}
	}

	@Override
	public final boolean equals(Object obj) {
		return (compareTo(obj) == 0);
	}

	/**
	 * Gets the username used for authentication with the server
	 *
	 * @return The username
	 */
	public abstract String getUserName();


	/**
	 * Gets the password used for authentication with the server
	 *
	 * @return The password
	 */
	public abstract String getPassword();


	/**
	 * Gets the group set defined for the user (which may not match the actual group set access)
	 *
	 * @return The group set
	 */
	public abstract GroupSetProfiles getDefinedGroupSet();


	/**
	 * Gets the server associated with this user
	 *
	 * @return The server
	 */
	public abstract AbstractServerProfile getServer();


	/**
	 * Gets the connection associated with this user
	 *
	 * @return The connection
	 */
	public abstract AbstractConnection getConnection();


	/**
	 * Gets the private certificate path associated with this user, if it exists.
	 * @return The certificate path
	 */
	public abstract Path getCertPublicPemPath();


	/**
	 * Gets the public certificate path associated with this user, if it exists.
	 * @return The certificate path
	 */
	public abstract Path getCertPrivateJksPath();

	/**
	 * Indicates if messages sent to or from this user should be validated
	 *
	 * @return The validation value
	 */
	public abstract boolean doValidation();


	/**
	 * This is meant to be a unique identifier to be used for tracking purposes.
	 * It has a few properties:
	 * <p>
	 * - It shall be used as the Uid if the Uid has not been overridden
	 * - Mutable instances of users shall inherit it
	 * - It shall be consistent through the life of the item, unless intentionally overridden to test edge cases
	 *
	 * @return the user identifier
	 */
	public abstract String getConsistentUniqueReadableIdentifier();


	/**
	 * Gets a variant of the getConsistentUniqueReadableIdentifier that if the user has been modified contains information also containing that.
	 * <p>
	 * That being the case, the getConsistentUniqueReadableIdentifier is not static and cannot be used as a unique identifier for the user
	 *
	 * @return The getConsistentUniqueReadableIdentifier indicating the user's identifier and current status
	 */
	public abstract String getDynamicName();


}
