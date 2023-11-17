

package com.bbn.marti.groups;

import java.io.Serializable;
import java.util.NavigableSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.remote.RemoteSubscription;
import com.bbn.marti.remote.groups.Authenticator;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.User;
import com.bbn.marti.remote.groups.UserClassification;

/*
 *
 * In-memory Group store implementation using concurrent data structures
 *
 */
public class InMemoryGroupStore implements GroupStore, Serializable {

    private static final long serialVersionUID = -2512215772739390649L;

	private Logger logger = LoggerFactory.getLogger(InMemoryGroupStore.class);

    // track groups by user
    protected final ConcurrentMap<User, NavigableSet<Group>> userGroupMap;

	// track classification by user
	protected final ConcurrentMap<User, UserClassification> userClassificationMap;

	// master group superset (track groups by name and direction)
    protected final ConcurrentMap<String, Group> groups;

    // track users by connectionId
    protected final ConcurrentMap<String, User> connectionIdUserMap;

    // track subscriptions by user
    protected final ConcurrentMap<User, RemoteSubscription> userSubscriptionMap;

    // track authenticators by type
    protected final ConcurrentMap<String, Authenticator<User>> authenticatorMap;

    public InMemoryGroupStore() {

    	if (logger.isDebugEnabled()) {
    		logger.debug(getClass().getSimpleName() + " constructor");
    	}

        userGroupMap = new ConcurrentHashMap<>();
    	userClassificationMap = new ConcurrentHashMap<>();
        groups = new ConcurrentHashMap<>();
        connectionIdUserMap = new ConcurrentHashMap<>();
        userSubscriptionMap = new ConcurrentHashMap<>();
        authenticatorMap = new ConcurrentHashMap<>();
    }

    /* (non-Javadoc)
	 * @see com.bbn.marti.groups.GroupStore#getUserGroupMap()
	 */
    @Override
	public ConcurrentMap<User, NavigableSet<Group>> getUserGroupMap() {
		return userGroupMap;
	}

	@Override
	public ConcurrentMap<User, UserClassification> getUserClassificationMap() {
    	return userClassificationMap;
	}

	/* (non-Javadoc)
	 * @see com.bbn.marti.groups.GroupStore#getGroups()
	 */
	@Override
	public ConcurrentMap<String, Group> getGroups() {
		return groups;
	}

	/* (non-Javadoc)
	 * @see com.bbn.marti.groups.GroupStore#getConnectionIdUserMap()
	 */
	@Override
	public ConcurrentMap<String, User> getConnectionIdUserMap() {
		return connectionIdUserMap;
	}

	/* (non-Javadoc)
	 * @see com.bbn.marti.groups.GroupStore#getUserSubscriptionMap()
	 */
	@Override
	public ConcurrentMap<User, RemoteSubscription> getUserSubscriptionMap() {
		return userSubscriptionMap;
	}

	/* (non-Javadoc)
	 * @see com.bbn.marti.groups.GroupStore#getAuthenticatorMap()
	 */
	@Override
	public ConcurrentMap<String, Authenticator<User>> getAuthenticatorMap() {
		return authenticatorMap;
	}
}
