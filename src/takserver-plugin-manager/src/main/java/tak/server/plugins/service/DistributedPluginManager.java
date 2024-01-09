package tak.server.plugins.service;

import com.bbn.marti.sync.Metadata;
import com.google.common.base.Strings;
import org.apache.ignite.services.Service;
import org.apache.ignite.services.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tak.server.PluginManager;
import tak.server.plugins.MessageInterceptorBase;
import tak.server.plugins.PluginInfo;
import tak.server.plugins.PluginLifecycle;
import tak.server.plugins.PluginResponse;
import tak.server.plugins.manager.loader.PluginSystemConfiguration;
import tak.server.plugins.util.PluginManagerDependencyInjectionProxy;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
                .filter(plugin -> !plugin.getPluginInfo().isStarted())
                .forEach(plugin -> {
                    plugin.internalStart();
					if (plugin instanceof MessageInterceptorBase) {
						PluginManagerDependencyInjectionProxy.getInstance().pluginApi().addInterceptorPluginsActive(1);
					}
                });
    }

    @Override
    public void stopAllPlugins() {
        getAllPlugins()
                .stream()
                .filter(plugin -> plugin.getPluginInfo().isStarted())
                .forEach(plugin -> {
                    plugin.internalStop();
					if (plugin instanceof MessageInterceptorBase) {
						PluginManagerDependencyInjectionProxy.getInstance().pluginApi().addInterceptorPluginsActive(-1);
					}
                });
    }

    @Override
    public void startPluginByName(String name) {
        getAllPlugins()
                .stream()
                .filter(plugin -> plugin.getPluginInfo().getName().equals(name))
                .filter(plugin -> !plugin.getPluginInfo().isStarted())
                .forEach(plugin -> {
                    plugin.internalStart();
					if (plugin instanceof MessageInterceptorBase) {
						PluginManagerDependencyInjectionProxy.getInstance().pluginApi().addInterceptorPluginsActive(1);
					}
                });
    }

    @Override
    public void stopPluginByName(String name) {
        getAllPlugins()
                .stream()
                .filter(plugin -> plugin.getPluginInfo().getName().equals(name))
                .filter(plugin -> plugin.getPluginInfo().isStarted())
                .forEach(plugin -> {
                    plugin.internalStop();
					if (plugin instanceof MessageInterceptorBase) {
						PluginManagerDependencyInjectionProxy.getInstance().pluginApi().addInterceptorPluginsActive(-1);
					}
                });
    }

    @Override
    public void setPluginEnabled(String name, boolean isPluginEnabled) {
        getAllPlugins()
                .stream()
                .filter(plugin -> plugin.getPluginInfo().getName().equals(name))
                .filter(plugin -> plugin.getPluginInfo().isEnabled() != isPluginEnabled)
                .forEach(plugin -> {
                    PluginInfo pluginInfo = plugin.getPluginInfo();
                    pluginInfo.setEnabled(isPluginEnabled);

                    persistPluginEnabledPropertyInPluginConfigurationFile(pluginInfo, isPluginEnabled);
                });
    }

    @Override
    public void setPluginArchive(String name, boolean isArchiveEnabled) {
        getAllPlugins()
                .stream()
                .filter(plugin -> plugin.getPluginInfo().getName().equals(name))
                .filter(plugin -> plugin.getPluginInfo().isArchiveEnabled() != isArchiveEnabled)
                .forEach(plugin -> {
                    PluginInfo pluginInfo = plugin.getPluginInfo();
                    pluginInfo.setArchiveEnabled(isArchiveEnabled);

                    persistPluginArchiveEnabledPropertyInPluginConfigurationFile(pluginInfo, isArchiveEnabled);
                });
    }

    protected Collection<PluginLifecycle> getAllPlugins() {
        return PluginManagerDependencyInjectionProxy.getInstance().pluginStarter().getAllPlugins();
    }

    @Override
    public void submitDataToPlugin(String pluginClassName, Map<String, String> allRequestParams, String data, String contentType) {

        if (Strings.isNullOrEmpty(pluginClassName)) {
            throw new IllegalArgumentException("plugin class name is empty");
        }

        if (data == null) {
            throw new IllegalArgumentException("null data can't be submitted to plugin");
        }

        if (Strings.isNullOrEmpty(contentType)) {
            throw new IllegalArgumentException("content type must be specified");
        }

        AtomicInteger pluginDataSubmitCounter = new AtomicInteger();

        // submit the data to any plugin matching the class name
        getAllPlugins()
                .stream()
                .filter(plugin -> plugin.getPluginInfo().getClassName().equals(pluginClassName))
                .forEach(plugin -> {
                    plugin.onSubmitData(allRequestParams, data, contentType);
                    pluginDataSubmitCounter.incrementAndGet();
                });

        if (pluginDataSubmitCounter.get() == 0) {
            throw new IllegalArgumentException("no plugin with class name " + pluginClassName + " is currently installed.");
        }
    }

    @Override
    public PluginResponse submitDataToPluginWithResult(String pluginClassName, Map<String, String> allRequestParams, String data, String contentType) {
        if (Strings.isNullOrEmpty(pluginClassName)) {
            throw new IllegalArgumentException("plugin class name is empty");
        }

        if (data == null) {
            throw new IllegalArgumentException("null data can't be submitted to plugin");
        }

        if (Strings.isNullOrEmpty(contentType)) {
            throw new IllegalArgumentException("content type must be specified");
        }

        AtomicInteger pluginDataSubmitCounter = new AtomicInteger();

        // submit the data to any plugin matching the class name
        List<PluginLifecycle> plugins = getAllPlugins()
                .stream()
                .filter(plugin -> plugin.getPluginInfo().getClassName().equals(pluginClassName))
                .collect(Collectors.toList());

        PluginResponse result = new PluginResponse();

        for (PluginLifecycle plugin : plugins) {
            result = plugin.onSubmitDataWithResult(allRequestParams, data, contentType);
            pluginDataSubmitCounter.incrementAndGet();
        }

        if (pluginDataSubmitCounter.get() == 0) {
            throw new IllegalArgumentException("no plugin with class name " + pluginClassName + " is currently installed.");
        }

        if (pluginDataSubmitCounter.get() != 1) {
            throw new IllegalArgumentException("multiple plugins with class name " + pluginClassName);
        }

        return result;
    }

    @Override
    public void updateDataInPlugin(String pluginClassName, Map<String, String> allRequestParams, String data, String contentType) {
        if (Strings.isNullOrEmpty(pluginClassName)) {
            throw new IllegalArgumentException("plugin class name is empty");
        }

        if (data == null) {
            throw new IllegalArgumentException("null data can't be updated in plugin");
        }

        if (Strings.isNullOrEmpty(contentType)) {
            throw new IllegalArgumentException("content type must be specified");
        }

        AtomicInteger pluginDataUpdateCounter = new AtomicInteger();

        // update the data in any plugin matching the class name
        getAllPlugins()
                .stream()
                .filter(plugin -> plugin.getPluginInfo().getClassName().equals(pluginClassName))
                .forEach(plugin -> {
                    plugin.onUpdateData(allRequestParams, data, contentType);
                    pluginDataUpdateCounter.incrementAndGet();
                });

        if (pluginDataUpdateCounter.get() == 0) {
            throw new IllegalArgumentException("no plugin with class name " + pluginClassName + " is currently installed.");
        }
    }

    @Override
    public PluginResponse requestDataFromPlugin(String pluginClassName, Map<String, String> allRequestParams, String contentType) {
        if (Strings.isNullOrEmpty(pluginClassName)) {
            throw new IllegalArgumentException("plugin class name is empty");
        }

        if (Strings.isNullOrEmpty(contentType)) {
            throw new IllegalArgumentException("content type must be specified");
        }

        AtomicInteger pluginDataRequestCounter = new AtomicInteger();

        // submit the data to any plugin matching the class name
        List<PluginLifecycle> plugins = getAllPlugins()
                .stream()
                .filter(plugin -> plugin.getPluginInfo().getClassName().equals(pluginClassName))
                .collect(Collectors.toList());

        PluginResponse result = new PluginResponse();
        for (PluginLifecycle plugin : plugins) {
            result = plugin.onRequestData(allRequestParams, contentType);
            pluginDataRequestCounter.incrementAndGet();
        }

        if (pluginDataRequestCounter.get() == 0) {
            throw new IllegalArgumentException("no plugin with class name " + pluginClassName + " is currently installed.");
        }

        if (pluginDataRequestCounter.get() != 1) {
            throw new IllegalArgumentException("multiple plugins with class name " + pluginClassName);
        }

        return result;
    }

    @Override
    public void deleteDataFromPlugin(String pluginClassName, Map<String, String> allRequestParams, String contentType) {
        if (Strings.isNullOrEmpty(pluginClassName)) {
            throw new IllegalArgumentException("plugin class name is empty");
        }

        if (Strings.isNullOrEmpty(contentType)) {
            throw new IllegalArgumentException("content type must be specified");
        }

        AtomicInteger pluginDataDeleteCounter = new AtomicInteger();

        // delete data from any plugin matching the class name
        getAllPlugins()
                .stream()
                .filter(plugin -> plugin.getPluginInfo().getClassName().equals(pluginClassName))
                .forEach(plugin -> {
                    plugin.onDeleteData(allRequestParams, contentType);
                    pluginDataDeleteCounter.incrementAndGet();
                });

        if (pluginDataDeleteCounter.get() == 0) {
            throw new IllegalArgumentException("no plugin with class name " + pluginClassName + " is currently installed.");
        }
    }

    private synchronized void persistPluginEnabledPropertyInPluginConfigurationFile(PluginInfo pluginInfo, boolean isEnabled) {

        try {
            // persist the configuration
            Class<?> clazz = Class.forName(pluginInfo.getClassName());
            PluginSystemConfiguration pluginSystemConfiguration = new PluginSystemConfiguration(clazz);
            pluginSystemConfiguration.setProperty(PluginSystemConfiguration.PLUGIN_ENABLED_PROPERTY, isEnabled);
            pluginSystemConfiguration.save(clazz);
        } catch (Exception e) {
            logger.error("Error in persisting plugin configuration", e);
        }
    }

    private synchronized void persistPluginArchiveEnabledPropertyInPluginConfigurationFile(PluginInfo pluginInfo, boolean isArchiveEnabled) {

        try {
            // persist the configuration
            Class<?> clazz = Class.forName(pluginInfo.getClassName());
            PluginSystemConfiguration pluginSystemConfiguration = new PluginSystemConfiguration(clazz);
            pluginSystemConfiguration.setProperty(PluginSystemConfiguration.ARCHIVE_ENABLED_PROPERTY, isArchiveEnabled);
            pluginSystemConfiguration.save(clazz);
        } catch (Exception e) {
            logger.error("Error in persisting plugin configuration", e);
        }
    }

    @Override
    public void onFileUpload(String pluginClassName, Metadata metadata) {

        AtomicInteger counter = new AtomicInteger();

        Stream<PluginLifecycle> plugins = getAllPlugins().stream();

        // if pluginClassName is provided, only notify plugins matching the class name
        if (pluginClassName != null) {
            plugins = plugins.filter(plugin -> plugin.getPluginInfo().getClassName().equals(pluginClassName));
        }

        plugins.forEach(plugin -> {
            if (plugin.getPluginInfo().isStarted()) {
                plugin.onFileUploadEvent(metadata);
            } else {
                logger.error("Plugin {} is not started", plugin.getPluginInfo().getClassName());
            }
            counter.incrementAndGet();
        });

        if (pluginClassName != null && counter.get() == 0) {
            throw new IllegalArgumentException("no plugin with class name " + pluginClassName + " is currently installed.");
        }
    }
}
