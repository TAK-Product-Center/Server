package tak.server.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class PluginBase implements PluginLifecycle {

    private static final Logger logger = LoggerFactory.getLogger(PluginBase.class);

    protected PluginConfiguration config;

    protected PluginInfo pluginInfo;

    @Autowired
    protected SystemInfoApi systemInfoApi;

    @Autowired
    private PluginApi pluginApi;

    @Autowired
    private PluginSelfStopApi pluginSelfStopApi;

    @Autowired
    protected PluginMissionApi pluginMissionApi;

    @Autowired
    protected PluginFileApi pluginFileApi;

    @Autowired
    protected PluginCoreConfigApi coreConfigApi;

    public PluginBase() throws ReservedConfigurationException {
        config = new PluginConfiguration(getClass());
    }

    @Override
    public final void internalStart() {
        start();
        pluginInfo.setStarted(true);
    }

    @Override
    public final void internalStop() {
        try {
            stop();
            pluginInfo.setStarted(false);
        } catch (AbstractMethodError e) {
            logger.info("Recompile plugin {} with TAK Server Plugin SDK version 4.2 or higher to support the stop method.",
                    getPluginInfo().getName());
        }
    }

    @Override
    public final PluginInfo getPluginInfo() {
        return pluginInfo;
    }

    @Override
    public final void setPluginInfo(PluginInfo pluginInfo) {
        this.pluginInfo = pluginInfo;
    }

    @Override
    public final void selfStop() {

        logger.info("Calling selfStop for plugin {}", pluginInfo.getName());
        pluginSelfStopApi.pluginSelfStop(pluginInfo.getName());
        logger.info("Done calling selfStop for plugin {}", pluginInfo.getName());
    }

    /**
     * Re-reads and returns the contents of the plugin's configuration file.Useful if the contents of the updated
     * configuration file need to be validated. If not, use {@link #updatePluginConfiguration()}.
     * </br>
     * Example use case: A plugin has been stopped and upon being stopped, modified the configuration file. Some of this info
     * might be state data that helps the plugin resume from a specified state.
     *
     * @return Refreshed plugin configuration that will reflect any changes made to the configuration file.
     * @since 4.9
     */
    public PluginConfiguration createPluginConfiguration() {
        return config.reloadPluginConfiguration(getClass());
    }

    /**
     * Updates the plugin's configuration. Use if confident that the plugin configuration file contents do not have to be validated.
     * Otherwise use {@link #createPluginConfiguration()}
     *
     * @since 4.9
     */
    public void updatePluginConfiguration() {
        config = createPluginConfiguration();
    }
}
