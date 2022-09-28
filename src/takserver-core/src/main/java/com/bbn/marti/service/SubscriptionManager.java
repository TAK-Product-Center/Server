

package com.bbn.marti.service;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.UUID;

import org.dom4j.XPath;

import com.bbn.cot.filter.GeospatialEventFilter;
import com.bbn.marti.config.Filter;
import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.nio.protocol.Protocol;
import com.bbn.marti.remote.RemoteSubscription;
import com.bbn.marti.remote.RemoteSubscriptionMetrics;
import com.bbn.marti.remote.SubscriptionManagerLite;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.User;
import com.bbn.marti.remote.socket.SituationAwarenessMessage;
import com.bbn.metrics.dto.MetricSubscription;

import tak.server.cot.CotEventContainer;

public interface SubscriptionManager extends SubscriptionManagerLite {

	void loadStaticSubscriptions();
	void initializeStaticSubscription(String uid, String protocolStr, String host, String iface, int port, String xpath, String name, List<String> groups, Filter filter) throws IOException;

	/**
	 * Returns the list of subscriptions that this message is going to.
	 * 
	 * If any explicit brokering keys are set (publish for sending to specific clients over a specified ip/port/protocol, or
	 * callsign for sending to specific streaming clients), or both, the list of subscriptions for those callsigns and endpoints
	 * is returned.
	 *
	 * Otherwise, the message is passed through every subscription's xpath, and only those subscriptions with matching xpaths are 
	 * returned. 
	 */
	Collection<Subscription> getMatches(CotEventContainer c); 

	boolean doExplicitBrokering(CotEventContainer c);

	boolean matchesXPath(CotEventContainer cot, String xpath);

	boolean matchesFilter(CotEventContainer cot, GeospatialEventFilter geospatialEventFilter);

	boolean matchesXPath(CotEventContainer c, XPath xpath);

	void addRawSubscription(Subscription subscription);

	void addSubscription(RemoteSubscription remote);

	void addSubscription(
			final String uid,
			final NavigableSet<Group> groups,
			final User user,
			final String callsign,
			final String team,
			final String role,
			final String takv);

	Subscription addSubscription(
			String uid, 
			Protocol<CotEventContainer> protocol, 
			ChannelHandler handler, 
			String xpath,
			User user);

	boolean deleteSubscriptionFromUI(String uid);

	boolean deleteSubscription(String uid);

	boolean toggleIncognito(String uid);

	/**
	 * Searches for a subscription using the given handler, deleting the subscription if a mathcing one can be found
	 */
	boolean removeSubscription(ChannelHandler handler);

	ArrayList<RemoteSubscription> getSubscriptionList();
	
	ArrayList<RemoteSubscription> getCachedSubscriptionList(String groupVector, String sort, int direction, int page, int limit);
	
	String getXpathForUid(String uid);

	void setClientForSubscription(String clientUid, String callsign, ChannelHandler handler, boolean overwriteSub);
	
	void setClientForSubscription(Subscription sub);
	
	void removeClientFromSubscription(String uid, String callsign, ChannelHandler handler);

	Subscription getSubscription(User user);
	
	Subscription getSubscription(String subscriptionUid);
	
	RemoteSubscription getRemoteSubscription(String subscriptionUid);
	
	MetricSubscription getMetricSubscription(String subscriptionUid);
	
	Subscription getSubscriptionByClientUid(String cUid);
	
	User getSubscriptionUserByClientUid(String cUid);

	Subscription setUserForSubscription(User user, Subscription subscription);
	
	CotEventContainer makeDeleteMessage(String linkUid, String linkType);

	CotEventContainer createMissionChangeMessage(String missionName, ChangeType changeType, String authorUid, String tool, String changes, String xmlContentForNotification);

	CotEventContainer createMissionCreateMessage(String missionName, String authorUid, String tool);

	CotEventContainer createMissionDeleteMessage(String missionName, String authorUid, String tool);

	CotEventContainer createMissionInviteMessage(String missionName, String authorUid, String tool, String token, String role);

	CotEventContainer createMissionRoleChangeMessage(String missionName, String authorUid, String tool, String role);

	Set<SituationAwarenessMessage> getLatestReachableSA(User destUser);

	void startProtocolNegotiation(Subscription subscription);
	
	void setSubscriptionsMetricsForClientUid(String clientUid, RemoteSubscriptionMetrics subscriptionMetrics);
	
	String createWebsocketSubscription(UUID apiNode, User user, String inGroupVector, String outGroupVector, String sessionId, String connectionId, InetSocketAddress local, InetSocketAddress remote);
	
	void updateWebsocketSubscription(String inGroupVector, String outGroupVector, String connectionId);
	
	void removeWebsocketSubscription(String connectionId);
	
	int getLocalSubscriptionCount();
}
