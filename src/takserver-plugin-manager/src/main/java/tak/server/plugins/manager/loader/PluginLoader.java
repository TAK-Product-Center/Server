package tak.server.plugins.manager.loader;

import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;

import tak.server.PluginRegistry;
import tak.server.plugins.MessageInterceptor;
import tak.server.plugins.MessageReceiver;
import tak.server.plugins.MessageSender;
import tak.server.plugins.MessageSenderReceiver;
import tak.server.plugins.MessageSenderReceiverBase;
import tak.server.plugins.PluginBase;
import tak.server.plugins.PluginInfo;
import tak.server.plugins.PluginsLoadedEvent;
import tak.server.plugins.TakServerPlugin;
import tak.server.plugins.TakServerPluginVersion;

public class PluginLoader {

	public PluginLoader(PluginRegistry registrar) {
		this.registrar = registrar;
	}

	private final PluginRegistry registrar;

	@Autowired
	GenericApplicationContext context;

	@Autowired
	private ApplicationEventPublisher applicationEventPublisher;

	private static final Logger logger = LoggerFactory.getLogger(PluginLoader.class);

	@EventListener(ContextRefreshedEvent.class)
	private void init() {

		logger.info("starting PluginLoader");

		// scan the classpath for plugins
		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);

		scanner.addIncludeFilter(new AnnotationTypeFilter(TakServerPlugin.class));

		for (BeanDefinition bd : scanner.findCandidateComponents("tak.server.plugins")) {

			String name = "";
			String description = "";

			PluginInfo pluginInfo = new PluginInfo();

			try {

				String pluginClassName = bd.getBeanClassName();

				logger.info("plugin class name: " + bd.getBeanClassName());

				Class<?> clazz = Class.forName(pluginClassName);

				TakServerPlugin pluginAnnotation = clazz.getAnnotation(TakServerPlugin.class);

				if (pluginAnnotation == null) {
					logger.warn("plugin " + bd.getBeanClassName() + " missing required annotation @TakServerPlugin - skipping");
					continue;
				}

				name = pluginAnnotation.name();
				description = pluginAnnotation.description();

				if (Strings.isNullOrEmpty(name)) {
					name = bd.getBeanClassName();
				}
				
				UUID id = UUID.randomUUID();

				pluginInfo.setName(name);
				pluginInfo.setDescription(description);
				pluginInfo.setClassName(clazz.getName());
				pluginInfo.setId(id); // TODO: unique id strategy - based on fully qualified class name?
				
				PluginSystemConfiguration pluginSytemConfiguration = new PluginSystemConfiguration(clazz);

				// get the plugin's isEnabled property from the Plugin Configuration file if it exists
				Boolean isEnabled = true;
				if (pluginSytemConfiguration.containsProperty(PluginSystemConfiguration.PLUGIN_ENABLED_PROPERTY)) {
					logger.debug("Plugin configuration contain {}", PluginSystemConfiguration.PLUGIN_ENABLED_PROPERTY);
					isEnabled = (Boolean)pluginSytemConfiguration.getProperty(PluginSystemConfiguration.PLUGIN_ENABLED_PROPERTY);
				}else {
					logger.debug("Plugin configuration does NOT contain {}", PluginSystemConfiguration.PLUGIN_ENABLED_PROPERTY);
				}
				pluginInfo.setEnabled(isEnabled);
				logger.info("Set isEnabled for plugin {} to {}", clazz, isEnabled);
				pluginInfo.setStarted(false); // Not yet started
				
				// get the archiveEnabled property from the Plugin Configuration file if it exists
				Boolean archiveEnabled = true;
				if (pluginSytemConfiguration.containsProperty(PluginSystemConfiguration.ARCHIVE_ENABLED_PROPERTY)) {
					logger.debug("Plugin configuration contain {}", PluginSystemConfiguration.ARCHIVE_ENABLED_PROPERTY);
					archiveEnabled = (Boolean)pluginSytemConfiguration.getProperty(PluginSystemConfiguration.ARCHIVE_ENABLED_PROPERTY);
				}else {
					logger.debug("Plugin configuration does NOT contain {}", PluginSystemConfiguration.ARCHIVE_ENABLED_PROPERTY);
				}
				pluginInfo.setArchiveEnabled(archiveEnabled); 
				logger.info("Set archiveEnabled for plugin {} to {}", clazz, archiveEnabled);
				
				// instantiate plugin
				Object pluginInstance = clazz.newInstance();
				
				// set version from file. this will be overridden if version annotations are found
				Integer major = null;
				Integer minor = null;
				Integer patch = 0;
				String hash = null;
				String tag = null;
				if (pluginInstance instanceof PluginBase) {
					try {
						String path = pluginInstance.getClass().asSubclass(pluginInstance.getClass()).getProtectionDomain()
								.getCodeSource().getLocation().getPath();
						String decodedPath = URLDecoder.decode(path, "UTF-8");
						URL url = loadResources("ver.json", decodedPath);

						Map<String, Object> result = new ObjectMapper().readValue(url.openStream(), HashMap.class);
						
						if (result.get("major") != null) {
							major = (Integer) result.get("major");
						}
						if (result.get("minor") != null) {
							minor = (Integer) result.get("minor");
						}
						if (result.get("patch") != null) {
							patch = (Integer) result.get("patch");
						}
						if (result.get("hash") != null) {
							hash = (String) result.get("hash");
						}	
						if (result.get("branch") != null) {
							tag = (String) result.get("branch");
						}
					} catch (Exception e) {
						logger.error("Could not load version file. Consider upgrding the plugin: " + name, e);
					}
				}
				
				TakServerPluginVersion pluginVersionAnnotation = clazz.getAnnotation(TakServerPluginVersion.class);
				if (pluginVersionAnnotation != null) {
					if (pluginVersionAnnotation.major() != -1) major = pluginVersionAnnotation.major();
					if (pluginVersionAnnotation.minor() != -1) minor = pluginVersionAnnotation.minor();
					if (pluginVersionAnnotation.patch() != -1) patch = pluginVersionAnnotation.patch();
					if (!"".equals(pluginVersionAnnotation.commitHash())) hash = pluginVersionAnnotation.commitHash();
					if (!"".equals(pluginVersionAnnotation.tag())) tag = pluginVersionAnnotation.tag();
				}
				
				if (major != null && minor != null && patch != null && hash != null) {
					pluginInfo.setVersion(major + "." + minor + "." + patch + "." + hash);
				}
				if (tag != null) {
					pluginInfo.setTag(tag);
				}
				
				if (pluginInstance instanceof MessageSenderReceiverBase) {
				    MessageSenderReceiver senderReceiverInstance = (MessageSenderReceiver) pluginInstance;

				    logger.info("sender-receiver plugin class instance " + senderReceiverInstance);
				    // register bean this way - need instance
				    context.registerBean(id.toString(), MessageSenderReceiver.class, () -> senderReceiverInstance);
				    pluginInfo.setSender(true);
				    pluginInfo.setReceiver(true);

				    senderReceiverInstance.setPluginInfo(pluginInfo);

					logger.info("Registered sender-receiver plugin instance: {}, name: {}", senderReceiverInstance, name);

				} else if (pluginInstance instanceof MessageSender) {

					MessageSender senderPluginInstance = (MessageSender) pluginInstance;

					logger.info("sender plugin class instance " + senderPluginInstance);

					// register bean this way - need instance
					context.registerBean(id.toString(), MessageSender.class, () -> senderPluginInstance);

					pluginInfo.setSender(true);

					senderPluginInstance.setPluginInfo(pluginInfo);

					logger.info("Registered sender plugin instance: {}, name: {}", senderPluginInstance, name);

				} else if (pluginInstance instanceof MessageReceiver) {

					MessageReceiver receiverPluginInstance = (MessageReceiver) pluginInstance;

					logger.info("receiver plugin class instance " + receiverPluginInstance);

					// register bean in app content
					context.registerBean(id.toString(), MessageReceiver.class, () -> receiverPluginInstance);

					pluginInfo.setReceiver(true);

					receiverPluginInstance.setPluginInfo(pluginInfo);

					logger.info("Registered receiver plugin instance: {}, name: {}", receiverPluginInstance, name);

				} else if (pluginInstance instanceof MessageInterceptor) {

					MessageInterceptor interceptorPluginInstance = (MessageInterceptor) pluginInstance;

					logger.info("interceptor plugin class instance " + interceptorPluginInstance);

					// register bean in app content
					context.registerBean(id.toString(), MessageInterceptor.class, () -> interceptorPluginInstance);

					pluginInfo.setInterceptor(true);

					interceptorPluginInstance.setPluginInfo(pluginInfo);

					logger.info("Registered interceptor plugin instance: {}, name: {}", interceptorPluginInstance, name);

				} else {
					logger.error("Skipping invalid plugin type " + pluginInstance.getClass().getName());
				}

			} catch (Exception e) {
				logger.warn("exception instantiating plugin", e);
				logger.info(e.getClass().getSimpleName());
				pluginInfo.setExceptionMessage(e.getClass().getSimpleName() + ": " + e.getMessage());
			}

			try  {
				registrar.register(pluginInfo);
			} catch (Exception e) {
				logger.error("Exception registering plugin " + pluginInfo);
			}
		}

		// get spring to set properties now
		applicationEventPublisher.publishEvent(new PluginsLoadedEvent(this, "plugins loaded"));
	}
	
	 public static URL loadResources(String name, String path) throws IOException {
	        final Enumeration<URL> systemResources = PluginBase.class.getClassLoader().getResources(name);
	        while (systemResources.hasMoreElements()) {
	        	URL url = systemResources.nextElement();
	        	if (url.getPath().toLowerCase().contains(path.toLowerCase())) {
	        		return url;
	        	}
	        }
			return null; 
	    }
}
