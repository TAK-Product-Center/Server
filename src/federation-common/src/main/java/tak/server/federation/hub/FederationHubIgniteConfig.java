package tak.server.federation.hub;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FederationHubIgniteConfig {
	
	private int ignitePoolSize = -1;
	private int ignitePoolSizeMultiplier = 2;
	private long metricsLogFrequency = 60000;
	
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
	@Override
	public String toString() {
		return "FederationHubIgniteConfig [ignitePoolSize=" + ignitePoolSize + ", ignitePoolSizeMultiplier="
				+ ignitePoolSizeMultiplier + ", metricsLogFrequency=" + metricsLogFrequency + "]";
	}
}
