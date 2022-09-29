package tak.server.system;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.bbn.marti.remote.ServerInfo;
import com.bbn.marti.remote.SubscriptionManagerLite;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.sync.EnterpriseSyncService;
import com.bbn.marti.sync.repository.MissionRepository;
import com.bbn.marti.sync.repository.MissionRoleRepository;
import com.bbn.marti.sync.service.MissionService;
import com.bbn.marti.util.CommonUtil;

import tak.server.PluginManager;
import tak.server.cache.CoTCacheHelper;

public class ApiDependencyProxy implements ApplicationContextAware {
	
	
	private static ApplicationContext springContext;

	private static ApiDependencyProxy instance = null;

	public static ApiDependencyProxy getInstance() {
		if (instance == null) {
			synchronized (ApiDependencyProxy.class) {
				if (instance == null) {
					instance = springContext.getBean(ApiDependencyProxy.class);
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

	private ServerInfo serverInfo = null;

	public ServerInfo serverInfo() {
		if (serverInfo == null) {
			synchronized (this) {
				if (serverInfo == null) {
					serverInfo = springContext.getBean(ServerInfo.class);
				}
			}
		}

		return serverInfo;
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

	private EnterpriseSyncService enterpriseSyncService = null;

	public EnterpriseSyncService enterpriseSyncService() {
		if (enterpriseSyncService == null) {
			synchronized (this) {
				if (enterpriseSyncService == null) {
					enterpriseSyncService = springContext.getBean(EnterpriseSyncService.class);
				}
			}
		}

		return enterpriseSyncService;
	}
	
	private CoTCacheHelper cotCacheHelper = null;

	public CoTCacheHelper cotCacheHelper() {
		if (cotCacheHelper == null) {
			synchronized (this) {
				if (cotCacheHelper == null) {
					cotCacheHelper = springContext.getBean(CoTCacheHelper.class);
				}
			}
		}

		return cotCacheHelper;
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
	
	private PluginManager pluginManager = null;

	public PluginManager pluginManager() {
		if (pluginManager == null) {
			synchronized (this) {
				if (pluginManager == null) {
					pluginManager = springContext.getBean(PluginManager.class);
				}
			}
		}

		return pluginManager;
	}
	
	private CommonUtil commonUtil = null;

	public CommonUtil commonUtil() {
		if (commonUtil == null) {
			synchronized (this) {
				if (commonUtil == null) {
					commonUtil = springContext.getBean(CommonUtil.class);
				}
			}
		}

		return commonUtil;
	}
}
