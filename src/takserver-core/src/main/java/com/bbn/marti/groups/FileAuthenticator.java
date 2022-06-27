

package com.bbn.marti.groups;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.security.auth.x500.X500Principal;
import javax.xml.bind.JAXBException;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.bcrypt.BCrypt;

import com.bbn.cluster.ClusterGroupDefinition;
import com.bbn.marti.config.Auth;
import com.bbn.marti.groups.value.FileAuthenticatorControl;
import com.bbn.marti.remote.groups.AuthCallback;
import com.bbn.marti.remote.groups.AuthResult;
import com.bbn.marti.remote.groups.AuthStatus;
import com.bbn.marti.remote.groups.AuthenticatedUser;
import com.bbn.marti.remote.groups.Direction;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.SimpleGroupWithUsersModel;
import com.bbn.marti.remote.groups.User;
import com.bbn.marti.remote.groups.FileUserManagementInterface;
import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.marti.service.DistributedConfiguration;
import com.bbn.marti.service.LocalConfiguration;
import com.bbn.marti.util.MessageConversionUtil;
import com.bbn.marti.xml.bindings.Role;
import com.bbn.marti.xml.bindings.UserAuthenticationFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import tak.server.Constants;
import tak.server.ignite.IgniteHolder;

/**
 * Created on 9/10/15.
 */
public class FileAuthenticator extends AbstractAuthenticator implements Serializable, FileUserManagementInterface {

    private static final long serialVersionUID = -1021460221422901426L;

    private String authFileName;

    private UserAuthenticationFile authFile;

    private final HashMap<String, UserAuthenticationFile.User> userFileMap = new HashMap<>();
    
    private IgniteCache<String, UserAuthenticationFile.User> userFileCache;

    private static final Logger logger = LoggerFactory.getLogger(FileAuthenticator.class);

    private static FileAuthenticator fileAuthenticator;
    
    @Autowired
	private ObjectMapper serializer;

    private FileAuthenticator() { 
    	if (logger.isDebugEnabled()) {
    		logger.debug("FileAuthenticator construct");
    	}
    	
    	if (DistributedConfiguration.getInstance().getRemoteConfiguration().getCluster().isEnabled()) {
    		CacheConfiguration<String, UserAuthenticationFile.User> cfg = new CacheConfiguration<>();
			cfg.setName(Constants.USER_AUTH_CACHE_NAME);
			cfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
			userFileCache = IgniteHolder.getInstance().getIgnite().getOrCreateCache(cfg);
    	}

        if (LocalConfiguration.getInstance().isMessagingProfileActive()) {
            initFile();
        }
    }

    public static synchronized FileAuthenticator getInstance() {

        if (fileAuthenticator == null) {
            fileAuthenticator = new FileAuthenticator();
        }

        return fileAuthenticator;
    }
    
    public String getAuthFileName() {
    	return authFileName;
    }

    @EventListener({ContextRefreshedEvent.class})
    private void init() {
    	
    	if (logger.isDebugEnabled()) {
    		logger.debug("FileAuthenticator init");
    	}
    	
    	// try to get the auth file from the messaging
    	if (LocalConfiguration.getInstance().isApiProfileActive()) {
    		try {
    			Ignite ignite = IgniteHolder.getInstance().getIgnite();
    			UserAuthenticationFile tempAuth = ignite
    				.services(ClusterGroupDefinition.getMessagingClusterDeploymentGroup(ignite))
    				.serviceProxy(Constants.DISTRIBUTED_USER_FILE_MANAGER, FileUserManagementInterface.class, false)
    				.getUserAuthenticationFile();
    			
    			if (tempAuth != null) {
    				MessageConversionUtil.saveJAXifiedObject(DistributedConfiguration.getInstance().getAuth().getFile().getLocation(), tempAuth, true);
    			}
   			} catch (Exception e) {
				e.printStackTrace();
				logger.info("error pulling auth file from messaging node " + e);
			}

    	    initFile();
        }

        groupManager.registerAuthenticator("file", this);

        // initialize singleton instance
        fileAuthenticator = this;
        
        // pull in anything sitting on the cache     
        if (userFileCache != null) {
        	 userFileCache.forEach(entry -> {
             	try {
             		addOrUpdateUser(entry.getValue(), true, null);
             	} catch (Exception e) {
     				logger.info("Error adding user from cache");
             	}
     		});
        }
    }
    
    public synchronized void initFile() {
    	Auth.File authFileConfig = DistributedConfiguration.getInstance().getAuth().getFile();

    	if (authFileConfig == null || Strings.isNullOrEmpty(authFileConfig.getLocation())) {
    		throw new IllegalArgumentException("null or empty authentication file config or uri");
    	}

    	this.authFileName = authFileConfig.getLocation();

    	UserAuthenticationFile tmpAuth;

    	try {
    		tmpAuth = MessageConversionUtil.loadJAXifiedXML(authFileName,
    				UserAuthenticationFile.class.getPackage().getName());

    	} catch (FileNotFoundException fnfe) {
    		tmpAuth = null;
    	} catch (JAXBException e) {
    		throw new IllegalArgumentException("Invalid authentication file supplied to FileAuthenticator!");
    	}

    	if (tmpAuth == null) {
    		authFile = new UserAuthenticationFile();
    	} else {
    		authFile = tmpAuth;
    	}

    	for (UserAuthenticationFile.User user : authFile.getUser()) {
    		userFileMap.put(user.getIdentifier(), user);
    	}
    }
    
    public void addControlUser(UserAuthenticationFile.User user) {
    	try {
        	addOrUpdateUser(user, true, null);
    	} catch (Exception e) {
			logger.info("Error adding control user");
    	}
    }
    
    public void updateControlUser(UserAuthenticationFile.User user) {
    	try {
        	addOrUpdateControlUser(user);
    	} catch (Exception e) {
			logger.info("Error updating control user");
    	}
    }
    
    public void updateControlUserPasswordWithoutOldPassword(UserAuthenticationFile.User user) {
    	try {
		    addOrUpdateUser(user.getIdentifier(), user.getPassword(), user.isPasswordHashed());
    	} catch (Exception e) {
			logger.info("Error updateControlUserPasswordWithoutOldPassword");
    	}
    }
    
    public void deleteControlUser(UserAuthenticationFile.User user) {
    	try {
    		removeUser(user.getIdentifier());
    	} catch (Exception e) {
			logger.info("Error deleting control user");
    	}
    }

    @Override
    public void authenticate(@NotNull User user, @NotNull AuthCallback cb) {
        if (cb == null) {
            throw new IllegalArgumentException("Callback is null");
        }

        cb.authenticationReturned(user, authenticate(user).getAuthStatus());
    }

    @Override
    public String toString() {
        return "FileAuthenticator [authFileName=" + authFileName +
                ", groupManager=" + groupManager + "]";
    }

    @Override
    @NotNull
    public AuthResult authenticate(@NotNull User user) {

        try {
            // TODO check type before cast here
            AuthenticatedUser authenticatedUser = (AuthenticatedUser) user;
            
            if (authenticatedUser == null) {
                throw new IllegalArgumentException("User is null");
            }

            UserAuthenticationFile.User authUser = userFileMap.get(authenticatedUser.getId());

            
            if (authUser != null && !authUser.isPasswordHashed()) {
                throw new UnsupportedOperationException("Plain-text passwords not supported for file auth");
            }
           
            if (authUser == null) {
                logger.error("User Authentication via File unsuccessful. No user found.");
                return new AuthResult(AuthStatus.FAILURE, user);

            } else if (authenticatedUser.getPassword() == null || authenticatedUser.getPassword().equals("")) {
                logger.error("User Authentication via file unsuccessful. No password set.");
                return new AuthResult(AuthStatus.FAILURE, user);

            } else if (!BCrypt.checkpw(authenticatedUser.getPassword(), authUser.getPassword())) {
                logger.error("User Authentication via File unsuccessful. Invalid password.");
                return new AuthResult(AuthStatus.FAILURE, user);

            } else {
            	
                if (logger.isDebugEnabled()) {
                    logger.debug("Authenticated user with details id=" + user.getId() + ", connectionId=" + user.getConnectionId() + ", callsign=" + ((AuthenticatedUser) user).getCallsign() + ", saUid=" + ((AuthenticatedUser) user).getCotSaUid() + ", login=" + ((AuthenticatedUser) user).getLogin() + ", name=" + user.getName());
                }

                assignGroups(authUser, authenticatedUser);

                user.getAuthorities().add(authUser.getRole().toString()); // assign role

                return new AuthResult(AuthStatus.SUCCESS, user);
            }
        } catch (Exception e) {
            logger.debug("exception during file auth " + e.getMessage(), e); // make sure this gets logged
            throw e;
        }
    }

    public static Set<Group> getGroups(@NotNull UserAuthenticationFile.User authUser) {
        Set<Group> groups = new ConcurrentSkipListSet<>();

        List<String> groupList = authUser.getGroupList();
        List<String> groupListIN = authUser.getGroupListIN();
        List<String> groupListOUT = authUser.getGroupListOUT();

        if (groupList == null || groupListIN == null || groupListOUT == null) {
            throw new IllegalArgumentException("null group list");
        }

        for (String groupName : groupList) {
            groups.add(new Group(groupName, Direction.IN));
            groups.add(new Group(groupName, Direction.OUT));
        }

        for (String groupName : groupListIN) {
            groups.add(new Group(groupName, Direction.IN));
        }

        for (String groupName : groupListOUT) {
            groups.add(new Group(groupName, Direction.OUT));
        }

        return groups;
    }

    private void assignGroups(@NotNull UserAuthenticationFile.User authUser, @NotNull User user) {

        // keep track of this user even if there are no groups
        groupManager.addUser(user);

        Set<Group> groups = getGroups(authUser);

        groupManager.updateGroups(user, groups);
    }

    private static void updateUserPassword(UserAuthenticationFile.User user, String newPassword, boolean wasPasswordAlreadyHashed) {
        if (newPassword == null) {
            user.setPassword(null);
            user.setPasswordHashed(false);
            return;
        } else if (!wasPasswordAlreadyHashed){
            // TODO: Is this strong enough?
            user.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
            user.setPasswordHashed(true);
        } else {
        	user.setPassword(newPassword);
            user.setPasswordHashed(true);
        }
    }

    private void addUser(@NotNull UserAuthenticationFile.User user, @NotNull String userIdentifier, @Nullable String fingerprint, @Nullable String password, boolean wasPasswordAlreadyHashed) {
        // TODO: Deauthorize user
        user.setIdentifier(userIdentifier);
        user.setFingerprint(fingerprint);
        updateUserPassword(user, password, wasPasswordAlreadyHashed);
        userFileMap.put(userIdentifier, user);
        authFile.getUser().add(user);
    }

    @Override
    public synchronized boolean userExists(@NotNull String userIdentifier) {
        return userFileMap.get(userIdentifier) != null;
    }

    @Override
    public FileAuthenticatorControl addOrUpdateUserFromCertificate(@NotNull X509Certificate certificate) {
        String certFingerprint = RemoteUtil.getInstance().getCertSHA256Fingerprint(certificate);
        X500Principal p = certificate.getSubjectX500Principal();
        String userIdentifier = MessageConversionUtil.getCN(p.getName());

        UserAuthenticationFile.User user = userFileMap.get(userIdentifier);

        if (user == null) {
        	user = new UserAuthenticationFile.User();
            addUser(user, userIdentifier, certFingerprint, null, false);
            return FileAuthenticatorControl.add(user);
        } else {
            user.setFingerprint(certFingerprint);
            return FileAuthenticatorControl.update(user);
        }
    }

    @Override
    public synchronized FileAuthenticatorControl removeUser(@NotNull String userIdentifier) {
        // TODO: Disconnect user
        UserAuthenticationFile.User user = userFileMap.remove(userIdentifier);
        
        if (user != null) {

        	authFile.getUser().remove(user);

        	if (user.getIdentifier() != null) {
        		groupManager.getConnectedUsersById(user.getIdentifier()).forEach(remoteUser -> {
        			logger.info("Removed authenticated user with name='" + userIdentifier + "' and subscriberUser=" + remoteUser);
        			if (remoteUser != null) {
        				groupManager.removeUser(remoteUser);
        			}
        		});
        	} 
        }
        
        return FileAuthenticatorControl.delete(user);
    }

    @Override
    public synchronized FileAuthenticatorControl addUserToGroup(@NotNull String userIdentifier, @NotNull String groupName) {
        UserAuthenticationFile.User fileUser = userFileMap.get(userIdentifier);
        addUserToGroup(fileUser, groupName);
        return FileAuthenticatorControl.update(fileUser);
    }
    
    public synchronized FileAuthenticatorControl addUserToGroup(@NotNull String userIdentifier, @NotNull String groupName, @NotNull Direction direction) {
        UserAuthenticationFile.User fileUser = userFileMap.get(userIdentifier);
        addUserToGroup(fileUser, groupName, direction);
        return FileAuthenticatorControl.update(fileUser);
    }

    private void addUserToGroup(@NotNull UserAuthenticationFile.User user, @NotNull String groupName) {
        // Update File DB
        if (!user.getGroupList().contains(groupName)) {
            user.getGroupList().add(groupName);
        }
    	
    	groupManager.getConnectedUsersById(user.getIdentifier()).forEach(remoteUser -> {
    		// Null indicates the user hasn't connected yet.
            if (remoteUser != null) {
                Group inGroup = new Group(groupName, Direction.IN);
                Group outGroup = new Group(groupName, Direction.OUT);
                groupManager.addUserToGroup(remoteUser, inGroup);
                groupManager.addUserToGroup(remoteUser, outGroup);
            }
    	});
    }
    
    private void addUserToGroup(@NotNull UserAuthenticationFile.User user, @NotNull String groupName, @NotNull Direction direction) {
    	
    	if (direction.equals(Direction.IN)) {
    		if (!user.getGroupListIN().contains(groupName)) {
                user.getGroupListIN().add(groupName);
            }
    	} else {
    		if (!user.getGroupListOUT().contains(groupName)) {
                user.getGroupListOUT().add(groupName);
            }
    	}
    	
    	// update active users
    	// TODO - why doesn't this work for webtak?
    	groupManager.getConnectedUsersById(user.getIdentifier()).forEach(remoteUser -> {
    		// Null indicates the user hasn't connected yet.
            if (remoteUser != null) {
                Group group = new Group(groupName, direction);
                groupManager.addUserToGroup(remoteUser, group);
            }
    	});
    }

    @Override
    public synchronized FileAuthenticatorControl removeUserFromGroup(@NotNull String userIdentifier, @NotNull String groupName) {
        UserAuthenticationFile.User fileUser = userFileMap.get(userIdentifier);
        removeUserFromGroup(fileUser, groupName);
        return FileAuthenticatorControl.delete(fileUser);
    }
    
    public synchronized FileAuthenticatorControl removeUserFromGroup(@NotNull String userIdentifier, @NotNull String groupName, @NotNull Direction direction) {
        UserAuthenticationFile.User fileUser = userFileMap.get(userIdentifier);
        removeUserFromGroup(fileUser, groupName, direction);
        return FileAuthenticatorControl.delete(fileUser);
    }

    private synchronized void removeUserFromGroup(@NotNull UserAuthenticationFile.User user, @NotNull String groupName) {
        // Update File DB
        // Since it is a list, duplications can be present. Let's make sure the group really is removed.
        while (user.getGroupList().contains(groupName)) {
            user.getGroupList().remove(groupName);
        }
        
        groupManager.getConnectedUsersById(user.getIdentifier()).forEach(remoteUser -> {
        	 // Null indicates the user hasn't connected yet
            if (remoteUser != null) {
                Group groupIn = groupManager.getGroup(groupName, Direction.IN);
                Group groupOut = groupManager.getGroup(groupName, Direction.OUT);
                groupManager.removeUserFromGroup(remoteUser, groupIn);
                groupManager.removeUserFromGroup(remoteUser, groupOut);
            }
        });
    }
    
    private synchronized void removeUserFromGroup(@NotNull UserAuthenticationFile.User user, @NotNull String groupName, @NotNull Direction direction) {
        // Update File DB
        // Since it is a list, duplications can be present. Let's make sure the group really is removed.
    	
    	if (direction.equals(Direction.IN)) {
    		while (user.getGroupListIN().contains(groupName)) {
    			user.getGroupListIN().remove(groupName);
    		}
    	} else {
    		while (user.getGroupListOUT().contains(groupName)) {
    			user.getGroupListOUT().remove(groupName);
    		}
    	}
        
        groupManager.getConnectedUsersById(user.getIdentifier()).forEach(remoteUser -> {
        	 // Null indicates the user hasn't connected yet
            if (remoteUser != null) {
                Group group = groupManager.getGroup(groupName, direction);
                groupManager.removeUserFromGroup(remoteUser, group);
            }
        });
    }

    @Override
    public synchronized Role getUserRole(@NotNull String userIdentifier) {
        UserAuthenticationFile.User user = userFileMap.get(userIdentifier);

        if (user != null) {
            return user.getRole();
        }
        return null;
    }

    @Override
    public synchronized FileAuthenticatorControl setUserRole(@NotNull String userIdentifier, @NotNull Role role) {
        UserAuthenticationFile.User user = userFileMap.get(userIdentifier);

        if (user != null) {
            user.setRole(role);
        }
        
        return FileAuthenticatorControl.update(user);
    }

    @NotNull
    @Override
    public Map<String, Set<String>> getUsersWithGroups() {
        Map<String, Set<String>> users = new HashMap<>();

        for (UserAuthenticationFile.User user : userFileMap.values()) {
            users.put(user.getIdentifier(), Sets.newHashSet(user.getGroupList()));
        }
        return users;
    }

    @NotNull
    @Override
    public Map<String, Set<String>> getRolesWithUsers() {
        Map<String, Set<String>> roles = new HashMap<>();

        // For each user
        for (UserAuthenticationFile.User user : userFileMap.values()) {

            // Get their group list
            Role role = user.getRole();
            if (role == null) {
                role = Role.ROLE_NONEXISTENT;
            }

            Set<String> userList = roles.get(role.value());

            if (userList == null) {
                userList = new HashSet<>();
                roles.put(role.value(), userList);
            }

            userList.add(user.getIdentifier());
        }

        // Then return the group list
        return roles;
    }

    @NotNull
    @Override
    public Map<String, Set<String>> getGroupsWithUsers() {
        Map<String, Set<String>> groups = new HashMap<>();

        // For each user
        for (UserAuthenticationFile.User user : userFileMap.values()) {

            // Get their group list
            List<String> userGroupList = user.getGroupList();

            // For each group in their list
            for (String group : userGroupList) {

                // Get the userList for that group
                Set<String> userList = groups.get(group);

                // If there is no userList, create it
                if (userList == null) {
                    userList = new HashSet<>();
                    groups.put(group, userList);
                }

                // Then add the user
                userList.add(user.getIdentifier());
            }
        }

        // Then return the group list
        return groups;
    }

    // This is more for testing than anything else...
    @Override
    public boolean isUserInGroup(@NotNull String userIdentifier, @NotNull String groupName) {
        return userFileMap.get(userIdentifier).getGroupList().contains(groupName);
    }

    @Override
    public synchronized void saveChanges(FileAuthenticatorControl control) throws JAXBException, IOException {
    	if (logger.isDebugEnabled()) {
			logger.debug("saveChanges");
		}
    	    	
    	if (control != null && control.getFileUser() != null) {
        	try {
        		if (userFileCache != null) {
        			if (control.getControlType() == FileAuthenticatorControl.ControlType.USER_DELETE) {
            			userFileCache.remove(control.getFileUser().getIdentifier());
            		} else {
            			userFileCache.put(control.getFileUser().getIdentifier(), control.getFileUser());
            		}
        		}
        		IgniteHolder.getInstance().getIgnite().message().send(Constants.FILE_AUTH_TOPIC, control);
        	} catch (Exception e) {
        		if (logger.isDebugEnabled()) {
        			logger.debug("exception notifying auth file", e);
        		}
        	}
    	}
    	
    	try {
    		MessageConversionUtil.saveJAXifiedObject(authFileName, authFile, true);
		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
    			logger.debug("exception saving auth file", e);
    		}
		}
    }

    @Override
    @NotNull
    public synchronized Set<String> getUserList() {
        return userFileMap.keySet();
    }

    @Override
    @NotNull
    public Collection<UserAuthenticationFile.User> getAllUsers() {
        return userFileMap.values();
    }

    @Override
    public List<UserAuthenticationFile.User> getUsers(String... userIdentifier) {
        LinkedList<UserAuthenticationFile.User> userList = new LinkedList<>();
        for (String uid : userIdentifier) {
            if (userFileMap.containsKey(uid)) {
                userList.add(userFileMap.get(uid));
            }
        }
        return userList;
    }
    
    @Override
    public UserAuthenticationFile.User getFirstUser(String userIdentifier) {
    	
    	for (UserAuthenticationFile.User user : authFile.getUser()) {
    		if (user.getIdentifier().equals(userIdentifier)) {
    			
    			if (logger.isDebugEnabled()) {
        			logger.debug("get user " + userIdentifier + " in groups " + user.getGroupListIN());
        		}
    			
    			return user;
    		}
    		
    	}
    	return null;
    }
    
    
    @Override
    public synchronized FileAuthenticatorControl addOrUpdateUser(@NotNull String userIdentifier, @Nullable String userPassword, boolean wasPasswordAlreadyHashed) {
        UserAuthenticationFile.User user = userFileMap.get(userIdentifier);
        if (user == null) {
        	user = new UserAuthenticationFile.User();
            addUser(user, userIdentifier, null, userPassword, wasPasswordAlreadyHashed);
            return FileAuthenticatorControl.add(user);
        } else {
            updateUserPassword(user, userPassword, wasPasswordAlreadyHashed);
//            return FileAuthenticatorControl.update(user);
            return FileAuthenticatorControl.passwordChangeWithoutOldPassword(user);
        }
    }
    
    @Override
    public FileAuthenticatorControl addOrUpdateUser(UserAuthenticationFile.User newUser, boolean wasPasswordAlreadyHashed, UserAuthenticationFile.User userChanges) {
        try {
            if (newUser.getIdentifier() == null) {
                throw new RuntimeException("User must have an identifier");
            }

            String uid = newUser.getIdentifier();
            UserAuthenticationFile.User realUser;

            if (userFileMap.containsKey(newUser.getIdentifier())) {
            	
            	if (userChanges == null) {
            		userChanges = newUser;
            	}
            	
            	logger.info("updating existing file-based user " + newUser.getIdentifier());
            	
            	if (logger.isDebugEnabled()) {
            		logger.debug("changes in: " + userChanges.getGroupListIN() + " new in " + newUser.getGroupListIN());
            	}
            	
                realUser = userFileMap.get(newUser.getIdentifier());

                String np = newUser.getPassword();
                String rp = realUser.getPassword();

                if (!((np == null && rp == null) || (np != null && np.equals(rp))) && (
                        (np == null || rp == null || !BCrypt.checkpw(np, rp)))) {
                    if (newUser.isPasswordHashed()) {
                        throw new RuntimeException("Passwords do not match and the passwordHashed flag has not changed. " +
                                "Password changes must be submitted as raw passwords with the passwordHashed change matching that state.");
                    } else {
                        updateUserPassword(realUser, newUser.getPassword(), wasPasswordAlreadyHashed);
                    }
                }

                String nf = newUser.getFingerprint();
                String rf = realUser.getFingerprint();
                if (!((nf == null && rf == null) || (nf != null && nf.equals(rf)))) {
                    setUserFingerprint(uid, newUser.getFingerprint());
                }

                if (newUser.getRole() != realUser.getRole()) {
                    realUser.setRole(newUser.getRole());
                }

                // Determine which groups the user needs to be added to
                Set<String> addGroups = new HashSet<>(newUser.getGroupList());
                addGroups.removeAll(realUser.getGroupList());
                for (String group : addGroups) {
                	if (logger.isDebugEnabled()) {
                		logger.debug("adding user " + uid + " to group " + group);
                	}
                    addUserToGroup(uid, group);
                }

                // Determine which groups the user needs to be removed from
                Set<String> removeGroups = new HashSet<>(realUser.getGroupList());
                removeGroups.removeAll(newUser.getGroupList());
                
                for (String group : removeGroups) {
                    removeUserFromGroup(uid, group);
                    realUser.getGroupList().remove(group);
                }
                
                // Determine which in groups the user needs to be added to
                Set<String> addGroupsIn = new HashSet<>(newUser.getGroupListIN());
                addGroupsIn.removeAll(realUser.getGroupListIN());
                for (String group : addGroupsIn) {
                	if (logger.isDebugEnabled()) {
                		logger.debug("adding user " + uid + " to IN group " + group);
                	}
                    addUserToGroup(uid, group, Direction.IN);
                }
                
                // Determine which IN groups the user needs to be removed from
                Set<String> removeGroupsIn = new HashSet<>(realUser.getGroupListIN());
                removeGroupsIn.removeAll(newUser.getGroupListIN());
                
                if (logger.isDebugEnabled()) {
                	logger.debug("groupsIn before change: " + realUser.getGroupListIN());
                	logger.debug("removeGroupsIn: " + removeGroupsIn);
                }
                
                for (String group : removeGroupsIn) {
                    removeUserFromGroup(uid, group, Direction.IN);
                    realUser.getGroupListIN().remove(group);
                }
                
                // Determine which out groups the user needs to be added to
                Set<String> addGroupsOut = new HashSet<>(newUser.getGroupListOUT());
                addGroupsOut.removeAll(realUser.getGroupListOUT());
                for (String group : addGroupsOut) {
                	if (logger.isDebugEnabled()) {
                		logger.debug("adding user " + uid + " to OUT group " + group);
                	}
                	addUserToGroup(uid, group, Direction.OUT);
                }
                
                // Determine which OUT groups the user needs to be removed from
                Set<String> removeGroupsOut = new HashSet<>(realUser.getGroupListOUT());
                removeGroupsOut.removeAll(newUser.getGroupListOUT());
              
                for (String group : removeGroupsOut) {
                    removeUserFromGroup(uid, group, Direction.OUT);
                    realUser.getGroupListOUT().remove(group);
                }
                
                // return realUser?
                return FileAuthenticatorControl.update(realUser);
                
            } else {
            	
            	logger.info("adding new file-based user " + newUser.getIdentifier());
            	
                userFileMap.put(newUser.getIdentifier(), newUser);
                authFile.getUser().add(newUser);
                addOrUpdateUser(uid, newUser.getPassword(), wasPasswordAlreadyHashed);

                if (!newUser.getGroupList().isEmpty()) {
                    for (String group : newUser.getGroupList()) {
                        addUserToGroup(uid, group);
                    }
                } else if (!newUser.getGroupListIN().isEmpty()) {
                	for (String group : newUser.getGroupListIN()) {
                        addUserToGroup(uid, group, Direction.IN);
                    }
                } else if (!newUser.getGroupListOUT().isEmpty()) {
                	for (String group : newUser.getGroupListOUT()) {
                        addUserToGroup(uid, group, Direction.OUT);
                    }
                } else {
                    addUserToGroup(uid, "__ANON__");
                }
                
                return FileAuthenticatorControl.add(newUser);
            }
        } catch (Exception e) {
            logger.error("Exception", e);
            throw new RuntimeException(e);
        }
    }
    
    private void addOrUpdateControlUser(UserAuthenticationFile.User user) {

        try {
            if (user.getIdentifier() == null) {
                throw new RuntimeException("User must have an identifier");
            }

            String uid = user.getIdentifier();
            UserAuthenticationFile.User realUser;
            
            if (userFileMap.containsKey(user.getIdentifier())) {
            	            	
            	logger.info("updating existing file-based user " + user.getIdentifier());
            	
                realUser = userFileMap.get(user.getIdentifier());

                String np = user.getPassword();
                String rp = realUser.getPassword();

                if (!((np == null && rp == null) || (np != null && np.equals(rp))) && (
                        (np == null || rp == null || !BCrypt.checkpw(np, rp)))) {
                	                	
                    updateUserPassword(realUser, user.getPassword(), true);

                }

                String nf = user.getFingerprint();
                String rf = realUser.getFingerprint();
                if (!((nf == null && rf == null) || (nf != null && nf.equals(rf)))) {
                    setUserFingerprint(uid, user.getFingerprint());
                }

                if (user.getRole() != realUser.getRole()) {
                    realUser.setRole(user.getRole());
                }

                // Determine which groups the user needs to be added to
                Set<String> addGroups = new HashSet<>(user.getGroupList());
                addGroups.removeAll(realUser.getGroupList());
                for (String group : addGroups) {
                	if (logger.isDebugEnabled()) {
                		logger.debug("adding user " + uid + " to group " + group);
                	}
                    addUserToGroup(uid, group);
                }

                // Determine which groups the user needs to be removed from
                Set<String> removeGroups = new HashSet<>(realUser.getGroupList());
                removeGroups.removeAll(user.getGroupList());
                
                for (String group : removeGroups) {
                    removeUserFromGroup(uid, group);
                    realUser.getGroupList().remove(group);
                }
                
                // Determine which in groups the user needs to be added to
                Set<String> addGroupsIn = new HashSet<>(user.getGroupListIN());
                addGroupsIn.removeAll(realUser.getGroupListIN());
                for (String group : addGroupsIn) {
                	if (logger.isDebugEnabled()) {
                		logger.debug("adding user " + uid + " to IN group " + group);
                	}
                    addUserToGroup(uid, group, Direction.IN);
                }
                
                // Determine which IN groups the user needs to be removed from
                Set<String> removeGroupsIn = new HashSet<>(realUser.getGroupListIN());
                removeGroupsIn.removeAll(user.getGroupListIN());
                
                if (logger.isDebugEnabled()) {
                	logger.debug("groupsIn before change: " + realUser.getGroupListIN());
                	logger.debug("removeGroupsIn: " + removeGroupsIn);
                }
                
                for (String group : removeGroupsIn) {
                    removeUserFromGroup(uid, group, Direction.IN);
                    realUser.getGroupListIN().remove(group);
                }
                
                // Determine which out groups the user needs to be added to
                Set<String> addGroupsOut = new HashSet<>(user.getGroupListOUT());
                addGroupsOut.removeAll(realUser.getGroupListOUT());
                for (String group : addGroupsOut) {
                	if (logger.isDebugEnabled()) {
                		logger.debug("adding user " + uid + " to OUT group " + group);
                	}
                	addUserToGroup(uid, group, Direction.OUT);
                }
                
                // Determine which OUT groups the user needs to be removed from
                Set<String> removeGroupsOut = new HashSet<>(realUser.getGroupListOUT());
                removeGroupsOut.removeAll(user.getGroupListOUT());
              
                for (String group : removeGroupsOut) {
                    removeUserFromGroup(uid, group, Direction.OUT);
                    realUser.getGroupListOUT().remove(group);
                }
                
                return;
                
            } else {
            	
            	logger.info("adding new file-based user " + user.getIdentifier());
            	
                userFileMap.put(user.getIdentifier(), user);
                authFile.getUser().add(user);
                addOrUpdateUser(uid, user.getPassword(), true);

                if (!user.getGroupList().isEmpty()) {
                    for (String group : user.getGroupList()) {
                        addUserToGroup(uid, group);
                    }
                } else if (!user.getGroupListIN().isEmpty()) {
                	for (String group : user.getGroupListIN()) {
                        addUserToGroup(uid, group, Direction.IN);
                    }
                } else if (!user.getGroupListOUT().isEmpty()) {
                	for (String group : user.getGroupListOUT()) {
                        addUserToGroup(uid, group, Direction.OUT);
                    }
                } else {
                    addUserToGroup(uid, "__ANON__");
                }
                
                return;
            }
        } catch (Exception e) {
            logger.error("Exception", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized FileAuthenticatorControl setUserFingerprint(@NotNull String userIdentifier, @NotNull String fingerprint) {
        UserAuthenticationFile.User user = userFileMap.get(userIdentifier);

        if (user == null) {
        	user = new UserAuthenticationFile.User();
            addUser(user, userIdentifier, fingerprint, null, false);
            return FileAuthenticatorControl.add(user);
        } else {
            user.setFingerprint(fingerprint);
            return FileAuthenticatorControl.update(user);
        }
    }

    @Override
    public synchronized String getUserFingerprint(@NotNull String userIdentifier) {
        UserAuthenticationFile.User user = userFileMap.get(userIdentifier);

        if (user != null) {
            String fingerprint = user.getFingerprint();
            if (fingerprint != null && !fingerprint.equals("")) {
                return fingerprint;
            }
        }
        return "";
    }
	
	@Override
	public UserAuthenticationFile getUserAuthenticationFile() {
		return authFile;
	}
	
    @Override
    public SimpleGroupWithUsersModel getUsersInGroup(String groupName) {
    	Set<String> usersInGroup = new HashSet<String>();
    	Set<String> usersInGroupIN = new HashSet<String>();
    	Set<String> usersInGroupOUT = new HashSet<String>();

        // For each user
        for (UserAuthenticationFile.User user : userFileMap.values()) {

        	user.getGroupList().forEach(groupNameInList->{
        		if (groupNameInList.equals(groupName)){
        			usersInGroup.add(user.getIdentifier());
        		}
        	});
        	
        	user.getGroupListIN().forEach(groupNameInList->{
        		if (groupNameInList.equals(groupName)){
        			usersInGroupIN.add(user.getIdentifier());
        		}
        	});
        		
        	user.getGroupListOUT().forEach(groupNameInList->{
        		if (groupNameInList.equals(groupName)){
        			usersInGroupOUT.add(user.getIdentifier());
        		}
        	});
        	
        }
        
        SimpleGroupWithUsersModel model = new SimpleGroupWithUsersModel();
        model.setGroupname(groupName);
        model.setUsersInGroupList(usersInGroup.toArray(new String[0]));
        model.setUsersInGroupListIN(usersInGroupIN.toArray(new String[0]));
        model.setUsersInGroupListOUT(usersInGroupOUT.toArray(new String[0]));

		return model;
    }
    
    @Override
    public Set<String> getGroupNames(){
    	
    	Set<String> groupNames = new HashSet<String>();
    	
        // For each user
        for (UserAuthenticationFile.User user : userFileMap.values()) {

            user.getGroupList().forEach(groupName ->{
            	groupNames.add(groupName);
            });

            user.getGroupListIN().forEach(groupName ->{
            	groupNames.add(groupName);
            });
            
            user.getGroupListOUT().forEach(groupName ->{
            	groupNames.add(groupName);
            });   
        }
    	
    	return groupNames;
    }
}
