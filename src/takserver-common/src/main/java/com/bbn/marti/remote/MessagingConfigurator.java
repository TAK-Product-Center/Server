package com.bbn.marti.remote;

import com.bbn.marti.config.Input;
import com.bbn.marti.remote.groups.ConnectionModifyResult;
import com.bbn.marti.remote.groups.NetworkInputAddResult;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;

public interface MessagingConfigurator {

    NetworkInputAddResult addInputAndSave(Input input);

    NetworkInputAddResult addInputNoSave(Input input);

    void removeInputAndSave(String name);

    void removeInputNoSave(String name);

    Collection<InputMetric> getInputMetrics(boolean excludeDataFeeds);

    ConnectionModifyResult modifyInputAndSave(String inputName, Input input);

    ConnectionModifyResult modifyInputNoSave(String inputName, Input input);

    MessagingConfigInfo getMessagingConfig();

    void modifyMessagingConfig(MessagingConfigInfo info);

    void addMetric(Input input, InputMetric metric);

    void updateMetric(Input input);

    InputMetric getMetric(Input input);

    AuthenticationConfigInfo getAuthenticationConfig();

    void modifyAuthenticationConfig(AuthenticationConfigInfo info);

    SecurityConfigInfo getSecurityConfig();

    void modifySecurityConfig(SecurityConfigInfo info);

    Collection<Integer> getNonSecurePorts();

    HashMap<String, Boolean> verifyConfiguration();

    void addInputAndSave(Input newInput, boolean cluster) throws IOException;

    void addInputNoSave(Input newInput, boolean cluster) throws IOException;

    void removeDataFeedAndSave(String name);

    void removeDataFeedNoSave(String name);

    InputMetric getInputMetric(String name);
}
