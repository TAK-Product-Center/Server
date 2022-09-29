

package com.bbn.marti.logging;

import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.security.auth.x500.X500Principal;
import javax.servlet.http.HttpServletRequest;

import org.apache.catalina.Role;
import org.owasp.esapi.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import com.bbn.security.web.MartiValidator;
import com.bbn.security.web.MartiValidatorConstants;
import com.google.common.base.Strings;

import tak.server.Constants;

/*
 * 
 * A class which maintains a java.security.Principal per thread, and logging methods to write to the audit log.
 * 
 */
public class AuditLogUtil {
	
	public static final String AUDIT_LOG_NAME = "data-access-log";
    
    private static ThreadLocal<String> requestStringThreadLocal;
    private static ThreadLocal<String> usernameThreadLocal;
    private static ThreadLocal<String> rolesThreadLocal;
  
    private static final Logger logger = LoggerFactory.getLogger(AUDIT_LOG_NAME);
    
    private static String ANON_USERNAME = "Anonymous";

    @Autowired
    private static Validator validator;

    static {

        requestStringThreadLocal = new ThreadLocal<>();
        usernameThreadLocal = new ThreadLocal<>();
        rolesThreadLocal = new ThreadLocal<>();

        List<String> loggerNames = Collections.list(java.util.logging.LogManager.getLogManager().getLoggerNames());

        Map<String, java.util.logging.Level> logLevelMap = new HashMap<>();

        for (String loggerName : loggerNames) {
            logLevelMap.put(loggerName, java.util.logging.Logger.getLogger(loggerName).getLevel());
        }

        // load the slf4j to jul bridge, but use the log level from tomcat logging.properties
        java.util.logging.Level julLevel = java.util.logging.Logger.getLogger("global").getLevel();
        java.util.logging.LogManager.getLogManager().reset();
        SLF4JBridgeHandler.install();
        java.util.logging.Logger.getLogger("global").setLevel(julLevel);

        for (Map.Entry<String, java.util.logging.Level> entry : logLevelMap.entrySet()) {
            if (entry.getValue() != null) {
                java.util.logging.Logger.getLogger(entry.getKey()).setLevel(entry.getValue());
            }
        }
    }

    private static String validatorContext = "AUDIT_LOG";

    private static void setRoles(Principal principal) {
        rolesThreadLocal = new ThreadLocal<>();

        if (principal == null) {
            rolesThreadLocal.set("");
        }
        
        if (principal instanceof org.apache.catalina.users.AbstractUser) {

            Iterator<Role> roles = ((org.apache.catalina.users.AbstractUser) principal).getRoles();
            StringBuilder sb = new StringBuilder();

            while (roles.hasNext()) {
                Role role = roles.next();

                sb.append(", " + role.getRolename());
            }

            rolesThreadLocal.set((sb.length() > 0 ? sb.substring(2) : ""));
        } else if (principal instanceof org.springframework.security.authentication.AbstractAuthenticationToken) {
            AbstractAuthenticationToken token = (AbstractAuthenticationToken) principal;
            
            StringBuilder sb = new StringBuilder();
            
            for (GrantedAuthority authority : token.getAuthorities()) {
                sb.append(", " + authority.getAuthority());
            }
            
            rolesThreadLocal.set((sb.length() > 0 ? sb.substring(2) : ""));
        }
    }

    public static void auditLog(String sqlQuery) {

        String username = usernameThreadLocal.get();
        String roles = rolesThreadLocal.get();

        if (username == null) {
            username = "";
        }

        if (roles == null) {
            roles = "";
        }

        try {
            if (validator != null) {
                username = validator.getValidInput(validatorContext, username, MartiValidatorConstants.Regex.CertCommonName.name(), MartiValidatorConstants.DEFAULT_STRING_CHARS, true);
                logger.trace("validated username: " + username);
            } else {
                logger.trace("null validator in AuditLogUtil. Not validating username.");
            }

            if (System.getProperty("disableAuditLog") == null) {
                logger.debug(MarkerFactory.getMarker(Constants.AUDIT_LOG_MARKER), "username: [" + username + "] roles: [" + roles + "] request: [" + getRequestString() + "] database query: [" + sqlQuery + "]");
            }
        } catch (Throwable t) {
            logger.warn("exception getting user principal", t);
        }
    }

    /*
     * clear the principal and request string on a per-thread basis
     */
    public static void init(HttpServletRequest request) {
        setUsernameAndRoles(request);
        setRequestString(request);
    }
    
    /*
     * explicitly set the current username and roles for this thread
     */
    public static void init(String username, String roles, String requestPath) {
        usernameThreadLocal = new ThreadLocal<>();
        rolesThreadLocal = new ThreadLocal<>();
        requestStringThreadLocal = new ThreadLocal<>();
        
        usernameThreadLocal.set(username);
        rolesThreadLocal.set(roles);
        requestStringThreadLocal.set(requestPath);
    }

    private static void setRequestString(HttpServletRequest request) {
        requestStringThreadLocal = new ThreadLocal<>();
        
        if (request == null) {
            return;
        }
        
        requestStringThreadLocal.set(getBaseUrl(request) + getRelativeUrl(request));
    }

    public static String getRequestString() {
        if (requestStringThreadLocal == null || requestStringThreadLocal.get() == null || requestStringThreadLocal.get().isEmpty()) {
            return "";
        }

        return requestStringThreadLocal.get();
    }

    private static void setUsernameAndRoles(HttpServletRequest request) {
        usernameThreadLocal = new ThreadLocal<>();
        rolesThreadLocal = new ThreadLocal<>();
        
        if (request == null) {
            return;
        }
        
        // first, try to get the username from the Principal object 
        Principal principal = request.getUserPrincipal();

        if (principal != null) {
            usernameThreadLocal.set(principal.getName());
            setRoles(principal);
            return;
        }
        
        // if that fails, get the username from the client cert
        String username = getUsernameFromClientCert(request);
        
        if (Strings.isNullOrEmpty(username)) {
            username = ANON_USERNAME;
        }

        usernameThreadLocal.set(username);
    }

    public static String getUsername() {
        if (usernameThreadLocal == null) {
            return "";
        }

        return usernameThreadLocal.get();
    }

    public static String getRoles() {
        if (rolesThreadLocal == null) {
            return "";
        }

        return rolesThreadLocal.get();
    }

    private static String getRelativeUrl(HttpServletRequest request) {
        String baseUrl = "";

        if (request.getServerPort() == 80 || request.getServerPort() == 443) {
            baseUrl = request.getScheme() + "://" + request.getServerName() + request.getContextPath();
        }
        else {
            baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
        }

        StringBuffer buf = request.getRequestURL();

        if (request.getQueryString() != null) {
            buf.append("?");
            buf.append(request.getQueryString());
        }

        return buf.substring(baseUrl.length());
    }

    private static String getBaseUrl(HttpServletRequest request) {
        if (request.getServerPort() == 80 || request.getServerPort() == 443) {
            return request.getScheme() + "://" + request.getServerName() + request.getContextPath();
        } else {
            return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
        }
    }

    private static String getUsernameFromClientCert(HttpServletRequest request) {
        // if the principal was not present in the request (will occur for resources that do not require authentication), get the principal name directly from the SSL cert, if that is present.
        X509Certificate[] certs = (X509Certificate[])request.getAttribute("javax.servlet.request.X509Certificate");

        if(certs == null || certs.length == 0) {
            return "";
        }

        // get the first cert
        X509Certificate clientCert = certs[0];

        if (clientCert == null) {
            return "";
        }

        // Get the Subject DN's X500Principal
        X500Principal x500principal = clientCert.getSubjectX500Principal();

        if (x500principal == null) {
            return "";
        }

        String username = x500principal.getName();

        if (username == null) {
            return "";
        }

        if (logger.isDebugEnabled()) {
        	logger.debug("obtained username from client cert: " + username);
        }

        return username;
    }
}

