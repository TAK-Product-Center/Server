

package com.bbn.marti.util.spring;

import java.security.cert.X509Certificate;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.remoting.RemoteLookupFailureException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;

import com.bbn.marti.exceptions.CoreCommunicationException;
import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.remote.groups.AuthResult;
import com.bbn.marti.remote.groups.AuthStatus;
import com.bbn.marti.remote.groups.AuthenticatedUser;
import com.bbn.marti.remote.groups.ConnectionType;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.remote.groups.User;
import com.bbn.marti.remote.util.RemoteUtil;
import com.google.common.base.Strings;

import tak.server.Constants;

/*
 * Spring Security authentication provider that delegates authentication to GroupManager, and does authorization based on whatever UserService is available.
 * 
 *
 */
public class TakAuthenticationProvider extends AbstractUserDetailsAuthenticationProvider {

    @Autowired
    private GroupManager groupManager;
    
    @Autowired
    private UserDetailsService userDetailsService;
    
    @Autowired
    private RemoteUtil remoteUtil;
    
    @Autowired
    private RolePortUserServiceWrapper rpusw;
    
    @Autowired
    private CoreConfig coreConfig;
    
    private String httpsAndBasicRole;
    
    public UserDetailsService getUserDetailsService() {
        return userDetailsService;
    }

    public void setUserDetailsService(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }
    
    public String getHttpsAndBasicRole() {
        return httpsAndBasicRole;
    }

    public void setHttpsAndBasicRole(String httpsAndBasicRole) {
        this.httpsAndBasicRole = httpsAndBasicRole;
    }

    private static final Logger logger = LoggerFactory.getLogger(TakAuthenticationProvider.class);
   
    public final boolean supports(Class<?> authentication) {
        
        if (PreAuthenticatedAuthenticationToken.class.isAssignableFrom(authentication)) {
            return true;
        }
        
        return (UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication));
    }

    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        // No addition authentication checks to do
    }
    
    protected User authenticateCore(Authentication authentication, String authenticatorName) {

        if (authentication.getCredentials() == null) {
            String msg = "Authentication failed: no credentials provided";
            throw new UsernameNotFoundException(msg);
        }

        // get username and password
        String username = (authentication.getPrincipal() == null) ? "NONE_PROVIDED" : authentication.getName();
        String password = authentication.getCredentials().toString();
        
        // use Java EE session id as the connection id for marti auth
        String connectionId = "";
        
        try {
            connectionId = ThreadLocalRequestHolder.getRequest().getSession().getId();
        } catch (Exception e) {
            throw new SessionAuthenticationException("exception getting Java EE session id - unable to do Marti authentication " + e.getMessage());
        }
        
        if (logger.isDebugEnabled()) {
        	logger.debug("connectionId (Java session id): " + connectionId);
        }
        
        // also fail authentication at this layer if connectionId is empty
        if (Strings.isNullOrEmpty(connectionId)) {
            throw new SessionAuthenticationException("empty session id - unable to do TAK authentication");
        }
        
        String ip = "";
        
        HttpServletRequest request = ThreadLocalRequestHolder.getRequest();
        
        try {
            ip = request.getRemoteAddr() + ":" + request.getRemotePort();
        } catch (Exception e) {
        	if (logger.isDebugEnabled()) {
        		logger.debug("exception getting ip address from http request", e);
        	}
        }

        X509Certificate clientCert = null;
        
        if (PreAuthenticatedAuthenticationToken.class.isAssignableFrom(authentication.getClass())) {
            if (((PreAuthenticatedAuthenticationToken) authentication).getCredentials() instanceof X509Certificate) {
                clientCert = (X509Certificate) ((PreAuthenticatedAuthenticationToken) authentication).getCredentials();
                request.getSession().setAttribute(Constants.X509_CERT, clientCert);
                request.getSession().setAttribute(Constants.X509_CERT_FP, remoteUtil.getCertSHA256Fingerprint(clientCert));
            }
        }
        
        User user = new AuthenticatedUser(username, connectionId, ip, clientCert, username, password, "", ConnectionType.WEB);
        
        if (logger.isTraceEnabled()) {
        	logger.trace(user.toString());
        }

        AuthResult authResult = new AuthResult(AuthStatus.FAILURE, user);

        // authenticate using authenticator registered in group manager
        try {
            authResult = groupManager.authenticate(authenticatorName, user);

            if (logger.isDebugEnabled()) {
            	logger.debug("auth result: " + authResult);
            }
        } catch (RemoteLookupFailureException e) {
            throw new CoreCommunicationException("Unable to establish connection with TAK Server core services", e);
        } catch (OAuth2Exception e) {
            throw e;
        } catch (Exception e) {
            throw new BadCredentialsException("Exception performing TAK Server authentication", e);
        } 

        switch (authResult.getAuthStatus()) {
        case SUCCESS:
        	if (logger.isDebugEnabled()) {
        		logger.debug("auth success for authentication type " + authentication.getClass());
        	}
            
            String requestPort = new Integer(request.getLocalPort()).toString();

            if (logger.isDebugEnabled()) {
            	logger.debug("request port " + requestPort);
            }
            
            if (rpusw.getPortRoleMap().containsKey(requestPort)) { 
                
                String role = rpusw.getPortRoleMap().get(requestPort);
                
                if (logger.isDebugEnabled()) {
                	logger.debug("special port " + requestPort + " detected: adding role " + role + " to user");
                }
                
                ((AuthenticatedUser) authResult.getUser()).getAuthorities().add(role);

            } else {
            	if (logger.isDebugEnabled()) {
            		logger.debug("portRoleMap does not contain port " + requestPort);
            	}
            }
            
            
            return authResult.getUser();
        case FAILURE:
        default:
            throw new BadCredentialsException("Bad credentials for Marti RMI authentication " + user);
        }
    }
    
    protected final UserDetails retrieveUser(String username, UsernamePasswordAuthenticationToken authentication)
            throws AuthenticationException {
        UserDetails loadedUser;

        try {
            loadedUser = this.getUserDetailsService().loadUserByUsername(username);
        } catch (UsernameNotFoundException notFound) {
            throw notFound;
        } catch (Exception repositoryProblem) {
            throw new InternalAuthenticationServiceException(repositoryProblem.getMessage(), repositoryProblem);
        }

        if (loadedUser == null) {
            throw new InternalAuthenticationServiceException("UserDetailsService returned null, which is an interface contract violation");
        }
        return loadedUser;
    }
    
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    	if (logger.isDebugEnabled()) {
    		logger.debug("martiAuth authentication: " + authentication);
    	}
        
        try {
            User coreUser = null;

            // X509 and OAuth Authentication will be this type
            if (PreAuthenticatedAuthenticationToken.class.isAssignableFrom(authentication.getClass())) {

                String authenticatorName = "X509";
                if (OAuth2AuthenticationDetails.class.isAssignableFrom(authentication.getDetails().getClass())) {
                    authenticatorName = "oauth";
                }

                coreUser = authenticateCore(authentication, authenticatorName);
            }  else {
                try {
                    coreUser = authenticateCore(authentication, coreConfig.getRemoteConfiguration().getAuth().getDefault());
                } catch (RemoteLookupFailureException e) {
                    return super.authenticate(authentication);
                } catch (BadCredentialsException | UsernameNotFoundException | SessionAuthenticationException | OAuth2Exception e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            if (coreUser != null) {
            	if (logger.isDebugEnabled()) {
            		logger.debug("auth success - core user: " + coreUser);
            	}

                UserDetails userDetails = new MartiSocketUserDetailsImpl(coreUser);
                return createSuccessAuthentication(userDetails, authentication, userDetails);
            }

            if (logger.isDebugEnabled()) {
            	logger.debug("auth for user " + authentication.getPrincipal() + " unsuccessful.");
            }

        }  catch (RemoteLookupFailureException e) {
        	if (logger.isDebugEnabled()) {
        		logger.debug("Unable to connect to core services");
        	}
        }  catch (IllegalArgumentException e) {
        	if (logger.isDebugEnabled()) {
        		logger.debug(e.getMessage(), e);
        	}
        }

        
        // since user is null, we know that TAK auth was not successful. Try marti-users.xml next.
        return super.authenticate(authentication);
        
    }
}