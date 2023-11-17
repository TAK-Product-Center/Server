package com.bbn.marti.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import com.atakmap.Tak.FederateGroups;
import com.atakmap.Tak.FederatedEvent;
import com.atakmap.Tak.ROL;
import com.bbn.marti.config.Federation.Federate;
import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.nio.channel.base.AbstractBroadcastingChannelHandler;
import com.bbn.marti.remote.ConnectionStatus;
import com.bbn.marti.remote.ConnectionStatusValue;
import com.bbn.marti.remote.InputMetric;
import com.bbn.marti.remote.RemoteCachedSubscription;
import com.bbn.marti.remote.RemoteContact;
import com.bbn.marti.remote.RemoteFile;
import com.bbn.marti.remote.RemoteSubscription;
import com.bbn.marti.remote.RemoteSubscriptionMetrics;
import com.bbn.marti.remote.groups.ConnectionInfo;
import com.bbn.marti.remote.groups.FederateUser;
import com.bbn.marti.remote.groups.User;
import com.bbn.marti.remote.util.ConcurrentMultiHashMap;
import com.bbn.marti.util.MessagingDependencyInjectionProxy;
import com.bbn.marti.util.spring.SpringContextBeanForApi;
import com.google.common.collect.Multimap;

import io.grpc.stub.StreamObserver;
import tak.server.Constants;
import tak.server.federation.FederateSubscription;
import tak.server.federation.FederationSubscriptionCacheDAO;
import tak.server.federation.GuardedStreamHolder;
import tak.server.federation.RemoteContactWithSA;
import tak.server.ignite.cache.IgniteCacheHolder;

/*
 * 
 * Storage mechanism for subscriptions in TAK server
 * 
 */
public class SubscriptionStore implements FederatedSubscriptionManager {

	private static final Logger logger = LoggerFactory.getLogger(SubscriptionStore.class);
	private static SubscriptionStore instance;

	@Autowired
	protected Ignite ignite;
	// Used to keep track of where/if federation connections got started
	private IgniteCache<String, FederationSubscriptionCacheDAO>  federationSubscriptionCache = null;
	// Used so that when an api process polls for federation outgoing data, we dont just get data from one messaging node.
	// we get it from all the nodes in the cluster
	private IgniteCache<String, ConnectionStatus>  federationOutgoingConnectionStatus = null;
	
	private ConcurrentHashMap<String, Subscription> uidSubscriptionMap = new ConcurrentHashMap<>();
	private ConcurrentHashMap<ChannelHandler, Subscription> channelHandlerSubscriptionMap = new ConcurrentHashMap<>();
		
	private final Set<RemoteFile> fileSet = new ConcurrentSkipListSet<>();
	
	private final Map<String, Subscription> callsignMap = new ConcurrentHashMap<String,Subscription>();
	private final Map<ConnectionInfo, FederateSubscription> subMap = new ConcurrentHashMap<>();
	private final Map<String, ConnectionStatus> outgoingMap = new ConcurrentHashMap<>();
	private final Map<String, AtomicInteger> outgoingRetryMap = new ConcurrentHashMap<>();
	private final Map<ChannelHandler, ConcurrentHashMap<String, RemoteContactWithSA>> contactList = new ConcurrentHashMap<>();
	private final Map<String, GuardedStreamHolder<FederatedEvent>> sessionClientStreamMap = new ConcurrentHashMap<>();
	private final Map<String, GuardedStreamHolder<ROL>> sessionROLClientStreamMap = new ConcurrentHashMap<>();
	private final Map<String, GuardedStreamHolder<FederateGroups>> sessionGroupServerStreamMap = new ConcurrentHashMap<>();
	private final Map<String, RemoteContact> contactMap = new ConcurrentHashMap<>();
	private final Map<ConnectionInfo, Subscription> connectionSubMap = new ConcurrentHashMap<>();
	private final Map<User, Subscription> userSubscriptionMap = new ConcurrentHashMap<>();
	private final Map<String, RemoteSubscriptionMetrics> clientUidToSubMapMetrics = new ConcurrentHashMap<String, RemoteSubscriptionMetrics>();
	private final Map<String,Subscription> clientUidToSubMap = new ConcurrentHashMap<String, Subscription>();  
	private final Map<String, AtomicBoolean> retryScheduledMap = new ConcurrentHashMap<>();
	
	// track mission subscriptions
    private final Multimap<String, String> missionUidMap = new ConcurrentMultiHashMap<String, String>();
    private final Multimap<String, String> uidMissionMap = new ConcurrentMultiHashMap<String, String>();

    // track uid contents of missions
    private final Multimap<String, String> uidMissionContentsMap = new ConcurrentMultiHashMap<String, String>();
    private final Multimap<String, String> missionContentsUidMap = new ConcurrentMultiHashMap<String, String>();

    
    public static synchronized SubscriptionStore getInstance() {
		if (instance == null) {
			synchronized (SubscriptionStore.class) {
				if (instance == null) {
					instance = SpringContextBeanForApi.getSpringContext().getBean(SubscriptionStore.class);
				}
			}
		}

		return instance;
	}
	
	public static synchronized FederatedSubscriptionManager getInstanceFederatedSubscriptionManager() {
		return getInstance();
	}
    
    @EventListener({ContextRefreshedEvent.class})
	private void init() {
		if (logger.isDebugEnabled()) {
			logger.debug("init SubscriptionStore. ignite instance: " + ignite);
		}
    }
    
    protected IgniteCache<String, FederationSubscriptionCacheDAO> getFederationSubscriptionCache() {
		if (federationSubscriptionCache == null) {
			federationSubscriptionCache = ignite.getOrCreateCache("fedsubcache");
		}
		
		return federationSubscriptionCache;
	}
	
	protected IgniteCache<String, ConnectionStatus> getFederationOutgoingConnectionStatusCache() {
		if (federationOutgoingConnectionStatus == null) {
			federationOutgoingConnectionStatus = ignite.getOrCreateCache("federationOutgoingConnectionStatus");
		}
				
		return federationOutgoingConnectionStatus;
	}
	
	private void updateSubscriptionCaches(Subscription subscription) {
		RemoteSubscription sub = new RemoteSubscription(subscription);		
		IgniteCacheHolder.cacheRemoteSubscription(sub);
	}
	
	public Set<RemoteFile> getFileSet() {
		return fileSet;
	}
	
	public void forEachUidSubscription(BiConsumer<? super String, ? super Subscription> action) {
		uidSubscriptionMap.forEach((Runtime.getRuntime().availableProcessors() == 1 ? 1 : Runtime.getRuntime().availableProcessors() / 2), action);
	}
	
	public Collection<Subscription> getAllSubscriptions() {
		return uidSubscriptionMap.values();
	}
			
	public ArrayList<RemoteSubscription> getFilteredPaginatedCachedRemoteSubscriptions(String groupVector, String sort, int direction,
			int page, int limit) {
		ArrayList<RemoteSubscription> subs = new ArrayList<>();

		SqlFieldsQuery qry = getAllSubscriptionQuery(groupVector, sort, direction, page, limit);
		try (QueryCursor<List<?>> cursor = IgniteCacheHolder.getIgniteSubscriptionUidTackerCache().query(qry)) {
			for (List<?> row : cursor) {
				subs.add(new RemoteCachedSubscription(row));
			}
		}

		return subs;
	}
		
	private SqlFieldsQuery getAllSubscriptionQuery(String groupVector, String sort, int direction, int page, int limit) {		
		StringBuilder sb = new StringBuilder();
		sb.append("select rs.callsign, rs.clientUid, rs.connectionId, rs.currentBandwidth, rs.handlerType, rs.iface, rs.incognito, rs.lastProcTime, rs.mode, rs.notes, rs.role, rs.takv, rs.team, rs.to, rs.uid, rs.username, rs.xpath, rs.numHits, rs.writeQueueDepth, rs.lastProcTime, ig._VAL, og._VAL, rs.originNode from RemoteSubscription as rs inner join \""  + Constants.IGNITE_USER_INBOUND_GROUP_CACHE +  "\".String ig on rs.connectionId = ig._KEY inner join \"" + Constants.IGNITE_USER_OUTBOUND_GROUP_CACHE + "\".String og on ig._KEY = og._KEY ORDER BY ?");
		sb.append(direction == 1 ? " ASC" : " DESC");
		sb.append(limit == -1 ? "" : " LIMIT ");
		sb.append(limit == -1 ? "" : limit);
		sb.append(page == -1 ? "" : " OFFSET ");
		sb.append(page == -1 ? "" : limit == -1 ? page : (page - 1) * limit);
		sb.append(";");
		
		SqlFieldsQuery qry = new SqlFieldsQuery(sb.toString());
		qry.setArgs(sort);
		qry.setDistributedJoins(true);
		
		return qry;
	}
	
	public Set<Entry<String, Subscription>> getSubscriptionsEntries() {
		return uidSubscriptionMap.entrySet();
	}
	
	public void put(String uid, Subscription subscription) {
		
		// only update count if new subscription
		if (!uidSubscriptionMap.contains(uid) && subscription.handler != null) {
			InputMetric inputMetric = SubmissionService.getInstance().getMetricByPort(subscription.handler.localPort());
			if (inputMetric != null) {
				inputMetric.getNumClients().incrementAndGet();
			}
		}
		
		uidSubscriptionMap.put(uid, subscription);
		updateSubscriptionCaches(subscription);
		if (logger.isTraceEnabled()) {
			logger.trace("storing handler " + subscription.getHandler() + " for subscription " + subscription);
		}
				
		if (subscription != null && subscription.getHandler() != null) {
			channelHandlerSubscriptionMap.put(subscription.getHandler(), subscription);
		}
	}
	
	public Subscription getBySubscriptionUid(String uid) {
		return uidSubscriptionMap.get(uid);
	}
	
	public Subscription getByHandler(ChannelHandler handler) {
		return channelHandlerSubscriptionMap.get(handler);
	}
	
	/*
	 * @returns the Subscription that was removed from the cache, or null if it was not present 
	 * 
	 */
	public Subscription removeByUid(String uid) {
		Subscription subscription = uidSubscriptionMap.remove(uid);
		if (subscription != null) {
			IgniteCacheHolder.removeCachedRemoteSubscription(subscription);
			channelHandlerSubscriptionMap.remove(subscription.handler);
            InputMetric inputMetric = SubmissionService.getInstance().getMetricByPort(subscription.handler.localPort());
            if (inputMetric != null) {
				inputMetric.getNumClients().decrementAndGet();
            }
		}
		
		return subscription;
	}
	
	public Subscription remove(Subscription subscription) {
		
		try {
			if (subscription.handler != null) {
				channelHandlerSubscriptionMap.remove(subscription.handler);
			}
		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("excdeption removing subscription from map", e);
			}
		}
		
		try {
			
			channelHandlerSubscriptionMap.entrySet().forEach(e -> {
				if (e.getValue().equals(subscription)) {
					channelHandlerSubscriptionMap.remove(e.getKey());
				}
			});
			
			clientUidToSubMap.entrySet().stream().forEach(e -> {
				if (e.getValue().equals(subscription)) {
					clientUidToSubMap.remove(e.getKey());
				}
			});
			
		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception removing subscription from clientUidToSubMap", e);
			}
		}
		
		IgniteCacheHolder.removeCachedRemoteSubscription(subscription);

		return uidSubscriptionMap.remove(subscription.uid);
	}	
	public long size() {
		return uidSubscriptionMap.size();
	}
	
	public int sizeInt() {
		return uidSubscriptionMap.size();
	}

	@Override
	public Collection<FederateSubscription> getFederateSubscriptions() {
		return subMap.values();
	}
	
	public Set<String> subscriptionCollectionToConnectionIdSet(Collection<Subscription> subs) {
		
		Set<String> destSubscriptionConnectionIds = new HashSet<>();

		for (Subscription sub: subs) {

			if (sub != null && sub.getHandler() != null && sub.getHandler() instanceof AbstractBroadcastingChannelHandler) {

				String connectionId = ((AbstractBroadcastingChannelHandler) sub.getHandler()).getConnectionId();

				if (connectionId != null) {
					destSubscriptionConnectionIds.add(connectionId);
				}

			}
		}
		
		return destSubscriptionConnectionIds;
	}

	@Override
	public List<ConnectionStatus> getActiveConnectionInfo() {
		List<ConnectionStatus> l = new LinkedList<>();
		for (Map.Entry<ConnectionInfo, FederateSubscription> s : subMap.entrySet()) {
			ConnectionStatus status = new ConnectionStatus(ConnectionStatusValue.CONNECTED);
			status.setConnection(s.getKey());
			
			Federate federate = ((FederateUser) s.getValue().getUser()).getFederateConfig();
			
			status.setUser((FederateUser) s.getValue().getUser());
			
			status.setGroups(MessagingDependencyInjectionProxy.getInstance().groupManager().getGroups((FederateUser) s.getValue().getUser()));
			
			status.setFederate(federate);
			l.add(status);
			
			if (logger.isDebugEnabled()) {
				logger.debug("federateConfig: " + federate);
				logger.debug("inbound fed groups: " + federate.getInboundGroup());

			}
		}
		
		return l;
	}

	@Override
	public void updateFederationSubscriptionCache(ConnectionInfo connectionInfo, Federate federate) {
		getFederationSubscriptionCache().put(connectionInfo.getConnectionId(), new FederationSubscriptionCacheDAO(connectionInfo, federate));		
	}
	
	@Override
	public ConnectionStatus getFederationConnectionStatus(String name) {
		ConnectionStatus status = outgoingMap.get(name);
		// we didnt find a status locally.. but lets check the cache
		// incase one got added on another node. then we'll track it locally as well
		if (status == null) {
			status = getCachedFederationConnectionStatus(name);
			
			if (status != null) {
				outgoingMap.put(name, status);
			}
		}
		return outgoingMap.get(name);
	}
	
	@Override
	public ConnectionStatus getCachedFederationConnectionStatus(String name) {
		return getFederationOutgoingConnectionStatusCache().get(name);
	}

	@Override
	public void updateFederateOutgoingStatusCache(String name, ConnectionStatus status) {
		getFederationOutgoingConnectionStatusCache().put(name, status);
	}

	@Override
	public ConnectionStatus getAndPutFederateOutgoingStatus(String name, ConnectionStatus status) {
		outgoingMap.put(name, status);
		return getFederationOutgoingConnectionStatusCache().getAndPutIfAbsent(name, status);
	}
	
	@Override
	public ConnectionStatus removeFederateOutgoingStatus(String name) {
		outgoingMap.remove(name);
		return getFederationOutgoingConnectionStatusCache().getAndRemove(name);
	}
	
	@Override
	public AtomicInteger getOutgoingNumRetries(String name) {
		outgoingRetryMap.putIfAbsent(name, new AtomicInteger(0));
		return outgoingRetryMap.get(name);
	}
	
	@Override
	public void putOutgoingNumRetries(String name) {
		outgoingRetryMap.put(name, new AtomicInteger(0));
	}

	@Override
	public AtomicInteger removeOutgoingNumRetries(String name) {
		return outgoingRetryMap.remove(name);
	}
	
	@Override
	public AtomicBoolean getOutgoingRetryScheduled(String name) {
		retryScheduledMap.putIfAbsent(name, new AtomicBoolean(false));
		return retryScheduledMap.get(name);
	}
	
	@Override
	public void putOutgoingRetryScheduled(String name) {
		retryScheduledMap.put(name, new AtomicBoolean(false));
	}

	@Override
	public AtomicBoolean removeOutgoingRetryScheduled(String name) {
		return retryScheduledMap.remove(name);
	}
	
	@Override
	public void putFederateSubscription(ConnectionInfo connectionInfo, FederateSubscription federateSubscription) {
		
		if (logger.isDebugEnabled()) {
			logger.debug("putFederateSubscription " + connectionInfo + " " + federateSubscription);
		}
		
		subMap.put(connectionInfo, federateSubscription);
	}

	@Override
	public FederateSubscription removeFederateSubcription(ConnectionInfo connectionInfo) {
		getFederationSubscriptionCache().remove(connectionInfo.getConnectionId());
		return subMap.remove(connectionInfo);
	}

	@Override
	public FederateSubscription getFederateSubscription(ConnectionInfo connectionInfo) {
		
		if (logger.isDebugEnabled()) {
			logger.debug("getFederateSubscription " + connectionInfo);
		}
		
		return subMap.get(connectionInfo);
	}

	@Override
	public void putContactToContactUid(String contactuid, RemoteContact remoteContact) {
		contactMap.put(contactuid, remoteContact);
	}

	@Override
	public RemoteContact removeContactByContactUid(String contactuid) {
		return contactMap.remove(contactuid);
	}

	@Override
	public RemoteContact getContactByContactUid(String contactuid) {
		return contactMap.get(contactuid);
	}
	
	@Override
	public Collection<RemoteContact> getContacts() {
		return Collections.unmodifiableCollection(contactMap.values());
	}

	@Override
	public void putSubscriptionToCallsign(String callsign, Subscription subscription) {
		callsignMap.put(callsign, subscription);
		updateSubscriptionCaches(subscription);
	}

	@Override
	public Subscription removeSubscriptionByCallsign(String callsign) {
		return callsignMap.remove(callsign);
	}

	@Override
	public Subscription getSubscriptionByCallsign(String callsign) {
		return callsignMap.get(callsign);
	}
	
	@Override
	synchronized public Subscription getSubscriptionByCallsignIgnoreCase(String callsign) {
		for (Map.Entry<String, Subscription> entry : callsignMap.entrySet()) {
			String key = entry.getKey();
			if (key.compareToIgnoreCase(callsign) == 0) {
				return entry.getValue();
			}
		}

		return null;
	}
	
	@Override
	public Map<String, Subscription> getViewOfCallsignMap() {
		return Collections.unmodifiableMap(callsignMap);
	}

	@Override
	public void putSubscriptionToConnectionInfo(ConnectionInfo connectionInfo, Subscription subscription) {
		connectionSubMap.put(connectionInfo, subscription);
	}

	@Override
	public Subscription removeSubscriptionByConnectionInfo(ConnectionInfo connectionInfo) {
		return connectionSubMap.remove(connectionInfo);
	}

	@Override
	public Subscription getSubscriptionByConnectionInfo(ConnectionInfo connectionInfo) {
		return connectionSubMap.get(connectionInfo);
	}
	
	@Override
	public Map<ConnectionInfo, Subscription> getViewOfConnectionSubMap() {
		return Collections.unmodifiableMap(connectionSubMap);
	}

	@Override
	public void putSubscriptionToUser(User user, Subscription subscription) {
		userSubscriptionMap.put(user, subscription);
		updateSubscriptionCaches(subscription);
	}

	@Override
	public Subscription removeSubscriptionByUser(User user) {
		return userSubscriptionMap.remove(user);
	}

	@Override
	public Subscription getSubscriptionByUser(User user) {
		return userSubscriptionMap.get(user);
	}
	
	@Override
	public Map<User, Subscription> getViewOfUserSubscriptionMap() {
		return Collections.unmodifiableMap(userSubscriptionMap);
	}
	
	@Override
	public RemoteSubscription getSubscriptionByUsersDisplayName(String displayName) {
		for (Map.Entry<User, Subscription> entry : userSubscriptionMap.entrySet()) {
			if (entry.getKey().getDisplayName().compareToIgnoreCase(displayName) == 0) {
				return Subscription.copyAsRemoteSubscription(entry.getValue());
			}
		}
		return null;
	}

	@Override
	public void putSubscriptionToClientUid(String clientuid, Subscription subscription) {
		clientUidToSubMap.put(clientuid, subscription);
		updateSubscriptionCaches(subscription);
	}

	@Override
	public Subscription removeSubscriptionByClientUid(String clientuid) {
		IgniteCacheHolder.getIgniteSubscriptionClientUidTackerCache().remove(clientuid);
		return clientUidToSubMap.remove(clientuid);
	}

	@Override
	public Subscription getSubscriptionByClientUid(String clientuid) {
		return clientUidToSubMap.get(clientuid);
	}

	@Override
	public Map<String, Subscription> getViewOfClientUidToSubMap() {
		return Collections.unmodifiableMap(clientUidToSubMap);
	}
	
	@Override
	public void putSubMetricsToClientUid(String clientuid, RemoteSubscriptionMetrics remoteSubscriptionMetrics) {
		clientUidToSubMapMetrics.put(clientuid, remoteSubscriptionMetrics);
	}

	@Override
	public RemoteSubscriptionMetrics removeSubMetricsByClientUid(String clientuid) {
		return clientUidToSubMapMetrics.remove(clientuid);
	}

	@Override
	public RemoteSubscriptionMetrics getSubMetricsByClientUid(String clientuid) {
		return clientUidToSubMapMetrics.get(clientuid);
	}

	@Override
	public void putClientStreamToSession(String sessionId, GuardedStreamHolder<FederatedEvent> guardedStreamHolder) {
		sessionClientStreamMap.put(sessionId, guardedStreamHolder);
	}

	@Override
	public GuardedStreamHolder<FederatedEvent> removeClientStreamHolderBySession(String sessionId) {
		return sessionClientStreamMap.remove(sessionId);
	}

	@Override
	public GuardedStreamHolder<FederatedEvent> getClientStreamBySession(String sessionId) {
		return sessionClientStreamMap.get(sessionId);
	}

	@Override
	public void putClientROLStreamToSession(String sessionId, GuardedStreamHolder<ROL> guardedStreamHolder) {
		sessionROLClientStreamMap.put(sessionId, guardedStreamHolder);
	}

	@Override
	public GuardedStreamHolder<ROL> removeClientROLStreamBySession(String sessionId) {
		return sessionROLClientStreamMap.remove(sessionId);
	}

	@Override
	public GuardedStreamHolder<ROL> getClientROLStreamBySession(String sessionId) {
		return sessionROLClientStreamMap.get(sessionId);
	}

	@Override
	public void putServerGroupStreamToSession(String sessionId, GuardedStreamHolder<FederateGroups> groupStream) {
		sessionGroupServerStreamMap.put(sessionId, groupStream);	
	}

	@Override
	public GuardedStreamHolder<FederateGroups> removeServerGroupStreamBySession(String sessionId) {
		return sessionGroupServerStreamMap.remove(sessionId);
	}

	@Override
	public GuardedStreamHolder<FederateGroups> getServerGroupStreamBySession(String sessionId) {
		return sessionGroupServerStreamMap.get(sessionId);
	}

	@Override
	public void putRemoteContactsMapToChannelHandler(ChannelHandler channelHandler, ConcurrentHashMap<String, RemoteContactWithSA> remoteContactMap) {
		contactList.put(channelHandler, remoteContactMap);
	}

	@Override
	public ConcurrentHashMap<String, RemoteContactWithSA> removeRemoteContactsMapByChannelHandler(ChannelHandler channelHandler) {
		return contactList.remove(channelHandler);
	}

	@Override
	public ConcurrentHashMap<String, RemoteContactWithSA> getRemoteContactsMapByChannelHandler(ChannelHandler channelHandler) {
		return contactList.get(channelHandler);
	}

	@Override
	public void putUidToMission(String mission, String uid) {
		missionUidMap.put(mission, uid);
	}

	@Override
	public void removeUidByMission(String mission, String uid) {
		missionUidMap.remove(mission, uid);
	}

	@Override
	public Collection<String> getUidsByMission(String mission) {
		return getLocalUidsByMission(mission);
	}
	
	@Override
	public Collection<String> getLocalUidsByMission(String mission) {
		return missionUidMap.get(mission);
	}

	@Override
	public void putMissionToUid(String uid, String mission) {
		uidMissionMap.put(uid, mission);
	}

	@Override
	public void removeMissionByUid(String uid, String mission) {
		uidMissionMap.remove(uid, mission);
	}

	@Override
	public Collection<String> getMissionsByUid(String uid) {
		return uidMissionMap.get(uid);
	}

	private Map<String, Set<String>> federateRemoteGroups = new ConcurrentHashMap<>();

	public Map<String, Set<String>> getFederateRemoteGroups() {
		return federateRemoteGroups;
	}

	@Override
	public void putUidToMissionContents(String mission, String uid) {
		missionContentsUidMap.put(mission, uid);
	}

	@Override
	public void removeUidByMissionContents(String mission, String uid) {
		missionContentsUidMap.remove(mission, uid);
	}

	@Override
	public Collection<String> getUidsByMissionContents(String mission) {
		return missionContentsUidMap.get(mission);
	}

	@Override
	public void putMissionToContentsUid(String uid, String mission) {
		uidMissionContentsMap.put(uid, mission);
	}

	@Override
	public void removeMissionByContentsUid(String uid, String mission) {
		uidMissionContentsMap.remove(uid, mission);
	}

	@Override
	public Collection<String> getMissionsByContentsUid(String uid) {
		return uidMissionContentsMap.get(uid);
	}
	
	@Override
	public void removeMission(String missionName, Set<String> uids) {
		for (String contentUid : uids) {
			removeMissionByContentsUid(contentUid, missionName);
			removeUidByMissionContents(missionName, contentUid);
		}
	}
	
	@Override
	public void clearFederationCaches() {
		getFederationSubscriptionCache().clear();
		getFederationOutgoingConnectionStatusCache().clear();
	}
}
