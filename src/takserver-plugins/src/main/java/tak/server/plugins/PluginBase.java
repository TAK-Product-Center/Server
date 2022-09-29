package tak.server.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class PluginBase implements PluginLifecycle{

    private static final Logger logger = LoggerFactory.getLogger(PluginBase.class);

    protected PluginConfiguration config;
    
    protected PluginInfo pluginInfo;
    
    @Autowired
    protected SystemInfoApi systemInfoApi;
    
    @Autowired
	private PluginApi pluginApi;
    
    @Autowired
    private PluginSelfStopApi pluginSelfStopApi;

    public PluginBase() throws ReservedConfigurationException {
        config = new PluginConfiguration(this.getClass());
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
            logger.info("Recompile plugin " + getPluginInfo().getName() + " with TAK Server Plugin SDK version 4.2 or higher to support the stop method.");
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
}
