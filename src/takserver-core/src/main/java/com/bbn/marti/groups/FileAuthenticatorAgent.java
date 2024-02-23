package com.bbn.marti.groups;

import org.apache.ignite.Ignite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import tak.server.util.ActiveProfiles;
import com.bbn.marti.groups.value.FileAuthenticatorControl;

import com.bbn.marti.remote.config.CoreConfigFacade;
import tak.server.Constants;
import tak.server.ignite.IgniteHolder;

public class FileAuthenticatorAgent {
	
	@Autowired
	private Ignite ignite;
	
	private static final Logger logger = LoggerFactory.getLogger(FileAuthenticatorAgent.class);
	
	@EventListener({ContextRefreshedEvent.class})
	private void init() {
		
		if (logger.isDebugEnabled()) {
			logger.debug("init FileAuthenticatorAgent");
		}
				
		ignite.message().localListen(Constants.FILE_AUTH_TOPIC, (nodeId, authControl) -> {
			// skip this notification if it was sent from this node
			if (nodeId.equals(IgniteHolder.getInstance().getIgniteId())) return true;
			
			try {
				if (authControl instanceof FileAuthenticatorControl) {
					FileAuthenticatorControl control = ((FileAuthenticatorControl) authControl);
					switch (control.getControlType()) {
					case USER_ADD:
						FileAuthenticator.getInstance().addControlUser(control.getFileUser());
						break;
					case USER_UPDATE:
						FileAuthenticator.getInstance().updateControlUser(control.getFileUser());
						break;
					case USER_DELETE:
						FileAuthenticator.getInstance().deleteControlUser(control.getFileUser());
						break;
					case USER_PASSWORD_CHANGE_WITHOUT_OLD_PASSWORD:
						FileAuthenticator.getInstance().updateControlUserPasswordWithoutOldPassword(control.getFileUser());
					default:
						break;
					}
					
					if (CoreConfigFacade.getInstance().getRemoteConfiguration().getCluster().isEnabled()
							|| (IgniteHolder.getInstance().areTakserverIgnitesLocal() && ActiveProfiles.getInstance().isMessagingProfileActive())) {
						FileAuthenticator.getInstance().saveChanges(null);
					} 
				}
			} catch (Exception e) {
				logger.error("exception processing FileAuthenticatorControl message", e);
				if (logger.isDebugEnabled()) {
					logger.debug("exception deserializing plugin message", e);
				}
			}

			// return true to continue listening
			return true;
		});
	}

}
