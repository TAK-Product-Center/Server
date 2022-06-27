package tak.server.cluster;

import java.util.Collection;
import java.util.HashMap;

import org.apache.ignite.services.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.remote.AuthenticationConfigInfo;
import com.bbn.marti.remote.SecurityConfigInfo;
import com.bbn.marti.remote.service.SecurityManager;
import com.bbn.marti.util.MessagingDependencyInjectionProxy;


/**
 */
public class DistributedSecurityManager implements SecurityManager, org.apache.ignite.services.Service {
	private static final long serialVersionUID = -8066905449888355883L;
	private static final Logger logger = LoggerFactory.getLogger(DistributedSecurityManager.class);

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
	public AuthenticationConfigInfo getAuthenticationConfig() {
		return MessagingDependencyInjectionProxy.getInstance().submissionService().getAuthenticationConfig();
	}

	@Override
	public void modifyAuthenticationConfig(AuthenticationConfigInfo info) {
		MessagingDependencyInjectionProxy.getInstance().submissionService().modifyAuthenticationConfig(info);
	}

	@Override
	public SecurityConfigInfo getSecurityConfig() {
		return MessagingDependencyInjectionProxy.getInstance().submissionService().getSecurityConfig();
	}

	@Override
	public void modifySecurityConfig(SecurityConfigInfo info) {
		MessagingDependencyInjectionProxy.getInstance().submissionService().modifySecurityConfig(info);
	}

	@Override
	public Collection<Integer> getNonSecurePorts() {
		return MessagingDependencyInjectionProxy.getInstance().submissionService().getNonSecurePorts();
	}

	@Override
	public HashMap<String, Boolean> verifyConfiguration() {
		return MessagingDependencyInjectionProxy.getInstance().submissionService().verifyConfiguration();
	}	
}
