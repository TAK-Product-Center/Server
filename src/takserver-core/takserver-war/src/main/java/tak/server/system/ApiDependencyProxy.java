package tak.server.system;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.bbn.marti.remote.ServerInfo;
import com.bbn.marti.sync.EnterpriseSyncService;
import com.bbn.marti.sync.repository.MissionRoleRepository;

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
	
}
