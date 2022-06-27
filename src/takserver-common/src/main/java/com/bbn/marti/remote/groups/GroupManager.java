

package com.bbn.marti.remote.groups;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;

import javax.naming.directory.DirContext;

import com.bbn.marti.remote.LdapGroup;
import com.bbn.marti.remote.LdapUser;
import com.bbn.marti.remote.RemoteSubscription;

/*
 * Operations for keeping track of users and groups.
 * 
 * 
 */
public interface GroupManager {

    /*
     * Get all groups of which a user is a member.
     */
    NavigableSet<Group> getGroups(User user);
    
    /*
     * Get all users.
     */
    Collection<User> getAllUsers();
    
    /*
     * Get all connection ids.
     */
    Set<String> getAllConnectionIds();
    
    /*
     * Get all groups.
     */
    Collection<Group> getAllGroups();
   
    /*
     * Add user to group. Create user and group if they don't exist.
     */
    void addUserToGroup(User user, Group group);
    
    /*
     * Add user, creating if it doesn't exist.
     */
    void addUser(User user);
    
    /*
     * Get group by id and direction
     */
    Group getGroup(String id, Direction direction);
    
    /*
     * Find a User object by connectionId
     */
    User getUserByConnectionId(String connectionId);

    /*
     * Get all groups by connection id
     */
    NavigableSet<Group> getGroupsByConnectionId(String connectionId);
    
    /*
     * Get cached group vector by connection id
     */
    String getCachedOutboundGroupVectorByConnectionId(String connectionId);
    String getCachedInboundGroupVectorByConnectionId(String connectionId);

    /*
     * Returns true if the connectionId has any groups in common with the groupVector
     */
    boolean hasAccess(String connectionId, String groupVector);

    /*
     * Get the reachability strategy for a ConnectionInfo object.
     *
     * handler is an Object, not a ConnectionInfo, to keep this interface decoupled from the NIO code.
     */
    Reachability<User> getReachability(Object connectionInfo);
    
    /*
     * Remove a user from a group.
     * 
     */
    void removeUserFromGroup(User user, Group group);
    
    /*
     * Remove a user and associated group memberships
     * 
     */
    void removeUser(User user);    
    
    /*
     * Update group membership for a user, comparing the current group membership set with the provided group set. 
     * 
     */
    void updateGroups(User user, Set<Group> groups);
    
    /*
     * Get the set of groups that set a but not in set b
     * 
     */
    Set<Group> getGroupDiff(Set<Group> a, Set<Group> b);
    
    /*
     * Generate a Set<Group> from a list of group names.
     */
    Set<Group> createGroupSet(List<String> groupNames);

    /*
     * Finds a Set<Group> from a list of existing groups.
     */
    Set<Group> findGroups(List<String> groupNames);
    
    /*
     * Explicity track user by connectionId
     */
    void putUserByConnectionId(User user, String connectionId);
   
    /*
     * Set subscription for a user
     */
    void setSubscriptionForUser(User user, RemoteSubscription subscription);

    /*
     * Get subscription for a user
     */
    RemoteSubscription getSubscriptionForUser(User user);
    
    /*
     * Register an authenticator
     */
    void registerAuthenticator(String type, Authenticator<User> authenticator);

    /*
     * Sychronously authenticate, using the locally registered authenticator type
     */
    AuthResult authenticate(String type, User user);

    /*
     * Finds all active Core users for the current username and reauthenticates them
     */
    void authenticateCoreUsers(String type, String username);

    /**
     * Searches ldap groups (e.g., to help user configure items that require a group distinguished name reference)
     * 
     * @param groupNameFilter String optional filter applied to the LDAP cn attribute
     * @return List<LdapGroup> instances
     * @throws RemoteException
     */
    List<LdapGroup> searchGroups(String groupNameFilter);

    /*
     * lookup an ldap user
     *
     */
    LdapUser searchUser(String username);

    String getGroupPrefix();
    
    /*
     * Make a copy of this user and its current group membership, so that it be independently managed. The copied user will be assigned a randomly generated connection id.
     */
    User replicateUserAndGroupMembership(User user);

    /*
     * Fill in Group details using the configured cache / DB
     * 
     */
    Group hydrateGroup(Group group);
    
    /*
     * make an LDAP connection to the configured server
     * 
     */
    DirContext connectLdap();

    /*
     * add a new ldap user
     *
     */
    void addLdapUser(String emailAddress, String userName, String password, String[] groupNames);

    /*
     * returns true if connectLdap returns a directory context
     *
     */
    boolean testLdap();
    
    /*
     * Get the representation of a group vector, as a NavigableSet data structure 
     * 
     */
    NavigableSet<Group> groupVectorToGroupSet(String groupVector);

    /*
     * Get the representation of a group vector, filtered by group direction, as a NavigableSet data structure
     *
     */
    NavigableSet<Group> groupVectorToGroupSet(String groupVector, int direction);

    public List<User> getConnectedUsersById(String id);
}
