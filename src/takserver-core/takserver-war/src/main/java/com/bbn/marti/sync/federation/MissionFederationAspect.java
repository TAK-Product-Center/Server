package com.bbn.marti.sync.federation;

import java.rmi.RemoteException;
import java.util.Date;
import java.util.NavigableSet;
import java.util.UUID;

import com.bbn.marti.remote.config.CoreConfigFacade;
import com.bbn.marti.sync.repository.LogEntryRepository;
import com.bbn.marti.sync.service.MissionService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Hibernate;
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
import com.bbn.marti.remote.sync.MissionUpdateDetailsForLogEntry;
import com.bbn.marti.remote.sync.MissionUpdateDetailsForLogEntryType;
import com.bbn.marti.remote.sync.MissionUpdateDetailsForMapLayer;
import com.bbn.marti.remote.sync.MissionUpdateDetailsForMapLayerType;
import com.bbn.marti.remote.sync.MissionUpdateDetailsForMissionLayer;
import com.bbn.marti.remote.sync.MissionUpdateDetailsForMissionLayerType;
import com.bbn.marti.remote.sync.MissionUpdateDetailsForExternalData;
import com.bbn.marti.remote.sync.MissionUpdateDetailsForExternalDataType;
import com.bbn.marti.sync.model.ExternalMissionData;
import com.bbn.marti.sync.model.LogEntry;
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
	private MissionService ms;

	@Autowired
	private LogEntryRepository logEntryRepository;

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


		if (!CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation().isAllowFederatedDelete()) {
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

			DataFeed dataFeed = CoreConfigFacade.getInstance().getRemoteConfiguration()
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

	@AfterReturning("execution(* com.bbn.marti.sync.service.MissionService.addMissionContentAtTime(..))")
	public void addMissionContentAtTime(JoinPoint jp) throws RemoteException {

		if (!isMissionFederationEnabled()) return;

		try {

			if (isCyclic()) {
				if (logger.isDebugEnabled()) {
					logger.debug("skipping cyclic mission aspect execution");
				}
				return;
			}

			if (logger.isDebugEnabled()) {
				logger.debug("addMissionContentAtTime advice " + jp.getSignature().getName() + " " + jp.getKind());
			}

			Mission mission = ms.getMissionByGuidCheckGroups((UUID) jp.getArgs()[0], (String) jp.getArgs()[3]);
			ms.validateMissionByGuid(mission, ((UUID) jp.getArgs()[0]).toString());

			// federated add mission content
			mfm.addMissionContent(mission.getName(), (MissionContent) jp.getArgs()[1], (String) jp.getArgs()[2],
					gm.groupVectorToGroupSet((String) jp.getArgs()[3]), (Date) jp.getArgs()[4], (String) jp.getArgs()[5]);

		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception executing create mission advice: " + e);
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

			Mission mission = ms.getMissionByGuidCheckGroups((UUID) jp.getArgs()[0], (String) jp.getArgs()[3]);
			ms.validateMissionByGuid(mission, ((UUID) jp.getArgs()[0]).toString());

			// federated add mission content
			mfm.addMissionContent(mission.getName(), (MissionContent) jp.getArgs()[1], (String) jp.getArgs()[2],
					gm.groupVectorToGroupSet((String) jp.getArgs()[3]), null, null);

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

			Mission mission = ms.getMissionByGuidCheckGroups((UUID) jp.getArgs()[0], (String) jp.getArgs()[4]);
			ms.validateMissionByGuid(mission, ((UUID) jp.getArgs()[0]).toString());

			// federated add mission content
			mfm.deleteMissionContent(mission.getName(), (String) jp.getArgs()[1], (String) jp.getArgs()[2], (String) jp.getArgs()[3], gm.groupVectorToGroupSet((String) jp.getArgs()[4]));

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

			Mission childMission = ms.getMissionByGuidCheckGroups((UUID) jp.getArgs()[0], (String) jp.getArgs()[2]);
			ms.validateMissionByGuid(childMission, ((UUID) jp.getArgs()[0]).toString());

			Mission parentMission = ms.getMissionByGuidCheckGroups((UUID) jp.getArgs()[1], (String) jp.getArgs()[2]);
			ms.validateMissionByGuid(parentMission, ((UUID) jp.getArgs()[1]).toString());

			// federated add mission content
			mfm.setParent(childMission.getName(), parentMission.getName(), gm.groupVectorToGroupSet((String) jp.getArgs()[2]));

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

			Mission childMission = ms.getMissionByGuidCheckGroups((UUID) jp.getArgs()[0], (String) jp.getArgs()[1]);
			ms.validateMissionByGuid(childMission, ((UUID) jp.getArgs()[0]).toString());


			// federated clear mission parent
			mfm.clearParent(childMission.getName(), gm.groupVectorToGroupSet((String) jp.getArgs()[1]));

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

	@AfterReturning("execution(* com.bbn.marti.sync.service.MissionService.setExternalMissionData(..))")
	public void setExternalMissionData(JoinPoint jp) {
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

			UUID missionGuid = (UUID) jp.getArgs()[0];
			String creatorUid = (String) jp.getArgs()[1];
			ExternalMissionData externalMissionData = (ExternalMissionData) jp.getArgs()[2];
			String groupVector = (String) jp.getArgs()[3];

			Mission mission = ms.getMissionByGuidCheckGroups(missionGuid, groupVector);
			ms.validateMissionByGuid(mission, missionGuid.toString());

			NavigableSet<Group> groups = gm.groupVectorToGroupSet(groupVector);

			MissionUpdateDetailsForExternalData missionUpdateDetailsForExternalData = new MissionUpdateDetailsForExternalData();
			missionUpdateDetailsForExternalData.setMissionGuid(missionGuid);
			missionUpdateDetailsForExternalData.setMissionName(mission.getName());
			missionUpdateDetailsForExternalData.setCreatorUid(creatorUid);
			missionUpdateDetailsForExternalData.setExternalMissionData(externalMissionData);
			missionUpdateDetailsForExternalData.setMissionUpdateDetailsForExternalDataType(
					MissionUpdateDetailsForExternalDataType.SET_EXTERNAL_DATA);

			mfm.updateExternalData(missionUpdateDetailsForExternalData, groups);

		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception executing setExternalMissionData advice: " + e);
			}
		}
	}

	@AfterReturning("execution(* com.bbn.marti.sync.service.MissionService.deleteExternalMissionData(..))")
	public void deleteExternalMissionData(JoinPoint jp) {
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

			UUID missionGuid = (UUID) jp.getArgs()[0];
			String externalMissionDataId = (String) jp.getArgs()[1];
			String notes = (String) jp.getArgs()[2];
			String creatorUid = (String) jp.getArgs()[3];
			String groupVector = (String) jp.getArgs()[4];

			Mission mission = ms.getMissionByGuidCheckGroups(missionGuid, groupVector);
			ms.validateMissionByGuid(mission, missionGuid.toString());

			NavigableSet<Group> groups = gm.groupVectorToGroupSet(groupVector);

			MissionUpdateDetailsForExternalData missionUpdateDetailsForExternalData = new MissionUpdateDetailsForExternalData();
			missionUpdateDetailsForExternalData.setMissionGuid(missionGuid);
			missionUpdateDetailsForExternalData.setMissionName(mission.getName());
			missionUpdateDetailsForExternalData.setExternalMissionDataId(externalMissionDataId);
			missionUpdateDetailsForExternalData.setNotes(notes);
			missionUpdateDetailsForExternalData.setCreatorUid(creatorUid);

			missionUpdateDetailsForExternalData.setMissionUpdateDetailsForExternalDataType(
					MissionUpdateDetailsForExternalDataType.DELETE_EXTERNAL_DATA);

			mfm.updateExternalData(missionUpdateDetailsForExternalData, groups);

		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception executing deleteExternalMissionData advice: " + e);
			}
		}
	}

	@AfterReturning("execution(* com.bbn.marti.sync.service.MissionService.notifyExternalMissionDataChanged(..))")
	public void notifyExternalMissionDataChanged(JoinPoint jp) {
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

			UUID missionGuid = (UUID) jp.getArgs()[0];
			String externalMissionDataId = (String) jp.getArgs()[1];
			String token = (String) jp.getArgs()[2];
			String notes = (String) jp.getArgs()[3];
			String creatorUid = (String) jp.getArgs()[4];
			String groupVector = (String) jp.getArgs()[5];

			Mission mission = ms.getMissionByGuidCheckGroups(missionGuid, groupVector);
			ms.validateMissionByGuid(mission, missionGuid.toString());

			NavigableSet<Group> groups = gm.groupVectorToGroupSet(groupVector);

			MissionUpdateDetailsForExternalData missionUpdateDetailsForExternalData = new MissionUpdateDetailsForExternalData();
			missionUpdateDetailsForExternalData.setMissionGuid(missionGuid);
			missionUpdateDetailsForExternalData.setMissionName(mission.getName());
			missionUpdateDetailsForExternalData.setExternalMissionDataId(externalMissionDataId);
			missionUpdateDetailsForExternalData.setToken(token);
			missionUpdateDetailsForExternalData.setNotes(notes);
			missionUpdateDetailsForExternalData.setCreatorUid(creatorUid);

			missionUpdateDetailsForExternalData.setMissionUpdateDetailsForExternalDataType(
					MissionUpdateDetailsForExternalDataType.NOTIFY_EXTERNAL_DATA_CHANGE);

			mfm.updateExternalData(missionUpdateDetailsForExternalData, groups);

		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception executing notifyExternalMissionDataChanged advice: " + e);
			}
		}
	}

	@Before("execution(* com.bbn.marti.sync.service.MissionService.deleteLogEntry(..))")
	public void deleteLogEntry(JoinPoint jp) {
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

			String id = (String) jp.getArgs()[0];
			String groupVector = (String) jp.getArgs()[1];

			LogEntry entry = logEntryRepository.getOne(id);

			MissionUpdateDetailsForLogEntry missionUpdateDetailsForLogEntry = new MissionUpdateDetailsForLogEntry();
			missionUpdateDetailsForLogEntry.setLogEntry((LogEntry)Hibernate.unproxy(entry));
			missionUpdateDetailsForLogEntry.setMissionUpdateDetailsForLogEntryType(
					MissionUpdateDetailsForLogEntryType.DELETE_LOG_ENTRY);
			missionUpdateDetailsForLogEntry.setId(id);

			NavigableSet<Group> groups = gm.groupVectorToGroupSet(groupVector);

			mfm.addUpdateDeleteLogEntry(missionUpdateDetailsForLogEntry, groups);

		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception executing deleteLogEntry advice: " + e);
			}
		}
	}

	@AfterReturning("execution(* com.bbn.marti.sync.service.MissionService.addUpdateLogEntry(..))")
	public void addUpdateLogEntry(JoinPoint jp) {
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

			LogEntry entry = (LogEntry) jp.getArgs()[0];
			Date created = (Date) jp.getArgs()[1];
			String groupVector = (String) jp.getArgs()[2];

			MissionUpdateDetailsForLogEntry missionUpdateDetailsForLogEntry = new MissionUpdateDetailsForLogEntry();
			missionUpdateDetailsForLogEntry.setLogEntry(entry);
			missionUpdateDetailsForLogEntry.setMissionUpdateDetailsForLogEntryType(
					MissionUpdateDetailsForLogEntryType.ADD_UPDATE_LOG_ENTRY);
			missionUpdateDetailsForLogEntry.setCreated(created);

			NavigableSet<Group> groups = gm.groupVectorToGroupSet(groupVector);

			mfm.addUpdateDeleteLogEntry(missionUpdateDetailsForLogEntry, groups);

		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception executing addUpdateLogEntry advice: " + e);
			}
		}
	}

	private boolean isMissionFederationEnabled() {
		boolean isEnabled = true;

		CoreConfig coreConfig = CoreConfigFacade.getInstance();

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
		if (!CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation().isAllowFederatedDelete()) {
			if (logger.isDebugEnabled()) {
				logger.debug("mission federation delete disabled in config");
			}
			return false;
		}
		return true;
	}

	private boolean isMissionDataFeedFederationEnabled() {
		boolean isEnabled = true;

		if (!CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation().isAllowDataFeedFederation()) {
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
