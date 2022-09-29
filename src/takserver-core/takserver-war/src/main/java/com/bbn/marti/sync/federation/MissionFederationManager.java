package com.bbn.marti.sync.federation;

import java.util.NavigableSet;

import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.sync.MissionContent;
import com.bbn.marti.sync.Metadata;

/*
 * Mission Federation Manager interface.
 * 
 * Provide access to primitives for manipulating missions and enterprise sync resources, that are used by the mission / esync federation subsystem.
 * 
 * 
 */
public interface MissionFederationManager {
	
	void createMission(String name, String creatorUid, String description, String chatRoom, String tool, NavigableSet<Group> groups);

	void deleteMission(String name, String creatorUid, NavigableSet<Group> groups);

	void addMissionContent(String missionName, MissionContent content, String creatorUid, NavigableSet<Group> groups);

	void deleteMissionContent(String missionName, String hash, String uid, String creatorUid, NavigableSet<Group> groups);

	void archiveMission(String missionName, String serverName, NavigableSet<Group> groups);

	void insertResource(Metadata metadata, byte[] content, NavigableSet<Group> groups);

	void updateMetadata(String hash, String key, String value, NavigableSet<Group> groups);

	void setParent(String missionName, String parentMissionName, NavigableSet<Group> groups);

	void clearParent(String missionName, NavigableSet<Group> groups);

}

    