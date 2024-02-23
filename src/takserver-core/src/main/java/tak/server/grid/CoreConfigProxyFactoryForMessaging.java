package tak.server.grid;

import org.apache.ignite.Ignite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;

import com.bbn.marti.remote.CoreConfig;

import com.bbn.marti.remote.config.CoreConfigFacade;

public class CoreConfigProxyFactoryForMessaging implements FactoryBean<CoreConfig> {

	private static final Logger logger = LoggerFactory.getLogger(CoreConfigProxyFactoryForMessaging.class);
	
	@Autowired
	Ignite ignite;

	@Override
	public CoreConfig getObject() throws Exception {

	    if (logger.isDebugEnabled()) {
            logger.debug("get " + getObjectType().getSimpleName() + " from ignite");
        }

		CoreConfig coreConfig = CoreConfigFacade.getInstance();
		if (logger.isDebugEnabled()) {
            logger.debug("CoreConfig proxy is: " + coreConfig);
        }
		return coreConfig;
	}

	@Override
	public Class<?> getObjectType() {
		return CoreConfig.class;
	}

}
