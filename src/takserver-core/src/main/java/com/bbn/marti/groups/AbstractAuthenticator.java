

package com.bbn.marti.groups;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.remote.groups.AuthCallback;
import com.bbn.marti.remote.groups.Authenticator;
import com.bbn.marti.remote.groups.Direction;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.remote.groups.User;
import com.bbn.marti.service.Resources;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractAuthenticator implements Authenticator<User> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractAuthenticator.class);

    @Autowired
    protected GroupManager groupManager;

    // how often to perform periodic updates
    private int updateIntervalSeconds = 300;

    public int getUpdateIntervalSeconds() {
        return updateIntervalSeconds;
    }

    public void setUpdateIntervalSeconds(int updateIntervalSeconds) {
        logger.debug("setting update interval to " + updateIntervalSeconds + " seconds");
        this.updateIntervalSeconds = updateIntervalSeconds;
    }
    
    /*
     * synchronously attempt to authenticate
     * 
     */
    @Override
    public void authenticate(@NotNull User user, @NotNull AuthCallback cb) { }

    /*
     * Asynchronously attempt to authenticate the user using the 
     * backend auth engine. When a result is returned, execute the 
     * callback.
     */
    @Override
    public void authenticateAsync(@NotNull final User user, @NotNull final AuthCallback cb) {
        Runnable auth = new Runnable() {
            public void run() {
                authenticate(user, cb);
            }
        };
        
        if (logger.isTraceEnabled()) {        	
        	logger.trace("scheduling periodic auth updates for " + updateIntervalSeconds + " seconds");
        }

        // schedule periodic updater for this user. Updates will be cancelled by an explicity thrown PeriodicUpdateCancellationException.
        Resources.authThreadPool.scheduleWithFixedDelay(auth, 0, updateIntervalSeconds, TimeUnit.SECONDS);
    }

    /*
     * Add the user to the anonymous group, and assign the anonymous role (authority)
     */
    protected void doAnonAssignment(User user, boolean readOnly) {
        
        logger.debug("doing anon assignment");
        
        try {

            if (!readOnly) {
                groupManager.addUserToGroup(user, new Group("__ANON__", Direction.IN));
            }

            groupManager.addUserToGroup(user, new Group("__ANON__", Direction.OUT));

            user.getAuthorities().add("ROLE_ANONYMOUS");
            

        } catch (Exception e) {
            logger.debug("exception doing anonymous user processing for " + user + " " + e.getMessage(), e);
        }
    }

    protected void doAnonAssignment(User user) {
        doAnonAssignment(user, false);
    }

}
