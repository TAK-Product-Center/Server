package tak.server.plugins;

public abstract class PluginBase {

    protected PluginConfiguration config;

    public PluginBase() throws ReservedConfigurationException {
        config = new PluginConfiguration(this.getClass());
    }

}
