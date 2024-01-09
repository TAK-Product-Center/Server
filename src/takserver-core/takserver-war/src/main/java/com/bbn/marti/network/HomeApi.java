

package com.bbn.marti.network;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.bbn.marti.util.CommonUtil;
import com.bbn.marti.remote.groups.Direction;
import com.bbn.marti.remote.groups.Group;
import com.google.common.base.Charsets;

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
    private static final String CLIENT_DASH = "/Marti/clients/index.html";
    private static final String METRICS_DASH = "/Marti/metrics/index.html";
    @SuppressWarnings("unused")
	private static final String ESYNC_LIST = "/Marti/EnterpriseSync.jsp";
    private static final String SECURITY_CONF = "/Marti/security/index.html";
    @SuppressWarnings("unused")
	private static final String MISSION_MANAGER = "/Marti/Missions.jsp";
    private static final String VER_RESOURCE = "ver.txt";
    
    // keep a reference to the currently active request
    @Autowired
    private HttpServletRequest request;
    
    @Autowired
    private CommonUtil martiUtil; 

    Logger logger = LoggerFactory.getLogger(HomeApi.class);
   
    @RequestMapping(value = "/home", method = RequestMethod.GET)
    public String getHome(HttpServletResponse response) {
    	return !request.isSecure() ? SECURITY_CONF : martiUtil.isAdmin() ? METRICS_DASH : WEBTAK;
    }
    
    @RequestMapping(value = "/ver", method = RequestMethod.GET)
    public String getVer(HttpServletResponse response) throws IOException {
        return IOUtils.toString(
                HomeApi.class.getResourceAsStream(VER_RESOURCE), Charsets.UTF_8);
    }

    @RequestMapping(value = "/util/isAdmin", method = RequestMethod.GET)
    public boolean isAdmin(HttpServletResponse response) throws IOException {
        return martiUtil.isAdmin();
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
        Set<Group> groups = martiUtil.getGroupsFromRequest(request);
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
