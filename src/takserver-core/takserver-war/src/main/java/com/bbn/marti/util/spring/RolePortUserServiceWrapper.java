package com.bbn.marti.util.spring;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsByNameServiceWrapper;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.bbn.marti.config.Network.Connector;
import com.bbn.marti.remote.exception.TakException;
import com.bbn.marti.remote.config.CoreConfigFacade;
import com.beust.jcommander.internal.Lists;
import com.google.common.base.Strings;

/*
 * Assign a security role based on the port number of the incoming request.
 * 
 * 
 */
@SuppressWarnings("rawtypes")
public class RolePortUserServiceWrapper extends UserDetailsByNameServiceWrapper {

    private static final Logger logger = LoggerFactory.getLogger(RolePortUserServiceWrapper.class);

    // This is defined in security-context.xml
    @Resource(name="httpsBasicPaths")
    private List<String> httpsAndBasicPaths;

    private Map<String, String> portRoleMap = new HashMap<>();
 
    public Map<String, String> getPortRoleMap() {
        return portRoleMap;
    }

    public void setPortRoleMap(Map<String, String> portRoleMap) {
        this.portRoleMap = portRoleMap;
    }

    /**
     * Get the UserDetails object from the wrapped UserDetailsService
     * implementation
     */
    @SuppressWarnings("unchecked")
    public UserDetails loadUserDetails(Authentication authentication) throws UsernameNotFoundException, DataAccessException {
    	
    	HttpServletRequest request = ThreadLocalRequestHolder.getRequest();

        String scheme = request.getScheme();
        if (scheme != null && scheme.toLowerCase(Locale.ENGLISH).equals("https")) {
            for (String subPath : httpsAndBasicPaths) {
                if (!Strings.isNullOrEmpty(subPath) && request.getRequestURI().toString().contains(subPath)) {
                    String header = request.getHeader("Authorization");
                    if (header == null || !header.startsWith("Basic ")) {

                        // if we're processing one of the httpsAndBasicPaths, and we haven't gone through basic auth
                        // yet, bail. This will allow the BasicAuthenticationExceptionTranslationFilter to
                        // begin the basic auth process and prevent the RolePortUserServiceWrapper from assigning
                        // a role prematurely

                        throw new AuthenticationCredentialsNotFoundException(
                                "RolePortUserServiceWrapper cannot proceed without completing Basic Auth");
                    }
                }
            }
        }

        // get the port for the particular servlet container connector through which this request originated
        String requestPort = new Integer(request.getLocalPort()).toString();
        
        if (logger.isDebugEnabled()) {
        	logger.debug("port in HTTP request: " + requestPort + " portRoleMap " + portRoleMap);
        }

        if (portRoleMap.containsKey(requestPort)) {
            
            String role = portRoleMap.get(requestPort);
            
            if (logger.isDebugEnabled()) {
            	logger.debug("special port " + requestPort + " detected: assigning role " + role + " using SimpleRoleUserDetails");
            }
            
            // special case for port-based role assignment. Assign only the role specified.
            SimpleRoleUserDetails roleUserDetails = new SimpleRoleUserDetails(role);

            try {
                // Get Configuration on the requestPort
                List<Connector> connectors = CoreConfigFacade.getInstance().getRemoteConfiguration().getNetwork().getConnector();
                Connector configConnectorOnTheRequestedPort = null;
                for (Connector connector : connectors) {
                    if (requestPort.equals(String.valueOf(connector.getPort()))) {
                        configConnectorOnTheRequestedPort = connector;
                        break;
                    }
                }

                if (configConnectorOnTheRequestedPort != null &&
                        (configConnectorOnTheRequestedPort.isEnableWebtak()
                                || configConnectorOnTheRequestedPort.isEnableNonAdminUI()
                                || configConnectorOnTheRequestedPort.isEnableNonAdminUI())) {
                    roleUserDetails.getAuthorities().add(new SimpleGrantedAuthority("ROLE_ALLOW_LOGIN"));
                }
            } catch (Exception e) {
                logger.error("exception determining ROLE_ALLOW_LOGIN access", e);
            }

            return roleUserDetails;
        } else {
        	if (logger.isDebugEnabled()) {
        		logger.debug("portRoleMap does not contain port " + requestPort);
        	}
        }
        
        UserDetails ud = null;
        try { 
            ud = super.loadUserDetails(authentication);
        } catch (Exception e) {
            
            // We are not using a UserDetailsService except for spring security default, so this exception is not informative
            
            if (portRoleMap.containsKey(requestPort)) { 
                
                String role = portRoleMap.get(requestPort);
                
                if (logger.isDebugEnabled()) {
                	logger.debug("special port " + requestPort + " detected: assigning role " + role + " using SimpleRoleUserDetails");
                }
                
                // special case for port-based role assignment. Assign only the role specified.
                return new SimpleRoleUserDetails(role);
            } else {
                throw e; // if the special port is not matched, let the exception propagate so that other Spring security filters can be applied, if configured
            }
        }

        // This path path is for the situation where the special port is used, and the cert has a fingerprint in the 
        try {
            if (portRoleMap.containsKey(requestPort)) { 
                
                String role = portRoleMap.get(requestPort);

                if (logger.isDebugEnabled()) {
                	logger.debug("special port " + requestPort + " detected: assigning role " + role);
                }
                
                // special case for port-based role assignment. Assign only the role specified.
                return new UserDetailsRoleAddingDelegate(ud, role);
            }
        } catch (Exception e) {
            logger.warn("exception assigning special role based on port " + e.getMessage(), e);
            throw new TakException(e);
        }

        // normal path for all other requests
        return ud;
    }


    private class UserDetailsRoleAddingDelegate implements UserDetails {

        private static final long serialVersionUID = 1987123985L;

        private final UserDetails delegate;

        private final Collection<GrantedAuthority> authorities;

        @SuppressWarnings("unchecked")
        public UserDetailsRoleAddingDelegate(UserDetails delegate, @NotNull String role) {

            if (Strings.isNullOrEmpty(role)) {
                throw new IllegalArgumentException("empty role");
            }

            this.delegate = delegate;
            this.authorities = (Collection<GrantedAuthority>) Lists.newArrayList(Collections.EMPTY_LIST);
            this.authorities.add(new SimpleGrantedAuthority(role));
        }

        @Override
        public Collection<GrantedAuthority> getAuthorities() {
            return authorities;
        }

        @Override
        public String getPassword() {
            return delegate.getPassword();
        }

        @Override
        public String getUsername() {
            return delegate.getUsername();
        }

        @Override
        public boolean isAccountNonExpired() {
            return delegate.isAccountNonExpired();
        }

        @Override
        public boolean isAccountNonLocked() {
            return delegate.isAccountNonLocked();
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return delegate.isCredentialsNonExpired();
        }

        @Override
        public boolean isEnabled() {
            return delegate.isEnabled();
        }
    }
    
    private class SimpleRoleUserDetails implements UserDetails {

        private static final long serialVersionUID = 197987687L;
        
        private final Collection<GrantedAuthority> authorities;
        
        private String role;

        @SuppressWarnings("unchecked")
        public SimpleRoleUserDetails(@NotNull String role) {

            if (Strings.isNullOrEmpty(role)) {
                throw new IllegalArgumentException("empty role");
            }

            this.authorities = (Collection<GrantedAuthority>) Lists.newArrayList(Collections.EMPTY_LIST);
            this.authorities.add(new SimpleGrantedAuthority(role));
            this.role = role;
        }

        @Override
        public Collection<GrantedAuthority> getAuthorities() {
            return authorities;
        }

        @Override
        public String getPassword() {
            return "";
        }

        @Override
        public String getUsername() {
            return "User" + role;
        }

        @Override
        public boolean isAccountNonExpired() {
            return true;
        }

        @Override
        public boolean isAccountNonLocked() {
            return true;
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return true;
        }

        @Override
        public boolean isEnabled() {
            return true;
        }
    }
}