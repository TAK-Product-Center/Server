package com.bbn.marti.remote;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

public class MessagingConfigInfo implements Serializable {
    private static final long serialVersionUID = 7233298132117049859L;

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(MessagingConfigInfo.class);

    private boolean latestSA;
    private int numDbConnections;
    private int numAutoDbConnections;
    private boolean connectionPoolAutoSize;
    private boolean archive;
    private String dbUsername;
    private String dbPassword;
    private String dbUrl;
    private boolean sslEnabled;
    private String sslMode;
    private String sslCert;
    private String sslKey;
    private String sslRootCert;

    public MessagingConfigInfo(){}

    public MessagingConfigInfo(boolean latestSA, int numDbConnections, boolean connectionPoolAutoSize,
                               boolean archive, String dbUsername, String dbPassword, String dbUrl,
                               boolean sslEnabled, String sslMode, String sslCert, String sslKey, String sslRootCert) {
    	this.latestSA = latestSA;
    	this.numDbConnections = numDbConnections;
    	this.numAutoDbConnections = 200 + (int)Math.min(845, (Runtime.getRuntime().availableProcessors() - 4) * 9.2);
    	this.connectionPoolAutoSize = connectionPoolAutoSize;
    	this.archive = archive;
    	this.dbUsername = dbUsername;
    	this.dbPassword = dbPassword;
    	this.dbUrl = dbUrl;
    	this.sslEnabled = sslEnabled;
    	this.sslMode = sslMode;
    	this.sslCert = sslCert;
    	this.sslKey = sslKey;
    	this.sslRootCert = sslRootCert;
    }

    public boolean isLatestSA() {
    	return latestSA;
    }

    public void setLatestSA(boolean latestSA) {
    	this.latestSA = latestSA;
    }

    public int getNumDbConnections() {
    	return numDbConnections;
    }

    public void setNumDbConnections(int numDbConnections) {
    	this.numDbConnections = numDbConnections;
    }

    public int getNumAutoDbConnections() {
        return numAutoDbConnections;
    }

    public void setNumAutoDbConnections(int numAutoDbConnections) {
        this.numAutoDbConnections = numAutoDbConnections;
    }

    public boolean isConnectionPoolAutoSize() {
        return this.connectionPoolAutoSize;
    }

    public void setConnectionPoolAutoSize(boolean connectionPoolAutoSize) {
        this.connectionPoolAutoSize = connectionPoolAutoSize;
    }

    public boolean isArchive() {
    	return archive;
    }

    public void setArchive(boolean archive) {
    	this.archive = archive;
    }

    public String getDbUsername() {
    	return dbUsername;
    }

    public void setDbUsername(String dbUsername) {
    	this.dbUsername = dbUsername;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }

    public String getDbUrl() {
        return dbUrl;
    }

    public void setDbUrl(String dbUrl) {
        this.dbUrl = dbUrl;
    }

	public boolean isSslEnabled() {
		return sslEnabled;
	}

	public void setSslEnabled(boolean sslEnabled) {
		this.sslEnabled = sslEnabled;
	}

	public String getSslMode() {
		return sslMode;
	}

	public void setSslMode(String sslMode) {
		this.sslMode = sslMode;
	}

	public String getSslCert() {
		return sslCert;
	}

	public void setSslCert(String sslCert) {
		this.sslCert = sslCert;
	}

	public String getSslKey() {
		return sslKey;
	}

	public void setSslKey(String sslKey) {
		this.sslKey = sslKey;
	}

	public String getSslRootCert() {
		return sslRootCert;
	}

	public void setSslRootCert(String sslRootCert) {
		this.sslRootCert = sslRootCert;
	}

}
