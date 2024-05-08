package com.bbn.marti.takcl.connectivity.server;

import com.bbn.marti.takcl.TAKCLCore;
import com.bbn.marti.test.shared.data.servers.AbstractServerProfile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum ServerProcessDefinition {
    FederationHubBroker("federation-hub-broker", "federation-hub/federation-hub-broker.jar", "/opt/tak/federation-hub/logs/federation-hub-broker.log", Collections.unmodifiableList(Arrays.asList(
        "Started FederationHubServer"
    )), Arrays.asList("-Dlogging.config=/opt/tak/federation-hub/configs/logback-broker.xml", "-DFEDERATION_HUB_BROKER_CONFIG=federation-hub/configs/federation-hub-broker.yml")),

    FederationHubPolicy("federation-hub-policy", "federation-hub/federation-hub-policy.jar", "/opt/tak/federation-hub/logs/federation-hub-policy.log", Collections.unmodifiableList(Arrays.asList(
        "Started FederationHubPolicyManagerService"
    )), Arrays.asList("-Dlogging.config=/opt/tak/federation-hub/configs/logback-policy.xml", "-DFEDERATION_HUB_POLICY_CONFIG=federation-hub/ui_generated_policy.json")),

    PluginManager("plugins", "takserver-pm.jar", "logs/takserver-plugins.log", Collections.unmodifiableList(Arrays.asList(
        "DistributedPluginManager - execute method DistributedPluginManager",
        "PluginService - Started PluginService"
    )), null),

    RetentionService("retention", "takserver-retention.jar", "logs/takserver-retention.log", Collections.unmodifiableList(Arrays.asList(
        "DistributedRetentionPolicyConfig -  execute method DistributedRetentionPolicyConfig",
        "RetentionApplication - Started RetentionApplication"
    )), null),

    ConfigService("config", "takserver.war", "logs/takserver-config.log", Collections.unmodifiableList(Arrays.asList(
        "ConfigServiceConfiguration - Setting up local and ignite configuration",
        "ConfigServiceConfiguration - Setting up distributed configuration",
        "DistributedConfiguration - execute method DistributedConfiguration"
    )),
        Arrays.asList("-Dspring.profiles.active=config")),

    MessagingService("messaging", "takserver.war", "logs/takserver-messaging.log", Collections.unmodifiableList(Arrays.asList(
        "DistributedSubscriptionManager - DistributedSubscriptionManager execute",
        "DistributedFederationManager - execute method DistributedFederationManager",
        "DistributedPersistentGroupManager - execute method DistributedPersistentGroupManager",
        "DistributedServerInfo - execute method DistributedServerInfo",
        "DistributedContactManager - execute method DistributedContactManager",
        "DistributedRepeaterManager - execute method DistributedRepeaterManager",
        "DistributedUserManager - DistributedUserManager execute",
        "DistributedSystemInfoApi - execute method DistributedSystemInfoApi",
        "DistributedInputManager - execute method DistributedInputManager",
        "DistributedSecurityManager - execute method DistributedSecurityManager",
        "DistributedMetricsCollector - execute method DistributedMetricsCollector",
        "DistributedInjectionService - execute method DistributedInjectionService",
        "DistributedPluginDataFeedApi - execute method DistributedPluginDataFeedApi",
        "DistributedPluginApi - execute method DistributedPluginApi",
        "DistributedPluginSelfStopApi - execute method DistributedPluginSelfStopApi",
        "MessagingInitializer - takserver-core init complete"
    )), Arrays.asList("-Dspring.profiles.active=messaging")),

    ApiService("api", "takserver.war", "logs/takserver-api.log", Collections.unmodifiableList(Arrays.asList(
        "DistributedFederationHttpConnectorManager - execute method DistributedFederationHttpConnectorManager",
        "DistributedRetentionQueryManager - execute method DistributedRetentionQueryManager",
        "DistributedPluginMissionApi - execute method DistributedPluginMissionApi",
        "TomcatWebServer - Tomcat started"
    )), Arrays.asList("-Dspring.profiles.active=api"));

    public final String identifier;
    public final String jarName;
    public final String logPath;
    public final List<String> logWatchValues;
    public final List<String> jvmFlags;

    ServerProcessDefinition(@NotNull String identifier, @NotNull String jarName,
                            @NotNull String logPath, @NotNull List<String> logWatchValues, @Nullable List<String> jvmFlags) {
        this.identifier = identifier;
        this.jarName = jarName;
        this.logPath = logPath;
        this.logWatchValues = logWatchValues;
        this.jvmFlags = jvmFlags;
    }

    public final List<String> waitForMissingLogStatements(@NotNull AbstractServerProfile serverIdentifier,
                                                          @NotNull Path takserverLogsPath, int maxWaitTimeMs) {
        boolean serverReady = false;
        int duration = 0;
        List<String> remainingStatementsToSee = new ArrayList<>(this.logWatchValues);

        File logFile = takserverLogsPath.resolve(this.logPath).toFile();

        try {
            while (!serverReady && duration < maxWaitTimeMs) {
                if (!logFile.exists()) {
                    Thread.sleep(500);
                    duration += 500;
                    continue;
                }

                BufferedReader logFileReader = new BufferedReader(new FileReader(logFile));

                List<String> statementsToRemove = new ArrayList<>(remainingStatementsToSee.size());

                String logLine = logFileReader.readLine();
                while (logLine != null) {
                    for (String value : remainingStatementsToSee) {
                        if (logLine.contains(value)) {
                            statementsToRemove.add(value);
                        }
                    }
                    logLine = logFileReader.readLine();
                }

                for (String value : statementsToRemove) {
                    remainingStatementsToSee.remove(value);
                }
                statementsToRemove.clear();

                if (remainingStatementsToSee.isEmpty()) {
                    serverReady = true;
                } else {
                    Thread.sleep(500);
                    duration += 500;
                }
            }

            if (TAKCLCore.serverStartupWaitTime != null && duration < TAKCLCore.serverStartupWaitTime) {
                System.out.println("Sleeping for " + (TAKCLCore.serverStartupWaitTime - duration) + " minutes.");
                Thread.sleep(TAKCLCore.serverStartupWaitTime - duration);
            }

            if (serverReady) {
                System.out.println("Server process " + this.identifier + " appears to be ready based on log statements after " + duration + " ms");
            } else {
                System.out.println("Server process " + this.identifier + " init timeout of " + maxWaitTimeMs + " ms reached. The following log statements were not seen:\n\t" +
                    String.join("\"\n\t\"", remainingStatementsToSee) + "\n There is a good chance the tests may fail!");
            }
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
        return remainingStatementsToSee;
    }
}
