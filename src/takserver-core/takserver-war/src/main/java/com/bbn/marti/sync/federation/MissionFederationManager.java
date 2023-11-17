package com.bbn.marti.sync.federation;

import java.io.InputStream;
import java.util.NavigableSet;

import com.bbn.marti.maplayer.model.MapLayer;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.sync.MissionContent;
import com.bbn.marti.remote.sync.MissionUpdateDetailsForMapLayer;
import com.bbn.marti.remote.sync.MissionUpdateDetailsForMissionLayer;
import com.bbn.marti.sync.Metadata;
import com.bbn.marti.sync.model.Mission;
import com.bbn.marti.sync.model.MissionLayer;

import mil.af.rl.rol.value.DataFeedMetadata;
import mil.af.rl.rol.value.MissionMetadata;

/*
 * Mission Federation Manager interface.
 * 
 * Provide access to primitives for manipulating missions and enterprise sync resources, that are used by the mission / esync federation subsystem.
 * 
 * 
 */
public interface MissionFederationManager {
	
	void createMission(MissionMetadata missionMeta, NavigableSet<Group> groups);

	void deleteMission(String name, String creatorUid, NavigableSet<Group> groups);

	void addMissionContent(String missionName, MissionContent content, String creatorUid, NavigableSet<Group> groups);
	
	void createMissionFeed(Mission mission, DataFeedMetadata missionMeta, NavigableSet<Group> groups);
	
	void updateMissionFeed(Mission mission, DataFeedMetadata missionMeta, NavigableSet<Group> groups);

	void deleteMissionFeed(Mission mission, DataFeedMetadata missionMeta, NavigableSet<Group> groups);
	
	void createDataFeed(DataFeedMetadata feedMeta, NavigableSet<Group> groups);
	
	void updateDataFeed(DataFeedMetadata feedMeta, NavigableSet<Group> groups);

	void deleteDataFeed(DataFeedMetadata feedMeta, NavigableSet<Group> groups);

	void deleteMissionContent(String missionName, String hash, String uid, String creatorUid, NavigableSet<Group> groups);

	void archiveMission(String missionName, String serverName, NavigableSet<Group> groups);
	
	void insertResource(Metadata metadata, byte[] content, NavigableSet<Group> groups);

	void insertResource(Metadata metadata, InputStream contentStream, NavigableSet<Group> groups);

	void updateMetadata(String hash, String key, String value, NavigableSet<Group> groups);

	void setParent(String missionName, String parentMissionName, NavigableSet<Group> groups);

	void clearParent(String missionName, NavigableSet<Group> groups);
	
	void setExpiration(String missionName, Long expiration, NavigableSet<Group> groups);

	void addMissionLayer(MissionUpdateDetailsForMissionLayer missionUpdateDetailsForMissionLayer, NavigableSet<Group> groups);

	void deleteMissionLayer(MissionUpdateDetailsForMissionLayer missionUpdateDetailsForMissionLayer, NavigableSet<Group> groups);
	
	void addMapLayerToMission(MissionUpdateDetailsForMapLayer missionUpdateDetailsForMapLayer, NavigableSet<Group> groups);
	
	void updateMapLayer(MissionUpdateDetailsForMapLayer missionUpdateDetailsForMapLayer, NavigableSet<Group> groups);
	
	void removeMapLayerFromMission(MissionUpdateDetailsForMapLayer missionUpdateDetailsForMapLayer, NavigableSet<Group> groups);
	
}

    