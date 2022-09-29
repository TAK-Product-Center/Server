

package tak.server.federation;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.query.ContinuousQuery;
import org.apache.ignite.cluster.ClusterGroup;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.events.DiscoveryEvent;
import org.apache.ignite.events.EventType;
import org.apache.ignite.lang.IgnitePredicate;
import org.apache.ignite.services.Service;
import org.apache.ignite.services.ServiceContext;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import com.atakmap.Tak.BinaryBlob;
import com.atakmap.Tak.CRUD;
import com.atakmap.Tak.ContactListEntry;
import com.atakmap.Tak.FederatedEvent;
import com.atakmap.Tak.ROL;
import com.bbn.cluster.ClusterGroupDefinition;
import com.bbn.cot.filter.GeospatialEventFilter;
import com.bbn.marti.config.Federation;
import com.bbn.marti.config.Federation.Federate;
import com.bbn.marti.config.Federation.FederationOutgoing;
import com.bbn.marti.config.Federation.FederationServer.FederationPort;
import com.bbn.marti.config.Filter;
import com.bbn.marti.config.Tls;
import com.bbn.marti.groups.CommonGroupDirectedReachability;
import com.bbn.marti.groups.GroupFederationUtil;
import com.bbn.marti.groups.MessagingUtilImpl;
import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.nio.channel.connections.TcpChannelHandler;
import com.bbn.marti.nio.codec.Codec;
import com.bbn.marti.nio.codec.impls.SslCodec;
import com.bbn.marti.nio.listener.AbstractAutoProtocolListener;
import com.bbn.marti.nio.listener.ProtocolListenerInstantiator;
import com.bbn.marti.nio.netty.NioNettyBuilder;
import com.bbn.marti.nio.protocol.Protocol;
import com.bbn.marti.nio.util.CodecSource;
import com.bbn.marti.remote.ConnectionStatus;
import com.bbn.marti.remote.ConnectionStatusValue;
import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.remote.FederationManager;
import com.bbn.marti.remote.RemoteContact;
import com.bbn.marti.remote.exception.DuplicateFederateException;
import com.bbn.marti.remote.exception.TakException;
import com.bbn.marti.remote.groups.ConnectionInfo;
import com.bbn.marti.remote.groups.Direction;
import com.bbn.marti.remote.groups.FederateUser;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.remote.groups.User;
import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.marti.service.DistributedConfiguration;
import com.bbn.marti.service.DistributedSubscriptionManager;
import com.bbn.marti.service.FederatedSubscriptionManager;
import com.bbn.marti.service.Resources;
import com.bbn.marti.service.SSLConfig;
import com.bbn.marti.service.Subscription;
import com.bbn.marti.service.SubscriptionStore;
import com.bbn.marti.sync.EnterpriseSyncService;
import com.bbn.marti.util.CommonUtil;
import com.bbn.marti.util.MessageConversionUtil;
import com.bbn.marti.util.MessagingDependencyInjectionProxy;
import com.bbn.marti.util.spring.SpringContextBeanForApi;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Multimap;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import com.google.protobuf.ByteString;

import io.micrometer.core.instrument.Metrics;
import tak.server.Constants;
import tak.server.cot.CotEventContainer;
import tak.server.ignite.IgniteHolder;
import tak.server.messaging.Messenger;

/*
 *
 * Manage federated TAKServer connections.
 *
 */
public class DistributedFederationManager implements FederationManager, Service {

	private static final long serialVersionUID = -8858646520017672442L;
	private static final String OUTGOING_DELETED_FROM_UI = "Outgoing deleted from UI";

	public DistributedFederationManager(NioNettyBuilder nettyBuilder, Ignite ignite, CoreConfig coreConfig) {
		this.nettyBuilder = nettyBuilder;
		this.coreConfig = DistributedConfiguration.getInstance();

		if (logger.isDebugEnabled()) {
			logger.debug("DistributedFederationManager constructor. coreConfig: " + coreConfig);
		}
	}

	private NioNettyBuilder nettyBuilder;

	private CoreConfig coreConfig;

	private final AtomicReference<Messenger<CotEventContainer>> cotMessenger = new AtomicReference<>();

	private static AtomicBoolean outgoingsInitiated = new AtomicBoolean();
	
	@SuppressWarnings("unchecked")
	private Messenger<CotEventContainer> messenger() {
		if (cotMessenger.get() == null) {
			synchronized (this) {
				if (cotMessenger.get() == null) {
					cotMessenger.set((Messenger<CotEventContainer>) SpringContextBeanForApi.getSpringContext().getBean(Constants.DISTRIBUTED_COT_MESSENGER));
				}
			}
		}

		return cotMessenger.get();
	}

	private GroupManager groupManager() {
		return MessagingDependencyInjectionProxy.getInstance().groupManager();
	}

	private static DistributedFederationManager instance = null;

	private final AtomicInteger counter = new AtomicInteger();

	private static final String SSL_TRUSTSTORE_KEY = "fed-ssl-truststore";

    private ContinuousQuery<String, SSLConfig> continuousTrustStoreQuery = new ContinuousQuery<>();


	public static DistributedFederationManager getInstance() {
		if (instance == null) {
			synchronized (DistributedFederationManager.class) {
				if (instance == null) {
					instance = SpringContextBeanForApi.getSpringContext().getBean(DistributedFederationManager.class);
				}
			}
		}
		return instance;
	}

	@Override
	public void cancel(ServiceContext ctx) {
		if (logger.isDebugEnabled()) {
			logger.debug("DistributedFederationManager service cancelled");
		}
	}

	@Override
	public void init(ServiceContext ctx) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("init method DistributedFederationManager");
		}

		// This occurs in WebSocketTest
		if (coreConfig == null || coreConfig.getRemoteConfiguration() == null
				|| coreConfig.getRemoteConfiguration().getFederation() == null) {
			return;
		}

		getSSLCache().putIfAbsent(SSL_TRUSTSTORE_KEY,
				SSLConfig.getInstance(coreConfig.getRemoteConfiguration().getFederation().getFederationServer().getTls()));

		continuousTrustStoreQuery.setLocalListener((evts) -> {
   	    	saveTruststoreFile();
			// reload the trust store from disk, and
   	    	Tls tlsConfig = coreConfig.getRemoteConfiguration().getFederation().getFederationServer().getTls();
   	    	tak.server.federation.FederationServer.refreshServer();
			SSLConfig.getInstance(tlsConfig).refresh();
     	 });

		getSSLCache().query(continuousTrustStoreQuery);

		Federation fedConfig = coreConfig.getRemoteConfiguration().getFederation();

		if (!fedConfig.isEnableFederation()) {

			if (logger.isDebugEnabled()) {
				logger.debug("federation disabled in config. Not starting federation server or initiating outgoing connections.");
			}

			return;
		}

		if (logger.isDebugEnabled()) {
			logger.debug("federation enabled in config.");
		}

		try {
			// start federation server
			if (fedConfig.getFederationServer().isV1Enabled()) {

				if (logger.isDebugEnabled()) {
					logger.debug("starting V1 federation server.");
				}

				startListening();
			} else {
				logger.info("not starting disabled v1 federation server.");
			}
		} catch (Exception e) {
			logger.warn("FAIL" + e);
		}
	}

	@Override
	public void execute(ServiceContext ctx) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("execute method DistributedFederationManager");
		}
	}

	// number of milliseconds that, without any activity, qualify a federate
	// connection as a zombie that should be forcibly disregarded
	public static final long FED_ZOMBIE_TIMEOUT = 30000;

	@Override
	public int incrementAndGetCounter() {

		int result = counter.incrementAndGet();

		if (logger.isDebugEnabled()) {
			logger.debug("counter incremented: " + result);
		}

		return result;
	}

	private static final Logger logger = LoggerFactory.getLogger(DistributedFederationManager.class);

	// start or restart the federation server
	private void startListening() {
		Federation fedConfig = coreConfig.getRemoteConfiguration().getFederation();

		if (fedConfig.getFederationServer().getFederationPort().isEmpty()) { // this will update old CoreConfig to the new format
			if (logger.isDebugEnabled()) {
				logger.debug("updating Federation Config to new format.");
			}
			int port = fedConfig.getFederationServer().getPort();
			String tlsVersion = fedConfig.getFederationServer().getTls().getContext();
			FederationPort p = new FederationPort();
			p.setPort(port);
			p.setTlsVersion(tlsVersion);
			fedConfig.getFederationServer().getFederationPort().add(p);

			try {
				coreConfig.setAndSaveFederation(fedConfig);
			} catch (Exception e) {
				logger.warn("exception trying to update CoreConfig.xml to new format." + e.getMessage(), e);
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Configuring federation server");
		}
		
		nettyBuilder.buildFederationServer();
	}


	@EventListener({ ContextRefreshedEvent.class })
	private void onContextRefreshed() {
		// This occurs in WebSocketTest
		if (coreConfig == null || coreConfig.getRemoteConfiguration() == null
				|| coreConfig.getRemoteConfiguration().getFederation() == null) {
			return;
		}

		if (DistributedConfiguration.getInstance().getRemoteConfiguration().getCluster().isEnabled()) {
			setupIgniteListeners();
		}
		
		ClusterGroup cg = ClusterGroupDefinition.getMessagingClusterDeploymentGroup(IgniteHolder.getInstance().getIgnite());
		// if the node starting up is the oldest messaging node in the cluster, clear the federation caches
		if (IgniteHolder.getInstance().getIgnite().services(cg).clusterGroup().forOldest().node().id()
				.equals(IgniteHolder.getInstance().getIgnite().cluster().localNode().id())) {
			SubscriptionStore.getInstance().clearFederationCaches();
			getFederationListenerCache().clear();
		}
						
		// initiage outgoing connections iff is this the first time federation has been enabled during this execution session of messaging process
		initiateAllOutgoing();
	}

	private void initiateAllOutgoing() {
		if (outgoingsInitiated.compareAndSet(false, true)) {
			// temporary set to check outgoing name uniqueness
			Set<String> names = new HashSet<>();
			Set<String> hostports = new HashSet<>();

			for (FederationOutgoing outgoing : coreConfig.getRemoteConfiguration().getFederation().getFederationOutgoing()) {
				// validate configuration
				if (names.contains(outgoing.getDisplayName().toLowerCase())) {
					throw new DuplicateFederateException("invalid configuration: multiple outgoing federates named " + outgoing.getDisplayName());
				}

				if (hostports.contains(outgoing.getAddress().toLowerCase() + "_" + outgoing.getPort())) {
					throw new DuplicateFederateException("invalid configuration: multiple outgoing federates with host and port " + outgoing.getAddress()
									+ " " + outgoing.getPort());
				}

				names.add(outgoing.getDisplayName().toLowerCase());
				hostports.add(outgoing.getAddress().toLowerCase() + "_" + outgoing.getPort());

				ConnectionStatus status = new ConnectionStatus(ConnectionStatusValue.DISABLED);
				// will only update cache if the previous was null
				ConnectionStatus previousCachedConnectionStatus = SubscriptionStore.getInstanceFederatedSubscriptionManager()
						.getAndPutFederateOutgoingStatus(outgoing.getDisplayName(), status);
				// no previous status entry.. so lets start the outgoing here
				if (previousCachedConnectionStatus == null) {

					if (outgoing.isEnabled()) {
						initiateOutgoing(outgoing, status);
					}

					// track outgoing status in cache
					SubscriptionStore.getInstanceFederatedSubscriptionManager().updateFederateOutgoingStatusCache(outgoing.getDisplayName(), status);
				}

				// track outgoing retries locally
				SubscriptionStore.getInstanceFederatedSubscriptionManager().putOutgoingNumRetries(outgoing.getDisplayName());
				SubscriptionStore.getInstanceFederatedSubscriptionManager().putOutgoingRetryScheduled(outgoing.getDisplayName());

			}
		}
	}

	private IgniteCache<String, SSLConfig>  sslCache = null;

	private IgniteCache<String, SSLConfig> getSSLCache() {
		if (sslCache == null) {
			sslCache = IgniteHolder.getInstance().getIgnite().getOrCreateCache("sslCache");
		}

		return sslCache;
	}

	private IgniteCache<String, String>  federationListenerCache = null;

	private IgniteCache<String, String> getFederationListenerCache() {
		if (federationListenerCache == null) {
			CacheConfiguration<String, String> cacheConfiguration = new CacheConfiguration<>();
			cacheConfiguration.setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(new Duration(TimeUnit.MINUTES, 1)));
			federationListenerCache = IgniteHolder.getInstance().getIgnite().getOrCreateCache("federationListenerCache");
		}

		return federationListenerCache;
	}

	private void setupIgniteListeners() {
		IgnitePredicate<DiscoveryEvent> ignitePredicate = new IgnitePredicate<DiscoveryEvent>() {
			@Override
			public boolean apply(DiscoveryEvent event) {
				// only react to messaging nodes, this will only ever be called in the cluster
				if (Constants.MESSAGING_PROFILE_NAME.equals(event.eventNode().attribute(Constants.TAK_PROFILE_KEY))) {	
					Resources.fedReconnectThreadPool.schedule(() -> {
						String removedNode = event.eventNode().id().toString();
						String thisNodeId = IgniteHolder.getInstance().getIgniteStringId();
						String handlingNodeId = getFederationListenerCache()
								.getAndPutIfAbsent(removedNode.toString(), thisNodeId);
												
						SubscriptionStore.getInstance().getActiveConnectionInfo().forEach(cs -> {
							if (UUID.fromString(removedNode).equals(UUID.fromString(cs.getNodeId()))) {
								ConnectionStatus cachedStatus = SubscriptionStore.getInstanceFederatedSubscriptionManager().getCachedFederationConnectionStatus(cs.getFederateName());
								cachedStatus.setConnectionStatusValue(ConnectionStatusValue.DISABLED);
								SubscriptionStore.getInstanceFederatedSubscriptionManager().updateFederateOutgoingStatusCache(cachedStatus.getFederateName(), cachedStatus);
								// try to remove sub from the cache incase the node that went down didnt execute the removal 
								SubscriptionStore.getInstanceFederatedSubscriptionManager().removeFederateSubcription(cs.getConnection());
							}
						});

						Ignite ignite = IgniteHolder.getInstance().getIgnite();
						// if the orphaned node is null - no other node has handled this event - so lets do it here.
						if (handlingNodeId == null) {

							DistributedConfiguration.getInstance()
								.getRemoteConfiguration()
								.getFederation()
								.getFederationOutgoing()
								.stream()
								.filter(o -> o.isEnabled())
								// filter out outgoing connections that were not on the downed node
								.filter(o -> UUID.fromString(removedNode).equals(UUID.fromString(getOutgoingStatus(o.getDisplayName()).getNodeId())))
								// load balance the orphaned federation outgoing connection to a random node
								.forEach(o -> {
									ClusterNode randomNode = ignite.services(ClusterGroupDefinition.getMessagingClusterDeploymentGroup(ignite)).clusterGroup().forRandom().node();
									ignite.services(ignite.cluster().forClients().forNode(randomNode))
										.serviceProxy(Constants.DISTRIBUTED_FEDERATION_MANAGER, FederationManager.class, false)
										.enableOutgoing(o.getDisplayName());
								});
						}
					}, 1, TimeUnit.SECONDS);
				}
				return true;
			}
		};

		IgniteHolder.getInstance().getIgnite().events().localListen(ignitePredicate, EventType.EVT_NODE_LEFT, EventType.EVT_NODE_FAILED);
	}

	private synchronized boolean initiateOutgoing(@NotNull FederationOutgoing outgoing, @NotNull ConnectionStatus status) {
		// v2 federation outgoing Connection
		if (outgoing.getProtocolVersion() == Constants.FIG_FEDERATION) {
			status.setConnectionStatusValue(ConnectionStatusValue.CONNECTING);
			try {
				// Try to make a FIG connection to the specified outgoing federate. If the
				// connection is successful, the TakFigClient will match itself up with a
				// federate by cert, assign groups, and register its subscription and user.
				// TakFigClient beanscope is prototype, so a new instance (with injected
				// dependencies) will be created by this call.
				TakFigClient figClient = SpringContextBeanForApi.getSpringContext().getBean(TakFigClient.class);
				figClient.start(outgoing, status);
				return true;
			} catch (Exception e) {
				logger.debug("exception initiating outgoing FIG federate connection " + getInfo(outgoing) + " "
						+ e.getMessage(), e);
			}
		} else if (outgoing.getProtocolVersion() == Constants.STANDARD_FEDERATION) {
			status.setConnectionStatusValue(ConnectionStatusValue.CONNECTING);
			try {
				nettyBuilder.buildFederationClient(outgoing, status);
				return true;
			} catch (Exception e) {
				if (logger.isDebugEnabled()) {
					logger.debug("exception initiating outgoing v1 core2 federate connection " + getInfo(outgoing), e);
				}
			}
		}
		SubscriptionStore.getInstanceFederatedSubscriptionManager().updateFederateOutgoingStatusCache(outgoing.getDisplayName(), status);

		return false;
	}

	private void disableAllOutgoing() {
		for (FederationOutgoing outgoing : coreConfig.getRemoteConfiguration()
				.getFederation()
				.getFederationOutgoing()) {
			if (logger.isDebugEnabled()) {
				logger.debug("disabling outgoing connection: " + outgoing.getDisplayName());
			}
			disableOutgoing(outgoing);
		}
	}

	@Override
	public List<Federate> getAllFederates() {
		return coreConfig.getRemoteConfiguration().getFederation().getFederate();
	}

	/*
	 * Add a federate to the fed config
	 *
	 */
	public void addFederateToConfig(@NotNull Federate federate) {

		Federation fedConfig = coreConfig.getRemoteConfiguration().getFederation();

		fedConfig.getFederate().add(federate);

		try {
			coreConfig.setAndSaveFederation(fedConfig);
		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("Remote Exception saving federate config", e);
			}
		}
	}

	@Override
	public Federate getFederate(String federateUID) {
		List<Federate> federates = coreConfig.getRemoteConfiguration().getFederation().getFederate();
		for (Federate f : federates) {
			if (f != null && f.getId() != null && f.getId().equals(federateUID)) {
				return f;
			}
		}
		return null;
	}

	@Override
	public List<String> getGroupsInbound(String federateUID) {
		List<Federate> federates = coreConfig.getRemoteConfiguration().getFederation().getFederate();
		for (Federate f : federates) {
			if (f.getId().compareTo(federateUID) == 0) {
				return f.getInboundGroup();
			}
		}
		return new ArrayList<String>();
	}

	@Override
	public Multimap<String, String> getInboundGroupMap(@NotNull String federateUID) {
		List<Federate> federates = coreConfig.getRemoteConfiguration().getFederation().getFederate();
		SortedSetMultimap<String, String> inboundMap = TreeMultimap.create();
		for (Federate f : federates) {
			if (f.getId().compareTo(federateUID) == 0) {
				// This list is built from the XML schema, a single key-value pair using a colon
				// as the delimiter
				// e.g., "remotegroup:localgroup"
				List<String> inboundGroupList = f.getInboundGroupMapping();
				for (String entry : inboundGroupList) {
					List<String> row = Splitter.on(":").limit(3).omitEmptyStrings().trimResults().splitToList(entry);
					// there should be a minimum of two entries, if more we just ignore them
					if (row.size() > 1) {
						// string mapping pattern is key:value
						inboundMap.put(row.get(0), row.get(1));
					}

				}
				break;
			}
		}
		return inboundMap;
	}

	@Override
	public List<String> getGroupsOutbound(String federateUID) {
		List<Federate> federates = coreConfig.getRemoteConfiguration().getFederation().getFederate();
		for (Federate f : federates) {
			if (f.getId().compareTo(federateUID) == 0) {
				return f.getOutboundGroup();
			}
		}
		return new ArrayList<String>();
	}

	@Override
	public List<String> getFederateRemoteGroups(String federateUID) {
		Map<String, Set<String>> groupMap = SubscriptionStore.getInstanceFederatedSubscriptionManager().getFederateRemoteGroups();
		if (groupMap != null && !groupMap.isEmpty()) {
			Set<String> groupSet = groupMap.get(federateUID);
			if (groupSet != null) {
				List<String> groupNames = new ArrayList<String>(groupMap.get(federateUID));

				return groupNames;
			}
		}
		return new ArrayList<String>();
	}

	@Override
	public List<String> getCAGroupsInbound(String caID) {
		List<Federation.FederateCA> federateCAs = coreConfig.getRemoteConfiguration().getFederation().getFederateCA();
		for (Federation.FederateCA ca : federateCAs) {
			if (ca.getFingerprint().compareTo(caID) == 0) {
				return ca.getInboundGroup();
			}
		}
		return new ArrayList<String>();
	}

	@Override
	public List<String> getCAGroupsOutbound(@NotNull String caID) {
		List<Federation.FederateCA> federateCAs = coreConfig.getRemoteConfiguration().getFederation().getFederateCA();
		for (Federation.FederateCA ca : federateCAs) {
			if (ca.getFingerprint().compareTo(caID) == 0) {
				return ca.getOutboundGroup();
			}
		}
		return new ArrayList<String>();
	}

	@Override
	public void addFederateToGroupsInbound(String federateUID, Set<String> localGroupNames) {

		Federation fedConfig = coreConfig.getRemoteConfiguration().getFederation();

		List<Federate> federates = fedConfig.getFederate();
		
		Federate federateConfig = null;
		
		for (Federate f : federates) {
			if (f.getId().compareTo(federateUID) == 0) {
				for (String gname : localGroupNames) {
					f.getInboundGroup().add(gname);
				}
				
				federateConfig = f;
				
				break;
			}
		}
		
		try {
			User fedUser =  MessagingDependencyInjectionProxy.getInstance().groupManager().getUserByConnectionId(federateUID);

			if (fedUser != null) {
				((FederateUser) fedUser).setFederateConfig(federateConfig);
			}
		} catch (Exception e) {
			logger.warn("error updating federation user group config", e);
		}
		
		coreConfig.setAndSaveFederation(fedConfig);

		clusterAddFederateUsersToGroups(federateUID, localGroupNames, Direction.IN, true);
	}

	@Override
	public void addFederateToGroupsOutbound(String federateUID, Set<String> localGroupNames) {

		Federation fedConfig = coreConfig.getRemoteConfiguration().getFederation();

		List<Federate> federates = fedConfig.getFederate();
		
		Federate federateConfig = null;
		
		for (Federate f : federates) {
			if (f.getId().compareTo(federateUID) == 0) {
				for (String gname : localGroupNames) {
					f.getOutboundGroup().add(gname);
				}
				
				federateConfig = f;
				
				break;
			}
		}
		
		try {
			User fedUser =  MessagingDependencyInjectionProxy.getInstance().groupManager().getUserByConnectionId(federateUID);

			if (fedUser != null) {
				((FederateUser) fedUser).setFederateConfig(federateConfig);
			}
		} catch (Exception e) {
			logger.warn("error updating federation user group config", e);
		}

		coreConfig.setAndSaveFederation(fedConfig);

		clusterAddFederateUsersToGroups(federateUID, localGroupNames, Direction.OUT, true);
	}

	@Override
	public void clusterAddFederateUsersToGroups(String federateUID, Set<String> localGroupNames, Direction direction, boolean cluster) {
		if (cluster) {
			// lets find any active federation connections for this federate and call into its node to dynamically add the groups.
			// if the connection isnt active we dont have to add them since they will be re(added) each time a connection is established.
			SubscriptionStore.getInstanceFederatedSubscriptionManager()
				.getActiveConnectionInfo()
				.stream()
				.filter(cs -> cs.getFederate().getId().compareTo(federateUID) == 0)
				.map(cs -> IgniteHolder.getInstance()
						.getIgnite()
						.cluster()
						.forNodeId(UUID.fromString(cs.getConnection().getNodeId())))
				.forEach(cg -> IgniteHolder.getInstance()
						.getIgnite()
						.services(cg)
						.serviceProxy(Constants.DISTRIBUTED_FEDERATION_MANAGER, FederationManager.class, false)
						.clusterAddFederateUsersToGroups(federateUID, localGroupNames, direction, false));
		} else {
			addFederateUsersToGroups(federateUID, localGroupNames, direction);
		}
	}

	@Override
	public void addFederateGroupsInboundMap(@NotNull String federateUID, @NotNull String remoteGroup,
			@NotNull String localGroup) {
		Federation fedConfig = coreConfig.getRemoteConfiguration().getFederation();
		List<Federate> federates = fedConfig.getFederate();

		for (Federate f : federates) {
			if (f.getId().compareTo(federateUID) == 0) {
				// we need to reconstruct the xml mapping pattern "remotegroup:localgroup"
				f.getInboundGroupMapping().add(remoteGroup.trim() + ":" + localGroup.trim());
				getInboundGroupMap(federateUID).put(remoteGroup.trim(), localGroup.trim());
				break;
			}
		}
		coreConfig.setAndSaveFederation(fedConfig);
	}

	@Override
	public synchronized void addInboundGroupToCA(@NotNull String caID, @NotNull Set<String> localGroupNames) {

		Federation fedConfig = coreConfig.getRemoteConfiguration().getFederation();

		List<Federation.FederateCA> federateCAs = fedConfig.getFederateCA();
		boolean foundCA = false;
		for (Federation.FederateCA ca : federateCAs) {
			if (ca.getFingerprint().compareTo(caID) == 0) {
				foundCA = true;
				for (String gname : localGroupNames) {
					ca.getInboundGroup().add(gname);
				}
				break;
			}
		}
		// If the CA doesn't exist yet in the CoreConfig.xml, see if it exists in
		// overall CA store
		if (!foundCA) {
			RemoteUtil remoteUtil = RemoteUtil.getInstance();
			for (X509Certificate cert : getCAList()) {
				if (caID.compareTo(remoteUtil.getCertSHA256Fingerprint(cert)) == 0) {
					// Add CA to the CoreConfig
					Federation.FederateCA federateCA = new Federation.FederateCA();
					federateCA.setFingerprint(caID);
					for (String gname : localGroupNames) {
						federateCA.getInboundGroup().add(gname);
					}
					fedConfig.getFederateCA().add(federateCA);
					break;
				}
			}
		}

		coreConfig.setAndSaveFederation(fedConfig);
	}

	@Override
	public synchronized void addOutboundGroupToCA(@NotNull String caID, @NotNull Set<String> localGroupNames) {

		Federation fedConfig = coreConfig.getRemoteConfiguration().getFederation();

		List<Federation.FederateCA> federateCAs = coreConfig.getRemoteConfiguration().getFederation().getFederateCA();
		boolean foundCA = false;
		for (Federation.FederateCA ca : federateCAs) {
			if (ca.getFingerprint().compareTo(caID) == 0) {
				foundCA = true;
				for (String gname : localGroupNames) {
					ca.getOutboundGroup().add(gname);
					coreConfig.setAndSaveFederation(fedConfig);
				}
				break;
			}
		}

		if (!foundCA) {
			RemoteUtil remoteUtil = RemoteUtil.getInstance();
			for (X509Certificate cert : getCAList()) {
				if (caID.compareTo(remoteUtil.getCertSHA256Fingerprint(cert)) == 0) {
					Federation.FederateCA federateCA = new Federation.FederateCA();
					federateCA.setFingerprint(caID);
					for (String gname : localGroupNames) {
						federateCA.getOutboundGroup().add(gname);
					}
					fedConfig.getFederateCA().add(federateCA);
					break;
				}
			}
		}

		coreConfig.setAndSaveFederation(fedConfig);
	}

	@Override
	public synchronized void removeFederateFromGroupsInbound(String federateUID, Set<String> localGroupNames) {

		Federation fedConfig = coreConfig.getRemoteConfiguration().getFederation();

		List<Federate> federates = fedConfig.getFederate();
		for (Federate f : federates) {
			if (f.getId().compareTo(federateUID) == 0) {
				for (String gname : localGroupNames) {
					f.getInboundGroup().remove(gname);
				}
				break;
			}
		}

		coreConfig.setAndSaveFederation(fedConfig);

		clusterRemoveFederateUsersToGroups(federateUID, localGroupNames, Direction.IN, true);
	}

	@Override
	public void removeFederateFromGroupsOutbound(String federateUID, Set<String> localGroupNames) {

		Federation fedConfig = coreConfig.getRemoteConfiguration().getFederation();

		List<Federate> federates = fedConfig.getFederate();
		for (Federate f : federates) {
			if (f.getId().compareTo(federateUID) == 0) {
				for (String gname : localGroupNames) {
					f.getOutboundGroup().remove(gname);
				}
				break;
			}
		}

		coreConfig.setAndSaveFederation(fedConfig);

		clusterRemoveFederateUsersToGroups(federateUID, localGroupNames, Direction.OUT, true);
	}

	@Override
	public void clusterRemoveFederateUsersToGroups(String federateUID, Set<String> localGroupNames, Direction direction, boolean cluster) {
		if (cluster) {
			// lets find any active federation connections for this federate and call into its node to dynamically remove the groups
			SubscriptionStore.getInstanceFederatedSubscriptionManager()
				.getActiveConnectionInfo()
				.parallelStream()
				.filter(cs -> cs.getFederate().getId().compareTo(federateUID) == 0)
				.map(cs -> IgniteHolder.getInstance()
						.getIgnite()
						.cluster()
						.forNodeId(UUID.fromString(cs.getConnection().getNodeId())))
				.forEach(cg -> IgniteHolder.getInstance()
						.getIgnite()
						.services(cg)
						.serviceProxy(Constants.DISTRIBUTED_FEDERATION_MANAGER, FederationManager.class, false)
						.clusterRemoveFederateUsersToGroups(federateUID, localGroupNames, direction, false));
		} else {
			removeFederateUsersFromGroups(federateUID, localGroupNames, direction);
		}
	}

	@Override
	public void removeFederateInboundGroupsMap(@NotNull String federateUID, @NotNull String remoteGroup,
			@NotNull String localGroup) {
		Federation fedConfig = coreConfig.getRemoteConfiguration().getFederation();
		List<Federate> federates = fedConfig.getFederate();

		Set localGroupSet = new HashSet<String>(1);
		for (Federate f : federates) {
			if (f.getId().compareTo(federateUID) == 0) {
				// we need to reconstruct the xml mapping pattern "remotegroup:localgroup"
				f.getInboundGroupMapping().remove(remoteGroup.trim() + ":" + localGroup.trim());
				localGroupSet.add(localGroup.trim());
				break;
			}
		}
		coreConfig.setAndSaveFederation(fedConfig);
	}

	@Override
	public synchronized void removeInboundGroupFromCA(@NotNull String caID, @NotNull Set<String> localGroupNames) {

		Federation fedConfig = coreConfig.getRemoteConfiguration().getFederation();

		List<Federation.FederateCA> federateCAs = fedConfig.getFederateCA();
		for (Federation.FederateCA ca : federateCAs) {
			if (ca.getFingerprint().compareTo(caID) == 0) {
				for (String gname : localGroupNames) {
					ca.getInboundGroup().remove(gname);
				}
				break;
			}
		}

		coreConfig.setAndSaveFederation(fedConfig);
	}

	@Override
	public synchronized void removeOutboundGroupFromCA(@NotNull String caID, @NotNull Set<String> localGroupNames) {

		Federation fedConfig = coreConfig.getRemoteConfiguration().getFederation();

		List<Federation.FederateCA> federateCAs = fedConfig.getFederateCA();
		for (Federation.FederateCA ca : federateCAs) {
			if (ca.getFingerprint().compareTo(caID) == 0) {
				for (String gname : localGroupNames) {
					ca.getOutboundGroup().remove(gname);
				}
				break;
			}
		}

		coreConfig.setAndSaveFederation(fedConfig);
	}

	@Override
	public List<Federation.FederationOutgoing> getOutgoingConnections() {

		List<FederationOutgoing> result = coreConfig.getRemoteConfiguration().getFederation().getFederationOutgoing();

		if (logger.isTraceEnabled()) {
			logger.trace("outgoing connections: " + result);
		}

		return result;
	}

	@Override
	public List<ConnectionStatus> getActiveConnectionInfo() {
		return SubscriptionStore.getInstanceFederatedSubscriptionManager().getActiveConnectionInfo();
	}

	@Override
	public Collection<RemoteContact> getContactsForFederate(String federateUID, String groupVector) {
		List<RemoteContact> rval = new LinkedList<>();

		checkNotNull(federateUID, "federate uid");

		for (FederateSubscription s : SubscriptionStore.getInstanceFederatedSubscriptionManager()
				.getFederateSubscriptions()) {

			checkNotNull(s, "federate subscription");

			if (logger.isDebugEnabled()) {
				logger.debug("subscription type: " + s.getClass().getName());
			}

			if (checkNotNull(
					checkNotNull(checkNotNull(((FederateUser) s.getUser()), "FederateUser").getFederateConfig(),
							"federate user FederateConfig").getId(),
					"federate user FederateConfig id").compareTo(federateUID) == 0) {

				ChannelHandler handler = checkNotNull(s.getHandler(), "fed subscription handler");

				ConcurrentHashMap<String, RemoteContactWithSA> contactMap =
						SubscriptionStore.getInstanceFederatedSubscriptionManager().getRemoteContactsMapByChannelHandler(handler);

				if (contactMap == null) {
					continue;
				}

				Collection<RemoteContactWithSA> remconcol = contactMap.values();

				for (RemoteContactWithSA rc : remconcol) {

					if (groupVector != null) {
						CotEventContainer event = rc.getLastSA();
						if (event != null) {
							NavigableSet<Group> saGroups = new ConcurrentSkipListSet<>((NavigableSet<Group>)
								event.getContext(Constants.GROUPS_KEY));
							NavigableSet<Group> destUserGroups = groupManager().groupVectorToGroupSet(groupVector);
							saGroups.retainAll(destUserGroups);
							if (saGroups.size() == 0) {
								continue;
							}
						}
					}

					if (rc != null) {
						rval.add(new RemoteContact(rc));
					}
				}
				return rval;
			}
		}
		return new HashSet<>();
	}

	@Override
	public List<FederationOutgoing> getOutgoingConnections(String address, int port) {

		List<FederationOutgoing> result = new ArrayList<>();

		for (FederationOutgoing outgoing : coreConfig.getRemoteConfiguration()
				.getFederation()
				.getFederationOutgoing()) {
			if (outgoing.getAddress().equalsIgnoreCase(address) && outgoing.getPort().equals(port)) {
				result.add(outgoing);
			}
		}

		return result;
	}

	public FederationOutgoing getOutgoingConnection(String name) {

		for (FederationOutgoing outgoing : coreConfig.getRemoteConfiguration()
				.getFederation()
				.getFederationOutgoing()) {
			if (outgoing.getDisplayName().equalsIgnoreCase(name)) {
				return outgoing;
			}
		}

		return null;
	}

	@Override
	public ConnectionStatus getOutgoingStatus(String name) {

		ConnectionStatus status = SubscriptionStore.getInstanceFederatedSubscriptionManager().getCachedFederationConnectionStatus(name);

		if (logger.isTraceEnabled()) {
			logger.trace("connection status for name " + name + ": " + status);
		}

		return status;
	}

	@Override
	public void disableOutgoingForNode(String name) {
		String federationConnectionNodeId = SubscriptionStore.getInstanceFederatedSubscriptionManager()
			.getCachedFederationConnectionStatus(name).getNodeId();

		ClusterGroup activeFederationNode = IgniteHolder.getInstance()
				.getIgnite()
				.cluster()
				.forNodeId(UUID.fromString(federationConnectionNodeId));

		// if we can find an active node with this outgoing - lets disable it there.
		// otherwise we can do it here
		if (activeFederationNode != null && activeFederationNode.node() != null) {
			IgniteHolder.getInstance()
				.getIgnite()
				.services(activeFederationNode)
				.serviceProxy(Constants.DISTRIBUTED_FEDERATION_MANAGER, FederationManager.class, false)
				.disableOutgoing(name);
		} else {
			disableOutgoing(name);
		}

	}

	@Override
	public void disableOutgoing(String name) {
		FederationOutgoing outgoing = getOutgoingConnection(name);

		if (outgoing == null) {
			throw new TakException("outgoing with name " + name + " not found");
		}

		disableOutgoing(outgoing);
	}

	@Override
	public synchronized void disableOutgoing(@NotNull FederationOutgoing outgoing) {
		if (outgoing == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("outgoing is null");
			}
			return;
		}

		FederatedSubscriptionManager fedSubManager = SubscriptionStore.getInstanceFederatedSubscriptionManager();
		if (fedSubManager == null) {
			String errorMessage = "FedSubManager is null";

			if (logger.isDebugEnabled()) {
				logger.debug(errorMessage);
			}
			throw new IllegalStateException(errorMessage);
		}

		ConnectionStatus status = fedSubManager.getFederationConnectionStatus(outgoing.getDisplayName());

		fedSubManager.getOutgoingNumRetries(outgoing.getDisplayName()).set(0);

		if (status == null) {
			String message = "ConnectionStatus not found for outgoing " + outgoing.getDisplayName();
			if (logger.isDebugEnabled()) {
				logger.debug(message);
			}
			throw new IllegalStateException(message);
		}

		synchronized (status) {
			outgoing.setEnabled(false);
			ConnectionStatusValue currentStatus = status.getConnectionStatusValue();
			status.setConnectionStatusValue(ConnectionStatusValue.DISABLED);

			// only try to disconnect if actually connected
			if (currentStatus.equals(ConnectionStatusValue.CONNECTED)
					|| currentStatus.equals(ConnectionStatusValue.CONNECTING)) {
				// FIG Outgoing Connection
				if (outgoing.getProtocolVersion() == Constants.FIG_FEDERATION) {
					logger.info("disabling FIG outgoing " + outgoing.getDisplayName());

					try {

						if (status.getConnection() != null && status.getConnection().getHandler() != null
								&& status.getConnection().getHandler() instanceof TakFigClient) {

							((TakFigClient) status.getConnection().getHandler())
									.processDisconnect(new TakException(OUTGOING_DELETED_FROM_UI));

						} else {
							logger.warn("null or invalid class of fig client in federate connection info");
						}
					} catch (Exception e) {
						if (logger.isDebugEnabled()) {
							logger.debug("exception disabling outgoing FIG federate connection " + getInfo(outgoing)
									+ " " + e.getMessage(), e);
						}
					}
					outgoing.setEnabled(false);
					fedSubManager.updateFederateOutgoingStatusCache(outgoing.getDisplayName(), status);
					saveOutgoing(outgoing, false);

					return;
				}

				// TAK Server V1 Outgoing connection
				ConnectionInfo connection = status.getConnection();

				if (connection == null) {
					if (logger.isDebugEnabled()) {
						logger.debug("null connectionInfo in status " + status);
					}
					return;
				}

				Object handler = connection.getHandler();

				if (handler == null) {
					throw new IllegalStateException("null or invalid object type channel handler for " + connection);
				}

				// close the connection
				((TcpChannelHandler) handler).forceClose();

				if (logger.isTraceEnabled()) {
					logger.trace("triggering close for " + handler);
				}
				outgoing.setEnabled(false);
				saveOutgoing(outgoing, false);
			} else {
				saveOutgoing(outgoing, false);
			}
			fedSubManager.updateFederateOutgoingStatusCache(outgoing.getDisplayName(), status);
		}
	}

	private void saveOutgoing(FederationOutgoing outgoing, boolean enabled) {
		Federation fedConfig = coreConfig.getRemoteConfiguration().getFederation();

		for (FederationOutgoing fo : fedConfig.getFederationOutgoing()) {
			if (fo != null && fo.getDisplayName() != null && fo.getDisplayName().equals(outgoing.getDisplayName())) {
				fo.setEnabled(enabled);
			}
		}
		coreConfig.setAndSaveFederation(fedConfig);
	}

	@Override
	public synchronized void enableOutgoing(String name) {
		FederatedSubscriptionManager subscriptionStore = SubscriptionStore.getInstanceFederatedSubscriptionManager();
		ConnectionStatus cachedConnectionStatus = subscriptionStore
				.getAndPutFederateOutgoingStatus(name, subscriptionStore.getFederationConnectionStatus(name));

		// if cache returns null.. lets proceed, or if
		// if ignite node associated with the ConnectionStatus doesnt exist or the ConnectionStatus is disabled
		// it is safe to start it here
		if (cachedConnectionStatus == null || (IgniteHolder.getInstance().getIgnite().cluster().node(UUID.fromString(cachedConnectionStatus.getNodeId())) == null
				|| cachedConnectionStatus.getConnectionStatusValue() == ConnectionStatusValue.DISABLED)) {

			FederationOutgoing outgoing = getOutgoingConnection(name);

			ConnectionStatus status = SubscriptionStore.getInstanceFederatedSubscriptionManager()
					.getFederationConnectionStatus(outgoing.getDisplayName());
			
			if (status == null) {
				throw new IllegalStateException("null status");
			}

			outgoing.setEnabled(true);

			initiateOutgoing(outgoing, status);

			SubscriptionStore.getInstanceFederatedSubscriptionManager().updateFederateOutgoingStatusCache(outgoing.getDisplayName(), status);

			saveOutgoing(outgoing, true);
		}
	}

	@Override
	public synchronized void addOutgoingConnection(String name, String host, int port, int reconnect, int maxRetries,
			boolean unlimitedRetries, boolean enable, int protocolVersion, String fallback) {

		// check for dupes by name in the cache
		if (SubscriptionStore.getInstanceFederatedSubscriptionManager().getCachedFederationConnectionStatus(name) != null) {
			throw new DuplicateFederateException("outgoing " + name + " already exists");
		}

		if (!getOutgoingConnections(host, port).isEmpty()) {
			throw new DuplicateFederateException("outgoing for " + host + ", " + port + " already exists");
		}

		Federation.FederationOutgoing outgoing = new Federation.FederationOutgoing();
		outgoing.setEnabled(enable);
		outgoing.setAddress(host);
		outgoing.setDisplayName(name);
		outgoing.setPort(port);
		outgoing.setReconnectInterval(reconnect);
		outgoing.setProtocolVersion(protocolVersion);
		outgoing.setFallback(fallback);
		outgoing.setMaxRetries(maxRetries);
		outgoing.setUnlimitedRetries(unlimitedRetries);

		Federation federationConfig = coreConfig.getRemoteConfiguration().getFederation();

		federationConfig.getFederationOutgoing().add(outgoing);

		coreConfig.setAndSaveFederation(federationConfig);

		SubscriptionStore.getInstanceFederatedSubscriptionManager().putOutgoingNumRetries(name);
		SubscriptionStore.getInstanceFederatedSubscriptionManager().putOutgoingRetryScheduled(name);

		if (enable) {

			ConnectionStatus status = new ConnectionStatus(ConnectionStatusValue.CONNECTING);

			SubscriptionStore.getInstanceFederatedSubscriptionManager().getAndPutFederateOutgoingStatus(outgoing.getDisplayName(), status);

			initiateOutgoing(outgoing, status);
		} else {

			if (logger.isTraceEnabled()) {
				logger.trace("creating disabled outgoing " + outgoing.getDisplayName());
			}

			SubscriptionStore.getInstanceFederatedSubscriptionManager()
					.getAndPutFederateOutgoingStatus(outgoing.getDisplayName(), new ConnectionStatus(ConnectionStatusValue.DISABLED));
		}
	}

	@Override
	public void removeOutgoing(String name) {
		disableOutgoingForNode(name);

		for (Federation.FederationOutgoing f : coreConfig.getRemoteConfiguration()
				.getFederation()
				.getFederationOutgoing()) {
			if (f.getDisplayName().compareTo(name) == 0) {

				Federation federationConfig = coreConfig.getRemoteConfiguration().getFederation();

				federationConfig.getFederationOutgoing().remove(f); // avoid concurrentmodification with the break

				coreConfig.setAndSaveFederation(federationConfig);

				ConnectionStatus status = SubscriptionStore.getInstanceFederatedSubscriptionManager().removeFederateOutgoingStatus(f.getDisplayName());
				SubscriptionStore.getInstanceFederatedSubscriptionManager().removeOutgoingNumRetries(f.getDisplayName());
				SubscriptionStore.getInstanceFederatedSubscriptionManager().removeOutgoingRetryScheduled(f.getDisplayName());

				if (status == null) {
					if (logger.isDebugEnabled()) {
						logger.debug("outgoing status for outgoing " + name + " not found in status map");
					}
					return;
				}
				break;
			}
		}
	}

	@Override
	public List<X509Certificate> getCAList() {
		List<X509Certificate> certs = new LinkedList<>();
		try {
			Enumeration<String> aliases = getSSLCache().get(SSL_TRUSTSTORE_KEY).getTrust().aliases();
			while (aliases.hasMoreElements()) {
				String s = aliases.nextElement();
				KeyStore.Entry entry = getSSLCache().get(SSL_TRUSTSTORE_KEY).getTrust().getEntry(s, null);
				if (entry instanceof KeyStore.TrustedCertificateEntry) {
					certs.add((X509Certificate) ((KeyStore.TrustedCertificateEntry) entry).getTrustedCertificate());
				}
			}
		} catch (Exception e) {
			logger.warn("exception getting CA list", e);
		}
		return certs;
	}

	public List<X509Certificate> getCALocalList() {
		List<X509Certificate> certs = new LinkedList<>();
		try {
			Enumeration<String> aliases = SSLConfig.getInstance(coreConfig.getRemoteConfiguration().getFederation().getFederationServer().getTls()).getTrust().aliases();
			while (aliases.hasMoreElements()) {
				String s = aliases.nextElement();
				KeyStore.Entry entry = getSSLCache().get(SSL_TRUSTSTORE_KEY).getTrust().getEntry(s, null);
				if (entry instanceof KeyStore.TrustedCertificateEntry) {
					certs.add((X509Certificate) ((KeyStore.TrustedCertificateEntry) entry).getTrustedCertificate());
				}
			}
		} catch (Exception e) {
			logger.warn("exception getting CA list", e);
		}
		return certs;
	}

	@Override
	public void addCA(@NotNull X509Certificate ca) {
		try {
			if (logger.isTraceEnabled()) {
				logger.trace("adding federate chain of trust entry: " + ca);
			}

			Tls tlsConfig = coreConfig.getRemoteConfiguration().getFederation().getFederationServer().getTls();

			String dn = ca.getSubjectX500Principal().getName();

			if (logger.isTraceEnabled()) {
				logger.trace("new chain of trust entry dn: " + dn);
			}

			// Use the CN as the truststore alias. This will throw an exception if the CN
			// can't be figured out.
			String alias = MessageConversionUtil.getCN(dn);

			if (logger.isTraceEnabled()) {
				logger.trace("new chain of trust entry alias: " + alias);
			}

			SSLConfig sslConfig = getSSLCache().get(SSL_TRUSTSTORE_KEY);
			sslConfig.getTrust().setEntry(alias, new KeyStore.TrustedCertificateEntry(ca), null);
			getSSLCache().put(SSL_TRUSTSTORE_KEY, sslConfig);
		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception adding ca", e);
			}
		}

	}

	@Override
	public void removeCA(X509Certificate ca) {
		logger.warn("Removing cert: " + ca.getSubjectDN());
		try {
			String alias = getSSLCache().get(SSL_TRUSTSTORE_KEY).getTrust().getCertificateAlias(ca);
			SSLConfig sslConfig = getSSLCache().get(SSL_TRUSTSTORE_KEY);
			sslConfig.getTrust().deleteEntry(alias);
			getSSLCache().put(SSL_TRUSTSTORE_KEY, sslConfig);
		} catch (Exception e) {
			logger.warn("exception removing ca", e);
		}
	}

	private synchronized void saveTruststoreFile() {
		Tls tlsConfig = coreConfig.getRemoteConfiguration().getFederation().getFederationServer().getTls();
		try (FileOutputStream fos = new FileOutputStream(tlsConfig.getTruststoreFile())) {
			getSSLCache().get(SSL_TRUSTSTORE_KEY).getTrust()
				.store(fos, tlsConfig.getTruststorePass().toCharArray());
			if (logger.isTraceEnabled()) {
				logger.trace("federation truststore file save complete");
			}
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
			logger.warn("exception saving truststore file", e);
		}
	}

	public List<CodecSource> getCodecSources(Tls tlsConfig) {
		List<CodecSource> codecSources = new ArrayList<>();

		// always include SSL and SSL federate cert auth in the codec pipeline
		codecSources.add(Codec.defaultCodecSource);
		codecSources.add(SslCodec.getSslCodecSource(tlsConfig));
		codecSources.add(FederateSslPreAuthCodec.getCodecSource());

		if (logger.isTraceEnabled()) {
			logger.trace("codecSources for federation: " + codecSources);
		}

		return codecSources;
	}

	public LinkedBlockingQueue<ProtocolListenerInstantiator<FederatedEvent>> getProtocolListeners() {
		LinkedBlockingQueue<ProtocolListenerInstantiator<FederatedEvent>> protocolListeners = new LinkedBlockingQueue<>();

		protocolListeners.add(federateDataReceivedListener);
		protocolListeners.add(federateSubscriptionListener);

		return protocolListeners;
	}

	private void addFederateUsersToGroups(@NotNull String fedId, @NotNull Set<String> groupNames,
			@NotNull Direction direction) {
		try {
			Federate federate = getFederate(fedId);

			if (federate == null) {
				throw new IllegalStateException("no federate registered for id " + fedId);
			}

			for (User user : groupManager().getAllUsers()) {
				// only consider federate users
				if (user.getClass().getName().equals(FederateUser.class.getName())) {
					FederateUser fed = (FederateUser) user;
					// match on federate config
					if (fed.getFederateConfig().getId().equals(fedId)) {
						for (String groupName : groupNames) {
							groupManager().addUserToGroup(fed, new Group(groupName, direction));
						}
					}
				}
			}
		} catch (Exception e) {
			logger.warn("exception updating federation user groups " + e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

	private void removeFederateUsersFromGroups(@NotNull String fedId, @NotNull Set<String> groupNames,
			@NotNull Direction direction) {
		try {
			Federate federate = getFederate(fedId);
			if (federate == null) {
				throw new IllegalStateException("no federate registered for id " + fedId);
			}

			for (User user : groupManager().getAllUsers()) {
				// only consider federate users
				if (user.getClass().getName().equals(FederateUser.class.getName())) {
					FederateUser fed = (FederateUser) user;
					// match on federate config
					if (fed.getFederateConfig().getId().equals(fedId)) {
						for (String groupName : groupNames) {
							groupManager().removeUserFromGroup(fed, new Group(groupName, direction));
						}
					}
				}
			}
		} catch (Exception e) {
			logger.warn("exception updating federation user groups " + e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

	public FederateSubscription addFederateSubscription(String uid, Protocol<FederatedEvent> protocol,
			ChannelHandler handler, String xpath, Filter filter, boolean shareAlerts,
			ConnectionInfo connection) {
		FederateSubscription subscription = new FederateSubscription();
		subscription.uid = uid;
		subscription.xpath = xpath;
		// TODO: replace with cot event sender
		// subscription.sender = new CotEventSender(protocol, handler);
		subscription.setProtoEncoder(protocol);
		subscription.setHandler(handler);

		subscription.to = handler == null ? "" : handler.toString();
		subscription.setShareAlerts(shareAlerts);

		if (filter != null && filter.getGeospatialFilter() != null) {
			subscription.geospatialEventFilter = new GeospatialEventFilter(filter.getGeospatialFilter());
		}

		SubscriptionStore.getInstanceFederatedSubscriptionManager().putFederateSubscription(connection, subscription);

		if (logger.isDebugEnabled()) {
			logger.debug("Added FederateSubscription: " + subscription);
		}

		return subscription;
	}

	public FigFederateSubscription addFigFederateSubscription(String uid, Protocol<FederatedEvent> protocol,
			ChannelHandler handler, String xpath, Filter filter, boolean shareAlerts,
			ConnectionInfo connection, TakFigClient figClient) {
		FigFederateSubscription subscription = new FigFederateSubscription(figClient);
		subscription.uid = uid;
		subscription.xpath = xpath;
		// TODO: replace with cot event sender
		// subscription.sender = new CotEventSender(protocol, handler);
		subscription.setProtoEncoder(protocol);
		subscription.setHandler(handler);

		subscription.to = handler == null ? "" : handler.toString();
		subscription.setShareAlerts(shareAlerts);

		if (filter != null && filter.getGeospatialFilter() != null) {
			subscription.geospatialEventFilter = new GeospatialEventFilter(filter.getGeospatialFilter());
		}

		SubscriptionStore.getInstanceFederatedSubscriptionManager().putFederateSubscription(connection, subscription);

		if (logger.isDebugEnabled()) {
			logger.debug("Added FederateSubscription: " + subscription);
		}

		return subscription;
	}

	public FigServerFederateSubscription addFigFederateSubscriptionV2(String uid, Protocol<FederatedEvent> protocol,
			ChannelHandler handler, String xpath, Filter filter, boolean shareAlerts,
			ConnectionInfo connection, Federate federate) {
		// connectionId is the sessionId for the gRPC TLS session
		FigServerFederateSubscription subscription = new FigServerFederateSubscription(connection.getConnectionId(), federate);
		subscription.uid = uid;
		subscription.xpath = xpath;
		// TODO: replace with cot event sender
		// subscription.sender = new CotEventSender(protocol, handler);
		subscription.setProtoEncoder(protocol);
		subscription.setHandler(handler);

		subscription.to = handler == null ? "" : handler.toString();
		subscription.setShareAlerts(shareAlerts);

		if (filter != null && filter.getGeospatialFilter() != null) {
			subscription.geospatialEventFilter = new GeospatialEventFilter(filter.getGeospatialFilter());
		}

		SubscriptionStore.getInstanceFederatedSubscriptionManager().putFederateSubscription(connection, subscription);

		if (logger.isDebugEnabled()) {
			logger.debug("Added FederateSubscription: " + subscription);
		}

		return subscription;
	}

	private final ProtocolListenerInstantiator<FederatedEvent> federateDataReceivedListener = new AbstractAutoProtocolListener<FederatedEvent>() {
		@Override
		public void onDataReceived(FederatedEvent data, ChannelHandler handler, Protocol<FederatedEvent> protocol) {
			handleOnDataReceived(data, handler, protocol);
		}

		@Override
		public String toString() {
			return "federate_data_submittor";
		}
	};

	public void handleOnDataReceived(FederatedEvent data, ChannelHandler handler, Protocol<FederatedEvent> protocol) {

		String fedId = ((TcpChannelHandler) handler).getConnectionInfo().getCert().getSubjectDN().toString();



		if (data.hasEvent()) {
			try {
				CotEventContainer cot = ProtoBufHelper.getInstance().proto2cot(data.getEvent());

				if (coreConfig.getRemoteConfiguration().getSubmission().isIgnoreStaleMessages()) {
					if (MessageConversionUtil.isStale(cot)) {
						if (logger.isDebugEnabled()) {
							logger.debug("ignoring stale federated message: " + cot);
						}
						return;
					}
				}

				cot.setContext(GroupFederationUtil.FEDERATE_ID_KEY, fedId);
				// burn handler and protocol into message context map
				cot.setContext(Constants.SOURCE_TRANSPORT_KEY, handler);
				cot.setContext(Constants.SOURCE_HASH_KEY, handler.identityHash());
				cot.setContext(Constants.SOURCE_PROTOCOL_KEY, protocol);

				FederateSubscription sub = (FederateSubscription) SubscriptionStore.getInstance().getByHandler(handler);

				if (sub != null && sub.getUser() != null) {
					cot.setContext(Constants.USER_KEY, sub.getUser());
					FederateUser fuser = (FederateUser) sub.getUser();
					cot.setContextValue(Constants.ARCHIVE_EVENT_KEY, fuser.getFederateConfig().isArchive());

					NavigableSet<Group> groups = groupManager().getGroups(fuser);

					if (groups != null) {

						NavigableSet<Group> filteredGroups = GroupFederationUtil.getInstance()
								.filterGroupDirection(Direction.IN, groups);

						if (logger.isTraceEnabled()) {
							logger.trace("marking groups in V1 federated message: " + filteredGroups);
						}

						// Only put IN groups in the message - out groups do not matter here
						cot.setContext(Constants.GROUPS_KEY, filteredGroups);
					}
				} else {
					if (logger.isDebugEnabled()) {
						logger.debug("user not found for subscription in federateDataReceivedListener");
					}
				}

				if (logger.isTraceEnabled()) {
					logger.trace(String.format(
							"Federate Submission service receiving message -- handler: %s protocol: %s message: %s",
							handler, protocol, cot.partial()));
				}

				// submit message to marti core for processing
				messenger().send(cot);

				// store for latestSA
				ConcurrentHashMap<String, RemoteContactWithSA> remoteContacts = SubscriptionStore
						.getInstanceFederatedSubscriptionManager()
						.getRemoteContactsMapByChannelHandler(handler);
				if (remoteContacts != null && remoteContacts.containsKey(cot.getUid())) {
					remoteContacts.get(cot.getUid()).setLastSA(cot);
				}
			} catch (Exception e) {
				if (logger.isDebugEnabled()) {
					logger.debug("exception getting federate DN", e);
				}
			}
		}

		if (data.hasContact()) {
			ContactListEntry contact = data.getContact();
			if (contact.getOperation() == CRUD.DELETE) {
				Subscription mySub = SubscriptionStore.getInstance().getByHandler(handler);
				CotEventContainer e = ProtoBufHelper.getInstance().protoBuf2delContact(contact);
				Collection<Subscription> reachable = GroupFederationUtil.getInstance().getReachableSubscriptions(mySub);
				if (reachable.contains(mySub)) {
					// logger.warn("reachable contained self!");
					reachable.remove(mySub);
				}
				e.setContext(GroupFederationUtil.FEDERATE_ID_KEY, fedId);
				e.setContext(Constants.SOURCE_TRANSPORT_KEY, handler);
				e.setContext(Constants.SOURCE_PROTOCOL_KEY, protocol);
				e.setContextValue(Constants.SUBSCRIBER_HITS_KEY,
						MessagingDependencyInjectionProxy.getInstance()
								.subscriptionStore()
								.subscriptionCollectionToConnectionIdSet(reachable));
				e.setContextValue(Constants.USER_KEY, mySub.getUser());
				messenger().send(e);
			}
			addRemoteContact(data.getContact(), handler);
		}
	}

	private final ProtocolListenerInstantiator<FederatedEvent> federateSubscriptionListener = new AbstractAutoProtocolListener<FederatedEvent>() {
		AtomicBoolean alreadyClosed = new AtomicBoolean(true);

		@Override
		public void onConnect(ChannelHandler handler, Protocol<FederatedEvent> protocol) {
			alreadyClosed.set(false);
			handleOnConnect(handler, protocol);
		}

		@Override
		public void onOutboundClose(ChannelHandler handler, Protocol<FederatedEvent> protocol) {
			handleClose(handler);
		}

		@Override
		public void onInboundClose(ChannelHandler handler, Protocol<FederatedEvent> protocol) {
			handleClose(handler);
		}

		private void handleClose(ChannelHandler handler) {
			ConnectionInfo connection = ((TcpChannelHandler) handler).getConnectionInfo();

			if (alreadyClosed.compareAndSet(false, true) == false) {
				return;
			}

			MessagingUtilImpl.getInstance()
					.processFederateClose(connection, handler, SubscriptionStore.getInstance().getByHandler(handler));
		}

		@Override
		public String toString() {
			return "federate_subscription_lifecycle_listener";
		}
	};

	public void handleOnConnect(ChannelHandler handler, Protocol<FederatedEvent> protocol) {
		ConnectionInfo connection = ((TcpChannelHandler) handler).getConnectionInfo();
		if (logger.isDebugEnabled()) {
			logger.debug(
					"federateSubscriptionListener onConnection connectionInfo: " + connection + " handler: " + handler);
		}

		try {

			FederateUser user = (FederateUser) groupManager().getUserByConnectionId(connection.getConnectionId());

			if (user == null) {
				throw new IllegalStateException("null user");
			}

			// create federate subscription
			FederateSubscription subscription = addFederateSubscription(connection.getConnectionId(), protocol, handler,
					null, null, user.getFederateConfig().isShareAlerts(), connection);

			if (logger.isDebugEnabled()) {
				logger.debug("created federate subscription: " + subscription);
			}

			// set user on subscription, so that message brokering will be able to find the
			// user
			subscription.setUser(user);
			subscription.callsign = "Federate:\n" + user.getFederateConfig().getName();

			DistributedSubscriptionManager.getInstance().addRawSubscription(subscription);

			if (MessagingDependencyInjectionProxy.getInstance().coreConfig().getBuffer().getLatestSA().isEnable()) {
				try {
					MessagingUtilImpl.getInstance().sendLatestReachableSA(user);
				} catch (Exception e) {
					logger.error("sendLatestSA threw exception: " + e.getMessage(), e);
				}
			}

			// send a current snapshot of the contact list (current subscriptions) to this
			// federate
			GroupFederationUtil.getInstance().sendLatestContactsToFederate(subscription);
			updateFederationSubscriptionCache(connection, user.getFederateConfig());

		} catch (Exception e) {
			logger.warn("Exception creating federate subscription: " + handler, e);
		}
	}

	@SuppressWarnings("unused")
	private String getInfo(Federation.Federate fed) {
		return "Federate: " + fed.getName() + " inbound group: " + fed.getInboundGroup() + " outbound groups: "
				+ fed.getOutboundGroup();
	}

	public String getInfo(FederationOutgoing outgoing) {
		String max_retries;
		if (outgoing.isUnlimitedRetries()) {
			max_retries = "unlimited";
		} else {
			max_retries = Integer.toString(outgoing.getMaxRetries());
		}
		return "FederationOutgoing [displayName=" + outgoing.getDisplayName() + ", address=" + outgoing.getAddress()
				+ ", port=" + outgoing.getPort() + ", enabled=" + outgoing.isEnabled() + ", reconnectInterval="
				+ outgoing.getReconnectInterval() + ", fallback= " + outgoing.getFallback() + "maxRetries= "
				+ max_retries + "]";
	}

	@SuppressWarnings("incomplete-switch")
	public void addRemoteContact(ContactListEntry contact, ChannelHandler handler) {

		if (logger.isDebugEnabled()) {
			logger.debug("process remote contact: " + contact + " handler: " + handler);
		}

		if (contact.getOperation() == null
				|| (Strings.isNullOrEmpty(contact.getCallsign()) && Strings.isNullOrEmpty(contact.getUid()))) {
			if (logger.isDebugEnabled()) {
				logger.debug("ignoring empty contact message");
			}
			return;
		}

		RemoteContactWithSA rc = new RemoteContactWithSA();
		rc.setContactName(contact.getCallsign());
		rc.setUid(contact.getUid());
		switch (contact.getOperation()) {
		case DELETE:
			if (logger.isDebugEnabled()) {
				logger.debug("processing DELETE Remote contact: " + rc.getUid() + ":" + rc.getContactName() + " "
						+ contact.getOperation().toString());
			}
			// logger.debug("Got a delete contact request: " + contact.getUid());
			if (SubscriptionStore.getInstanceFederatedSubscriptionManager().getRemoteContactsMapByChannelHandler(handler) != null) {
				// doing this loop stinks, but needed as long as we need the callsignmap in
				// submgr
				Iterator<RemoteContactWithSA> i = SubscriptionStore.getInstanceFederatedSubscriptionManager()
						.getRemoteContactsMapByChannelHandler(handler)
						.values()
						.iterator();
				while (i.hasNext()) {
					RemoteContact r = i.next();
					if (r.getUid().compareTo(contact.getUid()) == 0) {
						DistributedSubscriptionManager.getInstance()
								.removeClientFromSubscription(r.getUid(), r.getContactName(), handler);
						break;
					}
				}
				// if we didn't need to find the callsign, we could just do this
				SubscriptionStore.getInstanceFederatedSubscriptionManager()
						.getRemoteContactsMapByChannelHandler(handler)
						.remove(rc.getUid());

			} else {
				logger.warn("Got delete contact for federate that had no contacts, ignoring");
			}
			break;
		case UPDATE:
		case CREATE:
			if (logger.isDebugEnabled()) {
				logger.debug("processing CREATE or UPDATE Remote contact: " + rc.getUid() + ":" + rc.getContactName()
						+ " " + contact.getOperation().toString());
			}

			if (!(SubscriptionStore.getInstanceFederatedSubscriptionManager().getRemoteContactsMapByChannelHandler(handler) != null)) {
				SubscriptionStore.getInstanceFederatedSubscriptionManager()
						.putRemoteContactsMapToChannelHandler(handler, new ConcurrentHashMap<String, RemoteContactWithSA>());
			}
			SubscriptionStore.getInstanceFederatedSubscriptionManager()
					.getRemoteContactsMapByChannelHandler(handler)
					.put(rc.getUid(), rc);

			if (logger.isDebugEnabled()) {
				logger.debug("remote contact added");
			}

			if (logger.isDebugEnabled()) {
				logger.debug("remote contact list for " + handler + " : "
						+ SubscriptionStore.getInstanceFederatedSubscriptionManager().getRemoteContactsMapByChannelHandler(handler));
			}

			Subscription s = DistributedSubscriptionManager.getInstance().getSubscriptionByClientUid(contact.getUid());
			if (s == null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Remote contact: " + rc.getUid() + ":" + rc.getContactName() + " " + contact.getOperation());
				}
				DistributedSubscriptionManager.getInstance().setClientForSubscription(contact.getUid(), contact.getCallsign(), handler, false);
			}
			break;
		case READ:
			logger.warn("someone sent a non-sensical read operation");
		}
	}

	public void addLocalContact(CotEventContainer cot, ChannelHandler src) {

		if (logger.isDebugEnabled()) {
			logger.debug("federation manager add local contact " + cot.asXml());
		}

		// prevent NullPointerExceptions from getting thrown by the msg builder
		if (cot.getCallsign() == null || cot.getUid() == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("null callsign or uid in FederationManager.addLocalContact " + cot.asXml());
			}
			return;
		}

		ContactListEntry newContact = ContactListEntry.newBuilder()
				.setCallsign(cot.getCallsign())
				.setUid(cot.getUid())
				.setOperation(CRUD.CREATE)
				.build();
		try {
			FederatedEvent f = FederatedEvent.newBuilder().setContact(newContact).build();
			Subscription srcSub = SubscriptionStore.getInstance().getByHandler(src);
			
			Set<Subscription> reachable = GroupFederationUtil.getInstance().getReachableSubscriptionsSet(srcSub);
			reachable.addAll(GroupFederationUtil.getInstance().getReachableFederatedGroupMappingSubscriptons(srcSub));
								
			for (Subscription s : reachable) {
				if (s instanceof FederateSubscription) {
					((FederateSubscription) s).submitLocalContact(f, System.currentTimeMillis());
				}
			}
		} catch (NumberFormatException e) {
			logger.warn("Problem parsing SA message from " + cot.getUid() + ":" + cot.getCallsign() + ": " + e.getMessage());
		}
	}

	public void removeLocalContact(String uid) {
		// this call doesn't actually send network traffic, so we can blindly apply to
		// all
		for (FederateSubscription f : SubscriptionStore.getInstanceFederatedSubscriptionManager().getFederateSubscriptions()) {
			f.removeLocalContact(uid);
		}
	}

	public boolean checkAndCloseZombieFederate(ConnectionStatus possibleZombie) {

		boolean result = false;

		FederateSubscription dupeFedSub = SubscriptionStore.getInstanceFederatedSubscriptionManager()
				.getFederateSubscription(possibleZombie.getConnection());

		long activityInterval = new Date().getTime() - dupeFedSub.lastProcTime.get();

		if (logger.isDebugEnabled()) {
			logger.debug("activityInterval for " + dupeFedSub + ": " + activityInterval);
		}

		if (activityInterval >= DistributedFederationManager.FED_ZOMBIE_TIMEOUT) {

			if (logger.isDebugEnabled()) {
				logger.debug("possible zombie detected: " + possibleZombie + " attempting close.");
			}

			result = true;

			TcpChannelHandler handler = (TcpChannelHandler) possibleZombie.getConnection().getHandler();

			if (handler == null) {
				throw new IllegalStateException("null or invalid object type channel handler for possible zombie federate " + possibleZombie);
			}

			// close the connection
			handler.forceClose();

			// send disconnect message, and stop tracking the federate as a subscription
			MessagingUtilImpl.getInstance()
				.processFederateClose(possibleZombie.getConnection(), handler, SubscriptionStore.getInstance().getByHandler(handler));
		}

		return result;
	}

	public void checkAndSetReconnectStatus(FederationOutgoing outgoing, String errorMsg) {
		ConnectionStatus status = SubscriptionStore.getInstanceFederatedSubscriptionManager().getFederationConnectionStatus(outgoing.getDisplayName());

		if (status != null) {
			if (outgoing.getReconnectInterval() > 0 && outgoing.isEnabled()) {
				status.setConnectionStatusValue(ConnectionStatusValue.WAITING_TO_RETRY);
				status.setLastError(errorMsg);

				tryReconnect(status, outgoing);
			} else {
				status.setConnectionStatusValue(ConnectionStatusValue.DISABLED);
				status.setLastError(errorMsg);

				outgoing.setEnabled(false);
			}

			SubscriptionStore.getInstanceFederatedSubscriptionManager().updateFederateOutgoingStatusCache(outgoing.getDisplayName(), status);
		}
	}

	// Schedule a reconnect only if the conditions for reconnection are met. Also
	// check these connections when the scheduled job executes.
	public void tryReconnect(final ConnectionStatus status, final FederationOutgoing outgoing) {
		boolean scheduleRetry = SubscriptionStore.getInstanceFederatedSubscriptionManager().getOutgoingRetryScheduled(outgoing.getDisplayName()).compareAndSet(false, true);

		if (SubscriptionStore.getInstanceFederatedSubscriptionManager().getFederationConnectionStatus(outgoing.getDisplayName()) != null
				&& status.compareAndSetConnectionStatusValue(ConnectionStatusValue.WAITING_TO_RETRY,
						ConnectionStatusValue.RETRY_SCHEDULED) && scheduleRetry) {

			Resources.fedReconnectThreadPool.schedule(new Runnable() {
				@Override
				public void run() {
					boolean retryScheduled = SubscriptionStore.getInstanceFederatedSubscriptionManager().getOutgoingRetryScheduled(outgoing.getDisplayName()).compareAndSet(true, false);
					if (SubscriptionStore.getInstanceFederatedSubscriptionManager() .getFederationConnectionStatus(outgoing.getDisplayName()) != null
							&& status.compareAndSetConnectionStatusValue(ConnectionStatusValue.RETRY_SCHEDULED, ConnectionStatusValue.CONNECTING) && retryScheduled) {

						if (outgoing.isUnlimitedRetries() ||
								(SubscriptionStore.getInstanceFederatedSubscriptionManager()
													.getOutgoingNumRetries(outgoing.getDisplayName())
													.get() < outgoing.getMaxRetries())) {

							int retry_num = SubscriptionStore.getInstanceFederatedSubscriptionManager()
									.getOutgoingNumRetries(outgoing.getDisplayName())
									.incrementAndGet();

							if (logger.isWarnEnabled()) {
								logger.warn("trying to reconnect outgoing " + outgoing.getDisplayName() + " for the "
										+ retry_num + "th time.");
							}

							initiateOutgoing(outgoing, status);
							
						} else {
							if (logger.isDebugEnabled()) {
								logger.debug("falling back to: " + outgoing.getFallback());
							}
							try {
								disableOutgoing(outgoing);
								if (outgoing.getFallback() == null
										|| getOutgoingConnection(outgoing.getFallback()) == null) {
									if (logger.isDebugEnabled()) {
										logger.debug("no valid fallback, so we are done, and the connection remains diabled");
									}
								} else {
									enableOutgoing(outgoing.getFallback());
								}
							} catch (Exception e) {
								if (logger.isDebugEnabled()) {
									logger.debug("Remote Exception trying reconnect (enable or disable)", e);
								}
							}
						}
					} else {
						String msg = "not trying to reconnect outgoing " + outgoing.getDisplayName() + " status: " + status;
						if (logger.isWarnEnabled()) {
							logger.warn(msg);
						}
					}
					SubscriptionStore.getInstanceFederatedSubscriptionManager().updateFederateOutgoingStatusCache(outgoing.getDisplayName(), status);
				}
			}, outgoing.getReconnectInterval(), TimeUnit.SECONDS);
		}
	}

	@Override
	public void updateFederateDetails(String federateId, boolean archive, boolean shareAlerts, boolean federatedGroupMapping, boolean automaticGroupMapping, String notes) {

		Federation fedConfig = coreConfig.getRemoteConfiguration().getFederation();

		List<Federate> federates = fedConfig.getFederate();

		for (Federate f : federates) {
			if (f.getId().equals(federateId)) {
				f.setArchive(archive);
				f.setShareAlerts(shareAlerts);
				f.setFederatedGroupMapping(federatedGroupMapping);
				f.setAutomaticGroupMapping(automaticGroupMapping);
				f.setNotes(notes);
				break;
			}
		}
		coreConfig.setAndSaveFederation(fedConfig);
	}

	@Override
	public void removeFederate(String federateId) {

		Federation fedConfig = coreConfig.getRemoteConfiguration().getFederation();

		List<Federate> federates = fedConfig.getFederate();

		for (Federate f : federates) {
			if (f.getId().equals(federateId)) {
				fedConfig.getFederate().remove(f);
				break;
			}
		}

		coreConfig.setAndSaveFederation(fedConfig);
	}

	@Override
	public List<Federate> getConfiguredFederates() {
		if (coreConfig.getRemoteConfiguration().getFederation() == null
				|| coreConfig.getRemoteConfiguration().getFederation().getFederate() == null) {
			return new ArrayList<>();
		}

		return coreConfig.getRemoteConfiguration().getFederation().getFederate();
	}
	
	@Override
	public void submitFederateROL(final ROL rol, final NavigableSet<Group> groups) {
		
		submitFederateROL(rol, groups, null);
	}

	@Override
	public void submitFederateROL(ROL rol, final NavigableSet<Group> groups, String fileHash) {
		if (logger.isDebugEnabled()) {
			logger.debug("Federated ROL: " + rol.getProgram() + " groups: " + groups);
		}
		
		
		// Populate the file contents here in messaging if no content provided. Avoids serializing file over ignite
		try {
			if (rol.getPayloadList().isEmpty() && !Strings.isNullOrEmpty(fileHash)) {
				
		        String groupVector = RemoteUtil.getInstance().bitVectorToString(RemoteUtil.getInstance().getBitVectorForGroups(groups));
				
				// use the file hash to load the file from db / cache
				byte[] fileBytes = MessagingDependencyInjectionProxy.getInstance().esyncService().getContentByHash(fileHash, groupVector);
				
				if (logger.isDebugEnabled()) {
					if (fileBytes == null) {
						logger.debug("null bytes for file " + fileHash);
					} else {
					
					logger.debug("fetched " + fileBytes.length + " for hash " + fileHash);
					
					}
				}
				
				BinaryBlob filePayload = BinaryBlob.newBuilder().setData(ByteString.readFrom(new ByteArrayInputStream(fileBytes))).build();

				ROL.Builder rolBuilder = rol.toBuilder();
				
				rolBuilder.addPayload(filePayload);
				
				if (logger.isDebugEnabled()) {
					logger.debug("Added file payload size " + fileBytes.length + " bytes to rol");
				}
				
				rol = rolBuilder.build();
				
			}
		} catch (Exception e) {
			if (logger.isWarnEnabled()) {
				logger.warn("exception fetching file for federation from data layer", e);
			}
		}
		
		final ROL finalRol = rol;


		try {

			// Federate this ROL message if there is a reachability relationship
			Resources.brokerMatchingProcessor.execute(new Runnable() {
				@Override
				public void run() {

					try {

						for (FederateSubscription destFed : SubscriptionStore.getInstanceFederatedSubscriptionManager().getFederateSubscriptions()) {

							if (CommonGroupDirectedReachability.getInstance().isReachable(groups, destFed.getUser())) {

								Federate federate = null;
								if (destFed instanceof FigServerFederateSubscription) {
									federate = getFederate(((FigFederateSubscription) destFed).getFederate().getId());
								} else if (destFed instanceof FigFederateSubscription) {
									federate = ((FigFederateSubscription) destFed).getFigClient().getFederate();
								}

								if (federate == null) {
									if (logger.isDebugEnabled()) {
										logger.debug("unable to add federate groups to ROL, federate is null ");
									}
									continue;
								}

								ROL.Builder builder = finalRol.toBuilder();

								if (federate.isFederatedGroupMapping()) {
									Set<String> outGroups = GroupFederationUtil.getInstance().filterFedOutboundGroups(
											federate.getOutboundGroup(), groups, federate.getId());
									builder.addAllFederateGroups(outGroups);
								}

								ROL rolWithGroups = builder.build();

								if (destFed instanceof FigServerFederateSubscription) {
									if (logger.isDebugEnabled()) {
										logger.debug("for FigServerFederateSubscription - sending federated ROL "
												+ rolWithGroups.getProgram() + " from groups " + groups + " to " + destFed.getUser());
									}
									((FigServerFederateSubscription) destFed).lazyGetROLClientStream().send(rolWithGroups);

									trackSendChangesEventForFederate(((FigServerFederateSubscription) destFed).getFederate().getId(), ((FigServerFederateSubscription) destFed).getFederate().getName(), true);
									
									try {
							        	Metrics.counter(Constants.METRIC_FED_ROL_MESSAGE_READ_COUNT, "takserver", "messaging").increment();
							        } catch (Exception ex) {
							        	if (logger.isDebugEnabled()) {
							        		logger.debug("error recording fed message read metric", ex);
							        	}
							        }

								} else if (destFed instanceof FigFederateSubscription) {
									if (logger.isDebugEnabled()) {
										logger.debug("for FigFederateSubscription - sending federated ROL " + rolWithGroups.getProgram()
										+ " from groups " + groups + " to " + destFed.getUser());
									}
									((FigFederateSubscription) destFed).getFigClient().getRolCall().sendMessage(rolWithGroups);

									trackSendChangesEventForFederate(((FigFederateSubscription) destFed).getFederate().getId(), ((FigFederateSubscription) destFed).getFederate().getName(), false);
									
									try {
							        	Metrics.counter(Constants.METRIC_FED_ROL_MESSAGE_WRITE_COUNT, "takserver", "messaging").increment();
							        } catch (Exception ex) {
							        	if (logger.isDebugEnabled()) {
							        		logger.debug("error recording fed message write metric", ex);
							        	}
							        }
								}
							}
						}
					} catch (Exception e) {
						if (logger.isWarnEnabled()) {
							logger.warn("exception federating mission change", e);
						}
					}
				}
			});
		} catch (RejectedExecutionException ree) {
			// count how often full queue has blocked ROL send
			Metrics.counter(Constants.METRIC_FEDERATE_ROL_SKIP).increment();
		}
	}

	@Override
	public void reconfigureFederation() {
		Federation fedConfig = coreConfig.getRemoteConfiguration().getFederation();
		if (fedConfig.isEnableFederation()) {
			logger.info("starting federation.");
			if (fedConfig.getFederationServer().isV2Enabled()) {
				logger.info("starting federation v2.");
				tak.server.federation.FederationServer.refreshServer();
			} else {
				logger.info("federation v2 is not enabled, so stopping if running.");
				tak.server.federation.FederationServer.stopServer();
			}
		} else {
			logger.info("federation is disabled, stopping federation server if running");
			tak.server.federation.FederationServer.stopServer();
			disableAllOutgoing();
		}
	}

	@Override
	public void updateFederationSubscriptionCache(ConnectionInfo connectionInfo, Federate federate) {
		SubscriptionStore.getInstanceFederatedSubscriptionManager().updateFederationSubscriptionCache(connectionInfo, federate);
	}
	
	@Override
	public void trackConnectEventForFederate(String fedId, String fedName, boolean isRemote) {
		try {
			MessagingDependencyInjectionProxy.getInstance().fedEventRepo().trackConnectEventForFederate(fedId, fedName, isRemote);
		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception tracking federate connect", e);
			}
		}
	}
	
	@Override
	public void trackSendChangesEventForFederate(String fedId, String fedName, boolean isRemote) {
		try {
			MessagingDependencyInjectionProxy.getInstance().fedEventRepo().trackSendChangesEventForFederate(fedId, fedName, isRemote);
		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception tracking federate connect", e);
			}
		}
	}
	
	@Override
	public void trackDisconnectEventForFederate(String fedId, String fedName, boolean isRemote) {
		try {
			MessagingDependencyInjectionProxy.getInstance().fedEventRepo().trackDisconnectEventForFederate(fedId, fedName, isRemote);
		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception tracking federate disconnect", e);
			}
		}
	}
}
