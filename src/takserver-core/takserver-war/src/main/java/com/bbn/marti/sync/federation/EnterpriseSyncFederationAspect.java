package com.bbn.marti.sync.federation;

import java.rmi.RemoteException;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.sync.Metadata;

import tak.server.util.ExecutorSource;

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
    
    @Autowired
    private ExecutorSource executorSource;

    public EnterpriseSyncFederationAspect() {
    	if (logger.isDebugEnabled()) {
    		logger.debug("EnterpriseSyncFederationAspect constructor");
    	}
    }
    
    @Around("execution(* com.bbn.marti.sync.EnterpriseSyncService.insertResource(..)))") 
    public Object insertResource(ProceedingJoinPoint jp) throws Throwable {
    
    	if (logger.isDebugEnabled()) {
    		logger.debug("EnterpriseSyncService.insertResource() : " + jp.getSignature().getName() + ": Before Method Execution");
    	}
        try {
            Object result = jp.proceed();
            
            if (logger.isDebugEnabled()) {
            	logger.debug("result: " + result);
            	logger.debug("EnterpriseSyncService.insertResource() : " + jp.getSignature().getName() + ": After Method Execution");
            }
            
            if (!coreConfig.getRemoteConfiguration().getFederation().isEnableFederation()) {
    			return result;
    		}
    
        	if (!coreConfig.getRemoteConfiguration().getFederation().isAllowMissionFederation()) {
        		if (logger.isDebugEnabled()) {
        			logger.debug("mission federation disabled in config");
        		}
        		return result;
        	}
        	
        	try {
        		
        		if (isCyclic()) {
        			if (logger.isDebugEnabled()) {
        				logger.debug("skipping cyclic esync aspect execution");
        			}
        			return result;
        		}
        		
        		if (logger.isDebugEnabled()) {
        			logger.debug("esync advice " + jp.getSignature().getName() + " " + jp.getKind());
        		}
        		
        		executorSource.missionRepositoryProcessor.submit(() -> 	{
        			try {
        				// federate only file metadata (no content)
        				mfm.insertResource((Metadata) jp.getArgs()[0], (byte[]) jp.getArgs()[1], gm.groupVectorToGroupSet((String) jp.getArgs()[2]));
        			} catch (Exception e) {
        				logger.error("exception federating file", e);
        			}
        		});
        		

        	} catch (Exception e) {
        		logger.debug("exception executing create mission advice: " + e);
        	}

            
            return result;
        } finally { }
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
