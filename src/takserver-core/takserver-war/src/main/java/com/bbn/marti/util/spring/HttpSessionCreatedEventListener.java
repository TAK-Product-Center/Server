

package com.bbn.marti.util.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.security.web.session.HttpSessionCreatedEvent;

public class HttpSessionCreatedEventListener implements ApplicationListener<HttpSessionCreatedEvent> {
    
    private static final Logger logger = LoggerFactory.getLogger(HttpSessionCreatedEventListener.class);

    @Override
    public void onApplicationEvent(HttpSessionCreatedEvent event) {
        logger.trace("HTTP session created " + event.getSession().getId());
    }
}
