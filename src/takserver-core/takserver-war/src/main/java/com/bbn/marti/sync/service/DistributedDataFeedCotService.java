package com.bbn.marti.sync.service;

import java.util.Collection;
import java.util.List;

import org.apache.ignite.services.Service;
import org.apache.ignite.services.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.config.DataFeed;
import com.bbn.marti.remote.DataFeedCotService;
import com.bbn.marti.sync.model.Mission;
import com.bbn.marti.sync.model.MissionFeed;
import com.bbn.marti.remote.util.SpringContextBeanForApi;

import tak.server.cache.DataFeedCotCacheHelper;
import tak.server.cot.CotEventContainer;

// this class is used as a proxy to modify the real cache, which exist in {@DataFeedCotCacheHelper}
// the reason for this is so that we can maintain a local and remote version of this class.

// the local version (messaging process) will be used for doing puts. this ensures we avoid
// making remote ignite calls when the cache is already local (messaging process) anyways

// instead, remote ignite calls will only need to take place on gets from the API process, which are far less frequent, and not on the critical NIO path
public class DistributedDataFeedCotService implements DataFeedCotService, Service {
	private static final long serialVersionUID = 1022295969715185278L;

	private static final Logger logger = LoggerFactory.getLogger(DistributedDataFeedCotService.class);

	private static DistributedDataFeedCotService instance;
	public static DistributedDataFeedCotService getInstance() {
		if (instance == null) {
			synchronized (DistributedDataFeedCotService.class) {
				if (instance == null) {
					instance = SpringContextBeanForApi.getSpringContext().getBean(DistributedDataFeedCotService.class);
				}
			}
		}
		return instance;
	}


	@Override
	public void cancel(ServiceContext ctx) {
		if (logger.isDebugEnabled()) {
			logger.debug(getClass().getSimpleName() + " service cancelled");
		}
	}

	@Override
	public void init(ServiceContext ctx) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("init method " + getClass().getSimpleName());
		}
	}

	@Override
	public void execute(ServiceContext ctx) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("execute method " + getClass().getSimpleName());
		}
	}

	private static MissionService missionService;
	public static MissionService missionService() {
		if (missionService == null) {
			synchronized (MissionService.class) {
				if (missionService == null) {
					missionService = SpringContextBeanForApi.getSpringContext().getBean(MissionService.class);
				}
			}
		}
		return missionService;
	}

	@Override
	public void sendLatestFeedEvents(Mission mission, MissionFeed missionFeed, List<String> clientUidList, String groupVector) {
		missionService().sendLatestFeedEvents(mission, missionFeed, clientUidList, groupVector);
	}

	@Override
	public void cacheDataFeedEvent(DataFeed dataFeed, CotEventContainer data) {
		DataFeedCotCacheHelper.getInstance().cacheDataFeedEvent(dataFeed, data);
	}

	@Override
	public Collection<CotEventContainer> getCachedDataFeedEvents(String dataFeedUid) {
		return DataFeedCotCacheHelper.getInstance().getCachedDataFeedEvents(dataFeedUid);
	}
}
