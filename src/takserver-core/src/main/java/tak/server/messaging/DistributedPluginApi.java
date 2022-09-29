package tak.server.messaging;

import org.apache.ignite.events.DiscoveryEvent;
import org.apache.ignite.events.EventType;
import org.apache.ignite.lang.IgnitePredicate;
import org.apache.ignite.services.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.bbn.marti.remote.exception.TakException;
import com.bbn.marti.service.PluginStore;

import tak.server.Constants;
import tak.server.PluginManager;
import tak.server.ignite.IgniteHolder;
import tak.server.plugins.PluginApi;
import tak.server.plugins.PluginManagerConstants;

/*
 */
public class DistributedPluginApi implements PluginApi, org.apache.ignite.services.Service {
	private static final long serialVersionUID = -8643385239608114377L;
	private static final Logger logger = LoggerFactory.getLogger(DistributedPluginApi.class);
	
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
		setupPluginProcessListener();
	}

	@Override
	public void execute(ServiceContext ctx) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("execute method " + getClass().getSimpleName());
		}
	}

	@Override
	public void addInterceptorPluginsActive(int n) {
		PluginStore.getInstance().addInterceptorPluginsActive(n);
	}
	
	private void setupPluginProcessListener() {
		// we need to track when the plugin process exits so we can turn off interceptions
		IgnitePredicate<DiscoveryEvent> ignitePredicate = new IgnitePredicate<DiscoveryEvent>() {
			@Override
			public boolean apply(DiscoveryEvent event) {
				if (PluginManagerConstants.PLUGIN_MANAGER_IGNITE_PROFILE.equals(event.eventNode().attribute(Constants.TAK_PROFILE_KEY))) {
					PluginStore.getInstance().disableInterception();
				}
				return true;
			}
		};

		IgniteHolder.getInstance()
				.getIgnite()
				.events()
				.localListen(ignitePredicate, EventType.EVT_NODE_LEFT, EventType.EVT_NODE_FAILED);
	}

}
