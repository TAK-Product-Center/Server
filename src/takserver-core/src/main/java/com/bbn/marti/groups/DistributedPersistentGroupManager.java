

package com.bbn.marti.groups;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.ignite.services.Service;
import org.apache.ignite.services.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.config.Auth;
import com.bbn.marti.config.Configuration;
import com.bbn.marti.config.LdapSecurityType;
import com.bbn.marti.remote.LdapGroup;
import com.bbn.marti.remote.RemoteSubscription;
import com.bbn.marti.remote.exception.NotFoundException;
import com.bbn.marti.remote.exception.TakException;
import com.bbn.marti.remote.groups.AuthResult;
import com.bbn.marti.remote.groups.AuthenticatedUser;
import com.bbn.marti.remote.groups.Authenticator;
import com.bbn.marti.remote.groups.ConnectionType;
import com.bbn.marti.remote.groups.Direction;
import com.bbn.marti.remote.groups.FederateUser;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.remote.groups.Reachability;
import com.bbn.marti.remote.groups.User;
import com.bbn.marti.remote.groups.UserClassification;
import com.bbn.marti.remote.LdapUser;
import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.marti.util.MessagingDependencyInjectionProxy;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import tak.server.ignite.cache.IgniteCacheHolder;

/*
 * 
 * GroupManager implementation that caches groups using java concurrent data structures, backed by database persistence. 
 * 
 * Users (which are in effect "connected users"), and group membership for users, is only tracked in memory (not persisted.)
 * 
 */
public class DistributedPersistentGroupManager implements GroupManager, Service {
    
    private static final long serialVersionUID = 8868582877314994961L;

	private static final String CN_ATTR_NM = "cn";
    private static final String DESCRIPTION_ATTR_NM = "description";
    private static final String MEMBER_ATTR_NM = "member";

    private GroupDao groupDao() {
		return MessagingDependencyInjectionProxy.getInstance().groupDao();
	}
    
    private GroupStore groupStore;
    
    private Logger logger = LoggerFactory.getLogger(DistributedPersistentGroupManager.class);
    
    private final Reachability<User> reachability = new CommonGroupDirectedReachability(this);
    
    private GroupStore groupStore() {
    	return groupStore;
    }
    
    private Configuration config() {
    	return MessagingDependencyInjectionProxy.getInstance().simpleCoreConfig().getRemoteConfiguration();
    }
    
    public DistributedPersistentGroupManager(GroupStore groupStore) {
    	this.groupStore = groupStore;
    	if (logger.isDebugEnabled()) {
    		logger.debug("Construct " + getClass().getSimpleName());
    	}
    }

    private String getDnAttributeName() {
        try {
            if (config() != null
            &&  config().getAuth() != null
            &&  config().getAuth().getLdap() != null) {
                return config().getAuth().getLdap().getDnAttributeName();
            }
        } catch (Exception e) {
            logger.error("exception in getDnAttributeName", e);
        }

        return "distinguishedName";
    }

	@Override
	public void cancel(ServiceContext ctx) {
		if (logger.isDebugEnabled()) {
			logger.debug(getClass().getSimpleName() + " service cancelled");
		}
	}

	@Override
	public void init(ServiceContext ctx) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("init method " + getClass().getSimpleName());
		}
	}

	@Override
	public void execute(ServiceContext ctx) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("execute method " + getClass().getSimpleName());
		}
	}

    @Override
    public NavigableSet<Group> getGroups(User user) {

        if (user == null) {
            return new ConcurrentSkipListSet<>();
        }

        NavigableSet<Group> groups = groupStore().getUserGroupMap().get(user);

        if (groups != null) {
            return groups;
        }

        // return an empty set in case of a user that has not yet been saved, such as initial auth failure.
        return new ConcurrentSkipListSet<>();
    }

    @Override
    public void addUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("null user");
        }
        
        if (logger.isDebugEnabled()) {
        	logger.debug("adding " + user + " if it doesn't exist");
        }
        
        // track users by connectionId
        groupStore().getConnectionIdUserMap().putIfAbsent(user.getConnectionId(), user);
        
        // add user group map entry for this user
        groupStore().getUserGroupMap().putIfAbsent(user, new ConcurrentSkipListSet<Group>());
        
        if (shouldCacheUser(user)) {
        	IgniteCacheHolder.getIgniteUserOutboundGroupCache().put(user.getConnectionId(), getOutboundGroupVector(user));
            IgniteCacheHolder.getIgniteUserInboundGroupCache().put(user.getConnectionId(), getInboundGroupVector(user));
        }
    }
    
    @Override
    public void putUserByConnectionId(User user, String connectionId) {
        if (user == null || Strings.isNullOrEmpty(connectionId)) {
        	if (logger.isDebugEnabled()) {
        		logger.debug("null user or connectionId");
        	}
            return;
        }
                
        // track users by connectionId
        groupStore().getConnectionIdUserMap().putIfAbsent(connectionId, user);
        
        user.getConnectionType();

        if (shouldCacheUser(user)) {
        	IgniteCacheHolder.getIgniteUserOutboundGroupCache().put(connectionId, getOutboundGroupVector(user));
            IgniteCacheHolder.getIgniteUserInboundGroupCache().put(connectionId, getInboundGroupVector(user));
        }
    }
    
    @Override
    public void removeUser(final User user) {

        if (user == null) {
            throw new IllegalArgumentException("null user");
        }

        // remove user from all groups
        try {
            updateGroups(user, new ConcurrentSkipListSet<Group>());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        if (shouldCacheUser(user)) {
        	 IgniteCacheHolder.getIgniteUserOutboundGroupCache().remove(user.getConnectionId());
             IgniteCacheHolder.getIgniteUserInboundGroupCache().remove(user.getConnectionId());
        }
        
        User removedUser = groupStore().getConnectionIdUserMap().remove(user.getConnectionId());

        if (logger.isDebugEnabled()) {
        	if (removedUser == null) {
        		logger.debug(user + " was not found during removal.");
        	}
        }

        // remove user from user -> group map
        groupStore().getUserGroupMap().remove(user);

        if (logger.isDebugEnabled()) {
        	logger.debug("removal complete for " + user);
        }
    }

    @Override
    public void addUserToGroup(User user, Group group) {
        
        if (user == null) {
            throw new IllegalArgumentException("null user");
        }
        
        if (group == null) {
            throw new IllegalArgumentException("null group");
        }
        
        addUser(user);
        
        if (logger.isDebugEnabled()) {
        	logger.debug("adding " + user.getName() + " to " + group.getName() + ":" + group.getDirection());
        }
        
        Group storedGroup = hydrateGroup(group);
        
        storedGroup.addNeighbor(user);
        
        NavigableSet<Group> userGroups = groupStore().getUserGroupMap().get(user);
        
        // add this group to the user group list for this user if not already present
        userGroups.add(storedGroup);
        
        if (shouldCacheUser(user)) {
        	IgniteCacheHolder.getIgniteUserOutboundGroupCache().put(user.getConnectionId(), getOutboundGroupVector(user));
            IgniteCacheHolder.getIgniteUserInboundGroupCache().put(user.getConnectionId(), getInboundGroupVector(user));
        }
        
        if (logger.isDebugEnabled()) {
        	logger.debug("add user " + user.getId() + " to " + group);
        }
    }

    @Override
    public void setClassificationForUser(User user, UserClassification userClassification) {
        groupStore().getUserClassificationMap().put(user, userClassification);
    }

    @Override
    public UserClassification getClassificationForUser(User user) {
        return groupStore().getUserClassificationMap().get(user);
    }

    /*
     * Get / save the group from the cache / DB hierarchy
     * 
     */
    @Override
    public Group hydrateGroup(Group group) {
        // add group to the master group list, and put the user in it. 
        Group storedGroup = groupStore().getGroups().putIfAbsent(getGroupMapKey(group), group);
        
        // The group is now cached
        
        // ensure that storedGroup always points to the right group object, to account for the semantics of putIfAbsent. 
        if (storedGroup == null) {
            // since the group was not previously cached, it needs to be persisted
            storedGroup = group;
            
            if (config().getRepository().isEnable()) {
                groupDao().save(storedGroup);
            }
        }
        
        return storedGroup;
    }
    
    @Override
    public void removeUserFromGroup(User user, Group group) {
        
        if (user == null) {
            throw new IllegalArgumentException("null user");
        }
        
        if (group == null) {
            throw new IllegalArgumentException("null group");
        }
        
        if (logger.isDebugEnabled()) {
        	logger.debug("removing " + user.getId() + " from " + group.getName() + ":" + group.getDirection());
        }
        
        NavigableSet<Group> userGroups = groupStore().getUserGroupMap().get(user);
        
        // get the stored group
        group = groupStore().getGroups().get(getGroupMapKey(group));

        if (group == null) {
        	if (logger.isDebugEnabled()) {
        		logger.debug(user + " not a member of expected group " + group);
        	}
            return;
        }
        
        if (logger.isTraceEnabled()) {
        	logger.trace("pre-removal " + group + " members: " + group.getNeighbors());
        }
        
        // remove user from group
        group.getNeighbors().remove(user);
        
        if (logger.isTraceEnabled()) {
        	logger.trace("post-removal " + group + " members: " + group.getNeighbors());
        }

        if (userGroups != null) {
            // remove group from user -> group map
            userGroups.remove(group);
        }
        
        if (shouldCacheUser(user)) {
        	IgniteCacheHolder.getIgniteUserOutboundGroupCache().put(user.getConnectionId(), getOutboundGroupVector(user));
            IgniteCacheHolder.getIgniteUserInboundGroupCache().put(user.getConnectionId(), getInboundGroupVector(user));
        }
    }

    @Override
    public Collection<User> getAllUsers() {
       return groupStore().getConnectionIdUserMap().values();
    }

    /*
     * Fetches a list of all groups, and warms the group cache 
     */
    @Override
    public Collection<Group> getAllGroups() {
        
        logger.debug("getAllGroups");
        
        NavigableSet<Group> allGroups = groupDao().fetchAll();
        
        for (Group group : allGroups) {
        	groupStore().getGroups().putIfAbsent(getGroupMapKey(group), group);
        }
        
       return allGroups;
        
    }
    
    @Override
    public User getUserByConnectionId(String connectionId) {
        User ret = null;
        try {
           ret = groupStore().getConnectionIdUserMap().get(connectionId);
        } catch (Exception e) { /* ignore */ }
        return ret;
    }
    
    @Override
	public List<User> getConnectedUsersById(String id) {
		return groupStore().getConnectionIdUserMap().values().stream().filter(user -> user.getId().equals(id)).collect(Collectors.toList());
	}

    @Override
    public NavigableSet<Group> getGroupsByConnectionId(String connectionId) {

        NavigableSet<Group> results = new ConcurrentSkipListSet<>();

        NavigableSet<Group> groups = getGroups(getUserByConnectionId(connectionId));
        if (groups != null) {
            for (Group group : groups) {
                // return a copy of the group, without neighbors populated
                results.add(group.getCopy());
            }
        }

        return  results;
    }

    @Override
    public UserClassification getUserClassificationByConnectionId(String connectionId) {
        return getClassificationForUser(getUserByConnectionId(connectionId));
    }

    @Override
    public String getCachedOutboundGroupVectorByConnectionId(String connectionId) {
        return IgniteCacheHolder.getIgniteUserOutboundGroupCache().get(connectionId);
    }
    
    @Override
    public String getCachedInboundGroupVectorByConnectionId(String connectionId) {
        return IgniteCacheHolder.getIgniteUserInboundGroupCache().get(connectionId);
    }

    @Override
    public boolean hasAccess(String connectionId, String groupVector) {
        NavigableSet<Group> callerGroups = RemoteUtil.getInstance().getGroupsForBitVectorString(
                groupVector, getAllGroups());
        NavigableSet<Group> subGroups = getGroupsByConnectionId(connectionId);
        return Sets.intersection(callerGroups, subGroups).size() != 0;
    }
    
    @Override
    public Reachability<User> getReachability(Object connection) {
    	return reachability;
    }

    @Override
    public Set<String> getAllConnectionIds() {
        
        return groupStore().getConnectionIdUserMap().keySet();
    }
    
    @Override
    public Set<Group> getGroupDiff(Set<Group> a, Set<Group> b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("null Set<Group> a or b");
        }
        
        return Sets.difference(a, b);
    }

    @Override
    public Set<Group> createGroupSet(List<String> groupNames) {
        
        if (groupNames == null) {
            throw new IllegalArgumentException("null groupNames list");
        }
        
        Set<Group> groupSet = new ConcurrentSkipListSet<>();
        
        for (String groupName : groupNames) {
            groupSet.add(new Group(groupName, Direction.IN));
            groupSet.add(new Group(groupName, Direction.OUT));
        }
        
        return groupSet;
    }

    @Override
    public Set<Group> findGroups(List<String> groupNames) {

        NavigableSet<Group> groups = new ConcurrentSkipListSet<>();
        for (String groupName : groupNames) {

            if (Strings.isNullOrEmpty(groupName)) {
                throw new IllegalArgumentException("empty 'group' request parameter. A group must be specified when creating a mission.");
            }

            // resolve the referenced group
            Group group = hydrateGroup(new Group(groupName, Direction.IN));

            if (group == null) {
                group = hydrateGroup(new Group(groupName, Direction.OUT));
            }

            if (group == null) {
                throw new NotFoundException("The specified group: " + groupName + " does not exist");
            }

            groups.add(group);
        }

        return groups;
    }

    @Override
    public void updateGroups(User user, Set<Group> newGroups) {
        
        Set<Group> current = getGroups(user);
        
        // process removals
        Set<Group> removals = getGroupDiff(current, newGroups);
        
        if (logger.isDebugEnabled()) {
        	logger.debug("removals for " + user + ": " + removals);
        }
        
        for (Group group : removals) {
            removeUserFromGroup(user, group);
        }
        
        // process adds
        Set<Group> adds = getGroupDiff(newGroups,  current);
        
        if (logger.isDebugEnabled()) {
        	logger.debug("adds for " + user + ": " + adds);
        }
        
        for (Group group : adds) {
            addUserToGroup(user, group);
        }
    }
    
    @Override
    public void setSubscriptionForUser(User user, RemoteSubscription subscription) {
        
        if (user == null || subscription == null) {
            return;
        }
        
        groupStore().getUserSubscriptionMap().put(user, subscription);
    }
    
    @Override
    public RemoteSubscription getSubscriptionForUser(User user)  {
        if (user == null) {
            throw new IllegalArgumentException("null user");
        }
        
        RemoteSubscription subscription = groupStore().getUserSubscriptionMap().get(user);
        
        if (subscription == null) {
            throw new IllegalStateException("null subscription in user subscription map");
        }
        
        return subscription;
    }

    @Override
    public AuthResult authenticate(String type, User user) {
        
    	if (logger.isTraceEnabled()) {
    		logger.trace("processing getAuthenticator request for type " + type);
    	}
        
        if (Strings.isNullOrEmpty(type)) {
            throw new IllegalArgumentException("empty type");
        }
        
        if (user == null) {
            throw new IllegalArgumentException("null user");
        }
        
        Authenticator<User> auth = groupStore().getAuthenticatorMap().get(type);
        
        if (logger.isTraceEnabled()) {
        	logger.trace("authenticator: " + auth);
        }
        
        if (auth != null) {
            return auth.authenticate(user);
        } 
        
        throw new TakException(type + " authenticator not registered");
    }

    @Override
    public void authenticateCoreUsers(String username) {
        try {
            for (User user : getAllUsers()) {
                if (user.getConnectionType() == ConnectionType.CORE && user.getName().equals(username)) {
                    try {

                        String type = "X509";
                        if (user.getCert() == null) {
                            type = "oauth";
                        }

                        authenticate(type, user);
                    } catch (Exception e) {
                        logger.error("exception in authenticateCoreUsers for user : " + username, e);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("exception in authenticateCoreUsers!", e);
        }
    }

    @Override
    public void registerAuthenticator(String type, Authenticator<User> authenticator) {
    	groupStore().getAuthenticatorMap().putIfAbsent(type, authenticator);
        
        if (logger.isDebugEnabled()) {
        	logger.debug(type + " registered " + authenticator);
        }
    }
    // define the key for storing groups in a map. Storing in a set is by virtue of Group.equals(), but maps need help.
    private String getGroupMapKey(Group group) {
        return group.getName() + "_" + group.getDirection();
    }

    @Override
    public Group getGroup(String id, Direction direction) {
    	if (logger.isTraceEnabled()) {
    		logger.trace("getGroup " + id + ", " + direction);
    	}
        return hydrateGroup(new Group(id, direction));
    }
    
    @Override
    public DirContext connectLdap() {
        
        Auth.Ldap ldapConfig = config().getAuth().getLdap();
        
        String serviceAccountDN = null;
        String serviceAccountCredential = null;
        
        if (ldapConfig == null) {
            logger.error("CoreConfig is missing the LDAP configuration element needed to perform ldap queries.");
            throw new RuntimeException("Invalid configuration for LDAP queries.");
        } else {
            //The following two attributes may be null unless the LDAP security type is set to simple
            serviceAccountDN = ldapConfig.getServiceAccountDN();
            serviceAccountCredential = ldapConfig.getServiceAccountCredential();

            if (ldapConfig.getLdapSecurityType() == LdapSecurityType.SIMPLE && 
                    (serviceAccountDN == null || serviceAccountCredential.trim().length() == 0 
                    || serviceAccountCredential == null || serviceAccountCredential.trim().length() == 0)) {
                logger.error("CoreConfig specifies simple LDAP authentication but is missing the service account or credential.");
                throw new RuntimeException("Invalid configuration for LDAP queries.");
            }
        }

        //The remaining attributes are expected to be provided if the LDAP element exists (since the remaining attributes are labelled required in the XSD)

        String connectionDN = ldapConfig.getUrl().trim();

        if (logger.isTraceEnabled()) {
            logger.trace("LDAP Group Search Parameters:");
            logger.trace("*Connection DN={}", connectionDN);
            logger.trace("*LDAP Security Type={}", ldapConfig.getLdapSecurityType().value());
            logger.trace("*Service Account DN={}", serviceAccountDN);
            logger.trace("*Group Class={}", ldapConfig.getGroupObjectClass());
            logger.trace("*Group Base DN={}", ldapConfig.getGroupBaseRDN());
        }
                
        Hashtable<String, Object> properties = new Hashtable<String, Object>();
        
        properties.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        properties.put(Context.PROVIDER_URL, connectionDN);
        properties.put(Context.SECURITY_AUTHENTICATION, ldapConfig.getLdapSecurityType().value());

        if (connectionDN.toLowerCase().contains("ldaps")) {
            properties.put(Context.SECURITY_PROTOCOL,"ssl");
            properties.put("java.naming.ldap.factory.socket", "com.bbn.marti.groups.LdapSSLSocketFactory");
        }

        if (ldapConfig.getLdapSecurityType() == LdapSecurityType.SIMPLE) {
            properties.put(Context.SECURITY_PRINCIPAL, serviceAccountDN.trim());
            properties.put(Context.SECURITY_CREDENTIALS, serviceAccountCredential.trim());
        }

        try {
            return new InitialDirContext(properties);
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void addLdapUser(String emailAddress, String userName, String password, String[] groupNames) {

        DirContext context = connectLdap();

        Auth.Ldap ldapConfig = config().getAuth().getLdap();
        String entryDN = "cn=" + userName + "," + ldapConfig.getUserBaseRDN();

        // create the user
        try {
            BasicAttributes entry = new BasicAttributes();

            // set the CN
            Attribute cn = new BasicAttribute("cn", userName);
            entry.put(cn);

            // set the sAMAccountName
            Attribute sAMAccountName = new BasicAttribute("sAMAccountName", userName);
            entry.put(sAMAccountName);

            // set the email
            Attribute email = new BasicAttribute("mail", emailAddress);
            entry.put(email);

            // set the password
            final byte[] quotedPasswordBytes = ('"'+password+'"').getBytes("UTF-16LE");
            Attribute unicodePwd = new BasicAttribute("unicodePwd", quotedPasswordBytes);
            entry.put(unicodePwd);

            // set the objectClass
            Attribute oc = new BasicAttribute("objectClass");
            oc.add("top");
            oc.add("person");
            oc.add("organizationalPerson");
            oc.add("user");
            entry.put(oc);

            // enable the account
            int UF_ACCOUNT_ENABLE = 0x0001;
            int UF_DONT_EXPIRE_PASSWD = 0x10000;
            Attribute userAccountControl = new BasicAttribute("userAccountControl",
                    Integer.toString(UF_ACCOUNT_ENABLE | UF_DONT_EXPIRE_PASSWD));
            entry.put(userAccountControl);

            // add the new user
            context.createSubcontext(entryDN, entry);
        } catch (Exception e) {
            logger.error("exception in addLdapUser when adding new user! : " + e.getMessage());
        }

        // update the user's password
        try {
            final byte[] quotedPasswordBytes = ('"'+password+'"').getBytes("UTF-16LE");
            Attribute unicodePwd = new BasicAttribute("unicodePwd", quotedPasswordBytes);
            ModificationItem[] mods = new ModificationItem[1];
            mods[0] =new ModificationItem(DirContext.REPLACE_ATTRIBUTE, unicodePwd);
            context.modifyAttributes(entryDN, mods);
        } catch (Exception e) {
            logger.error("exception in addLdapUser when updating user's password! : " + e.getMessage());
        }

        // create the group
        try {
            for (String group : groupNames) {
                // create the state group if needed
                hydrateGroup(new Group(group, Direction.IN));
                hydrateGroup(new Group(group, Direction.OUT));

                Attribute objClasses = new BasicAttribute("objectClass");
                objClasses.add("top");
                objClasses.add("group");

                Attribute cn = new BasicAttribute("cn", group);
                Attribute groupType = new BasicAttribute("groupType", "2147483650"); // security group

                // Add these to the container
                Attributes container = new BasicAttributes();
                container.put(objClasses);
                container.put(cn);
                container.put(groupType);

                String groupDn = "cn=" + group + "," + ldapConfig.getGroupBaseRDN();
                context.createSubcontext(groupDn, container);
            }

        } catch (Exception e) {
            logger.error("exception in addLdapUser when creating group! : " + e.getMessage());
        }

        // add the user to the group
        try {
            String url = ldapConfig.getUrl();
            String domain = url.substring(url.lastIndexOf("/") + 1);

            for (String group : groupNames) {
                ModificationItem[] mods = new ModificationItem[1];
                String groupDn = "cn=" + group + "," + ldapConfig.getGroupBaseRDN();
                Attribute mod = new BasicAttribute("member", entryDN + "," + domain);
                mods[0] = new ModificationItem(DirContext.ADD_ATTRIBUTE, mod);
                context.modifyAttributes(groupDn, mods);
            }

        } catch (Exception e) {
            logger.error("exception in addLdapUser when setting group! : " + e.getMessage());
        }
    }

    @Override
    public boolean testLdap() {
        try {
            connectLdap();
            return true;
        } catch (Exception e) {
            logger.error("Exception in testLdap!", e);
            return false;
        }
    }
    
    @Override
    public List<LdapGroup> searchGroups(String groupNameFilter, boolean exactMatch) {
    	
    	List<LdapGroup> results = new ArrayList<LdapGroup>();
    	
    	DirContext context = null;

    	Auth.Ldap ldapConfig = config().getAuth().getLdap();
    	
    	if (ldapConfig == null) {
    		logger.error("CoreConfig is missing the LDAP configuration element needed to perform ldap queries.");
    		throw new RuntimeException("Invalid configuration for LDAP queries.");
    	} 

        try {
			context = connectLdap();
			
			String groupFilter = null;
			
			Object[] args = null;
			
			if (groupNameFilter != null && groupNameFilter.trim().length() > 0) {
			    if (exactMatch) {
                    groupFilter = "(&(objectclass={0})(CN={1}))";
                } else {
                    groupFilter = "(&(objectclass={0})(CN=*{1}*))";
                }

				args = new Object[2];
				args[0] = ldapConfig.getGroupObjectClass().trim();
				args[1] = groupNameFilter.trim();
			} else {
				args = new Object[1];
				args[0] = ldapConfig.getGroupObjectClass().trim();
				groupFilter = "(objectclass={0})";
			}
			
			SearchControls searchControls = new SearchControls();

			searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
			String[] attributeNames = new String[4];
			attributeNames[0] = CN_ATTR_NM;
			attributeNames[1] = getDnAttributeName();
			attributeNames[2] = DESCRIPTION_ATTR_NM;
			attributeNames[3] = MEMBER_ATTR_NM;

			searchControls.setReturningAttributes(attributeNames);
			
			NamingEnumeration<SearchResult> namingEnumeration = null;
			
			namingEnumeration = context.search(ldapConfig.getGroupBaseRDN().trim(), groupFilter, args, searchControls);
			
			while (namingEnumeration != null && namingEnumeration.hasMore()) {
				SearchResult searchResult = namingEnumeration.next();

				String cn = "";
				String dn = "";

				try {
                    if (searchResult.getAttributes().get(CN_ATTR_NM) != null) {
                        cn = ((String) searchResult.getAttributes().get(CN_ATTR_NM).get());
                    }
                } catch (Exception e) {
				    logger.error("exception getting CN", e);
                }

                try {
                    if (searchResult.getAttributes().get(getDnAttributeName()) != null) {
                        dn = ((String) searchResult.getAttributes().get(getDnAttributeName()).get());
                    }
                } catch (Exception e) {
                    logger.error("exception getting DN", e);
                }

				LdapGroup ldapGroup = new LdapGroup(cn, dn);

                try {
                    if (searchResult.getAttributes().get(DESCRIPTION_ATTR_NM) != null) {
                        ldapGroup.setDescription(((String) searchResult.getAttributes().get(DESCRIPTION_ATTR_NM).get()));
                    }
                } catch (Exception e) {
                    logger.error("exception getting description", e);
                }

                try {
                    if (searchResult.getAttributes().get(MEMBER_ATTR_NM) != null) {
                        Set<String> members = new ConcurrentSkipListSet<String>();
                        NamingEnumeration ne = searchResult.getAttributes().get(MEMBER_ATTR_NM).getAll();
                        while (ne.hasMore()) {
                            members.add(ne.next().toString());
                        }
                        ldapGroup.setMembers(members);
                    }
                } catch (Exception e) {
                    logger.error("exception getting member", e);
                }

                results.add(ldapGroup);
			}
		} catch (Exception e) {
			logger.error("Unexpected error working with directory context.", e);
			throw new RuntimeException("Unexpected error working with directory context.");
		} finally {
			if (context != null) {
				try {
					context.close();
				} catch (NamingException e) {
					//Do nothing; releasing resources
					logger.error("Unexpected error releasing directory context.", e);
				}
			}
		}

        return results;
    }

    @Override
    public LdapUser searchUser(String username) {

        DirContext context = null;

        Auth.Ldap ldapConfig = config().getAuth().getLdap();

        if (ldapConfig == null) {
            logger.error("CoreConfig is missing the LDAP configuration element needed to perform ldap queries.");
            throw new RuntimeException("Invalid configuration for LDAP queries.");
        }

        if (ldapConfig == null) {
            logger.error("searchUsers called with null username!");
            return null;
        }

        try {
            context = connectLdap();

            String userFilter = "(&(objectclass={0})(sAMAccountName={1}))";
            Object[] args = new Object[] {
                ldapConfig.getUserObjectClass().trim(),
                username.trim()
            };

            SearchControls searchControls = new SearchControls();
            searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);

            ArrayList<String> queryParams = new ArrayList<>();
            queryParams.add(CN_ATTR_NM);
            queryParams.add(getDnAttributeName());
            queryParams.add(DESCRIPTION_ATTR_NM);

            if (ldapConfig.getCallsignAttribute() != null) {
                queryParams.add(ldapConfig.getCallsignAttribute());
            }

            if (ldapConfig.getColorAttribute() != null) {
                queryParams.add(ldapConfig.getColorAttribute());
            }

            if (ldapConfig.getRoleAttribute() != null) {
                queryParams.add(ldapConfig.getRoleAttribute());
            }

            String[] attributeNames = queryParams.toArray(new String[0]);

            searchControls.setReturningAttributes(attributeNames);

            NamingEnumeration<SearchResult> namingEnumeration = context.search(
                    ldapConfig.getUserBaseRDN().trim(), userFilter, args, searchControls);

            if (namingEnumeration != null && namingEnumeration.hasMore()) {
                SearchResult searchResult = namingEnumeration.next();
                Attributes attributes = searchResult.getAttributes();

                String cn = (String)attributes.get(CN_ATTR_NM).get();
                String dn = (String)attributes.get(getDnAttributeName()).get();

                String desc = null;
                if (attributes.get(DESCRIPTION_ATTR_NM) != null) {
                    desc = (String)attributes.get(DESCRIPTION_ATTR_NM).get();
                }

                String callsign = null;
                if (ldapConfig.getCallsignAttribute() != null && attributes.get(ldapConfig.getCallsignAttribute()) != null) {
                    callsign = (String)attributes.get(ldapConfig.getCallsignAttribute()).get();
                }

                String color = null;
                if (ldapConfig.getColorAttribute() != null && attributes.get(ldapConfig.getColorAttribute()) != null) {
                    color = (String)attributes.get(ldapConfig.getColorAttribute()).get();
                }

                String role = null;
                if (ldapConfig.getRoleAttribute() != null && attributes.get(ldapConfig.getRoleAttribute()) != null) {
                    role = (String)attributes.get(ldapConfig.getRoleAttribute()).get();
                }

                LdapUser ldapUser = new LdapUser(cn, dn, desc, callsign, color, role);
                return ldapUser;
            }
        } catch (Exception e) {
            logger.error("Unexpected error working with directory context.", e);
        } finally {
            if (context != null) {
                try {
                    context.close();
                } catch (NamingException e) {
                    logger.error("Unexpected error releasing directory context.", e);
                }
            }
        }

        return null;
    }

    @Override
    public String getGroupPrefix() {
    	
    	String result = "";
    	
    	Auth.Ldap ldapConfig = config().getAuth().getLdap();
    	
    	if (ldapConfig == null) {
    		//Probably better to throw exception here than return an empty string
    		logger.error("CoreConfig is missing the LDAP configuration element needed to perform ldap queries.");
    		throw new RuntimeException("Invalid configuration for LDAP queries.");
    	} else {
    		if (ldapConfig.getGroupprefix() != null) {
    			result = ldapConfig.getGroupprefix().trim();
    		}
    	}

        return result;
    }

    @Override
    public User replicateUserAndGroupMembership(User a) {

        // get a's groups
        NavigableSet<Group> groups = this.getGroups(a);
        
        // duplicate user a
        User b;
        if (a instanceof AuthenticatedUser) {
            b = new AuthenticatedUser((AuthenticatedUser)a);
        } else if (a instanceof FederateUser) {
            b = new FederateUser((FederateUser)a);
        } else {
            logger.error("Got user of unknown type");
            return null;
        }

        // put b in all of a's groups
        for (Group group : groups) {
            this.addUserToGroup(b, group);
        }
        
        return b;
    }

    @Override
    public NavigableSet<Group> groupVectorToGroupSet(String groupVector, int direction) {

        if (Strings.isNullOrEmpty(groupVector)) {
            throw new IllegalArgumentException("empty groupVector");
        }

        if (logger.isDebugEnabled()) {
            logger.debug("groupVectorToGroupSet: " + groupVector);
        }

        NavigableSet<Group> result = new ConcurrentSkipListSet<>();

        Map<Integer, Group> groupsByBitpos = new ConcurrentHashMap<>();

        for (Group group : getAllGroups()) {
            groupsByBitpos.put(group.getBitpos(), group);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("sorted group list: " + groupsByBitpos);
        }

        for (int i = 0; i < groupVector.length(); i++) {
            switch(groupVector.charAt(groupVector.length() - 1 - i)) {
                case '1':
                    if (!groupsByBitpos.containsKey(i)) {
                        continue;
                    }

                    if ((direction & Direction.IN.getValue()) != 0) {
                        Group incp = groupsByBitpos.get(i).getCopy();
                        incp.setDirection(Direction.IN);
                        result.add(incp);
                    }

                    if ((direction & Direction.OUT.getValue()) != 0) {
                        result.add(groupsByBitpos.get(i));
                    }

                    break;
                default:
                    // ignore values other than 1
            }
        }

        return result;
    }

    @Override
    public NavigableSet<Group> groupVectorToGroupSet(String groupVector) {
        return groupVectorToGroupSet(groupVector, Direction.IN.getValue() | Direction.OUT.getValue());
    }

    private String getOutboundGroupVector(User user) {
		Set<Group> groups = getGroups(user).stream().filter(group -> group.getDirection() == Direction.OUT).collect(Collectors.toSet());			
		return RemoteUtil.getInstance().bitVectorToString(RemoteUtil.getInstance().getBitVectorForGroups(groups));
	}
	
	private String getInboundGroupVector(User user) {
		Set<Group> groups = getGroups(user).stream().filter(group -> group.getDirection() == Direction.IN).collect(Collectors.toSet());
		return RemoteUtil.getInstance().bitVectorToString(RemoteUtil.getInstance().getBitVectorForGroups(groups));
	}
	
	private boolean shouldCacheUser(User user) {
		ConnectionType connectionType = user.getConnectionType();
		return connectionType == ConnectionType.CORE || connectionType == ConnectionType.FEDERATE;
	}
}
