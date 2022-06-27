package com.bbn.marti.sync.federation;

import java.rmi.RemoteException;
import java.util.NavigableSet;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.remote.sync.MissionContent;

@Aspect
@Configurable
public class MissionFederationAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(MissionFederationAspect.class);
    
    @Autowired
    private MissionFederationManager mfm;
    
    @Autowired
    private GroupManager gm;
    
    @Autowired
    private CoreConfig coreConfig;

    // If we were using AspectJ instead of Spring AOP - something like !adviceexecution() would avoid the stack tracing
    // or name this pointcut and use !within(A)
    @AfterReturning("execution(* com.bbn.marti.sync.service.MissionService.createMission(..))")
    public void createMission(JoinPoint jp) throws RemoteException {

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
    			if (logger.isDebugEnabled())  {
    				logger.debug("skipping cyclic mission aspect execution");
    			}
    			return;
    		}
    		
    		if (logger.isDebugEnabled()) {    		
    			logger.debug("mission advice " + jp.getSignature().getName() + " " + jp.getKind());
    		}
    		
    		String name = (String) jp.getArgs()[0];
    		String creatorUid = (String) jp.getArgs()[1];
    		NavigableSet<Group> groups = gm.groupVectorToGroupSet((String) jp.getArgs()[2]);
    		String description = (String) jp.getArgs()[3];
    		String chatRoom = (String) jp.getArgs()[4];
    		String tool = (String) jp.getArgs()[5];
    		
    		// federated mission creation
    		mfm.createMission(name, creatorUid, description, chatRoom, tool, groups);
    		
    		if (logger.isDebugEnabled()) {
    			logger.debug("intercepted mission create - name: " + name + " creatorUid: " + creatorUid + " description: " + description + " chatRoom: " + chatRoom + " tool: " + tool + " groups: ");
    		}
    	} catch (Exception e) {
    		if (logger.isDebugEnabled()) {
    			logger.debug("exception executing create mission advice: " + e);
    		}
    	}
    }
    
    @AfterReturning("execution(* com.bbn.marti.sync.service.MissionService.deleteMission(..))")
    public void deleteMission(JoinPoint jp) throws RemoteException {

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
    			logger.debug("federated mission deletion disabled in config");
    		}
    		return;
    	}
    	
    	try {
    		
    		if (isCyclic()) {
    			if (logger.isDebugEnabled()) {
    				logger.debug("skipping cyclic mission aspect execution");
    			}
    			return;
    		}
    		
    		if (logger.isDebugEnabled()) {
    			logger.debug("mission advice " + jp.getSignature().getName() + " " + jp.getKind());
    		}
    		
    		// federated mission creation
    		mfm.deleteMission((String) jp.getArgs()[0], (String) jp.getArgs()[1], gm.groupVectorToGroupSet((String) jp.getArgs()[2]));
    		
    	} catch (Exception e) {
    		if (logger.isDebugEnabled()) {
    			logger.debug("exception executing delete mission advice: ", e);
    		}
    	}
    }
    
    @AfterReturning("execution(* com.bbn.marti.sync.service.MissionService.addMissionContent(..))")
    public void addMissionContent(JoinPoint jp) throws RemoteException {

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
    				logger.debug("skipping cyclic mission aspect execution");
    			}
    			return;
    		}
    		
    		if (logger.isDebugEnabled()) {
    			logger.debug("addMissionContent advice " + jp.getSignature().getName() + " " + jp.getKind());
    		}
    		
    		// federated add mission content
    		mfm.addMissionContent((String) jp.getArgs()[0], (MissionContent) jp.getArgs()[1], (String) jp.getArgs()[2], gm.groupVectorToGroupSet((String) jp.getArgs()[3]));
    		
    	} catch (Exception e) {
    		if (logger.isDebugEnabled()) {
    			logger.debug("exception executing create mission advice: " + e);
    		}
    	}
    }
    
    @AfterReturning("execution(* com.bbn.marti.sync.service.MissionService.deleteMissionContent(..))")
    public void deleteMissionContent(JoinPoint jp) throws RemoteException {

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
    			logger.debug("mission federation delete disabled in config");
    		}
    		return;
    	}
    	
    	try {
    		
    		if (isCyclic()) {
    			if (logger.isDebugEnabled()) {
    				logger.debug("skipping cyclic mission aspect execution");
    			}
    			return;
    		}
    		
    		if (logger.isDebugEnabled()) {
    			logger.debug("addMissionContent advice " + jp.getSignature().getName() + " " + jp.getKind());
    		}
    		
    		// federated add mission content
    		mfm.deleteMissionContent((String) jp.getArgs()[0], (String) jp.getArgs()[1], (String) jp.getArgs()[2], (String) jp.getArgs()[3], gm.groupVectorToGroupSet((String) jp.getArgs()[4]));
    		
    	} catch (Exception e) {
    		if (logger.isDebugEnabled()) {
    			logger.debug("exception executing create mission advice: " + e);
    		}
    	}
    }

	@AfterReturning("execution(* com.bbn.marti.sync.service.MissionService.setParent(..))")
	public void setParent(JoinPoint jp) throws RemoteException {

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
					logger.debug("skipping cyclic mission aspect execution");
				}
				return;
			}

			if (logger.isDebugEnabled()) {
				logger.debug("setParent advice " + jp.getSignature().getName() + " " + jp.getKind());
			}

			// federated add mission content
			mfm.setParent((String) jp.getArgs()[0], (String) jp.getArgs()[1], gm.groupVectorToGroupSet((String) jp.getArgs()[2]));

		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception executing setParent: " + e);
			}
		}
	}

	@AfterReturning("execution(* com.bbn.marti.sync.service.MissionService.clearParent(..))")
	public void clearParent(JoinPoint jp) throws RemoteException {

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
					logger.debug("skipping cyclic mission aspect execution");
				}
				return;
			}

			if (logger.isDebugEnabled()) {
				logger.debug("setParent advice " + jp.getSignature().getName() + " " + jp.getKind());
			}

			// federated clear mission parent
			mfm.clearParent((String) jp.getArgs()[0], gm.groupVectorToGroupSet((String) jp.getArgs()[1]));

		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception executing clearParent: " + e);
			}
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
