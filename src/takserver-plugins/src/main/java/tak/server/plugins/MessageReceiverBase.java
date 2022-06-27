package tak.server.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import atakmap.commoncommo.protobuf.v1.MessageOuterClass.Message;

public abstract class MessageReceiverBase extends PluginBase implements MessageReceiver {
	private static final Logger logger = LoggerFactory.getLogger(MessageReceiverBase.class);
	private PluginInfo pluginInfo;
	
    public MessageReceiverBase() throws ReservedConfigurationException {
        super();
    }

	@Override
	public final void internalStart() {
		start();
		pluginInfo.setEnabled(true);
	}

	@Override
	public final void internalStop() {
		try {
			stop();
			pluginInfo.setEnabled(false);
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
}
