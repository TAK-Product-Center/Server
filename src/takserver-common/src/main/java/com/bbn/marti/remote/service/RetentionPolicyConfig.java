package com.bbn.marti.remote.service;

import java.util.Map;

public interface RetentionPolicyConfig {
    Map<String, Integer> updateRetentionPolicyMap(Map<String, Integer> policyMap);
    Map<String, Integer> getRetentionPolicyMap();
    String getRetentionServiceSchedule();
    String setRetentionServiceSchedule(String cronExpression);
    void setMissionExpiryTask(String taskName, Long seconds);
    void setMissionPackageExpiryTask(String taskName, Long seconds);
    void setResourceExpiryTask(String taskName, Long seconds);

}
