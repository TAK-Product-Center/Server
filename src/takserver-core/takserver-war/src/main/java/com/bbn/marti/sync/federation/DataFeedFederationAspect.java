package com.bbn.marti.sync.federation;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.CodeSignature;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.atakmap.Tak.ROL;
import com.bbn.marti.feeds.DataFeedService;
import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.remote.FederationManager;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.sync.model.DataFeedDao;
import com.bbn.marti.util.CommonUtil;

import mil.af.rl.rol.value.DataFeedMetadata;
import tak.server.feeds.DataFeed.DataFeedType;

@Aspect
@Configurable
public class DataFeedFederationAspect {
	
    private static final Logger logger = LoggerFactory.getLogger(DataFeedFederationAspect.class);
    
    private MissionFederationManager mfm;
    
    private MissionActionROLConverter malrc;
    
    private FederationManager federationManager;
    
    @Autowired
    private CoreConfig coreConfig;
    
    @Autowired
    private DataFeedService dataFeedService;
    
    @Autowired
    private CommonUtil commonUtil;
    
    public DataFeedFederationAspect(FederationManager federationManager, MissionActionROLConverter malrc, MissionFederationManager mfm) {
    	this.malrc = malrc;
    	this.mfm = mfm;
    	this.federationManager = federationManager;
    }
    
    @AfterReturning(value = "execution(* com.bbn.marti.sync.repository.DataFeedRepository.addDataFeed(..))", returning="returnValue")
    public void addDataFeed(JoinPoint jp, Object returnValue) throws RemoteException {
    	if (!isDataFeedFederationEnabled()) return;
    	
    	Long res = (Long) returnValue;
    	if (res == null) return;
    	
    	try {
    		if (isCyclic()) {
    			if (logger.isDebugEnabled()) {
    				logger.debug("skipping cyclic data feed aspect execution");
    			}
    			return;
    		}
    		
    		DataFeedDao dataFeed = dataFeedService.getDataFeedById(res);
    		
    		DataFeedMetadata meta = new DataFeedMetadata();
    		meta.setDataFeedUid(dataFeed.getUUID());
    		meta.setArchive(dataFeed.getArchive());
    		meta.setArchiveOnly(dataFeed.getArchiveOnly());
    		meta.setSync(dataFeed.isSync());
    		meta.setFeedName(dataFeed.getName());
    		meta.setAuthType(dataFeed.getAuth().toString());
    		meta.setSyncCacheRetentionSeconds(dataFeed.getSyncCacheRetentionSeconds());
    		   
    		// use mfm if available (api), otherwise use fed manager (messaging)
    		if (mfm != null) {
    			mfm.createDataFeed(meta, commonUtil.getAllInOutGroups());
    		} else {
    			federationManager.submitFederateROL(malrc.createDataFeedToROL(meta), commonUtil.getAllInOutGroups());
    		}    		
    	} catch (Exception e) {
    		if (logger.isDebugEnabled()) {
    			logger.debug("exception executing addDataFeed advice: ", e);
    		}
    	}
    }
    
    @AfterReturning(value = "execution(* com.bbn.marti.sync.repository.DataFeedRepository.updateDataFeed(..))", returning="returnValue")
    public void updateDataFeed(JoinPoint jp, Object returnValue) throws RemoteException {
    	doUpdateDataFeed(jp, returnValue);
    }
    
    @AfterReturning(value = "execution(* com.bbn.marti.sync.repository.DataFeedRepository.updateDataFeedWithGroupVector(..))", returning="returnValue")
    public void updateDataFeedWithGroupVector(JoinPoint jp, Object returnValue) throws RemoteException {
    	doUpdateDataFeed(jp, returnValue);
    }
    
    private void doUpdateDataFeed(JoinPoint jp, Object returnValue) {
    	if (!isDataFeedFederationEnabled()) return;
    	
    	Long res = (Long) returnValue;
    	if (res == null) return;
    	
    	try {
    		if (isCyclic()) {
    			if (logger.isDebugEnabled()) {
    				logger.debug("skipping cyclic data feed aspect execution");
    			}
    			return;
    		}
    		
    		DataFeedDao dataFeed = dataFeedService.getDataFeedById(res);
    		
    		DataFeedMetadata meta = new DataFeedMetadata();
    		meta.setDataFeedUid(dataFeed.getUUID());
    		meta.setArchive(dataFeed.getArchive());
    		meta.setArchiveOnly(dataFeed.getArchiveOnly());
    		meta.setSync(dataFeed.isSync());
    		meta.setFeedName(dataFeed.getName());
    		meta.setAuthType(dataFeed.getAuth().toString());
    		meta.setTags(dataFeedService.getDataFeedTagsById(dataFeed.getId()));
    		meta.setSyncCacheRetentionSeconds(dataFeed.getSyncCacheRetentionSeconds());
    		
    		// use mfm if available (api), otherwise use fed manager (messaging)
    		if (mfm != null) {
    			mfm.updateDataFeed(meta, commonUtil.getAllInOutGroups());
    		} else {
    			federationManager.submitFederateROL(malrc.updateDataFeedToROL(meta), commonUtil.getAllInOutGroups());
    		} 
    	} catch (Exception e) {
    		if (logger.isDebugEnabled()) {
    			logger.debug("exception executing delete mission advice: ", e);
    		}
    	}
    }
    
    @AfterReturning(value = "execution(* com.bbn.marti.sync.repository.DataFeedRepository.addDataFeedTags(..))", returning="returnValue")
    public void addDataFeedTags(JoinPoint jp, Object returnValue) throws RemoteException {
    	if (!isDataFeedFederationEnabled()) return;
    	
    	try {
    		if (isCyclic()) {
    			if (logger.isDebugEnabled()) {
    				logger.debug("skipping cyclic data feed aspect execution");
    			}
    			return;
    		}
    		
    		Long id = (Long) jp.getArgs()[0];
        	DataFeedDao dataFeed = dataFeedService.getDataFeedById(id);
        	
        	DataFeedMetadata meta = new DataFeedMetadata();
        	meta.setDataFeedUid(dataFeed.getUUID());
    		meta.setArchive(dataFeed.getArchive());
    		meta.setArchiveOnly(dataFeed.getArchiveOnly());
    		meta.setSync(dataFeed.isSync());
    		meta.setFeedName(dataFeed.getName());
    		meta.setAuthType(dataFeed.getAuth().toString());
    		meta.setSyncCacheRetentionSeconds(dataFeed.getSyncCacheRetentionSeconds());
        	meta.setTags(dataFeedService.getDataFeedTagsById(dataFeed.getId()));
        	
    		// use mfm if available (api), otherwise use fed manager (messaging)
    		if (mfm != null) {
    			mfm.updateDataFeed(meta, commonUtil.getAllInOutGroups());
    		} else {
    			federationManager.submitFederateROL(malrc.updateDataFeedToROL(meta), commonUtil.getAllInOutGroups());
    		} 
		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
    			logger.debug("exception executing delete all data feed tags advice: ", e);
    		}
		}
    }

    @Before(value = "execution(* com.bbn.marti.sync.repository.DataFeedRepository.deleteDataFeed(..))")
    public void deleteDataFeed(JoinPoint jp) throws RemoteException {  
    	if (!isDataFeedFederationEnabled()) return;

    	try {
    		if (isCyclic()) {
    			if (logger.isDebugEnabled()) {
    				logger.debug("skipping cyclic data feed aspect execution");
    			}
    			return;
    		}
    		String name = (String) jp.getArgs()[0];
        	DataFeedDao dataFeed = dataFeedService.getDataFeedByName(name);
        	doDeleteDataFeed(jp, dataFeed);
    	} catch (Exception e) {
    		if (logger.isDebugEnabled()) {
    			logger.debug("exception executing delete data feed advice: ", e);
    		}
		}
    }
    
    @Before(value = "execution(* com.bbn.marti.sync.repository.DataFeedRepository.deleteDataFeedById(..))")
    public void deleteDataFeedById(JoinPoint jp) throws RemoteException {
    	if (!isDataFeedFederationEnabled()) return;
    	
    	try {
    		if (isCyclic()) {
    			if (logger.isDebugEnabled()) {
    				logger.debug("skipping cyclic data feed aspect execution");
    			}
    			return;
    		}
    		
    		Long id = (Long) jp.getArgs()[0];
        	DataFeedDao dataFeed = dataFeedService.getDataFeedById(id);
        	doDeleteDataFeed(jp, dataFeed);
    	} catch (Exception e) {
    		if (logger.isDebugEnabled()) {
    			logger.debug("exception executing delete data feed advice: ", e);
    		}
		}
    }
       
    private void doDeleteDataFeed(JoinPoint jp, DataFeedDao dataFeed) {    	
    	try {
    		if (dataFeed.getType() == DataFeedType.Federation.ordinal()) return;
    		
    		DataFeedMetadata meta = new DataFeedMetadata();
    		meta.setFeedName(dataFeed.getName());
    		meta.setDataFeedUid(dataFeed.getUUID());
  
       		// use mfm if available (api), otherwise use fed manager (messaging)
    		if (mfm != null) {
    			mfm.deleteDataFeed(meta, commonUtil.getAllInOutGroups());
    		} else {
    			federationManager.submitFederateROL(malrc.deleteDataFeedToROL(meta), commonUtil.getAllInOutGroups());
    		} 
    		
    	} catch (Exception e) {
    		if (logger.isDebugEnabled()) {
    			logger.debug("exception executing delete mission advice: ", e);
    		}
    	}
    }
    
    @Before(value = "execution(* com.bbn.marti.sync.repository.DataFeedRepository.removeAllDataFeedTagsById(..))")
    public void removeAllDataFeedTagsById(JoinPoint jp) throws RemoteException {
    	if (!isDataFeedFederationEnabled()) return;
    	
    	try {
    		if (isCyclic()) {
    			if (logger.isDebugEnabled()) {
    				logger.debug("skipping cyclic data feed aspect execution");
    			}
    			return;
    		}
    		
    		Long id = (Long) jp.getArgs()[0];
        	DataFeedDao dataFeed = dataFeedService.getDataFeedById(id);
        	
        	if (dataFeed.getType() == DataFeedType.Federation.ordinal()) return;
        	
        	DataFeedMetadata meta = new DataFeedMetadata();
        	meta.setDataFeedUid(dataFeed.getUUID());
    		meta.setArchive(dataFeed.getArchive());
    		meta.setArchiveOnly(dataFeed.getArchiveOnly());
    		meta.setSync(dataFeed.isSync());
    		meta.setFeedName(dataFeed.getName());
    		meta.setAuthType(dataFeed.getAuth().toString());
    		meta.setSyncCacheRetentionSeconds(dataFeed.getSyncCacheRetentionSeconds());
        	meta.setTags(new ArrayList<>());

    		// use mfm if available (api), otherwise use fed manager (messaging)
    		if (mfm != null) {
    			mfm.updateDataFeed(meta, commonUtil.getAllInOutGroups());
    		} else {
    			federationManager.submitFederateROL(malrc.updateDataFeedToROL(meta), commonUtil.getAllInOutGroups());
    		} 
		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
    			logger.debug("exception executing delete all data feed tags advice: ", e);
    		}
		}
    }
    
    // a federate data feed is a data feed that was received over a federate link
    // we do not want to federate back changes made to a federate feed. this will cause: 
    // 1. a loop and 2. connected federates the ability alter the source data feed
    public boolean federatedDataFeed(DataFeedDao dataFeed) {
    	return dataFeed.getType() == DataFeedType.Federation.ordinal();
    }
    
	private boolean isDataFeedFederationEnabled() {
		return coreConfig.getRemoteConfiguration().getFederation().isEnableFederation() && coreConfig.getRemoteConfiguration().getFederation().isAllowDataFeedFederation();
	}
    
    private boolean isCyclic() {
    	StackTraceElement[] stack = new Exception().getStackTrace();
    	
    	for (StackTraceElement el : stack) {
    		
    		if (logger.isTraceEnabled()) {
    			logger.trace("stack element: " + el.getClassName());
    		}
    		
			if (el.getClassName().equals(FederationROLHandler.class.getName())) {
				return true;
			}
		} 

    	return false;
    }
}
