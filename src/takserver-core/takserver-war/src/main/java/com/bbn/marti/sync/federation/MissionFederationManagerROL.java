package com.bbn.marti.sync.federation;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.NavigableSet;

import javax.naming.NamingException;

import com.bbn.marti.remote.config.CoreConfigFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.atakmap.Tak.ROL;
import com.atakmap.Tak.ROL.Builder;
import com.bbn.marti.maplayer.model.MapLayer;
import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.remote.FederationManager;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.sync.MissionContent;
import com.bbn.marti.remote.sync.MissionExpiration;
import com.bbn.marti.remote.sync.MissionHierarchy;
import com.bbn.marti.remote.sync.MissionUpdateDetailsForMapLayer;
import com.bbn.marti.remote.sync.MissionUpdateDetailsForMissionLayer;
import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.marti.sync.DataPackageFileBlocker;
import com.bbn.marti.sync.EnterpriseSyncService;
import com.bbn.marti.sync.Metadata;
import com.bbn.marti.sync.Metadata.Field;
import com.bbn.marti.sync.model.Mission;
import com.bbn.marti.sync.model.MissionChange;
import com.bbn.marti.sync.model.MissionLayer;
import com.bbn.marti.sync.model.MissionLayer.Type;
import com.bbn.marti.sync.service.MissionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;

import mil.af.rl.rol.value.DataFeedMetadata;
import mil.af.rl.rol.value.MissionMetadata;

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
	protected EnterpriseSyncService syncService;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private RemoteUtil remoteUtil;

	private final MissionActionROLConverter malrc;

	public MissionFederationManagerROL(MissionActionROLConverter malrc) {
		this.malrc = malrc;
	}

	@Override
	public void createMission(MissionMetadata missionMeta, NavigableSet<Group> groups) {
		if (!(CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation().isAllowMissionFederation())) {
			return;
		}

		try {
			if (!isMissionAllowed(missionMeta.getTool())) {
				if (logger.isDebugEnabled()) {
					logger.debug("not federation mission " + missionMeta.getName() + " with tool of " + missionMeta.getTool());
				}
				return;
			}

			try {
				if (CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation().isAllowMissionFederation()) {
					fedMgr.submitMissionFederateROL(malrc.createMissionToROL(missionMeta), groups, missionMeta.getName());
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

		if (!(CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation().isAllowMissionFederation())) {
			return;
		}

		try {

			if (logger.isDebugEnabled()) {
				logger.debug("intercepted mission delete " + name + " " + creatorUid + " " + groups);
			}

			try {
				fedMgr.submitMissionFederateROL(malrc.deleteMissionToROL(name, creatorUid), groups, name);
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
	public void createMissionFeed(Mission mission, DataFeedMetadata missionMeta, NavigableSet<Group> groups) {
		if (!(CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation().isAllowMissionFederation()) ||
				!(CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation().isAllowDataFeedFederation())) {
			return;
		}

		try {
			if (isMissionAllowed(mission.getTool())) {
				fedMgr.submitMissionFederateROL(malrc.createDataFeedToROL(missionMeta), groups, mission.getName());
			} else {
				if (logger.isDebugEnabled()) {
					logger.debug("not federating non-public mission action for mission " + mission.getName());
				}
			}
		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception constructing or sending feed create federated ROL", e);
			}
		}

	}

	@Override
	public void updateMissionFeed(Mission mission, DataFeedMetadata missionMeta, NavigableSet<Group> groups) {
		if (!(CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation().isAllowMissionFederation()) ||
				!(CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation().isAllowDataFeedFederation())) {
			return;
		}

		try {
			if (isMissionAllowed(mission.getTool())) {
				fedMgr.submitMissionFederateROL(malrc.updateDataFeedToROL(missionMeta), groups, mission.getName());
			} else {
				if (logger.isDebugEnabled()) {
					logger.debug("not federating non-public mission action for mission " + mission.getName());
				}
			}
		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception constructing or sending feed update federated ROL", e);
			}
		}

	}

	@Override
	public void deleteMissionFeed(Mission mission, DataFeedMetadata missionMeta, NavigableSet<Group> groups) {
		if (!(CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation().isAllowMissionFederation()) ||
				!(CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation().isAllowDataFeedFederation())) {
			return;
		}

		try {
			if (isMissionAllowed(mission.getTool())) {
				fedMgr.submitMissionFederateROL(malrc.deleteDataFeedToROL(missionMeta), groups, mission.getName());
			} else {
				if (logger.isDebugEnabled()) {
					logger.debug("not federating non-public mission action for mission " + mission.getName());
				}
			}
		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception constructing or sending feed delete federated ROL", e);
			}
		}
	}

	@Override
	public void createDataFeed(DataFeedMetadata feedMeta, NavigableSet<Group> groups) {
		if (!(CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation().isAllowDataFeedFederation())) {
			return;
		}

		try {
			fedMgr.submitFederateROL(malrc.createDataFeedToROL(feedMeta), groups);
		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception constructing or sending feed create federated ROL", e);
			}
		}
	}

	@Override
	public void updateDataFeed(DataFeedMetadata feedMeta, NavigableSet<Group> groups) {
		if (!(CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation().isAllowDataFeedFederation())) {
			return;
		}

		try {
			fedMgr.submitFederateROL(malrc.updateDataFeedToROL(feedMeta), groups);
		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception constructing or sending feed update federated ROL", e);
			}
		}
	}

	@Override
	public void deleteDataFeed(DataFeedMetadata feedMeta, NavigableSet<Group> groups) {
		if (!(CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation().isAllowDataFeedFederation())) {
			return;
		}

		try {
			fedMgr.submitFederateROL(malrc.deleteDataFeedToROL(feedMeta), groups);
		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception constructing or sending feed delete federated ROL", e);
			}
		}
	}

	@Override
	public void addMissionContent(String missionName, MissionContent content, String creatorUid, NavigableSet<Group> groups) {

		if (!(CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation().isAllowMissionFederation())) {
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

			if (!isMissionAllowed(mission.getTool())) {
				if (logger.isDebugEnabled()) {
					logger.debug("not federating non-public mission action for mission " + missionName);
				}
				return;
			}
			// TODO: this does not handle the case where the content is a zip format file, e.g, ODT or PDF?
			if (isBlockedFileEnabled()) {
				MissionChange latestMission = missionService.getLatestMissionChangeForContentHash(missionName, content.getHashes().get(0));
				String fileExt = "." + CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation().getFileFilter().getFileExtension().get(0).trim().toLowerCase();

//				MissionChangeUtils.findAndSetContentResource(latestMission);
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
				fedMgr.submitMissionFederateROL(malrc.addMissionContentToROL(content, missionName, creatorUid, mission), groups, missionName);
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

		if (!(CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation().isAllowMissionFederation())) {
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

			if (!isMissionAllowed(mission.getTool())) {
				if (logger.isDebugEnabled()) {
					logger.debug("not federating delete command for non-public mission " + missionName);
				}
				return;
			}

			try {
				fedMgr.submitMissionFederateROL(malrc.deleteMissionContentToROL(missionName, hash, uid, creatorUid, mission), groups, missionName);
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

		if (!(CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation().isAllowMissionFederation())) {
			return;
		}

		try {
			MissionHierarchy missionHierarchy = new MissionHierarchy();
			missionHierarchy.setMissionName(missionName);
			missionHierarchy.setParentMissionName(parentMissionName);

			if (logger.isDebugEnabled()) {
				logger.debug("federated setParent " + missionName + "," + parentMissionName + ", groups: " + groups);
			}

			try {
				fedMgr.submitMissionFederateROL(malrc.setParentToROL(missionHierarchy), groups, missionName);
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

		if (!(CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation().isAllowMissionFederation())) {
			return;
		}

		try {
			MissionHierarchy missionHierarchy = new MissionHierarchy();
			missionHierarchy.setMissionName(missionName);

			if (logger.isDebugEnabled()) {
				logger.debug("federated clearParent " + missionName + ", groups: " + groups);
			}

			try {
				fedMgr.submitMissionFederateROL(malrc.setParentToROL(missionHierarchy), groups, missionName);
			} catch (Exception e) {
				logger.warn("remote exception sending ROL", e);
			}

		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception constructing or sending mission clearParent federated ROL", e);
			}
		}
	}

	/*
	 * save file with payload from byte array
	 */
	@Override
	public void insertResource(Metadata metadata, byte[] content, NavigableSet<Group> groups) {

		if (metadata == null) {
			return;
		}

		if (!(CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation().isAllowMissionFederation())) {

			if (logger.isDebugEnabled()) {
				logger.debug("skipping federation of a file  " +  metadata);
			}

			return;
		}

		try {
			if (CoreConfigFacade.getInstance().getRemoteConfiguration().getNetwork().isEsyncEnableCotFilter()) {
				String cotFilter = CoreConfigFacade.getInstance().getRemoteConfiguration().getNetwork().getEsyncCotFilter();
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
			String fileExt = CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation().getFileFilter().getFileExtension().get(0).trim().toLowerCase();
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
				String hash = metadata.getFirst(Field.Hash);

				fedMgr.submitFederateROL(malrc.getInsertResourceROLNoContent(metadata), groups, hash);
			} catch (IOException e) {
				logger.error("exception federating enterprise sync insert resource", e);
			}
		}
	}

	/*
	 * save file with payload from InputStream
	 */
	@Override
	public void insertResource(Metadata metadata, InputStream contentStream, NavigableSet<Group> groups) {

		if (metadata == null) {
			logger.warn("not federating file - null metadata");
			return;
		}

		if (Strings.isNullOrEmpty(metadata.getHash())) {
			logger.warn("not federating file - blank hash");
			return;
		}

		if (logger.isDebugEnabled()) {
			logger.debug("metadata json for federated insert resource: " + metadata.toJSONObject() + " hash: " + metadata.getHash());
		}

		if (!(CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation().isAllowMissionFederation())) {

			if (logger.isDebugEnabled()) {
				logger.debug("skipping federation of a file  " +  metadata);
			}
			return;
		}


		byte[] content = null;

		// need to get content from database / cache instead of being passed in above.
		try {
			if (CoreConfigFacade.getInstance().getRemoteConfiguration().getNetwork().isEsyncEnableCotFilter()) {
				String cotFilter = CoreConfigFacade.getInstance().getRemoteConfiguration().getNetwork().getEsyncCotFilter();

				try {
					content = syncService.getContentByHash(metadata.getHash(), remoteUtil.bitVectorToString(RemoteUtil.getInstance().getBitVectorForGroups(groups)));
				} catch (SQLException | NamingException e) {
					logger.error("exception fetching content for file " + metadata.getHash() + " - not federating this file");
					return;
				}

				if (cotFilter != null && cotFilter.length() > 0) {

					if (content == null || content.length == 0) {
						logger.warn("not trying to federate empty file");
						return;
					}

					content = DataPackageFileBlocker.blockCoT(metadata, content, cotFilter);
					if (content == null) {
						if (logger.isDebugEnabled()) {  // i.e., the content was blocked
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
			try {
				content = syncService.getContentByHash(metadata.getHash(), remoteUtil.bitVectorToString(RemoteUtil.getInstance().getBitVectorForGroups(groups)));
			} catch (SQLException | NamingException e) {
				logger.error("exception fetching content for file " + metadata.getHash() + " - not federating this file");
				return;
			}
			String fileExt = CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation().getFileFilter().getFileExtension().get(0).trim().toLowerCase();
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
				String hash = metadata.getFirst(Field.Hash);

				fedMgr.submitFederateROL(malrc.getInsertResourceROLNoContent(metadata), groups, hash);
			} catch (IOException e) {
				logger.error("exception federating enterprise sync insert resource", e);
			}
		}
	}

	@Override
	public void updateMetadata(String hash, String key, String value, NavigableSet<Group> groups) {

		if (!(CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation().isAllowMissionFederation())) {
			return;
		}

		try {
			fedMgr.submitFederateROL(malrc.getUpdateMetadataROL(hash, key, value), groups);
		} catch (Exception e) {
			logger.error("exception federating enterprise sync update resource", e);
		}
	}

	@Override
	public void setExpiration(String missionName, Long expiration, NavigableSet<Group> groups) {

		if (!(CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation().isAllowMissionFederation())) {
			return;
		}

		try {
			MissionExpiration missionExpiration = new MissionExpiration();
			missionExpiration.setMissionName(missionName);
			missionExpiration.setMissionExpiration(expiration);

			if (logger.isDebugEnabled()) {
				logger.debug("federated setExpiration " + missionName + ", expiration" + expiration);
			}

			try {
				fedMgr.submitMissionFederateROL(malrc.setMissionExpirationToROL(missionExpiration), groups, missionName);
			} catch (Exception e) {
				logger.warn("remote exception sending ROL", e);
			}

		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception constructing or sending mission setExpiration federated ROL", e);
			}
		}
	}

	@Override
	public void addMissionLayer(MissionUpdateDetailsForMissionLayer missionUpdateDetailsForMissionLayer, NavigableSet<Group> groups) {

		if (!(CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation().isAllowMissionFederation())) {
			return;
		}

		try {

			fedMgr.submitMissionFederateROL(malrc.addMissionLayerToROL(missionUpdateDetailsForMissionLayer), groups, missionUpdateDetailsForMissionLayer.getMission().getName());

		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception constructing or sending addMissionLayer federated ROL", e);
			}
		}
	}

	@Override
	public void deleteMissionLayer(MissionUpdateDetailsForMissionLayer missionUpdateDetailsForMissionLayer, NavigableSet<Group> groups) {

		if (!(CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation().isAllowMissionFederation())) {
			return;
		}

		try {

			if (logger.isDebugEnabled()) {
				logger.debug("intercepted deleteMissionLayer: {}", missionUpdateDetailsForMissionLayer.getLayerUid());
			}

			fedMgr.submitMissionFederateROL(malrc.deleteMissionLayerToROL(missionUpdateDetailsForMissionLayer), groups, missionUpdateDetailsForMissionLayer.getMission().getName());

		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception constructing or sending deleteMissionLayer federated ROL", e);
			}
		}

	}

	@Override
	public void addMapLayerToMission(MissionUpdateDetailsForMapLayer missionUpdateDetailsForMapLayer, NavigableSet<Group> groups) {

		if (!(CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation().isAllowMissionFederation())) {
			return;
		}

		try {

			fedMgr.submitMissionFederateROL(malrc.addMapLayerToMissionToROL(missionUpdateDetailsForMapLayer), groups, missionUpdateDetailsForMapLayer.getMission().getName());

		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception constructing or sending addMapLayer federated ROL", e);
			}
		}

	}

	@Override
	public void updateMapLayer(MissionUpdateDetailsForMapLayer missionUpdateDetailsForMapLayer, NavigableSet<Group> groups) {

		if (!(CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation().isAllowMissionFederation())) {
			return;
		}

		try {

			fedMgr.submitMissionFederateROL(malrc.updateMapLayerToROL(missionUpdateDetailsForMapLayer), groups, missionUpdateDetailsForMapLayer.getMission().getName());

		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception constructing or sending updateMapLayer federated ROL", e);
			}
		}
	}

	@Override
	public void removeMapLayerFromMission(MissionUpdateDetailsForMapLayer missionUpdateDetailsForMapLayer, NavigableSet<Group> groups) {

		if (!(CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation().isAllowMissionFederation())) {
			return;
		}

		try {

			fedMgr.submitMissionFederateROL(malrc.removeMapLayerFromMissionToROL(missionUpdateDetailsForMapLayer), groups, missionUpdateDetailsForMapLayer.getMission().getName());

		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception constructing or sending updateMapLayer federated ROL", e);
			}
		}

	}

	private boolean isBlockedFileEnabled() {
		String fileExt = CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation().getFileFilter().getFileExtension().get(0).trim().toLowerCase();
		return (CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation().isEnableDataPackageAndMissionFileFilter() &&
				(! fileExt.isEmpty()));
	}

	private boolean isMissionAllowed(String tool) {
		tool = tool == null ? "public" : tool;
		boolean onlyFederatePublic = CoreConfigFacade.getInstance().getRemoteConfiguration().getFederation().isFederateOnlyPublicMissions();
		boolean isVBM = CoreConfigFacade.getInstance().getRemoteConfiguration().getVbm().isEnabled();

		// federate all missions
		if (!onlyFederatePublic) return true;

		else if (tool.equals("public")) {
			return true;
		}
		// if tool == cop attribute, always allow if vbm is enabled
		else if (tool.equals(CoreConfigFacade.getInstance().getRemoteConfiguration().getNetwork().getMissionCopTool())) {
			return isVBM;
		}
		// disallow if no conditions are met
		else {
			return false;
		}
	}

}
