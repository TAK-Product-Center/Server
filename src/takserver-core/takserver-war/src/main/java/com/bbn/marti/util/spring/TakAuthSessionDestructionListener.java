

package com.bbn.marti.util.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.security.web.session.HttpSessionDestroyedEvent;

import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.remote.groups.User;

public class TakAuthSessionDestructionListener implements ApplicationListener<HttpSessionDestroyedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(TakAuthSessionDestructionListener.class);

    @Autowired
    private GroupManager groupManager;

    @Override
    public void onApplicationEvent(HttpSessionDestroyedEvent event) {

        if (event == null || event.getSession() == null) {
            logger.debug("null event or session in HttpSessionDestroyedEvent");
            return;
        }

        String sessionId = event.getSession().getId();

        // remove the user
        User user = null;

        try {

            user = groupManager.getUserByConnectionId(sessionId);

            if (user != null) {
                logger.debug("removing user found for sessionId " + sessionId + " " + user);

                groupManager.removeUser(user);

            } else {
                logger.debug("no user found for sessionId " + sessionId);
            }

        } catch (Exception e) {
            logger.debug("exception during session destruction user removal " + user + " sessionId " + sessionId);
        }
    }
}
