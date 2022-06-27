
package com.bbn.locate;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.bbn.marti.remote.CoreConfig;


class LocateAuthorizationProvider {

    private final org.slf4j.Logger logger = LoggerFactory.getLogger(com.bbn.locate.LocateAuthorizationProvider.class);

    @Autowired
    CoreConfig coreConfig;

    public synchronized String getRole() {
        try {
            if (coreConfig == null
            || coreConfig.getRemoteConfiguration() == null
            || coreConfig.getRemoteConfiguration().getLocate() == null
            || !coreConfig.getRemoteConfiguration().getLocate().isEnabled()) {
                return "ROLE_NONEXISTENT";
            }

            if (coreConfig.getRemoteConfiguration().getLocate().isRequireLogin()) {
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
