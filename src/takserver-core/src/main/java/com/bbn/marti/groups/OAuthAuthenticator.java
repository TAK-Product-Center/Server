
package com.bbn.marti.groups;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;
import javax.naming.NamingException;

import com.google.common.base.Strings;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

import com.bbn.marti.config.Auth;
import com.bbn.marti.config.Oauth;
import com.bbn.marti.jwt.JwtUtils;
import com.bbn.marti.remote.config.CoreConfigFacade;
import com.bbn.marti.remote.exception.TakException;
import com.bbn.marti.remote.groups.AuthCallback;
import com.bbn.marti.remote.groups.AuthResult;
import com.bbn.marti.remote.groups.AuthStatus;
import com.bbn.marti.remote.groups.AuthenticatedUser;
import com.bbn.marti.remote.groups.ConnectionType;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.remote.groups.User;
import com.bbn.marti.remote.groups.UserClassification;
import com.bbn.marti.remote.util.SpringContextBeanForApi;
import com.bbn.marti.service.DistributedSubscriptionManager;
import com.bbn.marti.service.Resources;
import com.bbn.marti.xml.bindings.UserAuthenticationFile;
import tak.server.cache.ActiveGroupCacheHelper;


/*
 *
 * OAuth Authenticator
 *
 */
public class OAuthAuthenticator extends AbstractAuthenticator implements Serializable {

    private static final long serialVersionUID = -4317122669577006009L;

    private Logger logger = LoggerFactory.getLogger(OAuthAuthenticator.class);
    private Oauth oauthConf = CoreConfigFacade.getInstance().getRemoteConfiguration().getAuth().getOauth();
    private Auth.Ldap ldapConf = CoreConfigFacade.getInstance().getRemoteConfiguration().getAuth().getLdap();
    private OAuth2AuthorizationService oAuth2AuthorizationService;
    private ActiveGroupCacheHelper activeGroupCacheHelper;
    private static OAuthAuthenticator instance;
    private String groupPrefix;
    private String readOnlyGroup;
    private String readGroupSuffix;
    private String writeGroupSuffix;
    private String groupsClaim;
    private boolean loginWithEmail;


    public static synchronized OAuthAuthenticator getInstance(
            GroupManager groupManager, ActiveGroupCacheHelper activeGroupCacheHelper) {
        if (instance == null) {
            instance = new OAuthAuthenticator(groupManager, activeGroupCacheHelper);
        }

        return instance;
    }

    public static synchronized OAuthAuthenticator getInstance() {
        if (instance == null) {
            instance = new OAuthAuthenticator(
                    SpringContextBeanForApi.getSpringContext().getBean(GroupManager.class),
                    SpringContextBeanForApi.getSpringContext().getBean(ActiveGroupCacheHelper.class));
        }

        return instance;
    }

    public OAuthAuthenticator(GroupManager groupManager, ActiveGroupCacheHelper activeGroupCacheHelper) {

        this.groupManager = groupManager;
        this.activeGroupCacheHelper = activeGroupCacheHelper;

        groupManager.registerAuthenticator("oauth", this);

        oAuth2AuthorizationService = SpringContextBeanForApi.getSpringContext().getBean(OAuth2AuthorizationService.class);

        if (oauthConf != null) {
            groupPrefix = oauthConf.getGroupprefix().toLowerCase();
            readOnlyGroup = oauthConf.getReadOnlyGroup();
            readGroupSuffix = oauthConf.getReadGroupSuffix();
            writeGroupSuffix = oauthConf.getWriteGroupSuffix();
            groupsClaim = oauthConf.getGroupsClaim();
            loginWithEmail = oauthConf.isLoginWithEmail();
        }
    }

    @Override
    public void authenticate(@NotNull User user, @NotNull AuthCallback cb) {

        AuthResult authResult = auth(user);

        cb.authenticationReturned(user, authResult.getAuthStatus());
    }

    @Override
    public AuthResult authenticate(User user) {
        return auth(user);
    }

    private AuthResult auth(User user) {

        AuthStatus authStatus = AuthStatus.SUCCESS;
        try {

            String token = user.getName();
            Claims claims = JwtUtils.getInstance().parseClaims(token, SignatureAlgorithm.RS256);
            if (claims == null) {
                token = user.getToken();
                claims = JwtUtils.getInstance().parseClaims(token, SignatureAlgorithm.RS256);
                if (claims == null) {
                    throw new InvalidBearerTokenException("Unable to parse claims from token : " + token);
                }
            }

            String username;
            if (oauthConf != null && oauthConf.getUsernameClaim() != null
                    && claims.get(oauthConf.getUsernameClaim()) != null) {
                username = (String) claims.get(oauthConf.getUsernameClaim());
            } else if (claims.get("email") != null) {
                // For jwt's from keycloak, get the username from the email claim
                username = (String) claims.get("email");
            } else if (claims.get("sub") != null) {
                // jwt's from takserver will contain the sub claim
                username = (String) claims.get("sub");

                OAuth2Authorization authorization = oAuth2AuthorizationService.findByToken(
                        token, OAuth2TokenType.ACCESS_TOKEN);
                if (authorization == null || authorization.getAccessToken() == null ||
                        authorization.getAccessToken().isExpired()) {
                    throw new InvalidBearerTokenException("oAuth2AuthorizationService.findByToken failed!");
                }

            } else {
                // For other trusted tokens, assign a random username if we don't have an attribute mapping
                username = UUID.randomUUID().toString();
            }

            if (user instanceof AuthenticatedUser) {
                AuthenticatedUser auser = (AuthenticatedUser) user;

                if (Strings.isNullOrEmpty(username)) {
                    throw new TakException("empty username extracted from token");
                }

                logger.debug("username extracted from OAuth token", username);

                // make a new user object with the identifier from the file
                user = new AuthenticatedUser(username, auser.getConnectionId(), auser.getAddress(), auser.getCert(), username, "", "", auser.getConnectionType()); // no password or uid
                user.setToken(token);
            }

            // set user's classification if included in the token
            String country = (String) claims.get("country");
            ArrayList<String> classification = (ArrayList<String>) claims.get("classification");
            if (country != null && classification != null) {

                ArrayList<String> accms = (ArrayList<String>) claims.get("accms");
                ArrayList<String> sciControls = (ArrayList<String>) claims.get("sciControls");

                UserClassification userClassification = new UserClassification(
                        country, classification, accms, sciControls);

                groupManager.setClassificationForUser(user, userClassification);
            }

            ArrayList<String> groupNames =  (ArrayList<String>) claims.get(groupsClaim);

            boolean useGroupCache = oauthConf != null && oauthConf.isOauthUseGroupCache();

            if (groupNames != null) {

                Set<String> groupNameSet = LdapAuthenticator.applyGroupPrefixFilter(
                        new HashSet<String>(groupNames), groupPrefix);

                Set<Group> groups = new ConcurrentSkipListSet<>();
                LdapAuthenticator.groupNamesToGroups(
                        groupManager, groupNameSet, groups, readOnlyGroup, readGroupSuffix, writeGroupSuffix);

                if (useGroupCache) {

                    if (activeGroupCacheHelper.assignGroupsCheckCache(groups, user, username)) {
                        // notify the user that their cache has been updated
                        try {
                            DistributedSubscriptionManager.getInstance().sendGroupsUpdatedMessage(username, null);
                        } catch (Exception e) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("exception calling sendGroupsUpdatedMessage!", e);
                            }
                        }
                    }

                } else {
                    // do the group updates based on this set of groups
                    groupManager.updateGroups(user, groups);
                }

            } else {

                String auth = CoreConfigFacade.getInstance().getRemoteConfiguration().getAuth().getDefault();
                if (auth.compareToIgnoreCase("file") == 0) {

                    for (UserAuthenticationFile.User fileUser : FileAuthenticator.getInstance().getAllUsers()) {
                        if (fileUser.getIdentifier().compareTo(username) == 0) {

                            // if the file user has a role, set it on the user
                            if (fileUser.getRole() != null) {
                                user.getAuthorities().add(fileUser.getRole().toString());
                            }

                            if (logger.isDebugEnabled() || !user.getConnectionType().equals(ConnectionType.WEB)) {
                                logger.debug("file user oauth client_id match for user " + fileUser.getIdentifier());

                                logger.debug("groups: " + fileUser.getGroupList());
                            }

                            Set<Group> groups = FileAuthenticator.getGroups(fileUser);

                            if (useGroupCache) {
                                Set<Group> hydrated = new ConcurrentSkipListSet<>();
                                for (Group group : groups) {
                                    hydrated.add(groupManager.hydrateGroup(group));
                                }

                                if (activeGroupCacheHelper.assignGroupsCheckCache(hydrated, user, username)) {
                                    // notify the user that their cache has been updated
                                    try {
                                        DistributedSubscriptionManager.getInstance().sendGroupsUpdatedMessage(username, null);
                                    } catch (Exception e) {
                                        if (logger.isDebugEnabled()) {
                                            logger.debug("exception calling sendGroupsUpdatedMessage!", e);
                                        }
                                    }
                                }

                            } else {
                                groupManager.updateGroups(user, groups);
                            }
                        }
                    }

                } else if (auth.compareToIgnoreCase("ldap") == 0) {

                    // Assign LDAP groups for this users based on LDAP lookup by username.
                    // if the LDAP authenticator is configured, use it to assign groups for the user, using the service credentials. Can be disabled by setting the x509groups option to false.
                    try {
                        if (ldapConf != null && CoreConfigFacade.getInstance().getRemoteConfiguration().getAuth().isX509Groups() && ldapConf.isX509Groups()) {

                            try {
                                if (logger.isDebugEnabled()) {
                                    logger.debug("username: " + username);
                                }

                                Map<String, String> groupInfo = LdapAuthenticator.getInstance()
                                        .getGroupInfoBySearch(username, loginWithEmail);

                                boolean readOnly = false;
                                if (!groupInfo.isEmpty()) {
                                    if (useGroupCache) {

                                        // extract the set of group names from the ldap search results
                                        Set<String> setGroupNames = LdapAuthenticator.getInstance().
                                                getGroupNamesFromSearchResults(groupInfo);

                                        // get a set of Group objects for the current ldap results
                                        Set<Group> ldapGroups = new ConcurrentSkipListSet<>();
                                        readOnly = LdapAuthenticator.getInstance().groupNamesToGroups(
                                                setGroupNames, ldapGroups);

                                        if (activeGroupCacheHelper.assignGroupsCheckCache(ldapGroups, user, username)) {
                                            // notify the user that their cache has been updated
                                            try {
                                                DistributedSubscriptionManager.getInstance().sendGroupsUpdatedMessage(username, null);
                                            } catch (Exception e) {
                                                if (logger.isDebugEnabled()) {
                                                    logger.debug("exception calling sendGroupsUpdatedMessage!", e);
                                                }
                                            }
                                        }

                                    } else {
                                        readOnly = LdapAuthenticator.getInstance().assignGroups(groupInfo, user);
                                    }
                                }

                                if (ldapConf.isX509AddAnonymous() &&
                                        CoreConfigFacade.getInstance().getRemoteConfiguration().getAuth().isX509AddAnonymous()) {
                                    doAnonAssignment(user, readOnly);
                                }

                            } catch (NamingException e) {
                                logger.warn("exception connecting to LDAP", e);
                            }
                        }
                    } catch (Exception e) {
                        logger.debug("exception searching for ldap groups for user " + user, e);
                    }
                }
            }


            if (!useGroupCache) {
                if (oauthConf != null && oauthConf.isOauthAddAnonymous()) {
                    doAnonAssignment(user, false);
                }

                try {
                    if (groupManager.getGroups(user).isEmpty()) {
                        String msg = "no groups assigned to user " + user + " doing anonymous assignment";

                        if (logger.isDebugEnabled()) {

                            logger.debug(msg);
                        }

                        doAnonAssignment(user);
                    }
                } catch (Exception e) {
                    logger.debug("exception assigning anonymous groups for user " + user, e);
                }
            }

            AuthenticatorUtil.setUserRolesBasedOnRequestPort(user, logger);
        } catch (InvalidBearerTokenException invalidBearerTokenException) {
            logger.error("rethrowing InvalidBearerTokenException in OAuthAuthenticator!", invalidBearerTokenException);
            throw invalidBearerTokenException;
        } catch (JwtException jwtException) {
            if (logger.isDebugEnabled()) {
                logger.debug("rethrowing JwtException!", jwtException);
            }
            throw jwtException;
        } catch (Exception e) {
            logger.error("Exception in OAuthAuthenticator!", e);
            authStatus = AuthStatus.FAILURE;
        }

        return new AuthResult(authStatus, user);
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
        return "OAuthAuthenticator [groupManager=" + groupManager + "]";
    }
}
