package com.bbn.roger.fig.model;

/*
 * 
 * FIG configuration object
 * 
 */
public class FigServerConfig {

    public FigServerConfig() { }
    
    private Integer port;
    private int clientTimeoutTime;
    private int clientRefreshTime;
    private Integer maxConcurrentCallsPerConnection;
    private boolean skipGateway = false;
    private int maxMessageSizeBytes = 4 * 1024;
    private int metricsLogIntervalSeconds = 5;
    private String primeClientFederateID;
    private boolean enableHealthCheck = true;
    private String keystoreType = "JKS"; // JKS is the default truststore type.
    private String keystoreFile = "";
    private String keystorePassword;
    private String keymanagerType = "SunX509"; // SunX509 is the default keymanager type
    private String truststoreType = "JKS"; // JKS is the default truststore type
    private String truststoreFile = "";
    private String truststorePassword;
    private String context;
    private String ciphers;
    private boolean useCaGroups = false;
    private String syncCachePath = ""; // location on filesystem where enterprise sync / mission package files will be cached
    private boolean nonManagedFederates = false;

    public Integer getPort() {
        return port;
    }
    public void setPort(Integer port) {
        this.port = port;
    }
    
    public Integer getMaxConcurrentCallsPerConnection() {
        return maxConcurrentCallsPerConnection;
    }
    public void setMaxConcurrentCallsPerConnection(Integer maxConcurrentCallsPerConnection) {
        this.maxConcurrentCallsPerConnection = maxConcurrentCallsPerConnection;
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
    public boolean isSkipGateway() {
        return skipGateway;
    }
    public void setSkipGateway(boolean skipGateway) {
        this.skipGateway = skipGateway;
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
    public String getPrimeClientFederateID() {
		return primeClientFederateID;
	}
	public void setPrimeClientFederateID(String primeClientFederateID) {
		this.primeClientFederateID = primeClientFederateID;
	}
	public boolean isEnableHealthCheck() {
        return enableHealthCheck;
    }
    public void setEnableHealthCheck(boolean enableHealthCheck) {
        this.enableHealthCheck = enableHealthCheck;
    }
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
    public String getKeymanagerType() {
        return keymanagerType;
    }
    public void setKeymanagerType(String keymanagerType) {
        this.keymanagerType = keymanagerType;
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
    public String getContext() {
        return context;
    }
    public void setContext(String context) {
        this.context = context;
    }
    public String getCiphers() {
        return ciphers;
    }
    public void setCiphers(String ciphers) {
        this.ciphers = ciphers;
    }
    public boolean isUseCaGroups() {
        return useCaGroups;
    }
    public void setUseCaGroups(boolean useCaGroups) {
        this.useCaGroups = useCaGroups;
    }

    public boolean isNonManagedFederates() {
        return nonManagedFederates;
    }

    public void setNonManagedFederates(boolean nonManagedFederates) {
        this.nonManagedFederates = nonManagedFederates;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("FigServerConfig [port=");
        builder.append(port);
        builder.append(", clientTimeoutTime=");
        builder.append(clientTimeoutTime);
        builder.append(", clientRefreshTime=");
        builder.append(clientRefreshTime);
        builder.append(", maxConcurrentCallsPerConnection=");
        builder.append(maxConcurrentCallsPerConnection);
        builder.append(", skipGateway=");
        builder.append(skipGateway);
        builder.append(", maxMessageSizeBytes=");
        builder.append(maxMessageSizeBytes);
        builder.append(", metricsLogIntervalSeconds=");
        builder.append(metricsLogIntervalSeconds);
        builder.append(", primeClientFederateID=");
        builder.append(primeClientFederateID);
        builder.append(", enableHealthCheck=");
        builder.append(enableHealthCheck);
        builder.append(", keystoreType=");
        builder.append(keystoreType);
        builder.append(", keystoreFile=");
        builder.append(keystoreFile);
        builder.append(", keystorePassword=");
        builder.append(keystorePassword);
        builder.append(", keymanagerType=");
        builder.append(keymanagerType);
        builder.append(", truststoreType=");
        builder.append(truststoreType);
        builder.append(", truststoreFile=");
        builder.append(truststoreFile);
        builder.append(", truststorePassword=");
        builder.append(truststorePassword);
        builder.append("]");
        return builder.toString();
    }
}
