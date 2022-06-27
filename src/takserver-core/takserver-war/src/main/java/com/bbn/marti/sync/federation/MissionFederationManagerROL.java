package com.bbn.marti.sync.federation;

import java.io.IOException;
import java.util.Locale;
import java.util.NavigableSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.atakmap.Tak.ROL;
import com.atakmap.Tak.ROL.Builder;
import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.remote.FederationManager;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.sync.MissionContent;
import com.bbn.marti.remote.sync.MissionHierarchy;
import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.marti.sync.DataPackageFileBlocker;
import com.bbn.marti.sync.EnterpriseSyncService;
import com.bbn.marti.sync.Metadata;
import com.bbn.marti.sync.model.Mission;
import com.bbn.marti.sync.model.MissionChange;
import com.bbn.marti.sync.service.MissionService;
import com.fasterxml.jackson.databind.ObjectMapper;

/*
 * Mission federation manager implementation that federates ROL-encoded representations of Mission API commands.
 * 
 * 
 */
public class MissionFederationManagerROL implements MissionFederationManager {

	private static final Logger logger = LoggerFactory.getLogger(MissionFederationManagerROL.class);

	@Autowired
	FederationManager fedMgr;

	@Autowired
	MissionService missionService;

	@Autowired
	protected EnterpriseSyncService persistenceStore;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private RemoteUtil remoteUtil;

	@Autowired
	private CoreConfig coreConfig;

	private final MissionActionROLConverter malrc;

	public MissionFederationManagerROL(MissionActionROLConverter malrc) {
		this.malrc = malrc;
	}

	@Override
	public void createMission(String name, String creatorUid, String description, String chatRoom, String tool, NavigableSet<Group> groups) {

		if (!(coreConfig.getRemoteConfiguration().getFederation().isAllowMissionFederation())) {
			return;
		}
		
		try {
			if (coreConfig.getRemoteConfiguration().getFederation().isFederateOnlyPublicMissions() && tool != null && !tool.toLowerCase(Locale.ENGLISH).equals("public")) {
				if (logger.isDebugEnabled()) {
					logger.debug("not federation mission " + name + " with tool of " + tool);
				}
				return;
			}

			try {
				if (coreConfig.getRemoteConfiguration().getFederation().isAllowMissionFederation()) {
					fedMgr.submitFederateROL(malrc.createMissionToROL(name, creatorUid, description, chatRoom, tool), groups);
				}
			} catch (Exception e) {
				logger.warn("remote exception sending ROL", e);
			}

		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception constructing or sending mission create federated ROL", e);
			}
		}
	}

	@Override
	public void deleteMission(String name, String creatorUid, NavigableSet<Group> groups) {

		if (!(coreConfig.getRemoteConfiguration().getFederation().isAllowMissionFederation())) {
			return;
		}
		
		try {

			if (logger.isDebugEnabled()) {
				logger.debug("intercepted mission delete " + name + " " + creatorUid + " " + groups);
			}

			try {
				fedMgr.submitFederateROL(malrc.deleteMissionToROL(name, creatorUid), groups);
			} catch (Exception e) {
				logger.warn("remote exception sending ROL", e);
			}
		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception constructing or sending mission create federated ROL", e);
			}
		}
	}

	@Override
	public void addMissionContent(String missionName, MissionContent content, String creatorUid, NavigableSet<Group> groups) {

		if (!(coreConfig.getRemoteConfiguration().getFederation().isAllowMissionFederation())) {
			return;
		}


		if (logger.isDebugEnabled()) {
			logger.debug("intercepted add mission content: " + content + " mission name " + missionName + " creator uid " + creatorUid);
		}

		try {

			// include mission metadata in federated add content, so that the mission can be created on the federate, if it does not exist
			Mission mission = missionService.getMission(missionName, remoteUtil.bitVectorToString(RemoteUtil.getInstance().getBitVectorForGroups(groups)));

			if (mission == null) {
				throw new IllegalArgumentException("can't federate mission change for mission " + missionName + " that does not exist");
			}

			if (coreConfig.getRemoteConfiguration().getFederation().isFederateOnlyPublicMissions() && mission.getTool() != null && !mission.getTool().equals("public")) {
				if (logger.isDebugEnabled()) {
					logger.debug("not federating non-public mission action for mission " + missionName);
				}
				return;
			}
            // TODO: this does not handle the case where the content is a zip format file, e.g, ODT or PDF?
			if (isBlockedFileEnabled()) {
				MissionChange latestMission = missionService.getLatestMissionChangeForContentHash(missionName, content.getHashes().get(0));
				String fileExt = "." + coreConfig.getRemoteConfiguration().getFederation().getFileFilter().getFileExtension().get(0).trim().toLowerCase();
				String name = latestMission.getContentResource().getName();
				// TODO do we need to check file name instead?
				if (name != null) {
					if (name.endsWith(fileExt)) {
						logger.debug(" not federating blocked file in add mission content " + name + " file ext: " + fileExt);
						return;
					}
				}
			}

			try {
				fedMgr.submitFederateROL(malrc.addMissionContentToROL(content, missionName, creatorUid, mission), groups);
			} catch (Exception e) {
				logger.warn("remote exception sending ROL", e);
			}

		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception constructing or sending mission create federated ROL", e);
			}
		}
	}

	@Override
	public void deleteMissionContent(String missionName, String hash, String uid, String creatorUid, NavigableSet<Group> groups) {

		if (!(coreConfig.getRemoteConfiguration().getFederation().isAllowMissionFederation())) {
			return;
		}
		
		if (logger.isDebugEnabled()) {
			logger.debug("intercepted delete mission content: hash: " + hash + " uid: " + uid + " mission name " + missionName + " creator uid " + creatorUid + " groups " + groups);
		}

		try {

			// include mission metadata in federated add content, so that the mission can be created on the federate, if it does not exist
			Mission mission = missionService.getMission(missionName, remoteUtil.bitVectorToString(RemoteUtil.getInstance().getBitVectorForGroups(groups)));

			if (mission == null) {
				throw new IllegalArgumentException("can't federate mission change for mission " + missionName + " that does not exist");
			}

			if (coreConfig.getRemoteConfiguration().getFederation().isFederateOnlyPublicMissions() && mission.getTool() != null && !mission.getTool().equals("public")) {
				if (logger.isDebugEnabled()) {
					logger.debug("not federating delete command for non-public mission " + missionName);
				}
				return;
			}

			try {
				fedMgr.submitFederateROL(malrc.deleteMissionContentToROL(missionName, hash, uid, creatorUid, mission), groups);
			} catch (Exception e) {
				logger.warn("remote exception sending ROL", e);
			}

		} catch (Exception e) {
			logger.debug("exception constructing or sending mission create federated ROL", e);
		}
	}

	@Override
	public void archiveMission(String missionName, String serverName, NavigableSet<Group> groups) { }

	@Override
	public void setParent(String missionName, String parentMissionName, NavigableSet<Group> groups) {
		
		if (!(coreConfig.getRemoteConfiguration().getFederation().isAllowMissionFederation())) {
			return;
		}
		
		try {
			MissionHierarchy missionHierarchy = new MissionHierarchy();
			missionHierarchy.setMissionName(missionName);
			missionHierarchy.setParentMissionName(parentMissionName);

			if (logger.isDebugEnabled()) {
				logger.debug("federated setParent " + missionName + "," + parentMissionName + ", groups: " + groups);
			}

			Builder rol = ROL.newBuilder().setProgram("assign mission\n" +
					mapper.writeValueAsString(missionHierarchy) + ";");

			try {
				fedMgr.submitFederateROL(rol.build(), groups);
			} catch (Exception e) {
				logger.warn("remote exception sending ROL", e);
			}

		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception constructing or sending mission setParent federated ROL", e);
			}
		}
	}

	@Override
	public void clearParent(String missionName, NavigableSet<Group> groups) {
		
		if (!(coreConfig.getRemoteConfiguration().getFederation().isAllowMissionFederation())) {
			return;
		}
		
		try {
			MissionHierarchy missionHierarchy = new MissionHierarchy();
			missionHierarchy.setMissionName(missionName);

			if (logger.isDebugEnabled()) {
				logger.debug("federated clearParent " + missionName + ", groups: " + groups);
			}

			Builder rol = ROL.newBuilder().setProgram("assign mission\n" +
					mapper.writeValueAsString(missionHierarchy) + ";");

			try {
				fedMgr.submitFederateROL(rol.build(), groups);
			} catch (Exception e) {
				logger.warn("remote exception sending ROL", e);
			}

		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception constructing or sending mission clearParent federated ROL", e);
			}
		}
	}

	@Override
	public void insertResource(Metadata metadata, byte[] content, NavigableSet<Group> groups) {
		
		if (!(coreConfig.getRemoteConfiguration().getFederation().isAllowMissionFederation())) {
			return;
		}

		try {
			if (coreConfig.getRemoteConfiguration().getNetwork().isEsyncEnableCotFilter()) {
				String cotFilter = coreConfig.getRemoteConfiguration().getNetwork().getEsyncCotFilter();
				if (cotFilter != null && cotFilter.length() > 0) {
					content = DataPackageFileBlocker.blockCoT(metadata, content, cotFilter);
					if (content == null) {
						if (logger.isDebugEnabled()) {
							logger.debug("filtered content is null, not federating enterprise sync insert resource");
						}
						return;
					}
				}
			}
		} catch (Exception e) {
			logger.error("exception federating enterprise sync insert resource", e);
		}

		if (isBlockedFileEnabled()) {
			String fileExt = coreConfig.getRemoteConfiguration().getFederation().getFileFilter().getFileExtension().get(0).trim().toLowerCase();
			try {
					byte[] filteredContent = DataPackageFileBlocker.blockResourceContent(metadata, content, fileExt);
					if (filteredContent != null) {
						fedMgr.submitFederateROL(malrc.getInsertResourceROL(metadata, filteredContent), groups);
					} else {
						if (logger.isDebugEnabled()) {
							logger.debug("filtered content is null, not federating enterprise sync insert resource");
						}
					}
				} catch (Exception e) {
					logger.error("exception federating enterprise sync insert resource", e);
				}
		} else { // just let it ROL
			try {
				fedMgr.submitFederateROL(malrc.getInsertResourceROL(metadata, content), groups);
			} catch (IOException e) {
				logger.error("exception federating enterprise sync insert resource", e);
			}
		}
	}

	@Override
	public void updateMetadata(String hash, String key, String value, NavigableSet<Group> groups) {

		if (!(coreConfig.getRemoteConfiguration().getFederation().isAllowMissionFederation())) {
			return;
		}

		try {
			fedMgr.submitFederateROL(malrc.getUpdateMetadataROL(hash, key, value), groups);
		} catch (Exception e) {
			logger.error("exception federating enterprise sync update resource", e);
		}
	}

	private boolean isBlockedFileEnabled() {
		String fileExt = coreConfig.getRemoteConfiguration().getFederation().getFileFilter().getFileExtension().get(0).trim().toLowerCase();
		return (coreConfig.getRemoteConfiguration().getFederation().isEnableDataPackageAndMissionFileFilter() &&
				(! fileExt.isEmpty()));
	}

}
