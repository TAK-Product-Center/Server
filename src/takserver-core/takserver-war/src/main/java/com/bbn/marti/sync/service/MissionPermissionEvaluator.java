package com.bbn.marti.sync.service;

import java.io.Serializable;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;

import com.bbn.marti.sync.model.MissionPermission;
import com.bbn.marti.sync.model.MissionRole;
import com.bbn.marti.util.spring.RequestHolderBean;

public class MissionPermissionEvaluator implements PermissionEvaluator {

    private static final Logger logger = LoggerFactory.getLogger(MissionPermissionEvaluator.class);
    
    @Autowired
    private RequestHolderBean requestHolderBean;

    @Override
    public boolean hasPermission(Authentication authentication, Object requestx, Object permission)   {

        if (authentication == null || !(permission instanceof String)) {
            logger.error("hasPermission called with null or invalid parameter types!");
            
            return false;
        }
        
        final HttpServletRequest req = requestHolderBean.getRequest();

        MissionRole role = requestHolderBean.getMissionRole();
        if (role == null) {
            logger.error("hasPermission unable to find role attribute on http request! : " + req.getServletPath());
            return false;
        }

        MissionPermission.Permission missionPermission = null;
        try {
            missionPermission = MissionPermission.Permission.valueOf((String)permission);
        } catch (IllegalArgumentException e) {
            logger.error("hasPermission unable to map valid permission type : " + (String)permission);
            return false;
        }

        boolean hasPermission = role.hasPermission(missionPermission);
        if (!hasPermission) {
            logger.error("hasPermission denied access! currentRole: " + role.getRole().name()
                    + ", requested permission: " + missionPermission.name() + ", request : " + req.getServletPath());
        }

        return hasPermission;
    }

    @Override
    public boolean hasPermission(
            Authentication authentication, Serializable serializable, String targetType, Object permission) {
        return false;
    }
}