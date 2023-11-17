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
import com.bbn.marti.maplayer.model.MapLayer;
import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.remote.sync.MissionContent;
import com.bbn.marti.remote.sync.MissionUpdateDetails;
import com.bbn.marti.remote.sync.MissionUpdateDetailsForMapLayer;
import com.bbn.marti.remote.sync.MissionUpdateDetailsForMapLayerType;
import com.bbn.marti.remote.sync.MissionUpdateDetailsForMissionLayer;
import com.bbn.marti.remote.sync.MissionUpdateDetailsForMissionLayerType;
import com.bbn.marti.sync.model.Mission;
import com.bbn.marti.sync.model.MissionFeed;
import com.bbn.marti.sync.model.MissionLayer;
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
    		
    		MissionMetadata meta = MissionActionROLConverter.missionToROLMissionMetadata(mission);
    		
    		if (defaultRole != null) {
    			meta.setDefaultRoleId(defaultRole.getId());    			
    		}
   
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
    		meta.setFilterPolygon(missionFeed.getFilterPolygon());
    		meta.setFilterCallsign(missionFeed.getFilterCallsign());
    		meta.setFilterCotTypes(missionFeed.getFilterCotTypes());
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
	
	@AfterReturning("execution(* com.bbn.marti.sync.service.MissionService.setExpiration(..))")
	public void setExpiration(JoinPoint jp) throws RemoteException {
    	
		if (!isMissionFederationEnabled()) return;

		try {

			if (isCyclic()) {
				if (logger.isDebugEnabled()) {
					logger.debug("skipping cyclic mission aspect execution");
				}
				return;
			}

			if (logger.isDebugEnabled()) {
				logger.debug("setExpiration advice " + jp.getSignature().getName() + " " + jp.getKind());
			}

			String missionName = (String) jp.getArgs()[0];
			Long expiration = (Long) jp.getArgs()[1];
			String groupVector = (String) jp.getArgs()[2];
			
			mfm.setExpiration(missionName, expiration, gm.groupVectorToGroupSet(groupVector));

		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception executing setExpiration: " + e);
			}
		}
	}
	
    @AfterReturning(value = "execution(* com.bbn.marti.sync.service.MissionService.addMissionLayer(..))", returning="returnValue")
	public void addMissionLayer(JoinPoint jp, Object returnValue) {
    	
    	if (!isMissionFederationEnabled()) return;

    	try {
    		
    		if (isCyclic()) {
    			if (logger.isDebugEnabled()) {
    				logger.debug("skipping cyclic addMissionLayer aspect execution");
    			}
    			return;
    		}
    		
    		if (logger.isDebugEnabled()) {
    			logger.debug("addMissionLayer aspect " + jp.getSignature().getName() + " " + jp.getKind());
    		}
    		
    		if (returnValue == null) {
    			logger.error("MissionLayer is null in addMissionLayer");
    			return;
    		}
    		
    		MissionLayer missionLayer = (MissionLayer) returnValue;

			String missionName = (String) jp.getArgs()[0];
    		Mission mission = (Mission) jp.getArgs()[1];
			String uid = (String) jp.getArgs()[2];
			String name = (String) jp.getArgs()[3];
    		MissionLayer.Type missionLayerType = (MissionLayer.Type) jp.getArgs()[4];
			String parentUid = (String) jp.getArgs()[5];
			String afterUid = (String) jp.getArgs()[6];
			String creatorUid = (String) jp.getArgs()[7];
			String groupVector = (String) jp.getArgs()[8];

			NavigableSet<Group> groups = gm.groupVectorToGroupSet(groupVector);

			MissionUpdateDetailsForMissionLayer missionUpdateDetailsForMissionLayer = new MissionUpdateDetailsForMissionLayer();
    		missionUpdateDetailsForMissionLayer.setType(MissionUpdateDetailsForMissionLayerType.ADD_MISSION_LAYER_TO_MISSION);
    		missionUpdateDetailsForMissionLayer.setMissionName(missionName);
    		missionUpdateDetailsForMissionLayer.setMission(mission);
			missionUpdateDetailsForMissionLayer.setUid(uid);
			missionUpdateDetailsForMissionLayer.setName(name);
    		missionUpdateDetailsForMissionLayer.setMissionLayerType(missionLayerType);
			missionUpdateDetailsForMissionLayer.setParentUid(parentUid);
			missionUpdateDetailsForMissionLayer.setAfter(afterUid);
    		missionUpdateDetailsForMissionLayer.setCreatorUid(creatorUid);
    		
    		mfm.addMissionLayer(missionUpdateDetailsForMissionLayer, groups);

    	} catch (Exception e) {
    		if (logger.isDebugEnabled()) {
    			logger.debug("exception executing addMissionLayer: " + e);
    		}
    	}
	}
	
    @AfterReturning("execution(* com.bbn.marti.sync.service.MissionService.removeMissionLayer(..))")
    public void removeMissionLayer(JoinPoint jp) throws RemoteException {

    	if (!isMissionFederationEnabled()) return;
    	
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
    		
    		String missionName = (String)jp.getArgs()[0];
    		Mission mission = (Mission) jp.getArgs()[1];
    		String layerUid = (String)jp.getArgs()[2];
    		String creatorUid = (String)jp.getArgs()[3];
    		String groupVector = (String) jp.getArgs()[4];
    		
    		MissionUpdateDetailsForMissionLayer missionUpdateDetailsForMissionLayer = new MissionUpdateDetailsForMissionLayer();
    		missionUpdateDetailsForMissionLayer.setType(MissionUpdateDetailsForMissionLayerType.REMOVE_MISSION_LAYER_FROM_MISSION);
    		missionUpdateDetailsForMissionLayer.setMissionName(missionName);
    		missionUpdateDetailsForMissionLayer.setMission(mission);
    		missionUpdateDetailsForMissionLayer.setLayerUid(layerUid);
    		missionUpdateDetailsForMissionLayer.setCreatorUid(creatorUid);
    		
    		mfm.deleteMissionLayer(missionUpdateDetailsForMissionLayer, gm.groupVectorToGroupSet(groupVector));
    		
    	} catch (Exception e) {
    		if (logger.isDebugEnabled()) {
    			logger.debug("exception executing delete mission advice: ", e);
    		}
    	}
    }
    
    @AfterReturning(value = "execution(* com.bbn.marti.sync.service.MissionService.addMapLayerToMission(..))", returning="returnValue")
	public void addMapLayerToMission(JoinPoint jp, Object returnValue) {
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
    		
    		if (returnValue == null) {
    			logger.error("MapLayer is null in addMapLayerToMission");
    			return;
    		}
    		
    		MapLayer mapLayer = (MapLayer) returnValue;
    	
    		NavigableSet<Group> groups = gm.groupVectorToGroupSet(mapLayer.getMission().getGroupVector());
    		    		
    		String missionName = (String) jp.getArgs()[0];
    		String creatorUid = (String) jp.getArgs()[1];
    		Mission mission = (Mission) jp.getArgs()[2];
//    		MapLayer mapLayer = (MapLayer) jp.getArgs()[3]; // use the return value instead of the input parameter
    		
    		MissionUpdateDetailsForMapLayer missionUpdateDetailsForMapLayer = new MissionUpdateDetailsForMapLayer();
    		missionUpdateDetailsForMapLayer.setType(MissionUpdateDetailsForMapLayerType.ADD_MAPLAYER_TO_MISSION);
    		missionUpdateDetailsForMapLayer.setMissionName(missionName);
    		missionUpdateDetailsForMapLayer.setCreatorUid(creatorUid);
    		missionUpdateDetailsForMapLayer.setMission(mission);
    		missionUpdateDetailsForMapLayer.setMapLayer(mapLayer);
    		
    		mfm.addMapLayerToMission(missionUpdateDetailsForMapLayer, groups);

    		if (logger.isDebugEnabled()) {
    			logger.debug("intercepted addMapLayerToMission - mapLayer: " + mapLayer.getName());
    		}
    	} catch (Exception e) {
    		if (logger.isDebugEnabled()) {
    			logger.debug("exception executing addMapLayerToMission advice: " + e);
    		}
    	}
	}
    
    @AfterReturning(value = "execution(* com.bbn.marti.sync.service.MissionService.updateMapLayer(..))", returning="returnValue")
	public void updateMapLayer(JoinPoint jp, Object returnValue) {
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
    		
    		if (returnValue == null) {
    			logger.error("MapLayer is null in addMapLayerToMission");
    			return;
    		}
    		
    		MapLayer mapLayer = (MapLayer) returnValue;
    		
    		NavigableSet<Group> groups = gm.groupVectorToGroupSet(mapLayer.getMission().getGroupVector());
    		
      		String missionName = (String) jp.getArgs()[0];
    		String creatorUid = (String) jp.getArgs()[1];
    		Mission mission = (Mission) jp.getArgs()[2];
//    		MapLayer mapLayer = (MapLayer) jp.getArgs()[3]; // use the return value instead of the input parameter
    		
    		MissionUpdateDetailsForMapLayer missionUpdateDetailsForMapLayer = new MissionUpdateDetailsForMapLayer();
    		missionUpdateDetailsForMapLayer.setType(MissionUpdateDetailsForMapLayerType.UPDATE_MAPLAYER);
    		missionUpdateDetailsForMapLayer.setMissionName(missionName);
    		missionUpdateDetailsForMapLayer.setCreatorUid(creatorUid);
    		missionUpdateDetailsForMapLayer.setMission(mission);
    		missionUpdateDetailsForMapLayer.setMapLayer(mapLayer);
    		
    		mfm.updateMapLayer(missionUpdateDetailsForMapLayer, groups);

    		if (logger.isDebugEnabled()) {
    			logger.debug("intercepted updateMapLayer - mapLayer: " + mapLayer.getName());
    		}
    	} catch (Exception e) {
    		if (logger.isDebugEnabled()) {
    			logger.debug("exception executing updateMapLayer advice: " + e);
    		}
    	}
	}
    
    @AfterReturning("execution(* com.bbn.marti.sync.service.MissionService.removeMapLayerFromMission(..))")
	public void removeMapLayerFromMission(JoinPoint jp) {
    	
    	if (!isMissionFederationEnabled()) return;
    	
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
    		String mapLayerUid = (String) jp.getArgs()[3];
    		
    		NavigableSet<Group> groups = gm.groupVectorToGroupSet(mission.getGroupVector());
    		
    		MissionUpdateDetailsForMapLayer missionUpdateDetailsForMapLayer = new MissionUpdateDetailsForMapLayer();
    		missionUpdateDetailsForMapLayer.setType(MissionUpdateDetailsForMapLayerType.REMOVE_MAPLAYER_FROM_MISSION);
    		missionUpdateDetailsForMapLayer.setMissionName(missionName);
    		missionUpdateDetailsForMapLayer.setCreatorUid(creatorUid);
    		missionUpdateDetailsForMapLayer.setMission(mission);
    		MapLayer mapLayer = new MapLayer();
    		mapLayer.setUid(mapLayerUid);
    		missionUpdateDetailsForMapLayer.setMapLayer(mapLayer);
    		
    		mfm.removeMapLayerFromMission(missionUpdateDetailsForMapLayer, groups);
    		 		
    	} catch (Exception e) {
    		if (logger.isDebugEnabled()) {
    			logger.debug("exception executing delete mission advice: ", e);
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
