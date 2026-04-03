package tak.server.federation.hub.plugins;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;

public class PluginIgniteConfig {

    private int ignitePoolSize = -1;
    private int ignitePoolSizeMultiplier = 2;
    private long metricsLogFrequency = 60000;

    private String igniteHost = "127.0.0.1";
    private Integer nonMulticastDiscoveryPort = 48500;
    private Integer nonMulticastDiscoveryPortCount = 100;

    private Integer communicationPort = 48100;
    private Integer communicationPortCount = 100;

    private final String igniteProfile;
    private final String igniteProfileKey = "fedhub-profile";
    
    private final Map<String, Object> attributes = new HashMap<>();

    public PluginIgniteConfig(@NotNull String pluginName) {
    	igniteProfile = "fedhub-plugin-" + pluginName;
	}

    public int getIgnitePoolSize() {
        return ignitePoolSize;
    }

    public void setIgnitePoolSize(int ignitePoolSize) {
        this.ignitePoolSize = ignitePoolSize;
    }

    public int getIgnitePoolSizeMultiplier() {
        return ignitePoolSizeMultiplier;
    }

    public void setIgnitePoolSizeMultiplier(int ignitePoolSizeMultiplier) {
        this.ignitePoolSizeMultiplier = ignitePoolSizeMultiplier;
    }

    public long getMetricsLogFrequency() {
        return metricsLogFrequency;
    }

    public void setMetricsLogFrequency(long metricsLogFrequency) {
        this.metricsLogFrequency = metricsLogFrequency;
    }

    public String getIgniteHost() {
        return igniteHost;
    }

    public void setIgniteHost(String igniteHost) {
        this.igniteHost = igniteHost;
    }

    public Integer getNonMulticastDiscoveryPort() {
        return nonMulticastDiscoveryPort;
    }

    public void setNonMulticastDiscoveryPort(Integer nonMulticastDiscoveryPort) {
        this.nonMulticastDiscoveryPort = nonMulticastDiscoveryPort;
    }

    public Integer getNonMulticastDiscoveryPortCount() {
        return nonMulticastDiscoveryPortCount;
    }

    public void setNonMulticastDiscoveryPortCount(Integer nonMulticastDiscoveryPortCount) {
        this.nonMulticastDiscoveryPortCount = nonMulticastDiscoveryPortCount;
    }

    public Integer getCommunicationPort() {
        return communicationPort;
    }

    public void setCommunicationPort(Integer communicationPort) {
        this.communicationPort = communicationPort;
    }

    public Integer getCommunicationPortCount() {
        return communicationPortCount;
    }

    public void setCommunicationPortCount(Integer communicationPortCount) {
        this.communicationPortCount = communicationPortCount;
    }

    public String getIgniteProfile() {
        return igniteProfile;
    }

    public String getIgniteProfileKey() {
        return igniteProfileKey;
    }

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	@Override
	public String toString() {
		return "FederationHubPluginIgniteConfig [ignitePoolSize=" + ignitePoolSize + ", ignitePoolSizeMultiplier="
				+ ignitePoolSizeMultiplier + ", metricsLogFrequency=" + metricsLogFrequency + ", igniteHost="
				+ igniteHost + ", nonMulticastDiscoveryPort=" + nonMulticastDiscoveryPort
				+ ", nonMulticastDiscoveryPortCount=" + nonMulticastDiscoveryPortCount + ", communicationPort="
				+ communicationPort + ", communicationPortCount=" + communicationPortCount + ", igniteProfile="
				+ igniteProfile + ", igniteProfileKey=" + igniteProfileKey + "]";
	}
}
