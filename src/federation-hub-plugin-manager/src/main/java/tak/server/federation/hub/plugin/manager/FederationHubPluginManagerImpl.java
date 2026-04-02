package tak.server.federation.hub.plugin.manager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map.Entry;
import java.util.Objects;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.cluster.ClusterGroup;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.events.DiscoveryEvent;
import org.apache.ignite.events.EventType;
import org.apache.ignite.lang.IgnitePredicate;
import org.apache.ignite.services.Service;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import tak.server.federation.FederationException;
import tak.server.federation.hub.plugin.FederationHubPluginMetadata;
import tak.server.federation.hub.plugin.FederationHubPluginRegistry;

public class FederationHubPluginManagerImpl implements FederationHubPluginManager, Service {
	private static final Logger logger = LoggerFactory.getLogger(FederationHubPluginManagerImpl.class);
	
	private Ignite ignite;
	
	private final String configPath;
	
	private PluginManagerConfig config;
	
	public FederationHubPluginManagerImpl(Ignite ignite, String configPath) {
		this.ignite = ignite;
		this.configPath = configPath;
	}
	
	@Override
	public void cancel() {
		if (logger.isDebugEnabled()) {
			logger.debug("cancel() in " + getClass().getName());
		}
	}

	@Override
	public void init() throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("init() in " + getClass().getName());
		}
	}

	@Override
	public void execute() throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("execute() in " + getClass().getName());
		}
		
//		try {
//			this.config = loadConfig();
//		} catch (Exception e) {
//			logger.error("Could not load plugin lifecycle config file " + configPath);
//		}
//		
//		if (this.config == null) {
//			logger.info("Generating new plugin lifecycle config file at " + configPath);
//			this.config = new PluginManagerConfig();
//			savePluginManagerConfig();
//		}
		
		// clear any cache state on restart
		FederationHubPluginRegistry.clearCache(ignite);
		
		IgnitePredicate<DiscoveryEvent> ignitePredicate = new IgnitePredicate<DiscoveryEvent>() {
			@Override
			public boolean apply(DiscoveryEvent event) {				
				String pluginName = event.eventNode().attribute("plugin-name");
				logger.info("Node discovery detected " + pluginName + " has been shut down.");
				
				FederationHubPluginMetadata metadata = FederationHubPluginRegistry.registrationCache(ignite).get(pluginName);
				if (metadata != null) {
					logger.info("Removing registration for "  + metadata);
				}
				
				FederationHubPluginRegistry.registrationCache(ignite).remove(pluginName);
				return true;
			}
		};
		ignite.events().localListen(ignitePredicate, EventType.EVT_NODE_LEFT, EventType.EVT_NODE_FAILED);
	}

	private PluginManagerConfig loadConfig() throws Exception {
        if (getClass().getResource(configPath) != null) {
            // It's a resource.
        	try (InputStream is = getClass().getResourceAsStream(configPath)) {
                return new ObjectMapper(new YAMLFactory()).readValue(is, PluginManagerConfig.class);
        	}
        }

        // It's a file.
        try (InputStream is = new FileInputStream(configPath)) {
        	 return new ObjectMapper(new YAMLFactory()).readValue(is, PluginManagerConfig.class);
        }
    }
	
	private void savePluginManagerConfig() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
        	mapper.writerWithDefaultPrettyPrinter().writeValue(new File(configPath), config);            
        } catch (IOException e) {
            logger.error("Could not write policy to file", e);
        }
	}
	
	@Override
	public synchronized FederationException registerPlugin(FederationHubPluginMetadata metadata) {
		String name = metadata.getName();
		
		// we can't have two plugins with the same name
		if (FederationHubPluginRegistry.registrationCache(ignite).containsKey(name)) {
			return new FederationException("A plugin is already registered to this name!");
		}
		
		// updates disk and cache
		updatePluginConfig(name, metadata);
		
		logger.info("Plugin Registered! " + metadata);
		
		return null;
	}
	
//	@Override
//	public synchronized void startPlugin(String name) {
//		// fail safe just incase
//		stopPlugin(name);
//		
//		FederationHubPluginMetadata pluginConfig = config.getPlugins().get(name);
//		if (pluginConfig.isDisabled()) {
//			logger.info(name + " is disabled. Plugin will not be started.");
//			return;
//		}
//		
//		if (Strings.isBlank(pluginConfig.getStartCommand())) {
//			logger.info(name + " has no startup command. Plugin will not be started.");
//			return;
//		}
//		
//		try {			
//			ProcessBuilder pb;
//
//            if (isWindows()) {
//                pb = new ProcessBuilder("cmd", "/c", pluginConfig.getStartCommand());
//            } else {
//                pb = new ProcessBuilder("bash", "-c", pluginConfig.getStartCommand());
//            }
//
//            // Don't let stdout/stderr fill up buffers (since app logs itself)
//            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
//            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
//
//            pb.start();
//            
//            logger.info(name + " start command executed.");
//		} catch (Exception e) {
//			logger.error("Error executing plugin startup command for " + name);
//		}
//	}
//
//	@Override
//	public synchronized void stopPlugin(String name) {
//		try {			
//			ClusterGroup runningPlugin = ignite.cluster().forPredicate(node -> {
//				return Objects.equals(node.attribute("plugin-name"), name);
//			});
//
//			if (runningPlugin.nodes().size() > 0) {
//				for (ClusterNode node : runningPlugin.nodes()) {
//					ignite.compute(ignite.cluster().forNode(node)).run(() -> {
//						Ignition.stop(true);
//					});
//				}
//			}
//						
//			logger.info("Attempting to stop ignite programmatically for " + name);
//		} catch (Exception e) {
//			logger.error("Error shutting down plugin ignite node for " + name);
//		}
//	}
//
//	@Override
//	public synchronized void disablePlugin(String name) {
//		// stop plugin if running
//		stopPlugin(name);
//		// update status
//		FederationHubPluginMetadata pluginConfig = config.getPlugins().get(name);
//		pluginConfig.setDisabled(true);
//		
//		// updates disk and cache
//		updatePluginConfig(name, pluginConfig);	
//	}
	
	private void updatePluginConfig(String name, FederationHubPluginMetadata metadata) {
		// update config locally
		//config.getPlugins().put(name, metadata);

		// update config on disk
		//savePluginManagerConfig();

		// update both caches
		FederationHubPluginRegistry.registrationCache(ignite).put(name, metadata);
	}
	
	private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}
