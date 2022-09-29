package com.bbn.marti.config;

import java.rmi.RemoteException;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.bbn.marti.network.BaseRestController;
import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.util.CommonUtil;

@RestController
public class ConfigAPI extends BaseRestController {

    private static final Logger logger = LoggerFactory.getLogger(ConfigAPI.class);

    @Autowired
    private CoreConfig coreConfig;

    @Autowired
    private CommonUtil martiUtil;

    @RequestMapping(value = "/config", method = RequestMethod.GET)
    Configuration getCoreConfig(HttpServletResponse response) throws RemoteException {

        if (!martiUtil.isAdmin()) {
            logger.error("Non admin user attempted to access ConfigAPI!");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return null;
        }

        return coreConfig.getRemoteConfiguration();
    }
    
    @RequestMapping(value = "/cachedConfig", method = RequestMethod.GET)
    Configuration getCachedCoreConfig(HttpServletResponse response) throws RemoteException {

        if (!martiUtil.isAdmin()) {
            logger.error("Non admin user attempted to access ConfigAPI!");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return null;
        }

        return coreConfig.getCachedConfiguration();
    }
    
    @RequestMapping(value = "/cachedInputConfig", method = RequestMethod.GET)
    List<Input> getCachedInputConfig(HttpServletResponse response) throws RemoteException {

        if (!martiUtil.isAdmin()) {
            logger.error("Non admin user attempted to access ConfigAPI!");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return null;
        }

        return coreConfig.getCachedConfiguration().getNetwork().getInput();
    }
}
