

package com.bbn.marti.util;

import com.bbn.marti.network.PluginDataFeedJdbc;
import com.bbn.marti.remote.util.RemoteUtil;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;

import com.bbn.marti.groups.FileAuthenticator;
import com.bbn.marti.groups.GroupDao;
import com.bbn.marti.groups.GroupStore;
import com.bbn.marti.remote.ContactManager;
import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.remote.ServerInfo;
import com.bbn.marti.remote.SubscriptionManagerLite;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.repeater.RepeaterStore;
import com.bbn.marti.service.DistributedConfiguration;
import com.bbn.marti.service.SubmissionService;
import com.bbn.marti.service.SubscriptionStore;
import com.bbn.marti.sync.EnterpriseSyncService;
import com.bbn.marti.sync.repository.DataFeedRepository;
import com.bbn.marti.sync.repository.FederationEventRepository;
import com.bbn.marti.sync.repository.MissionRepository;
import com.bbn.marti.sync.repository.MissionRoleRepository;
import com.bbn.marti.sync.repository.MissionSubscriptionRepository;
import com.bbn.marti.sync.service.MissionService;

import tak.server.Constants;
import tak.server.cache.PluginDatafeedCacheHelper;
import tak.server.cluster.ClusterManager;
import tak.server.cot.CotEventContainer;
import tak.server.messaging.Messenger;
import tak.server.qos.MessageDOSStrategy;
import tak.server.qos.MessageDeliveryStrategy;
import tak.server.qos.MessageReadStrategy;
import tak.server.qos.QoSManager;

/*
 * 
 * Singleton that provides access to Spring context for non Spring-managed objects, and keeps references to other singleton services and utility classes.
 * 
 */
public class MessagingDependencyInjectionProxy implements ApplicationContextAware {
	private static ApplicationContext springContext;

	private static MessagingDependencyInjectionProxy instance = null;

	public static MessagingDependencyInjectionProxy getInstance() {
		if (instance == null) {
			synchronized (MessagingDependencyInjectionProxy.class) {
				if (instance == null) {
					instance = springContext.getBean(MessagingDependencyInjectionProxy.class);
				}
			}
		}

		return instance;
	}

	public static ApplicationContext getSpringContext() {
		return springContext;
	}

	@SuppressWarnings("static-access")
	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		this.springContext = context;
	}

	private GroupManager groupManager = null;

	public GroupManager groupManager() {
		if (groupManager == null) {
			synchronized (this) {
				if (groupManager == null) {
					groupManager = springContext.getBean(GroupManager.class);
				}
			}
		}

		return groupManager;
	}

	private GroupDao groupDao = null;

	public GroupDao groupDao() {
		if (groupDao == null) {
			synchronized (this) {
				if (groupDao == null) {
					groupDao = springContext.getBean(GroupDao.class);
				}
			}
		}

		return groupDao;
	}

	private GroupStore groupStore = null;

	public GroupStore groupStore() {
		if (groupStore == null) {
			synchronized (this) {
				if (groupStore == null) {
					groupStore = springContext.getBean(GroupStore.class);
				}
			}
		}

		return groupStore;
	}

	private DistributedConfiguration coreConfig = null;

	public DistributedConfiguration coreConfig() {
		if (coreConfig == null) {
			synchronized (this) {
				if (coreConfig == null) {
					coreConfig = springContext.getBean(DistributedConfiguration.class);
				}
			}
		}

		return coreConfig;
	}
	
	private CoreConfig simpleCoreConfig = null;

	public CoreConfig simpleCoreConfig() {
		if (simpleCoreConfig == null) {
			synchronized (this) {
				if (simpleCoreConfig == null) {
					simpleCoreConfig = springContext.getBean(CoreConfig.class);
				}
			}
		}

		return simpleCoreConfig;
	}

	private SubscriptionStore subStore = null;

	public SubscriptionStore subscriptionStore() {
		if (subStore == null) {
			synchronized (this) {
				if (subStore == null) {
					subStore = springContext.getBean(SubscriptionStore.class);
				}
			}
		}

		return subStore;
	}

	private MissionSubscriptionRepository msr = null;

	public MissionSubscriptionRepository missionSubscriptionRepository() {
		if (msr == null) {
			synchronized (this) {
				if (msr == null) {
					msr = springContext.getBean(MissionSubscriptionRepository.class);
				}
			}
		}

		return msr;
	}

	private SubmissionService submissionService = null;

	public SubmissionService submissionService() {
		if (submissionService == null) {
			synchronized (this) {
				if (submissionService == null) {
					submissionService = springContext.getBean(SubmissionService.class);
				}
			}
		}

		return submissionService;
	}

	private ContactManager contactManager = null;

	public ContactManager contactManager() {
		if (contactManager == null) {
			synchronized (this) {
				if (contactManager == null) {
					contactManager = springContext.getBean(ContactManager.class);
				}
			}
		}

		return contactManager;
	}

	private RepeaterStore repeaterStore = null;

	public RepeaterStore repeaterStore() {
		if (repeaterStore == null) {
			synchronized (this) {
				if (repeaterStore == null) {
					repeaterStore = springContext.getBean(RepeaterStore.class);
				}
			}
		}

		return repeaterStore;
	}
	
	private MissionService missionService = null;

	public MissionService missionService() {
		if (missionService == null) {
			synchronized (this) {
				if (missionService == null) {
					missionService = springContext.getBean(MissionService.class);
				}
			}
		}

		return missionService;
	}

	private FileAuthenticator fileAuthenticator = null;
	
	public FileAuthenticator fileAuthenticator() {
		if (fileAuthenticator  == null) {
			synchronized (this) {
				if (fileAuthenticator  == null) {
					fileAuthenticator = springContext.getBean(FileAuthenticator.class);
				}
			}
		}

		return fileAuthenticator;
	}
	
	private Messenger<CotEventContainer> cotMessenger = null;
	
	@SuppressWarnings("unchecked")
	public Messenger<CotEventContainer> cotMessenger() {
		if (cotMessenger  == null) {
			synchronized (this) {
				if (cotMessenger  == null) {
					cotMessenger = (Messenger<CotEventContainer>) springContext.getBean(Constants.DISTRIBUTED_COT_MESSENGER);
				}
			}
		}

		return cotMessenger;
	}
	
	private ClusterManager clusterManager = null;
	
	public ClusterManager clusterManager() {
		if (!coreConfig().getRemoteConfiguration().getCluster().isEnabled()) return null;
		
		if (clusterManager  == null) {
			synchronized (this) {
				if (clusterManager  == null) {
					clusterManager = (ClusterManager) springContext.getBean(ClusterManager.class);
				}
			}
		}

		return clusterManager;
	}
	
	private FederationEventRepository fedEventRepo = null;
	
	private Object fedEventRepoLock = new Object();
	
	public FederationEventRepository fedEventRepo() {
		
		if (fedEventRepo == null) {
			synchronized (fedEventRepoLock) {
				if (fedEventRepo  == null) {
					fedEventRepo = (FederationEventRepository) springContext.getBean(FederationEventRepository.class);
				}
			}
		}

		return fedEventRepo;
	}
	
	private ServerInfo serverInfo = null;
	
	public ServerInfo serverInfo() {
		
		if (serverInfo  == null) {
			synchronized (this) {
				if (serverInfo  == null) {
					serverInfo = (ServerInfo) springContext.getBean(ServerInfo.class);
				}
			}
		}

		return serverInfo;
	}
	
	private MessageDeliveryStrategy mds = null;

	public MessageDeliveryStrategy mds() {

		if (mds == null) {
			synchronized (this) {
				if (mds == null) {
					mds = (MessageDeliveryStrategy) springContext.getBean(MessageDeliveryStrategy.class);
				}
			}
		}

		return mds;
	}

	private MessageReadStrategy mrs = null;

	public MessageReadStrategy mrs() {

		if (mrs == null) {
			synchronized (this) {
				if (mrs == null) {
					mrs = (MessageReadStrategy) springContext.getBean(MessageReadStrategy.class);
				}
			}
		}

		return mrs;
	}
	
	private MessageDOSStrategy mdoss = null;

	public MessageDOSStrategy mdoss() {

		if (mdoss == null) {
			synchronized (this) {
				if (mdoss == null) {
					mdoss = (MessageDOSStrategy) springContext.getBean(MessageDOSStrategy.class);
				}
			}
		}

		return mdoss;
	}
	
	private VersionBean vb = null;
	
	public VersionBean versionBean() {
		
		if (vb == null) {
			synchronized (this) {
				if (vb == null) {
					vb = (VersionBean) springContext.getBean(VersionBean.class);
				}
			}
		}
		
		return vb;
	}
	
	private QoSManager qm = null;
	
	public QoSManager qosManager() {
		
		if (qm == null) {
			synchronized (this) {
				if (qm == null) {
					qm = (QoSManager) springContext.getBean(QoSManager.class);
				}
			}
		}
		
		return qm;
	}
	
	@Autowired
	private ApplicationEventPublisher aep;
	
	public ApplicationEventPublisher eventPublisher() {
		
		return aep;
	}
	
	private DataFeedRepository dataFeedRepository = null;

	public DataFeedRepository dataFeedRepository() {
		if (dataFeedRepository == null) {
			synchronized (this) {
				if (dataFeedRepository == null) {
					dataFeedRepository = springContext.getBean(DataFeedRepository.class);
				}
			}
		}

		return dataFeedRepository;
	}
	
	private EnterpriseSyncService esyncService = null;

	public EnterpriseSyncService esyncService() {
		if (esyncService == null) {
			synchronized (this) {
				if (esyncService == null) {
					esyncService = springContext.getBean(EnterpriseSyncService.class);
				}
			}
		}
		return esyncService;
	}
					
	private MissionRepository missionRepository = null;
	
	public MissionRepository missionRepository() {
		if (missionRepository == null) {
			synchronized (this) {
				if (missionRepository == null) {
					missionRepository = springContext.getBean(MissionRepository.class);
				}
			}
		}

		return missionRepository;
	}
	
	private SubscriptionManagerLite subscriptionManagerLite = null;
	
	public SubscriptionManagerLite subscriptionManagerLite() {
		if (subscriptionManagerLite == null) {
			synchronized (this) {
				if (subscriptionManagerLite == null) {
					subscriptionManagerLite = springContext.getBean(SubscriptionManagerLite.class);
				}
			}
		}

		return subscriptionManagerLite;
	}
	
	private MissionRoleRepository missionRoleRepository = null;
	
	public MissionRoleRepository missionRoleRepository() {
		if (missionRoleRepository == null) {
			synchronized (this) {
				if (missionRoleRepository == null) {
					missionRoleRepository = springContext.getBean(MissionRoleRepository.class);
				}
			}
		}
		return missionRoleRepository;
	}

	private PluginDatafeedCacheHelper pluginDatafeedCacheHelper = null;

	public PluginDatafeedCacheHelper pluginDatafeedCacheHelper() {
		if (pluginDatafeedCacheHelper == null) {
			synchronized (this) {
				if (pluginDatafeedCacheHelper == null) {
					pluginDatafeedCacheHelper = springContext.getBean(PluginDatafeedCacheHelper.class);
				}
			}
		}
		return pluginDatafeedCacheHelper;
	}

	private PluginDataFeedJdbc pluginDataFeedJdbc = null;

	public PluginDataFeedJdbc pluginDataFeedJdbc() {
		if (pluginDataFeedJdbc == null) {
			synchronized (this) {
				if (pluginDataFeedJdbc == null) {
					pluginDataFeedJdbc = springContext.getBean(PluginDataFeedJdbc.class);
				}
			}
		}
		return pluginDataFeedJdbc;
	}

	private RemoteUtil remoteUtil = null;

	public RemoteUtil remoteUtil() {
		if (remoteUtil == null) {
			synchronized (this) {
				if (remoteUtil == null) {
					remoteUtil = springContext.getBean(RemoteUtil.class);
				}
			}
		}
		return remoteUtil;
	}
}
