package com.bbn.marti.sync.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.sync.service.MissionService;
import com.bbn.marti.util.spring.SpringContextBeanForApi;

public class MissionFeedUtils {

    protected static final Logger logger = LoggerFactory.getLogger(MissionFeedUtils.class);

    private static MissionService missionService;
    
    private static MissionService missionService() {
    	if (missionService == null) {
    		synchronized (ResourceUtils.class) {
    			if (missionService == null) {
    				missionService = SpringContextBeanForApi.getSpringContext().getBean(MissionService.class);
    			}
    		}
    	}
    	
    	return missionService;
    }

    private static String findName(MissionFeed missionFeed) {
    	if (missionFeed.getDataFeedUid() == null) {
    		return null;
    	}
    	
    	if (missionService() == null) {
    		logger.error("missionService is null");
    	}
        DataFeedDao dataFeedDao = missionService().getDataFeed(missionFeed.getDataFeedUid());
        if (dataFeedDao != null) {
            return dataFeedDao.getName();
        }
        return null;
    }
    
    public static void findAndSetNameForMissionFeed(MissionFeed missionFeed) {
        
    	String missionFeedName = findName(missionFeed);
        missionFeed.setName(missionFeedName);
        
    }

}