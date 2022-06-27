package tak.server.federation.hub.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FederationHubUIConfig {

    public static final String AUTH_USER_FILE_DEFAULT = "/opt/tak/federation-hub/authorized_users.yml";

    private String keystoreType = "JKS";
    private String keystoreFile = "";
    private String keystorePassword;

    private String truststoreType = "JKS";
    private String truststoreFile = "";
    private String truststorePassword;

    private String keyAlias = "";

    private String authUsers = AUTH_USER_FILE_DEFAULT;

    private Integer port = 9100;

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

    public String getKeyAlias() {
        return keyAlias;
    }
    public void setKeyAlias(String keyAlias) {
        this.keyAlias = keyAlias;
    }

    public String getAuthUsers() {
        return this.authUsers;
    }
    public void setAuthUsers(String authUsers) {
        this.authUsers = authUsers;
    }

    public Integer getPort() {
        return port;
    }
    public void setPort(Integer port) {
        this.port = port;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("FederationHubUIConfig [keystoreType=");
        builder.append(keystoreType);
        builder.append(", keystoreFile=");
        builder.append(keystoreFile);
        builder.append(", keystorePassword=");
        builder.append(keystorePassword);
        builder.append(", truststoreType=");
        builder.append(truststoreType);
        builder.append(", truststoreFile=");
        builder.append(truststoreFile);
        builder.append(", truststorePassword=");
        builder.append(truststorePassword);
        builder.append(", keyAlias=");
        builder.append(keyAlias);
        builder.append(", port=");
        builder.append(port);
        builder.append("]");

        return builder.toString();
    }
}
