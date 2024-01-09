

package tak.server;

import java.util.SimpleTimeZone;
import java.util.TimeZone;

/**
 *
 * TAK Server system constants
 *
 */
public class Constants {

	/**
	 * Default host name / IP address where TAK Server is running.
	 */
	public static final String CoreServicesHost = "127.0.0.1";
	public static final Integer CoreServicesRmiPort = 3334;
	public static final String CoreConfigJndiPath = "/CoreConfig";
	public static final String JNDI_PREFIX = "//";
	public static final String COT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.S'Z'";
	public static final String COT_DATE_FORMAT_PAD_MILLIS = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
	public static final String ISO_DATE_FORMAT_NO_MILLIS = "yyyy-MM-dd'T'HH:mm:ssXXX";

	public static final String ENV_CONTEXT = "java:comp/env/"; // JNDI context name for environment variables
	public static final String SQL_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
	// The following should be no smaller than the limit set in the CoT broker
	// see com.bbn.marti.net.StreaminCotProcessor.INDIVIDUAL_COT_MSG_SIZE_LIMIT
	public static final int MAXIMUM_COT_LENGTH = 10*1000000;

	public static final TimeZone UTC_TIME_ZONE = new SimpleTimeZone(0, "UTC");

	public static final long MAXIMUM_TIMESTAMP = 100000000000000l;
	public static final long MINIMUM_TIMESTAMP = -100000000000000l;

	// Audit log logback marker
	public static final String AUDIT_LOG_MARKER = "audit";

	// CoT search queue initial capacity
	public static final int COT_SEARCH_QUEUE_INITIAL_CAPACITY = 524288;

	// API version
	public static final String API_VERSION_HEADER = "API_VERSION";
	public static final String API_VERSION = "3";

	// queue for mission xml deserialization
	public static final int MISSION_XML_QUEUE_INITAL_CAPACITY = 65536;

	// iconset version
	public static final int ICONSET_CURRENT_VERSION = 1;

	// version 
    public static final String SHORT_VER_RESOURCE_PATH = "/shortver.txt";
    public static final String VERSION_INFO_JSON_PATH = "/ver.json";

	// default database connection pool size (per process)
    public static final int CONNECTION_POOL_DEFAULT_SIZE = 16;

	// maximum number of images to include in KML
	public static final int KML_MAX_IMAGE_COUNT = 5;

	public static final String XML_HEADER = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?>";

	// Pub/Sub topics
	public static final String CONFIG_TOPIC_KEY = "CONFIG_TOPIC_KEY";

	// Attribute keys for objects kept in the WebSocket session
    public static final String SOCKET_TOPIC_KEY = "SOCKET_TOPIC";
    public static final String SOCKET_SESSION_KEY = "SOCKET_MARTI_SESSION_ID";
    public static final String SOCKET_AUTH_KEY = "SOCKET_AUTH_KEY";
    public static final String X509_CERT = "X509_CERT";
    public static final String X509_CERT_FP = "X509_CERT_FP";
    
    // Federation
	public static final int STANDARD_FEDERATION = 1;
	public static final int FIG_FEDERATION = 2;
    public static final String FEDERATION_VARIANT = "DIRECT"; // returned by federation version RPC endpoint. To distinguish direct federation (TAK Server) from hub (TAK Server Federation Hub)

	// Messaging
	public static final int STANDARD_NIO = 1;
	public static final int NETTY_NIO = 2;
	public static final int GRPC_INPUT_CHANNEL_VERSION = 1;

	// Grouping
	public static final String ANON_GROUP = "__ANON__";
	
    // WebSocket Topic Reaper
    public static final int TOPIC_REAPER_SCHEDULE = 1200; // run reaper every 20 minutes
    public static final int TOPIC_LIFETIME_SECONDS = 7200; // topic lifetime: 2 hours

	public static final String FEDERATE_ROLE = "ROLE_FEDERATE";
	public static final String ANONYMOUS_ROLE = "ROLE_ANONYMOUS";
	public static final String READONLY_ROLE = "ROLE_READONLY";

	// name of ignite instance
	public static final String IGNITE_INSTANCE_NAME = "ignite-takserver";

	// message subjects for clustering (NATS)
	public static final String CLUSTER_DATA_MESSAGE = "takserver-data-message";
	public static final String CLUSTER_MISSION_DATA_MESSAGE = "takserver-mission-data-message";
	public static final String CLUSTER_CONTROL_MESSAGE = "takserver-control-message";

	// distributed cache / messaging names (ignite)
	public static final String ALL_MISSION_CACHE = "allMissionCache";
	public static final String INVITE_ONLY_MISSION_CACHE = "inviteOnlyMissionCache";
	public static final String ALL_COPS_MISSION_CACHE = "allCopsMissionCache";
	public static final String MISSION_ROLE_CACHE = "missionRoleCache";
	public static final String MISSION_SUBSCRIPTION_CACHE = "missionSubscriptionCache";
	public static final String ENTERPRISE_SYNC_CACHE_NAME = "enterprise-sync-cache";
	public static final String CLUSTER_STATS_CACHE_NAME = "cluster-stats-cache";
	public static final String CONFIGURATION_CACHE_NAME = "config-cache";
	public static final String USER_AUTH_CACHE_NAME = "user-auth-cache";
	public static final String SUBSCRIPTION_CACHE_NAME = "subscription-cache";
	public static final String CONTACTS_CACHE = "contacts-cache";
	public static final String ACTIVE_GROUPS_CACHE = "active-groups-cache";
	public static final String CLASSIFICATION_CACHE = "classification-cache";
	public static final String FEDERATION_OUTGOING_CACHE = "federation-outgoing-cache";
	public static final String IGNITE_SUBSCRIPTION_UID_TRACKER_CACHE = "ignite-subscription-uid-tacker-cache";
	public static final String IGNITE_SUBSCRIPTION_CLIENTUID_TRACKER_CACHE = "ignite-subscription-clientuid-tacker-cache";
	public static final String INGITE_LATEST_SA_CONNECTION_UID_CACHE = "ignite-latest-sa-connection-uid-cache";
	public static final String IGNITE_USER_OUTBOUND_GROUP_CACHE = "ignite-user-outbound-group-cache";
	public static final String IGNITE_USER_INBOUND_GROUP_CACHE = "ignite-user-inbound-group-cache";
	public static final String LATEST_COT_CACHE = "latest-cot-cache";
	public static final String CLIENT_MSG_TS_CACHE = "client-msg-ts-cache";
	public static final String CERTIFICATE_CACHE = "certificate-cache";
	public static final String VIDEO_CACHE = "video-cache";
	public static final String DATA_FEED_CACHE = "data-feed-cache";


	// distributed message topics (ignite)
	public static final String SUBMISSION_TOPIC_BASE = "submission-topic-";
	public static final String TAK_MESSAGE_TOPIC_BASE = "tak-message-topic-";
	public static final String FILE_AUTH_TOPIC = "file-auth-control";

	// ignite node attribute key
	public static final String TAK_PROFILE_KEY = "tak-profile";

	// configuration profile names
	public static final String CLUSTER_PROFILE_NAME = "cluster";
	public static final String MESSAGING_PROFILE_NAME = "messaging";	// messaging process
	public static final String API_PROFILE_NAME = "api";				// API process
	public static final String CONFIG_PROFILE_NAME = "config";				// Config process
	public static final String HTTP_UI_PROFILE_NAME = "http-ui"; 		// reserved for future use
	public static final String MONOLITH_PROFILE_NAME = "monolith"; 		// single-process execution
	public static final String PLUGINS_ENABLED_PROFILE_NAME = "plugins"; 		// plugins enabled
	public static final String PLUGINS_DISABLED_PROFILE_NAME = "no-plugins"; 		// plugins disabled
	public static final String RETENTION_PROFILE_NAME = "retention";


	// Message context keys
	public static final String NATS_MESSAGE_KEY = "nats";
	public static final String CLUSTER_MESSAGE_KEY = "cluster";
	public static final String PLUGIN_MESSAGE_KEY = "plugin";
	public static final String NETCFG_INPUT_PREFIX = "network.input.";
	public static final String SOURCE_TRANSPORT_KEY = "source.transport";
	public static final String SOURCE_HASH_KEY = "source.hash";
	public static final String SOURCE_PROTOCOL_KEY = "source.protocol";
	public static final String DEFAULT_FLOWTAG_KEY = "filter.flowtag.text";
	public static final String DEFAULT_FLOWTAG_TEXT = "marti";
	public static final String USER_KEY = "user";
	public static final String GROUPS_KEY = "groups";
	public static final String GROUPS_BIT_VECTOR_KEY = "groups.bit.vector";
	public static final String NOFEDV2_KEY = "nofedv2";
	public static final String REPEATER_KEY = "repeater";
	public static final String TOPICS_KEY = "topics";
	public static final String OFFLINE_CHANGE_TIME_KEY = "offlineChangeTime";
	public static final String CLIENT_UID_KEY = "clientUid";
	public static final String DO_NOT_BROKER_KEY = "brokering.needed";
	public static final String ARCHIVE_EVENT_KEY = "respository.archive"; // value is a Boolean
	public static final String SUBSCRIBER_HITS_KEY = "SUBSCRIBER_HITS_KEY";
	public static final String CONNECTION_ID_KEY = "connection.id";
	public static final String COT_MESSENGER_TOPIC_KEY = "tak.messenger.topic";
	public static final String PROCESSED_COUNT = "processed.count";
	public static final String MESSAGING_ARCHIVER = "messaging.archiver"; // value is a String
	public static final String PLUGIN_PROVENANCE = "plugin.provenance";
	public static final String STORE_FORWARD_KEY = "storeforward";
	public static final String DATA_FEED_KEY = "data.feed";
	public static final String DATA_FEED_UUID_KEY = "data.feed.uuid";

	// Grid Service Names
	public static final String DISTRIBUTED_FEDERATION_MANAGER = "distributed-federation-manager";
	public static final String DISTRIBUTED_SUBSCRIPTION_MANAGER = "distributed-subscription-manager";
	public static final String DISTRIBUTED_SUBSCRIPTION_STORE = "distributed-subscription-store"; // not implemented
	public static final String DISTRIBUTED_GROUP_MANAGER = "distributed-group-manager";
	public static final String DISTRIBUTED_CONTACT_MANAGER = "distributed-contact-manager";
	public static final String DISTRIBUTED_CONFIGURATION = "distributed-configuration";
	public static final String DISTRIBUTED_REPEATER_MANAGER = "distributed-repeater-manager";
	public static final String DISTRIBUTED_SERVER_INFO = "distributed-server-info";
    public static final String DISTRIBUTED_USER_FILE_MANAGER = "distributed-user-file-manager";
	public static final String DISTRIBUTED_METRICS_COLLECTOR = "distributed-metrics-collector";
	public static final String DISTRIBUTED_INPUT_MANAGER = "distributed-input-manager";
	public static final String DISTRIBUTED_SECURITY_MANAGER = "distributed-security-manager";
	public static final String DISTRIBUTED_FEDERATION_HTTP_CONNECTOR_SERVICE= "distributed-federation-http-connector-service";
	public static final String DISTRIBUTED_PLUGIN_MANAGER = "distributed-plugin-manager";
	public static final String DISTRIBUTED_INJECTION_SERVICE = "distributed-injection-service";
	public static final String DISTRIBUTED_RETENTION_QUERY_MANAGER = "distributed-retention-query-manager";
	public static final String DISTRIBUTED_RETENTION_POLICY_CONFIGURATION = "distributed-retention-policy-configuration";
	public static final String DISTRIBUTED_MISSION_ARCHIVE_MANAGER = "distributed-mission-archive-manager";
	public static final String DISTRIBUTED_QOS_MANAGER = "distributed-qos-manager";
	public static final String DISTRIBUTED_SYSTEM_INFO_API = "distributed-system-info-api";
    public static final String DISTRIBUTED_PLUGIN_DATA_FEED_API = "distributed-plugin-data-feed-api";
    public static final String DISTRIBUTED_PLUGIN_API = "plugin-api";
    public static final String DISTRIBUTED_PLUGIN_SELF_STOP_API = "distributed-plugin-self-stop-api";
    public static final String DISTRIBUTED_PLUGIN_MISSION_API = "distributed-plugin-mission-api";
    public static final String DISTRIBUTED_DATAFEED_COT_SERVICE = "distributed-datafeed-cot-service";
    public static final String DISTRIBUTED_PLUGIN_FILE_API = "distributed-plugin-file-api";
	public static final String DISTRIBUTED_PLUGIN_CORECONFIG_API = "distributed-plugin-coreconfig-api";

    // Bean Names
    public static final String DISTRIBUTED_COT_MESSENGER = "cotMessenger";
    public static final String DISTRIBUTED_TAK_MESSENGER = "takMessenger";
    public static final String TAKMESSAGE_MAPPER = "takMessageMapper";

    public static final String DISTRIBUTED_CONFIGURATION_BEAN = "distributedConfigurationBean";
    public static final String MESSAGING_CORE_CONFIG_PROXY_BEAN = "messagingCoreConfigProxyBean";

    public static final String MARTI_XPATH = "/event/detail/marti";

    // loggers
    public static final String CHANGE_LOGGER = "missionchange";
    
    // metrics
    public static final String METRIC_MESSAGE_READ_COUNT = "message.read";
    public static final String METRIC_MESSAGE_WRITE_COUNT = "message.write";
    public static final String METRIC_MESSAGE_PRECONVERT_COUNT = "message.preconverted";

    
    public static final String METRIC_FED_DATA_MESSAGE_READ_COUNT = "fed.message.read.data";
    public static final String METRIC_FED_DATA_MESSAGE_WRITE_COUNT = "fed.message.write.data";
    
    public static final String METRIC_FED_ROL_MESSAGE_READ_COUNT = "fed.message.read.rol";
    public static final String METRIC_FED_ROL_MESSAGE_WRITE_COUNT = "fed.message.write.rol";
    
    public static final String METRIC_FED_CONTACT_MESSAGE_READ_COUNT = "fed.message.read.contact";
    public static final String METRIC_FED_CONTACT_MESSAGE_WRITE_COUNT = "fed.message.write.contact";
    
    public static final String METRIC_MESSAGE_QOS_READ_SKIP_COUNT = "message.qos.read.skip";
    public static final String METRIC_MESSAGE_QOS_DELIVERY_SKIP_COUNT = "message.qos.delivery.skip";
    public static final String METRIC_MESSAGE_QOS_DOS_SKIP_COUNT = "message.qos.dos.skip";
    public static final String METRIC_MESSAGE_WATERMARK_SKIP_COUNT = "message.watermark.skip";
    public static final String METRIC_MESSAGE_QUEUE_FULL_SKIP = "message.qos.q.skip";

    public static final String METRIC_MESSAGE_QOS_NO_TIMESTAMP_COUNT = "message.qos.no.timestamp";
    public static final String METRIC_MESSAGE_AGE_AT_QOS_CHECK = "message.qos.age.seconds";
    public static final String METRIC_WRITE_ACTIVE_RATE_LIMIT = "message.write.active.rate.limit.seconds";
    public static final String METRIC_WRITE_ACTIVE_RATE_LIMIT_THRESHOLD = "message.write.active.rate.limit.threshold";
    public static final String METRIC_READ_ACTIVE_RATE_LIMIT = "message.read.active.rate.limit.seconds";
    public static final String METRIC_READ_ACTIVE_RATE_LIMIT_THRESHOLD = "message.read.active.rate.limit.threshold";
    public static final String METRIC_DOS_ACTIVE_RATE_LIMIT = "message.dos.active.rate.limit.seconds";
    public static final String METRIC_DOS_ACTIVE_RATE_LIMIT_THRESHOLD = "message.dos.active.rate.limit.threshold";
    public static final String METRIC_CLIENT_COUNT = "messaging.clients.connected";
    public static final String METRIC_MESSAGE_WRITE_LATENCY = "message.write.latency-ms";

    
    public static final String METRIC_FEDERATE_ROL_SKIP = "federation.rol.skip";
    public static final String METRIC_QOS_DELIVERY_CACHE_PUT_SKIP = "qos.delivery.cache.skip";
    
    public static final String METRIC_CLIENT_CONNECT = "client.connect";
    public static final String METRIC_CLIENT_DISCONNECT = "client.disconnect";
    
    public static final String METRIC_REPOSITORY_QUEUE_FULL_SKIP = "message.repository.q.skip";
    
    // Provenance keys used in Message
    public static final String PLUGIN_MANAGER_PROVENANCE = "PluginManager";
    public static final String PLUGIN_INTERCEPTOR_PROVENANCE = "PluginInterceptor";
    
}