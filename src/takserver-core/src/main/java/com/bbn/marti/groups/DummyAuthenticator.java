

package com.bbn.marti.groups;

import java.io.Serializable;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import com.bbn.marti.remote.groups.AuthCallback;
import com.bbn.marti.remote.groups.AuthResult;
import com.bbn.marti.remote.groups.AuthStatus;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.remote.groups.User;
import com.bbn.marti.service.Resources;
import com.bbn.marti.util.spring.SpringContextBeanForApi;

/*
 * Dummy authenticator, always succeeds.
 * 
 */
public class DummyAuthenticator extends AbstractAuthenticator implements Serializable {
    
    private static final long serialVersionUID = -4317122669577006008L;

    Logger logger = LoggerFactory.getLogger(DummyAuthenticator.class);
    
    private static DummyAuthenticator instance;
    
    public static synchronized DummyAuthenticator getInstance(GroupManager groupManager) {
        if (instance == null) {
            instance = new DummyAuthenticator(groupManager);
        }

        return instance;
    }
    
    public static synchronized DummyAuthenticator getInstance() {
        if (instance == null) {
        	instance = new DummyAuthenticator();
        }
        
        return instance;
    }
    
    public DummyAuthenticator(GroupManager groupManager) {
    	
    	this.groupManager = groupManager;
        
    	groupManager.registerAuthenticator("dummy", this);
    }
    
    public DummyAuthenticator() { }
    
	@EventListener({ContextRefreshedEvent.class})
    private void init() {
    	instance = SpringContextBeanForApi.getSpringContext().getBean(DummyAuthenticator.class);
    }
    
    @Override
    public void authenticate(@NotNull User user, @NotNull AuthCallback cb) {
        logger.debug("dummy auth successful for " + user); 
        
        // can't fail
        cb.authenticationReturned(user, AuthStatus.SUCCESS);
    }
    
    @Override
    public AuthResult authenticate(User user) {
        logger.debug("dummy auth successful for " + user); 
        
        return new AuthResult(AuthStatus.SUCCESS, user);
    }

    // Using the superclass only for its thread pool
    @Override
    public void authenticateAsync(@NotNull final User user, @NotNull final AuthCallback cb) {

        // execute auth callback just once for the dummy case - no periodic updates
        Resources.authThreadPool.execute(new Runnable() {
            public void run() {
                try {
                    authenticate(user, cb);
                } catch (Exception e) {
                    logger.error("authenticateAsync failed.", e);
                }
            }
        });        
    }

    @Override
    public String toString() {
        return "DummyAuthenticator [groupManager=" + groupManager + "]";
    }
    
}
