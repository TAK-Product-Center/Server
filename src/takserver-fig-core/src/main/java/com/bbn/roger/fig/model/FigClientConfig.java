package com.bbn.roger.fig.model;

/*
 * 
 * FIG client configuration object
 * 
 */
public class FigClientConfig {

    public FigClientConfig() { }
    
    private String serverHost;
    private Integer serverPort;
    private String certFile;
    private String keyFile;
    private String caCertFile;
    private Integer saMessageCount;
    private Integer binMessageCount;
    private Integer mpMessageCount = 0;
    private Integer mpMessageDelay = 3000;
    private Integer clientLifetimeMs;
    private boolean disconnectOnError = true;
    private String clientName; // if name is not specified, one will be auto-assigned
    private String filter = "";
    private String logFile;
    
    private int saMessageDelay = -1; // default to no message delay
    public String getServerHost() {
        return serverHost;
    }
    public void setServerHost(String serverHost) {
        this.serverHost = serverHost;
    }
    public Integer getServerPort() {
        return serverPort;
    }
    public void setServerPort(Integer serverPort) {
        this.serverPort = serverPort;
    }
    public String getCertFile() {
        return certFile;
    }
    public void setCertFile(String certFile) {
        this.certFile = certFile;
    }
    public String getKeyFile() {
        return keyFile;
    }
    public void setKeyFile(String keyFile) {
        this.keyFile = keyFile;
    }
    public String getCaCertFile() {
        return caCertFile;
    }
    public void setCaCertFile(String caCertFile) {
        this.caCertFile = caCertFile;
    }
    public Integer getBinMessageCount() {
        return binMessageCount;
    }
    public Integer getSaMessageCount() {
        return saMessageCount;
    }
    public void setSaMessageCount(Integer saMessageCount) {
        this.saMessageCount = saMessageCount;
    }
    public void setBinMessageCount(Integer binMessageCount) {
        this.binMessageCount = binMessageCount;
    }
    public Integer getClientLifetimeMs() {
        return clientLifetimeMs;
    }
    public void setClientLifetimeMs(Integer clientLifetimeMs) {
        this.clientLifetimeMs = clientLifetimeMs;
    }
    public int getSaMessageDelay() {
        return saMessageDelay;
    }
    public void setSaMessageDelay(int saMessageDelay) {
        this.saMessageDelay = saMessageDelay;
    }
    public boolean isDisconnectOnError() {
        return disconnectOnError;
    }
    public void setDisconnectOnError(boolean disconnectOnError) {
        this.disconnectOnError = disconnectOnError;
    }
    public String getClientName() {
        return clientName;
    }
    public void setClientName(String clientName) {
        this.clientName = clientName;
    }
    public String getFilter() {
        return filter;
    }
    public void setFilter(String filter) {
        this.filter = filter;
    }
    public String getLogFile() {
        return logFile;
    }
    public void setLogFile(String logFile) {
        this.logFile = logFile;
    }
    public Integer getMpMessageCount() {
        return mpMessageCount;
    }
    public void setMpMessageCount(Integer mpMessageCount) {
        this.mpMessageCount = mpMessageCount;
    }
    public Integer getMpMessageDelay() {
        return mpMessageDelay;
    }
    public void setMpMessageDelay(Integer mpMessageDelay) {
        this.mpMessageDelay = mpMessageDelay;
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((binMessageCount == null) ? 0 : binMessageCount.hashCode());
        result = prime * result
                + ((caCertFile == null) ? 0 : caCertFile.hashCode());
        result = prime * result
                + ((certFile == null) ? 0 : certFile.hashCode());
        result = prime
                * result
                + ((clientLifetimeMs == null) ? 0 : clientLifetimeMs.hashCode());
        result = prime * result
                + ((clientName == null) ? 0 : clientName.hashCode());
        result = prime * result + (disconnectOnError ? 1231 : 1237);
        result = prime * result + ((keyFile == null) ? 0 : keyFile.hashCode());
        result = prime * result
                + ((saMessageCount == null) ? 0 : saMessageCount.hashCode());
        result = prime * result + saMessageDelay;
        result = prime * result
                + ((serverHost == null) ? 0 : serverHost.hashCode());
        result = prime * result
                + ((serverPort == null) ? 0 : serverPort.hashCode());
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FigClientConfig other = (FigClientConfig) obj;
        if (binMessageCount == null) {
            if (other.binMessageCount != null)
                return false;
        } else if (!binMessageCount.equals(other.binMessageCount))
            return false;
        if (caCertFile == null) {
            if (other.caCertFile != null)
                return false;
        } else if (!caCertFile.equals(other.caCertFile))
            return false;
        if (certFile == null) {
            if (other.certFile != null)
                return false;
        } else if (!certFile.equals(other.certFile))
            return false;
        if (clientLifetimeMs == null) {
            if (other.clientLifetimeMs != null)
                return false;
        } else if (!clientLifetimeMs.equals(other.clientLifetimeMs))
            return false;
        if (clientName == null) {
            if (other.clientName != null)
                return false;
        } else if (!clientName.equals(other.clientName))
            return false;
        if (disconnectOnError != other.disconnectOnError)
            return false;
        if (keyFile == null) {
            if (other.keyFile != null)
                return false;
        } else if (!keyFile.equals(other.keyFile))
            return false;
        if (saMessageCount == null) {
            if (other.saMessageCount != null)
                return false;
        } else if (!saMessageCount.equals(other.saMessageCount))
            return false;
        if (saMessageDelay != other.saMessageDelay)
            return false;
        if (serverHost == null) {
            if (other.serverHost != null)
                return false;
        } else if (!serverHost.equals(other.serverHost))
            return false;
        if (serverPort == null) {
            if (other.serverPort != null)
                return false;
        } else if (!serverPort.equals(other.serverPort))
            return false;
        return true;
    }
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("FigClientConfig [serverHost=");
        builder.append(serverHost);
        builder.append(", serverPort=");
        builder.append(serverPort);
        builder.append(", certFile=");
        builder.append(certFile);
        builder.append(", keyFile=");
        builder.append(keyFile);
        builder.append(", caCertFile=");
        builder.append(caCertFile);
        builder.append(", saMessageCount=");
        builder.append(saMessageCount);
        builder.append(", binMessageCount=");
        builder.append(binMessageCount);
        builder.append(", clientLifetimeMs=");
        builder.append(clientLifetimeMs);
        builder.append(", disconnectOnError=");
        builder.append(disconnectOnError);
        builder.append(", clientName=");
        builder.append(clientName);
        builder.append(", saMessageDelay=");
        builder.append(saMessageDelay);
        builder.append("]");
        return builder.toString();
    }
}
