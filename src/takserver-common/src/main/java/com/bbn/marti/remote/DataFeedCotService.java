package com.bbn.marti.remote;

import java.util.Collection;
import java.util.List;

import com.bbn.marti.config.DataFeed;
import com.bbn.marti.sync.model.Mission;
import com.bbn.marti.sync.model.MissionFeed;

import tak.server.cot.CotEventContainer;

public interface DataFeedCotService {
	Collection<CotEventContainer> getCachedDataFeedEvents(String dataFeedUid);
	void cacheDataFeedEvent(DataFeed dataFeed, CotEventContainer data);
	void sendLatestFeedEvents(Mission mission, MissionFeed missionFeed, List<String> clientUidList, String groupVector);
}
