

package com.bbn.marti.network;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bbn.marti.cot.search.model.ApiResponse;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.LdapGroup;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.marti.util.CommonUtil;
import com.google.common.collect.ComparisonChain;

import tak.server.Constants;

/**
 * 
 * REST endpoint for interfacing with ldap functionality exposed by components in Marti Router
 *
 */
@RestController
public class LDAPApi extends BaseRestController {
    
    Logger logger = LoggerFactory.getLogger(LDAPApi.class);
   
    @Autowired
    private GroupManager groupManager;

    @Autowired
    private CommonUtil martiUtil;

    @Autowired
    private RemoteUtil remoteUtil;

    public GroupManager getGroupManager() {
        return groupManager;
    }

    public void setGroupManager(GroupManager groupManager) {
        this.groupManager = groupManager;
    }

    @RequestMapping(value = "/groups", method = RequestMethod.GET)
    public ResponseEntity<ApiResponse<SortedSet<LdapGroup>>> getLdapGroups(HttpServletResponse response, @RequestParam String groupNameFilter) {

    	setCacheHeaders(response);
    	
    	List<String> errors = new ArrayList<String>();
    	
        SortedSet<LdapGroup> groups = null;
        ResponseEntity<ApiResponse<SortedSet<LdapGroup>>> result = null;
        
        try {

    		if (groupNameFilter != null && groupNameFilter.trim().length() > 0 && 
    				groupNameFilter.replaceFirst("^[\\w\\d\\s\\.\\(\\)@#$_\\'\\&\\-\\+\\[\\]\\{\\}:,\\/\\|\\\\]*$", "").length() > 0) {
    			errors.add("Group filter name contains invalid characters.");
    		}
    		
        	if (errors.isEmpty()) {
	    		groups = new ConcurrentSkipListSet<LdapGroup>(new Comparator<LdapGroup>() {
	                @Override
	                public int compare(LdapGroup thiz, LdapGroup that) {
	                    return ComparisonChain.start()
	                            .compare(thiz.getCn(), that.getCn())
	                            .result();
	                }
	            });
	            
	            List<LdapGroup> groupsAsList = groupManager.searchGroups(groupNameFilter, false);
	            
	            groups.addAll(groupsAsList);
	            
	            result = new ResponseEntity<ApiResponse<SortedSet<LdapGroup>>>(new ApiResponse<SortedSet<LdapGroup>>(Constants.API_VERSION, LdapGroup.class.getName(), groups), HttpStatus.OK);
        	}
        } catch (Exception e) { 
        	errors.add("An unexpected error has occurred. Contact the system administrator.");
            logger.error("Exception getting LDAP group search results from GroupManager.", e);  
        }

        if (result == null) {
        	//This would be an error condition (not an empty list)
            result = new ResponseEntity<ApiResponse<SortedSet<LdapGroup>>>(new ApiResponse<SortedSet<LdapGroup>>(Constants.API_VERSION, LdapGroup.class.getName(), null, errors), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        return result;
    }

    @RequestMapping(value = "/groups/members", method = RequestMethod.GET)
    public ResponseEntity<ApiResponse<Integer>> getLdapGroupMembers(
            HttpServletRequest request, HttpServletResponse response, @RequestParam String[] groupNameFilter) {

        NavigableSet<Group> userGroups = martiUtil.getUserGroups(request);

        List<String> search = new ArrayList<>();
        for (String groupName : groupNameFilter) {
            for (Group group : userGroups) {
                if (group.getName().compareTo(groupName) == 0) {
                    search.add(groupName);
                }
            }
        }

        setCacheHeaders(response);

        List<String> errors = new ArrayList<String>();

        ResponseEntity<ApiResponse<Integer>> result = null;

        try {
            for (String filter : search) {
                if (filter != null && filter.trim().length() > 0 &&
                        filter.replaceFirst("^[\\w\\d\\s\\.\\(\\)@#$_\\'\\&\\-\\+\\[\\]\\{\\}:,\\/\\|\\\\]*$", "").length() > 0) {
                    errors.add("Group filter name contains invalid characters.");
                    break;
                }
            }

            Set<String> members = new ConcurrentSkipListSet<String>();

            if (errors.isEmpty()) {
                for (String filter : search) {
                    for (LdapGroup group : groupManager.searchGroups(filter, false)) {
                        if (group.getMembers() == null) {
                            continue;
                        }
                        members.addAll(group.getMembers());
                    }
                }

                result = new ResponseEntity<ApiResponse<Integer>>(new ApiResponse<Integer>(Constants.API_VERSION, LdapGroup.class.getName(), members.size()), HttpStatus.OK);
            }
        } catch (Exception e) {
            errors.add("An unexpected error has occurred. Contact the system administrator.");
            logger.error("Exception getting LDAP group search results from GroupManager.", e);
        }

        if (result == null) {
            //This would be an error condition (not an empty list)
            result = new ResponseEntity<ApiResponse<Integer>>(new ApiResponse<Integer>(Constants.API_VERSION, LdapGroup.class.getName(), 0, errors), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return result;
    }

    @RequestMapping(value = "/groupprefix", method = RequestMethod.GET)
    public ResponseEntity<ApiResponse<String>> getGroupPrefix(HttpServletResponse response) {

    	setCacheHeaders(response);
    	
    	List<String> errors = new ArrayList<String>();
    	
        ResponseEntity<ApiResponse<String>> result = null;
        
        try {
        	String groupPrefix = groupManager.getGroupPrefix();
        	
            result = new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION, String.class.getName(), groupPrefix), HttpStatus.OK);
        } catch (Exception e) { 
        	errors.add("An unexpected error has occurred. Contact the system administrator.");
            logger.error("Exception getting LDAP group prefix from GroupManager.", e);  
        }

        if (result == null) {
        	//This would be an error condition (not an empty list)
            result = new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION, String.class.getName(), null, errors), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        return result;
    }
}