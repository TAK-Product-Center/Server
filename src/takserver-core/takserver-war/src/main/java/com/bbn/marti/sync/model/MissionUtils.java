package com.bbn.marti.sync.model;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MissionUtils {
	
    private static final Logger logger = LoggerFactory.getLogger(MissionUtils.class);

	public static void findAndSetTransientValuesForMission(Mission mission) {
	
		Set<MissionFeed> feeds  = mission.getFeeds();

		if (feeds != null) {
			for (MissionFeed feed: feeds) {
				MissionFeedUtils.findAndSetNameForMissionFeed(feed);
			}
		}
		
		Set<MissionChange> missionChanges = mission.getMissionChanges();
				
		if (missionChanges != null) {
			
	        for (MissionChange missionChange: missionChanges) {
				MissionChangeUtils.findAndSetTransientValuesForMissionChange(missionChange);
	        }
	        
		}
		
	}
}
