package com.bbn.marti.sync.federation;

import java.rmi.RemoteException;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.sync.Metadata;

@Aspect
@Configurable
public class EnterpriseSyncFederationAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(EnterpriseSyncFederationAspect.class);
    
    @Autowired
    private MissionFederationManager mfm;
    
    @Autowired
    private GroupManager gm;
    
    @Autowired
    private CoreConfig coreConfig;

    public EnterpriseSyncFederationAspect() {
    	if (logger.isDebugEnabled()) {
    		logger.debug("EnterpriseSyncFederationAspect constructor");
    	}
    }

    // If we were using AspectJ instead of Spring AOP - something like !adviceexecution() would avoid the stack tracing
    // or name this pointcut and use !within(A)
    @Before("execution(* com.bbn.marti.sync.EnterpriseSyncService.insertResource(..))")
    public void insertResource(JoinPoint jp) throws RemoteException {

		if (!coreConfig.getRemoteConfiguration().getFederation().isEnableFederation()) {
			return;
		}

    	if (!coreConfig.getRemoteConfiguration().getFederation().isAllowMissionFederation()) {
    		if (logger.isDebugEnabled()) {
    			logger.debug("mission federation disabled in config");
    		}
    		return;
    	}
    	
    	try {
    		
    		if (isCyclic()) {
    			if (logger.isDebugEnabled()) {
    				logger.debug("skipping cyclic esync aspect execution");
    			}
    			return;
    		}
    		
    		if (logger.isDebugEnabled()) {
    			logger.debug("esync advice " + jp.getSignature().getName() + " " + jp.getKind());
    		}
    		
    		mfm.insertResource((Metadata) jp.getArgs()[0], (byte[]) jp.getArgs()[1], gm.groupVectorToGroupSet((String) jp.getArgs()[2]));
    	} catch (Exception e) {
    		logger.debug("exception executing create mission advice: " + e);
    	}
    }
    
    @Before("execution(* com.bbn.marti.sync.EnterpriseSyncService.delete(..))")
    public void deleteResource(JoinPoint jp) throws RemoteException {

		if (!coreConfig.getRemoteConfiguration().getFederation().isEnableFederation()) {
			return;
		}

    	if (!coreConfig.getRemoteConfiguration().getFederation().isAllowMissionFederation()) {
    		if (logger.isDebugEnabled()) {
    			logger.debug("mission federation disabled in config");
    		}
    		return;
    	}
    	
    	if (!coreConfig.getRemoteConfiguration().getFederation().isAllowFederatedDelete()) {
    		if (logger.isDebugEnabled()) {
    			logger.debug("mission federation deletion disabled in config");
    		}
    		return;
    	}
    	
    	// TODO Implement federated enterprise sync deletion 
    }


	@Before("execution(* com.bbn.marti.sync.EnterpriseSyncService.updateMetadata(..))")
	public void updateMetadata(JoinPoint jp) throws RemoteException {

		if (!coreConfig.getRemoteConfiguration().getFederation().isEnableFederation()) {
			return;
		}

		if (!coreConfig.getRemoteConfiguration().getFederation().isAllowMissionFederation()) {
			if (logger.isDebugEnabled()) {
				logger.debug("mission federation disabled in config");
			}
			return;
		}

		try {

			if (isCyclic()) {
				if (logger.isDebugEnabled()) {
					logger.debug("skipping cyclic esync aspect execution");
				}
				return;
			}

			if (logger.isDebugEnabled()) {
				logger.debug("esync advice " + jp.getSignature().getName() + " " + jp.getKind());
			}

			mfm.updateMetadata((String)jp.getArgs()[0], (String)jp.getArgs()[1], (String)jp.getArgs()[2],
					gm.groupVectorToGroupSet((String)jp.getArgs()[3]));

		} catch (Exception e) {
			logger.debug("exception executing update metadata advice: " + e);
		}
	}

    private boolean isCyclic() {
    	StackTraceElement[] stack = new Exception().getStackTrace();
    	
    	for (StackTraceElement el : stack) {
    		
    		if (logger.isTraceEnabled()) {
    			logger.trace("stack element: " + el.getClassName());
    		}
    		
			if (el.getClassName().equals(FederationROLHandler.class.getName())) {
				return true;
			}
		} 

    	return false;
    }
}
