package com.bbn.marti.remote.service;

import java.util.List;
import java.util.Map;

import org.dom4j.Element;

public interface RetentionQueryService {

    void deleteMissionByExpiration(Long ttl);
    void deleteMissionByTtl(Integer ttl);
    void deleteMission(String name, String creatorUid, String groupVector, boolean deepDelete);
    void deleteMission(String name, String creatorUid, List<String> groups, boolean deepDelete);
    
    byte[] getArchivedMission(String missionName, String groupVector, String serverName);
         
    boolean restoreMission(Map<String, byte[]> files, Map<String, String> properties, List<String> groups, String defaultRole, List<String> defaultPermissions);
	void restoreCoT(String missionName, List<byte[]> files, List<String> groups);
	void restoreContent(String missionName, byte[] file, Element missionContent, List<String> groups) throws Exception;
}
