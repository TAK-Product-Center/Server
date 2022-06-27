package com.bbn.marti.remote.service;

import com.bbn.marti.remote.MissionArchiveConfig;

public interface MissionArchiveManager {
	
	String getMissionArchive();
	
	String restoreMissionFromArchive(int id);
	
	MissionArchiveConfig getMissionArchiveConfig();
	
	void updateMissionArchiveConfig(MissionArchiveConfig missionArchiveConfig);

}
