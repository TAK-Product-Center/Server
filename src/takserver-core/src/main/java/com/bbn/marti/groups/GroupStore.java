package com.bbn.marti.groups;

import java.util.NavigableSet;
import java.util.concurrent.ConcurrentMap;

import com.bbn.marti.remote.RemoteSubscription;
import com.bbn.marti.remote.groups.Authenticator;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.User;

/*
 * 
 * Group store interface
 * 
 */
public interface GroupStore {

	ConcurrentMap<User, NavigableSet<Group>> getUserGroupMap();

	ConcurrentMap<String, Group> getGroups();

	ConcurrentMap<String, User> getConnectionIdUserMap();

	ConcurrentMap<User, RemoteSubscription> getUserSubscriptionMap();

	ConcurrentMap<String, Authenticator<User>> getAuthenticatorMap();

}