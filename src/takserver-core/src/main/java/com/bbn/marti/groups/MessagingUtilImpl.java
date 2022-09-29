package com.bbn.marti.groups;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.bbn.marti.feeds.DataFeedService;
import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.remote.groups.ConnectionInfo;
import com.bbn.marti.remote.groups.Direction;
import com.bbn.marti.remote.groups.FederateUser;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.remote.groups.Reachability;
import com.bbn.marti.remote.groups.User;
import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.marti.service.BrokerService;
import com.bbn.marti.service.DistributedConfiguration;
import com.bbn.marti.service.FederatedSubscriptionManager;
import com.bbn.marti.service.Subscription;
import com.bbn.marti.service.SubscriptionManager;
import com.bbn.marti.service.SubscriptionStore;
import com.bbn.marti.sync.service.DistributedDataFeedCotService;
import com.bbn.marti.util.MessagingDependencyInjectionProxy;
import com.bbn.marti.util.spring.SpringContextBeanForApi;

import tak.server.Constants;
import tak.server.cot.CotEventContainer;
import tak.server.federation.FederateSubscription;
import tak.server.federation.FigFederateSubscription;
import tak.server.federation.RemoteContactWithSA;
import tak.server.messaging.Messenger;

public class MessagingUtilImpl implements MessagingUtil {

	@Autowired
	private BrokerService broker;

	@Autowired
	private GroupManager groupManager;

	@Autowired
	private SubscriptionManager subscriptionManager;

	@Autowired
	private SubscriptionStore subscriptionStore;
	
	@Autowired
	private FederatedSubscriptionManager federatedSubscriptionManager;

	@Autowired
	private GroupFederationUtil groupFederationUtil;
	
	private static MessagingUtilImpl instance;
	
	public static MessagingUtilImpl getInstance() {
    	if (instance == null) {
    		synchronized (MessagingUtilImpl.class) {
    			if (instance == null) {	
    				instance = SpringContextBeanForApi.getSpringContext().getBean(MessagingUtilImpl.class);
    			}
    		}
    	}

    	return instance;
    }

	private static final Logger logger = LoggerFactory.getLogger(MessagingUtilImpl.class);

	private void sendLatestSA(CotEventContainer sa, Subscription dest) {
		if (sa == null) {
			return;
		}
		// make a copy of this CoT event before mutating it
		sa = sa.copy();
		// mark this message for delivery to this subscriber

		Collection<Subscription> dests = new ConcurrentLinkedDeque<Subscription>();

		dests.add(dest);

		sa.setContextValue(Constants.SUBSCRIBER_HITS_KEY, subscriptionStore.subscriptionCollectionToConnectionIdSet(dests));

		if (logger.isDebugEnabled()) {
			logger.debug("sending a latest SA message: " + sa);
		}
		
		sa.setSubmissionTime(new Date());
		sa.setCreationTime(new Date().getTime());
		
		
		// send the message
		broker.processMessage(sa);
	}

	@Override
	public void sendLatestReachableSA(User destUser) {
		// don't send latest SA if we're in vbm mode with sa sharing disabled
//		if (DistributedConfiguration.getInstance().getRemoteConfiguration().getVbm().isEnabled() && 
//				DistributedConfiguration.getInstance().getRemoteConfiguration().getVbm().isDisableSASharing()) return;

		if (destUser == null || groupManager == null || subscriptionManager == null) {
			throw new IllegalArgumentException("null user, GroupManager or SubscriptionManager");
		}

		if (logger.isDebugEnabled()) {
			logger.debug("sendLatestReachableSA " + destUser);
		}

		Subscription destSubscription = subscriptionManager.getSubscription(destUser);
		
		if (logger.isDebugEnabled()) {
			logger.debug("latestReachableSA destSubscription " + destSubscription);
		}

		if (destSubscription == null) {
			throw new IllegalStateException("subscription not found for " + destUser);
		}
		
		
		if (destSubscription instanceof FederateSubscription ) {
			// make sure if we are sending to a federate, data feed federation is enabled
			if (DistributedConfiguration.getInstance().getRemoteConfiguration().getFederation().isAllowDataFeedFederation()) {
				sendLatestFeedEventsToSub(destSubscription);
			}
		} else {
			sendLatestFeedEventsToSub(destSubscription);
		}

		Reachability<User> r = new CommonGroupDirectedReachability(groupManager);

		Collection<User> reachableUsers = r.getAllReachableFrom(destUser);
		Set<Subscription> reachableSubs = ConcurrentHashMap.newKeySet();
		
		for (User u : reachableUsers) {
			try {
				reachableSubs.add(subscriptionManager.getSubscription(u));
			} catch (IllegalStateException e) {
				if (logger.isDebugEnabled()) {
					logger.debug(e.getMessage());
				}
			} catch (Exception e) {
				if (logger.isDebugEnabled()) {
					logger.debug("exception sending latest SA", e);
				}
			}
		}
				
		reachableSubs.addAll(GroupFederationUtil.getInstance().getReachableFederatedGroupMappingSubscriptons(destSubscription));
		
		if (logger.isDebugEnabled()) {
			logger.debug("destUser " + destUser + " destSubscription for latest sa send: " + destSubscription + " reachableSubs: " + reachableSubs);
		}
		
		try {
			// find all of the subscriptions to get the latest sa for all reachable users to send out
			for (Subscription sub : reachableSubs) {
				if (sub != null) {

					if (sub.incognito) {
						continue;
					}
					
					Set<Group> federateMappingGroups = new ConcurrentSkipListSet<>();
					// if we have a group mapped FigFederateSubscription, we need to get the mapped groups so we can attach them to the message
					if (destSubscription instanceof FigFederateSubscription) {
						FigFederateSubscription figSub = (FigFederateSubscription) destSubscription;
						if (figSub.getFederate().isFederatedGroupMapping()) {
							NavigableSet<Group> inGroups = GroupFederationUtil.getInstance().filterGroupDirection(Direction.IN, groupManager.getGroups(sub.getUser()));
							GroupFederationUtil.getInstance()
									.filterFedOutboundGroups(figSub.getFederate().getOutboundGroup(), inGroups, figSub.getFederate().getId())
									.forEach(name -> federateMappingGroups.add(new Group(name, Direction.IN)));
						}
					}

					if (sub instanceof FederateSubscription) {
						if (!(destSubscription instanceof FederateSubscription)) { // don't forward federated latest SA to other federates
							List<CotEventContainer> saList = getLatestSAForHandler(sub.getHandler());
							for (CotEventContainer c : saList) {
								NavigableSet<Group> saGroups = new ConcurrentSkipListSet<>((NavigableSet<Group>)c.getContext(Constants.GROUPS_KEY));
								NavigableSet<Group> destUserGroups = groupManager.getGroups(destUser);
								saGroups.retainAll(destUserGroups);
								if (saGroups.size() > 0) {
									sendLatestSA(c, destSubscription);
								}
							}
						}
					} else {
						CotEventContainer sa = sub.getLatestSA();
						
						if (!federateMappingGroups.isEmpty()) {
							sa.setContext(Constants.GROUPS_KEY, federateMappingGroups);
						}
						
						if (logger.isDebugEnabled()) {
							logger.debug("sa to send: " + sa);
						}
						
						sendLatestSA(sa, destSubscription);
					}
				} else {
					if (logger.isDebugEnabled()) {
						logger.debug("null sa CoT event in subscription " + destSubscription + " for " + sub);
					}
				}
			}
		} catch (IllegalStateException e) {
			if (logger.isDebugEnabled()) {
				logger.debug(e.getMessage());
			}
		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception sending latest SA", e);
			}
		}
	}
	
	private void sendLatestFeedEventsToSub(Subscription sub) {
		try {
			if (DistributedConfiguration.getInstance().getRemoteConfiguration().getVbm().isEnabled()) return;
			
			Set<Group> groups = groupManager.getGroups(sub.getUser());
			groups = groups.stream().filter(g->g.getDirection() == Direction.OUT).collect(Collectors.toSet());
			String groupVector = RemoteUtil.getInstance().bitVectorToString(RemoteUtil.getInstance().getBitVectorForGroups(groups));

			DataFeedService.getDataFeedService().getDataFeedsByGroup(groupVector).forEach(feed -> {
				if (feed.isSync()) {
					Collection<CotEventContainer> events = DistributedDataFeedCotService.getInstance().getCachedDataFeedEvents(feed.getUUID());
					events.forEach(event -> {
						try {
							sub.submit(event);
						} catch (Exception e) {
							logger.error("Error submitting latest feed event to sub " + sub, e);
						}
					});
				}
			});
		} catch (Exception e) {
			logger.error("Error getting latest feed events " + sub, e);
		}
	}

	@Override
	public List<CotEventContainer> getLatestSAForHandler(ChannelHandler handler) {
		List<CotEventContainer> rval = new LinkedList<>();
		try {
			if(federatedSubscriptionManager.getRemoteContactsMapByChannelHandler(handler) == null)
				return rval;

			for (RemoteContactWithSA rc : federatedSubscriptionManager.getRemoteContactsMapByChannelHandler(handler).values()) {
				if (rc.getLastSA() != null) {
					rval.add(rc.getLastSA());
				}
			}
		} catch(Exception e) {
			logger.error("Getting latest SA for federate: " + e.toString());
		}
		return rval;
	}

	/* (non-Javadoc)
	 * @see com.bbn.marti.groups.GroupFederationUtilInterface#processFederateClose(com.bbn.marti.remote.groups.ConnectionInfo, com.bbn.marti.nio.channel.ChannelHandler)
	 */
	@Override
	public void processFederateClose(ConnectionInfo connection, ChannelHandler handler, Subscription subscription) {

		try {

			if (logger.isDebugEnabled()) {
				logger.debug("processFederateClose: connection " + connection + " handler: " + handler + " subscription: " + subscription);
			}

			if (connection != null && subscription != null) {
				// send disconnect message
				sendDisconnectMessage(Objects.requireNonNull(subscription, "federated subscription for disconnect cleanup"), connection);

				// remove federate subscription
				federatedSubscriptionManager.removeFederateSubcription(connection);
			}

			if (handler != null) {
				subscriptionManager.removeSubscription(handler);
			}

			if (subscription != null) {
				try {
					subscriptionStore.remove(subscription);
				} catch (Exception e) {
					logger.debug("exception removing subscription", e);
				}

				try {

					Entry<User, Subscription> found = null;
					for (Entry<User, Subscription> entry : subscriptionStore.getViewOfUserSubscriptionMap().entrySet()) {
						if (entry.getValue().equals(subscription)) {
							found = entry;
						}
					}

					if (found != null) {
						subscriptionStore.removeSubscriptionByUser(found.getKey());
					}

				} catch (Exception e) {
					logger.debug("exception removing sub from user subscription map", e);
				}
			}

			if (handler != null) {
				try {
					// clear remote contacts for this federate
					federatedSubscriptionManager.removeRemoteContactsMapByChannelHandler(handler);
				} catch (Exception e) {
					logger.warn("exception clearing remote contacts for federate " + subscription.uid, e);
				}
			}

		} catch(Exception e) {
			logger.error("Error cleaning up federated subscription: " + e.getMessage(), e);
		}
		logger.debug("removed federate subscription for " + connection);
	} 

	@Override
	public void sendDisconnectMessage(Subscription subscription, ConnectionInfo connection) {

		try {
			logger.debug("Sending disconnect message for all remote contacts");

			if (connection.getHandler() instanceof ChannelHandler) {

				List<CotEventContainer> latestSA = getLatestSAForHandler((ChannelHandler) connection.getHandler());
				for(CotEventContainer c : latestSA) {
					logger.debug("  Sending disconnect for remote client: " + c.getCallsign());
					sendDisconnect(c, subscription);
				}
			}
		} catch (Exception e) {
			logger.warn("exception sending federate disconnect message " + e.getMessage(), e);
		}
	}
	
	@Override
	public void sendDisconnect(CotEventContainer lastSA, Subscription subscription) {
	    Collection<Subscription> reachable = groupFederationUtil.getReachableSubscriptions(subscription);
	    if (lastSA != null) {
	        // make delete message and submit to the broker
	        CotEventContainer deleteMessage = subscriptionManager.makeDeleteMessage(lastSA.getUid(), lastSA.getType());
	        deleteMessage.setContextValue(Constants.SUBSCRIBER_HITS_KEY, subscriptionStore.subscriptionCollectionToConnectionIdSet(reachable));
	        if (subscription.getUser() != null) {
	            deleteMessage.setContextValue(Constants.USER_KEY, subscription.getUser());

				NavigableSet<Group> groups = groupManager.getGroups(subscription.getUser());
				// Only put IN groups in the message - out groups do not matter here
				deleteMessage.setContext(Constants.GROUPS_KEY, groupFederationUtil.filterGroupDirection(Direction.IN, groups));
			}
	
	        if (subscription.getHandler() != null) {
	            deleteMessage.setContext(Constants.SOURCE_TRANSPORT_KEY, subscription.getHandler());
	        }
	
	        cotMessenger().send(deleteMessage);
	        if (logger.isTraceEnabled()) {
	        	logger.trace("submitted delete message for processing: " + deleteMessage);
	        }
	    }
	}
	
	private Messenger<CotEventContainer> cotMessenger() {
		return MessagingDependencyInjectionProxy.getInstance().cotMessenger();
	}

}
