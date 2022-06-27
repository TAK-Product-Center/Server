package tak.server.cluster;

import java.rmi.RemoteException;
import java.util.Set;

import org.apache.ignite.services.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.injector.UidCotTagInjector;
import com.bbn.marti.remote.injector.InjectorConfig;
import com.bbn.marti.remote.service.InjectionService;

/**
 */
public class DistributedInjectionService implements InjectionService, org.apache.ignite.services.Service {
	private static final long serialVersionUID = -7894312879666068630L;
	private static final Logger logger = LoggerFactory.getLogger(DistributedInjectionService.class);
		
	public DistributedInjectionService() {
		
	}

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
	public boolean setInjector(String uid, String toInject) {
		return UidCotTagInjector.getInstance().setInjector(uid, toInject);
	}

	@Override
	public InjectorConfig deleteInjector(InjectorConfig injector) throws RemoteException {
		return UidCotTagInjector.getInstance().deleteInjector(injector);
	}

	@Override
	public Set<InjectorConfig> getInjectors(String uid) {
		return UidCotTagInjector.getInstance().getInjectors(uid);
	}

	@Override
	public Set<InjectorConfig> getAllInjectors() {
		return UidCotTagInjector.getInstance().getAllInjectors();
	}
	
}
