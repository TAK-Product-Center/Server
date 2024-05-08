package com.bbn.marti.service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.ignite.Ignite;
import org.apache.ignite.services.ServiceContext;
import org.dom4j.DocumentException;
import org.dom4j.Element;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.remote.service.RetentionQueryService;
import com.bbn.marti.remote.sync.MissionContent;
import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.marti.sync.Metadata;
import com.bbn.marti.sync.model.Mission;
import com.bbn.marti.sync.model.MissionRole;
import com.bbn.marti.sync.model.MissionRole.Role;
import com.bbn.marti.sync.service.MissionService;
import com.bbn.marti.util.MessagingDependencyInjectionProxy;

import tak.server.cot.CotEventContainer;
import tak.server.cot.CotParser;
import tak.server.system.ApiDependencyProxy;

public class DistributedRetentionQueryManager implements RetentionQueryService, org.apache.ignite.services.Service {

	private static final Logger logger = LoggerFactory.getLogger(DistributedRetentionQueryManager.class);

	Ignite ignite;

	GroupManager groupManager;

	// Currently this only contains the mission service
	public DistributedRetentionQueryManager(Ignite ignite, GroupManager groupManager) {
		this.ignite = ignite;
		this.groupManager = groupManager;
	}

	@Override
	public void deleteMissionByExpiration(Long expiration) {
		try {
			MissionService missionService = missionService();

			if (missionService == null) {
				logger.error(" mission service not ready yet " + missionService);
				return;
			}

			if (expiration == null || expiration.longValue() < 0) {
				if (logger.isWarnEnabled()) {
					logger.warn(" bad argument, delete MissionByExpiration is ignored expiration:  " + expiration);
				}
			}

			if (logger.isInfoEnabled()) {
				logger.info(" running deleteMissionByExpiration " + expiration);
			}

			missionService().deleteMissionByExpiration(expiration);

		} catch (Exception e) {
			logger.error("error running deleteMissionByExpiration ", e);
		}
	}

	@Override
	public void deleteMissionByTtl(Integer ttl) {
		try {
			MissionService missionService = missionService();

			if (missionService == null) {
				logger.error(" mission service not ready yet " + missionService);
				return;
			}

			if (ttl == null) {
				logger.info("delete MissionByTtl ttl is null, nothing to do:  " + ttl);
				return;
			}

			if (ttl.intValue() < 0) {
				if (logger.isWarnEnabled()) {
					logger.warn(" bad argument, delete MissionByTtl is ignored ttl:  " + ttl);
				}
				return;
			}

			if (logger.isInfoEnabled()) {
				logger.info(" running deleteMissionByTtl ttl:" + ttl);
			}
			missionService().deleteMissionByTtl(ttl);

		} catch (Exception e) {
			logger.error("error running deleteMissionByTtl ", e);
		}
	}

	@Override
	public byte[] getArchivedMission(String missionName, String groupVector, String serverName) {
		
		Mission m = missionService().getMissionByNameCheckGroups(missionName, groupVector);
		
		return missionService().archiveMission(m.getGuidAsUUID(), groupVector, serverName);
	}
	
	@Override
	public void deleteMission(String name, String creatorUid, List<String> groups, boolean deepDelete) {
		Set<Group> groupSet = groupManager.findGroups(groups);
		String groupVector = RemoteUtil.getInstance().bitVectorToString(RemoteUtil.getInstance().getBitVectorForGroups(groupSet));

		deleteMission(name, creatorUid, groupVector, deepDelete);
	}
	
	@Override
	public void deleteMission(String name, String creatorUid, String groupVector, boolean deepDelete) {
		MissionService missionService = missionService();

		if (missionService == null) {
			logger.error(" mission service not ready yet " + missionService);
			return;
		}

		missionService.deleteMission(name, creatorUid, groupVector, deepDelete);
	}

	private MissionService missionService() {
		return MessagingDependencyInjectionProxy.getInstance().missionService();
	}

	@Override
	public void cancel(ServiceContext ctx) {
		if (logger.isDebugEnabled()) {
			logger.debug(getClass().getSimpleName() + " service cancelled");
		}
	}

	@Override
	public void init(ServiceContext ctx) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("init method " + getClass().getSimpleName());
		}
	}

	@Override
	public void execute(ServiceContext ctx) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("execute method " + getClass().getSimpleName());
		}
	}

	@Override
	public boolean restoreMission(Map<String, byte[]> files, Map<String, String> properties, List<String> groups,
			String defaultRole, List<String> defaultPermissions) {

		MissionRole missionRole = null;
		if (!StringUtils.isEmpty(defaultRole)) {
			Role role = MissionRole.Role.valueOf(defaultRole);
			missionRole = ApiDependencyProxy.getInstance().missionRoleRepository().findFirstByRole(role);
		}

		Set<Group> groupSet = groupManager.findGroups(groups);
		String groupVector = RemoteUtil.getInstance()
				.bitVectorToString(RemoteUtil.getInstance().getBitVectorForGroups(groupSet));

		long expiration = Long.valueOf(properties.get("expiration"));
		if (expiration != -1L) {
			long createTime = Long.valueOf(properties.get("create_time"));
			// calculate expiration duration
			expiration = expiration - createTime;
			// add duration to current time
			expiration = new Date().getTime() + expiration;
		}

		if (missionService().getMission(properties.get("mission_name"), false) != null) {
			return false;
		}

		missionService().createMission(properties.get("mission_name"), properties.get("creatorUid"), groupVector,
				properties.get("description"), properties.get("chatroom"), properties.get("baseLayer"),
				properties.get("bbox"), properties.get("path"), properties.get("classification"), properties.get("tool"), properties.get("password_hash"), missionRole, expiration, properties.get("bounding_polygon"), Boolean.valueOf(properties.get("invite_only")));

		return true;
	}

	@Override
	public void restoreCoT(String missionName, List<byte[]> files, List<String> groups) {
		Set<Group> groupSet = groupManager.findGroups(groups);
		String groupVector = RemoteUtil.getInstance()
				.bitVectorToString(RemoteUtil.getInstance().getBitVectorForGroups(groupSet));

		CotParser parser = new CotParser(false);
		List<CotEventContainer> cotEvents = files.stream().map(file -> {
			try {
				return new CotEventContainer(parser.parse(new String(file)));
			} catch (DocumentException e) {
				return null;
			}
		}).filter(cot -> cot != null).collect(Collectors.toList());

		RepositoryService.getInstance().insertBatchCotData(cotEvents);

		Mission m = missionService().getMissionByNameCheckGroups(missionName, groupVector);
		
		final UUID missionGuid = m.getGuidAsUUID();
		
		cotEvents.forEach(cot -> {
			MissionContent missionContent = new MissionContent();
			missionContent.getUids().add(cot.getUid());
			missionService().addMissionContent(missionGuid, missionContent, cot.getUid().split("_mission_")[0],
					groupVector);
		});
	}

	@Override
	public void restoreContent(String missionName, byte[] file, Element missionContent, List<String> groups) throws Exception {
		Set<Group> groupSet = groupManager.findGroups(groups);
		String groupVector = RemoteUtil.getInstance()
				.bitVectorToString(RemoteUtil.getInstance().getBitVectorForGroups(groupSet));

		Metadata toStore = new Metadata();
		if (missionContent.attributeValue("altitude") != null) {
			toStore.set(Metadata.Field.Altitude, Double.valueOf(missionContent.attributeValue("altitude")));
		}
		if (missionContent.attributeValue("latitude") != null) {
			toStore.set(Metadata.Field.Latitude, Double.valueOf(missionContent.attributeValue("latitude")));
		}
		if (missionContent.attributeValue("longitude") != null) {
			toStore.set(Metadata.Field.Longitude, Double.valueOf(missionContent.attributeValue("longitude")));
		}
		if (missionContent.attributeValue("size") != null) {
			toStore.set(Metadata.Field.Size, Long.valueOf(missionContent.attributeValue("size")));
		}
		if (missionContent.attributeValue("submissionTime") != null) {
			toStore.set(Metadata.Field.SubmissionDateTime, missionContent.attributeValue("submissionTime"));
		}
		if (missionContent.attributeValue("creatorUid") != null) {
			toStore.set(Metadata.Field.CreatorUid, missionContent.attributeValue("creatorUid"));
		}
		if (missionContent.attributeValue("submitter") != null) {
			toStore.set(Metadata.Field.SubmissionUser, missionContent.attributeValue("submitter"));
		}
		if (missionContent.attributeValue("keywords") != null) {
			toStore.set(Metadata.Field.Keywords, missionContent.attributeValue("keywords").split(","));
		}
		if (missionContent.attributeValue("name") != null) {
			toStore.set(Metadata.Field.Name, missionContent.attributeValue("name"));
		}
		if (missionContent.attributeValue("mimeType") != null) {
			toStore.set(Metadata.Field.MIMEType, missionContent.attributeValue("mimeType"));
		}
		if (missionContent.attributeValue("tool") != null) {
			toStore.set(Metadata.Field.Tool, missionContent.attributeValue("tool"));
		}
		if (missionContent.attributeValue("filename") != null) {
			toStore.set(Metadata.Field.DownloadPath, missionContent.attributeValue("filename"));
		}

		toStore.set(Metadata.Field.UID, new String[] { UUID.randomUUID().toString() });
		
		Metadata fromStore = ApiDependencyProxy.getInstance().enterpriseSyncService().insertResource(toStore, file, groupVector);
		
		MissionContent mc = new MissionContent();
		mc.getHashes().add(toStore.getHash());
		mc.getUids().add(fromStore.getUid());
		
		Mission m = missionService().getMissionByNameCheckGroups(missionName, groupVector);

		missionService().addMissionContent(m.getGuidAsUUID(), mc, missionContent.attributeValue("creatorUid"), groupVector);
	}

	@Override
	public List<Mission> getAllMissions(boolean passwordProtected, boolean defaultRole, String tool)
			throws Exception {

		if (logger.isDebugEnabled()) {
			logger.debug(" getAllMissions service called");
		}

		NavigableSet<Group> allGroups = (NavigableSet<Group>) groupManager.getAllGroups();

		MissionService missionService = missionService();
		List<Mission> missions = missionService.getAllMissions(passwordProtected, defaultRole, tool, allGroups);
		return missions;
	}

}
