package tak.server.plugins.service;

import java.util.Collection;

import javax.cache.event.CacheEntryEvent;

import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.query.ContinuousQuery;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.services.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;

import tak.server.CommonConstants;
import tak.server.plugins.PluginInfo;
import tak.server.plugins.PluginsStartedEvent;
import tak.server.plugins.util.PluginManagerDependencyInjectionProxy;

/**
 */
public class DistributedClusterPluginManager extends DistributedPluginManager {
	
	private static final long serialVersionUID = 753070297094974770L;
	private static final Logger logger = LoggerFactory.getLogger(DistributedClusterPluginManager.class);
		
	private IgniteCache<String, Boolean> pluginCache;
    private ContinuousQuery<String, Boolean> continuousPluginQuery = new ContinuousQuery<>();

    @EventListener({ PluginsStartedEvent.class })
    private void initCache() {
    	// initialize plugins only if they arent in the cache    		
    	getAllPlugins()
    		.stream()
    		.forEach(plugin -> getPluginCache().putIfAbsent(plugin.getPluginInfo().getName(), new Boolean(true)));
    	
    	// pull the most up to date plugin status from cache and set them
    	getAllPlugins()
			.stream()
			.forEach(plugin -> {
				Boolean status = getPluginCache().get(plugin.getPluginInfo().getName());
				plugin.getPluginInfo().setEnabled(status.booleanValue());
				
				if (!status.booleanValue()) {
					plugin.internalStop();
					// the stop failed (likely due to incompatibility - so re-cache as running)
					if (plugin.getPluginInfo().isEnabled()) {
						updateCache(plugin.getPluginInfo().getName(), true);
					}
				}
			});
    	
    	// listen for plugin cache changes and make local updates as needed
    	continuousPluginQuery.setLocalListener((evts) -> {
   	     	for (CacheEntryEvent<? extends String, ? extends Boolean> e : evts) {
   	     		getAllPlugins()
   	     			.stream()
   	     			// plugin matches   	     			
   	     			.filter(plugin -> plugin.getPluginInfo().getName().equals(e.getKey()))
   	     			// make sure we are only setting a new status if its not already set
   	     			.filter(plugin -> plugin.getPluginInfo().isEnabled() != e.getValue().booleanValue())
   	     			.forEach(plugin -> {
   	     				if (e.getValue().booleanValue()) {
   	     					plugin.internalStart();
   	     				} else {
   	     					plugin.internalStop();
   	     					// the stop failed (likely due to incompatibility - so re-cache as running)
   	     					if (plugin.getPluginInfo().isEnabled()) {
   								updateCache(plugin.getPluginInfo().getName(), true);
   							}
   	     				}	
   	     			});
   	     	}
     	 });
   	
		getPluginCache().query(continuousPluginQuery);
    }
    
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
			.forEach(plugin -> updateCache(plugin.getPluginInfo().getName(), true));
	}

	@Override
	public void stopAllPlugins() { 
		getAllPlugins()
			.stream()
			.forEach(plugin -> updateCache(plugin.getPluginInfo().getName(), false));
	}

	@Override
	public void startPluginByName(String name) {						
		updateCache(name, true);
	}

	@Override
	public void stopPluginByName(String name) { 				
		updateCache(name, false);
	}
	
	private IgniteCache<String, Boolean> getPluginCache() {

		if (pluginCache == null) {
			CacheConfiguration<String, Boolean> cfg = new CacheConfiguration<String, Boolean>();
			
			cfg.setName(CommonConstants.PLUGIN_CACHE);
			cfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);				
			pluginCache = PluginManagerDependencyInjectionProxy.getInstance().ignite().getOrCreateCache(cfg);
		}
		
		return pluginCache;
	}
	
	private void updateCache(String name, boolean status) {
		getPluginCache().put(name, status);
	}
}
