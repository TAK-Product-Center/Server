

package tak.server.plugins.util;

import org.apache.ignite.Ignite;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.bbn.marti.remote.ServerInfo;

import tak.server.PluginRegistry;
import tak.server.plugins.PluginApi;
import tak.server.plugins.PluginStarter;

/*
 * 
 * Singleton that provides access to Spring context for non Spring-managed objects, and keeps references to other singleton services and utility classes.
 * 
 */
public class PluginManagerDependencyInjectionProxy implements ApplicationContextAware {
	private static ApplicationContext springContext;

	private volatile static PluginManagerDependencyInjectionProxy instance = null;

	public static PluginManagerDependencyInjectionProxy getInstance() {
		if (instance == null) {
			synchronized (PluginManagerDependencyInjectionProxy.class) {
				if (instance == null) {
					instance = springContext.getBean(PluginManagerDependencyInjectionProxy.class);
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

	private volatile Ignite ignite = null;

	public Ignite ignite() {
		if (ignite == null) {
			synchronized (this) {
				if (ignite == null) {
					ignite = springContext.getBean(Ignite.class);
				}
			}
		}

		return ignite;
	}
	
	private volatile PluginRegistry pluginRegistry = null;

	public PluginRegistry pluginRegistry() {
		if (pluginRegistry == null) {
			synchronized (this) {
				if (pluginRegistry == null) {
					pluginRegistry = springContext.getBean(PluginRegistry.class);
				}
			}
		}

		return pluginRegistry;
	}
	
	private volatile PluginStarter pluginStarter = null;

	public PluginStarter pluginStarter() {
		if (pluginStarter == null) {
			synchronized (this) {
				if (pluginStarter == null) {
					pluginStarter = springContext.getBean(PluginStarter.class);
				}
			}
		}

		return pluginStarter;
	}
	
	private volatile PluginApi pluginApi = null;

	public PluginApi pluginApi() {
		if (pluginApi == null) {
			synchronized (this) {
				if (pluginApi == null) {
					pluginApi = springContext.getBean(PluginApi.class);
				}
			}
		}

		return pluginApi;
	}
}
