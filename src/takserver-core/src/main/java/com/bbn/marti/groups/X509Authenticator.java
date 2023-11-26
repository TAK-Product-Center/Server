

package com.bbn.marti.groups;

import java.io.Serializable;
import java.security.cert.CertificateParsingException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.naming.NamingException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import io.jsonwebtoken.JwtException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;

import com.bbn.marti.config.Auth.Ldap;
import com.bbn.marti.config.Input;
import com.bbn.marti.remote.exception.NotFoundException;
import com.bbn.marti.remote.exception.RevokedException;
import com.bbn.marti.remote.exception.TakException;
import com.bbn.marti.remote.groups.AuthCallback;
import com.bbn.marti.remote.groups.AuthResult;
import com.bbn.marti.remote.groups.AuthStatus;
import com.bbn.marti.remote.groups.AuthenticatedUser;
import com.bbn.marti.remote.groups.ConnectionType;
import com.bbn.marti.remote.groups.Direction;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.remote.groups.User;
import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.marti.remote.util.X509UsernameExtractor;
import com.bbn.marti.service.DistributedConfiguration;
import com.bbn.marti.service.DistributedSubscriptionManager;
import com.bbn.marti.service.Resources;
import com.bbn.marti.util.spring.SpringContextBeanForApi;
import com.bbn.marti.xml.bindings.UserAuthenticationFile;
import com.bbn.tak.tls.TakCert;
import com.bbn.tak.tls.repository.TakCertRepository;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import tak.server.cache.ActiveGroupCacheHelper;


/*
 * 
 * X509 Client cert authenticator
 * 
 */
public class X509Authenticator extends AbstractAuthenticator implements Serializable {
    
    private static final long serialVersionUID = -4317122669577006008L;

    Logger logger = LoggerFactory.getLogger(X509Authenticator.class);
    
    private static X509Authenticator instance;

    ActiveGroupCacheHelper activeGroupCacheHelper;

    TakCertRepository takCertRepository;

    Ldap ldapConf = DistributedConfiguration.getInstance().getAuth().getLdap();

    private final X509UsernameExtractor usernameExtractor = new X509UsernameExtractor(DistributedConfiguration.getInstance().getAuth().getDNUsernameExtractorRegex());
    

    public static synchronized X509Authenticator getInstance() {
        if (instance == null) {
            instance = new X509Authenticator(
                    SpringContextBeanForApi.getSpringContext().getBean(GroupManager.class),
                    SpringContextBeanForApi.getSpringContext().getBean(ActiveGroupCacheHelper.class),
                    SpringContextBeanForApi.getSpringContext().getBean(TakCertRepository.class));
        }
        
        return instance;
    }
    
    public X509Authenticator(GroupManager groupManager, ActiveGroupCacheHelper activeGroupCacheHelper, TakCertRepository takCertRepository) {
        
   	    this.groupManager = groupManager;
   	    this.activeGroupCacheHelper = activeGroupCacheHelper;
   	    this.takCertRepository = takCertRepository;

        groupManager.registerAuthenticator("X509", this);
    }
    
    @Override
    public void authenticate(@NotNull User user, @NotNull AuthCallback cb) {
        
        auth(user, null);
        
        // can't fail
        cb.authenticationReturned(user, AuthStatus.SUCCESS);
    }
    
    @Override
    public AuthResult authenticate(User user) {
        
        return new AuthResult(AuthStatus.SUCCESS, auth(user, null));
    }
    
    public AuthStatus authenticate(User user, Input input) {
        
        auth(user, input);
    
        return AuthStatus.SUCCESS;
    }
    
    private User auth(User user, Input input) {
	
        try {

        	if (user.getCert() == null) {
        		throw new NotFoundException("X509 Certificate not found in user object");
        	}
        	
            String certFingerprint = RemoteUtil.getInstance().getCertSHA256Fingerprint(user.getCert());

        	TakCert cert = null;
            if (DistributedConfiguration.getInstance().getAuth().isX509CheckRevocation() ||
                DistributedConfiguration.getInstance().getAuth().isX509TokenAuth()) {
                cert = takCertRepository.findOneByHash(certFingerprint);
                if (cert != null && cert.getRevocationDate() != null) {
                    throw new RevokedException("Attempt to use revoked certificate : " +
                            cert.getSubjectDn());
                }
            }

            if (DistributedConfiguration.getInstance().getAuth().isX509TokenAuth() &&
                    cert != null && cert.token != null && cert.token.length() > 0) {
                user.setName(cert.token);
                try {
                    groupManager.authenticate("oauth", user);
                } catch (OAuth2Exception | JwtException e) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("{} {} ", e.getMessage(), cert.token);
                    }
                    throw new TakException();
                }

                if (input == null) {
                    AuthenticatorUtil.setUserRolesBasedOnRequestPort(user, logger);
                }

                return user;
            }

        	if (logger.isDebugEnabled()) {
        		logger.debug("cert fingerprint: " + certFingerprint);
        	}

            if (user instanceof AuthenticatedUser) {
                AuthenticatedUser auser = (AuthenticatedUser) user;
                
                String username = usernameExtractor.extractUsername(auser.getCert());

                if (auser.getId().compareToIgnoreCase(username) != 0 ||
                        auser.getLogin().compareToIgnoreCase(username) != 0) {

                    if (Strings.isNullOrEmpty(username)) {
                        throw new TakException("empty username extracted from cert");
                    }

                	if (logger.isDebugEnabled()) {
                		logger.debug("username extracted from X509 cert", username);
                	}

                    // make a new user object with the identifier from the file
                    user = new AuthenticatedUser(username, auser.getConnectionId(), auser.getAddress(), auser.getCert(), username, "", "", auser.getConnectionType()); // no password or uid
                }
            }

            switch (DistributedConfiguration.getInstance().getRemoteConfiguration().getAuth().getDefault().toLowerCase(Locale.ENGLISH)) {
            case "file": 
            case "ldap":

                String username = user.getId();

                boolean useGroupCache = false;

                // enable group cache if enabled by the admin in CoreConfig and the client certificate
                // contains the channels ext key usage attribute (using Challenge Password OID)
                if (DistributedConfiguration.getInstance().getAuth().isX509UseGroupCache()) {
                    try {
                        useGroupCache =
                                user.getCert().getExtendedKeyUsage().contains("1.2.840.113549.1.9.7") ||
                                !DistributedConfiguration.getInstance().getAuth().isX509UseGroupCacheRequiresExtKeyUsage();
                    } catch (CertificateParsingException cpe) {
                        logger.error("exception getting cert's extendedKeyUsage", cpe);
                    }
                }

                if (logger.isDebugEnabled()) {
                    logger.error("useGroupCache : " + useGroupCache);
                }

                for (UserAuthenticationFile.User fileUser : FileAuthenticator.getInstance().getAllUsers()) {
                    if ((fileUser.getFingerprint() != null && fileUser.getFingerprint().equals(certFingerprint)) ||
                            (fileUser.getIdentifier() != null && fileUser.getIdentifier().equals(username)))
                    {
                        // if the file user has a role, set it on the user
                        if (fileUser.getRole() != null) {
                            user.getAuthorities().add(fileUser.getRole().toString());
                        }
                        
                        if (logger.isDebugEnabled() || !user.getConnectionType().equals(ConnectionType.WEB)) {
                            logger.debug("file user cert fingerprint match for user " + fileUser.getIdentifier());
                            logger.debug("groups: " + fileUser.getGroupList());
                        }

                        Set<Group> groups = FileAuthenticator.getGroups(fileUser);

                        if (useGroupCache) {
                            Set<Group> hydrated = new ConcurrentSkipListSet<>();
                            for (Group group : groups) {
                                hydrated.add(groupManager.hydrateGroup(group));
                            }
                            assignGroupsCheckCache(hydrated, user, username);
                        } else {
                            groupManager.updateGroups(user, groups);
                        }
                    }
                }

                // Assign LDAP groups for this users based on LDAP lookup by username.
                // if the LDAP authenticator is configured, use it to assign groups for the user, using the service credentials. Can be disabled by setting the x509groups option to false.
                try {
                    if (ldapConf != null && DistributedConfiguration.getInstance().getAuth().isX509Groups() && ldapConf.isX509Groups()) {

                        try {
                            String cn = usernameExtractor.extractUsername(user.getCert());

                            if (!Strings.isNullOrEmpty(cn)) {
                                username = cn;
                            }
                        } catch (Exception e) {
                            logger.debug("exception extracting CN from cert", e);
                        }
                        
                        try {
                        	if (logger.isDebugEnabled()) {                        		
                        		logger.debug("username: " + username);
                        	}

                            Map<String, String> groupInfo = LdapAuthenticator.getInstance()
                                    .getGroupInfoBySearch(username);

                            if (logger.isDebugEnabled()) {
                                logger.debug("group info for " + username + " : " + groupInfo);
                            }

                            //
                            // Multi-level filtering scheme for x509 authenticated input ports. filtergroups present
                            // on the x509 input will serve as a filter to limit groups that are returned from ldap
                            //
                            if (input != null && input.getFiltergroup() != null && input.getFiltergroup().size() > 0) {

                                // iterate over groups returned from ldap
                                Iterator<Map.Entry<String, String>> it = groupInfo.entrySet().iterator();
                                while (it.hasNext()) {
                                    String nextGroup = it.next().getValue();

                                    // check to see if the ldap group is found in the filtergroup list
                                    boolean found = false;
                                    for (String filterGroup : input.getFiltergroup()) {
                                        found = nextGroup.contains(filterGroup);
                                        if (found) {
                                            break;
                                        }
                                    }

                                    // dont assign the ldap group if not present in the filter group list
                                    if (!found) {
                                        it.remove();
                                    }
                                }
                            }

                            boolean readOnly = false;
                            if (!groupInfo.isEmpty()) {
                                if (useGroupCache) {
                                    // extract the set of group names from the ldap search results
                                    Set<String> groupNames = LdapAuthenticator.getInstance().
                                            getGroupNamesFromSearchResults(groupInfo);

                                    // get a set of Group objects for the current ldap results
                                    Set<Group> ldapGroups = new ConcurrentSkipListSet<>();
                                    readOnly = LdapAuthenticator.getInstance().groupNamesToGroups(
                                            groupNames, ldapGroups);

                                    assignGroupsCheckCache(ldapGroups, user, username);

                                } else {
                                    readOnly = LdapAuthenticator.getInstance().assignGroups(
                                            groupInfo, user);
                                }
                            }

                            if (logger.isDebugEnabled()) {
                                logger.debug("X509 / LDAP group assignment complete for " + user.getId());
                            }

                            if (ldapConf.isX509AddAnonymous() &&
                                    DistributedConfiguration.getInstance().getAuth().isX509AddAnonymous()) {
                            	doAnonAssignment(user, readOnly);
                            }

                        } catch (NamingException e) {
                            logger.warn("exception connecting to LDAP", e);
                        }
                    }
                } catch (Exception e) {
                    logger.error("exception searching for ldap groups for user " + user, e);
                }

                if (!useGroupCache) {
                    try {
                        if (groupManager.getGroups(user).isEmpty()) {

                            if (DistributedConfiguration.getInstance().getAuth().isX509GroupsDefaultRDN()) {
                                doRDNAssignment(user);
                            }

                            if (groupManager.getGroups(user).isEmpty()) {
                                String msg = "no groups assigned to user " + user + " doing anonymous assignment";

                                if (logger.isDebugEnabled()) {

                                    logger.debug(msg);
                                }

                                doAnonAssignment(user);
                            }
                        }
                    } catch (Exception e) {
                        logger.error("exception assigning anonymous groups for user " + user, e);
                    }
                }
            	
                if (input == null) {
                	AuthenticatorUtil.setUserRolesBasedOnRequestPort(user, logger);
                }

                break;
            default:
                throw new UnsupportedOperationException("default auth method " + DistributedConfiguration.getInstance().getRemoteConfiguration().getAuth().getDefault() + " not supported");
            }
        } catch (IllegalStateException e) {

            logger.error("IllegalStateException in x509Authenticator.auth", e);

            if (input != null && input.isAuthRequired()) {
            	
            	if (logger.isDebugEnabled()) {
            		logger.debug("Authentication is required for input " + input.getName() + " Not adding user to anon group.");
            	}
            	
            } else {
                
                String msg = "FileAuthenticator not configured - unable to assign groups based on cert fingerprint. Defaulting to anon group membership only, and ROLE_ANONYMOUS authority."; 
            	
                if (logger.isDebugEnabled()) {
            		logger.debug(msg);
                }
                
                doAnonAssignment(user);
                
                throw new TakException(msg, e); // propagate this to the web level so that fallback to marti-users.xml can occur
            }
        }
        
        return user;
    }

    private void doRDNAssignment(User user) {
        try {
            StringBuilder rdnBuilder = new StringBuilder();
            LdapName ldapName = new LdapName(user.getCert().getSubjectX500Principal().getName());
            for (Rdn rdn : ldapName.getRdns()) {
                if (rdn.getType().compareToIgnoreCase("CN") != 0) {
                    rdnBuilder.append(rdn.getValue());
                    rdnBuilder.append("-");
                }
            }

            if (rdnBuilder.length() > 0) {
                String rdnGroup = rdnBuilder.toString();
                groupManager.addUserToGroup(user, new Group(rdnGroup, Direction.IN));
                groupManager.addUserToGroup(user, new Group(rdnGroup, Direction.OUT));
            } else {
                logger.error("failed to extract RDNs from cert!");
            }

        } catch (Exception e) {
            logger.error("exception in doRDNAssignment", e);
        }
    }

    public void assignGroupsCheckCache(Set<Group> groups, User user, String username) {

        // check to see if we have any cache entries for the current username
        List<Group> activeGroups = activeGroupCacheHelper.getActiveGroupsForUser(username);
        if (activeGroups == null) {
            activeGroups = new LinkedList<>();
        }

        // recreate as a full LinkedList to support delete (cant delete on list coming from cache)
        activeGroups = new LinkedList<>(activeGroups);

        // keep track of this user even if there are no groups
        groupManager.addUser(user);

        // find groups that need to get removed from the cache
        Set<Group> cacheGroups = new ConcurrentSkipListSet<>(activeGroups);
        Set<Group> removals = Sets.difference(cacheGroups, groups);
        activeGroups.removeAll(removals);

        // find groups that need to get added to the cache
        Set<Group> adds = Sets.difference(groups, cacheGroups);
        activeGroups.addAll(adds);

        for (Group group : adds) {
            group.setActive(false);
        }

        // if we only have one group, make sure its active
        if (activeGroups.size() == 1) {
            activeGroups.get(0).setActive(true);
            // need to check case when size=2 for IN/OUT groups with same name
        } else if (activeGroups.size() == 2 && activeGroups.get(0).getName().equals(activeGroups.get(1).getName())) {
            activeGroups.get(0).setActive(true);
            activeGroups.get(1).setActive(true);
        }

        // update the cache if required
        if (adds.size() > 0 || removals.size() > 0) {
            activeGroupCacheHelper.setActiveGroupsForUser(username, activeGroups);

            // notify the user that their cache has been updated
            try {
                DistributedSubscriptionManager.getInstance().sendGroupsUpdatedMessage(username, null);
            } catch (Exception e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("exception calling sendGroupsUpdatedMessage!", e);
                }
            }
        }

        // remove any inactive cache entries prior to push the groups to the user
        Iterator<Group> activeGroupIter = activeGroups.iterator();
        while (activeGroupIter.hasNext()) {
            Group activeGroup = activeGroupIter.next();
            if (!activeGroup.getActive()) {
                activeGroupIter.remove();
            }
        }

        // do the group updates based on this set of groups
        groupManager.updateGroups(user, new ConcurrentSkipListSet<>(activeGroups));
    }

    // Using the superclass only for its thread pool
    @Override
    public void authenticateAsync(@NotNull final User user, @NotNull final AuthCallback cb) {

        // execute auth callback just once for the dummy case - no periodic updates
        Resources.authThreadPool.execute(new Runnable() {
            public void run() {
                try {
                    authenticate(user, cb);
                } catch (Exception e) {
                    logger.error("authenticateAsync failed.", e);
                }
            }
        });        
    }

    @Override
    public String toString() {
        return "X509Authenticator [groupManager=" + groupManager + "]";
    }
}
