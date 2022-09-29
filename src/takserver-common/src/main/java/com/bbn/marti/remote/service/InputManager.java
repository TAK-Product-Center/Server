package com.bbn.marti.remote.service;

import java.util.Collection;

import com.bbn.marti.config.DataFeed;
import com.bbn.marti.config.Input;
import com.bbn.marti.remote.InputMetric;
import com.bbn.marti.remote.MessagingConfigInfo;
import com.bbn.marti.remote.groups.ConnectionModifyResult;
import com.bbn.marti.remote.groups.NetworkInputAddResult;

/**
 */
public interface InputManager {
	
	NetworkInputAddResult createInput(Input input);
	
	NetworkInputAddResult createDataFeed(DataFeed dataFeed);

	ConnectionModifyResult modifyInput(String id, Input input);
	
	void updateFederationDataFeed(DataFeed dataFeed);
	
	void deleteInput(String name);
	
	void deleteDataFeed(String name);

	Collection<InputMetric> getInputMetrics(boolean excludeDataFeeds);
	
	MessagingConfigInfo getConfigInfo();
	
	void modifyConfigInfo(MessagingConfigInfo messagingConfigInfo);
}
