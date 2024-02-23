
package com.bbn.locate;

import com.bbn.marti.remote.config.CoreConfigFacade;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.bbn.marti.remote.CoreConfig;


class LocateAuthorizationProvider {

    private final org.slf4j.Logger logger = LoggerFactory.getLogger(com.bbn.locate.LocateAuthorizationProvider.class);

    public synchronized String getRole() {
        try {
            if (CoreConfigFacade.getInstance() == null
            || CoreConfigFacade.getInstance().getRemoteConfiguration() == null
            || CoreConfigFacade.getInstance().getRemoteConfiguration().getLocate() == null
            || !CoreConfigFacade.getInstance().getRemoteConfiguration().getLocate().isEnabled()) {
                return "ROLE_NONEXISTENT";
            }

            if (CoreConfigFacade.getInstance().getRemoteConfiguration().getLocate().isRequireLogin()) {
                return "ROLE_WEBTAK";
            } else {
                return "ROLE_NO_CLIENT_CERT";
            }

        } catch (Exception e) {
            logger.error("exception in getRole", e);
            return "ROLE_NONEXISTENT";
        }
    }
}
