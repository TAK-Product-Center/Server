package tak.server.config;

import org.apache.ignite.services.Service;
import org.apache.ignite.services.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.service.DistributedConfiguration;

import tak.server.plugins.SystemInfoApi;

/**
 */
public class DistributedSystemInfoApi implements Service, SystemInfoApi {

	private static final long serialVersionUID = -900700616308028885L;
	private static final Logger logger = LoggerFactory.getLogger(DistributedSystemInfoApi.class);
    
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
	public String getTAKServerUrl() {
		try {
			return DistributedConfiguration.getInstance()
					.getRemoteConfiguration().getFederation().getFederationServer().getWebBaseUrl();
		} catch (Exception e) {
			return null;
		}
	}
}
