package com.bbn.marti.remote;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;

import com.bbn.marti.config.Network;
import com.bbn.marti.config.Network.Input;
import com.bbn.marti.remote.groups.ConnectionModifyResult;
import com.bbn.marti.remote.groups.NetworkInputAddResult;

public interface MessagingConfigurator {

    NetworkInputAddResult addInputAndSave(Network.Input input);

    void removeInputAndSave(String name);

    Collection<InputMetric> getInputMetrics();

    ConnectionModifyResult modifyInputAndSave(String inputName, Network.Input input);

    MessagingConfigInfo getMessagingConfig();

    void modifyMessagingConfig(MessagingConfigInfo info);

    void addMetric(Network.Input input, InputMetric metric);

	InputMetric getMetric(Network.Input input);

	AuthenticationConfigInfo getAuthenticationConfig();

	void modifyAuthenticationConfig(AuthenticationConfigInfo info);

	SecurityConfigInfo getSecurityConfig();

	void modifySecurityConfig(SecurityConfigInfo info);

	Collection<Integer> getNonSecurePorts();

	HashMap<String, Boolean> verifyConfiguration();
	
	void addInput(Input newInput, boolean cluster) throws IOException;
}
