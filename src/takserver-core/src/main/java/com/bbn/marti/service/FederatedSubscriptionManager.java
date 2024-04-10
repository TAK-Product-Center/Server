package com.bbn.marti.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.atakmap.Tak.FederateGroups;
import com.atakmap.Tak.FederatedEvent;
import com.atakmap.Tak.ROL;
import com.bbn.marti.config.Federation.Federate;
import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.remote.ConnectionStatus;
import com.bbn.marti.remote.RemoteContact;
import com.bbn.marti.remote.RemoteSubscription;
import com.bbn.marti.remote.RemoteSubscriptionMetrics;
import com.bbn.marti.remote.groups.ConnectionInfo;
import com.bbn.marti.remote.groups.User;

import tak.server.federation.FederateSubscription;
import tak.server.federation.GuardedStreamHolder;
import tak.server.federation.RemoteContactWithSA;

public interface FederatedSubscriptionManager {
		
	void putFederateSubscription(ConnectionInfo connectionInfo, FederateSubscription federateSubscription);
	FederateSubscription removeFederateSubcription(ConnectionInfo connectionInfo);
	FederateSubscription getFederateSubscription(ConnectionInfo connectionInfo);
		
	void updateFederateOutgoingStatusCache(String name, ConnectionStatus status);
	ConnectionStatus removeFederateOutgoingStatus(String name);
	ConnectionStatus getFederationConnectionStatus(String name);
	ConnectionStatus getAndPutFederateOutgoingStatus(String name, ConnectionStatus status);

	void putOutgoingNumRetries(String name);
	AtomicInteger removeOutgoingNumRetries(String name);
	AtomicInteger getOutgoingNumRetries(String name);
	
	void putContactToContactUid(String contactuid, RemoteContact remoteContact);
	RemoteContact removeContactByContactUid(String contactuid);
	RemoteContact getContactByContactUid(String contactuid);
	Collection<RemoteContact> getContacts();
	
	void putSubscriptionToCallsign(String callsign, Subscription subscription);
	Subscription removeSubscriptionByCallsign(String callsign);
	Subscription getSubscriptionByCallsign(String callsign);
	Map<String, Subscription> getViewOfCallsignMap();
	Subscription getSubscriptionByCallsignIgnoreCase(String callsign);
	
	void putSubscriptionToConnectionInfo(ConnectionInfo connectionInfo, Subscription subscription);
	Subscription removeSubscriptionByConnectionInfo(ConnectionInfo connectionInfo);
	Subscription getSubscriptionByConnectionInfo(ConnectionInfo connectionInfo);
	Map<ConnectionInfo, Subscription> getViewOfConnectionSubMap();

	void putSubscriptionToUser(User user, Subscription subscription);
	Subscription removeSubscriptionByUser(User user);
	Subscription getSubscriptionByUser(User user);
	Map<User, Subscription> getViewOfUserSubscriptionMap();
	RemoteSubscription getSubscriptionByUsersDisplayName(String displayName);
	
	Map<String, Set<String>> getFederateRemoteGroups();

	void putSubscriptionToClientUid(String clientuid, Subscription subscription);
	Subscription removeSubscriptionByClientUid(String clientuid);
	Subscription getSubscriptionByClientUid(String clientuid);
	Map<String, Subscription> getViewOfClientUidToSubMap();
	
	void putSubMetricsToClientUid(String clientuid, RemoteSubscriptionMetrics remoteSubscriptionMetrics);
	RemoteSubscriptionMetrics removeSubMetricsByClientUid(String clientuid);
	RemoteSubscriptionMetrics getSubMetricsByClientUid(String clientuid);
	
	void putClientStreamToSession(String sessionId, GuardedStreamHolder<FederatedEvent> guardedStreamHolder);
	GuardedStreamHolder<FederatedEvent> removeClientStreamHolderBySession(String sessionId);
	GuardedStreamHolder<FederatedEvent> getClientStreamBySession(String sessionId);
	
	void putClientROLStreamToSession(String sessionId, GuardedStreamHolder<ROL> guardedStreamHolder);
	GuardedStreamHolder<ROL> removeClientROLStreamBySession(String sessionId);
	GuardedStreamHolder<ROL> getClientROLStreamBySession(String sessionId);
	
	void putServerGroupStreamToSession(String sessionId, GuardedStreamHolder<FederateGroups> groupStreamHolder);
	GuardedStreamHolder<FederateGroups> removeServerGroupStreamBySession(String sessionId);
	GuardedStreamHolder<FederateGroups> getServerGroupStreamBySession(String sessionId);
	
	void putRemoteContactsMapToChannelHandler(ChannelHandler channelHandler, ConcurrentHashMap<String, RemoteContactWithSA> remoteContactMap);
	ConcurrentHashMap<String, RemoteContactWithSA> removeRemoteContactsMapByChannelHandler(ChannelHandler channelHandler);
	ConcurrentHashMap<String, RemoteContactWithSA> getRemoteContactsMapByChannelHandler(ChannelHandler channelHandler);
	
	void putUidToMission(UUID missionGuid, String uid);
	void removeUidByMission(UUID missionGuid, String uid);
	Collection<String> getUidsByMission(UUID missionGuid);
	Collection<String> getLocalUidsByMission(UUID missionGuid);
	
	void putMissionToUid(String uid, UUID missionGuid);
	void removeMissionByUid(String uid, UUID missionGuid);
	Collection<UUID> getMissionsByUid(String uid);
	
	void putUidToMissionContents(UUID missionGuid, String uid);
	void removeUidByMissionContents(UUID missionGuid, String uid);
	Collection<String> getUidsByMissionContents(UUID missionGuid);
	
	void putMissionToContentsUid(String uid, UUID missionGuid);
	void removeMissionByContentsUid(String uid, UUID missionGuid);
	Collection<UUID> getMissionsByContentsUid(String uid);
	
	void removeMission(UUID missionGuid, Set<String> uids);
	
	Collection<FederateSubscription> getFederateSubscriptions();
	List<ConnectionStatus> getActiveConnectionInfo();
	void updateFederationSubscriptionCache(ConnectionInfo connectionInfo, Federate federate);
	ConnectionStatus getCachedFederationConnectionStatus(String name);
	
	AtomicBoolean getOutgoingRetryScheduled(String name);
	void putOutgoingRetryScheduled(String name);
	AtomicBoolean removeOutgoingRetryScheduled(String name);
	
	void clearFederationCaches();

}
