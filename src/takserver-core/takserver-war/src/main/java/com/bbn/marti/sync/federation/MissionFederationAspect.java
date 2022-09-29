package com.bbn.marti.sync.federation;

import java.rmi.RemoteException;
import java.util.NavigableSet;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.bbn.marti.config.DataFeed;
import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.remote.sync.MissionContent;
import com.bbn.marti.sync.model.Mission;
import com.bbn.marti.sync.model.MissionFeed;
import com.bbn.marti.sync.model.MissionRole;

import mil.af.rl.rol.value.DataFeedMetadata;
import mil.af.rl.rol.value.MissionMetadata;

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
    @AfterReturning(value = "execution(* com.bbn.marti.sync.service.MissionService.createMission(..))", returning="returnValue")
    public void createMission(JoinPoint jp, Object returnValue) throws RemoteException {

		if (!isMissionFederationEnabled()) return;
    	
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
    		    		
    		Mission mission = (Mission) returnValue;
    		
    		NavigableSet<Group> groups = gm.groupVectorToGroupSet((String) jp.getArgs()[2]);
    		MissionRole defaultRole = (MissionRole) jp.getArgs()[11];
    		
    		MissionMetadata meta = new MissionMetadata();
    		meta.setName(mission.getName());
    		meta.setCreatorUid(mission.getCreatorUid());
    		meta.setDescription(mission.getDescription());
    		meta.setChatRoom(mission.getChatRoom());
    		meta.setTool(mission.getTool());
    		meta.setBbox(mission.getBbox());
    		meta.setBoundingPolygon(mission.getBoundingPolygon()); 		
    		meta.setPasswordHash(mission.getPasswordHash());
    		meta.setPath(mission.getPath());
    		meta.setClassification(mission.getClassification());
    		meta.setBaseLayer(mission.getBaseLayer());
    		
    		if (defaultRole != null)
    			meta.setDefaultRoleId(defaultRole.getId());
    		
    		meta.setExpiration(mission.getExpiration());
   
    		// federated mission creation
    		mfm.createMission(meta, groups);
    		
    		if (logger.isDebugEnabled()) {
    			logger.debug("intercepted mission create - name: " + mission.getName() + " creatorUid: " + mission.getCreatorUid() 
    			+ " description: " + mission.getDescription() + " chatRoom: " + mission.getChatRoom() + " tool: " + mission.getTool() + " groups: ");
    		}
    	} catch (Exception e) {
    		if (logger.isDebugEnabled()) {
    			logger.debug("exception executing create mission advice: " + e);
    		}
    	}
    }
    
    @AfterReturning("execution(* com.bbn.marti.sync.service.MissionService.deleteMission(..))")
    public void deleteMission(JoinPoint jp) throws RemoteException {

    	if (!isMissionFederationEnabled() || !isMissionFederatedDeleteEnabled()) return;
    	
    	
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
   
    
    @AfterReturning(value = "execution(* com.bbn.marti.sync.service.MissionService.addFeedToMission(..))", returning="returnValue")
	public void addDataFeedToMission(JoinPoint jp, Object returnValue) {
    	if (!isMissionFederationEnabled() || !isMissionDataFeedFederationEnabled()) return;

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
    		
    		MissionFeed missionFeed = (MissionFeed) returnValue;
    		DataFeed dataFeed = coreConfig.getRemoteConfiguration()
    					.getNetwork()
    					.getDatafeed()
    					.stream()
    					.filter(df -> df.getUuid().equals(missionFeed.getDataFeedUid()))
    					.findFirst().orElse(null);
    		
    		NavigableSet<Group> groups = gm.groupVectorToGroupSet(missionFeed.getMission().getGroupVector());
    		
    		
    		
    		DataFeedMetadata meta = new DataFeedMetadata();
    		meta.setMissionName(missionFeed.getMission().getName());
    		meta.setFilterBbox(missionFeed.getFilterBbox());
    		meta.setFilterCallsign(missionFeed.getFilterCallsign());
    		meta.setFilterType(missionFeed.getFilterType());
    		meta.setDataFeedUid(missionFeed.getDataFeedUid());
    		meta.setMissionFeedUid(missionFeed.getUid());
    		meta.setArchive(dataFeed.isArchive());
    		meta.setArchiveOnly(dataFeed.isArchiveOnly());
    		meta.setSync(dataFeed.isSync());
    		meta.setFeedName(dataFeed.getName());
    		meta.setAuthType(dataFeed.getAuth().toString());
    		meta.setTags(dataFeed.getTag());
   
    		// federated mission feed creation
    		mfm.createMissionFeed(missionFeed.getMission(), meta, groups);

    		if (logger.isDebugEnabled()) {
    			logger.debug("intercepted mission feed create - mission name: " + missionFeed.getMission().getName() + " dataFeedUid: " + missionFeed.getDataFeedUid());
    		}
    	} catch (Exception e) {
    		if (logger.isDebugEnabled()) {
    			logger.debug("exception executing create mission advice: " + e);
    		}
    	}
	}

    @AfterReturning("execution(* com.bbn.marti.sync.service.MissionService.removeFeedFromMission(..))")
	public void removeDataFeedFromMission(JoinPoint jp) {
    	
    	if (!isMissionFederationEnabled() || !isMissionFederatedDeleteEnabled() || !isMissionDataFeedFederationEnabled()) return;
    	
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
    		
    		String missionName = (String) jp.getArgs()[0];
    		String creatorUid = (String) jp.getArgs()[1];
    		Mission mission = (Mission) jp.getArgs()[2];
    		String missionFeedUid = (String) jp.getArgs()[3];
    		
    		DataFeedMetadata meta = new DataFeedMetadata();
    		meta.setMissionName(missionName);
    		meta.setMissionFeedUid(missionFeedUid);
    		
    		NavigableSet<Group> groups = gm.groupVectorToGroupSet(mission.getGroupVector());
    		
    		// federated mission feed delete
    		mfm.deleteMissionFeed(mission, meta, groups);
    		 		
    	} catch (Exception e) {
    		if (logger.isDebugEnabled()) {
    			logger.debug("exception executing delete mission advice: ", e);
    		}
    	}
	}
    
    @AfterReturning("execution(* com.bbn.marti.sync.service.MissionService.addMissionContent(..))")
    public void addMissionContent(JoinPoint jp) throws RemoteException {

    	if (!isMissionFederationEnabled()) return;
    	
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

    	if (!isMissionFederationEnabled() || !isMissionFederatedDeleteEnabled()) return;
    	
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
    	
		if (!isMissionFederationEnabled()) return;

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

    	if (!isMissionFederationEnabled()) return;

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
	
	private boolean isMissionFederationEnabled() {
		boolean isEnabled = true;
		if (!coreConfig.getRemoteConfiguration().getFederation().isEnableFederation()) {
			isEnabled = false;
		}

		if (!coreConfig.getRemoteConfiguration().getFederation().isAllowMissionFederation()) {
			if (logger.isDebugEnabled()) {
				logger.debug("mission federation disabled in config");
			}
			isEnabled = false;
		}
		return isEnabled;
	}

	private boolean isMissionFederatedDeleteEnabled() {
		if (!coreConfig.getRemoteConfiguration().getFederation().isAllowFederatedDelete()) {
			if (logger.isDebugEnabled()) {
				logger.debug("mission federation delete disabled in config");
			}
			return false;
		}
		return true;
	}
	
	private boolean isMissionDataFeedFederationEnabled() {
		boolean isEnabled = true;

		if (!coreConfig.getRemoteConfiguration().getFederation().isAllowDataFeedFederation()) {
			if (logger.isDebugEnabled()) {
				logger.debug("data feed federation disabled in config");
			}
			isEnabled = false;
		}
		return isEnabled;
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
