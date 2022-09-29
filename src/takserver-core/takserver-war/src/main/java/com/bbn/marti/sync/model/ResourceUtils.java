package com.bbn.marti.sync.model;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.remote.exception.NotFoundException;
import com.bbn.marti.sync.repository.ResourceRepository;
import com.bbn.marti.sync.service.MissionService;
import com.bbn.marti.util.spring.SpringContextBeanForApi;

public class ResourceUtils{
    
    private static ResourceRepository resourceRepository;
    
    private static MissionService missionService;
    
    @SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(ResourceUtils.class);
  
    public static Resource fetchResourceByHash(@NotNull String hash) {
        
    	List<Resource> resources = resourceRepository().findByHash(hash);
        
        if (resources == null || resources.isEmpty()) {
            throw new NotFoundException("no resource found for hash " + hash);
        }
        
        // return the first one
        return missionService().hydrate(resources.get(0));
    }
    
    private static ResourceRepository resourceRepository() {
    	if (resourceRepository == null) {
    		synchronized (ResourceUtils.class) {
    			if (resourceRepository == null) {
    				resourceRepository = SpringContextBeanForApi.getSpringContext().getBean(ResourceRepository.class);
    			}
    		}
    	}
    	
    	return resourceRepository;
    	
    }
    
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
}
