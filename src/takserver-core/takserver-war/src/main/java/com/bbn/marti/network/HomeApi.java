package com.bbn.marti.network;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.bbn.marti.remote.groups.Direction;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.util.CommonUtil;
import com.google.common.base.Charsets;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tak.server.Constants;

/**
 * 
 * REST endpoint specifying default home page
 *
 */
@RestController
public class HomeApi extends BaseRestController {
    
    private static final String WEBTAK = "/webtak/index.html";
    @SuppressWarnings("unused")
    private static final String SUBLIST = "/Marti/ListSubs.jsp";
    @SuppressWarnings("unused")
	private static final String CLIENT_DASH = "/Marti/clients/index.html";
    private static final String METRICS_DASH = "/Marti/metrics/index.html";
    @SuppressWarnings("unused")
	private static final String ESYNC_LIST = "/Marti/EnterpriseSync.jsp";
    @SuppressWarnings("unused")
	private static final String SECURITY_CONF = "/Marti/security/index.html";
    @SuppressWarnings("unused")
	private static final String MISSION_MANAGER = "/Marti/Missions.jsp";
    private static final String VER_RESOURCE = "ver.txt";
    
    // The currently active request. This is populated independently in request method invocations.
    @Autowired
    private HttpServletRequest request;
    
    @Autowired
    private CommonUtil commonUtil; 

    Logger logger = LoggerFactory.getLogger(HomeApi.class);
   
    @RequestMapping(value = "/home", method = RequestMethod.GET)
    public String getHome(HttpServletResponse response) {
    	return commonUtil.isAdmin(request) ? METRICS_DASH : WEBTAK;
    }
    
    @RequestMapping(value = "/ver", method = RequestMethod.GET)
    public String getVer(HttpServletResponse response) throws IOException {
    	try (InputStream stream = HomeApi.class.getResourceAsStream(VER_RESOURCE)) {
    		return IOUtils.toString(stream, Charsets.UTF_8);
    	}
    }

    @RequestMapping(value = "/util/isAdmin", method = RequestMethod.GET)
    public boolean isAdmin(HttpServletResponse response) throws IOException {
        return commonUtil.isAdmin(request);
    }
    
    @SuppressWarnings("unchecked")
	@RequestMapping(value = "/util/user/roles", method = RequestMethod.GET)
    public Collection<String> getUserRoles(HttpServletResponse response) throws IOException {
    	
    	List<String> roles = new ArrayList<>();
    	
        for (SimpleGrantedAuthority auth : (Collection<SimpleGrantedAuthority>) SecurityContextHolder.getContext().getAuthentication().getAuthorities()) {
        	roles.add(auth.getAuthority());
        }

        //
        // add the READONLY_ROLE if the user isn't part of any IN groups
        //

        boolean readOnly = true;
        Set<Group> groups = commonUtil.getGroupsFromRequest(request);
        for (Group group : groups) {
            if (group.getDirection() == Direction.IN) {
                readOnly = false;
                break;
            }
        }

        if (readOnly) {
            roles.add(Constants.READONLY_ROLE);
        }

        return roles;
        
    }
}
