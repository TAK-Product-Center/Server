package com.bbn.marti.remote;

import java.io.Serializable;

public class AuthenticationConfigInfo implements Serializable {
    private String url;
    private String userString;
    private int updateInterval;
    private String groupPrefix;
    private String serviceAccountDN;
    private String serviceAccountCredential;
    private String groupBaseRDN;

    public AuthenticationConfigInfo() {}

    public AuthenticationConfigInfo(String url, String userString, int updateInterval, String groupPrefix,
                                    String serviceAccountDN, String serviceAccountCredential, String groupBaseRDN){
        this.url = url;
        this.userString = userString;
        this.updateInterval = updateInterval;
        this.groupPrefix = groupPrefix;
        this.serviceAccountDN = serviceAccountDN;
        this.serviceAccountCredential = serviceAccountCredential;
        this.groupBaseRDN = groupBaseRDN;
    }
	public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUserString() {
        return userString;
    }

    public void setUserString(String userString) {
        this.userString = userString;
    }

    public int getUpdateInterval() {
        return updateInterval;
    }

    public void setUpdateInterval(int updateInterval) {
        this.updateInterval = updateInterval;
    }

    public String getGroupPrefix() {
        return groupPrefix;
    }

    public void setGroupPrefix(String groupPrefix) {
        this.groupPrefix = groupPrefix;
    }

    public String getServiceAccountDN() {
        return serviceAccountDN;
    }

    public void setServiceAccountDN(String serviceAccountDN) {
        this.serviceAccountDN = serviceAccountDN;
    }

    public String getServiceAccountCredential() {
        return serviceAccountCredential;
    }

    public void setServiceAccountCredential(String serviceAccountCredential) {
        this.serviceAccountCredential = serviceAccountCredential;
    }

    public String getGroupBaseRDN() {
        return groupBaseRDN;
    }

    public void setGroupBaseRDN(String groupBaseRDN) {
        this.groupBaseRDN = groupBaseRDN;
    }
}
