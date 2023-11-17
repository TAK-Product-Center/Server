package com.bbn.marti.sync.federation;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.NavigableSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import javax.naming.NamingException;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.owasp.esapi.errors.IntrusionException;
import org.owasp.esapi.errors.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.atakmap.Tak.ROL;
import com.bbn.marti.config.DataFeed;
import com.bbn.marti.maplayer.model.MapLayer;
import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.service.InputManager;
import com.bbn.marti.remote.sync.MissionExpiration;
import com.bbn.marti.remote.sync.MissionHierarchy;
import com.bbn.marti.remote.sync.MissionUpdateDetails;
import com.bbn.marti.remote.sync.MissionUpdateDetailsForMapLayer;
import com.bbn.marti.remote.sync.MissionUpdateDetailsForMapLayerType;
import com.bbn.marti.remote.sync.MissionUpdateDetailsForMissionLayer;
import com.bbn.marti.remote.sync.MissionUpdateDetailsForMissionLayerType;
import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.marti.sync.EnterpriseSyncService;
import com.bbn.marti.sync.Metadata;
import com.bbn.marti.sync.model.DataFeedDao;
import com.bbn.marti.sync.model.Mission;
import com.bbn.marti.sync.model.MissionRole;
import com.bbn.marti.sync.model.MissionRole.Role;
import com.bbn.marti.sync.repository.DataFeedRepository;
import com.bbn.marti.sync.service.MissionService;
import com.google.common.base.Strings;

import mil.af.rl.rol.FederationProcessor;
import mil.af.rl.rol.Resource;
import mil.af.rl.rol.ResourceOperationParameterEvaluator;
import mil.af.rl.rol.RolLexer;
import mil.af.rl.rol.RolParser;
import mil.af.rl.rol.value.DataFeedMetadata;
import mil.af.rl.rol.value.MissionMetadata;
import tak.server.feeds.DataFeed.DataFeedType;

public class FederationROLHandler {

	private static final Logger logger = LoggerFactory.getLogger(FederationROLHandler.class);

	protected MissionService missionService;

	protected EnterpriseSyncService syncService;

	protected RemoteUtil remoteUtil;

	protected CoreConfig coreConfig;
	
	protected DataFeedRepository dataFeedRepository;
	
	@Autowired
    private InputManager inputManager;

	public FederationROLHandler(MissionService missionService, EnterpriseSyncService syncService, RemoteUtil remoteUtil, CoreConfig coreConfig, DataFeedRepository dataFeedRepository) throws RemoteException {
		this.missionService = missionService;
		this.syncService = syncService;
		this.remoteUtil = remoteUtil;
		this.coreConfig = coreConfig;
		this.dataFeedRepository = dataFeedRepository;
	}

	public void onNewEvent(ROL rol, Set<Group> groups) throws RemoteException {

		if (rol == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("skipping null ROL message");
			}
			return;
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Got ROL message from core: " + rol.getProgram() + " for groups " + groups);
		}

		// interpret and execute the ROL program
		RolLexer lexer = new RolLexer(new ANTLRInputStream(rol.getProgram()));

		CommonTokenStream tokens = new CommonTokenStream(lexer);

		RolParser parser = new RolParser(tokens);
		parser.setErrorHandler(new BailErrorStrategy());

		// parse the ROL program
		ParseTree rolParseTree = parser.program();

		requireNonNull(rolParseTree, "parsed ROL program");

		final AtomicReference<String> res = new AtomicReference<>();
		final AtomicReference<String> op = new AtomicReference<>();
		final AtomicReference<Object> parameters = new AtomicReference<>();

		new MissionEnterpriseSyncRolVisitor(new ResourceOperationParameterEvaluator<Object, String>() {
			@Override
			public String evaluate(String resource, String operation, Object params) {
				if (logger.isDebugEnabled()) {
					logger.debug(" evaluating " + operation + " on " + resource + " given " + params);
				}

				res.set(resource);
				op.set(operation);
				parameters.set(params);

				return resource;
			}
		}).visit(rolParseTree);

		try {
			new FederationProcessorFactory().newProcessor(res.get(), op.get(), parameters.get(), (NavigableSet<Group>) groups).process(rol);
		} catch (Exception e) {
			logger.warn("exception processing incoming ROL", e);
		}
	}

	class FederationProcessorFactory {

		FederationProcessor<ROL> newProcessor(String resource, String operation, Object parameters, NavigableSet<Group> groups) {
			switch (Resource.valueOf(resource.toUpperCase())) {
			case PACKAGE:
				throw new UnsupportedOperationException("federated mission package processing occurs in core - this ROL should not have been sent");
			case MISSION:
				return new FederationMissionProcessor(resource, operation, parameters, groups);
			case DATA_FEED:
				return new FederationDataFeedProcessor(resource, operation, parameters, groups);
			case RESOURCE:
				return new FederationSyncResourceProcessor(resource, operation, (com.bbn.marti.sync.model.Resource) parameters, groups);
			default:
				throw new IllegalArgumentException("invalid federation processor kind " + resource);
			}
		}
	}

	private class FederationMissionProcessor implements FederationProcessor<ROL> {

		private final String op;
		private final Object parameters;
		private final NavigableSet<Group> groups;

		FederationMissionProcessor(String res, String op, Object parameters, NavigableSet<Group> groups) {
			this.op = op;
			this.parameters = parameters;
			this.groups = groups;
		}

		@Override
		public void process(ROL rol) {

			if (!coreConfig.getRemoteConfiguration().getFederation().isAllowMissionFederation()) {
				if (logger.isDebugEnabled()) {
					logger.debug("mission federation disabled in config");
				}
				return;
			}

			switch (op.toLowerCase(Locale.ENGLISH)) {
			case "create":
				processCreate(rol);
				break;
			case "delete":
				if (requireNonNull(coreConfig.getRemoteConfiguration().getFederation(), "federation CoreConfig").isAllowFederatedDelete()) {
					processDelete(rol);
				} else {
					logger.info("ignoring federated 'delete mission' command: federated delete is disabled in CoreConfig");
				}

				break;
			case "update":
				processUpdate(rol);
				break;
			case "assign":
				processAssign(rol);
				break;
			default:
			}
		}

		private void processCreate(ROL rol) {

			if (logger.isDebugEnabled()) {
				logger.debug("recieved ROL create mission from core " + rol);
			}

			if (parameters instanceof MissionMetadata) {
				MissionMetadata md = (MissionMetadata) parameters;
				int defaultRoleId = (int) md.getDefaultRoleId();
				
				MissionRole missionRole = null;
				if (defaultRoleId > 0) {
					Role defaultRole = MissionRole.Role.values()[defaultRoleId - 1];
					missionRole = new MissionRole(defaultRole);
					missionRole.setId(md.getDefaultRoleId());
				}
				
				if (!missionService.exists(md.getName(), remoteUtil.bitVectorToString(remoteUtil.getBitVectorForGroups(groups)))) {
					missionService.createMission(md.getName(), md.getCreatorUid(), remoteUtil.bitVectorToString(remoteUtil.getBitVectorForGroups(groups)), 
							md.getDescription(), md.getChatRoom(), md.getBaseLayer(), md.getBbox(), md.getPath(), md.getClassification(), md.getTool(),
							md.getPasswordHash(), missionRole, md.getExpiration(), md.getBoundingPolygon(), md.getInviteOnly());
				}
			}
		}

		private void processDelete(ROL rol) {

			if (logger.isDebugEnabled()) {
				logger.debug("recieved ROL delete mission from core " + rol);
			}

			if (parameters instanceof MissionMetadata) {
				MissionMetadata md = (MissionMetadata) parameters;

				String groupVector = remoteUtil.bitVectorToString(remoteUtil.getBitVectorForGroups(groups));

				Mission mission = missionService.getMission(md.getName(), groupVector);

				if (mission == null) {
					logger.info("ignoring federated delete command for non-existent mission " + md.getName());
					return;
				}

				if (!isMissionAllowed(mission.getTool())) {
					logger.info("ignoring federated delete command for non-public mission " + md.getName());
					return;
				}

				missionService.deleteMission(md.getName(), md.getCreatorUid(), groupVector, false);
			}
		}

		private void processUpdate(ROL rol) {

			if (logger.isDebugEnabled()) {
				logger.debug("recieved ROL update mission from core " + rol.getProgram());
			}

			if (parameters instanceof MissionUpdateDetails) {
				MissionUpdateDetails mud = (MissionUpdateDetails) parameters;

				if (!isMissionAllowed(mud.getMissionTool())) {
					if (logger.isDebugEnabled()) {
						logger.debug("Not processing non-public mission update " + mud);
					}
					return;
				}

				if (logger.isDebugEnabled()) {
					logger.debug("mission update details: " + mud);
				}

				switch (requireNonNull(mud.getChangeType(), "mission update change type")) {
				case ADD_CONTENT:

					if (!missionService.exists(mud.getMissionName(), remoteUtil.bitVectorToString(remoteUtil.getBitVectorForGroups(groups)))) {
						missionService.createMission(mud.getMissionName(), mud.getMissionCreatorUid(), remoteUtil.bitVectorToString(remoteUtil.getBitVectorForGroups(groups)), mud.getMissionDescription(), mud.getMissionChatRoom(), null, null, null, null, mud.getMissionTool(), null, null, null, null, false);
					}

					if (logger.isDebugEnabled()) {
						logger.debug("adding mission content");
					}
					missionService.addMissionContent(mud.getMissionName(), mud.getContent(), mud.getCreatorUid(), remoteUtil.bitVectorToString(remoteUtil.getBitVectorForGroups(groups)));

					if (logger.isDebugEnabled()) {
						logger.debug("adding mission content complete");
					}
					break;
				case REMOVE_CONTENT:
					try {
						if (requireNonNull(coreConfig.getRemoteConfiguration().getFederation(), "federation CoreConfig").isAllowFederatedDelete()) {

							String hash = null;
							String uid = null;

							if (!mud.getContent().getHashes().isEmpty()) {
								hash = mud.getContent().getHashes().get(0);
							}

							if (!mud.getContent().getUids().isEmpty()) {
								uid = mud.getContent().getUids().get(0);
							}

							missionService.deleteMissionContent(mud.getMissionName(), hash, uid, mud.getCreatorUid(), remoteUtil.bitVectorToString(remoteUtil.getBitVectorForGroups(groups)));

						} else {
							logger.info("ignoring federated delete content - disabled in CoreConfig");
						}
					} catch (Exception e) {
						logger.warn("exception accessing remote CoreConfig", e);
					}
					break;
				default:
					throw new IllegalArgumentException("invalid mission change type: " + mud.getChangeType());
				}
			} else if (parameters instanceof MissionUpdateDetailsForMapLayer) {
				
				MissionUpdateDetailsForMapLayer mud = (MissionUpdateDetailsForMapLayer) parameters;

				if (logger.isDebugEnabled()) {
					logger.debug("MissionUpdateDetailsForMapLayer: " + mud);
				}
				
				if (mud.getType() == MissionUpdateDetailsForMapLayerType.ADD_MAPLAYER_TO_MISSION) {
					missionService.addMapLayerToMission(mud.getMissionName(), mud.getCreatorUid(), mud.getMission(), mud.getMapLayer());
					
				} else if (mud.getType() ==  MissionUpdateDetailsForMapLayerType.UPDATE_MAPLAYER) {
					missionService.updateMapLayer(mud.getMissionName(), mud.getCreatorUid(), mud.getMission(), mud.getMapLayer());
					
				} else if (mud.getType() == MissionUpdateDetailsForMapLayerType.REMOVE_MAPLAYER_FROM_MISSION) {
					missionService.removeMapLayerFromMission(mud.getMissionName(), mud.getCreatorUid(), mud.getMission(), mud.getMapLayer().getUid());
				
				} else {
					throw new IllegalArgumentException("invalid MissionUpdateDetailsForMapLayerType: " + mud.getType());
				}
			} else if (parameters instanceof MissionUpdateDetailsForMissionLayer){
				
				MissionUpdateDetailsForMissionLayer mud = (MissionUpdateDetailsForMissionLayer) parameters;

				if (logger.isDebugEnabled()) {
					logger.debug("MissionUpdateDetailsForMissionLayer: " + mud);
				}
				
				if (mud.getType() == MissionUpdateDetailsForMissionLayerType.ADD_MISSION_LAYER_TO_MISSION) {
					String groupVector = remoteUtil.bitVectorToString(remoteUtil.getBitVectorForGroups(groups));
					missionService.addMissionLayer(mud.getMissionName(), mud.getMission(), mud.getUid(), mud.getName(), mud.getMissionLayerType(), mud.getParentUid(), mud.getAfter(), mud.getCreatorUid(), groupVector);
					
				} else if (mud.getType() == MissionUpdateDetailsForMissionLayerType.REMOVE_MISSION_LAYER_FROM_MISSION) {
					String groupVector = remoteUtil.bitVectorToString(remoteUtil.getBitVectorForGroups(groups));
					missionService.removeMissionLayer(mud.getMissionName(), mud.getMission(), mud.getLayerUid(), mud.getCreatorUid(), groupVector);
				} else {
					throw new IllegalArgumentException("invalid MissionUpdateDetailsForMissionLayerType: " + mud.getType());
				}		
				
			} else {
				throw new IllegalArgumentException("invalid parameters object type for mission update action: " + parameters.getClass().getSimpleName());
			}
			
		}

		private void processAssign(ROL rol) {

			if (logger.isDebugEnabled()) {
				logger.debug("received ROL process assign mission from core " + rol);
			}

			if (parameters instanceof MissionHierarchy) { // for setParent
				MissionHierarchy missionHierarchy = (MissionHierarchy) parameters;

				if (Strings.isNullOrEmpty(missionHierarchy.getParentMissionName())) {
					missionService.clearParent(missionHierarchy.getMissionName(),
							remoteUtil.bitVectorToString(remoteUtil.getBitVectorForGroups(groups)));
				} else {
					missionService.setParent(missionHierarchy.getMissionName(), missionHierarchy.getParentMissionName(),
							remoteUtil.bitVectorToString(remoteUtil.getBitVectorForGroups(groups)));
				}
			}else if (parameters instanceof MissionExpiration) {
				
				MissionExpiration missionExpiration = (MissionExpiration) parameters;
				
				missionService.setExpiration(missionExpiration.getMissionName(), missionExpiration.getMissionExpiration(), 
						remoteUtil.bitVectorToString(remoteUtil.getBitVectorForGroups(groups)));
				
			}else {
				throw new IllegalArgumentException("ROL assign mission does not have correct parameters");
			}
			
		}
	}
	
	private class FederationDataFeedProcessor implements FederationProcessor<ROL> {

		private final String op;
		private final Object parameters;
		private final NavigableSet<Group> groups;

		FederationDataFeedProcessor(String res, String op, Object parameters, NavigableSet<Group> groups) {
			this.op = op;
			this.parameters = parameters;
			this.groups = groups;
		}

		@Override
		public void process(ROL rol) {			
			if (!coreConfig.getRemoteConfiguration().getFederation().isAllowDataFeedFederation()) {
				if (logger.isDebugEnabled()) {
					logger.debug("data feed federation disabled in config");
				}
				return;
			}

			switch (op.toLowerCase(Locale.ENGLISH)) {
			case "create":
				processCreate(rol);
				break;
			case "update":
				processUpdate(rol);
				break;
			case "delete":
				if (requireNonNull(coreConfig.getRemoteConfiguration().getFederation(), "federation CoreConfig").isAllowFederatedDelete()) {
					processDelete(rol);
				} else {
					logger.info("ignoring federated 'delete mission feed' command: federated delete is disabled in CoreConfig");
				}

				break;
			default:
			}
		}

		private void processCreate(ROL rol) {
			if (logger.isDebugEnabled()) {
				logger.debug("recieved ROL create feed from core " + rol);
			}

			if (parameters instanceof DataFeedMetadata) {
				DataFeedMetadata meta = (DataFeedMetadata) parameters;
				
				String groupVector = remoteUtil.bitVectorToString(remoteUtil.getBitVectorForGroups(groups));
					
				// add a federated data feed to the data feeds table if it doesn't exist
				List<DataFeedDao> datafeeds = dataFeedRepository.getDataFeedByUUID(meta.getDataFeedUid());
				if (datafeeds == null || datafeeds.size() == 0) {
					long dataFeedId = dataFeedRepository.addDataFeed(meta.getDataFeedUid(), meta.getFeedName(), DataFeedType.Federation.ordinal(), 
							meta.getAuthType(), -1, false, null, null, null, meta.isArchive(), false, meta.isArchiveOnly(), 2, null, meta.isSync(), 
							meta.getSyncCacheRetentionSeconds(), groupVector, true, false);
					
					if (meta.getTags() != null && meta.getTags().size() > 0)
						dataFeedRepository.addDataFeedTags(dataFeedId, meta.getTags());
					
					// add/update the input metrics for this feed so we can track it in the UI
					DataFeed dataFeedConfig = dataFeedRepository.getDataFeedByUUID(meta.getDataFeedUid()).get(0).toInput();
					inputManager.updateFederationDataFeed(dataFeedConfig);
				}
				
				Mission mission = missionService.getMission(meta.getMissionName(), groupVector);
				// check mission exists before making feed					
				if (mission != null) {
					// create mission feed association
					missionService.addFeedToMission(meta.getMissionFeedUid(), meta.getMissionName(), "", mission, meta.getDataFeedUid(), meta.getFilterBbox(), 
							meta.getFilterType(), meta.getFilterCallsign());
				}
			}
		}
		
		private void processUpdate(ROL rol) {
			if (logger.isDebugEnabled()) {
				logger.debug("recieved ROL update feed from core " + rol);
			}

			if (parameters instanceof DataFeedMetadata) {
				DataFeedMetadata meta = (DataFeedMetadata) parameters;
				
				String groupVector = remoteUtil.bitVectorToString(remoteUtil.getBitVectorForGroups(groups));
					
				// add a federated data feed to the data feeds table, or update if it exists
				List<DataFeedDao> datafeeds = dataFeedRepository.getDataFeedByUUID(meta.getDataFeedUid());
				if (datafeeds == null || datafeeds.size() == 0) {
					long dataFeedId = dataFeedRepository.addDataFeed(meta.getDataFeedUid(), meta.getFeedName(), DataFeedType.Federation.ordinal(), 
							meta.getAuthType(), -1, false, null, null, null, meta.isArchive(), false, meta.isArchiveOnly(), 2, null, meta.isSync(), 
							meta.getSyncCacheRetentionSeconds(), groupVector, true, false);
					dataFeedRepository.addDataFeedTags(dataFeedId, meta.getTags());
				} else {
					DataFeedDao dataFeed = datafeeds.get(0);
					dataFeedRepository.updateDataFeed(meta.getDataFeedUid(), meta.getFeedName(), DataFeedType.Federation.ordinal(), 
							meta.getAuthType(), -1, false, null, null, null, meta.isArchive(), false, meta.isArchiveOnly(), 2, null, 
							meta.isSync(), meta.getSyncCacheRetentionSeconds(), true, false);
					
					dataFeedRepository.removeAllDataFeedTagsById(dataFeed.getId());
					
					if (meta.getTags() != null && meta.getTags().size() > 0)
						dataFeedRepository.addDataFeedTags(dataFeed.getId(), meta.getTags());
				}
				
				// add/update the input metrics for this feed so we can track it in the UI
				DataFeed dataFeedConfig = dataFeedRepository.getDataFeedByUUID(meta.getDataFeedUid()).get(0).toInput();
				inputManager.updateFederationDataFeed(dataFeedConfig);
			}
		}
		
		private void processDelete(ROL rol) {

			if (logger.isDebugEnabled()) {
				logger.debug("recieved ROL delete mission feed from core " + rol);
			}
			
			if (parameters instanceof DataFeedMetadata) {
				DataFeedMetadata meta = (DataFeedMetadata) parameters;
				
				String groupVector = remoteUtil.bitVectorToString(remoteUtil.getBitVectorForGroups(groups));
				
				// if there is a mission in the delete request, remove the mission feed	
				if (!Strings.isNullOrEmpty(meta.getMissionName())) {	
					Mission mission = missionService.getMission(meta.getMissionName(), groupVector);
					missionService.removeFeedFromMission(meta.getMissionName(), "", mission, meta.getMissionFeedUid());
				}
				// if there isnt a mission in the delete, remove the data feed completely
				else {
					// get data feed
					List<DataFeedDao> datafeeds = dataFeedRepository.getDataFeedByUUID(meta.getDataFeedUid());
					DataFeedDao dataFeed = datafeeds.get(0);
					// delete any mission associations
					missionService.getMissionsForDataFeed(meta.getDataFeedUid()).forEach(m -> missionService.removeFeedFromMission(m.getName(), "", m, meta.getMissionFeedUid()));
					// remove all tags
					dataFeedRepository.removeAllDataFeedTagsById(dataFeed.getId());
					// delete feed
					dataFeedRepository.deleteDataFeedById(dataFeed.getId());
				}
			}
		}
	}

	private class FederationSyncResourceProcessor implements FederationProcessor<ROL> {

		private final String op;
		private final com.bbn.marti.sync.model.Resource resource;
		private final NavigableSet<Group> groups;

		FederationSyncResourceProcessor(String res, String op, com.bbn.marti.sync.model.Resource resource, NavigableSet<Group> groups) {
			this.op = op;
			this.resource = resource;
			this.groups = groups;
		}

		@Override
		public void process(ROL rol) {

			switch (op.toLowerCase(Locale.ENGLISH)) {
			case "create":
				processCreate(rol);
				break;
			case "delete":
				processDelete(rol);
				break;
			case "update":
				processUpdate(rol);
			default:
			}
		}

		private void processCreate(ROL rol) {

			if (logger.isDebugEnabled()) {
				logger.debug("recieved ROL create resource from core " + rol.getProgram());
			}

			if (rol.getPayloadList().isEmpty()) {
				if (logger.isDebugEnabled()) {
					logger.debug("empty resource payload");
				}
				return;
			}

			byte[] content = rol.getPayload(0).getData().toByteArray();

			if (logger.isDebugEnabled()) {
				logger.debug("rol payload size: " + content.length + " bytes");
			}

			// save the federated resource to the database
			try {
				String vector = remoteUtil.bitVectorToString(remoteUtil.getBitVectorForGroups(groups));
				if (syncService.getContentByUid(resource.getUid(), vector) == null) {
					syncService.insertResource(resource.toMetadata(), content, remoteUtil.bitVectorToString(remoteUtil.getBitVectorForGroups(groups)));
				}
			} catch (IllegalArgumentException | ValidationException | IntrusionException | IllegalStateException | SQLException | NamingException | IOException e) {
				if (logger.isDebugEnabled()) {
					logger.debug("exception saving federated resource to database", e);
				}
			}
		}

		private void processDelete(ROL rol) {
			if (logger.isDebugEnabled()) {
				logger.debug("recieved ROL delete resource from core " + rol);
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

			// delete the enterprise sync resource
			try {
				syncService.delete(resource.getHash(), remoteUtil.bitVectorToString(remoteUtil.getBitVectorForGroups(groups)));
			} catch (IllegalArgumentException | IntrusionException | IllegalStateException | SQLException | NamingException e) {
				if (logger.isDebugEnabled()) {
					logger.debug("exception executing federated enterprise sync delete", e);
				}
			}
		}

		private void processUpdate(ROL rol) {
			if (logger.isDebugEnabled()) {
				logger.debug("recieved ROL update resource from core " + rol);
			}

			if (!coreConfig.getRemoteConfiguration().getFederation().isAllowMissionFederation()) {
				if (logger.isDebugEnabled()) {
					logger.debug("mission federation disabled in config");
				}
				return;
			}

			// update the enterprise sync resource
			try {
				if (resource.getTool() != null && resource.getTool().length() > 0) {
					syncService.updateMetadata(resource.getHash(), Metadata.Field.Tool.name(), resource.getTool(),
							remoteUtil.bitVectorToString(remoteUtil.getBitVectorForGroups(groups)));
				} else if (resource.getMimeType() != null && resource.getMimeType().length() > 0) {
					syncService.updateMetadata(resource.getHash(), Metadata.Field.MIMEType.name(), resource.getMimeType(),
							remoteUtil.bitVectorToString(remoteUtil.getBitVectorForGroups(groups)));
				}
			} catch (IllegalArgumentException | IntrusionException | IllegalStateException | SQLException
					| NamingException | ValidationException e) {
				if (logger.isDebugEnabled()) {
					logger.debug("exception executing federated enterprise sync update", e);
				}
			}
		}

	}

	private boolean isMissionAllowed(String tool) {
		tool = tool == null ? "public" : tool;
		boolean onlyFederatePublic = coreConfig.getRemoteConfiguration().getFederation().isFederateOnlyPublicMissions();
		boolean isVBM = coreConfig.getRemoteConfiguration().getVbm().isEnabled();
		
		if (tool.equals("public")) {
			return true;
		} else if(tool.equals(coreConfig.getRemoteConfiguration().getNetwork().getMissionCopTool()) && onlyFederatePublic) {
			return isVBM;
		} else {
			if (onlyFederatePublic) return false;
			else return true;
		}
	}
}
