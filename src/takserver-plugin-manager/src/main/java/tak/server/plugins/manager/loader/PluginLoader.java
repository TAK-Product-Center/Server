package tak.server.plugins.manager.loader;

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

import com.google.common.base.Strings;

import tak.server.PluginRegistry;
import tak.server.plugins.MessageInterceptor;
import tak.server.plugins.MessageReceiver;
import tak.server.plugins.MessageSender;
import tak.server.plugins.MessageSenderReceiver;
import tak.server.plugins.MessageSenderReceiverBase;
import tak.server.plugins.PluginInfo;
import tak.server.plugins.PluginsLoadedEvent;
import tak.server.plugins.TakServerPlugin;

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

				// instantiate plugin
				Object pluginInstance = clazz.newInstance();

				if (pluginInstance instanceof MessageSenderReceiverBase) {
				    MessageSenderReceiver senderReceiverInstance = (MessageSenderReceiver) pluginInstance;

				    logger.info("sender-receiver plugin class instance " + senderReceiverInstance);
				    // register bean this way - need instance
				    context.registerBean(id.toString(), MessageSenderReceiver.class, () -> senderReceiverInstance);
				    pluginInfo.setSender(true);
				    pluginInfo.setReceiver(true);
				    pluginInfo.setEnabled(true);

				    logger.info("registered sender-receiver plugin " + senderReceiverInstance + " name: " + name + " description: " + description);
				    senderReceiverInstance.setPluginInfo(pluginInfo);

				} else if (pluginInstance instanceof MessageSender) {

					MessageSender senderPluginInstance = (MessageSender) pluginInstance;

					logger.info("sender plugin class instance " + senderPluginInstance);

					// register bean this way - need instance
					context.registerBean(id.toString(), MessageSender.class, () -> senderPluginInstance);

					pluginInfo.setSender(true);
					pluginInfo.setEnabled(true);

					senderPluginInstance.setPluginInfo(pluginInfo);

					logger.info("registered sender plugin " + senderPluginInstance + " name: " + name + " description" + description);

				} else if (pluginInstance instanceof MessageReceiver) {

					MessageReceiver receiverPluginInstance = (MessageReceiver) pluginInstance;

					logger.info("receiver plugin class instance " + receiverPluginInstance);

					// register bean in app content
					context.registerBean(id.toString(), MessageReceiver.class, () -> receiverPluginInstance);

					pluginInfo.setReceiver(true);
					pluginInfo.setEnabled(true);

					receiverPluginInstance.setPluginInfo(pluginInfo);

					logger.info("registered receiver plugin " + receiverPluginInstance);

				} else if (pluginInstance instanceof MessageInterceptor) {

					MessageInterceptor interceptorPluginInstance = (MessageInterceptor) pluginInstance;

					logger.info("interceptor plugin class instance " + interceptorPluginInstance);

					// register bean in app content
					context.registerBean(id.toString(), MessageInterceptor.class, () -> interceptorPluginInstance);

					pluginInfo.setInterceptor(true);
					pluginInfo.setEnabled(true);

					interceptorPluginInstance.setPluginInfo(pluginInfo);

					logger.info("registered interceptor plugin " + interceptorPluginInstance);

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
}
