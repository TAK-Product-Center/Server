
package com.bbn.marti.sync.service;

import java.util.Date;
import java.util.List;

import jakarta.annotation.PostConstruct;

import com.bbn.marti.remote.config.CoreConfigFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.bbn.marti.sync.model.Mission;
import com.bbn.marti.util.CommonUtil;

import tak.server.cache.MissionCacheHelper;

public class MissionCacheWarmer {

	@Autowired
	private MissionService missionService;

	@Autowired
	private CommonUtil commonUtil;

	@Autowired
	private MissionCacheHelper cacheHelper;

	private final Logger logger = LoggerFactory.getLogger(MissionCacheWarmer.class);

	@PostConstruct
	private void init() {

		if (CoreConfigFacade.getInstance().getRemoteConfiguration().getBuffer().getQueue().isEnableIndividualHydratedMissionsCacheWarmer()) {
			try {
				logger.info("initializing getAllMission cache");
				Date start = new Date();
				List<Mission> missions = missionService.getAllMissions(true, true, "public", commonUtil.getAllInOutGroups());
				logger.info("getAllMission cache warmed - took " + ((new Date().getTime() - start.getTime()) / 1000) + " seconds");

				for (Mission mission : missions) {
					if (mission != null && mission.getName() != null) {
						cacheHelper.getCacheManager().getCache(mission.getName());
						logger.info("init cache for mission " + mission.getName());
					} else {
						logger.warn("null mission or mission name");
					}
				}

			} catch (Exception e) {
				logger.error("exception initializing caches", e);
			}
		}
		
		if (CoreConfigFacade.getInstance().getRemoteConfiguration().getBuffer().getQueue().isEnableGetAllMissionsCacheWarmer()) {
			try {
				logger.info("initializing getAllMission cache");
				Date start = new Date();
				List<Mission> missions = missionService.getAllMissions(true, true, "public", commonUtil.getAllInOutGroups());
				logger.info("getAllMission cache warmed - took " + ((new Date().getTime() - start.getTime()) / 1000) + " seconds");
			} catch (Exception e) {
				logger.error("exception initializing caches", e);
			}
		}
	}
}
