package com.bbn.marti.util.spring;

import java.util.Collection;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.bbn.marti.sync.model.MissionRole;

import tak.server.Constants;

@Scope(scopeName = "thread", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class RequestHolderBean {
	
	private HttpServletRequest localRequest;
	
	private boolean isAdmin;
	
	private boolean isFederate;
	
	private String sessionId;
	
	private MissionRole missionRole;
	
	public MissionRole getMissionRole() {
		return missionRole;
	}

	public void setMissionRole(MissionRole missionRole) {
		this.missionRole = missionRole;
	}

	private static final Logger logger = LoggerFactory.getLogger(RequestHolderBean.class);
	
	@SuppressWarnings("unchecked")	
	public void setRequest(ServletRequest request) {

		try {

			if (logger.isDebugEnabled()) {
				logger.debug("request: " + request);
			}

			if (request != null && request instanceof HttpServletRequest) {
				if (logger.isDebugEnabled()) {
					logger.debug("setting request " + request);
				}
				localRequest = (HttpServletRequest) request;

				sessionId = ((HttpServletRequest) request).getSession().getId();

				if (logger.isDebugEnabled()) {
					logger.debug("session ID: " + sessionId);
				}

			} 

			isAdmin = ((Collection<SimpleGrantedAuthority>) SecurityContextHolder.getContext().getAuthentication().getAuthorities()).contains(new SimpleGrantedAuthority("ROLE_ADMIN"));

			for (SimpleGrantedAuthority sga : (Collection<SimpleGrantedAuthority>) SecurityContextHolder.getContext().getAuthentication().getAuthorities()) {

				if (sga.getAuthority().equals(Constants.FEDERATE_ROLE)) {
					isFederate = true;
				}
			}
		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception setting request ", e);
			}
		}
	}
	
	public HttpServletRequest getRequest() {
		return localRequest;
	}
	
	public boolean isAdmin() {
		return isAdmin;
	}
	
	public boolean isFederate() {
		return isFederate;
	}
	
	public String sessionId() {
		return sessionId;
	}

}
