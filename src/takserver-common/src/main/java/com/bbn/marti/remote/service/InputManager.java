package com.bbn.marti.remote.service;

import com.bbn.marti.config.DataFeed;
import com.bbn.marti.config.Input;
import com.bbn.marti.remote.InputMetric;
import com.bbn.marti.remote.MessagingConfigInfo;
import com.bbn.marti.remote.groups.ConnectionModifyResult;
import com.bbn.marti.remote.groups.NetworkInputAddResult;

import java.util.Collection;

/**
 *
 */
public interface InputManager {

    NetworkInputAddResult createInput(Input input, boolean saveConfig);

    NetworkInputAddResult createDataFeed(DataFeed dataFeed, boolean saveConfig);

    ConnectionModifyResult modifyInput(String id, Input input, boolean saveConfig);

    void updateFederationDataFeed(DataFeed dataFeed);

    void deleteInput(String name, boolean saveConfig);

    void deleteDataFeed(String name, boolean saveConfig);

    Collection<InputMetric> getInputMetrics(boolean excludeDataFeeds);

    MessagingConfigInfo getConfigInfo();

    void modifyConfigInfo(MessagingConfigInfo messagingConfigInfo);
}
