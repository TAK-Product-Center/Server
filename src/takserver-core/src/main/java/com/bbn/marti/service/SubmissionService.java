package com.bbn.marti.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import javax.xml.XMLConstants;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import io.micrometer.core.instrument.Metrics;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.ignite.Ignite;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.xml.sax.SAXException;

import atakmap.commoncommo.protobuf.v1.MessageOuterClass.Message;

import com.bbn.cot.filter.DataFeedFilter;
import com.bbn.cot.filter.DropEventFilter;
import com.bbn.cot.filter.FlowTagFilter;
import com.bbn.cot.filter.GeospatialEventFilter;
import com.bbn.cot.filter.ScrubInvalidValues;
import com.bbn.cot.filter.StreamingEndpointRewriteFilter;
import com.bbn.marti.config.Auth;
import com.bbn.marti.config.Auth.Ldap;
import com.bbn.marti.config.AuthType;
import com.bbn.marti.config.Buffer.LatestSA;
import com.bbn.marti.config.CAType;
import com.bbn.marti.config.CertificateConfig;
import com.bbn.marti.config.CertificateSigning;
import com.bbn.marti.config.Configuration;
import com.bbn.marti.config.DataFeed;
import com.bbn.marti.config.Dropfilter;
import com.bbn.marti.config.Federation.FederationServer;
import com.bbn.marti.config.Filter;
import com.bbn.marti.config.GeospatialFilter;
import com.bbn.marti.config.Input;
import com.bbn.marti.config.MicrosoftCAConfig;
import com.bbn.marti.config.NameEntries;
import com.bbn.marti.config.NameEntry;
import com.bbn.marti.config.Network.Connector;
import com.bbn.marti.config.Repository;
import com.bbn.marti.config.TAKServerCAConfig;
import com.bbn.marti.config.Tls;
import com.bbn.marti.groups.GroupFederationUtil;
import com.bbn.marti.groups.LdapAuthenticator;
import com.bbn.marti.groups.MessagingUtilImpl;
import com.bbn.marti.injector.InjectionManager;
import com.bbn.marti.nio.binder.ServerBinder;
import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.nio.channel.base.AbstractBroadcastingChannelHandler;
import com.bbn.marti.nio.channel.connections.TcpChannelHandler;
import com.bbn.marti.nio.codec.Codec;
import com.bbn.marti.nio.codec.impls.AnonymousAuthCodec;
import com.bbn.marti.nio.codec.impls.FileAuthCodec;
import com.bbn.marti.nio.codec.impls.LdapAuthCodec;
import com.bbn.marti.nio.codec.impls.SslCodec;
import com.bbn.marti.nio.codec.impls.X509AuthCodec;
import com.bbn.marti.nio.listener.AbstractAutoProtocolListener;
import com.bbn.marti.nio.listener.ProtocolListenerInstantiator;
import com.bbn.marti.nio.netty.NioNettyBuilder;
import com.bbn.marti.nio.protocol.Protocol;
import com.bbn.marti.nio.protocol.connections.StreamingCotProtocol;
import com.bbn.marti.nio.server.NioServer;
import com.bbn.marti.nio.util.CodecSource;
import com.bbn.marti.remote.AuthenticationConfigInfo;
import com.bbn.marti.remote.ConnectionEventTypeValue;
import com.bbn.marti.remote.ContactManager;
import com.bbn.marti.remote.InputMetric;
import com.bbn.marti.remote.MessagingConfigInfo;
import com.bbn.marti.remote.MessagingConfigurator;
import com.bbn.marti.remote.QueueMetric;
import com.bbn.marti.remote.RemoteContact;
import com.bbn.marti.remote.RemoteSubscriptionMetrics;
import com.bbn.marti.remote.SecurityConfigInfo;
import com.bbn.marti.remote.ServerInfo;
import com.bbn.marti.remote.exception.TakException;
import com.bbn.marti.remote.groups.ConnectionInfo;
import com.bbn.marti.remote.groups.ConnectionModifyResult;
import com.bbn.marti.remote.groups.Direction;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.remote.groups.NetworkInputAddResult;
import com.bbn.marti.remote.groups.User;
import com.bbn.marti.remote.util.DateUtil;
import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.marti.sync.repository.DataFeedRepository;
import com.bbn.marti.util.FixedSizeBlockingQueue;
import com.bbn.marti.util.MessageConversionUtil;
import com.bbn.marti.util.MessagingDependencyInjectionProxy;
import com.bbn.marti.util.Tuple;
import com.bbn.marti.remote.util.SpringContextBeanForApi;

import tak.server.CommonConstants;
import com.bbn.marti.remote.config.CoreConfigFacade;
import tak.server.Constants;
import tak.server.cache.ActiveGroupCacheHelper;
import tak.server.cache.DatafeedCacheHelper;
import tak.server.cluster.ClusterManager;
import tak.server.cot.CotElement;
import tak.server.cot.CotEventContainer;
import tak.server.cot.CotParser;
import tak.server.federation.DistributedFederationManager;
import tak.server.feeds.DataFeedDTO;
import tak.server.feeds.DataFeed.DataFeedType;
import tak.server.feeds.DataFeedStatsHelper;
import tak.server.ignite.IgniteHolder;
import tak.server.messaging.MessageConverter;

public class SubmissionService extends BaseService implements MessagingConfigurator {

    private static final Logger logger = LoggerFactory.getLogger(SubmissionService.class);
    private static final Logger dlogger = LoggerFactory.getLogger("DistributedSubmissionServiceReceiver");
	private static final Logger dupeLogger = LoggerFactory.getLogger("dupe-message-logger");
	public static String LINK_XPATH = "/event/detail/link";

    public static final String CHANNEL_TYPE_KEY = "channel.type";
    public static enum ChannelType {FED_V1, FED_V2, COT};

    private static final AtomicLong messageReceivedCluster = new AtomicLong();
    private static final AtomicLong messageReceivedLocal = new AtomicLong();
    private ScrubInvalidValues scrubInvalidValues;

    List<DropEventFilter> dropFilters = new LinkedList<>();
    static GeospatialEventFilter geospatialEventFilter = null;

    private NioServer server;

    private NioNettyBuilder nettyBuilder;

    // default latest SA and delete messages to off, unless explicitly enabled.
    private boolean enableLatestSa = false;

    // makes all points added to missions visible to __ANON__
    private boolean postMissionEventsAsPublic = false;

	private boolean alwaysArchiveMissionCot = false;

    private final DistributedFederationManager federationManager;

	private final GroupFederationUtil groupFederationUtil;

    private final GroupManager groupManager;

    private final InjectionManager injectionManager;

    private final MessageConversionUtil util;

    private final RepositoryService repositoryService;

    private final Ignite ignite;

    private final MessagingUtilImpl messagingUtil;

    private final ContactManager contactManager;

    private final FlowTagFilter flowTagFilter;

    private final ServerInfo serverInfo;

    private final MessageConverter messageConverter;

    private final RemoteUtil remoteUtil;

    private static SubmissionService instance = null;

    private static String MASK_WORD_FOR_DISPLAY = "********";

    private DataFeedRepository dataFeedRepository;
    
	@Autowired
	private DatafeedCacheHelper pluginDatafeedCacheHelper;
    
	public static SubmissionService getInstance() {
		if (instance == null) {
			synchronized (SubmissionService.class) {
				if (instance == null) {
					instance = SpringContextBeanForApi.getSpringContext().getBean(SubmissionService.class);
				}
			}
		}

		return instance;
	}

    private ConcurrentHashMap<String, LinkedBlockingQueue<ProtocolListenerInstantiator<CotEventContainer>>>
            inputListenersMap = new ConcurrentHashMap<>();

    // A lock to try and keep what is in CoreConfig and what is in active use
    // in sync without risking deadlocks if the config is being used as a lock elsewhere
    private static final Object configLock = new Object();

    // initialize control message types
    private static final Set<String> controlMsgTypes = new HashSet<String>(Arrays.asList(
            new String[]{
                    "t-b",
                    "t-b-a",
                    "t-b-c",
                    "t-b-q",
					"t-x-c-f",
					"t-x-c-t",
                    "t-x-c-t-r",
                    "t-x-takp-q",
                    "t-x-c-m",
                    "t-x-c-i-e",
                    "t-x-c-i-d"
            }));

    public SubmissionService(DistributedFederationManager dfm, NioNettyBuilder nb, MessagingUtilImpl mui, NioServer ns,
                             GroupManager gm, ScrubInvalidValues siv, MessageConversionUtil mcu,
                             GroupFederationUtil gfu, InjectionManager im, RepositoryService rs,
                             Ignite i, SubscriptionManager sm, SubscriptionStore store, FlowTagFilter flowTag,
							 ContactManager contactManager, ServerInfo serverInfo, MessageConverter messageConverter,
							 ActiveGroupCacheHelper agch, RemoteUtil remoteUtil, DataFeedRepository dfr) {
    	this.federationManager = dfm;
        this.nettyBuilder = nb;
        this.messagingUtil = mui;
        this.server = ns;
        this.groupManager = gm;
        this.scrubInvalidValues = siv;
        this.util = mcu;
        this.groupFederationUtil = gfu;
        this.injectionManager = im;
        this.repositoryService = rs;
        this.ignite = i;
        this.subMgr = sm;
        this.subscriptionStore = store;
        this.flowTagFilter = flowTag;
        this.contactManager = contactManager;
        this.serverInfo = serverInfo;
        this.messageConverter = messageConverter;
        this.remoteUtil = remoteUtil;
        this.dataFeedRepository = dfr;

		Configuration config = CoreConfigFacade.getInstance().getRemoteConfiguration();
        Auth.Ldap ldapConfig = config.getAuth().getLdap();
        if (ldapConfig != null) {
        	if (logger.isDebugEnabled()) {
        		logger.debug("setting up LDAP authenticator");
        	}

            // "inject" group manager dependency in the singleton instance of LdapAuthenticator
            LdapAuthenticator.getInstance(ldapConfig, gm);

			config.getAuth().setX509AddAnonymous(ldapConfig.isX509AddAnonymous());
			config.getAuth().setX509Groups(ldapConfig.isX509Groups());
			CoreConfigFacade.getInstance().saveChanges();
        }
    }

    SubscriptionManager subMgr;

    SubscriptionStore subscriptionStore;

    FixedSizeBlockingQueue<CotEventContainer> inputQueue = new FixedSizeBlockingQueue<CotEventContainer>();
    LinkedList<StreamingCotProtocol> protocolQueue = new LinkedList<StreamingCotProtocol>();
    AtomicInteger streamingUidGen = new AtomicInteger(1);

    @EventListener({ContextRefreshedEvent.class})
    public void init() throws IOException, DocumentException {

        Configuration config = CoreConfigFacade.getInstance().getRemoteConfiguration();

        // setup the global geospatial input filter
        if (config.getFilter().getGeospatialFilter() != null) {
            geospatialEventFilter = new GeospatialEventFilter(config.getFilter().getGeospatialFilter());
        }

        enableLatestSa = config.getBuffer().getLatestSA().isEnable();

        postMissionEventsAsPublic = config.getAuth() != null && config.getAuth().getLdap() != null &&
                config.getAuth().getLdap().isPostMissionEventsAsPublic();

        alwaysArchiveMissionCot = config.getNetwork().isAlwaysArchiveMissionCot();

        List<Input> inputs = config.getNetwork().getInput();


        if (logger.isDebugEnabled()) {
            logger.debug("number of inputs configured in CoreConfig: " + inputs.size());
        }

        if (logger.isTraceEnabled()) {
        	logger.trace("inputs from cache or local CoreConfig:");
        }

        try {
            for (Input input : inputs) {
                if (CollectionUtils.isNotEmpty(input.getFiltergroup()) && input.getAuth().equals(AuthType.X_509)) {
                    if (logger.isErrorEnabled()) {
                        logger.error("You have configured an input with both x509 auth and filter groups, the filter groups will be ignored. " + input.getFiltergroup());
                    }
                }
                addInput(input);
            }
        } catch (Exception e) {
            logger.error("Configuration or setup of network inputs failed: " + e.getMessage(), e);
            System.exit(-1);
        }
        
        List<DataFeed> feeds = config.getNetwork().getDatafeed();

        if (!feeds.isEmpty()) {
        	logger.info("listening for " + feeds.size() + " data feed(s)");
        }
        
        try {
        	Set<String> feedUids = new HashSet<>();

			if (logger.isDebugEnabled()) {
				logger.debug("Looping through DataFeed list.");
			}
            for (DataFeed feed : feeds) {
            	if (CollectionUtils.isNotEmpty(feed.getFiltergroup()) && feed.getAuth().equals(AuthType.X_509)) {
                    if (logger.isErrorEnabled()) {
                        logger.error("You have configured a datafeed with both x509 auth and filter groups, the filter groups will be ignored. {}", feed.getFiltergroup());
                    }
                }
                if (Strings.isNullOrEmpty(feed.getUuid())) {
                  logger.info("Failed to initialize Data Feed: {} because no uuid tag was specified. Here is an auto-generated uuid you can use: <uuid>{}</uuid>", feed.getName(), UUID.randomUUID().toString());
                  continue;
                }
                
                if (feedUids.contains(feed.getUuid())) {
                  logger.info("Failed to initialize Data Feed: {} because a data feed with that uuid already exists.", feed.getName());
                  continue;
                }

				if (logger.isDebugEnabled()) {
					logger.debug("Adding DataFeed UUID to set of DataFeed UUIDs.");
				}
                feedUids.add(feed.getUuid());

				if (logger.isTraceEnabled()) {
					logger.trace("Setting DataFeed values.");
				}
				DataFeedType feedType = EnumUtils.getEnumIgnoreCase(DataFeedType.class, feed.getType());
        		
        		if (feedType == null) {
        			feedType = DataFeedType.Streaming;
        		}
        		
        		feed.setType(feedType.toString());

				AuthType feedAuthType = feed.getAuth();

				if (feedAuthType == null) {
					feedAuthType = AuthType.X_509;
				}

				feed.setAuth(feedAuthType);

				// Determine the anongroup value
				boolean anonGroup;

				if (feed.isAnongroup() == null) {
					List<String> groupList = feed.getFiltergroup();
					boolean hasGroups = (groupList != null && !groupList.isEmpty());
					anonGroup = !hasGroups;
				} else {
					anonGroup = feed.isAnongroup();
				}

				feed.setAnongroup(anonGroup);

				Long dataFeedId = null;

				if (feed.getFiltergroup().isEmpty()) {
					feed.getFiltergroup().add(Constants.ANON_GROUP);
				}

				Set<Group> groups = groupManager.findGroups(feed.getFiltergroup());
				String groupVector = remoteUtil.bitVectorToString(remoteUtil.getBitVectorForGroups(groups));


				if (logger.isDebugEnabled()) {
					logger.debug("Checking if DataFeed is already in the repository.");
				}
				if (dataFeedRepository.getDataFeedByUUID(feed.getUuid()).size() > 0) {
					if (logger.isDebugEnabled()) {
						logger.debug("Updating the DataFeed in the repository.");
					}
					
					// TODO: update to support predicate feeds if necessary (may not be because the messages don't flow thru the server
					dataFeedId = dataFeedRepository.updateDataFeed(feed.getUuid(), feed.getName(), feedType.ordinal(),
							feed.getAuth().toString(), feed.getPort(), feed.isAuthRequired(), feed.getProtocol(),
							feed.getGroup(), feed.getIface(), feed.isArchive(), feed.isAnongroup(),
							feed.isArchiveOnly(), feed.getCoreVersion(), feed.getCoreVersion2TlsVersions(),
							feed.isSync(), feed.getSyncCacheRetentionSeconds(), feed.isFederated(), feed.isBinaryPayloadWebsocketOnly(), null, null, null, null);

					if (feed.getTag().size() > 0) {
						dataFeedRepository.removeAllDataFeedTagsById(dataFeedId);
						dataFeedRepository.addDataFeedTags(dataFeedId, feed.getTag().toArray(String[]::new));
					}
					if (feed.getFiltergroup().size() > 0) {
						dataFeedRepository.removeAllDataFeedFilterGroupsById(dataFeedId);
						dataFeedRepository.addDataFeedFilterGroups(dataFeedId, feed.getFiltergroup().toArray(String[]::new));
					}
				} else {
					if (dataFeedRepository.getDataFeedByName(feed.getName()).size() != 1) {
						if (logger.isDebugEnabled()) {
							logger.debug("Adding the DataFeed to the repository.");
						}
						dataFeedId = dataFeedRepository.addDataFeed(feed.getUuid(), feed.getName(), feedType.ordinal(),
								feed.getAuth().toString(), feed.getPort(), feed.isAuthRequired(), feed.getProtocol(),
								feed.getGroup(), feed.getIface(), feed.isArchive(), feed.isAnongroup(),
								feed.isArchiveOnly(), feed.getCoreVersion(), feed.getCoreVersion2TlsVersions(),
								feed.isSync(), feed.getSyncCacheRetentionSeconds(), groupVector, feed.isFederated(), feed.isBinaryPayloadWebsocketOnly(), null, null, null, null);

						if (feed.getTag().size() > 0) {
							dataFeedRepository.removeAllDataFeedTagsById(dataFeedId);
							dataFeedRepository.addDataFeedTags(dataFeedId, feed.getTag().toArray(String[]::new));
						}
						if (feed.getFiltergroup().size() > 0) {
							dataFeedRepository.removeAllDataFeedFilterGroupsById(dataFeedId);
							dataFeedRepository.addDataFeedFilterGroups(dataFeedId, feed.getFiltergroup().toArray(String[]::new));
						}
					}
				}

				if (logger.isDebugEnabled()) {
					logger.debug("Adding the DataFeed to the list of inputs.");
				}
                addInput(feed);
            }
            
            // add input metrics for federation data feeds from DB         
			if (logger.isDebugEnabled()) {
				logger.trace("Adding InputMetrics for the Federation DataFeeds.");
			}
            dataFeedRepository.getFederationDataFeeds().forEach(fedFeed -> {
            	DataFeed datafeed = fedFeed.toInput();
            	addMetric(datafeed, new InputMetric(datafeed));
            });
        } catch (Exception e) {
            logger.error("Configuration or setup of network feeds failed: " + e.getMessage(), e);
        }


        if (config.getFilter().getDropfilter() != null) {
            for (Dropfilter.Typefilter t : config.getFilter().getDropfilter().getTypefilter()) {
                if (t.getType() != null) {
                    logger.info("Adding drop event filter for type: " + t.getType());
                } else if (t.getDetail() != null) {
                    logger.info("Adding drop event filter for detail: " + t.getDetail());
                }
                dropFilters.add(new DropEventFilter(t.getType(), t.getDetail(), t.getThreshold()));
            }
        }

        if (dlogger.isDebugEnabled()) {
			dlogger.debug("listening for messages on ignite topic " + serverInfo.getSubmissionTopic());
		}

        // listen for messages on this node's submission topic
        ignite.message().localListen(serverInfo.getSubmissionTopic(), (nodeId, message) -> {
			if (logger.isDebugEnabled()) {
				logger.debug("In SubmissionTopic listener within Submission Service.");
			}
        	if (!(message instanceof byte[])) {

        		if (dlogger.isDebugEnabled()) {
        			dlogger.debug("ignoring unsupported message type " + message.getClass().getName());
        		}

        		return true;
        	}

        	byte[] protoMessage = (byte[]) message;

        	try {
				if (logger.isDebugEnabled()) {
					logger.debug("Converting Proto Message to CotEventContainer.");
				}
        		CotEventContainer cot = messageConverter.dataMessageToCot(protoMessage);

        		if (dlogger.isTraceEnabled()) {

        			@SuppressWarnings("unchecked")
        			NavigableSet<Group> groups = (NavigableSet<Group>) cot.getContextValue(Constants.GROUPS_KEY);

        			dlogger.trace("Received CoT from ignite node " + nodeId + ": " + cot + " groups: " + groups);
        		}

        		if (dupeLogger.isTraceEnabled()) {
        			@SuppressWarnings("unchecked")
        			NavigableSet<Group> groups = (NavigableSet<Group>) cot.getContextValue(Constants.GROUPS_KEY);

        			dupeLogger.trace("Received CoT from ignite node " + nodeId + ": " + cot + " groups: " + groups + " message context map: " + cot.getContext());
        		}

        		String connectionId = null;

        		// use connectionId to get the handler
        		try {

        			connectionId = (String) cot.getContext(Constants.CONNECTION_ID_KEY);

        			if (dlogger.isDebugEnabled()) {
        				dlogger.debug("connectionId in message from ignite: " + connectionId);
        			}

        			ConnectionInfo ci = new ConnectionInfo();

        			ci.setConnectionId(connectionId);

        			if (dlogger.isTraceEnabled()) {
        				dlogger.trace("connectionSubMap: " + subscriptionStore.getViewOfConnectionSubMap());
        			}

        			Subscription sub = subscriptionStore.getSubscriptionByConnectionInfo(ci);

        			if (dlogger.isDebugEnabled()) {
        				dlogger.debug("sub from connectionId " + ci + " " + sub);
        			}

        			if (sub != null) {
        				ChannelHandler handler = sub.getHandler();

        				if (dlogger.isDebugEnabled()) {
        					dlogger.debug("handler: " + handler);
        				}

        				cot.setContext(Constants.SOURCE_TRANSPORT_KEY, handler);
        			} else {
        				if (dlogger.isDebugEnabled()) {
        					dlogger.debug("null handler - can't set transport key in message received from ignite");
        				}
        			}

        		} catch (Exception e) {
        			if (dlogger.isDebugEnabled()) {
        				dlogger.debug("exception getting handler for connection id", e);
        			}
        		}

				if (logger.isDebugEnabled()) {
					logger.debug("Adding CotEventContainer to InputQueue.");
				}
        		addToInputQueue(cot);

        	} catch (RemoteException | DocumentException e) {
        		if (logger.isDebugEnabled()) {
        			logger.debug("exception deserializing data message", e);
        		}
        	}

        	// return true to continue listening
        	return true;
        });

        // use ignite for plugin messaging when outside the cluster
        if (config.getPlugins().isUsePluginMessageQueue() && !config.getCluster().isEnabled()) {

        	// listen for messages from plugins
        	ignite.message().localListen(CommonConstants.PLUGIN_PUBLISH_TOPIC, (nodeId, message) -> {
				if (logger.isDebugEnabled()) {
					logger.debug("In Plugin Publish Topic listener.");
				}
        		try {
        			
        			if (logger.isTraceEnabled()) {
        				logger.trace("received message from plugin publish topic " + CommonConstants.PLUGIN_PUBLISH_TOPIC + " " + message);
        			}
        			if (!(message instanceof byte[])) {

        				if (dlogger.isDebugEnabled()) {
        					dlogger.debug("ignoring unsupported message type " + message.getClass().getName());
        				}

        				// return true to continue listening
        				return true;
        			}

        			try {
						if (logger.isTraceEnabled()) {
							logger.trace("Parse bytes into a Message.");
						}
        				Message m = Message.parseFrom((byte[]) message);
        				if (m != null) {
        					boolean isInterceptorMessage = false;
           					List<String> provenance = m.getProvenanceList();
            				if (provenance != null && provenance.contains(Constants.PLUGIN_INTERCEPTOR_PROVENANCE)) {
            					isInterceptorMessage = true;
            				}

							if (logger.isDebugEnabled()) {
								logger.debug("Check if Interceptor Message.");
							}
        					if (isInterceptorMessage) {
            					// do not republish the message if it is marked as intercepted, submit it directly
        						SubmissionService.this.addToInputQueue(messageConverter.dataMessageToCot(m, false));
            				} else {
            					if (logger.isDebugEnabled()) {
            						logger.debug("processing through plugin pipeline");
            					}

            					// Check if the message is a datafeed
            					boolean isDataFeedMessage = false;
								if (logger.isDebugEnabled()) {
									logger.debug("Check if DataFeed Message.");
								}
            					if (m.getFeedUuid() != null && !m.getFeedUuid().isEmpty()) {
            						isDataFeedMessage = true;
            					}

								if (logger.isDebugEnabled()) {
									logger.debug("Convert Message to Plugin CotEventContainer.");
								}
            					CotEventContainer pluginCotEvent = messageConverter.dataMessageToCot(m, false);

								if (!Strings.isNullOrEmpty(pluginCotEvent.getCallsign())
										&& !Strings.isNullOrEmpty(pluginCotEvent.getEndpoint()) && !Strings.isNullOrEmpty(pluginCotEvent.getUid())) {
									if (config.getFederation() != null) {
										try {
											NavigableSet<Group> groups = (NavigableSet<Group>) pluginCotEvent.getContextValue(Constants.GROUPS_KEY);
											federationManager.addPluginContact(pluginCotEvent, groups);
										} catch (Exception e) {
											if (logger.isDebugEnabled()) {
												logger.debug("exception adding federated plugin contact", e);
											}
										}
									}
								} else if (pluginCotEvent.getType().equals("t-x-d-d")) {
									Element link = (Element)pluginCotEvent.getDocument().selectSingleNode(LINK_XPATH);
									if (link != null && link.attribute("uid") != null) {
										federationManager.removeLocalContact(link.attributeValue("uid"));
									}
								}

            					if (isDataFeedMessage) {

									if (logger.isDebugEnabled()) {
										logger.debug("Check if the DataFeed is in cache.");
									}
            						List<tak.server.plugins.PluginDataFeed> cacheResult = pluginDatafeedCacheHelper.getPluginDatafeed(m.getFeedUuid());
            						
            						if (cacheResult == null) { // Does not have in cache

										if (logger.isDebugEnabled()) {
											logger.debug("Not in cache. Check if DataFeed is in the Repository.");
										}
            							List<DataFeedDTO> dataFeedInfo = dataFeedRepository.getDataFeedByUUID(m.getFeedUuid());
                    					if (dataFeedInfo.size() == 0) {
                    						
                    						// update cache with empty result
                        					pluginDatafeedCacheHelper.cachePluginDatafeed(m.getFeedUuid(), new ArrayList<>());

                        					if (logger.isWarnEnabled()) {
                        						logger.warn("Datafeed with UUID {} does not exist. Ignore the message.", m.getFeedUuid());
                        					}
                        					
                    					} else {
                    						List<String> tags = dataFeedRepository.getDataFeedTagsById(dataFeedInfo.get(0).getId());

                    						Set<String> groupNames = RemoteUtil.getInstance().getGroupNamesForBitVectorString(
                    								dataFeedInfo.get(0).getGroupVector(), groupManager.getAllGroups());

                    						// update cache
											if (logger.isDebugEnabled()) {
												logger.debug("Create PluginDataFeed and add to Cache.");
											}
                    						List<tak.server.plugins.PluginDataFeed> pluginDatafeeds = new ArrayList<>();
                    						tak.server.plugins.PluginDataFeed pluginDataFeed = new tak.server.plugins.PluginDataFeed(
                    								m.getFeedUuid(), dataFeedInfo.get(0).getName(), tags, dataFeedInfo.get(0).getArchive(),
													dataFeedInfo.get(0).isSync(),  new ArrayList<String>(groupNames), dataFeedInfo.get(0).getFederated());
                    						
                    						pluginDatafeeds.add(pluginDataFeed);
                        					pluginDatafeedCacheHelper.cachePluginDatafeed(m.getFeedUuid(), pluginDatafeeds);
                    						
                    						com.bbn.marti.config.DataFeed dataFeed = new com.bbn.marti.config.DataFeed();
                        					dataFeed.setUuid(m.getFeedUuid());
                        					dataFeed.setName(dataFeedInfo.get(0).getName());
                        					dataFeed.getTag().addAll(tags); 
                        					dataFeed.setArchive(dataFeedInfo.get(0).getArchive());
                        					dataFeed.setSync(dataFeedInfo.get(0).isSync());
											dataFeed.getFiltergroup().addAll(groupNames);
                    						dataFeed.setFederated(dataFeedInfo.get(0).getFederated());

											if (logger.isDebugEnabled()) {
                            					logger.debug("Retrieve Datafeed info from dataFeedRepository: uuid: {}, name: {}, tags: {}, archive: {}, sync: {}, federated: {}, filtergroup: {}", dataFeed.getUuid(), dataFeed.getName(), dataFeed.getTag(), dataFeed.isArchive(), dataFeed.isSync(), dataFeed.isFederated(), dataFeed.getFiltergroup());
                        					}
                        					
                        					DataFeedFilter.getInstance().filter(pluginCotEvent, dataFeed);

											if (logger.isDebugEnabled()) {
												logger.debug("Forward the Plugin CotEventContainer along to other services.");
											}
											MessagingDependencyInjectionProxy.getInstance().cotMessenger().send(pluginCotEvent);
											if (logger.isDebugEnabled()) {
												logger.debug("Update InputMetric for DataFeed.");
											}
											InputMetric inputMetric = getInputMetric(dataFeed.getName());
											if (inputMetric != null) {
												inputMetric.getMessagesReceived().incrementAndGet();
												inputMetric.getBytesRecieved().addAndGet(((byte[]) message).length);
											}
                    					}
            							
            						} else { // exist in cache
										if (logger.isDebugEnabled()) {
											logger.debug("Found DataFeed in Cache.");
										}
            							if (cacheResult.size() == 0) { // datafeed with this uuid does not exist
            								
            								if (logger.isWarnEnabled()) {
            									logger.warn("-Datafeed with UUID {} does not exist. Ignore the message.", m.getFeedUuid());
            								}
                        					
            							} else {
											if (logger.isDebugEnabled()) {
												logger.debug("Create new DataFeed from Message.");
											}
            								com.bbn.marti.config.DataFeed dataFeed = new com.bbn.marti.config.DataFeed();
                        					dataFeed.setUuid(m.getFeedUuid());
                        					dataFeed.setName(cacheResult.get(0).getName());
                        					dataFeed.getTag().addAll(cacheResult.get(0).getTags());
                        					dataFeed.setArchive(cacheResult.get(0).isArchive());
                        					dataFeed.setSync(cacheResult.get(0).isSync());
											dataFeed.getFiltergroup().addAll(cacheResult.get(0).getFilterGroups());
                        					dataFeed.setFederated(cacheResult.get(0).isFederated());

											if (logger.isDebugEnabled()) {
												logger.debug("Retrieve Datafeed info from cache: uuid: {}, name: {}, tags: {}, archive: {}, sync: {}, federated: {}, filtergroup: {}", dataFeed.getUuid(), dataFeed.getName(), dataFeed.getTag(), dataFeed.isArchive(), dataFeed.isSync(), dataFeed.isFederated(), dataFeed.getFiltergroup());
                        					}
                        				
                        					DataFeedFilter.getInstance().filter(pluginCotEvent, dataFeed);

											if (logger.isDebugEnabled()) {
												logger.debug("Forward the Plugin CotEventContainer along to other services.");
											}
											MessagingDependencyInjectionProxy.getInstance().cotMessenger().send(pluginCotEvent);

											if (logger.isDebugEnabled()) {
												logger.debug("Update InputMetric for DataFeed.");
											}
											InputMetric inputMetric = getInputMetric(dataFeed.getName());
											if (inputMetric != null) {
												inputMetric.getMessagesReceived().incrementAndGet();
												inputMetric.getBytesRecieved().addAndGet(((byte[])message).length);
											}
            							}
            						}
									// whether its in cache or not, we need to update the data feed stats for a data feed message
									if (logger.isDebugEnabled()) {
										logger.debug("Add the Data Feed message to the DataFeed Stats.");
									}
									double lat = Double.valueOf(pluginCotEvent.getLat()).doubleValue();
									double lon = Double.valueOf(pluginCotEvent.getLon()).doubleValue();
									if (!DataFeedStatsHelper.getInstance().addStatsForDataFeedMessage(
											m.getFeedUuid(), pluginCotEvent.getType(), lat,	lon,
											pluginCotEvent.getCreationTime(), ((byte[])message).length)) {
										logger.error("Unable to add new DataFeedStats for Message.");
									}

								} else {
									if (logger.isDebugEnabled()) {
										logger.debug("Forward the Plugin CotEventContainer along to other services.");
									}
                					MessagingDependencyInjectionProxy.getInstance().cotMessenger().send(pluginCotEvent);
            					}
            					
            				}
        				}
        			
        			} catch (Exception e) {

        				if (logger.isDebugEnabled()) {
        					logger.debug("exception deserializing plugin message", e);
        				}
        			} 
        		} catch (Exception e) {
        			if (logger.isDebugEnabled()) {
        				logger.debug("exception processing pluging message received from ignite topic " + CommonConstants.PLUGIN_PUBLISH_TOPIC, e);
        			}
        		}

        		// return true to continue listening
        		return true;
        	});
        }

        // Push a dummy SA message into the system to make sure pipeline resources get initialized
        // before any actual clients try to connect. This dummy message will help alleviate first time
        // lazy initialization slowdowns
        String dummySA = "<event version=\"2.0\" uid=\"dummy\" type=\"a-f-G-U-C\" how=\"m-g\" time=\"2020-02-12T13:16:07Z\" start=\"2020-02-12T13:16:05Z\" stale=\"2020-02-12T13:16:50Z\"><point lat=\"40\" lon=\"-72\" hae=\"-22\" ce=\"4.9\" le=\"9999999.0\"/><detail><contact endpoint=\"*:-1:stcp\" callsign=\"dummy\"/><uid Droid=\"dummy\"/></detail></event>";
        try {
        	SAXReader reader = new SAXReader();
			reader.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			Document doc = reader.read(new ByteArrayInputStream(dummySA.getBytes()));
			CotEventContainer cotEventContainer = new CotEventContainer(doc);
	        MessagingDependencyInjectionProxy.getInstance().cotMessenger().send(cotEventContainer);
		} catch (SAXException e) {
			logger.error("Could not push dummy message into pipeline", e);
		}
    }

    public static class InputListenerAuxillaryRouter {

        public static void addListenersForInput(@NotNull LinkedBlockingQueue<ProtocolListenerInstantiator<CotEventContainer>> listeners, @NotNull Input input) {

            if (input.isArchiveOnly()) {
                listeners.add(onArchiveOnlyDataReceivedCallback);
            }

            if (!input.isArchive()) {
                listeners.add(onNoArchiveDataReceivedCallback);
            }
        }

        private InputListenerAuxillaryRouter() {
        }

        public static final ProtocolListenerInstantiator<CotEventContainer> onArchiveOnlyDataReceivedCallback = new AbstractAutoProtocolListener<CotEventContainer>() {
            private static final long serialVersionUID = -2338348114005512168L;

			@Override
            public void onDataReceived(CotEventContainer data, final ChannelHandler handler, final Protocol<CotEventContainer> protocol) {
                data.setContextValue(Constants.DO_NOT_BROKER_KEY, Boolean.TRUE);
            }

            @Override
            public String toString() {
                return "broker_data_manager";
            }
        };

        public static final ProtocolListenerInstantiator<CotEventContainer> onNoArchiveDataReceivedCallback = new AbstractAutoProtocolListener<CotEventContainer>() {
            private static final long serialVersionUID = 6936588138193322737L;

			@Override
            public void onDataReceived(CotEventContainer data, final ChannelHandler handler, final Protocol<CotEventContainer> protocol) {
                data.setContextValue(Constants.ARCHIVE_EVENT_KEY, Boolean.FALSE);
            }

            @Override
            public String toString() {
                return "archive_data_manager";
            }
        };
    }

    public final ProtocolListenerInstantiator<CotEventContainer> subscriptionLifecycleCallback = new AbstractAutoProtocolListener<CotEventContainer>() {
        private static final long serialVersionUID = 8948907635338980454L;

		@Override
        public void onConnect(ChannelHandler handler, Protocol<CotEventContainer> protocol) {

        	if (logger.isDebugEnabled()) {
        		logger.debug("subscriptionLifecycleCallback onConnect " + handler + " " + protocol);
        	}

            createSubscriptionFromConnection(handler, protocol, null);
        }

        @Override
        public void onOutboundClose(ChannelHandler handler, Protocol<CotEventContainer> protocol) {

        	if (logger.isDebugEnabled()) {
        		logger.debug("onOutboundClose for handler " + handler + " protocol " + protocol);
        	}

            handleChannelDisconnect(handler);
        }

        @Override
        public void onInboundClose(ChannelHandler handler, Protocol<CotEventContainer> protocol) {

        	if (logger.isDebugEnabled()) {
        		logger.debug("onInboundClose for handler " + handler + " protocol " + protocol);
        	}

            handleChannelDisconnect(handler);
        }

        @Override
        public String toString() {
            return "subscription_lifecycle_manager";
        }
    };

    public Subscription createSubscriptionFromConnection(ChannelHandler handler, Protocol<CotEventContainer> protocol,
														 UUID websocketApiNode) {

    	if (logger.isTraceEnabled()) {
    		logger.trace("createSubscriptionFromConnection " + handler + " " + protocol);
    	}
    	
		// Add to subscribers
        try {
			Configuration config = CoreConfigFacade.getInstance().getRemoteConfiguration();

            long uid = config.getCluster().isEnabled() ?
                        IgniteHolder.getInstance().getIgnite().atomicLong("streamingUidGen", 1, true).getAndIncrement() :
                        streamingUidGen.getAndIncrement();

            subMgr.addSubscription(handler.netProtocolName() + ":" + uid, /* uid for the subscription */
                    protocol,
                    handler,
                    null /* null xpath means receiver gets everything */,
                    null);

            if (handler instanceof TcpChannelHandler) {

                TcpChannelHandler tcpHandler = (TcpChannelHandler) handler;
                String connectionId = tcpHandler.getConnectionId();
                User user = (connectionId == null ? null : groupManager.getUserByConnectionId(connectionId));
                if (user != null) {
                	// make data feed users read only                	
                	if (tcpHandler.getInput() instanceof DataFeed) {
                		Set<Group> readOnlyGroups = groupManager
            					.getGroups(user)
            					.stream()
            					.filter(g->g.getDirection() == Direction.IN).collect(Collectors.toSet());
                		
                		groupManager.updateGroups(user, readOnlyGroups);
                	}
                	
                	Subscription subscription = subMgr.getSubscription(handler.netProtocolName() + ":" + uid);

                	if (websocketApiNode != null) {
						subscription.isWebsocket.set(true);
						subscription.websocketApiNode = websocketApiNode;
					}

                	if (logger.isDebugEnabled()) {
                		logger.debug("subscription in subscriptionLifecycleCallback " + subscription);
                	}
                    // set user on subscription, so that message brokering will be able to find the user
                    subMgr.setUserForSubscription(user, subMgr.getSubscription(handler.netProtocolName() + ":" + uid));

                    if (config.getBuffer().getLatestSA().isEnable()) {
                        try {
                            messagingUtil.sendLatestReachableSA(user);
                        } catch (Exception e) {
                            logger.error("sendLatestSA threw exception: " + e.getMessage(), e);
                        }
                    }

                    subMgr.startProtocolNegotiation(subscription);
                    
                    return subscription;
                }
            }

        } catch (Exception e) {
            logger.warn("Remote exception adding subscription: " + handler, e);
        }
        
        return null;
  	}

  	private String getGroupVectorFromHandler(ChannelHandler handler) {
        try {
            final Subscription sub = subscriptionStore.getByHandler(handler);
            User user = sub.getUser();
            NavigableSet<Group> groups = groupManager.getGroups(user);
            return RemoteUtil.getInstance().bitVectorToString(RemoteUtil.getInstance().getBitVectorForGroups(groups));
        } catch (Exception e) {
            logger.error("exception in getGroupVectorFromHandler!", e);
            return RemoteUtil.getInstance().getBitStringNoGroups();
        }
    }

    public void handleChannelDisconnect(ChannelHandler handler) {
		Configuration config = CoreConfigFacade.getInstance().getRemoteConfiguration();

		try {

			if (logger.isDebugEnabled()) {
				logger.debug("handleChannelDisconnect for handler " + handler);
			}

            Subscription subscription = subscriptionStore.getByHandler(handler);

            if (subscription == null) {
                logger.debug("null subscription in subscription lifecycle callback onClose");
                return;
            }

            // send CoT delete message to all reachable subscriptions
            if (!(Strings.isNullOrEmpty(subscription.callsign) || Strings.isNullOrEmpty(subscription.clientUid))) {

                if (enableLatestSa) {
                    // have nonempty uid and callsign for the given, closing handler
                    // get list of reachable subscriptions
                    CotEventContainer lastSA = subscription.getLatestSA();
                    messagingUtil.sendDisconnect(lastSA, subscription);
                }

                if (config.getRepository().isEnable()) {

                    String username = subscription.getUser() != null ? subscription.getUser().getName() : "";

                    //Audit disconnected event for callsign/uid pair
                    repositoryService.auditCallsignUIDEventAsync(subscription.callsign, subscription.clientUid, username, ConnectionEventTypeValue.DISCONNECTED,
                            getGroupVectorFromHandler(handler));
                }
            }
            if (config.getFederation() != null) {
                federationManager.removeLocalContact(subscription.clientUid);
            }

        } catch (Exception e) {
            logger.debug("exception sending delete message", e);
        }

        Resources.tcpCloseThreadPool.execute(() -> {
            try {
                subMgr.removeSubscription(handler);
            } catch (Exception e) {
                logger.warn("Remote exception removing subscription: " + handler, e);
            }
        });
	}

    public final ProtocolListenerInstantiator<CotEventContainer> staticSubscriptionRemovingCallback = (new AbstractAutoProtocolListener<CotEventContainer>() {
        private static final long serialVersionUID = -7142427436573649795L;

		@Override
        public void onOutboundClose(ChannelHandler handler, Protocol<CotEventContainer> protocol) {
            try {
                subMgr.removeSubscription(handler);
            } catch (Exception e) {
                logger.warn("Remote exception removing subscription: " + handler, e);
            }
        }

        @Override
        public String toString() {
            return "static_subscription_manager";
        }
    });

    public ProtocolListenerInstantiator<CotEventContainer> onDataReceivedCallback = new AbstractAutoProtocolListener<CotEventContainer>() {
        private static final long serialVersionUID = 8609977739208495040L;

		@Override
        public void onDataReceived(final CotEventContainer data, ChannelHandler handler, Protocol<CotEventContainer> protocol) {
			Configuration config = CoreConfigFacade.getInstance().getRemoteConfiguration();

        	if (config.getSubmission().isIgnoreStaleMessages()) {
                if (MessageConversionUtil.isStale(data)) {
                	if (logger.isDebugEnabled()) {
                		logger.debug("ignoring stale message: " + data);
                	}
                    return;
                }
            }

            // TODO: do this asynchronously in a metrics threadpool
            try {
            	if (logger.isDebugEnabled()) {
            		logger.debug("onDataReceived channel handler " + handler.getClass().getName() + " " + handler + " " + handler.hashCode());
            	}

                // update reads metric for this input
				Input input = ((AbstractBroadcastingChannelHandler) handler).getInput();
                InputMetric metric = util.getInputMetric(input);
                metric.getMessagesReceived().incrementAndGet();
                metric.getBytesRecieved().addAndGet(data.toString().length());

                if (input instanceof DataFeed) {
					DataFeed feed = (DataFeed) input;
					if (Strings.isNullOrEmpty(feed.getUuid())) {
						double lat = Double.valueOf(data.getLat()).doubleValue();
						double lon = Double.valueOf(data.getLon()).doubleValue();
						if (!DataFeedStatsHelper.getInstance().addStatsForDataFeedMessage(
								feed.getUuid(), data.getType(), lat,	lon,
								data.getCreationTime(), data.toString().length())) {
							logger.error("Unable to add new DataFeedStats for Message.");
						}
					}
				}

            } catch (Exception e) {
            	if (logger.isDebugEnabled()) {
            		logger.debug("exception writing metric", e);
            	}
            }

            // burn handler and protocol into message context map
            data.setContext(Constants.SOURCE_TRANSPORT_KEY, handler);
            data.setContext(Constants.SOURCE_HASH_KEY, handler.identityHash());
            data.setContext(Constants.SOURCE_PROTOCOL_KEY, protocol);

            if (handler instanceof AbstractBroadcastingChannelHandler && ((AbstractBroadcastingChannelHandler) handler).getConnectionInfo() != null && ((AbstractBroadcastingChannelHandler) handler).getConnectionInfo().getConnectionId() != null) {

            	String cid = ((AbstractBroadcastingChannelHandler) handler).getConnectionInfo().getConnectionId();

            	if (logger.isDebugEnabled()) {
            		logger.debug("connectionId: " + cid);
            	}

            	data.setContext(Constants.CONNECTION_ID_KEY, cid);
            }

            if (logger.isTraceEnabled()) {
            	 logger.trace(String.format(
                         "Submission service receiving message -- handler: %s protocol: %s message: %s",
                         handler,
                         protocol,
                         data.partial()
                 ));
            }

            final Subscription sub = subscriptionStore.getByHandler(handler);

            User user = null;

            if (sub != null) {
                //
                // if we're incognito and the current message isn't a control message and isn't
                // directly addressing another user, don't do any further processing
                //
                if (sub.incognito
                &&  !isControlMessage(data.getType())
                &&  data.getDocument().selectNodes("/event/detail/marti/dest[@callsign]").size() == 0) {
                    return;
                }

                user = sub.getUser();
                data.setContext(Constants.CLIENT_UID_KEY, sub.clientUid);
            }

            // no subscription found, or user in subscription was null
            if (user == null) {
                try {
                	if (logger.isDebugEnabled()) {
                		logger.debug("user not found in subscription. Trying to getting user based on connectionId.");
                	}

                    String connectionId = MessageConversionUtil.getConnectionId((AbstractBroadcastingChannelHandler) handler);

                    if (!Strings.isNullOrEmpty(connectionId)) {
                        try {
                            user = groupManager.getUserByConnectionId(connectionId);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        if (logger.isDebugEnabled()) {
                        	logger.debug("got user based on connectionId: " + user);
                        }
                    }
                } catch (Exception e) {
                	if (logger.isDebugEnabled()) {
                		logger.debug("exception getting user from handler", e);
                	}
                }
            } else {
            	if (logger.isDebugEnabled()) {
            		logger.debug("assigned user to message from subscription");
            	}
            }

			if (user != null) {
				data.setContext(Constants.USER_KEY, user);

				try {
					final Subscription subscriptionFromUser = subMgr.getSubscription(user);
					if (data.getContextValue(Constants.CLIENT_UID_KEY) == null && subscriptionFromUser != null) {
						data.setContext(Constants.CLIENT_UID_KEY, subscriptionFromUser.clientUid);
					}
				} catch (Exception e) {
					logger.debug("Could not find subscription for user");
				}
			}

            // process per-input filters
            Input input = ((AbstractBroadcastingChannelHandler) handler).getInput();
            Filter filter = input.getFilter();
            if (filter != null) {

                // check for invalid input level filters
                if (filter.getThumbnail() != null || filter.getUrladd() != null || filter.getFlowtag() != null ||
                    filter.getStreamingbroker() != null || filter.getDropfilter() != null ||
                    filter.getInjectionfilter() != null || filter.getScrubber() != null) {
                    logger.error("Invalid filter assigned for Input: " + input.getName());
                }

                if (filter.getGeospatialFilter() != null) {
                    GeospatialEventFilter geospatialEventFilter = new GeospatialEventFilter(filter.getGeospatialFilter());
                    if (geospatialEventFilter.filter(data) == null) {
                        return;
                    }
                }
            }

            if (scrubInvalidValues.filter(data) == null) {
                logger.warn("Dropping CoT with invalid values");
                return;
            }

            boolean isMpAck = data.getType() != null && data.getType().compareTo("b-f-t-a") == 0;

            if (logger.isDebugEnabled()) {
            	//Prevent NPE when sub is null
            	String clientId = (sub==null)? "null" : sub.clientUid;
            	logger.debug("isMpAck: " + isMpAck + " current sub: " + sub + " sub.clientUid: " + clientId + " message uid: " + data.getUid());
            }

            if (!isMpAck && sub != null) {

            	if (logger.isDebugEnabled()) {
            		logger.debug("tracking for latestSA and local contact - subscription type: " + Subscription.getClassName());
            	}
                try {
                	if (sub.clientUid.compareTo(data.getUid()) == 0) {
                		groupFederationUtil.trackLatestSA(sub, data, true);
                	}
                } catch (Exception e) {
                	if (logger.isDebugEnabled()) {
                		logger.debug("exception saving latest SA message " + " " + data.asXml() + " ", e);
                	}
                }

                if (Strings.isNullOrEmpty(sub.clientUid) || sub.clientUid.compareTo(data.getUid()) == 0) {
                	if (config.getFederation() != null) {
                		try {
                			federationManager.addLocalContact(data, handler);
                		} catch (Exception e) {
                			if (logger.isDebugEnabled()) {
                				logger.debug("exception adding federated local contact", e);
                			}
                		}
                	}
                }
            }

            final User uzer = user;
            Resources.groupProcessor.execute(new Runnable() {
                @Override
                public void run() {

                    // Attach group information to the message, but don't block the submission thread(s) while doing so.
                    try {

                    	GroupFederationUtil gfu = groupFederationUtil;

                        NavigableSet<Group> groups = groupManager.getGroups(uzer);

                        if (groups != null) {

                        	if (postMissionEventsAsPublic || alwaysArchiveMissionCot) {
								if (data.getDocument().selectNodes(
										"/event/detail/marti/dest[@mission]").size() > 0) {

									if (postMissionEventsAsPublic) {
										//make a copy of the group set so we don't modify the actual user
										groups = new ConcurrentSkipListSet<>(groups);
										groups.add(groupManager.getGroup("__ANON__", Direction.IN));
									}

									if (alwaysArchiveMissionCot) {
										data.setContext(Constants.ARCHIVE_EVENT_KEY, Boolean.TRUE);
									}
								}
							}

                            // Only put IN groups in the message - out groups do not matter here
                            data.setContext(Constants.GROUPS_KEY, gfu.filterGroupDirection(Direction.IN, groups));

                            if (logger.isTraceEnabled()) {
                                logger.trace("groups at send time for message " + data + ": " + groups);
                            }
                        }

                    } catch (Exception e) {
                    	if (logger.isDebugEnabled()) {
                    		logger.debug("exception getting group information " + e.getMessage(), e);
                    	}
                    }

                    // perform message injection if configured, and there is a match on this message. In any case, process the message.
                    injectionManager.process(sub, data);
                }
            });
        }

        @Override
        public String toString() {
            return "data_submittor";
        }
    };

    private void forwardMessage(String destUid, Iterator<CotElement> it, CotParser cotParser, long storeForwardSendBufferMs) {
        try {

            CotElement missed = it.next();

            // build up a CotEventContainer from the CotElement
            CotEventContainer next = new CotEventContainer(cotParser.parse(missed.toCotXml()));

            // strip off the flow tag filter so we can resend from this server again
            flowTagFilter.unfilter(next);

            // attach groups from the original missed message when resending
            NavigableSet<Group> groups = groupManager.groupVectorToGroupSet(missed.groupString);
            // Only put IN groups in the message - out groups do not matter here
            next.setContext(Constants.GROUPS_KEY, groupFederationUtil.filterGroupDirection(Direction.IN, groups));

            // turn off message archiving so we dont save the message again
            next.setContext(Constants.ARCHIVE_EVENT_KEY, Boolean.FALSE);

            // mark the message with the store/forward key so that we can guarantee ordered delivery
            next.setContext(Constants.STORE_FORWARD_KEY, Boolean.TRUE);

            // setup explicit UID based addressing here so that for messages send to the All Chat room, we only
            // send them to the individual client that's picking up messages here, vs broadcasting again
            Set<String> uids = new HashSet<>();
            uids.add(destUid);
            next.setContextValue(StreamingEndpointRewriteFilter.EXPLICIT_UID_KEY, new ArrayList<String>(uids));

            // send the message
            MessagingDependencyInjectionProxy.getInstance().cotMessenger().send(next);

            // schedule next call to forwardMessage if anything left to send
            if (it.hasNext()) {
                Resources.storeForwardChatSendExecutor.schedule(() -> {
                            forwardMessage(destUid, it, cotParser, storeForwardSendBufferMs);
                        },
                        storeForwardSendBufferMs, TimeUnit.MILLISECONDS);
            }

        } catch (Exception e) {
            logger.error("exception in forwardMessage!", e);
        }
    }

    private void forwardMessages(Subscription sub, String groupVector) {
        Resources.storeForwardChatDbExecutor.execute(() -> {
            try {

                if (repositoryService == null) {
                    logger.error("null repositoryService in forwardMessages");
                    return;
                }

                if (sub == null) {
                    logger.error("null Subscription in forwardMessages");
                    return;
                }

				Configuration config = CoreConfigFacade.getInstance().getRemoteConfiguration();

                long storeForwardQueryBufferMs = config.getBuffer().getQueue().getStoreForwardQueryBufferMs();
                long storeForwardSendBufferMs = config.getBuffer().getQueue().getStoreForwardSendBufferMs();

                // use repository service to get missed messages
                List<CotElement> missedMessages = repositoryService.getChatMessagesForUidSinceLastDisconnect(
                        sub.clientUid, storeForwardQueryBufferMs, groupVector);

                if (logger.isDebugEnabled()) {
                    logger.debug("forwardMessages found " + missedMessages.size() + " missed messages for " + sub.clientUid);
                }

                Iterator<CotElement> it = missedMessages.iterator();
                if (it.hasNext()) {
                    forwardMessage(sub.clientUid, it, new CotParser(false), storeForwardSendBufferMs);
                }

            } catch (Exception e) {
                logger.error("exception in forwardMessages!", e);
            }
        });
    }

    public AbstractAutoProtocolListener<CotEventContainer> callsignExtractorCallback = new AbstractAutoProtocolListener<CotEventContainer>() {

		private static final long serialVersionUID = 9987987987L;

		@Override
		public void onDataReceived(CotEventContainer data, ChannelHandler handler, Protocol<CotEventContainer> protocol) {
			try {
				Resources.callsignAssignmentExecutor.execute(() -> {
					CotEventContainer copy = data.copy();
					String endpoint = copy.getEndpoint();
					if (endpoint != null) {
						String callsign = copy.getCallsign();
						protocol.removeProtocolListener(this);
						if (logger.isDebugEnabled()) {
							logger.debug("Extracting callsign for message from handler " + handler);
						}
						// Set the callsign on the subscription to match the client's reported SA message

						if (logger.isDebugEnabled()) {
							logger.debug("callsignExtractor callback execution " + hashCode() + " setting client sub " + data.getUid() + " " + callsign + " handler: " + handler.hashCode());
						}

						subMgr.setClientForSubscription(data.getUid(), callsign, handler, true);

						Subscription sub = subscriptionStore.getByHandler(handler);
						String groupVector = getGroupVectorFromHandler(handler);

						if (enableLatestSa) {
							// also track this message as the latest SA message
							if (sub != null) {
								groupFederationUtil.trackLatestSA(sub, data, true);
							}
						}

						if (CoreConfigFacade.getInstance().getRemoteConfiguration().getBuffer().getQueue().isEnableStoreForwardChat()) {
							try {
								forwardMessages(sub, groupVector);
							} catch (Exception e) {
								if (logger.isDebugEnabled()) {
									logger.debug("error in forwardMessages", e);
								}
							}
						}

						try {
                            String username = sub.getUser() != null ? sub.getUser().getName() : "";
							repositoryService.auditCallsignUIDEventAsync(
                                callsign, data.getUid(), username, ConnectionEventTypeValue.CONNECTED, groupVector);
						} catch (Exception e) {
							if (logger.isDebugEnabled()) {
								logger.debug("error recording connection event", e);
							}
						}
					}
				});

			} catch (RejectedExecutionException ree) {
				// count how often full queue has blocked message send
				Metrics.counter(Constants.METRIC_MESSAGE_QUEUE_FULL_SKIP).increment();
			} catch (Exception e) {
				if (logger.isErrorEnabled()) {
					logger.error("exception in callsignAssignmentExecutor", e);
				}
			}
		}
    };

    // default to not clustering the input add and saving
    public void addInput(Input input) throws IOException {
        addInput(input, false, true);
    }

    @Override
    public void addInputAndSave(Input newInput, boolean cluster) throws IOException {
        addInput(newInput, cluster, true);
    }

    @Override
    public void addInputNoSave(Input newInput, boolean cluster) throws IOException {
        addInput(newInput, cluster, false);
    }

    // add input business logic, optional clustering of the input add
    public void addInput(Input input, boolean cluster, boolean saveChanges) throws IOException {
    	
    	logger.info("input class type: " + input.getClass().getSimpleName());
    	
        String name = input.getName();
        logger.info("Configuring " + (cluster ? "clustered " : "") + "network input " + input.getClass().getName() + " " + name + ": ");
        
        if (input instanceof DataFeed) {
			DataFeed feed = (DataFeed) input;

			if (Strings.isNullOrEmpty(feed.getUuid())) {
				feed.setUuid(UUID.randomUUID().toString());
			}
			if (feed.getType() == null) {
				feed.setType("Streaming");
			}
			if (feed.getProtocol() == null || feed.getType().equals("Plugin")) {
		        addMetric(input, new InputMetric(input));
                if (saveChanges) {
				CoreConfigFacade.getInstance().addInputAndSave(input);
                }
		        return;
			}
        }

        TransportCotEvent transport = TransportCotEvent.findByID(input.getProtocol());
        boolean isTls = (transport == TransportCotEvent.TLS || transport == TransportCotEvent.SSL
                || transport == TransportCotEvent.PROTOTLS || transport == TransportCotEvent.COTPROTOTLS);

        boolean isUdp =  transport == TransportCotEvent.MUDP || transport == TransportCotEvent.UDP || transport == TransportCotEvent.COTPROTOMUDP;

        if (input.getProtocol().equals("grpc")) {
        	nettyBuilder.buildGrpcServer(input);
        } else if (input.getProtocol().equals("quic")) {
        	input.setCoreVersion2TlsVersions("TLSv1.3");     	
        	nettyBuilder.buildQuicServer(input);
        } else if (!isUdp) {

        	if (isTls) {
        		nettyBuilder.buildTlsServer(input);
        	} else if (transport == TransportCotEvent.TCP){
        		nettyBuilder.buildTcpServer(input);
        	} else if (transport == TransportCotEvent.STCP) {
        		nettyBuilder.buildStcpServer(input);
        	}
        } else {

            if (CoreConfigFacade.getInstance().getRemoteConfiguration().getSecurity() == null) {
                throw new IllegalArgumentException("Security section of CoreConfig is required");
            }
            
			int localPort = input.getPort();

			// Multicast options
			InetAddress group = null;
			List<NetworkInterface> interfs = new LinkedList<NetworkInterface>();

			if (input.getGroup() != null) {
				group = InetAddress.getByName(input.getGroup());
				// only set the interface if we're using multicast
				if (input.getIface() != null) {
					interfs.add(NetworkInterface.getByName(input.getIface()));
				}
			}
            
			if (input.getCoreVersion() == 2) {
				if (transport == TransportCotEvent.UDP)
					nettyBuilder.buildUdpServer(input);
				if (transport == TransportCotEvent.MUDP || transport == TransportCotEvent.COTPROTOMUDP)
					nettyBuilder.buildMulticastServer(input, group, interfs);
			} else {

				// Get codec sources, such as SSL and LDAPAUTH
				List<CodecSource> codecSources = getCodecSources(input);
				LinkedBlockingQueue<ProtocolListenerInstantiator<CotEventContainer>> protocolListenerInstantiators = new LinkedBlockingQueue<ProtocolListenerInstantiator<CotEventContainer>>();
				addProtocolListeners(protocolListenerInstantiators, input);

				ServerBinder binder = transport.binder(localPort, protocolListenerInstantiators, codecSources, interfs,
						group);

				server.bind(binder, input);
			}

			if (!TransportCotEvent.isStreaming(input.getProtocol())) {
				groupFederationUtil.updateInputGroups(input);
			}

		}
        addMetric(input, new InputMetric(input));
        if (saveChanges) {
		CoreConfigFacade.getInstance().addInputAndSave(input);
        }
    }

    public void removeInput(String inputName) {
        // TODO: Remove Codec Sources?
        // TODO: Remove Protocol Listeners?
        server.unbind(inputName);
        inputListenersMap.remove(inputName);
    }

    private void addProtocolListeners(@NotNull LinkedBlockingQueue<ProtocolListenerInstantiator<CotEventContainer>> listeners, @NotNull Input input) {

        //
        // cache off the listeners for the input and call the update function to do first time setup
        //

        inputListenersMap.put(input.getName(), listeners);
        updateProtocolListeners(input);
    }

    private void updateProtocolListeners(@NotNull Input input) {

        LinkedBlockingQueue<ProtocolListenerInstantiator<CotEventContainer>> listeners = inputListenersMap.get(input.getName());
        if (listeners == null) {
            logger.error("updateProtocolListeners failed to find listeners for " + input.getName());
            return;
        }

        //
        // clear out the current listeners and add them all back to guarantee ordering is preserved
        //
        listeners.clear();

        // add markup listeners
        InputListenerAuxillaryRouter.addListenersForInput(listeners, input);

        // add data submitting listener
        listeners.add(onDataReceivedCallback);
    }

    @Override
    public String name() {
        return "Submission";
    }

    @Override
    public void startService() {
        super.startService();

        try {
            server.listen();
        } catch (Exception e) {
            logger.warn("exception starting server " + e.getMessage(), e);
        }

    }

    @Override
    public void stopService(boolean wait) {
        try {
            server.stop();
        } catch (Exception e) {
        	logger.error("Could not stop service", e);
        }
        super.stopService(wait);
    }

    @Override
    public boolean addToInputQueue(CotEventContainer c) {
        if (logger.isTraceEnabled()) {
            logger.trace("Adding CoT to input queue " + c);
        }
        
        if (c != null && (c.getSubmissionTime() == null || c.getSubmissionTime().getTime() <= 0)) {
        	c.setSubmissionTime(new Date());
        }

        if (dupeLogger.isTraceEnabled()) {
    		@SuppressWarnings("unchecked")
			NavigableSet<Group> groups = (NavigableSet<Group>) c.getContextValue(Constants.GROUPS_KEY);

    		dupeLogger.trace("CoT SubmissionService addToInputQueue " + c + " groups: " + groups + " message context map: " + c.getContext());
    	}

        try {

        	if (!c.hasContextKey(Constants.CLUSTER_MESSAGE_KEY)) {

        		messageReceivedLocal.incrementAndGet();

				Configuration config = CoreConfigFacade.getInstance().getRemoteConfiguration();

        		if (config.getCluster().isEnabled() && MessagingDependencyInjectionProxy.getInstance().clusterManager() != null) {
        			ClusterManager.countMessageReceived();
        		}

        		if (config.getCluster().isEnabled()
        				&& MessagingDependencyInjectionProxy.getInstance().clusterManager() != null
        				&& !isControlMessage(c.getType())
        				&& (c.getContextValue(Constants.PLUGIN_MESSAGE_KEY) == null
                    || ((Boolean) c.getContextValue(Constants.PLUGIN_MESSAGE_KEY)).booleanValue() != true)) {
        			Resources.clusterStateProcessor.execute(() -> {
        				MessagingDependencyInjectionProxy.getInstance().clusterManager().onDataMessage(c);
        			});
        		}
        	} else {
    			messageReceivedCluster.incrementAndGet();
        	}
        } catch (Exception e) {
        	logger.warn("exception clustering message", e);
        }

        return inputQueue.add(c);
    }

    @Override
    protected void processNextEvent() {

        CotEventContainer c = null;
		try {
            c = inputQueue.take();

            if (logger.isTraceEnabled()) {
            	logger.trace("SubmissionService processNextEvent " + c);
            }

            if (c == null) {
                return;
            }

			if (logger.isTraceEnabled()) {
				logger.trace("Found a CotEventContainer in the inputQueue");
			}

        } catch (InterruptedException e1) {
            logger.warn("Exception taking object from queue " + inputQueue, e1);
        }

        if (c.getLat() == null || c.getLat().length() == 0) {
            return;
        }

        if (geospatialEventFilter != null && geospatialEventFilter.filter(c) == null) {
            return;
        }

        if (logger.isTraceEnabled()) {
        	logger.trace("passed geospatial");
        }

        for (DropEventFilter f : dropFilters) {
            if (f.filter(c) == null) {
                return;
            }
        }

        if (logger.isTraceEnabled()) {
        	logger.trace("passed dropfilters");
        }

        if (isControlMessage(c.getType())) {
            try {
				if (logger.isTraceEnabled()) {
					logger.trace("Processing Control Message.");
				}
                processControlMessage(c);
            } catch (IOException e) {
                logger.debug("exception processing control message", e);
            }
            // consume control messages
            // (i.e., do NOT send them to subscribers)
            return;
        }

        if (logger.isTraceEnabled()) {
        	logger.trace("passed control message check");
        }

		Configuration config = CoreConfigFacade.getInstance().getRemoteConfiguration();

        if (config.getFilter().getFlowtag().isEnable() && c.matchXPath("/event/detail/_flow-tags_[@" + flowTagFilter.flowTag() + "]")) {
            //we've already processed this message, throw it away
        	logger.error("Duplicate message - already processed by this takserver");

            return;
        }

        // add a flow tag (to show that Marti has processed the message)
		if (logger.isTraceEnabled()) {
			logger.trace("Adding flow tag (to show that the message has been processed.");
		}
        flowTagFilter.filter(c);

		if (logger.isTraceEnabled()) {
			logger.trace("Processing a Contact Message.");
		}
        processContactMessage(c);

        if (logger.isTraceEnabled()) {
        	logger.trace("passed flowtag and contact");
        }

        c.setServerTime(DateUtil.toCotTime(new Date().getTime()));

        boolean allServicesHaveRoom = true;
        for (BaseService s : consumers) {
            if (!s.hasRoomInQueueFor(c)) {
                allServicesHaveRoom = false;
            }
        }

        if (allServicesHaveRoom ||
        		(config.getSubmission().isDropMesssagesIfAnyServiceIsFull() == false)) {

			if (logger.isTraceEnabled()) {
				logger.trace("Adding a copy of the CotEventContainer to the other services via the InputQueue.");
			}
        	for (BaseService s : consumers) {
        		s.addToInputQueue(c.copy());
        	}
        }
    }
    
    public boolean isControlMessage(String ct) {
    	
    	if (Strings.isNullOrEmpty(ct)) {
    		return false;
    	}
    	
    	String lct = ct.toLowerCase();
    	
        return controlMsgTypes.contains(lct);
    }

    public void processControlMessage(CotEventContainer c) throws UnknownHostException, IOException {
    	if (logger.isTraceEnabled()) {
    		logger.trace("processing control message for uid: " + c.getUid());
    	}

    	switch (c.getType()) {
            case "t-b":
                processSubscriptionMessage(c);
                break;
			case "t-x-c-f":
				processFilterMessage(c);
				break;
			case "t-b-q":
                logger.info("ignoring durable messaging (t-b-q) control message");
                break;
            case "t-x-c-t":
                sendPong(c);
                break;
            case "t-x-takp-q":
                // ignore protobuf negotiation messages
                break;
            case "t-x-c-m":
                processMetricsMessage(c);
                break;
            case "t-x-c-i-e":
                setIncognito(c, true);
                break;
            case "t-x-c-i-d":
                setIncognito(c, false);
                break;
            default:
                subMgr.deleteSubscription(c.getUid());
        }
    }

    private void setIncognito(CotEventContainer msg, boolean incognito) {
        try {
            ChannelHandler handler = msg.getContext(Constants.SOURCE_TRANSPORT_KEY, ChannelHandler.class);
            Subscription subscription = subscriptionStore.getByHandler(handler);
            subscription.incognito = incognito;
        } catch (Exception e) {
            logger.error("Exception in processIncognitoMessage!", e);
        }
    }
    
    private void processSubscriptionMessage(CotEventContainer msg) throws UnknownHostException, IOException {
        String xpath = msg.getDocument().valueOf("/event/detail/subscription/tests/@xpath");
        Node subNode = msg.getDocument().selectSingleNode("/event/detail/subscription");
        String connectStr = subNode.valueOf("@publish");
        String[] tokens = connectStr.split(":");

        if (tokens.length < 3) {
            logger.error("Malformed subscription endpoint: " + connectStr);
            return;
        }

        TransportCotEvent transportType = TransportCotEvent.findByID(tokens[0]);
        Tuple<ChannelHandler, Protocol<CotEventContainer>> handlerAndProtocol = null;
        Subscription subscription = null;

        switch (transportType) {
            case STCP:
                ChannelHandler handler = msg.getContext(Constants.SOURCE_TRANSPORT_KEY, ChannelHandler.class);
                @SuppressWarnings("unchecked")
                Protocol<CotEventContainer> protocol = msg.getContext(Constants.SOURCE_PROTOCOL_KEY, Protocol.class);
                if (handler != null && protocol != null) {
                    subscription = subscriptionStore.getByHandler(handler);
                    if (subscription != null) {
                    	if (logger.isDebugEnabled()) {
                    		logger.debug("updating subscription: " + subscription);
                    	}
                        subscription.xpath = xpath;
                    } else {
                        logger.warn("can't update a subscription that doesn't exist");
                    }

                    // live channel -- attach listener that removes its subscription on close
                    protocol.addProtocolListener(
                            staticSubscriptionRemovingCallback.newInstance(handler, protocol)
                    );

                    handlerAndProtocol = Tuple.create(handler, protocol);
                }

                break;
            default:
                InetAddress address = InetAddress.getByName(tokens[1]);
                int port = Integer.parseInt(tokens[2]);

                handlerAndProtocol = transportType.client(
                        address,
                        port,
                        server,
                        Lists.newArrayList(Codec.defaultCodecSource)
                );
        }

        // NOW, Add the subscription...
        // ...unless, of course, the protocol or transport weren't set above
        //    (e.g., onConnect subscription add)
        if (subscription == null && handlerAndProtocol != null) {
        	if (logger.isDebugEnabled()) {
        		logger.debug("adding subscription for uid: " + msg.getUid());
        	}
            User user = groupFederationUtil.getUser(msg.getUid(), true);

            groupManager.addUser(user);

            subMgr.addSubscription(
                    msg.getUid(),
                    handlerAndProtocol.right(),
                    handlerAndProtocol.left(),
                    xpath,
                    user);
        }
    }

	private void processFilterMessage(CotEventContainer msg) {
		try {
			GeospatialEventFilter filter = null;

			List<Node> bboxNodes = msg.getDocument().selectNodes(
					"/event/detail/subscription/geospatialFilter/boundingBox");
			if (bboxNodes != null && !bboxNodes.isEmpty()) {
				GeospatialFilter geospatialFilter = new GeospatialFilter();
				for (Node bboxNode : bboxNodes) {
					GeospatialFilter.BoundingBox boundingBox = new GeospatialFilter.BoundingBox();
					boundingBox.setMinLongitude(Double.valueOf(bboxNode.valueOf("@minLongitude")));
					boundingBox.setMinLatitude(Double.valueOf(bboxNode.valueOf("@minLatitude")));
					boundingBox.setMaxLongitude(Double.valueOf(bboxNode.valueOf("@maxLongitude")));
					boundingBox.setMaxLatitude(Double.valueOf(bboxNode.valueOf("@maxLatitude")));
					geospatialFilter.getBoundingBox().add(boundingBox);
				}
				filter = new GeospatialEventFilter(geospatialFilter);
			}

			ChannelHandler handler = msg.getContext(Constants.SOURCE_TRANSPORT_KEY, ChannelHandler.class);
			Subscription subscription = subscriptionStore.getByHandler(handler);
			subscription.geospatialEventFilter = filter;

		} catch (Exception e) {
			logger.error("Exception in processFilterMessage!", e);
		}
	}

    private void processMetricsMessage(CotEventContainer msg) {
        try {
            Subscription subscription = subscriptionStore.getByHandler((ChannelHandler) msg.getContextValue(Constants.SOURCE_TRANSPORT_KEY));
            if (subscription == null) {
                return;
            }

            RemoteSubscriptionMetrics subscriptionMetrics = new RemoteSubscriptionMetrics();

            Element stats = msg.getDocument().getRootElement().element("detail").element("stats");

            subscriptionMetrics.appFramerate = stats.attribute("app_framerate").getValue();
            subscriptionMetrics.battery = stats.attribute("battery").getValue();
            subscriptionMetrics.batteryStatus = stats.attribute("battery_status").getValue();
            subscriptionMetrics.batteryTemp = stats.attribute("battery_temp").getValue();
            subscriptionMetrics.deviceDataRx = stats.attribute("deviceDataRx").getValue();
            subscriptionMetrics.deviceDataTx = stats.attribute("deviceDataTx").getValue();
            subscriptionMetrics.heapCurrentSize = stats.attribute("heap_current_size").getValue();
            subscriptionMetrics.heapFreeSize = stats.attribute("heap_free_size").getValue();
            subscriptionMetrics.heapMaxSize = stats.attribute("heap_max_size").getValue();
            subscriptionMetrics.ipAddress = stats.attribute("ip_address").getValue();
            subscriptionMetrics.storageAvailable = stats.attribute("storage_available").getValue();
            subscriptionMetrics.storageTotal = stats.attribute("storage_total").getValue();

            subMgr.setSubscriptionsMetricsForClientUid(subscription.clientUid, subscriptionMetrics);

        } catch (Exception e) {
            logger.error("exception in processMetricsMessage! : " + e.getMessage());
        }
    }

    private void sendPong(final CotEventContainer msg) {
        try {
            Subscription dest = subscriptionStore.getByHandler((ChannelHandler) msg.getContextValue(Constants.SOURCE_TRANSPORT_KEY));
            if (dest == null) {
                return;
            }

			dest.lastPingTime = new AtomicLong(new Date().getTime());

            CotEventContainer pong = new CotEventContainer(DocumentHelper.parseText(
                    "<event version='2.0' uid='takPong' type='t-x-c-t-r' how='h-g-i-g-o' time='" +
                            DateUtil.toCotTime(System.currentTimeMillis()) + "' start='" +
                            DateUtil.toCotTime(System.currentTimeMillis()) + "' stale='" +
                            DateUtil.toCotTime(System.currentTimeMillis() + 20L * 1000L) + "'> " +
                            "<point ce='9999999' le='9999999' hae='0' lat='0' lon='0' /></event>"));

            if (dest.isWebsocket.get() && dest.getHandler() instanceof AbstractBroadcastingChannelHandler) {
				Set<String> websocketHits = new ConcurrentSkipListSet<>();
				websocketHits.add(((AbstractBroadcastingChannelHandler) dest.getHandler()).getConnectionId());
				WebsocketMessagingBroker.brokerTargetedWebSocketMessage(websocketHits, pong, dest.websocketApiNode);
			} else {
				dest.submit(pong);
			}
        } catch (Exception e) {
            logger.error("Attempting to send pong", e);
        }

    }

    private void processContactMessage(CotEventContainer c) {
        String callsign = c.getCallsign();
        String endpoint = c.getEndpoint();
        if (callsign != null && endpoint != null) {
            RemoteContact contact = new RemoteContact();
            contact.setContactName(callsign);
            contact.setEndpoint(endpoint);
            contact.setLastHeardFromMillis(System.currentTimeMillis());
            contact.setUid(c.getUid());
            contactManager.updateContact(contact);
        }
    }

    @Override
    public boolean hasRoomInQueueFor(CotEventContainer c) {
        return true; // Not used for this service.
    }

    /*
     * @param input A configuration object for an input
     *
     * @return An ordered list of CodecSources for the given input config object
     *
     */
    private List<CodecSource> getCodecSources(Input input) {
        List<CodecSource> codecSources = new ArrayList<>();

        codecSources.add(Codec.defaultCodecSource);

        boolean isTls = TransportCotEvent.isTls(input.getProtocol());

        if (isTls) {

            Tls tlsConfig = CoreConfigFacade.getInstance().getRemoteConfiguration().getSecurity().getTls();

            if (tlsConfig == null) {
                throw new IllegalArgumentException("tls config not available");
            }

            SSLConfig.getInstance(tlsConfig);

            codecSources.add(SslCodec.getSslCodecSource(tlsConfig)); // TLS encryption
        }

        if (input.getAuth() != null) {
        	if (logger.isDebugEnabled()) {
        		logger.debug(" auth value: " + input.getAuth());
        	}

            switch (input.getAuth()) {
                case LDAP:
                	if (logger.isDebugEnabled()) {
                		logger.debug("configuring ldap authentication");
                	}
                    codecSources.add(LdapAuthCodec.getCodecSource());
                    break;
                case FILE:
                	if (logger.isDebugEnabled()) {
                		logger.debug("configuring file-based authentication");
                	}
                    codecSources.add(FileAuthCodec.getCodecSource());
                    break;
                case ANONYMOUS:
                	if (logger.isDebugEnabled()) {
                		logger.debug("configuring anonymous authentication");
                	}

                    codecSources.add(AnonymousAuthCodec.getCodecSource(input));
                    break;
                case X_509:
                	if (logger.isDebugEnabled()) {
                		logger.debug("configuring X509 authentication");
                	}

                    if (!isTls) {
                        codecSources.add(AnonymousAuthCodec.getCodecSource(input));
                    } else {
                    	codecSources.add(X509AuthCodec.getCodecSource());
                    }
                    break;
                default:
                    throw new IllegalArgumentException("invalid auth configuration " + input.getAuth());
            }
        }

        if (logger.isTraceEnabled()) {
            logger.trace("codecSources for input " + input + ": " + codecSources);
        }

        return codecSources;
    }

    // keep track of metrics based on the Network.Input object
    private Map<String, InputMetric> inputMetrics = new ConcurrentHashMap<>();

    @Override
    public NetworkInputAddResult addInputAndSave(Input newInput) {
        return validateAndAddInput(newInput, true);
    }

    @Override
    public NetworkInputAddResult addInputNoSave(Input newInput) {
        return validateAndAddInput(newInput, false);
    }

    private NetworkInputAddResult validateAndAddInput(Input newInput, boolean saveChanges) {


    	if (logger.isDebugEnabled()) {
    		logger.debug("addInputAndSave " + newInput.getName() + " " + newInput.getPort() + " " + newInput.getAuth());
    	}

		Configuration config = CoreConfigFacade.getInstance().getRemoteConfiguration();

    	NetworkInputAddResult returnResult = NetworkInputAddResult.SUCCESS;

    	try {
    		synchronized (configLock) {
    			if (newInput.getAuth() == AuthType.LDAP && config.getAuth().getLdap() == null) {
    				returnResult = NetworkInputAddResult.FAIL_LDAP_AUTH_NOT_ENABLED;

    			} else if (newInput.getAuth() == AuthType.FILE && config.getAuth().getFile() == null) {
    				returnResult = NetworkInputAddResult.FAIL_FILE_AUTH_NOT_ENABLED;

    			} else if (newInput.getProtocol().equals(TransportCotEvent.MUDP.configID) && newInput.getGroup() == null) {
    				returnResult = NetworkInputAddResult.FAIL_MCAST_GROUP_UNSET;

    			} else {

    				List<Input> currentInputList = CoreConfigFacade.getInstance().getNetworkInputs();
					List<DataFeed> currentDataFeedList = CoreConfigFacade.getInstance().getNetworkDataFeeds();
    				String protocol = newInput.getProtocol();

    				boolean isUdp = protocol.equals(TransportCotEvent.UDP.configID);
    				boolean isMcast = protocol.equals(TransportCotEvent.MUDP.configID);

    				for (Input loopinput : currentInputList) {
    					if (newInput.getName().equals(loopinput.getName())) {
    						returnResult = NetworkInputAddResult.FAIL_INPUT_NAME_EXISTS;

    					} else if (newInput.getPort() == loopinput.getPort()) {
    						String loopProtocol = loopinput.getProtocol();
    						boolean loopIsUdp = loopProtocol.equals(TransportCotEvent.UDP.configID);
    						boolean loopIsMcast = loopProtocol.equals(TransportCotEvent.MUDP.configID);

    						if (loopIsUdp && isUdp) {
    							returnResult = NetworkInputAddResult.FAIL_UDP_PORT_ALREADY_IN_USE;
    						} else if (loopIsMcast && isMcast) {
    							returnResult = NetworkInputAddResult.FAIL_MCAST_PORT_ALREADY_IN_USE;
    						} else if (!loopIsUdp && !loopIsMcast && !isUdp && !isMcast) {
    							returnResult = NetworkInputAddResult.FAIL_TCP_PORT_ALREADY_IN_USE;
    						}
    					}

    					if (returnResult != NetworkInputAddResult.SUCCESS) {
    						break;
    					}
    				}

					for (DataFeed loopfeed : currentDataFeedList) {
						if (newInput.getName().equals(loopfeed.getName())) {
							returnResult = NetworkInputAddResult.FAIL_INPUT_NAME_EXISTS;
						} else {
							boolean isPlugin = false;
							if (newInput instanceof DataFeed) {
								DataFeed newDataFeed = (DataFeed) newInput;
								if (newDataFeed.getType().equals("Plugin")) {
									isPlugin = true;
								}
							}

							if (!isPlugin && newInput.getPort() == loopfeed.getPort()) {
								String loopProtocol = loopfeed.getProtocol();
								boolean loopIsUdp = loopProtocol.equals(TransportCotEvent.UDP.configID);
								boolean loopIsMcast = loopProtocol.equals(TransportCotEvent.MUDP.configID);
	
								if (loopIsUdp && isUdp) {
									returnResult = NetworkInputAddResult.FAIL_UDP_PORT_ALREADY_IN_USE;
								} else if (loopIsMcast && isMcast) {
									returnResult = NetworkInputAddResult.FAIL_MCAST_PORT_ALREADY_IN_USE;
								} else if (!loopIsUdp && !loopIsMcast && !isUdp && !isMcast) {
									returnResult = NetworkInputAddResult.FAIL_TCP_PORT_ALREADY_IN_USE;
								}
							}
						}

						if (returnResult != NetworkInputAddResult.SUCCESS) {
							break;
						}
					}
    			}

    			if (logger.isDebugEnabled()) {
    				logger.debug("add input result: " + returnResult);
    			}

    			if (returnResult == NetworkInputAddResult.SUCCESS) {
                    addInput(newInput, true, saveChanges);
    			}

    			if (logger.isDebugEnabled()) {
    				logger.debug("addInputAndSave('" + newInput.getName() + "') result: " + returnResult);
    			}
    		}
    	} catch (IOException e) {
    		logger.warn("error saving new input - result " + returnResult, e);
    	}

    	return returnResult;

    }

    @Override
    public void removeInputAndSave(String name) {
        removeInput(name, true);
    }

    @Override
    public void removeInputNoSave(String name) {
        removeInput(name, false);
    }

    public void removeInput(String name, boolean saveChanges) {

    	if (logger.isDebugEnabled()) {
    		logger.debug("Removing input '" + name + "' if it exists.");
    	}
        if (saveChanges) {
    	try {
			CoreConfigFacade.getInstance().removeInputAndSave(name);
    	} catch (RemoteException e) {
    		throw new TakException(e);
            }
    	}

        // untrack metrics
        InputMetric inputMetric = inputMetrics.get(name);

		if (inputMetric == null) {
			throw new IllegalStateException("input named " + name + " not found in input metric map for deletion");
		}

		Input input = inputMetric.getInput();

		if (input == null) {
			throw new IllegalStateException("input metric contained null input");
		}

        inputMetrics.remove(input.getName());
        logger.info("Stopping server for input: "  + input.getName());
        
		TransportCotEvent transport = TransportCotEvent.findByID(input.getProtocol());

		boolean isUdp = transport == TransportCotEvent.MUDP || transport == TransportCotEvent.UDP
				|| transport == TransportCotEvent.COTPROTOMUDP;

		try {
			if (input.getProtocol().equals("grpc") || !isUdp) {
				nettyBuilder.stopServer(input.getPort());
			} else {
				server.unbind(name);
			}
		} catch (Exception e) {
			logger.warn("exception unbinding server port for input " + name);
		}
    }

    @Override
    public Collection<InputMetric> getInputMetrics(boolean excludeDataFeeds) {

        if (logger.isDebugEnabled()) {
            logger.debug("getInputMetrics: " + inputMetrics.values());
        }

		if (excludeDataFeeds) {
			Collection<InputMetric> onlyInputsMetrics = inputMetrics.values().stream()
					.filter(input -> !(input.getInput() instanceof DataFeed))
					.collect(Collectors.toList());
			return onlyInputsMetrics;
		}

        return inputMetrics.values();
    }
    
    @Override
    public InputMetric getInputMetric(String name) {
        return inputMetrics.get(name);
    }

    @Override
    public ConnectionModifyResult modifyInputAndSave(String inputName, Input input) {
        return modifyInput(inputName, input, true);
    }

    @Override
    public ConnectionModifyResult modifyInputNoSave(String inputName, Input input) {
        return modifyInput(inputName, input, false);
    }

    public ConnectionModifyResult modifyInput(String inputName, Input modifiedInput, boolean saveChanges) {

    	if (logger.isDebugEnabled()) {
    		logger.debug("modifyInputAndSave " + inputName + " " + modifiedInput);
    	}

    	synchronized (configLock) {
    		try {
    			Input currentState = CoreConfigFacade.getInstance().getInputByName(inputName);

    			if (currentState == null) {
    				return ConnectionModifyResult.FAIL_NONEXISTENT;

    			} else if (!inputName.equals(modifiedInput.getName())) {
    				return ConnectionModifyResult.FAIL_NOMOD_NAME;

    			} else if (currentState.getAuth() != modifiedInput.getAuth()) {
    				return ConnectionModifyResult.FAIL_NOMOD_AUTH_TYPE;

    			} else if (currentState.getProtocol() != null && modifiedInput.getProtocol() != null &&
    					!currentState.getProtocol().toLowerCase(Locale.ENGLISH).equals(modifiedInput.getProtocol().toLowerCase(Locale.ENGLISH))) {
    				return ConnectionModifyResult.FAIL_NOMOD_PROTOCOL;

    			} else if (currentState.getPort() != modifiedInput.getPort()) {
    				return ConnectionModifyResult.FAIL_NOMOD_PORT;

    			} else if ((currentState.getGroup() == null && modifiedInput.getGroup() != null) ||
    					(currentState.getGroup() != null && modifiedInput.getGroup() == null) ||
    					(currentState.getGroup() != modifiedInput.getGroup() && !currentState.getGroup().equals(modifiedInput.getGroup()))) {
    				return ConnectionModifyResult.FAIL_NOMOD_GROUP;

    			} else if ((currentState.getIface() == null && modifiedInput.getIface() != null) ||
    					(currentState.getIface() != null && modifiedInput.getIface() == null) ||
    					(currentState.getIface() != modifiedInput.getIface() && !currentState.getIface().equals(modifiedInput.getIface()))) {
    				return ConnectionModifyResult.FAIL_NOMOD_IFACE;
    			}

    			// Modify the groups
    			String originalGroupList = Arrays.deepToString(currentState.getFiltergroup().toArray());

                ConnectionModifyResult result;

                if (saveChanges) {
                    result = CoreConfigFacade.getInstance().updateInputGroupsNoSave(inputName, modifiedInput.getFiltergroup().toArray(new String[modifiedInput.getFiltergroup().size()]));
    			if (result.getHttpStatusCode() != ConnectionModifyResult.SUCCESS.getHttpStatusCode()) {
                    return result;
                }
                } else {
                    if (CoreConfigFacade.getInstance().getInputByName(inputName) == null) {
                        return ConnectionModifyResult.FAIL_NONEXISTENT;
                    }
                }


    			groupFederationUtil.updateInputGroups(CoreConfigFacade.getInstance().getInputByName(inputName));
                if (saveChanges) {
				CoreConfigFacade.getInstance().saveChangesAndUpdateCache();
                }

    			if (logger.isDebugEnabled()) {
    				logger.debug("Input '" + inputName + "' has had its groups changed from " + originalGroupList + " to " + Arrays.deepToString(modifiedInput.getFiltergroup().toArray()));
    			}

    			// Modify the archive flags
    			if (currentState.isArchive() != modifiedInput.isArchive() || currentState.isArchiveOnly() != modifiedInput.isArchiveOnly() || currentState.isFederated() != modifiedInput.isFederated() || currentState.getSyncCacheRetentionSeconds() != modifiedInput.getSyncCacheRetentionSeconds()) {
    				// Modify the archive flag
			    logger.info("current arch, arch_only, federated: " + currentState.isArchive() + "," + currentState.isArchiveOnly() +
					"," + currentState.isFederated());
			    logger.info("modified arch, arch_only, federated: " + modifiedInput.isArchive() + "," + modifiedInput.isArchiveOnly() +
					"," + modifiedInput.isFederated());
    				if (currentState.isArchive() != modifiedInput.isArchive()) {
    					result = CoreConfigFacade.getInstance().setArchiveFlagNoSave(inputName, modifiedInput.isArchive());
    					if (result.getHttpStatusCode() != ConnectionModifyResult.SUCCESS.getHttpStatusCode()) {
                            return result;
                        }
    				}

    				// Modify the archive only flag
    				if (currentState.isArchiveOnly() != modifiedInput.isArchiveOnly()) {
    					result = CoreConfigFacade.getInstance().setArchiveOnlyFlagNoSave(inputName, modifiedInput.isArchiveOnly());
    					if (result.getHttpStatusCode() != ConnectionModifyResult.SUCCESS.getHttpStatusCode()) {
                            return result;
                        }
    				}
    				
    				// Modify federated flag
      				if (currentState.isFederated() != modifiedInput.isFederated()) {
      					result = CoreConfigFacade.getInstance().setFederatedFlagNoSave(inputName, modifiedInput.isFederated());
    					if (result.getHttpStatusCode() != ConnectionModifyResult.SUCCESS.getHttpStatusCode()) {
                            return result;
                        }
    				}

				// Modify syncCacheRetentionSeconds value

				if (currentState.getSyncCacheRetentionSeconds() !=
                        modifiedInput.getSyncCacheRetentionSeconds()) {
                        if (saveChanges) {
					result = CoreConfigFacade.getInstance().setSyncCacheRetentionSeconds(inputName, modifiedInput.getSyncCacheRetentionSeconds());
                            if (result.getHttpStatusCode() != ConnectionModifyResult.SUCCESS.getHttpStatusCode()) {
						return result;
                            }
                        } else {
                            if (CoreConfigFacade.getInstance().getInputByName(inputName) == null) {
                                return ConnectionModifyResult.FAIL_NONEXISTENT;
                            }
					    }
				    }
    				
					// Save the flag changes;
    				updateProtocolListeners(CoreConfigFacade.getInstance().getInputByName(inputName));
    				nettyBuilder.modifyServerInput(modifiedInput);
                    if (saveChanges) {
					CoreConfigFacade.getInstance().saveChangesAndUpdateCache();
    			}
                }
    			
    			// Modify data feed attributes
				if (modifiedInput instanceof DataFeed && currentState instanceof DataFeed) {
					DataFeed modifiedDataFeed = (DataFeed) modifiedInput;
					DataFeed currentDataFeed = (DataFeed) currentState;

					CoreConfigFacade.getInstance().updateTagsNoSave(inputName, modifiedDataFeed.getTag());
					
					if (modifiedDataFeed.isSync() != currentDataFeed.isSync()) {
						result = CoreConfigFacade.getInstance().setSyncFlagNoSave(inputName, modifiedDataFeed.isSync());
						if (result.getHttpStatusCode() != ConnectionModifyResult.SUCCESS.getHttpStatusCode()) {
							return result;
						}
					}
                    if (saveChanges) {
					CoreConfigFacade.getInstance().saveChangesAndUpdateCache();
				}
                }
				
				// Update input in inputMetric
				updateMetric(CoreConfigFacade.getInstance().getInputByName(inputName));
            } catch (RemoteException e) {
    			throw new TakException(e); // will not happen
    		}

    		return ConnectionModifyResult.SUCCESS;
        }
    	}

    @Override
    public void removeDataFeedAndSave(String name) {
        removeDataFeed(name, true);
    }

	@Override
    public void removeDataFeedNoSave(String name) {
        removeDataFeed(name, false);
    }

    public void removeDataFeed(String name, boolean saveChanges) {
		if (logger.isDebugEnabled()) {
			logger.debug("Removing datafeed '" + name + "' if it exists.");
		}

        if (saveChanges) {
		try {
			CoreConfigFacade.getInstance().removeDataFeedAndSave(name);
		} catch (RemoteException e) {
			throw new TakException(e);
		}
        }

        // untrack metrics
        InputMetric inputMetric = inputMetrics.get(name);

        if (inputMetric == null) {
            throw new IllegalStateException("input named " + name + " not found in input metric map for deletion");
        }

        Input input = inputMetric.getInput();
        
        if (input == null) {
            throw new IllegalStateException("input metric contained null input");
        }

        inputMetrics.remove(input.getName());
        logger.info("Stopping server for input: "  + input.getName());
        
        if (input.getProtocol() != null) {
			TransportCotEvent transport = TransportCotEvent.findByID(input.getProtocol());
	
			boolean isUdp = transport == TransportCotEvent.MUDP || transport == TransportCotEvent.UDP
					|| transport == TransportCotEvent.COTPROTOMUDP;
	
			try {
				if (input.getProtocol().equals("grpc") || !isUdp) {
					nettyBuilder.stopServer(input.getPort());
				} else {
					server.unbind(name);
				}
			} catch (Exception e) {
				logger.warn("exception unbinding server port for input " + name);
			}
        }
	}

    @Override
    public void addMetric(Input input, InputMetric metric) {
        if (input == null || metric == null) {
            throw new IllegalArgumentException("null input or metric");
        }

        inputMetrics.put(input.getName(), metric);
    }

	@Override
	public void updateMetric(Input input) {
		if (input == null) {
			throw new IllegalArgumentException("null input");
		}
		if (!inputMetrics.containsKey(input.getName())) {
			throw new IllegalArgumentException("input to update not found");
		}

		InputMetric inputMetric = inputMetrics.get(input.getName());
		inputMetric.setInput(input);
		inputMetrics.put(input.getName(), inputMetric);
	}

    @Override
    public InputMetric getMetric(Input input) {
        return inputMetrics.get(input.getName());
    }

	public InputMetric getMetricByPort(int port) {
		for (InputMetric inputMetric : inputMetrics.values()) {
			if (inputMetric.getInput().getPort() == port) {
				return inputMetric;
			}
		}
		return null;
	}

    @Override
    public MessagingConfigInfo getMessagingConfig() {
        Configuration conf = CoreConfigFacade.getInstance().getRemoteConfiguration();

        return new MessagingConfigInfo(
                conf.getBuffer().getLatestSA().isEnable(),
                conf.getRepository().getNumDbConnections(),
                conf.getRepository().isConnectionPoolAutoSize(),
                conf.getRepository().isArchive(),
                conf.getRepository().getConnection().getUsername(),
                MASK_WORD_FOR_DISPLAY,
                conf.getRepository().getConnection().getUrl(),
                conf.getRepository().getConnection().isSslEnabled(),
                conf.getRepository().getConnection().getSslMode(),
                conf.getRepository().getConnection().getSslCert(),
                conf.getRepository().getConnection().getSslKey(),
                conf.getRepository().getConnection().getSslRootCert()
                );
    }

    @Override
    public void modifyMessagingConfig(MessagingConfigInfo info) {
        CoreConfigFacade.getInstance().modifyMessagingConfig(info);
    }

    public QueueMetric getQueueMetrics() {
		return inputQueue.getQueueMetrics();
	}

	@Override
	public AuthenticationConfigInfo getAuthenticationConfig() {
		Ldap ldap = CoreConfigFacade.getInstance().getRemoteConfiguration().getAuth().getLdap();
		if (ldap == null) {
			/**
	        	return new AuthenticationConfigInfo(
	        			"",
	        			"",
	        			60,
	        			"",
	        			"",
	        			"",
	        			"");
			 **/
			return null;
		}
		return new AuthenticationConfigInfo(
				ldap.getUrl(),
				ldap.getUserstring(),
				ldap.getUpdateinterval(),
				ldap.getGroupprefix(),
				ldap.getServiceAccountDN(),
				ldap.getServiceAccountCredential(),
				ldap.getGroupBaseRDN()
				);
	}


	@Override
	public void modifyAuthenticationConfig(AuthenticationConfigInfo info) {
        CoreConfigFacade.getInstance().modifyAuthenticationConfig(info);
	}

	@Override
    public SecurityConfigInfo getSecurityConfig() {
	    Configuration conf = CoreConfigFacade.getInstance().getRemoteConfiguration();
        Tls tls = conf.getSecurity().getTls();

        boolean enableEnrollment = false;
        String caType = null;
        String signingKeystoreFile = null;
        String signingKeystorePass = null;
        int validityDays = 30;
        String mscaUserName = null;
        String mscaPassword = null;
        String mscaTruststore = null;
        String mscaTruststorePass = null;
        String mscaTemplateName = null;

        CertificateSigning certificateSigning = conf.getCertificateSigning();
        if (certificateSigning != null) {

            enableEnrollment = true;
            caType = certificateSigning.getCA().value();

            if (certificateSigning.getCA() == CAType.TAK_SERVER) {
                TAKServerCAConfig takServerCAConfig = certificateSigning.getTAKServerCAConfig();
                if (takServerCAConfig != null) {
                    signingKeystoreFile = takServerCAConfig.getKeystoreFile();
                    signingKeystorePass = takServerCAConfig.getKeystorePass();
                    validityDays = takServerCAConfig.getValidityDays();
                }

            } else if (certificateSigning.getCA() == CAType.MICROSOFT_CA) {
                MicrosoftCAConfig microsoftCAConfig = certificateSigning.getMicrosoftCAConfig();
                if (microsoftCAConfig != null) {
                    mscaUserName = microsoftCAConfig.getUsername();
                    mscaPassword = microsoftCAConfig.getPassword();
                    mscaTruststore = microsoftCAConfig.getTruststore();
                    mscaTruststorePass = microsoftCAConfig.getTruststorePass();
                    mscaTemplateName = microsoftCAConfig.getTemplateName();
                }
            }
        }

        return new SecurityConfigInfo(
                tls.getKeystoreFile(),
                tls.getTruststoreFile(),
                tls.getKeystorePass(),
                tls.getTruststorePass(),
                tls.getContext(),
                conf.getAuth().isX509Groups(),
                conf.getAuth().isX509AddAnonymous(),
                enableEnrollment,
                caType,
                signingKeystoreFile,
                signingKeystorePass,
                validityDays,
                mscaUserName,
                mscaPassword,
                mscaTruststore,
                mscaTruststorePass,
                mscaTemplateName
        );
    }

    @Override
    public void modifySecurityConfig(SecurityConfigInfo info) {
        CoreConfigFacade.getInstance().modifySecurityConfig(info);
    }

    @Override
    public Collection<Integer> getNonSecurePorts() {
		Configuration config = CoreConfigFacade.getInstance().getRemoteConfiguration();
        List<Input> inputs = config.getNetwork().getInput();
        List<Connector> connectors = config.getNetwork().getConnector();
        Set<Integer> unsecurePorts = new HashSet<Integer>();
        for (Input input : inputs) {
            if (!input.getProtocol().contains("tls") && !input.getProtocol().contains("quic")) {
                unsecurePorts.add(input.getPort());
            }
        }
        for (Connector connector : connectors) {
            if (!connector.isTls()) {
                unsecurePorts.add(connector.getPort());
            }
        }

        return unsecurePorts;
    }

    @Override
    public HashMap<String, Boolean> verifyConfiguration() {
		Configuration config = CoreConfigFacade.getInstance().getRemoteConfiguration();
        String keystoreFileName = config.getSecurity().getTls().getKeystoreFile();
        String truststoreFileName = config.getSecurity().getTls().getTruststoreFile();
        File keystoreFile = new File(keystoreFileName);
        File truststoreFile = new File(truststoreFileName);
        HashMap<String, Boolean> ret = new HashMap<String, Boolean>();
        ret.put("keystoreFile", keystoreFile.exists());
        ret.put("truststoreFile", truststoreFile.exists());
        return ret;
    }
	  
}
