

package com.bbn.marti.groups;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import com.google.common.base.Strings;
import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;

import com.bbn.marti.cot.search.model.ApiResponse;
import com.bbn.marti.network.BaseRestController;
import com.bbn.marti.remote.config.CoreConfigFacade;
import com.bbn.marti.remote.groups.Direction;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.remote.groups.User;
import com.bbn.marti.remote.LdapGroup;
import com.bbn.marti.remote.SubscriptionManagerLite;
import com.bbn.marti.util.CommonUtil;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tak.server.Constants;
import tak.server.cache.ActiveGroupCacheHelper;


/*
 * 
 * API for groups and users
 * 
 * base path is /Marti/api
 * 
 */
@RestController
public class GroupsApi extends BaseRestController {
    
    Logger logger = LoggerFactory.getLogger(GroupsApi.class);

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private GroupManager groupManager;

    @Autowired
    private CommonUtil martiUtil;

    @Autowired
    private ActiveGroupCacheHelper activeGroupCacheHelper;

    @Autowired
    SubscriptionManagerLite subscriptionManager;

    private Map<String, String> descriptionMap = new ConcurrentHashMap<>();

    public GroupManager getGroupManager() {
        return groupManager;
    }

    public void setGroupManager(GroupManager groupManager) {
        this.groupManager = groupManager;
    }

    /*
     * get all users
     */
    @RequestMapping(value = "/users/all", method = RequestMethod.GET)
    public ResponseEntity<ApiResponse<SortedSet<User>>> getAllUsers() {

        ResponseEntity<ApiResponse<SortedSet<User>>> result = new ResponseEntity<ApiResponse<SortedSet<User>>>(new ApiResponse<SortedSet<User>>(Constants.API_VERSION, User.class.getName(), new ConcurrentSkipListSet<User>()), HttpStatus.OK);

        logger.trace("RMI groupManager: " + groupManager);

        if (groupManager == null) {
            return result;
        }
        
        SortedSet<User> userSet = null;
        
        // first sort by natural ordering (id, connectionId)
        try {
            userSet = new ConcurrentSkipListSet<User>(groupManager.getAllUsers());
        } catch (Exception e) { 
            logger.debug("exception getting users", e);  
        }

        SortedSet<User> users = null;
        
        // now sort by date descending
        try {
            users = new ConcurrentSkipListSet<User>(new Comparator<User>() {
                public int compare(User thisUser, User thatUser) {

                    Date thisDate = thisUser.getCreated();
                    Date thatDate = thatUser.getCreated();

                    if (thisDate != null && thatDate != null) {
                        return thatDate.compareTo(thisDate);
                    } else { 
                        return thatUser.getId().compareTo(thisUser.getId());
                    }
                }
            });
            
            users.addAll(userSet);
        } catch (Exception e) { 
            logger.debug("exception getting users", e);  
        }

        if (users != null) {
            result = new ResponseEntity<ApiResponse<SortedSet<User>>>(new ApiResponse<SortedSet<User>>(Constants.API_VERSION, User.class.getName(), users), HttpStatus.OK);
        } else {
            logger.trace("null user set");
        }

        return result;
    }

    /*
     * get a user, and group memberships, by id
     */
    @RequestMapping(value = "/users/{connectionId:.+}", method = RequestMethod.GET)
    public ResponseEntity<ApiResponse<UserGroups>> getUser(@PathVariable("connectionId") String connectionId) {
        
        ResponseEntity<ApiResponse<UserGroups>> result = new ResponseEntity<ApiResponse<UserGroups>>(new ApiResponse<UserGroups>(Constants.API_VERSION, User.class.getName(), new UserGroups()), HttpStatus.NOT_FOUND);

        logger.trace("RMI groupManager: " + groupManager);

        if (groupManager == null) {
            return result;
        }

        User user = null;
        Set<Group> groups = null;
        
        try {
            user = groupManager.getUserByConnectionId(connectionId);
            groups = groupManager.getGroups(user);
        } catch (Exception e) {
          logger.debug("exception getting user and groups", e);  
        }
        
        UserGroups userGroups = new UserGroups();
        userGroups.user = user;
        userGroups.groups = groups;

        if (user != null) {
            result = new ResponseEntity<ApiResponse<UserGroups>>(new ApiResponse<UserGroups>(Constants.API_VERSION, User.class.getName(), userGroups), HttpStatus.OK);
        } else {
            logger.trace("null user");
        }
        
        return result;
    }
    
    /*
     * get a user, and group memberships, by id
     */
    @RequestMapping(value = "/groups/{name}/{direction:.+}", method = RequestMethod.GET)
    public ResponseEntity<ApiResponse<Group>> getGroup(@PathVariable("name") String name, @PathVariable("direction") Direction direction) {
        
        ResponseEntity<ApiResponse<Group>> result = new ResponseEntity<ApiResponse<Group>>(new ApiResponse<Group>(Constants.API_VERSION, Group.class.getName(), null), HttpStatus.NOT_FOUND);

        logger.trace("RMI groupManager: " + groupManager);

        if (groupManager == null) {
            return result;
        }
        
        Group group = null;
        
        try {
            logger.trace("getting group for " + name + " " + direction);
            group = groupManager.getGroup(name, direction);
        } catch (Exception e) {
          logger.debug("exception getting and groups", e);  
        }
        
        if (group != null) {
           
            return new ResponseEntity<ApiResponse<Group>>(new ApiResponse<Group>(Constants.API_VERSION, Group.class.getName(), group), HttpStatus.OK);
        } else {
            logger.trace("null group");
        }
        
        return result;
    }
    
    /*
     * get all groups
     * 
     */
    @RequestMapping(value = "/groups/all", method = RequestMethod.GET)
    public ResponseEntity<ApiResponse<Collection<Group>>> getAllGroups(
            @RequestParam(value = "useCache", defaultValue = "false") boolean useCache,
            @RequestParam(value = "sendLatestSA", defaultValue = "false") boolean sendLatestSA
    ) throws RemoteException {

        Collection<Group> groups = null;
        if (martiUtil.isAdmin()) {
            // admins can see all groups that have been loaded
            groups = groupManager.getAllGroups();
        } else {

            if (useCache) {
                String username = SecurityContextHolder.getContext().getAuthentication().getName();
                groups = activeGroupCacheHelper.getActiveGroupsForUser(username);

                if (sendLatestSA) {
                    subscriptionManager.sendLatestReachableSA(username);
                }
            }

            if (groups == null || groups.size() == 0) {

                // regular users only see groups they have access to
                groups = martiUtil.getGroupsFromRequest(request);

                // if this was a cache miss, return IN groups too
                if (!useCache) {
                    // the call to getAllGroups above only returns OUT groups. Since the GroupDAO doesn't persist direction
                    // only OUT groups are passed back through getAllGroups. getGroupsFromRequest contains IN groups too
                    // so for consistency we'll filter them out here.
                    for (Group group : groups) {
                        if (group.getDirection() != Direction.OUT) {
                            groups.remove(group);
                        }
                    }
                }
            }

            try {
                if (CoreConfigFacade.getInstance().getRemoteConfiguration().getAuth().getDefault().equalsIgnoreCase("ldap")) {
                    for (Group group : groups) {
                        String description = descriptionMap.get(group.getName());
                        if (description == null) {
                            List<LdapGroup> ldapGroups = groupManager.searchGroups(group.getName(), true);
                            if (ldapGroups.size() == 0) {
                                logger.debug("unable to find description for group! " + group.getName());
                                continue;
                            } else if (ldapGroups.size() > 1) {
                                logger.error("found more than one result for group! " + group.getName());
                            }

                            LdapGroup ldapGroup = ldapGroups.get(0);
                            description = ldapGroup.getDescription();
                            if (description == null) {
                                description = "";
                            }
                            descriptionMap.put(group.getName(), description);
                        }

                        if (!Strings.isNullOrEmpty(description)) {
                            group.setDescription(description);
                        }
                    }
                }
            } catch (Exception e) {
				logger.error("exception getting group description", e);
            }

            //
            // Ensure that all groups meet the ATAK ServerGroup.isValid check
            //
            for (Group g : groups)  {
                if (g.getBitpos() == null || g.getBitpos() < 0) {
                    g.setBitpos(0);
                }
                if (g.getCreated() == null) {
                    g.setCreated(new Date());
                }
                if (g.getType() == null) {
                    g.setType(Group.Type.SYSTEM);
                }
                if (g.getDirection() == null) {
                    g.setDirection(Direction.OUT);
                }
            }

        }

        return new ResponseEntity<ApiResponse<Collection<Group>>>(new ApiResponse<Collection<Group>>(
                Constants.API_VERSION, Group.class.getName(), groups), HttpStatus.OK);
    }
    
    public static class UserGroups {
        public User user;
        public Set<Group> groups;
    }

    @RequestMapping(value = "/groups/groupCacheEnabled", method = RequestMethod.GET)
    public ResponseEntity<ApiResponse<Boolean>> getGroupCacheEnabled() throws RemoteException {

        boolean groupCacheEnabled = false;
        try {
            groupCacheEnabled = CoreConfigFacade.getInstance().getRemoteConfiguration().getAuth().isX509UseGroupCache();
        } catch (Exception e) {
            logger.error("exception in getGroupCacheEnabled", e);
        }

        return new ResponseEntity<ApiResponse<Boolean>>(new ApiResponse<Boolean>(
                Constants.API_VERSION, Boolean.class.getName(), groupCacheEnabled), HttpStatus.OK);
    }
}