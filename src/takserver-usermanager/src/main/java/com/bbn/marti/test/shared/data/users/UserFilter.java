package com.bbn.marti.test.shared.data.users;

import com.bbn.marti.test.shared.data.connections.ConnectionFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Created on 3/15/16.
 */
public class UserFilter {
	private ConnectionFilter connectionFilter;
	private Boolean isUserActivityValidated = null;
	private Boolean isUserValid = null;
	private Boolean doesUserHaveAnonAccess = null;

	public final UserFilter setConnectionFilter(@Nullable ConnectionFilter connectionFilter) {
		this.connectionFilter = connectionFilter;
		return this;
	}

	public final ConnectionFilter getConnectionFilter() {
		if (connectionFilter == null) {
			connectionFilter = new ConnectionFilter();
		}
		return connectionFilter;
	}

	public final UserFilter setUserActivityIsValidated(@Nullable Boolean value) {
		isUserActivityValidated = value;
		return this;
	}

	public final UserFilter setUserCredentialValidity(@Nullable Boolean value) {
		isUserValid = value;
		return this;
	}

	public final UserFilter setUserAnonAccess(@Nullable Boolean value) {
        doesUserHaveAnonAccess = value;
		return this;
	}

	public final CopyOnWriteArraySet<AbstractUser> filterUsers(Set<AbstractUser> currentSet) {
		Set<AbstractUser> returnSet = new HashSet<>();

		for (AbstractUser user : currentSet) {
			if (doesUserMatch(user)) {
				returnSet.add(user);
			}
		}
		return new CopyOnWriteArraySet<>(returnSet);
	}

	public final boolean doesUserMatch(@NotNull AbstractUser user) {
		if (isUserActivityValidated != null && isUserActivityValidated != user.doValidation()) {
			return false;
		} else if (isUserValid != null && isUserValid != user.isUserCredentialsValid()) {
			return false;
		} else if (connectionFilter != null && !connectionFilter.doesConnectionMatch(user.getConnection())) {
			return false;
		} else if (doesUserHaveAnonAccess != null && doesUserHaveAnonAccess != user.getActualAnonAccess()) {
			return false;
		} else {
			return true;
		}
	}
}
