package com.bbn.marti.util.spring;

import java.security.Principal;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.bbn.marti.sync.model.Mission;
import com.bbn.marti.sync.model.MissionRole;

import jakarta.servlet.http.HttpServletRequest;
import tak.server.Constants;

public class RequestUtilBean {

	private static final Logger logger = LoggerFactory.getLogger(RequestUtilBean.class);

	public HttpServletRequest getRequest() {
		RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
		if (requestAttributes instanceof ServletRequestAttributes) {
			return ((ServletRequestAttributes) requestAttributes).getRequest();
		}

		return null;
	}

	public MissionRole getMissionRole() {
		HttpServletRequest request = getRequest();

		if (request == null) {
			throw new IllegalArgumentException("null request getting mission role");
		}

		return getMissionRoleFromRequest(request);
	}

	public boolean isAdmin(HttpServletRequest request) {

		// use request first - but only if it contains authorities
		Principal reqPrincipal = request.getUserPrincipal();

		if ((reqPrincipal != null) &&
				reqPrincipal instanceof Authentication &&
				((Authentication) reqPrincipal).isAuthenticated() &&
				(!((Authentication) reqPrincipal).getAuthorities().isEmpty())) {
			
			return isAuthAdmin((Authentication) reqPrincipal);

		} else {
			
			// fall back to SecurityContextHolder for cases where authorities aren't populated (probably due to filter chain ordering) - like MissionRoleAssignmentRequestHolderBean

			return isAuthAdmin(SecurityContextHolder.getContext().getAuthentication());
			
		}
	}
	
	private boolean isAuthAdmin(Authentication authentication) {
		
		Set<String> authorities = new HashSet<>();

		if (authentication != null && authentication.isAuthenticated()) {
			Collection<? extends GrantedAuthority> grantedAuthorities = authentication.getAuthorities();
			for (GrantedAuthority authority : grantedAuthorities) {
				authorities.add(authority.getAuthority());
			}
		}

		logger.debug("current roles {}", authorities);

		if (authorities.contains("ROLE_ADMIN")) {
			return true;
		}
		
		return false;
	}
	

	public boolean isFederate() {
		RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
		if (requestAttributes instanceof ServletRequestAttributes) {
			HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();

			return request.isUserInRole(Constants.FEDERATE_ROLE);
		}

		return false;
	}

	public String sessionId() {
		RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
		if (requestAttributes instanceof ServletRequestAttributes) {
			HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();

			return request.getSession().getId();
		}

		return null;
	}

	public Mission getMissionFromRequest(HttpServletRequest request) {
		Object missionObject = request.getAttribute(Mission.class.getName());

		if (missionObject == null) {
			return null;
		}

		if (missionObject instanceof Mission) {
			return (Mission) missionObject;
		}

		return null; 
	}

	public MissionRole getMissionRoleFromRequest(HttpServletRequest request) {
		Object missionRoleObject = request.getAttribute(MissionRole.class.getName());

		if (missionRoleObject == null) {
			return null;
		}

		if (missionRoleObject instanceof MissionRole) {
			return (MissionRole) missionRoleObject;
		}

		return null; 
	}

}
