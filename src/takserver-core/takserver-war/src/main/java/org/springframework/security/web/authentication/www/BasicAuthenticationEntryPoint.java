
package org.springframework.security.web.authentication.www;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.util.Assert;

/**
 * Used by the <code>ExceptionTraslationFilter</code> to commence authentication via the
 * {@link BasicAuthenticationFilter}.
 * <p>
 * Once a user agent is authenticated using BASIC authentication, logout requires that the
 * browser be closed or an unauthorized (401) header be sent. The simplest way of
 * achieving the latter is to call the
 * {@link #commence(HttpServletRequest, HttpServletResponse, AuthenticationException)}
 * method below. This will indicate to the browser its credentials are no longer
 * authorized, causing it to prompt the user to login again.
 *
 * @author Ben Alex
 */
public class BasicAuthenticationEntryPoint implements AuthenticationEntryPoint,
		InitializingBean {
	
	
    
    public BasicAuthenticationEntryPoint() {
    	super();
    	logger.debug("Loading customized BasicAuthenticationEntryPoint");
	}

	private static final Logger logger = LoggerFactory.getLogger(BasicAuthenticationEntryPoint.class);
	// ~ Instance fields
	// ================================================================================================

	private String realmName;
//	
//	@Autowired
//	private HolderBean<Set<String>> httpsAndBasicPaths;
//	
//	@Autowired
//	@Qualifier("RequireBothHttpsAndBasicPathList")
//	private List<String> httpAndBasicPaths;
//	
	
	// This is defined in security-context.xml
	@Resource(name="httpsBasicPaths")
	private List<String> httpsAndBasicPaths;
	
	// ~ Methods
	// ========================================================================================================

	public void afterPropertiesSet() throws Exception {
		Assert.hasText(realmName, "realmName must be specified");
		
		logger.debug("hbp: " + httpsAndBasicPaths);
	}
	
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException, ServletException {
	    
	    // begin change
	    
	    String scheme = request.getScheme();
        logger.debug("request scheme: " + scheme);

        if (scheme != null && scheme.toLowerCase(Locale.ENGLISH).equals("https")) {

            logger.debug("request URI: " + request.getRequestURI());

            logger.debug(new Boolean(request.getRequestURI().toString().toLowerCase().contains("database")).toString());

            if (httpsAndBasicPaths.isEmpty()) {
                respondUnauthorized(response, authException);
                return;
            }

            String subPathMatch = null;

            for (String subPath : httpsAndBasicPaths) {
				if (!Strings.isNullOrEmpty(subPath) && request.getRequestURI().toString().contains(subPath)) {
					subPathMatch = subPath;
					break;
				}
			}

            if (subPathMatch == null) {
				respondUnauthorized(response, authException);
				return;
			} else {
				logger.debug(" explicity allowing URI " + request.getRequestURI() + " for X509 + BASIC auth due to path match on \"" + subPathMatch + "\"");
			}
        } else {
			logger.debug("non-https request - performing HTTP BASIC filter flow");
			response.addHeader("WWW-Authenticate", "Basic realm=\"" + realmName + "\"");
		}

	    // end change
		response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
				authException.getMessage());
	}
	
	private void respondUnauthorized(HttpServletResponse response, AuthenticationException authException) throws IOException {
        logger.debug("https request - HTTP BASIC authentication is not allowed, responding with 401 Unauthorized");
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, authException.getMessage());
    }

	public String getRealmName() {
		return realmName;
	}

	public void setRealmName(String realmName) {
		this.realmName = realmName;
	}

}
