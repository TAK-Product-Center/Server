package com.bbn.marti.sync.federation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atakmap.Tak.BinaryBlob;
import com.atakmap.Tak.ROL;
import com.atakmap.Tak.ROL.Builder;
import com.bbn.marti.remote.sync.MissionChangeType;
import com.bbn.marti.remote.sync.MissionContent;
import com.bbn.marti.remote.sync.MissionUpdateDetails;
import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.marti.sync.Metadata;
import com.bbn.marti.sync.model.Mission;
import com.bbn.marti.sync.model.Resource;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;

import mil.af.rl.rol.value.DataFeedMetadata;
import mil.af.rl.rol.value.MissionMetadata;

public class MissionActionROLConverter {
	
	private final RemoteUtil remoteUtil;
	
	private final ObjectMapper mapper;
	
	private static final Logger logger = LoggerFactory.getLogger(MissionActionROLConverter.class);
	
	public MissionActionROLConverter(RemoteUtil remoteUtil, ObjectMapper mapper) {
		this.remoteUtil = remoteUtil;
		this.mapper = mapper;
	}
	
	public MissionMetadata missionToROLMissionMetadata(Mission mission) {
		MissionMetadata mc = new MissionMetadata();
		mc.setName(mission.getName());
		mc.setCreatorUid(mission.getCreatorUid());
		mc.setDescription(mission.getDescription());
		mc.setChatRoom(mission.getChatRoom());
		mc.setTool(mission.getTool());
		mc.setBbox(mission.getBbox());
		mc.setBoundingPolygon(mission.getBoundingPolygon());
		
		if (mission.getParent() != null)
			mc.setParentMissionId(mission.getParent().getId());
		
		mc.setPasswordHash(mission.getPasswordHash());
		mc.setPath(mission.getPath());
		mc.setClassification(mission.getClassification());
		mc.setBaseLayer(mission.getBaseLayer());
		
		if (mission.getDefaultRole() != null)
			mc.setDefaultRoleId(mission.getDefaultRole().getId());
		
		mc.setExpiration(mission.getExpiration());
		
		return mc;
	}

	public ROL createMissionToROL(MissionMetadata mc) throws JsonProcessingException {		
		return ROL.newBuilder().setProgram("create mission\n" + mapper.writeValueAsString(mc) + ";").build();
	}
	
	
	public ROL deleteMissionToROL(String name, String creatorUid) throws JsonProcessingException {
		
		MissionMetadata mc = new MissionMetadata();

		mc.setName(name);
		mc.setCreatorUid(creatorUid);
		
		return ROL.newBuilder().setProgram("delete mission\n" + mapper.writeValueAsString(mc) + ";").build();
	}
	
	public ROL createDataFeedToROL(DataFeedMetadata meta) throws JsonProcessingException {		
		return ROL.newBuilder().setProgram("create data_feed\n" + mapper.writeValueAsString(meta) + ";").build();
	}
	
	public ROL updateDataFeedToROL(DataFeedMetadata meta) throws JsonProcessingException {		
		return ROL.newBuilder().setProgram("update data_feed\n" + mapper.writeValueAsString(meta) + ";").build();
	}
	
	public ROL deleteDataFeedToROL(DataFeedMetadata meta) throws JsonProcessingException {		
		return ROL.newBuilder().setProgram("delete data_feed\n" + mapper.writeValueAsString(meta) + ";").build();
	}
	
	public ROL addMissionContentToROL(MissionContent content, String missionName, String creatorUid, Mission mission) {
		
		return remoteUtil.getROLforMissionChange(content, missionName, creatorUid, mission.getCreatorUid(), mission.getChatRoom(), mission.getTool(), mission.getDescription());
	}
	
	public ROL deleteMissionContentToROL(String missionName, String hash, String uid, String creatorUid, Mission mission) throws JsonProcessingException {
		
		MissionContent content = new MissionContent();
		
		if (hash != null) {
			content.getHashes().add(hash);
		}
		
		if (uid != null) {
			content.getUids().add(uid);
		}
		
		
		MissionUpdateDetails contentDetails = new MissionUpdateDetails();
		
		contentDetails.setContent(content);
		contentDetails.setMissionName(missionName);
		contentDetails.setCreatorUid(creatorUid);
		contentDetails.setChangeType(MissionChangeType.REMOVE_CONTENT);
		
		contentDetails.setMissionCreatorUid(mission.getCreatorUid());
		contentDetails.setMissionChatRoom(mission.getChatRoom());
		contentDetails.setMissionTool(mission.getTool());
		contentDetails.setMissionDescription(mission.getDescription());
		
		String contentDetailsJson = mapper.writeValueAsString(contentDetails);
	
		return ROL.newBuilder().setProgram("update mission\n" + contentDetailsJson + ";").build();
		
	}
	
	/*
	 * generate save file ROL with content attached
	 */
		public ROL getInsertResourceROL(Metadata metadata, byte[] content) throws IOException {
		
		Resource res = new Resource(metadata);
		String metadataJson = mapper.writeValueAsString(res);

		if (logger.isDebugEnabled()) {
			logger.debug("intercepted enterprise sync insert resource " + metadataJson + " size: " + content.length);
		}

		Objects.requireNonNull(metadataJson, "resource metadata");
		Objects.requireNonNull(content, "resource content bytes");
		
		Builder rol = ROL.newBuilder().setProgram("create resource\n" + metadataJson + ";");
		
		BinaryBlob file = BinaryBlob.newBuilder().setData(ByteString.readFrom(new ByteArrayInputStream(content))).build();

        rol.addPayload(file);
        
        return rol.build();
		
	}	
	
	/*
	 * generate save file ROL with only metadata attached
	 */
	public ROL getInsertResourceROLNoContent(Metadata metadata) throws IOException {
		
		Resource res = new Resource(metadata);
		String metadataJson = mapper.writeValueAsString(res);

		if (logger.isDebugEnabled()) {
			logger.debug("intercepted enterprise sync insert resource (without content)" + metadataJson);
		}

		Objects.requireNonNull(metadataJson, "resource metadata");
		
		Builder rol = ROL.newBuilder().setProgram("create resource\n" + metadataJson + ";");
		        
        return rol.build();		
	}	

	public ROL getUpdateMetadataROL(String hash, String key, String value) throws IOException {

		Metadata metadata = new Metadata();

		// do a case-insensitive match on the metadata field since we store lowercase in the database
		Metadata.Field setField = null;
		for (Metadata.Field field : Metadata.Field.class.getEnumConstants()) {
			if (field.name().equalsIgnoreCase(key)) {
				setField = field;
				break;
			}
		}

		if (setField == null) {
			logger.error("unable to fine matching metadata field for ROL!");
			return null;
		}

		metadata.set(Metadata.Field.Hash, hash);
		metadata.set(setField, value);

		Resource res = new Resource(metadata);
		String metadataJson = mapper.writeValueAsString(res);

		if (logger.isDebugEnabled()) {
			logger.debug("intercepted enterprise sync update resource " + metadataJson);
		}

		Objects.requireNonNull(metadataJson, "resource metadata");

		Builder rol = ROL.newBuilder().setProgram("update resource\n" + metadataJson + ";");

		return rol.build();
	}
}
