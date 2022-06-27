package com.bbn.marti.util.spring;

import java.io.IOException;
import java.net.URLDecoder;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.GenericFilterBean;

import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.remote.exception.MissionDeletedException;
import com.bbn.marti.remote.exception.NotFoundException;
import com.bbn.marti.sync.model.Mission;
import com.bbn.marti.sync.model.MissionRole;
import com.bbn.marti.sync.service.MissionService;
import com.bbn.marti.util.CommonUtil;

@Order(0)
public class RequestHolderFilterBean extends GenericFilterBean {
	private static final Logger logger = LoggerFactory.getLogger(RequestHolderFilterBean.class);

	@Autowired
	private RequestHolderBean requestBean;

	@Autowired
	private MissionService missionService;

	@Autowired
	private CommonUtil martiUtil;

	@Autowired
	private CoreConfig config;

	private final String apiMissions = "/api/missions/";

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
		requestBean.setRequest(servletRequest);
		requestBean.setMissionRole(null);

		HttpServletRequest req = (HttpServletRequest) servletRequest;
		HttpServletResponse resp = (HttpServletResponse) servletResponse;

		if (servletRequest.getLocalPort() != 8080) {
			resp.setHeader("Strict-Transport-Security", "max-age=63072000; includeSubDomains");
		}

		if (config.getRemoteConfiguration().getNetwork().isAllowAllOrigins()) {
			resp.setHeader("Access-Control-Allow-Origin", "*");
			resp.setHeader("Access-Control-Allow-Headers", "*");
		}

		String path = req.getRequestURI();
		int missionStart = path.indexOf(apiMissions);

		if (logger.isDebugEnabled()) {
			
			// NB this can act as a request logger
			logger.debug("path: " + path);
		}

		if (missionStart != -1) {

			boolean missionCreate = false;

			int missionEnd = path.indexOf("/", missionStart + apiMissions.length());
			if (missionEnd == -1) {
				missionEnd = path.length();

				if (req.getMethod().equals("PUT")
				|| (req.getMethod().equals("OPTIONS") && config.getRemoteConfiguration().getNetwork().isAllowAllOrigins())) {
					missionCreate = true;
				}
			}

			String missionName = path.substring(missionStart + apiMissions.length(), missionEnd);
			if (missionName != null && !missionName.isEmpty()) {

				missionName = missionService.trimName(missionName);
				missionName = URLDecoder.decode(missionName, "UTF-8");

				//
				// ignore /missions endpoints that don't refer to an individual mission
				//
				if (missionName.compareTo("all") != 0
				&& 	missionName.compareTo("logs") != 0
				&& 	missionName.compareTo("invitations") != 0) {

					//
					// get the mission
					//
					Mission mission = null;

					try {
						mission = missionService.getMissionNoContent(missionName, martiUtil.getGroupVectorBitString(req.getSession().getId()));

						MissionRole role = missionService.getRoleForRequest(mission, req);

						if (logger.isDebugEnabled()) {
							logger.debug("assigned role: " + role);
						}

						requestBean.setMissionRole(role);

						req.setAttribute(MissionRole.class.getName(), role);

					} catch (NotFoundException nfe) {
						if (logger.isDebugEnabled()) {
							logger.debug("mission " + missionName + " not found - not assigning role");
						}

						if (!missionCreate) {
							((HttpServletResponse)servletResponse).setStatus(HttpServletResponse.SC_NOT_FOUND);
							return;
						}
					} catch (MissionDeletedException mde) {
						logger.warn("attempt to access a deleted mission : " + missionName);

						if (!missionCreate) {
							((HttpServletResponse)servletResponse).setStatus(HttpServletResponse.SC_GONE);
							return;
						}
					} catch (Exception e) {
						logger.warn("exception assigning mission role", e);
					}
				}
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug("RequestHolderFilterBean doFilter: " + servletRequest);
		}

		filterChain.doFilter(servletRequest, servletResponse);
	}
}