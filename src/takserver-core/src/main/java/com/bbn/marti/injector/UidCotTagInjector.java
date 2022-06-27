package com.bbn.marti.injector;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.*;

import com.bbn.marti.config.Filter;
import com.bbn.marti.config.Injectionfilter;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.bbn.marti.config.Configuration;
import com.bbn.marti.config.UidInject;
import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.remote.RemoteSubscription;
import com.bbn.marti.remote.exception.NotFoundException;
import com.bbn.marti.remote.exception.TakException;
import com.bbn.marti.remote.injector.Injector;
import com.bbn.marti.remote.injector.InjectorConfig;
import com.bbn.marti.remote.util.ConcurrentMultiHashMap;
import com.bbn.marti.util.spring.SpringContextBeanForApi;
import com.google.common.base.Strings;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Multimap;

import tak.server.cot.CotEventContainer;

/*
 * 
 * String injector for CoT message detail tag, by CoT UID
 * 
 */
public class UidCotTagInjector implements Injector<RemoteSubscription, CotEventContainer>, Serializable {
	private static final long serialVersionUID = -7036559689093969913L;  
	    
	private static final Logger logger = LoggerFactory.getLogger(UidCotTagInjector.class);
	    
    private final Multimap<String, Element> uidInjectStringMap;

    @Autowired
    private CoreConfig coreConfig;
    
    public UidCotTagInjector() throws RemoteException {
        super();
        uidInjectStringMap = new ConcurrentMultiHashMap<>();
     
        // Add a comparator, since dom4j Element doesn't have one
        ((ConcurrentMultiHashMap<String, Element>) uidInjectStringMap).setComparator(new Comparator<Element>() {
            @Override
            public int compare(Element thiz, Element that) {
                return ComparisonChain.start().compare(thiz.asXML(), that.asXML()).result();
            }
        });
    }

    public boolean addInjector(UidInject uidInject) throws DocumentException {
        Document document = DocumentHelper.parseText(uidInject.getToInject());

        Element element = document.getRootElement();

        // return true if an existing injector for this uid was replaced
        return uidInjectStringMap.put(uidInject.getUid(), element);
    }

    private void loadUidInjects() {
        try {
            if (coreConfig == null) {
                logger.error("coreConfig is null in loadUidInjects");
                return;
            }

            Configuration configuration = coreConfig.getRemoteConfiguration();
            if (configuration == null
                    || configuration.getFilter() == null
                    || configuration.getFilter().getInjectionfilter() == null) {
                return;
            }

            for (UidInject uidInject : configuration.getFilter().getInjectionfilter().getUidInject()) {
                addInjector(uidInject);
            }
        } catch (Exception e) {
            logger.error("exception in load!", e);
        }
    }

    protected void saveUidInject(UidInject uidInject) {
        if (coreConfig == null) {
            logger.error("coreConfig is null in saveUidInject");
            return;
        }

        Filter filter = coreConfig.getRemoteConfiguration().getFilter();
        if (filter == null) {
            filter = new Filter();
            coreConfig.getRemoteConfiguration().setFilter(filter);
        }

        Injectionfilter injectionfilter = filter.getInjectionfilter();
        if (injectionfilter == null) {
            injectionfilter = new Injectionfilter();
            filter.setInjectionfilter(injectionfilter);
        }

        injectionfilter.setEnable(true);
        injectionfilter.getUidInject().add(uidInject);

        coreConfig.saveChangesAndUpdateCache();
    }

    protected void deleteUidInject(String uid, String toInject) {
        if (coreConfig == null) {
            logger.error("coreConfig is null in deleteUidInject");
            return;
        }

        Filter filter = coreConfig.getRemoteConfiguration().getFilter();
        if (filter == null) {
            return;
        }

        Injectionfilter injectionfilter = filter.getInjectionfilter();
        if (injectionfilter == null) {
            return;
        }

        Iterator<UidInject> it = injectionfilter.getUidInject().iterator();
        while (it.hasNext()) {
            UidInject uidInject = it.next();
            if (uidInject.getUid().compareTo(uid) == 0 &&
                    uidInject.getToInject().compareTo(padToInject(toInject)) == 0) {
                it.remove();
            }
        }

        coreConfig.saveChangesAndUpdateCache();
    }

    private static UidCotTagInjector instance = null; 
    
    public static UidCotTagInjector getInstance() {
		if (instance == null) {
			synchronized (UidCotTagInjector.class) {
				if (instance == null) {
					instance = SpringContextBeanForApi.getSpringContext().getBean(UidCotTagInjector.class);
					instance.loadUidInjects();
				}
			}
		}
		return instance;
	}
    
    public boolean setInjector(String uid, String toInject) {
        if (Strings.isNullOrEmpty(uid) || Strings.isNullOrEmpty(toInject)) {
            throw new IllegalArgumentException("empty uid or injection string");
        }
        
        toInject = padToInject(toInject);

        try {
            UidInject uidInject = new UidInject();
            uidInject.setUid(uid);
            uidInject.setToInject(toInject);
            saveUidInject(uidInject);

            logger.trace("toInject: " + toInject);

            return addInjector(uidInject);

        } catch (Exception e) {
            // trigger a 400 Bad Request from the API to the front-end
            logger.debug("unparseable injection: " + toInject + e.getMessage(), e);

            throw new IllegalArgumentException("unparseable injection");
        }

    }
    
    public Set<InjectorConfig> getAllInjectors() {
        Set<InjectorConfig> result = new HashSet<>();
        
        for (String uid : uidInjectStringMap.keySet()) {
            
            for (Element element : uidInjectStringMap.get(uid)) {
            
                result.add(new InjectorConfig(uid, trimToInject(element.asXML())));
            }
        }
        
        return result;
    }
    
    public Set<InjectorConfig> getInjectors(@NotNull String uid) {
        if (Strings.isNullOrEmpty(uid)) {
            throw new IllegalArgumentException("empty uid");
        }
        
        Set<InjectorConfig> injectors = new HashSet<>();
        
        for (Element element : uidInjectStringMap.get(uid)) {
            injectors.add(new InjectorConfig(uid, trimToInject(element.asXML())));
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
            Collection<Element> injectElements = uidInjectStringMap.get(cot.getUid());

            for (Element injectElement : injectElements) {

                Element injectElementCopy = (Element)injectElement.clone();
                injectElementCopy.detach();

                cot = cot.copy();
                
                Node detail = cot.getDocument().selectSingleNode("/event/detail");

                if (detail instanceof Element) {
                    Element detailElement = (Element) detail;

                    detailElement.add(injectElementCopy);
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
        return this.getClass().getName() + " injector count: " + uidInjectStringMap.size();
    }

    public InjectorConfig deleteInjector(InjectorConfig injector) throws RemoteException {

        if (injector == null) {
            throw new IllegalArgumentException("null injector");
        }
        
        logger.debug("deleting injector: " + injector);

        deleteUidInject(injector.getUid(), injector.getToInject());

        boolean isRemoved = uidInjectStringMap.remove(injector.getUid(), getElementToInject(injector.getToInject()));
        
        if (!isRemoved) {
            throw new NotFoundException("Injector " + injector + " not found");
        }
        
        return injector;
    }
    
    protected String trimToInject(String toInject) {
        if (toInject == null || toInject.length() < 3) {
            return "";
        }
        
        else return toInject.substring(1, toInject.length() - 2);
    }
    
    protected String padToInject(String toInject) {
        if (toInject == null) {
            return "";
        }
        
        else return "<" + toInject + "/>";
    }
    
    protected Element getElementToInject(String toInject) {
        
        try {
            Document document = DocumentHelper.parseText(padToInject(toInject));
            return document.getRootElement();
        } catch (DocumentException e) {
            throw new TakException(e);
        }
    }
}
