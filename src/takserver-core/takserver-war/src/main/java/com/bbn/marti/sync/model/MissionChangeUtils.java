/*
 * Copyright (c) 2013-2016 Raytheon BBN Technologies. Licensed to US Government with unlimited rights.
 */

package com.bbn.marti.sync.model;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.maplayer.model.MapLayer;
import com.bbn.marti.sync.service.MissionService;
import com.bbn.marti.util.spring.SpringContextBeanForApi;

public class MissionChangeUtils {
    
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

    protected static final Logger logger = LoggerFactory.getLogger(MissionChangeUtils.class);

    private static UidDetails findUidDetails(MissionChange missionChange) {

        if (missionChange.getContentUid() != null && missionChange.getTimestamp() != null)   { // FIXME: Why this one check getTimestamp(), but the below use Server time???) 
            try {
            	UidDetails uidDetails = missionChange.getUidDetails();
                if (uidDetails == null) {
                    uidDetails = new UidDetails();
                	
                	if (missionService() == null) {
                		logger.error("missionService is null");
                	}
                	
                    missionService().hydrate(uidDetails, missionChange.getContentUid(), missionChange.getServerTime());
                    missionChange.setUidDetails(uidDetails);
                }
                return uidDetails;
            } catch (Exception e) {
                logger.debug("Exception in findUidDetails!", e);
            }
        }
        return null;
    }
    
    public static void findAndSetUidDetails(MissionChange missionChange) {
    	UidDetails uidDetails = findUidDetails(missionChange);
    	missionChange.setUidDetails(uidDetails);
    }

    private static MissionFeed findMissionFeed(MissionChange missionChange) {
    	String missionFeedUid = missionChange.getMissionFeedUid();
        if (missionFeedUid != null) {
        	
        	if (missionService() == null) {
        		logger.error("missionService is null");
        	}
        	
            MissionFeed missionFeed =  missionService().getMissionFeed(missionFeedUid);
            if (missionFeed == null) {
                missionFeed = new MissionFeed();
                missionFeed.setUid(missionFeedUid);
            }
            
            MissionFeedUtils.findAndSetNameForMissionFeed(missionFeed);
            
            return missionFeed;
        }

        return null;
    }

    public static void findAndSetMissionFeed(MissionChange missionChange) {
    	MissionFeed missionFeed = findMissionFeed(missionChange);
    	missionChange.setMissionFeed(missionFeed);
    }
    
    private static MapLayer findMapLayer(MissionChange missionChange) {
        try {
        	String mapLayerUid = missionChange.getMapLayerUid();
            if (mapLayerUid != null) {
            	
            	if (missionService() == null) {
            		logger.error("missionService is null");
            	}
            	
                MapLayer mapLayer = missionService().getMapLayer(mapLayerUid);
                if (mapLayer == null) {
                    mapLayer = new MapLayer();
                    mapLayer.setUid(mapLayerUid);
                }

                return mapLayer;
            }
        } catch (Exception e) {
            logger.error("exception in findMapLayer", e);
        }

        return null;
    }
    
    public static void findAndSetMapLayer(MissionChange missionChange) {
    	MapLayer mapLayer = findMapLayer(missionChange);
    	missionChange.setMapLayer(mapLayer);
    }
    
    private static ExternalMissionData findExternalMissionData(MissionChange missionChange) {
    	
    	if (missionService() == null) {
    		logger.error("missionService is null");
    	}

        if (missionChange.getExternalDataUid() != null && missionChange.getExternalDataName()!= null
                && missionChange.getExternalDataTool()!= null && missionChange.getExternalDataNotes() != null){
        	ExternalMissionData externalMissionData = missionService().hydrate(
            		missionChange.getExternalDataUid(), missionChange.getExternalDataName(), missionChange.getExternalDataTool(), missionChange.getExternalDataToken(), missionChange.getExternalDataNotes());
        	return  externalMissionData;
        }

        return null;
    }
    
    public static void findAndSetExternalMissionData(MissionChange missionChange) {
    	ExternalMissionData externalMissionData = findExternalMissionData(missionChange);
    	missionChange.setExternalMissionData(externalMissionData);
    }

    public static Resource findContentResource(MissionChange missionChange) {

        Resource resource = null;

        if (missionChange.getContentHash() != null) {

            try {
                resource = ResourceUtils.fetchResourceByHash(missionChange.getContentHash());
               
            } catch (Exception e) {
                logger.debug("exception fetching resource by hash " + e.getMessage(), e);
            }
            
            // in case the resource has been deleted, just capture the hash
            if (resource == null) {
                resource = new Resource();
                resource.setHash(missionChange.getContentHash());
                resource.setCreatorUid(null);
                resource.setFilename(null);
                resource.setId(null);
                resource.setKeywords(null);
                resource.setMimeType(null);
                resource.setName(null);
                resource.setSubmissionTime(null);
                resource.setSize(null);
                resource.setSubmitter(null);
                resource.setUid(null);
            }
        }

        return resource;

    }
    
    public static void findAndSetContentResource(MissionChange missionChange) {
    	Resource resource = findContentResource(missionChange);
    	missionChange.setContentResource(resource);
    }
    
	public static void findAndSetTransientValuesForMissionChange(MissionChange missionChange) {
		
		logger.debug("~~~~ Finding ContentType for missionChange");
		findAndSetContentResource(missionChange);
		logger.debug("~~~~ Finding UidDetails for missionChange");
		findAndSetUidDetails(missionChange);
		logger.debug("~~~~ Finding MissionFeed for missionChange");
		findAndSetMissionFeed(missionChange);
		logger.debug("~~~~ Finding MapLayer for missionChange");
		findAndSetMapLayer(missionChange);
        logger.debug("~~~~ Finding MapLayer for externalData");
        findAndSetExternalMissionData(missionChange);

	}
}