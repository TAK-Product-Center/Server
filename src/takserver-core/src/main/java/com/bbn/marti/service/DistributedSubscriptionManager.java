

package com.bbn.marti.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.ignite.events.DiscoveryEvent;
import org.apache.ignite.events.EventType;
import org.apache.ignite.lang.IgnitePredicate;
import org.apache.ignite.services.ServiceContext;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.XPath;
import org.dom4j.io.SAXReader;
import org.owasp.esapi.Validator;
import org.owasp.esapi.errors.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import com.bbn.cot.filter.DropEventFilter;
import com.bbn.cot.filter.GeospatialEventFilter;
import com.bbn.cot.filter.StreamingEndpointRewriteFilter;
import com.bbn.marti.config.CertificateSigning;
import com.bbn.marti.config.Dropfilter;
import com.bbn.marti.config.Filter;
import com.bbn.marti.config.GeospatialFilter;
import com.bbn.marti.config.TAKServerCAConfig;
import com.bbn.marti.groups.CommonGroupDirectedReachability;
import com.bbn.marti.groups.GroupFederationUtil;
import com.bbn.marti.groups.MessagingUtil;
import com.bbn.marti.groups.MessagingUtilImpl;
import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.nio.channel.base.AbstractBroadcastingChannelHandler;
import com.bbn.marti.nio.codec.Codec;
import com.bbn.marti.nio.netty.NioNettyBuilder;
import com.bbn.marti.nio.protocol.Protocol;
import com.bbn.marti.nio.protocol.base.AbstractBroadcastingProtocol;
import com.bbn.marti.nio.server.NioServer;
import com.bbn.marti.nio.websockets.NioWebSocketHandler;
import com.bbn.marti.remote.RemoteSubscription;
import com.bbn.marti.remote.RemoteSubscriptionMetrics;
import com.bbn.marti.remote.SubscriptionManagerLite;
import com.bbn.marti.remote.exception.TakException;
import com.bbn.marti.remote.groups.AuthenticatedUser;
import com.bbn.marti.remote.groups.ConnectionInfo;
import com.bbn.marti.remote.groups.ConnectionType;
import com.bbn.marti.remote.groups.Direction;
import com.bbn.marti.remote.groups.FederateUser;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.remote.groups.Reachability;
import com.bbn.marti.remote.groups.User;
import com.bbn.marti.remote.socket.SituationAwarenessMessage;
import com.bbn.marti.remote.util.DateUtil;
import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.marti.sync.model.MissionSubscription;
import com.bbn.marti.sync.repository.MissionSubscriptionRepository;
import com.bbn.marti.util.Assertion;
import com.bbn.marti.util.CommonUtil;
import com.bbn.marti.util.MessageConversionUtil;
import com.bbn.marti.util.MessagingDependencyInjectionProxy;
import com.bbn.marti.util.Tuple;
import com.bbn.marti.util.concurrent.future.AsyncFuture;
import com.bbn.marti.util.spring.SpringContextBeanForApi;
import com.bbn.metrics.dto.MetricSubscription;
import com.bbn.security.web.MartiValidator;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

import io.micrometer.core.instrument.Metrics;
import tak.server.Constants;
import tak.server.cluster.ClusterManager;
import tak.server.cot.CotEventContainer;
import tak.server.federation.FederateSubscription;
import tak.server.federation.FigFederateSubscription;
import tak.server.ignite.IgniteHolder;
import tak.server.ignite.cache.IgniteCacheHolder;

public class DistributedSubscriptionManager implements SubscriptionManager, org.apache.ignite.services.Service {
	
	private static final long serialVersionUID = 7990324163612197225L;
	private static final String OUTGOING_DELETED_FROM_UI = "Outgoing deleted from UI";

	public static String SUBSCRIPTION_PREFIX = "subscription.static.";
	private static final Logger logger = LoggerFactory.getLogger(DistributedSubscriptionManager.class);
	
	private static final Logger dupeLogger = LoggerFactory.getLogger("dupe-message-logger");
	
	private final Logger changeLogger = LoggerFactory.getLogger(Constants.CHANGE_LOGGER);

	private Validator validator;

	public DistributedSubscriptionManager() {
		if (logger.isDebugEnabled()) {
			logger.debug("DistributedSubscriptionManager constructor");
		}
	}
    
	private static DistributedSubscriptionManager instance = null;
	
    public static synchronized DistributedSubscriptionManager getInstance() {
		if (instance == null) {
			synchronized (DistributedSubscriptionManager.class) {
				if (instance == null) {
					instance = SpringContextBeanForApi.getSpringContext().getBean(DistributedSubscriptionManager.class);
				}
			}
		}

		return instance;
	}

	public synchronized Validator getValidator() {
		if (validator == null) {
			validator = SpringContextBeanForApi.getSpringContext().getBean(Validator.class);
		}

		return validator;
	}

    private final AtomicReference<GroupManager> groupManager = new AtomicReference<>();
	
	private GroupManager groupManager() {
		if (groupManager.get() == null) {
			synchronized (this) {
				if (groupManager.get() == null) {
					groupManager.set(SpringContextBeanForApi.getSpringContext().getBean(GroupManager.class));
				}
			}
		}
		
		return groupManager.get();
	}
	
	private final AtomicReference<CommonUtil> commonUtil = new AtomicReference<>();
	
	private CommonUtil commonUtil() {
		if (commonUtil.get() == null) {
			synchronized (this) {
				if (commonUtil.get() == null) {
					commonUtil.set(SpringContextBeanForApi.getSpringContext().getBean(CommonUtil.class));
				}
			}
		}
		
		return commonUtil.get();
	}
    
    @Override
	public void cancel(ServiceContext ctx) {
    	if (logger.isDebugEnabled()) {
    		logger.debug(getClass().getSimpleName() + " cancelled");
    	}
	}

	@Override
	public void init(ServiceContext ctx) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug(getClass().getSimpleName() + " init");
		}
	}

	@Override
	public void execute(ServiceContext ctx) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug(getClass().getSimpleName() + " execute");
		}
	}
	
	private AtomicReference<AtomicLong> clientCountRef = new AtomicReference<>(new AtomicLong(0L));
	
	private AtomicLong clientCount() {
		return clientCountRef.get();
	}
	
	@EventListener({ContextRefreshedEvent.class})
	private void onContextRefresh() {
		if (DistributedConfiguration.getInstance().getRemoteConfiguration().getCluster().isEnabled()) {
			setupIgniteListeners();
		}
        // load static subscriptions
        loadStaticSubscriptions();
        schedulePeriodicSubscriptionCacheUpdates();
        
		clientCountRef.set(Metrics.gauge(Constants.METRIC_CLIENT_COUNT, clientCountRef.get()));
	}
	
	private void schedulePeriodicSubscriptionCacheUpdates() {
		Resources.metricsReportingPool.scheduleWithFixedDelay(() -> {
			Map<String,RemoteSubscription> uidMap = new HashMap<>();
			Map<String,RemoteSubscription> cuidMap = new HashMap<>();
			subscriptionStore()
				.getAllSubscriptions()
				.stream()
				.filter(sub -> sub.hasUpdate.getAndSet(false))
				.forEach(sub -> {
					RemoteSubscription rs = new RemoteSubscription(sub);
					rs.prepareForSerialization();
					uidMap.put(rs.uid, rs);
					cuidMap.put(rs.clientUid, rs);
				});
			
			IgniteCacheHolder.getIgniteSubscriptionUidTackerCache().putAll(uidMap);
			IgniteCacheHolder.getIgniteSubscriptionClientUidTackerCache().putAll(cuidMap);
		}, 5, 5, TimeUnit.SECONDS);
	}
	
	private void setupIgniteListeners() {
		// we need to remove websocket subscriptions that were created from an api node that went down
		IgnitePredicate<DiscoveryEvent> ignitePredicate = new IgnitePredicate<DiscoveryEvent>() {
			@Override
			public boolean apply(DiscoveryEvent event) {
				// only react to api nodes, this will only ever be called in the cluster
				if (Constants.API_PROFILE_NAME.equals(event.eventNode().attribute(Constants.TAK_PROFILE_KEY))) {
					websocketMap.entrySet().stream().forEach(e -> {
						if (e.getValue().getApiNode().equals(event.eventNode().id())) {
							removeWebsocketSubscription(e.getKey());
						}
					});
				}
				return true;
			}
		};

		IgniteHolder.getInstance().getIgnite().events().localListen(ignitePredicate, EventType.EVT_NODE_LEFT, EventType.EVT_NODE_FAILED);
	}
	
	public synchronized void loadStaticSubscriptions() {
	 	
	 	for (com.bbn.marti.config.Subscription.Static staticSubscription : config().getSubscription().getStatic()) {
	 	    
	 	    if (logger.isDebugEnabled()) {
	 	    	logger.debug("Adding static subscription: " + staticSubscription);
	 	    }
     		try {
     		   	String fullName = SUBSCRIPTION_PREFIX + staticSubscription.getName();
     		   	String uid = staticSubscription.getName(); //config.getAttributeString(fullName + ".uid");
     		   	String protocolStr = staticSubscription.getProtocol();
     		   	int port = staticSubscription.getPort();
     		   	String host = staticSubscription.getAddress();
				String xpath = staticSubscription.getXpath();
				List<String> groups = staticSubscription.getFiltergroup();
				String iface = staticSubscription.getIface();

				initializeStaticSubscription(uid, protocolStr, host, iface, port, xpath, fullName, groups, staticSubscription.getFilter());
   	 	  	} catch (Exception e) {
   	 	  		logger.error("error loading static subscription: " + e.getMessage(), e);
	 	    }
		}
	}
	
	public void addFilterToSub(Subscription subscription, Filter filter) {
		if (filter != null && subscription != null) {
			if (filter.getGeospatialFilter() != null) {
				subscription.geospatialEventFilter = new GeospatialEventFilter(filter.getGeospatialFilter());
			}

			if (filter.getDropfilter() != null) {
				subscription.dropFilters = new CopyOnWriteArrayList<>();
				for (Dropfilter.Typefilter t : filter.getDropfilter().getTypefilter()) {
					subscription.dropFilters.add(new DropEventFilter(t.getType(), t.getDetail(), t.getThreshold()));
				}
			}
		}
	}

	public void initializeStaticSubscription(String uid, String protocolStr, String host, String iface,
			int port, String xpath, String name, List<String> groups, Filter filter) throws IOException {

		User user = groupFederationUtil().getAnonymousUser(name);
		groupManager().addUser(user);

		for (String s : groups) {
			groupManager().addUserToGroup(user, new Group(s, Direction.IN));
			groupManager().addUserToGroup(user, new Group(s, Direction.OUT));
		}
		
		if (xpath == null) {
			xpath = "*";
		}

		TransportCotEvent transport = TransportCotEvent.findByID(protocolStr);

		boolean isUdp = transport == TransportCotEvent.MUDP || transport == TransportCotEvent.UDP
				|| transport == TransportCotEvent.COTPROTOMUDP;
		
		if (isUdp) {
			Tuple<ChannelHandler, Protocol<CotEventContainer>> handlerAndProtocol = transport.client(iface,
					InetAddress.getByName(host), port, NioServer.getInstance(),
					Lists.newArrayList(Codec.defaultCodecSource));
			
			Subscription sub = addSubscription(uid, handlerAndProtocol.right(), handlerAndProtocol.left(), xpath, user);
			
			addFilterToSub(sub, filter);
		} else {
			if (transport == TransportCotEvent.TCP) {
				NioNettyBuilder.getInstance().buildTcpStaticSubClient(host, port, uid, protocolStr, xpath, name, user, filter);
			} else {
				NioNettyBuilder.getInstance().buildStcpStaticSubClient(host, port, uid, protocolStr, xpath, name, user, filter);
			}
		}
	}
	
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
	@SuppressWarnings("unchecked")
    public Collection<Subscription> getMatches(CotEventContainer c) {
		
		boolean isDoExplicit = doExplicitBrokering(c);
		
		if (logger.isTraceEnabled()) {
			logger.trace("isDoExplicit " + isDoExplicit);
		}
		
		if (isDoExplicit) {
			
			Collection<Subscription> rawMatches = getExplicitMatches(c);
			
			if (logger.isDebugEnabled()) {
				logger.debug("raw matches for explicit brokering: " + rawMatches);
			}

			Collection<Subscription> reachableMatches = new ConcurrentLinkedDeque<>();
			try {
			    User sender = (User) c.getContextValue(Constants.USER_KEY);
			    for (Subscription destSubscription : rawMatches) {

			        AbstractBroadcastingChannelHandler destHandler = (AbstractBroadcastingChannelHandler) destSubscription.getHandler();

                    if (destHandler == null) {
                    	if (logger.isDebugEnabled()) {
                    		logger.debug("null handler in subscription: " + destSubscription);
                    	}
                        continue;
                    }

                    User receiver = destSubscription.getUser();
                    if (receiver == null) {
                    	if (logger.isTraceEnabled()) {
                    		logger.trace("null receiver - no matches");
                    	}
                        continue;
                    }
                    
			        // source related
			        NavigableSet<Group> srcGroups = null;
                    
			        if (c.getContextValue(Constants.GROUPS_KEY) != null) {
			            try {
			                srcGroups = (NavigableSet<Group>) c.getContextValue(Constants.GROUPS_KEY);
			                if (srcGroups == null || srcGroups.isEmpty()) {
			                	if (logger.isDebugEnabled()) {
			                		logger.debug("no groups in message - " + c);
			                	}
			                } else {
			                    if (reachability().isReachable(srcGroups, receiver)) {
			                        reachableMatches.add(destSubscription);
			                    }
			                }
			            } catch (ClassCastException e) {
			            	if (logger.isDebugEnabled()) {
			            		logger.debug("Ignoring message with invalid type of group set object - " + c);
			            	}
			                continue;
			            }
			        }
			        
			        if (reachableMatches.isEmpty()) {
			        	if (logger.isDebugEnabled()) {
			        		logger.debug("no reachable matches for message - trying by messaging sending user's groups");
			        	}
	                    if (reachability().isReachable(sender, receiver)) {
	                        // if no destinations, try to look at the user to determine group membership
	                        reachableMatches.add(destSubscription);
	                    }
	                }
			        
			        if (logger.isDebugEnabled()) {
			        	logger.debug("reachable matches: " + reachableMatches);
			        }
			    }

			    return reachableMatches;
			} catch (Exception e) {
				if (logger.isDebugEnabled()) {
					logger.debug("Ignoring message with invalid groups or user object type - " + c + " " + e.getMessage(), e);
				}
                return new ArrayList<>();
            }

		} else {
			
			return getImplicitMatches(c);
		}
	}
	
	private synchronized List<Subscription> getExplicitUidMatches(CotEventContainer c, List<String> destList) {
		List<Subscription> result = new LinkedList<Subscription>();
		for (String d : destList) {
			Subscription s = subscriptionStore().getSubscriptionByClientUid(d);
			if(s != null) {
				boolean found = false;
				for(Subscription i : result) {
					if(s == i) {
						found = true;
						break;
					}
				}
				if(!found) {
					result.add(s);
				}
			} else {
				logger.warn("Couldn't find subscription with destination uid: " + d);
			}
		}
		return result;
	}
	
	private List<Subscription> getFederatedMissionMatches(CotEventContainer c) {
		List<Subscription> matches = new ArrayList<>();
		
		// TODO: replace this with a ScanQuery or other filter to get the right federated subscription
		for (Subscription sub : subscriptionStore().getAllSubscriptions()) {
			if (sub instanceof FigFederateSubscription) {
				matches.add(sub);
			}
		}

		return matches;
	}
   

	/**
	* Returns whether the message should be explicitly brokered -- that is, whether an
	* explicit callsign or explicit endpoint publish list are attached to the message
	*/
	@SuppressWarnings("unchecked")
    public boolean doExplicitBrokering(CotEventContainer c) {
		
		if (logger.isDebugEnabled()) {
			logger.debug("check doExplicitBrokering for message " + c.asXml() + " context map: " + c.getContext());
		}

		List<String> callsignList = (List<String>) c.getContextValue(StreamingEndpointRewriteFilter.EXPLICIT_CALLSIGN_KEY);
		List<String> publishList = (List<String>) c.getContextValue(StreamingEndpointRewriteFilter.EXPLICIT_PUBLISH_KEY);
		List<String> uidList = (List<String>) c.getContextValue(StreamingEndpointRewriteFilter.EXPLICIT_UID_KEY);
		List<String> feedUidList = (List<String>) c.getContextValue(StreamingEndpointRewriteFilter.EXPLICIT_FEED_UID_KEY);
		Set<String> missionSet = (Set<String>) c.getContextValue(StreamingEndpointRewriteFilter.EXPLICIT_MISSION_KEY);
				
		if (missionSet != null && !missionSet.isEmpty()) {
			if (logger.isDebugEnabled()) {
				logger.debug("invalidate mission cache");
			}

//			for (String mission : missionSet) {
//				MessagingDependencyInjectionProxy.getInstance().missionService().invalidateMissionCache(mission);
//			}
		}
		
		return ((callsignList != null && !callsignList.isEmpty()) ||
				 (publishList != null && !publishList.isEmpty())) ||
				 (uidList != null && !uidList.isEmpty()) ||
				 (missionSet != null && !missionSet.isEmpty()) ||
				 feedUidList != null;
	}

	/**
	* Returns the list of subscriptions that match the explicit callsign/endpoint publish list
	* attached to the message
	*/
	@SuppressWarnings("unchecked")
    private Collection<Subscription> getExplicitMatches(CotEventContainer c) {
		Collection<Subscription> matches = new ConcurrentLinkedDeque<>();

		List<String> destList;
		if ((destList = (List<String>) c.getContextValue(StreamingEndpointRewriteFilter.EXPLICIT_PUBLISH_KEY)) != null) {
			if (logger.isTraceEnabled()) {
				for (String dest : destList) {
					logger.trace("Found explicit listing for " + dest + " with message " + c.partial());
				}
			}
			matches.addAll(getExplicitPubMatches(c, destList));
		}
		
		if ((destList = (List<String>) c.getContextValue(StreamingEndpointRewriteFilter.EXPLICIT_CALLSIGN_KEY)) != null) {
			if (logger.isTraceEnabled()) {
				for (String dest : destList) {
					logger.trace("Found explicit listing for " + dest + " with message " + c.partial());
				}
			}
			matches.addAll(getExplicitCallsignMatches(c, destList));
		}

		if ((destList = (List<String>) c.getContextValue(StreamingEndpointRewriteFilter.EXPLICIT_UID_KEY)) != null) {
			matches.addAll(getExplicitUidMatches(c, destList));
		}
		
		if ((destList = (List<String>) c.getContextValue(StreamingEndpointRewriteFilter.EXPLICIT_FEED_UID_KEY)) != null) {
			matches.addAll(getExplicitUidMatches(c, destList));
		}
		
		try {
			if (c.getContextValue(StreamingEndpointRewriteFilter.EXPLICIT_MISSION_KEY) != null) {
				matches.addAll(getFederatedMissionMatches(c));
			}
		} catch (Exception e) {
			logger.warn("exception getting explicit mission matches", e);
		}
		
		return matches;
	}

	/**
	* Currently warns that explicit endpoints are not addressable
	*
	* TODO: implement explicit endpoint to subscription matching
	*/
	private List<Subscription> getExplicitPubMatches(CotEventContainer c, List<String> destList) {
		if(logger.isWarnEnabled()) {
			for (String dest : destList) {
				logger.warn("Don't support explicit endpoint publishing yet: " + dest);
			}
		}

		return Collections.emptyList();
	}

	/**
	* Converts the list of destination callsigns for the message to 
	* subscriptions by looking them up in the callsign->subscription table
	*/	
	private synchronized List<Subscription> getExplicitCallsignMatches(CotEventContainer c, List<String> destList) {
		List<Subscription> subscriptions = new LinkedList<Subscription>();
		
		if (logger.isTraceEnabled()) {
			logger.trace("callsignMap contents: " + subscriptionStore().getViewOfCallsignMap());
		}
		
		for (String callsign : destList) {
	  		Subscription subscription = subscriptionStore().getSubscriptionByCallsign(callsign);
			if (subscription != null) {
				boolean found = false;
				for(Subscription i : subscriptions) {
					if(subscription == i) {
						found = true;
						break;
					}
				}
				if(!found) {
					
					if (logger.isTraceEnabled()) {
						logger.trace("Found explicit match for callsign " + callsign + " for message " + c.partial());
					}
					subscriptions.add(subscription);
				}
			} else {
				if (logger.isTraceEnabled()) {
					logger.trace("Couldn't find subscription with callsign: " + callsign + " for message " + c.partial());
				}
			}
		}

		return subscriptions;
	}

	/**
	* Returns the list of subscriptions interested in receiving the message
	*/
	@SuppressWarnings("unchecked")
    private Collection<Subscription> getImplicitMatches(CotEventContainer cot) {
	    
	    Collection<Subscription> matches = new ArrayList<Subscription>();

	    Collection<Subscription> subscriptions = subscriptionStore().getAllSubscriptions();
	    
	    User sender = null;
	    
	    // user is only used for logging (see below)
        if (cot.getContextValue(Constants.USER_KEY) != null) {
            try {
                sender = (User) cot.getContextValue(Constants.USER_KEY);
            } catch (ClassCastException e) {
            	if (logger.isDebugEnabled()) {
            		logger.debug("Ignoring message with invalid type of user object - " + cot);
            	}
            }
        }
        
        if (logger.isDebugEnabled()) {
        	logger.debug("message sender: " + sender + " number of subscriptions: " + subscriptions.size());
        }
        
        NavigableSet<Group> srcGroups = null;

        if (cot.getContextValue(Constants.GROUPS_KEY) != null) {
            try {
                srcGroups = (NavigableSet<Group>) cot.getContextValue(Constants.GROUPS_KEY);
                if (srcGroups == null || srcGroups.isEmpty()) {
                	if (logger.isDebugEnabled()) {
                		logger.debug("Ignoring message with no in groups - " + cot);
                	}
                    return new ArrayList<>();
                }
            } catch (ClassCastException e) {
            	if (logger.isDebugEnabled()) {
            		logger.debug("Ignoring message with invalid type of group set object - " + cot);
            	}
                return new ArrayList<>();
            }
        }
        
        if (srcGroups == null || srcGroups.isEmpty()) {
            if (sender != null) {
                // if message didn't contain any groups, so try to get groups from the user instead
                try {
                    srcGroups = groupManager().getGroups(sender);
                } catch (Exception e) {
                	if (logger.isErrorEnabled()) {
                		logger.error("remote exception getting groups" + e.getMessage(), e);
                	}
                }
            }
        }
        
        if (srcGroups == null || srcGroups.isEmpty()) {
        	if (logger.isDebugEnabled()) {
        		logger.debug("ignoring message with no destination groups " + cot);
        	}
            return new ArrayList<>();
        }

		// message contained groups. consider each destination subscription.

        final User zender = sender;
        final NavigableSet<Group> srcGroupz = srcGroups;
        
        if (dupeLogger.isTraceEnabled()) {
			dupeLogger.trace("message: " + cot + " message context map: " + cot.getContext());
		}
     
        for (Entry<String, Subscription> destSubscriptionEntry : subscriptionStore().getSubscriptionsEntries()) {
        	
        	Subscription destSubscription = destSubscriptionEntry.getValue();
      
        	if (config().getRemoteConfiguration().getFederation() == null || !config().getRemoteConfiguration().getFederation().isIVoidMyWarrantyAndWantToForwardFederationTraffic()) {
        		if (zender instanceof FederateUser && destSubscription.getUser() instanceof FederateUser) {
        			continue;
        		}
        	}

        	if (config().getRemoteConfiguration().getFederation() != null && config().getRemoteConfiguration().getFederation().isIVoidMyWarrantyAndWantToForwardFederationTraffic()) {
        		if (logger.isDebugEnabled()) {
        			logger.debug("iVoidMyWarranty");
        		}
        	}

        	// Special case for ROGER FIG federation. There is no channel handler, since we are using gRPC / Netty as the network transport.
        	// So we are keeping the reachability directly in the subscription.
        	if (destSubscription instanceof FigFederateSubscription) {

        	} else {

        		AbstractBroadcastingChannelHandler destHandler = (AbstractBroadcastingChannelHandler) destSubscription.getHandler();

        		if (destHandler == null) {
        			if (logger.isDebugEnabled()) {
        				logger.debug("null handler in subscription: " + destSubscription);
        			}
        			
        			continue;
        		}
        	}

        	User receiver = destSubscription.getUser();

        	if (receiver == null) {
        		if (logger.isDebugEnabled()) {
        			logger.debug("receiver user not found in subscription - skipping subscription: " + destSubscription);
        		}
        		
    			continue;
        	}

        	if (cot.getContextValue(Constants.REPEATER_KEY) != null) {
        		if (receiver instanceof FederateUser) {
        			if (destSubscription instanceof FederateSubscription &&
        					((FederateSubscription) destSubscription).getShareAlerts() == false) {
        				if (logger.isTraceEnabled()) {
        					logger.trace("not federating repeater message");
        				}
        				
            			continue;
        			}
        		}
        	}

        	// check if this subscriber is allowed to receive messages from the source groups
        	if (reachability().isReachable(srcGroupz, receiver)) {

        		if (logger.isDebugEnabled()) {
        			logger.debug(srcGroupz + " can reach " + receiver);
        		}

        		if (matchesXPath(cot, destSubscription.xpath) &&
        				matchesFilter(cot, destSubscription.geospatialEventFilter) &&
						matchesFilter(cot, destSubscription.dropFilters)
				) {

					if (dupeLogger.isTraceEnabled()) {
						dupeLogger.trace("dest sub: " + destSubscription + " identity hash: " + destSubscription.getHandler().identityHash());
					}

					String subIdHash = destSubscription.getHandler().identityHash();
					String msgIdHash = (String) cot.getContextValue(Constants.SOURCE_HASH_KEY);

					// don't send messages back over the EXACT same transport stream from which they came.
					if (destSubscription.getHandler() != null && (!subIdHash.equals(msgIdHash))) {
						// otherwise, add to match list

						if (logger.isDebugEnabled()) {
							logger.debug("Found implicit match for message. Receiver " + destSubscription.getHandler() + " message: " + cot.partial());
						}

						if (dupeLogger.isTraceEnabled()) {
							dupeLogger.trace("not self message - subIdHash: " + subIdHash + " msgIdHash " + msgIdHash);
						}

						matches.add(destSubscription);
					} else {
						if (dupeLogger.isTraceEnabled()) {
							dupeLogger.trace("skipping self message echo - subIdHash: " + subIdHash + " msgIdHash " + msgIdHash);
						}
					}
        		}
        	} else {
        		if (logger.isDebugEnabled()) {
        			logger.debug(srcGroupz + " cannot reach " + receiver);
        		}
        	}

        	if (logger.isTraceEnabled()) {
        		logger.trace("implicit matches: " + matches);
        	}
        }
        
        return matches;
	}

	public boolean matchesXPath(CotEventContainer cot, String xpath) {
		return (xpath == null // xpath is null
				|| xpath.trim().length() < 1 	// or xpath is empty
				|| cot.matchXPath(xpath)); // or the xpath expression actually matches
	}

	public boolean matchesFilter(CotEventContainer cot, GeospatialEventFilter geospatialEventFilter) {
		return geospatialEventFilter == null // filter is null
				|| geospatialEventFilter.filter(cot) != null; // filter actually matches
	}

	public boolean matchesFilter(CotEventContainer cot, List<DropEventFilter> dropEventFilters) {
		if (dropEventFilters == null) {
			return true;
		}

		for (DropEventFilter dropEventFilter : dropEventFilters) {
			if (dropEventFilter.filter(cot) == null) {
				return false;
			}
		}

		return true;
	}

	public boolean matchesXPath(CotEventContainer c, XPath xpath) {
		return (xpath == null 
			|| xpath.booleanValueOf(c.getDocument()));
	}

	public void addRawSubscription(Subscription subscription) {
		subscriptionStore().put(subscription.uid, subscription);
		subscriptionStore().putSubscriptionToUser(subscription.getUser(), subscription);
		
		if (subscription.getHandler() != null && ((AbstractBroadcastingChannelHandler) subscription.getHandler()).getConnectionInfo() != null) {
			subscriptionStore().putSubscriptionToConnectionInfo(((AbstractBroadcastingChannelHandler) subscription.getHandler()).getConnectionInfo(), subscription);
		}
		
		if (logger.isDebugEnabled()) {
			logger.debug("Added Raw Subscription: " + subscription);
		}
	}

	private void _addSubscription(Subscription subscription) {
		if (logger.isDebugEnabled()) {
			logger.debug("tracking uid " + subscription.uid + " for subscription " + subscription);
		}
		
		ClusterManager.addSubscription(subscription);
		
		subscriptionStore().put(subscription.uid, subscription);

		if(subscription.handler.host() != null) {  // static subs 
			logger.info("Added Subscription: id=" + subscription.uid + " source=" +
					subscription.handler.host().toString().replace("/", ""));
		}
		
		if (logger.isDebugEnabled()) {
			logger.debug("Added Subscription: " + subscription);
		}
		
		midp().eventPublisher().publishEvent(new AddSubscriptionEvent(this));
		
		clientCount().incrementAndGet();
		
	}
		
	@Override
	synchronized public void addSubscription(RemoteSubscription remote) {
		try {
			String[] tokens = remote.to.split(":");
			if (tokens.length < 3) {
				logger.error("Malformed subscription endpoint: " + remote.to);
				return;
			}

			initializeStaticSubscription(remote.uid, tokens[0],
					tokens[1], remote.iface,
					Integer.parseInt(tokens[2]), remote.xpath, remote.uid, remote.filterGroups, remote.toStatic().getFilter());

			config().getSubscription().getStatic().add(remote.toStatic());
			config().saveChangesAndUpdateCache();
		} catch (Exception e) {
			throw new TakException("Error adding subscription", e);
		}
	}
	
	@Override
	synchronized public void addSubscription(
	        final String uid,
	        final NavigableSet<Group> groups,
	        final User user,
	        final String callsign,
	        final String team,
	        final String role,
	        final String takv) {
	    
	    Objects.requireNonNull(user, "user");
	    Objects.requireNonNull(uid, "uid");
	    
	    Subscription sub = new Subscription();
	    
	    ChannelHandler dummyHandler = new AbstractBroadcastingChannelHandler() {

	        @Override
	        public AsyncFuture<ChannelHandler> close() {
	            return null;
	        }

	        @Override
	        public void forceClose() { }

	        @Override
	        public String netProtocolName() { 
	            return "";
	        }

	        @Override
	        public String toString() {
	            return uid + ":" + callsign;
	        }
	    };

	        sub.uid = "topic:" + uid;
	        sub.clientUid = uid;
	        sub.setEncoder(new AbstractBroadcastingProtocol<CotEventContainer>() {

	            @Override
	            public void onConnect(ChannelHandler handler) { }

	            @Override
	            public void onDataReceived(ByteBuffer buffer, ChannelHandler handler) { }

	            @Override
	            public AsyncFuture<Integer> write(CotEventContainer data, ChannelHandler handler) {
	                return null;
	            }

	            @Override
	            public void onInboundClose(ChannelHandler handler) { }

	            @Override
	            public void onOutboundClose(ChannelHandler handler) { }
	            
	        });

	        sub.xpath = "";

	        sub.notes = "topic:" + uid + " " + callsign + " " + team + " " + role + " " + takv;
	        sub.callsign = callsign != null ? callsign : "";
	        sub.team = team != null ? team : "";
	        sub.role = role != null ? role : "";
	        sub.takv = takv != null ? takv : "";
	        sub.to = dummyHandler.toString();
	        sub.setUser(user);
	        sub.lastProcTime = new AtomicLong(new Date().getTime());

	        sub.setHandler(dummyHandler);
	        
	        addRawSubscription(sub);
	}

	public Subscription addSubscription(
		String uid, 
		Protocol<CotEventContainer> protocol, 
		ChannelHandler handler, 
		String xpath,
		User user)
	{
		Subscription subscription = new Subscription();
		subscription.uid = uid;
		subscription.xpath = xpath;
		subscription.encoder = protocol;
		subscription.handler = handler;
		subscription.to = handler.toString();
		subscription.setUser(user);

		_addSubscription(subscription);
		
		try {
			subscriptionStore().putSubscriptionToConnectionInfo(((AbstractBroadcastingChannelHandler) handler).getConnectionInfo(), subscription);
		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception tracking subscription by ConnectionInfo", e);
			}
		}

		return subscription;
	}
	
	@Override
	public boolean deleteSubscriptionFromUI(String uid) {
		
		if (logger.isDebugEnabled()) {
			logger.debug("Deleting subscription from UI : " + uid);
		}
		
		checkAndDeleteStaticSubscription(uid);
		
        Subscription sub = subscriptionStore().getBySubscriptionUid(uid);
        try {
	    	if (sub instanceof FigFederateSubscription) {
	    		FigFederateSubscription fedsub = (FigFederateSubscription) sub;
	    		if (fedsub.getFigClient() != null) {
	    			fedsub.getFigClient().processDisconnect(new TakException(OUTGOING_DELETED_FROM_UI));
		    		return true;
	    		}
	    	}
    	} catch (Exception e) {
        	logger.warn("exception removing FigFederateSubscription", e);
        }
        
        return doDeleteSubscription(sub, uid);
	}

	@Override
	public boolean deleteSubscription(String uid) {
		
		if (logger.isDebugEnabled()) {
			logger.debug("Deleting subscription : " + uid);
		}

		checkAndDeleteStaticSubscription(uid);
		
        Subscription sub = subscriptionStore().getBySubscriptionUid(uid);
        
        return doDeleteSubscription(sub, uid);
	}
	
	private void checkAndDeleteStaticSubscription(String uid) {
		List<com.bbn.marti.config.Subscription.Static> staticSubs = config().getSubscription().getStatic();
		for(com.bbn.marti.config.Subscription.Static ss : staticSubs) {
			if(ss.getName().compareTo(uid) == 0) {
				staticSubs.remove(ss);
				config().saveChangesAndUpdateCache();
				break;
			}
        }
	}
	
	private boolean doDeleteSubscription(Subscription sub, String uid) {
		if (logger.isDebugEnabled()) {
        	logger.debug("got subscription to remove : " + sub);
        }
		
		if (sub != null && sub.handler != null) {
			sub.handler.forceClose();
		}
    	
        try {
        	if (sub instanceof FederateSubscription) {

        		FederateSubscription fedSub = (FederateSubscription) sub;

        		ChannelHandler handler = fedSub.handler;

        		messagingUtil().processFederateClose(((AbstractBroadcastingChannelHandler) handler).getConnectionInfo(), handler, sub);
        	}
        } catch (Exception e) {
        	logger.warn("exception removing federated subscription", e);
        }
        
        // remove the user and group memberships
        try {
            if (sub == null) {
            	if (logger.isDebugEnabled()) {
            		logger.debug("null subscription - no user to remove");
            	}
				return false;
            } else {
                User user = sub.getUser();
                
                if (logger.isDebugEnabled()) {
                	logger.debug("removing " + user + " for subscription " + sub);
                }
                
                if (user != null) {
                    // remove this user and associated group memberships
                    groupManager().removeUser(user);
                    
                    if (logger.isDebugEnabled()) {
                    	logger.debug(user + " removed");
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("exception during user removal", e);
        }
        
        String clientUid = sub.clientUid;
        
        try {
        	for(Map.Entry<String,Subscription> entry : subscriptionStore().getViewOfCallsignMap().entrySet()) {
        		if(entry.getValue() == sub) {
        			subscriptionStore().removeSubscriptionByCallsign(entry.getKey());
        			break;
        		}
        	}
        } catch (Exception e) {
        	if (logger.isDebugEnabled()) {
        		logger.debug("exception removing callsignMap entry", e);
        	}
        }

        try {
        	for (Map.Entry<String, Subscription> entry : subscriptionStore().getViewOfClientUidToSubMap().entrySet()) {
        		if (entry.getValue() == sub) {
        			try {
        				subscriptionStore().removeSubscriptionByClientUid(entry.getKey());
        			} catch (Exception e) {
        				if (logger.isDebugEnabled()) {
        					logger.debug("exception removing entry from clientUidToSubMap()", e);
        				}
        			}
        		}
        	}
        } catch (Exception e) {
        	if (logger.isDebugEnabled()) {
        		logger.debug("exception removing clientUidToSubMap entry", e);
        	}
        }
        
    	boolean keepMissionSubsForClientUid = false;

        try {
        	for(Map.Entry<ConnectionInfo,Subscription> entry : subscriptionStore().getViewOfConnectionSubMap().entrySet()) {
        		if(entry.getValue() == sub) {
        			subscriptionStore().removeSubscriptionByConnectionInfo(entry.getKey());
        			// if we already found a clientUid match in another sub, we can bail
        			if (keepMissionSubsForClientUid) {
        				break;
        			}
        		} else {
        			try {
        				if (entry.getValue().clientUid.equalsIgnoreCase(clientUid)) {
        					keepMissionSubsForClientUid = true;
        				}
        			} catch (Exception e) {
        				logger.warn("exception getting clientUid for Subscription");
        			}
        		}
        	}
        } catch (Exception e) {
        	logger.debug("exception removing entry from connectionSubMap", e);
        }

        
        try {
        	for(Map.Entry<User,Subscription> entry : subscriptionStore().getViewOfUserSubscriptionMap().entrySet()) {
        		if(entry.getValue() == sub) {
        			//log.warn("removing uid from sub: " + entry.getKey());
        			subscriptionStore().removeSubscriptionByUser(entry.getKey());
        			break;
        		}
        	}
        } catch (Exception e) {
        	if (logger.isDebugEnabled()) {
        		logger.debug("exception removing entry from userSubscriptionMap()", e);
        	}
        }

        try {
			if (!keepMissionSubsForClientUid) {
				for (String missionName : subscriptionStore().getMissionsByUid(clientUid)) {
					try {
						missionDisconnect(missionName, clientUid);
					} catch (Exception e) {
						logger.error("exception removing mission subscription for mission : "  +
								missionName + ", uid : " + clientUid, e);
						continue;
					}
				}
			}
		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception removing mission subscription", e);
			}
		}
		
		forceRemoveFederateSubscription(sub);

		// Remove from subscription store
		Subscription result = removeSubscriptionFromSubscriptionStore(uid);
		
		if (result != null) {
			ClusterManager.removeSubscription(sub);
			midp().eventPublisher().publishEvent(new RemoveSubscriptionEvent(this));
			
			clientCount().decrementAndGet();
		}

		return result != null;
	}

	private void forceRemoveFederateSubscription(Subscription sub) {
		try {
		    
		   if (sub instanceof FederateSubscription) {
		        FederateSubscription fedsub = (FederateSubscription) sub;
		        
		        ChannelHandler handler = fedsub.getHandler();
		        
		        ConnectionInfo connection = ((AbstractBroadcastingChannelHandler) handler).getConnectionInfo();
		        
		        handler.forceClose();
		        
		        messagingUtil().sendDisconnectMessage(sub, connection);
		        
		        groupFederationUtil().removeFedSubForConnection(connection);
		    } 
		    
		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception force removing federate " + e.getMessage());
			}
		}
	}

	private Subscription removeSubscriptionFromSubscriptionStore(String uid) {
		Subscription result = null;

		try {
			result = subscriptionStore().removeByUid(uid);
		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception removing subscription from subscription store", e);
			}
		}
		
		if (result != null) {
			logger.info("Removed Subscription: " + uid);
		} else {
			logger.info("No subscription found for removal: " + uid);
		}
		return result;
	}

	@Override
	synchronized public boolean toggleIncognito(String uid) {
		try {
			Subscription sub = subscriptionStore().getBySubscriptionUid(uid);
			sub.incognito = !sub.incognito;

			if (logger.isDebugEnabled()) {
				logger.debug("Deleting subscription : " + uid);
			}

			return sub.incognito;
		} catch (Exception e) {
			logger.error("Exception in toggleIncognito!", e);
			return false;
		}
	}
	
	/**
	* Searches for a subscription using the given handler, deleting the subscription if a mathcing one can be found
	*/
	synchronized public boolean removeSubscription(ChannelHandler handler) {
		
		if (logger.isDebugEnabled()) {
			logger.debug("remove subscription for handler " + handler);
		}
		
		if (isCyclicSubscriptionRemoval()) {
			return true;
		}
		
		AtomicBoolean result = new AtomicBoolean();

		for (Subscription subscription : subscriptionStore().getAllSubscriptions()) {

			if (subscription.handler == handler) {
				try {
					result.set(deleteSubscription(subscription.uid));
				} catch (Exception e) {
					logger.error("Exception deleting subcription", e);
				}
			}
		}
		
		return result.get();
	}
	
	private boolean isCyclicSubscriptionRemoval() {
    	StackTraceElement[] stack = new Exception().getStackTrace();
    	
    	for (StackTraceElement el : stack) {
    		
    		if (logger.isTraceEnabled()) {
    			logger.trace("stack element: " + el.getClassName());
    		}
    		
			if (el.getClassName().equals(MessagingUtilImpl.class.getName())) {
				return true;
			}
		} 

    	return false;
    }

	/**
	* converts all active subscriptions to a remote subscription list for viewing over the web
	*/	
	@Override
	public synchronized ArrayList<RemoteSubscription> getSubscriptionList() {
		ArrayList<RemoteSubscription> subscriptions = new ArrayList<RemoteSubscription>(subscriptionStore().sizeInt());
		for(Subscription subscription : subscriptionStore().getAllSubscriptions()) {
			synchronized(subscription) {
				try {
					if (!Strings.isNullOrEmpty(subscription.callsign)) {
						getValidator().getValidInput("getSubscriptionList", subscription.callsign,
								MartiValidator.Regex.MartiSafeString.name(), MartiValidator.DEFAULT_STRING_CHARS, false);
					}
					RemoteSubscription remote = new RemoteSubscription(subscription);
					remote.prepareForSerialization();
					subscriptions.add(remote);
				} catch (ValidationException validationException) {
					logger.error("invalid callsign found in getSubscriptionList!");
				}
			}
		}
		
		return subscriptions;
	}
	
	/**
	* converts active subscriptions from all messaging nodes to a remote subscription list for viewing over the web
	*/	
	@Override
	public ArrayList<RemoteSubscription> getCachedSubscriptionList(String groupVector, String sort, int direction, int page, int limit) {

		ArrayList<RemoteSubscription> cachedSubscriptionList = subscriptionStore().
				getFilteredPaginatedCachedRemoteSubscriptions(groupVector, sort, direction, page, limit);

		for (RemoteSubscription subscription : cachedSubscriptionList) {
			try {
				if (!Strings.isNullOrEmpty(subscription.callsign)) {
					getValidator().getValidInput("getCachedSubscriptionList", subscription.callsign,
							MartiValidator.Regex.MartiSafeString.name(), MartiValidator.DEFAULT_STRING_CHARS, false);
				}
			} catch (ValidationException validationException) {
				logger.error("invalid callsign found in getCachedSubscriptionList!");
			}
		}

		return cachedSubscriptionList;
	}
	
	/**
	 * converts all active subscriptions to a remote subscription list for viewing over the web. Limits results to subscriptions with access to the groupVector
	 */
	@Override
	public List<RemoteSubscription> getSubscriptionsWithGroupAccess(String groupVector, boolean noFederates, Set<Group> filterWriteOnlyGroups) {
		if (groupManager() == null) {
			logger.error("cannot obtain groupManager!");
			return new ArrayList<RemoteSubscription>();
		}

		ArrayList<RemoteSubscription> subscriptions = new ArrayList<RemoteSubscription>(subscriptionStore().sizeInt());

		Collection<Subscription> allSubs = subscriptionStore().getAllSubscriptions();
		
		if (logger.isDebugEnabled()) {
			logger.debug("getSubscriptions all subs " + allSubs);
		}
		
		Iterator<Subscription> iter = allSubs.iterator();
		while (iter.hasNext()) {
			Subscription subscription = (Subscription)iter.next();

			if (noFederates && subscription instanceof FederateSubscription) {
				if (logger.isDebugEnabled()) {
					logger.debug("Removing federate subscription from results: " + subscription);
				}
				iter.remove();
				continue;
			}

			try {
				if (!Strings.isNullOrEmpty(subscription.callsign)) {
					getValidator().getValidInput("getSubscriptionsWithGroupAccess", subscription.callsign,
							MartiValidator.Regex.MartiSafeString.name(), MartiValidator.DEFAULT_STRING_CHARS, false);
				}

				User user = subscription.getUser();
				if (user == null) {
					logger.error("found subscription with null user!");
					continue;
				}

				if (logger.isDebugEnabled()) {
					logger.debug("user " + user + " for subscription in SubscriptionManager impl: " + subscription.uid);
				}
				
				// first, filter out contact-filtered write-only groups if necessary 
				if (!filterWriteOnlyGroups.isEmpty()) {
					// groups for this subscription
					SetView<Group> commonWriteOnlyGroups = Sets.intersection(filterWriteOnlyGroups, groupManager().getGroups(subscription.getUser()));
					
					if (logger.isDebugEnabled()) {
						logger.debug("write-only group filter intersection set size " + commonWriteOnlyGroups.size());
					}

					// filter out write only groups that are configured for filtering
					if (commonWriteOnlyGroups.size() > 0) {
						continue;
					}
				}
				
				// filter by group vector intersection
				if (!groupManager().hasAccess(user.getConnectionId(), groupVector)) {
					continue;
				}

				RemoteSubscription remote = new RemoteSubscription(subscription);
				remote.prepareForSerialization();
				subscriptions.add(remote);
			} catch (ValidationException validationException) {
				logger.error("invalid callsign found in getSubscriptionsWithGroupAccess!");
			}
		}
		
		if (logger.isDebugEnabled()) {
			logger.debug("getSubscriptions result " + subscriptions);
		}

		return subscriptions;
	}
	
	/**
	 * converts all active subscriptions to a remote subscription list for viewing over the web. Limits results to
	 * subscriptions with access to the groupVector
	 */
	@Override
	public List<RemoteSubscription> getSubscriptionsWithGroupAccess(String groupVector, boolean noFederates) {
		return getSubscriptionsWithGroupAccess(groupVector, noFederates, new ConcurrentSkipListSet<Group>());
	}
	
	public String getXpathForUid(String uid) {
		Subscription s = subscriptionStore().getBySubscriptionUid(uid);
		String xpath = s.xpath;
		if (xpath == null) {
			xpath = "";
		}
		return xpath;
	}
	
	@Override
	public void setXpathForUid(String uid, String xpath) {
		Subscription subscription = subscriptionStore().getBySubscriptionUid(uid);
		Assertion.notNull(subscription, "Invalid subscription uid: " + uid);
		subscription.xpath = xpath;
	}

	public void setClientForSubscription(String clientUid, String callsign, ChannelHandler handler, boolean overwriteSub) {
		
		if (logger.isDebugEnabled()) {
			logger.debug("trying to set client for subscription: " + clientUid + " to " + callsign + " (" + handler + ")");
		}
		
        Subscription match = subscriptionStore().getByHandler(handler);
        if (match != null) {
			if(overwriteSub) {
				match.callsign = callsign;
				match.clientUid = clientUid;
			}
			
			subscriptionStore().putSubscriptionToClientUid(clientUid, match);
			subscriptionStore().putSubscriptionToCallsign(callsign, match);

            if (match.getUser() != null) {
                match.notes = match.getUser().getId();
            }
            
            logger.info("Set client for subscription: " + match.uid + " to " + callsign + " (" + clientUid + ")");
        } else {
        	logger.warn(" unable to set callsign for clientUid: " + clientUid + " to " + callsign + " (" + clientUid + ") - can't find subscription for handler " + handler);
        }
    }

	synchronized public void setClientForSubscription(Subscription sub) {
		subscriptionStore().putSubscriptionToClientUid(sub.clientUid, sub);
		subscriptionStore().putSubscriptionToCallsign(sub.callsign, sub);
    }

	synchronized public void removeClientFromSubscription(String uid, String callsign, ChannelHandler handler) {
		Subscription match = subscriptionStore().getByHandler(handler);
		if (match != null) {
			subscriptionStore().removeSubscriptionByClientUid(uid);
			subscriptionStore().removeSubscriptionByCallsign(callsign);
		}
	}

	@Override
	public RemoteSubscription getSubscriptionByCallsign(String callsign) {
		return Subscription.copyAsRemoteSubscription(
				subscriptionStore().getSubscriptionByCallsignIgnoreCase(callsign));
	}
	
	public Subscription getSubscription(User user) {
        if (user == null) {
            throw new IllegalArgumentException("null user");
        }
        
        Subscription subscription = subscriptionStore().getSubscriptionByUser(user);
        
        if (subscription == null) {
            throw new IllegalStateException("null subscription");
        }
        
        return subscription;
    }  
	
	public Subscription getSubscription(String subscriptionUid) {
        if (Strings.isNullOrEmpty(subscriptionUid)) {
            throw new IllegalArgumentException("empty uid");
        }
        
        return subscriptionStore().getBySubscriptionUid(subscriptionUid);
    }
	
	public RemoteSubscription getRemoteSubscription(String subscriptionUid) {
        if (Strings.isNullOrEmpty(subscriptionUid)) {
            throw new IllegalArgumentException("empty uid");
        }
        
        return new RemoteSubscription(subscriptionStore().getBySubscriptionUid(subscriptionUid));
    }
	
	public MetricSubscription getMetricSubscription(String subscriptionUid) {
        if (Strings.isNullOrEmpty(subscriptionUid)) {
            throw new IllegalArgumentException("empty uid");
        }
        
        return new MetricSubscription(subscriptionStore().getBySubscriptionUid(subscriptionUid));
    }


	@Override
	synchronized public Subscription getSubscriptionByClientUid(String cUid) {
	     return subscriptionStore().getSubscriptionByClientUid(cUid);
	}
	
	@Override
	synchronized public RemoteSubscription getRemoteSubscriptionByClientUid(String cUid) {
	     return Subscription.copyAsRemoteSubscription(subscriptionStore().getSubscriptionByClientUid(cUid));
	}

	@Override
	synchronized public User getSubscriptionUserByClientUid(String cUid) {
		Subscription sub = getSubscriptionByClientUid(cUid);
		if (sub == null) {
			logger.error("clientUidToSubMap failed to find : " + cUid);
			return null;
		}
		return sub.getUser();
	}

	@Override
	public RemoteSubscription getSubscriptionByUsersDisplayName(String displayName) {
		return subscriptionStore().getSubscriptionByUsersDisplayName(displayName);
	}

	// TODO: does this really need to be synchronized
	synchronized public Subscription setUserForSubscription(User user, Subscription subscription) {
	    
	    if (user == null) {
	        return subscription;
	    }
	    
		subscription.setUser(user);

		// track subscription by user
		subscriptionStore().putSubscriptionToUser(user, subscription);

		// store some information about the user in the notes field
		subscription.notes += " " + user.getId();

		if (logger.isDebugEnabled()) {
		    logger.debug("Set user for subscription: " + subscription.uid + " to " + subscription.getUser());
		}
		return subscription;
	}

	// delete message related
	private static final String detailXPath = "/event/detail";
	private static final String linkXPath = detailXPath + "/link";
	private static final String missionXPath = detailXPath + "/mission";
	private static final String missionContentXPath = detailXPath + "/mission/MissionChanges/MissionChange/content";


	private static Document deleteMessageSeed = null;
	private static Document missionChangeMessageSeed = null;
	private static Document groupChangeMessageSeed = null;

	static {
		try {
			deleteMessageSeed = DocumentHelper.parseText("<?xml version='1.0' encoding='UTF-8' standalone='yes'?><event how='h-g-i-g-o' type='t-x-d-d' version='2.0' ><point ce='9999999' le='9999999' hae='0' lat='0' lon='0' /><detail><link relation='p-p' /></detail></event>");
			groupChangeMessageSeed = DocumentHelper.parseText("<?xml version='1.0' encoding='UTF-8' standalone='yes'?><event how='h-g-i-g-o' type='t-x-g-c' version='2.0' ><point ce='9999999' le='9999999' hae='0' lat='0' lon='0' /><detail><link relation='p-p' /></detail></event>");
			missionChangeMessageSeed = DocumentHelper.parseText("<?xml version='1.0' encoding='UTF-8' standalone='yes'?><event how='h-g-i-g-o' type='t-x-m-c' version='2.0'><point ce='9999999' le='9999999' hae='0' lat='0' lon='0' /><detail><mission type=\"CHANGE\" tool=\"\"/></detail></event>");
		} catch (DocumentException e) {
			logger.error("Error parsing delete message template", e);
		}
	}

	public CotEventContainer makeDeleteMessage(String linkUid, String linkType) {
		// make delete message with the given uid/callsign in the CoT link element
		Document deleteMessage = (Document) deleteMessageSeed.clone();

		// add unique uid, start/stale/time into event element
		Element eventElem = deleteMessage.getRootElement();
		eventElem.addAttribute("uid", MessageConversionUtil.generateUid());

		long millis = System.currentTimeMillis();
		String startAndTime = DateUtil.toCotTime(millis);
		String stale = DateUtil.toCotTime(millis + 20L * 1000L);

		eventElem.addAttribute("start", startAndTime);
		eventElem.addAttribute("time", startAndTime);
		eventElem.addAttribute("stale", stale);

		Element linkElem = DocumentHelper.makeElement(deleteMessage, linkXPath);
		linkElem.addAttribute("uid", linkUid);
		linkElem.addAttribute("type", linkType);

		return new CotEventContainer(deleteMessage);
	}

	public CotEventContainer makeGroupChangeMessage() {
		Document groupChangeMessage = (Document) groupChangeMessageSeed.clone();

		// add unique uid, start/stale/time into event element
		Element eventElem = groupChangeMessage.getRootElement();
		eventElem.addAttribute("uid", MessageConversionUtil.generateUid());

		long millis = System.currentTimeMillis();
		String startAndTime = DateUtil.toCotTime(millis);
		String stale = DateUtil.toCotTime(millis + 20L * 1000L);

		eventElem.addAttribute("start", startAndTime);
		eventElem.addAttribute("time", startAndTime);
		eventElem.addAttribute("stale", stale);

		return new CotEventContainer(groupChangeMessage);
	}

	private static void addMissionXml(Document document, String xml) {
		try {
			SAXReader reader = new SAXReader();
			Document xmlDoc = reader.read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
			Element missionElem = DocumentHelper.makeElement(document, missionXPath);
			missionElem.add(xmlDoc.getRootElement());
		} catch (DocumentException ex) {
			logger.error("Exception attaching mission xml to message!", ex);
		}
	}

	private static void addMissionChangeContentXml(Document document, String xml) {
		try {
			SAXReader reader = new SAXReader();
			Document xmlDoc = reader.read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
			Element missionElem = DocumentHelper.makeElement(document, missionContentXPath);
			missionElem.add(xmlDoc.getRootElement());
		} catch (DocumentException ex) {
			logger.error("Exception attaching xml to mission change!", ex);
		}
	}

	private static CotEventContainer createMissionMessage(
			String missionName, String cotType, String msgType, String authorUid, String tool, String changes,
			String uid, String token, String roleXml, String xmlContentForNotification) {
		// make mission change message with the given mission name
		Document mcMessage = (Document) missionChangeMessageSeed.clone();

		// add unique uid, start/stale/time into event element
		Element eventElem = mcMessage.getRootElement();
		eventElem.addAttribute("uid", MessageConversionUtil.generateUid());

		eventElem.addAttribute("type", cotType);

		long millis = System.currentTimeMillis();
		String startAndTime = DateUtil.toCotTime(millis);
		String stale = DateUtil.toCotTime(millis + 20L * 1000L);

		eventElem.addAttribute("start", startAndTime);
		eventElem.addAttribute("time", startAndTime);
		eventElem.addAttribute("stale", stale);

		Element linkElem = DocumentHelper.makeElement(mcMessage, missionXPath);
		linkElem.addAttribute("name", missionName);
		linkElem.addAttribute("type", msgType);

		if (authorUid != null && authorUid.length() != 0) {
			linkElem.addAttribute("authorUid", authorUid);
		}

		if (tool != null && tool.length() != 0) {
			linkElem.addAttribute("tool", tool);
		}

		if (uid != null && uid.length() != 0) {
			linkElem.addAttribute("uid", uid);
		}

		if (token != null && token.length() != 0) {
			linkElem.addAttribute("token", token);
		}

		if (changes != null) {
			addMissionXml(mcMessage, changes);
			if (xmlContentForNotification != null) {
				addMissionChangeContentXml(mcMessage, xmlContentForNotification);
			}
		}

		if (roleXml != null) {
			addMissionXml(mcMessage, roleXml);
		}


		return new CotEventContainer(mcMessage);
	}

	public CotEventContainer createMissionChangeMessage(String missionName, ChangeType changeType, String authorUid, String tool, String changes, String xmlContentForNotification) {

		String cotType;
		switch (changeType) {
			case LOG:				{ cotType = "t-x-m-c-l"; break; }
			case KEYWORD:			{ cotType = "t-x-m-c-k"; break; }
			case UID_KEYWORD:		{ cotType = "t-x-m-c-k-u"; break; }
			case RESOURCE_KEYWORD:	{ cotType = "t-x-m-c-k-c"; break; }
			case METADATA:			{ cotType = "t-x-m-c-m"; break; }
			case EXTERNAL_DATA:		{ cotType = "t-x-m-c-e"; break; }
			default:
			case CONTENT:			{ cotType = "t-x-m-c";  break; }
		}

		return createMissionMessage(missionName, cotType, "CHANGE", authorUid, tool, changes, null, null, null, xmlContentForNotification);
    }

	public CotEventContainer createMissionCreateMessage(String missionName, String authorUid, String tool) {
		return createMissionMessage(missionName, "t-x-m-n", "CREATE", authorUid, tool, null, null, null, null, null);
	}

	public CotEventContainer createMissionDeleteMessage(String missionName, String authorUid, String tool) {
		return createMissionMessage(missionName, "t-x-m-d", "DELETE", authorUid, tool, null, null, null, null, null);
	}

	public CotEventContainer createMissionInviteMessage(String missionName, String authorUid, String tool, String token, String roleXml) {
		return createMissionMessage(missionName, "t-x-m-i", "INVITE", authorUid, tool,null, null, token, roleXml, null);
	}

	public CotEventContainer createMissionRoleChangeMessage(String missionName, String authorUid, String tool, String roleXml) {
		return createMissionMessage(missionName, "t-x-m-r", "INVITE", authorUid, tool,null, null, null, roleXml, null);
	}

	@Override
    public void missionSubscribe(String missionName, String clientUid) {
		
		if (logger.isDebugEnabled()) {
			logger.debug("subscribe to mission " + missionName + " for client uid " + clientUid);
		}

		subscriptionStore().putMissionToUid(clientUid, missionName);
		subscriptionStore().putUidToMission(missionName, clientUid);
    }

	@Override
	public void missionDisconnect(String missionName, String clientUid) {

		if (logger.isDebugEnabled()) {
			logger.debug("disconnect from mission " + missionName + " for uid " + clientUid);
		}
		
		subscriptionStore().removeMissionByUid(clientUid, missionName);
		subscriptionStore().removeUidByMission(missionName, clientUid);
	}

	@Override
	public void missionUnsubscribe(String missionName, String clientUid, String username, boolean disconnectOnly) {

		if (logger.isDebugEnabled()) {
			logger.debug("unsubscribe from mission " + missionName + " for uid " + clientUid);
		}

		missionDisconnect(missionName, clientUid);

		if (disconnectOnly) {
			return;
		}

		if (config().getRepository().isEnable()) {
			if (username != null) {
				missionSubscriptionRepository().deleteByMissionNameAndClientUidAndUsername(missionName, clientUid, username);
			} else {
				missionSubscriptionRepository().deleteByMissionNameAndClientUid(missionName, clientUid);
			}
		}
	}

	@Override
	public void removeAllMissionSubscriptions(String missionName) {
		
		if (logger.isDebugEnabled()) {
			logger.debug("removing all subscriptions from mission " + missionName);
		}
		
		for (String uid : subscriptionStore().getUidsByMission(missionName)) {
			missionUnsubscribe(missionName, uid, null, false);
		}
	}

	@Override
	public List<String> getMissionSubscriptions(String missionName, boolean connectedOnly) {
		if (connectedOnly) {
			return Lists.newArrayList(subscriptionStore().getLocalUidsByMission(missionName));
		} else {
			List<String> missionSubscriptions = new ArrayList<>();
			for (MissionSubscription missionSubscription : missionSubscriptionRepository().findAllByMissionNameNoMission(missionName)) {
				missionSubscriptions.add(missionSubscription.getClientUid());
			}
			return missionSubscriptions;
		}
	}

	@Override
    public List<String> getMissionSubscriptionsForUid(String uid) {
        return Lists.newArrayList(subscriptionStore().getMissionsByUid(uid));
    }

	@Override
    public void announceMissionChange(String missionName, String creatorUid, String tool, String changes) {
		announceMissionChange(missionName, ChangeType.CONTENT, creatorUid, tool, changes);
    }
	
	private AtomicInteger changeCount = new AtomicInteger();
	private AtomicInteger changeHitCount = new AtomicInteger();

	@Override
    public void announceMissionChange(String missionName, ChangeType changeType, String creatorUid, String tool, String changes) {
		announceMissionChange(missionName, changeType, creatorUid, tool, changes, null);
   	}

   	@Override
   	public  void announceMissionChange(String missionName, ChangeType changeType, String creatorUid, String tool, String changes, String xmlContentForNotification) {
	   CotEventContainer changeMessage = createMissionChangeMessage(missionName, changeType, creatorUid, tool, changes, xmlContentForNotification);

	   if (DistributedConfiguration.getInstance().getRemoteConfiguration().getCluster().isEnabled()) {
		   MessagingDependencyInjectionProxy.getInstance().clusterManager().onAnnounceMissionChangeMessage(changeMessage, missionName);
	   } else {
		   submitAnnounceMissionChangeCot(missionName, changeMessage);
	   }
   	}

	public void submitAnnounceMissionChangeCot(String missionName, CotEventContainer changeMessage) {
		if (changeLogger.isDebugEnabled()) {
    		changeLogger.debug("announce mission change for mission " + missionName);
    		changeCount.incrementAndGet();
    	}
        
        Set<String> explicitTopics = new HashSet<>();
        Set<String> websocketHits = new ConcurrentSkipListSet<>();
        
        // iterate through subscribers for this mission, and send them a change message 
        for (String uid : subscriptionStore().getLocalUidsByMission(missionName)) {        	
        	if (changeLogger.isDebugEnabled()) {
        		changeHitCount.incrementAndGet();
        	}

        	if (changeLogger.isTraceEnabled()) {
        		changeLogger.trace("for uid " + uid + " mission change CoT message: " + changeMessage);
        	}

            
            if (uid != null && uid.startsWith("topic:")) {
                explicitTopics.add(uid.replaceFirst("topic:", ""));

            } else {

                Subscription sub = getSubscriptionByClientUid(uid);

                if (sub == null) {
                	if (logger.isDebugEnabled()) {
                		 logger.debug("subscription not found for uid " + uid);
                	}
                    continue;
                }

                try {
                	if (sub.isWebsocket.get() && sub.getHandler() instanceof AbstractBroadcastingChannelHandler) {
        				websocketHits.add(((AbstractBroadcastingChannelHandler) sub.getHandler()).getConnectionId());
        			} else {
        				sub.submit(changeMessage);
        			}
                } catch (Exception e) {
                    logger.warn("exception sending mission change message " + e.getMessage(), e);
                }
            }
        }
        
        if (changeLogger.isDebugEnabled()) {
    		changeLogger.debug("total change hits: " + changeHitCount.get());
    	}
        
        if (!websocketHits.isEmpty()) {
			WebsocketMessagingBroker.brokerWebSocketMessage(websocketHits, changeMessage);
		} 
        
        if (!explicitTopics.isEmpty()) {
            
            changeMessage.setContext(Constants.TOPICS_KEY, explicitTopics);

        }
	}

    @Override
    public void broadcastMissionAnnouncement(
    		String missionName, String groupVector, String creatorUid, SubscriptionManagerLite.ChangeType changeType, String tool) {

    	if (logger.isDebugEnabled()) {
    		logger.debug("broadcasting notification for mission " + missionName);
    	}

		CotEventContainer message = null;

		if (changeType == ChangeType.MISSION_CREATE) {
			message = createMissionCreateMessage(missionName, creatorUid, tool);
		} else if (changeType == ChangeType.MISSION_DELETE) {
			message = createMissionDeleteMessage(missionName, creatorUid, tool);
		} else if (changeType == ChangeType.KEYWORD || changeType == ChangeType.METADATA) {
			message = createMissionChangeMessage(
					missionName, changeType, creatorUid, tool, null, null);
		} else {
			logger.error("attempt to broadcast unsupported change type: " + changeType);
			return;
		}
    	
		if (DistributedConfiguration.getInstance().getRemoteConfiguration().getCluster().isEnabled()) {
			MessagingDependencyInjectionProxy.getInstance().clusterManager().onBroadcastMissionAnnouncementMessage(message, creatorUid, groupVector);
		} else {
			submitBroadcastMissionAnnouncementCot(creatorUid, groupVector, message); 
		}
	}

	public void submitBroadcastMissionAnnouncementCot(String creatorUid, String groupVector, CotEventContainer message) {
		BigInteger groupVectorMission = RemoteUtil.getInstance().bitVectorStringToInt(groupVector);
		Set<String> websocketHits = new ConcurrentSkipListSet<>();

    	for (Subscription sub : subscriptionStore().getAllSubscriptions()) {
    		try {    			
    			if (creatorUid.equalsIgnoreCase(sub.clientUid)) {
    				continue;
    			}

    			User user = sub.getUser();
    			NavigableSet<Group> groups =  groupManager().getGroups(user);

    			// need to add __ANON__ to groups that we get from streaming user since we don't assign
    			// __ANON__ for LDAP users. this allows LDAP users to receive broadcast announcements for
    			// missions being added to the Public/__ANON__ group.
    			Group anon = groupManager().getGroup("__ANON__", Direction.IN);
    			if (anon != null) {
    				//make a copy of the group set so we don't modify the actual user
    				groups = new ConcurrentSkipListSet<>(groups);
    				groups.add(anon);
    			} else {
    				logger.error("Unable to find __ANON__ group!");
    			}

    			BigInteger groupVectorSub = RemoteUtil.getInstance().bitVectorToInt(
    					RemoteUtil.getInstance().getBitVectorForGroups(groups));

    			// dont sent the notification if the subscription doesnt have access to the mission
    			if (groupVectorMission.and(groupVectorSub).compareTo(BigInteger.ZERO) == 0) {
    				continue;
    			}

				if (logger.isDebugEnabled()) {
					logger.debug("for uid " + sub.uid + " mission notification: " + message);
				}

				try {
					if (sub.isWebsocket.get() && sub.getHandler() instanceof AbstractBroadcastingChannelHandler) {
        				websocketHits.add(((AbstractBroadcastingChannelHandler) sub.getHandler()).getConnectionId());
        			} else {
        				sub.submit(message);
        			}
				} catch (Exception e) {
					logger.warn("exception sending mission notification " + e.getMessage(), e);
				}

    		} catch (Exception e) {
    			if (logger.isDebugEnabled()) {
    				logger.debug("exception getting subscriptions for mission announcement", e);
    			}
    		}
    	}
    	
    	if (!websocketHits.isEmpty()) {
			WebsocketMessagingBroker.brokerWebSocketMessage(websocketHits, message);
		}
	}

	public void sendMissionInvite(String missionName, String[] uids, String authorUid, String tool, String token, String roleXml) {		
		if (logger.isDebugEnabled()) {
			logger.debug("send mission invites for mission " + missionName);
		}

		CotEventContainer inviteMessage = createMissionInviteMessage(missionName, authorUid, tool, token, roleXml);
				
		if (DistributedConfiguration.getInstance().getRemoteConfiguration().getCluster().isEnabled()) {
			MessagingDependencyInjectionProxy.getInstance().clusterManager().onSendMissionInviteMessage(inviteMessage, uids);
		} else {
			submitSendMissionInviteCot(uids, inviteMessage); 
		}
	}

	public void submitSendMissionInviteCot(String[] uids, CotEventContainer inviteMessage) {
		Set<String> websocketHits = new ConcurrentSkipListSet<>();

		for (String uid : uids) {

			Subscription sub = getSubscriptionByClientUid(uid);

			if (sub == null) {
				logger.warn("subscription not found for uid " + uid);
				continue;
			}

			if (logger.isDebugEnabled()) {
				logger.debug("for uid " + uid + " mission invite CoT message: " + inviteMessage);
			}

			try {
				if (sub.isWebsocket.get() && sub.getHandler() instanceof AbstractBroadcastingChannelHandler) {
    				websocketHits.add(((AbstractBroadcastingChannelHandler) sub.getHandler()).getConnectionId());
    			} else {
    				sub.submit(inviteMessage);
    			}
			} catch (Exception e) {
				logger.warn("exception sending mission invite message " + e.getMessage(), e);
			}
		}
		
		if (!websocketHits.isEmpty()) {
			WebsocketMessagingBroker.brokerWebSocketMessage(websocketHits, inviteMessage);
		}
	}
	
	@Override
    public void sendMissionRoleChange(String missionName, String uid, String authorUid, String tool, String roleXml) {

		CotEventContainer roleChangeMessage = createMissionRoleChangeMessage(missionName, authorUid, tool, roleXml);
		
		if (DistributedConfiguration.getInstance().getRemoteConfiguration().getCluster().isEnabled()) {
			MessagingDependencyInjectionProxy.getInstance().clusterManager().onSendMissionRoleChangeMessage(roleChangeMessage, uid);
		} else {
			submitSendMissionRoleChangeCot(uid, roleChangeMessage);
		}
	}

	public void submitSendMissionRoleChangeCot(String uid, CotEventContainer roleChangeMessage) {
		Subscription sub = getSubscriptionByClientUid(uid);

		if (sub == null) {
			logger.warn("subscription not found for uid " + uid);
			return;
		}

		if (logger.isDebugEnabled()) {
			logger.debug("for uid " + uid + " mission role change CoT message: " + roleChangeMessage);
		}

		try {
			if (sub.isWebsocket.get() && sub.getHandler() instanceof AbstractBroadcastingChannelHandler) {
				Set<String> websocketHits = new ConcurrentSkipListSet<>();
				websocketHits.add(((AbstractBroadcastingChannelHandler) sub.getHandler()).getConnectionId());
				WebsocketMessagingBroker.brokerWebSocketMessage(websocketHits, roleChangeMessage);
			} else {
				sub.submit(roleChangeMessage);
			}
		} catch (Exception e) {
			logger.warn("exception sending mission role change message " + e.getMessage(), e);
		}
	}

	@Override
    public void putMissionContentUid(String missionName, String contentUid) {
        if (Strings.isNullOrEmpty(missionName) || Strings.isNullOrEmpty(contentUid)) {
            throw new IllegalArgumentException("empty mission name or content uid");
        }
        
    	subscriptionStore().putMissionToContentsUid(contentUid, missionName);
    	subscriptionStore().putUidToMissionContents(missionName, contentUid);
    }

    @Override
    public void removeMissionContentUids(String missionName, Set<String> uids) {
        if (Strings.isNullOrEmpty(missionName) || uids == null || uids.isEmpty()) {
        	return;
        }

        subscriptionStore().removeMission(missionName, uids);
    }

    @Override
    public Collection<String> getContentUidsForMission(String missionName) {

        Collection<String> uids = subscriptionStore().getUidsByMissionContents(missionName);

        if (uids == null) {
            return new HashSet<>();
        }

        return uids;
    }

    @Override
    public Collection<String> getMissionsForContentUid(String uid) {

        Collection<String> missions = subscriptionStore().getMissionsByContentsUid(uid);

        if (missions == null) {
            return new HashSet<>();
        }

        return missions;
    }
	
	@Override
	public Set<SituationAwarenessMessage> getLatestReachableSA(User destUser) {

	    if (destUser == null || groupManager() == null) {
	        throw new IllegalArgumentException("null user, GroupManager or SubscriptionManager");
	    }

	    Set<SituationAwarenessMessage> latestSASet = new HashSet<>();
	    
		if (logger.isDebugEnabled()) {
			logger.debug("get latest reachable SA for user " + destUser.getId());
		}

	    Set<User> reachableUsers = new CommonGroupDirectedReachability(groupManager()).getAllReachableFrom(destUser);

	    // find all of the subscriptions to get the latest sa for all reachable users to send out
	    for (User u : reachableUsers) {

	    	if (u != null) {
	    		try {
	    			Subscription sub = getSubscription(u);

	    			CotEventContainer cot = sub.getLatestSA();

	    			SituationAwarenessMessage saMessage = MessageConversionUtil.saMessageFromCot(cot);

	    			if (saMessage != null) {
	    				latestSASet.add(saMessage);
	    			}

	    		} catch (java.lang.IllegalStateException e) {
	    			if (logger.isTraceEnabled()) {
	    				logger.trace("ignoring non-socket web connection for latest SA");
	    			}
	    		} catch (Exception e) {
	    			if (logger.isDebugEnabled()) {
	    				logger.debug("exception getting latest sa for socket", e);
	    			}
	    		}
	    	}
	    }

	    return latestSASet;
	}

	private TAKServerCAConfig getTAKServerCAConfig() {
		try {
			CertificateSigning certificateSigningConfig = config().getRemoteConfiguration().getCertificateSigning();
			if (certificateSigningConfig == null) {
				logger.error("Certificate signing config not found!");
				return null;
			}

			TAKServerCAConfig takServerCAConfig = certificateSigningConfig.getTAKServerCAConfig();
			if (takServerCAConfig == null) {
				logger.error("TAK server CA config not found!");
				return null;
			}

			return takServerCAConfig;

		} catch (Exception e) {
			logger.error("exception in getTAKServerCAConfig!", e);
			return null;
		}
	}

	@Override
	public X509Certificate[] getSigningCertChain() {
		try {
			TAKServerCAConfig takServerCAConfig = getTAKServerCAConfig();
			if (takServerCAConfig == null) {
				logger.error("getTAKServerCAConfig return null!");
				return null;
			}

			KeyStore ks = KeyStore.getInstance(takServerCAConfig.getKeystore());
			ks.load(new FileInputStream(new File(takServerCAConfig.getKeystoreFile())),
					takServerCAConfig.getKeystorePass().toCharArray());
			
			if (logger.isTraceEnabled()) {
				logger.trace("Aliases in signing keystore: " + ks.aliases());
			}

			CertificateFactory cf = CertificateFactory.getInstance("X.509");

			List<X509Certificate> results = new ArrayList<X509Certificate>();

			Certificate[] certificates = ks.getCertificateChain(ks.aliases().nextElement());
			for (Certificate certificate : certificates) {
				ByteArrayInputStream bais =
						new ByteArrayInputStream(certificate.getEncoded());
				X509Certificate x509 = (X509Certificate) cf.generateCertificate(bais);
				results.add(x509);
			}

			return results.toArray(new X509Certificate[results.size()]);

		} catch (Exception e) {
			logger.error("exception in getSigningCertChain!", e);
			return null;
		}
	}

	@Override
	public PrivateKey getSigningKey() {
		try {
			TAKServerCAConfig takServerCAConfig = getTAKServerCAConfig();
			if (takServerCAConfig == null) {
				logger.error("getTAKServerCAConfig return null!");
				return null;
			}

			KeyStore ks = KeyStore.getInstance(takServerCAConfig.getKeystore());
			ks.load(new FileInputStream(new File(takServerCAConfig.getKeystoreFile())),
					takServerCAConfig.getKeystorePass().toCharArray());
			
			if (logger.isTraceEnabled()) {
				logger.trace("First alias in signing keystore: " + ks.aliases().nextElement());
			}
			return (PrivateKey) ks.getKey(ks.aliases().nextElement(),
					takServerCAConfig.getKeystorePass().toCharArray());

		} catch (Exception e) {
			logger.error("exception in getSigningKey!", e);
			return null;
		}
	}

	@Override
	public long getSigningValidity() {
		try {
			TAKServerCAConfig takServerCAConfig = getTAKServerCAConfig();
			if (takServerCAConfig == null) {
				logger.error("getTAKServerCAConfig return null!");
				return -1;
			}

			return takServerCAConfig.getValidityDays();

		} catch (Exception e) {
			logger.error("exception in getSigningValidity!", e);
			return -1;
		}
	}

	@Override
	public boolean setGeospatialFilterOnSubscription(String clientUid, GeospatialFilter filter) {
		try {
			Subscription subscription = getSubscriptionByClientUid(clientUid);
			if (subscription == null) {
				logger.error("failed to find clientUid in setGeospatialFilterOnSubscription: " + clientUid);
				return false;
			}

			GeospatialEventFilter geospatialEventFilter = null;
			if (filter != null) {
				geospatialEventFilter = new GeospatialEventFilter(filter);
			}

			subscription.geospatialEventFilter = geospatialEventFilter;
			return true;

		} catch (Exception e) {
			logger.error("exception in setGeospatialFilterOnSubscription!", e);
			return false;
		}
	}

	public void startProtocolNegotiation(Subscription subscription) {
		try {
			subscription.getProtocol().negotiate();
		} catch (Exception e) {
			logger.error("exception starting protocol negotiation!", e);
		}
	}

	@Override
	synchronized public void setSubscriptionsMetricsForClientUid(
			String clientUid, RemoteSubscriptionMetrics subscriptionMetrics) {
		subscriptionStore().putSubMetricsToClientUid(clientUid, subscriptionMetrics);
	}

	@Override
	public RemoteSubscriptionMetrics getSubscriptionMetricsForClientUid(String clientUid) {
		return subscriptionStore().getSubMetricsByClientUid(clientUid);
	}
	
	private DistributedConfiguration config() {
		return DistributedConfiguration.getInstance();
	}
	
	private GroupFederationUtil groupFederationUtil() {
		return GroupFederationUtil.getInstance();
	}
	
	private Reachability<User> reachability() {
		return CommonGroupDirectedReachability.getInstance();
	}
	
	private SubscriptionStore subscriptionStore() {
		return SubscriptionStore.getInstance();
	}

	private MissionSubscriptionRepository missionSubscriptionRepository() {
		return MessagingDependencyInjectionProxy.getInstance().missionSubscriptionRepository();
	}
	
	private MessagingUtil messagingUtil() {
		return MessagingUtilImpl.getInstance();
	}
	
	private ConcurrentHashMap<String, NioWebSocketHandler> websocketMap = new ConcurrentHashMap<>();

	@Override
	public String createWebsocketSubscription(UUID apiNode, User user, String inGroupVector, String outGroupVector, String sessionId, String connectionId, InetSocketAddress local, InetSocketAddress remote) {
		User coreUser = new AuthenticatedUser(user.getId(), user.getConnectionId(), user.getAddress(), user.getCert(),
				user.getName(), "", "", ConnectionType.CORE);
		try {
			Set<Group> groups = groupManager().groupVectorToGroupSet(inGroupVector, Direction.IN.getValue());
			Set<Group> outGroups = groupManager().groupVectorToGroupSet(outGroupVector, Direction.OUT.getValue());
			groups.addAll(outGroups);

			groupManager().addUser(coreUser);
			groupManager().updateGroups(coreUser, groups);
			groupManager().putUserByConnectionId(coreUser, connectionId);
			NioWebSocketHandler handler = new NioWebSocketHandler(apiNode, sessionId, connectionId, local, remote);
			handler.channelActive(null);
			websocketMap.put(connectionId, handler);
		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.error("Could not create websocket subscription",e);
			}
			return null;
		}
		
		return IgniteHolder.getInstance().getIgniteStringId();
	}
	
	@Override
	public void updateWebsocketSubscription(String inGroupVector, String outGroupVector, String connectionId) {
		Set<Group> groups = groupManager().groupVectorToGroupSet(inGroupVector, Direction.IN.getValue());
		Set<Group> outGroups = groupManager().groupVectorToGroupSet(outGroupVector, Direction.OUT.getValue());
		groups.addAll(outGroups);
		groupManager().updateGroups(groupManager().getUserByConnectionId(connectionId), groups);
	}
	
	@Override
	public void removeWebsocketSubscription(String connectionId) {
		NioWebSocketHandler handler = websocketMap.remove(connectionId);
		if (handler != null ) {
			handler.channelUnregistered(null);
		}
		User user = groupManager().getUserByConnectionId(connectionId);
		
		if (user != null) {
			groupManager().removeUser(user);
		}
	}
	
	@Override
	public int getLocalSubscriptionCount() {
		return subscriptionStore().getAllSubscriptions().size();
	}
	
	private MessagingDependencyInjectionProxy midp() {
		return MessagingDependencyInjectionProxy.getInstance();
	}

	private List<User> getUsersByUsername(String username) {
		try {
			List<User> users = new ArrayList<>();
			for (User user : groupManager().getAllUsers()) {
				if (user.getConnectionType() == ConnectionType.CORE && user.getName().equals(username)) {
					users.add(user);
				}
			}

			return users;

		} catch (Exception e) {
			logger.error("exception in getUsersByUsername!", e);
			return null;
		}
	}

	@Override
	public void sendLatestReachableSA(String username) {
		try {
			List<User> users = getUsersByUsername(username);
			if (users == null || users.size() == 0) {
				logger.error("sendLatestReachableSA : User lookup failed for " + username);
				return;
			}

			for (User user : users) {
				try {
					messagingUtil().sendLatestReachableSA(user);
				} catch (Exception e) {
					logger.error("exception in sendLatestReachableSA for user : " + username, e);
				}
			}

		} catch (Exception e) {
			logger.error("exception in sendLatestReachableSA!", e);
		}
	}

	@Override
	public void sendGroupsUpdatedMessage(String username, String clientUid) {
		try {
			List<User> users = getUsersByUsername(username);
			if (users == null || users.size() == 0) {
				logger.error("sendGroupsUpdatedMessage : User lookup failed for " + username);
				return;
			}

			for (User user : users) {
				try {
					Subscription subscription = getSubscription(user);
					if (subscription == null) {
						logger.error("sendGroupsUpdatedMessage : Subscription lookup failed for " + username);
						return;
					}

					// dont send the notification back to the device that initiated the change
					if (clientUid != null && subscription.clientUid != null
							&& subscription.clientUid.equals(clientUid)) {
						continue;
					}

					CotEventContainer groupChangeMessage = makeGroupChangeMessage();
					subscription.submit(groupChangeMessage);
				} catch (Exception e) {
					logger.error("sendGroupsUpdatedMessage : exception sending to user : " + username, e);
				}
			}

		} catch (Exception e) {
			logger.error("exception in sendGroupsUpdatedMessage!", e);
		}
	}

	@Override
	public boolean deleteSubscriptionssByCertificate(X509Certificate x509Certificate) {
		List<Subscription> deletes = new LinkedList<>();
		for(Map.Entry<User,Subscription> entry : subscriptionStore().getViewOfUserSubscriptionMap().entrySet()) {
			if(entry.getKey().getCert() != null && entry.getKey().getCert().equals(x509Certificate)) {
				deletes.add(entry.getValue());
			}
		}

		boolean success = true;
		for (Subscription subscription : deletes) {
			success &= deleteSubscription(subscription.uid);
		}
		return success;
	}
}
