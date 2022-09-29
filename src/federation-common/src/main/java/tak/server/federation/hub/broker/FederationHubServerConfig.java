package tak.server.federation.hub.broker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.RandomStringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class FederationHubServerConfig {

    public FederationHubServerConfig() {
        this.tlsVersions = new ArrayList<String>();
        this.tlsVersions.add("TLSv1.2");
        this.tlsVersions.add("TLSv1.3");
        this.nonce = RandomStringUtils.random(8, true, true);
        this.fullId = this.id + "-" + this.nonce;
    }

    /* Shared parameters. */
    private String keystoreType = "JKS";
    private String keystoreFile = "";
    private String keystorePassword;

    private String truststoreType = "JKS";
    private String truststoreFile = "";
    private String truststorePassword;

    private String keyManagerType = "SunX509";

    /* For v1 federation only. */
    private boolean v1Enabled;
    private Integer v1Port;
    private String context = "TLSv1.2";
    private boolean useEpoll = true;
    private boolean allow128cipher = true;
    private boolean allowNonSuiteB = true;
    private boolean enableOCSP = false;
    private List<String> tlsVersions;

    /* For v2 federation only. */
    private boolean v2Enabled;
    private Integer v2Port;
    private int maxMessageSizeBytes = 67108864;
    private int metricsLogIntervalSeconds = 5;
    private int clientTimeoutTime = 15;
    private int clientRefreshTime = 5;
    /* Netty default is unlimited. */
    private Integer maxConcurrentCallsPerConnection;
    private boolean enableHealthCheck = true;
    private boolean useCaGroups = true;
    
    private String serverName = "";
    private int outgoingReconnectSeconds = 5;
    private String id;
    @JsonIgnore
    private String nonce;
    @JsonIgnore
    private String fullId;
   
    
    public int getOutgoingReconnectSeconds() {
		return outgoingReconnectSeconds;
	}

	public void setOutgoingReconnectSeconds(int outgoingReconnectSeconds) {
		this.outgoingReconnectSeconds = outgoingReconnectSeconds;
	}

	public String getServerName() {
		return serverName;
	}
    
	public void setServerName(String serverName) {
		this.serverName = serverName;
	}
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
		this.fullId = this.id + "-" + this.nonce;
	}

	public String getFullId() {
		return this.fullId;
 	}
	
    /*
     * Shared parameters.
     */

    public String getKeystoreType() {
        return keystoreType;
    }
    public void setKeystoreType(String keystoreType) {
        this.keystoreType = keystoreType;
    }

    public String getKeystoreFile() {
        return keystoreFile;
    }
    public void setKeystoreFile(String keystoreFile) {
        this.keystoreFile = keystoreFile;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }
    public void setKeystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
    }

    public String getTruststoreType() {
        return truststoreType;
    }
    public void setTruststoreType(String truststoreType) {
        this.truststoreType = truststoreType;
    }
    public String getTruststoreFile() {
        return truststoreFile;
    }
    public void setTruststoreFile(String truststoreFile) {
        this.truststoreFile = truststoreFile;
    }
    public String getTruststorePassword() {
        return truststorePassword;
    }
    public void setTruststorePass(String truststorePassword) {
        this.truststorePassword = truststorePassword;
    }

    public String getKeyManagerType() {
        return keyManagerType;
    }
    public void setKeyManagerType(String keyManagerType) {
        this.keyManagerType = keyManagerType;
    }

    /*
     * v1 federation only.
     */

    public boolean isV1Enabled() {
        return v1Enabled;
    }
    public void setV1Enabled(boolean v1Enabled) {
        this.v1Enabled = v1Enabled;
    }

    public Integer getV1Port() {
        return v1Port;
    }
    public void setV1Port(Integer v1Port) {
        this.v1Port = v1Port;
    }

    public String getContext() {
        return context;
    }
    public void setContext(String context) {
        this.context = context;
    }

    public boolean isUseEpoll() {
        return useEpoll;
    }
    public void setUseEpoll(boolean useEpoll) {
        this.useEpoll = useEpoll;
    }

    public boolean isAllow128cipher() {
        return allow128cipher;
    }
    public void setAllow128cipher(boolean allow128cipher) {
        this.allow128cipher = allow128cipher;
    }

    public boolean isAllowNonSuiteB() {
        return allowNonSuiteB;
    }
    public void setAllowNonSuiteB(boolean allowNonSuiteB) {
        this.allowNonSuiteB = allowNonSuiteB;
    }

    public boolean isEnableOCSP() {
        return enableOCSP;
    }
    public void setEnableOCSP(boolean enableOCSP) {
        this.enableOCSP = enableOCSP;
    }

    public List<String> getTlsVersions() {
        return tlsVersions;
    }
    public void setTlsVersions(List<String> tlsVersions) {
        this.tlsVersions = tlsVersions;
    }

    /*
     * v2 federation only.
     */

    public boolean isV2Enabled() {
        return v2Enabled;
    }
    public void setV2Enabled(boolean v2Enabled) {
        this.v2Enabled = v2Enabled;
    }

    public Integer getV2Port() {
        return v2Port;
    }
    public void setV2Port(Integer v2Port) {
        this.v2Port = v2Port;
    }

    public int getMaxMessageSizeBytes() {
        return maxMessageSizeBytes;
    }
    public void setMaxMessageSizeBytes(int maxMessageSizeBytes) {
        this.maxMessageSizeBytes = maxMessageSizeBytes;
    }

    public int getMetricsLogIntervalSeconds() {
        return metricsLogIntervalSeconds;
    }
    public void setMetricsLogIntervalSeconds(int metricsLogIntervalSeconds) {
        this.metricsLogIntervalSeconds = metricsLogIntervalSeconds;
    }

    public int getClientTimeoutTime() {
        return clientTimeoutTime;
    }
    public void setClientTimeoutTime(int clientTimeoutTime) {
        this.clientTimeoutTime = clientTimeoutTime;
    }

    public int getClientRefreshTime() {
        return clientRefreshTime;
    }
    public void setClientRefreshTime(int clientRefreshTime) {
        this.clientRefreshTime = clientRefreshTime;
    }

    public Integer getMaxConcurrentCallsPerConnection() {
        return maxConcurrentCallsPerConnection;
    }
    public void setMaxConcurrentCallsPerConnection(Integer maxConcurrentCallsPerConnection) {
        this.maxConcurrentCallsPerConnection = maxConcurrentCallsPerConnection;
    }

    public boolean isEnableHealthCheck() {
        return enableHealthCheck;
    }
    public void setEnableHealthCheck(boolean enableHealthCheck) {
        this.enableHealthCheck = enableHealthCheck;
    }

    public boolean isUseCaGroups() {
        return useCaGroups;
    }
    public void setUseCaGroups(boolean useCaGroups) {
        this.useCaGroups = useCaGroups;
    }
	
	@Override
	public String toString() {
		return "FederationHubServerConfig [keystoreType=" + keystoreType + ", keystoreFile=" + keystoreFile
				+ ", keystorePassword=" + keystorePassword + ", truststoreType=" + truststoreType + ", truststoreFile="
				+ truststoreFile + ", truststorePassword=" + truststorePassword + ", keyManagerType=" + keyManagerType
				+ ", v1Enabled=" + v1Enabled + ", v1Port=" + v1Port + ", context=" + context + ", useEpoll=" + useEpoll
				+ ", allow128cipher=" + allow128cipher + ", allowNonSuiteB=" + allowNonSuiteB + ", enableOCSP="
				+ enableOCSP + ", tlsVersions=" + tlsVersions + ", v2Enabled=" + v2Enabled + ", v2Port=" + v2Port
				+ ", maxMessageSizeBytes=" + maxMessageSizeBytes + ", metricsLogIntervalSeconds="
				+ metricsLogIntervalSeconds + ", clientTimeoutTime=" + clientTimeoutTime + ", clientRefreshTime="
				+ clientRefreshTime + ", maxConcurrentCallsPerConnection=" + maxConcurrentCallsPerConnection
				+ ", enableHealthCheck=" + enableHealthCheck + ", useCaGroups=" + useCaGroups + ", serverName="
				+ serverName + ", outgoingReconnectSeconds=" + outgoingReconnectSeconds + ", id=" + id + ", nonce="
				+ nonce + ", fullId=" + fullId + "]";
	}
}
