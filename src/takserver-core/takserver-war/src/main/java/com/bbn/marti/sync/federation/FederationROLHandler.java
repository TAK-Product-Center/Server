package com.bbn.marti.sync.federation;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.Locale;
import java.util.NavigableSet;
import java.util.Set;
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

import com.atakmap.Tak.ROL;
import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.sync.MissionHierarchy;
import com.bbn.marti.remote.sync.MissionUpdateDetails;
import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.marti.sync.EnterpriseSyncService;
import com.bbn.marti.sync.Metadata;
import com.bbn.marti.sync.model.Mission;
import com.bbn.marti.sync.service.MissionService;
import com.google.common.base.Strings;

import mil.af.rl.rol.FederationProcessor;
import mil.af.rl.rol.Resource;
import mil.af.rl.rol.ResourceOperationParameterEvaluator;
import mil.af.rl.rol.RolLexer;
import mil.af.rl.rol.RolParser;
import mil.af.rl.rol.value.MissionMetadata;

public class FederationROLHandler {

	private static final Logger logger = LoggerFactory.getLogger(FederationROLHandler.class);

	protected MissionService missionService;

	protected EnterpriseSyncService syncService;

	protected RemoteUtil remoteUtil;

	protected CoreConfig coreConfig;

	public FederationROLHandler(MissionService missionService, EnterpriseSyncService syncService, RemoteUtil remoteUtil, CoreConfig coreConfig) throws RemoteException {
		this.missionService = missionService;
		this.syncService = syncService;
		this.remoteUtil = remoteUtil;
		this.coreConfig = coreConfig;
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
				processMissionHierarchyUpdate(rol);
				break;
			default:
			}
		}

		private void processCreate(ROL rol) {

			if (logger.isDebugEnabled()) {
				logger.debug("recieved ROL create mission from core " + rol);
			}

			if (!(parameters instanceof MissionMetadata)) {
				throw new IllegalArgumentException("invalid parameters object type for mission create action: " + parameters.getClass().getSimpleName());
			}

			MissionMetadata md = (MissionMetadata) parameters;

			// TODO
			missionService.createMission(md.getName(), md.getCreatorUid(), remoteUtil.bitVectorToString(remoteUtil.getBitVectorForGroups(groups)), md.getDescription(), md.getChatRoom(), null, null, null, null, md.getTool(), null, null, null, null);
		}

		private void processDelete(ROL rol) {

			if (logger.isDebugEnabled()) {
				logger.debug("recieved ROL delete mission from core " + rol);
			}

			if (!(parameters instanceof MissionMetadata)) {
				throw new IllegalArgumentException("invalid parameters object type for mission create action: " + parameters.getClass().getSimpleName());
			}

			MissionMetadata md = (MissionMetadata) parameters;

			String groupVector = remoteUtil.bitVectorToString(remoteUtil.getBitVectorForGroups(groups));

			Mission mission = missionService.getMission(md.getName(), groupVector);

			if (mission == null) {
				logger.info("ignoring federated delete command for non-existent mission " + md.getName());
				return;
			}

			if (coreConfig.getRemoteConfiguration().getFederation().isFederateOnlyPublicMissions() && Strings.isNullOrEmpty(mission.getTool()) || !mission.getTool().toLowerCase(Locale.ENGLISH).equals("public")) {
				logger.info("ignoring federated delete command for non-public mission " + md.getName());
				return;
			}

			missionService.deleteMission(md.getName(), md.getCreatorUid(), groupVector, false);
		}

		private void processUpdate(ROL rol) {

			if (logger.isDebugEnabled()) {
				logger.debug("recieved ROL update mission from core " + rol.getProgram());
			}

			if (!(parameters instanceof MissionUpdateDetails)) {
				throw new IllegalArgumentException("invalid parameters object type for mission update action: " + parameters.getClass().getSimpleName());
			}

			MissionUpdateDetails mud = (MissionUpdateDetails) parameters;

			if (coreConfig.getRemoteConfiguration().getFederation().isFederateOnlyPublicMissions() && mud != null && mud.getMissionTool() != null && !mud.getMissionTool().toLowerCase(Locale.ENGLISH).equals("public")) {
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
					missionService.createMission(mud.getMissionName(), mud.getMissionCreatorUid(), remoteUtil.bitVectorToString(remoteUtil.getBitVectorForGroups(groups)), mud.getMissionDescription(), mud.getMissionChatRoom(), null, null, null, null, mud.getMissionTool(), null, null, null, null);
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
		}

		private void processMissionHierarchyUpdate(ROL rol) {

			if (logger.isDebugEnabled()) {
				logger.debug("received ROL setParent mission from core " + rol);
			}

			if (!(parameters instanceof MissionHierarchy)) {
				throw new IllegalArgumentException("invalid parameters object type for mission setParent action: "
						+ parameters.getClass().getSimpleName());
			}

			MissionHierarchy missionHierarchy = (MissionHierarchy) parameters;

			if (Strings.isNullOrEmpty(missionHierarchy.getParentMissionName())) {
				missionService.clearParent(missionHierarchy.getMissionName(),
						remoteUtil.bitVectorToString(remoteUtil.getBitVectorForGroups(groups)));
			} else {
				missionService.setParent(missionHierarchy.getMissionName(), missionHierarchy.getParentMissionName(),
						remoteUtil.bitVectorToString(remoteUtil.getBitVectorForGroups(groups)));
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
				syncService.insertResource(resource.toMetadata(), content, remoteUtil.bitVectorToString(remoteUtil.getBitVectorForGroups(groups)));
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
}
