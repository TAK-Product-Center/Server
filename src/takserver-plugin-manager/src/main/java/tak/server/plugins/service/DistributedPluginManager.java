package tak.server.plugins.service;

import java.util.Collection;

import org.apache.ignite.services.Service;
import org.apache.ignite.services.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tak.server.PluginManager;
import tak.server.plugins.PluginInfo;
import tak.server.plugins.PluginLifecycle;
import tak.server.plugins.util.PluginManagerDependencyInjectionProxy;

public class DistributedPluginManager implements PluginManager, Service {
	
	private static final long serialVersionUID = 753070297094974770L;
	private static final Logger logger = LoggerFactory.getLogger(DistributedPluginManager.class);
    
	@Override
	public void cancel(ServiceContext ctx) {
		if (logger.isDebugEnabled()) {
			logger.debug(getClass().getSimpleName() + " service cancelled");
		}
	}

	@Override
	public void init(ServiceContext ctx) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("init method " + getClass().getSimpleName());
		}
	}

	@Override
	public void execute(ServiceContext ctx) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("execute method " + getClass().getSimpleName());
		}
	}

	@Override
	public Collection<PluginInfo> getAllPluginInfo() {
		return PluginManagerDependencyInjectionProxy.getInstance().pluginRegistry().getAllPluginInfo();
	}

	@Override
	public void startAllPlugins() {		
		getAllPlugins()
			.stream()
			.filter(plugin -> !plugin.getPluginInfo().isEnabled())
			.forEach(plugin -> plugin.internalStart());
	}

	@Override
	public void stopAllPlugins() { 
		getAllPlugins()
			.stream()
			.filter(plugin -> plugin.getPluginInfo().isEnabled())
			.forEach(plugin -> plugin.internalStop());
	}

	@Override
	public void startPluginByName(String name) {						
		getAllPlugins()
			.stream()
			.filter(plugin -> plugin.getPluginInfo().getName().equals(name))
			.filter(plugin -> !plugin.getPluginInfo().isEnabled())
			.forEach(plugin -> plugin.internalStart());
	}

	@Override
	public void stopPluginByName(String name) { 				
		getAllPlugins()
			.stream()
			.filter(plugin -> plugin.getPluginInfo().getName().equals(name))
			.filter(plugin -> plugin.getPluginInfo().isEnabled())
			.forEach(plugin -> plugin.internalStop());
	}
	
	protected Collection<PluginLifecycle> getAllPlugins() {
		return PluginManagerDependencyInjectionProxy.getInstance().pluginStarter().getAllPlugins();
	}
}
