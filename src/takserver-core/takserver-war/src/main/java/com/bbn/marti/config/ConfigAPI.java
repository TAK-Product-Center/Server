package com.bbn.marti.config;

import java.rmi.RemoteException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.bbn.marti.network.BaseRestController;
import com.bbn.marti.remote.config.CoreConfigFacade;
import com.bbn.marti.util.CommonUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
public class ConfigAPI extends BaseRestController {

    private static final Logger logger = LoggerFactory.getLogger(ConfigAPI.class);

    @Autowired
    private CommonUtil martiUtil;
    
    @Autowired
    private HttpServletRequest request;

    @RequestMapping(value = "/config", method = RequestMethod.GET)
    Configuration getCoreConfig(HttpServletResponse response) throws RemoteException {

        if (!martiUtil.isAdmin(request)) {
            logger.error("Non admin user attempted to access ConfigAPI!");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return null;
        }

        return CoreConfigFacade.getInstance().getRemoteConfiguration();
    }

    @RequestMapping(value = "/cachedConfig", method = RequestMethod.GET)
    Configuration getCachedCoreConfig(HttpServletResponse response) throws RemoteException {

        if (!martiUtil.isAdmin(request)) {
            logger.error("Non admin user attempted to access ConfigAPI!");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return null;
        }

        return CoreConfigFacade.getInstance().getCachedConfiguration();
    }

    @RequestMapping(value = "/cachedInputConfig", method = RequestMethod.GET)
    List<Input> getCachedInputConfig(HttpServletResponse response) throws RemoteException {

        if (!martiUtil.isAdmin(request)) {
            logger.error("Non admin user attempted to access ConfigAPI!");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return null;
        }

        return CoreConfigFacade.getInstance().getCachedConfiguration().getNetwork().getInput();
    }
}
