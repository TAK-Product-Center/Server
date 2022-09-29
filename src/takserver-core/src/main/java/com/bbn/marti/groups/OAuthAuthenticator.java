
package com.bbn.marti.groups;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import javax.naming.NamingException;

import com.bbn.marti.config.Oauth;
import com.google.common.base.Strings;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;

import com.bbn.marti.config.Auth;
import com.bbn.marti.jwt.JwtUtils;
import com.bbn.marti.remote.exception.TakException;
import com.bbn.marti.remote.groups.*;
import com.bbn.marti.service.DistributedConfiguration;
import com.bbn.marti.service.Resources;
import com.bbn.marti.util.spring.SpringContextBeanForApi;
import com.bbn.marti.xml.bindings.UserAuthenticationFile;


/*
 *
 * OAuth Authenticator
 *
 */
public class OAuthAuthenticator extends AbstractAuthenticator implements Serializable {

    private static final long serialVersionUID = -4317122669577006009L;

    private Logger logger = LoggerFactory.getLogger(OAuthAuthenticator.class);
    private Oauth oauthConf = DistributedConfiguration.getInstance().getAuth().getOauth();
    private Auth.Ldap ldapConf = DistributedConfiguration.getInstance().getAuth().getLdap();
    private DefaultTokenServices defaultTokenServices;
    private static OAuthAuthenticator instance;
    private String groupPrefix;
    private String readOnlyGroup;
    private String readGroupSuffix;
    private String writeGroupSuffix;


    public static synchronized OAuthAuthenticator getInstance(GroupManager groupManager) {
        if (instance == null) {
            instance = new OAuthAuthenticator(groupManager);
        }

        return instance;
    }

    public static synchronized OAuthAuthenticator getInstance() {
        if (instance == null) {
            instance = new OAuthAuthenticator(SpringContextBeanForApi.getSpringContext().getBean(GroupManager.class));
        }

        return instance;
    }

    public OAuthAuthenticator(GroupManager groupManager) {

        this.groupManager = groupManager;

        groupManager.registerAuthenticator("oauth", this);

        defaultTokenServices = SpringContextBeanForApi.getSpringContext().getBean(DefaultTokenServices.class);

        if (oauthConf != null) {
            groupPrefix = oauthConf.getGroupprefix().toLowerCase();
            readOnlyGroup = oauthConf.getReadOnlyGroup();
            readGroupSuffix = oauthConf.getReadGroupSuffix();
            writeGroupSuffix = oauthConf.getWriteGroupSuffix();
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
                throw new InvalidTokenException("Unable to parse claims from token : " + token);
            }

            String username = (String) claims.get("user_name");
            if (username != null) {
                // jwt's from takserver will contain the user_name claim.
                // Ensure the token hasn't been revoked by checking the token store
                OAuth2AccessToken oAuth2AccessToken = defaultTokenServices.readAccessToken(token);
                if (oAuth2AccessToken == null || oAuth2AccessToken.isExpired()) {
                    throw new OAuth2Exception("defaultTokenServices.readAccessToken failed!");
                }
            } else {
                // For jwt's from keycloak, get the username from the email claim
                username = (String) claims.get("email");
            }

            if (user instanceof AuthenticatedUser) {
                AuthenticatedUser auser = (AuthenticatedUser) user;

                if (Strings.isNullOrEmpty(username)) {
                    throw new TakException("empty username extracted from cert");
                }

                logger.debug("username extracted from OAuth token", username);

                // make a new user object with the identifier from the file
                user = new AuthenticatedUser(username, auser.getConnectionId(), auser.getAddress(), auser.getCert(), username, "", "", auser.getConnectionType()); // no password or uid
            }

            // if we have a groups claim in the token, use it to assign groups directly
            ArrayList<String> groupNames = (ArrayList<String>)claims.get("groups");
            if (groupNames != null) {

                Set<String> groupNameSet = LdapAuthenticator.applyGroupPrefixFilter(
                        new HashSet<String>(groupNames), groupPrefix);

                Set<Group> groups = new ConcurrentSkipListSet<>();
                LdapAuthenticator.groupNamesToGroups(
                        groupManager, groupNameSet, groups, readOnlyGroup, readGroupSuffix, writeGroupSuffix);

                // do the group updates based on this set of groups
                groupManager.updateGroups(user, groups);

            } else {

                String auth = DistributedConfiguration.getInstance().getRemoteConfiguration().getAuth().getDefault();
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

                            groupManager.updateGroups(user, groups);
                        }
                    }

                } else if (auth.compareToIgnoreCase("ldap") == 0) {

                    // Assign LDAP groups for this users based on LDAP lookup by username.
                    // if the LDAP authenticator is configured, use it to assign groups for the user, using the service credentials. Can be disabled by setting the x509groups option to false.
                    try {
                        if (ldapConf != null && DistributedConfiguration.getInstance().getAuth().isX509Groups() && ldapConf.isX509Groups()) {

                            try {
                                if (logger.isDebugEnabled()) {
                                    logger.debug("username: " + username);
                                }

                                Map<String, String> groupInfo = LdapAuthenticator.getInstance()
                                        .getGroupInfoBySearch(username);

                                if (logger.isDebugEnabled()) {
                                    logger.debug("group info for " + username + " : " + groupInfo);
                                }

                                boolean readOnly = false;
                                if (!groupInfo.isEmpty()) {
                                    readOnly = LdapAuthenticator.getInstance().assignGroups(groupInfo, user);
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
                        logger.debug("exception searching for ldap groups for user " + user, e);
                    }
                }
            }

            Oauth oauthConfig = DistributedConfiguration.getInstance().getAuth().getOauth();
            if (oauthConfig != null && oauthConfig.isOauthAddAnonymous()) {
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

            AuthenticatorUtil.setUserRolesBasedOnRequestPort(user, logger);
        } catch (OAuth2Exception oAuth2Exception) {
            logger.error("rethrowing OAuth2Exception in OAuthAuthenticator!", oAuth2Exception);
            throw oAuth2Exception;
        } catch (JwtException jwtException) {
            logger.error("rethrowing JwtException as InvalidTokenException!", jwtException);
            throw new InvalidTokenException(jwtException.getMessage());
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
