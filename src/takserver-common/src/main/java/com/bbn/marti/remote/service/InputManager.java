package com.bbn.marti.remote.service;

import java.util.Collection;

import com.bbn.marti.config.Network.Input;
import com.bbn.marti.remote.InputMetric;
import com.bbn.marti.remote.MessagingConfigInfo;
import com.bbn.marti.remote.groups.ConnectionModifyResult;
import com.bbn.marti.remote.groups.NetworkInputAddResult;

/**
 */
public interface InputManager {
	
	NetworkInputAddResult createInput(Input input);
	
	ConnectionModifyResult modifyInput(String id, Input input);
	
	void deleteInput(String name);
	
	Collection<InputMetric> getInputMetrics();
	
	MessagingConfigInfo getConfigInfo();
	
	void modifyConfigInfo(MessagingConfigInfo messagingConfigInfo);
}
