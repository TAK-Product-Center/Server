package com.bbn.marti.injector;

import org.dom4j.Element;
import org.dom4j.Node;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.remote.RemoteSubscription;
import com.bbn.marti.remote.injector.Injector;

import tak.server.cot.CotEventContainer;


/*
 * TODO: Add list of CotTagInjectors, based on UID (or all uids? or by group?)
 * 
 * store in database?
 * 
 * store in data 
 * 
 */
public class StringCotTagInjector implements Injector<RemoteSubscription, CotEventContainer> {
    
    private static final Logger logger = LoggerFactory.getLogger(StringCotTagInjector.class);
    
    private final String inject;
    
    public StringCotTagInjector(@NotNull String inject) {
        this.inject = inject;
      }

    @Override
    public CotEventContainer process(RemoteSubscription subscription, CotEventContainer cot) {
        
        // defensive copy
        cot = cot.copy();
        
        if (cot == null) {
            logger.debug("null CoT or null/empty client subscription uid");
        }
        
        Node detail = cot.getDocument().selectSingleNode("/event/detail");
        
        if (detail instanceof Element) {
            Element detailElement = (Element) detail;
            
            // can also do add(Element element). Better for complex tags.
            
            detailElement.addElement(inject);
        }
        
        logger.trace("injected cot: " + cot);
       
        return cot;
    }

    @Override
    public String getName() {
        return this.getClass().getName() + " '" + inject  + "'";
    }
}
