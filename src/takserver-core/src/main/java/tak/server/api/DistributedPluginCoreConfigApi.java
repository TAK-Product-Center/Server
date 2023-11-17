package tak.server.api;

import org.apache.ignite.services.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tak.server.plugins.PluginCoreConfigApi;
import tak.server.system.ApiDependencyProxy;


/**
 * Exposes a select number of CoreConfig options to the Plugins
 */
public class DistributedPluginCoreConfigApi implements PluginCoreConfigApi, org.apache.ignite.services.Service {

	private static final long serialVersionUID = 1L;

	private static final Logger logger = LoggerFactory.getLogger(DistributedPluginCoreConfigApi.class);


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

	private Security security;

	@Override
	public synchronized Security getSecurity() throws Exception {
		com.bbn.marti.config.Tls tls = ApiDependencyProxy.getInstance().coreConfig().getRemoteConfiguration().getSecurity().getTls();
		if (security == null) {
			security = new Security(new Tls(tls.getKeystore(), tls.getKeystoreFile(), tls.getKeystorePass(),
					tls.getTruststore(), tls.getTruststoreFile(), tls.getTruststorePass(),
					tls.getContext(), tls.getKeymanager()));
		}
		return security;
	}
}
