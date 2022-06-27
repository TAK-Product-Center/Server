package tak.server.plugins;

public interface PluginLifecycle {
	
	void start();
	void stop();
	
	void internalStart();
	void internalStop();
	
	PluginInfo getPluginInfo();
	void setPluginInfo(PluginInfo pluginInfo);
}
