package com.bbn.marti.injector;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.ignite.IgniteSet;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CollectionConfiguration;
import org.dom4j.Element;
import org.dom4j.Node;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.config.UidInject;
import com.bbn.marti.remote.RemoteSubscription;
import com.bbn.marti.remote.exception.NotFoundException;
import com.bbn.marti.remote.injector.InjectorConfig;
import com.google.common.base.Strings;

import tak.server.cot.CotEventContainer;
import tak.server.ignite.IgniteHolder;

/*
 * 
 * String injector for CoT message detail tag, by CoT UID
 * 
 */
public class ClusterUidCotTagInjector extends UidCotTagInjector {    
   
	private static final long serialVersionUID = 7405537089702640271L;

	private static final String UID_COT_TAG_INJECTOR = "uidCotTagInjector-";
	
	IgniteSet<String> uidCotTagInjectorNames;
	
	public ClusterUidCotTagInjector() throws RemoteException {
        super();
        uidCotTagInjectorNames = IgniteHolder.getInstance().getIgnite().set("uidCotTagInjectorNames", getCollectionConfiguration());
    }
   
    
    private static final Logger logger = LoggerFactory.getLogger(ClusterUidCotTagInjector.class);

	@Override
    public boolean addInjector(UidInject uidInject) {
        return getOrCreateSet(uidInject.getUid()).add(uidInject.getToInject());
    }

    // add or update injector for a uid
    /* (non-Javadoc)
     * @see com.bbn.marti.remote.injector.RemoteUidCotInjector#setInjector(java.lang.String, java.lang.String)
     */
    @Override
    public boolean setInjector(String uid, String toInject) {
        if (Strings.isNullOrEmpty(uid) || Strings.isNullOrEmpty(toInject)) {
            throw new IllegalArgumentException("empty uid or injection string");
        }
        
        try {
            
            logger.trace("toInject: " + toInject);

            UidInject uidInject = new UidInject();
            uidInject.setUid(uid);
            uidInject.setToInject(toInject);
            saveUidInject(uidInject);

            return addInjector(uidInject);

        } catch (Exception e) {
            // trigger a 400 Bad Request from the API to the front-end
            logger.debug("unparseable injection: " + toInject + e.getMessage(), e);

            throw new IllegalArgumentException("unparseable injection");
        }

    }
    
    /* (non-Javadoc)
     * @see com.bbn.marti.remote.injector.RemoteUidCotInjector#getInjectors()
     */
    @Override
    public Set<InjectorConfig> getAllInjectors() {
        Set<InjectorConfig> result = new HashSet<>();
        
        for (String uid : uidCotTagInjectorNames) {
            
            for (String inject : getOrCreateSet(uid)) {
            
                result.add(new InjectorConfig(uid, trimToInject(getElementToInject(inject).asXML())));
            }
        }
        
        return result;
    }
    
    /* (non-Javadoc)
     * @see com.bbn.marti.remote.injector.RemoteUidCotInjector#getInjectors()
     */
    @Override
    public Set<InjectorConfig> getInjectors(@NotNull String uid) {
        if (Strings.isNullOrEmpty(uid)) {
            throw new IllegalArgumentException("empty uid");
        }
        
        Set<InjectorConfig> injectors = new HashSet<>();
        
        for (String inject : getOrCreateSet(uid)) {
            injectors.add(new InjectorConfig(uid, trimToInject(getElementToInject(inject).asXML())));
        }
        
        return injectors;
    }
    
    // The subscription parameter is included for extensibility, and could be used for receiver injection
    @Override
    public CotEventContainer process(RemoteSubscription subscription, CotEventContainer cot) {

        if (cot == null) {
            throw new IllegalStateException("null CoT");
        }

        try {
            Collection<String> injectElements = getOrCreateSet(cot.getUid());

            for (String inject : injectElements) {
                
                cot = cot.copy();
                
                Node detail = cot.getDocument().selectSingleNode("/event/detail");

                if (detail instanceof Element) {
                    Element detailElement = (Element) detail;

                    detailElement.add(getElementToInject(inject));
                } else {
                    logger.debug("can't perform injection, event/detail not found in CoT message " + cot);
                }

                logger.trace("injected cot: " + cot);
            }

        } catch (Exception e) {
            logger.debug("exception injecting tag " + e.getMessage(), e);
        }

        return cot;
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public InjectorConfig deleteInjector(InjectorConfig injector) throws RemoteException {

        if (injector == null) {
            throw new IllegalArgumentException("null injector");
        }
        
        logger.debug("deleting injector: " + injector);

        deleteUidInject(injector.getUid(), injector.getToInject());
                       
        boolean isRemoved = getOrCreateSet(injector.getUid()).remove(injector.getToInject());
        
        if (!isRemoved) {
            throw new NotFoundException("Injector " + injector + " not found");
        }
        
        checkAndDeleteEmptySet(injector.getUid());
        
        return injector;
    }
    
    
    private void checkAndDeleteEmptySet(String setName) {
		IgniteSet<String> set = IgniteHolder.getInstance().getIgnite().set(UID_COT_TAG_INJECTOR + setName, getCollectionConfiguration());
		
		if (set.size() == 0) {
			set.close();
			uidCotTagInjectorNames.remove(setName);
		}
	}
	
	private IgniteSet<String> getOrCreateSet(String setName) {
		uidCotTagInjectorNames.add(setName);
		return IgniteHolder.getInstance().getIgnite().set(UID_COT_TAG_INJECTOR + setName, getCollectionConfiguration());
	}
	
	private CollectionConfiguration setCfg;
	
	private CollectionConfiguration getCollectionConfiguration() {
		if (setCfg == null) {
			setCfg = new CollectionConfiguration()
				.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL)
	        	.setCacheMode(CacheMode.PARTITIONED)
	        	.setGroupName("uidCotInjectors");
		}
        
        return setCfg;
	}
}
