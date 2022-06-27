package com.bbn.marti.remote;

import java.io.Serializable;

public class SecurityConfigInfo implements Serializable {
    private String keystoreFile;
    private String truststoreFile;
    private String keystorePass;
    private String truststorePass;
    private String tlsVersion;
    private boolean x509Groups;
    private boolean x509addAnon;
    private boolean enableEnrollment;
    private String caType;
    private String signingKeystoreFile;
    private String signingKeystorePass;
    private int validityDays;
    private String mscaUserName;
    private String mscaPassword;
    private String mscaTruststore;
    private String mscaTruststorePass;
    private String mscaTemplateName;

    public SecurityConfigInfo() {}

    public SecurityConfigInfo(String keystoreFile, String truststoreFile, String keystorePass, String truststorePass,
                              String tlsVersion, boolean x509Groups, boolean x509addAnon, boolean enableEnrollment,
                              String caType, String signingKeystoreFile, String signingKeystorePass, int validityDays,
                              String mscaUserName, String mscaPassword,
                              String mscaTruststore, String mscaTruststorePass,
                              String mscaTemplateName) {
        this.keystoreFile = keystoreFile;
        this.truststoreFile = truststoreFile;
        this.keystorePass = keystorePass;
        this.truststorePass = truststorePass;
        this.tlsVersion = tlsVersion;
        this.x509Groups = x509Groups;
        this.x509addAnon = x509addAnon;
        this.enableEnrollment = enableEnrollment;
        this.caType = caType;
        this.signingKeystoreFile = signingKeystoreFile;
        this.signingKeystorePass = signingKeystorePass;
        this.validityDays = validityDays;
        this.mscaUserName = mscaUserName;
        this.mscaPassword = mscaPassword;
        this.mscaTruststore = mscaTruststore;
        this.mscaTruststorePass = mscaTruststorePass;
        this.mscaTemplateName = mscaTemplateName;
    }
    public String getKeystoreFile() {
        return keystoreFile;
    }

    public void setKeystoreFile(String keystoreFile) {
        this.keystoreFile = keystoreFile;
    }

    public String getTruststoreFile() {
        return truststoreFile;
    }

    public void setTruststoreFile(String truststoreFile) {
        this.truststoreFile = truststoreFile;
    }

    public String getKeystorePass() {
        return keystorePass;
    }

    public void setKeystorePass(String keystorePass) {
        this.keystorePass = keystorePass;
    }

    public String getTruststorePass() {
        return truststorePass;
    }

    public void setTruststorePass(String truststorePass) {
        this.truststorePass = truststorePass;
    }

    public String getTlsVersion() {
        return tlsVersion;
    }

    public void setTlsVersion(String tlsVersion) {
        this.tlsVersion = tlsVersion;
    }

    public boolean isX509Groups() {
        return x509Groups;
    }

    public void setX509Groups(boolean x509Groups) {
        this.x509Groups = x509Groups;
    }

    public boolean isX509addAnon() {
        return x509addAnon;
    }

    public void setX509addAnon(boolean x509addAnon) {
        this.x509addAnon = x509addAnon;
    }

    public boolean isEnableEnrollment() {
        return enableEnrollment;
    }

    public void setEnableEnrollment(boolean enableEnrollment) {
        this.enableEnrollment = enableEnrollment;
    }

    public String getCaType() {
        return caType;
    }

    public void setCaType(String caType) {
        this.caType = caType;
    }

    public String getSigningKeystoreFile() {
        return signingKeystoreFile;
    }

    public void setSigningKeystoreFile(String signingKeystoreFile) { this.signingKeystoreFile = signingKeystoreFile; }

    public String getSigningKeystorePass() {
        return signingKeystorePass;
    }

    public void setSigningKeystorePass(String signingKeystorePass) { this.signingKeystorePass = signingKeystorePass; }

    public int getValidityDays() { return validityDays; }

    public void setValidityDays(int validityDays) { this.validityDays = validityDays; }

    public String getMscaUserName() {
        return mscaUserName;
    }

    public void setMscaUserName(String mscaUserName) {
        this.mscaUserName = mscaUserName;
    }

    public String getMscaPassword() {
        return mscaPassword;
    }

    public void setMscaPassword(String mscaPassword) {
        this.mscaPassword = mscaPassword;
    }

    public String getMscaTruststore() {
        return mscaTruststore;
    }

    public void setMscaTruststore(String mscaTruststore) {
        this.mscaTruststore = mscaTruststore;
    }

    public String getMscaTruststorePass() {
        return mscaTruststorePass;
    }

    public void setMscaTruststorePass(String mscaTruststorePass) {
        this.mscaTruststorePass = mscaTruststorePass;
    }

    public String getMscaTemplateName() {
        return mscaTemplateName;
    }

    public void setMscaTemplateName(String mscaTemplateName) {
        this.mscaTemplateName = mscaTemplateName;
    }
}
