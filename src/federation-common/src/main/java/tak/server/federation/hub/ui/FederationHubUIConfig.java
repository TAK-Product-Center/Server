package tak.server.federation.hub.ui;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
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
    
    private boolean allowOauth  = false;
    private Integer oauthPort;
    private String keycloakServerName;
    private String keycloakTlsCertFile;
    private String keycloakClientId;
    private String keycloakSecret;
    private String keycloakrRedirectUri;
    private String keycloakConfigurationEndpoint;
    private String keycloakClaimName;
    private String keycloakAdminClaimValue;
    
    private boolean oauthPortTls = true;
    
    private boolean enableFlowIndicators = true;

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

    public Integer getOauthPort() {
		return oauthPort;
	}
	public void setOauthPort(Integer oauthPort) {
		this.oauthPort = oauthPort;
	}
	public String getKeycloakServerName() {
		return keycloakServerName;
	}
	public void setKeycloakServerName(String keycloakServerName) {
		this.keycloakServerName = keycloakServerName;
	}
	public String getKeycloakTlsCertFile() {
		return keycloakTlsCertFile;
	}
	public void setKeycloakTlsCertFile(String keycloakTlsCertFile) {
		this.keycloakTlsCertFile = keycloakTlsCertFile;
	}
	public String getKeycloakClientId() {
		return keycloakClientId;
	}
	public void setKeycloakClientId(String keycloakClientId) {
		this.keycloakClientId = keycloakClientId;
	}
	public String getKeycloakSecret() {
		return keycloakSecret;
	}
	public void setKeycloakSecret(String keycloakSecret) {
		this.keycloakSecret = keycloakSecret;
	}
	public String getKeycloakrRedirectUri() {
		return keycloakrRedirectUri;
	}
	public void setKeycloakrRedirectUri(String keycloakrRedirectUri) {
		this.keycloakrRedirectUri = keycloakrRedirectUri;
	}
	public boolean isAllowOauth() {
		return allowOauth;
	}
	public void setAllowOauth(boolean allowOauth) {
		this.allowOauth = allowOauth;
	}
	public String getKeycloakClaimName() {
		return keycloakClaimName;
	}
	public void setKeycloakClaimName(String keycloakClaimName) {
		this.keycloakClaimName = keycloakClaimName;
	}
	public String getKeycloakAdminClaimValue() {
		return keycloakAdminClaimValue;
	}
	public void setKeycloakAdminClaimValue(String keycloakAdminClaimValue) {
		this.keycloakAdminClaimValue = keycloakAdminClaimValue;
	}
	public boolean isEnableFlowIndicators() {
		return enableFlowIndicators;
	}
	public void setEnableFlowIndicators(boolean enableFlowIndicators) {
		this.enableFlowIndicators = enableFlowIndicators;
	}
	
	public boolean isOauthPortTls() {
		return oauthPortTls;
	}
	public void setOauthPortTls(boolean oauthPortTls) {
		this.oauthPortTls = oauthPortTls;
	}
	public String getKeycloakConfigurationEndpoint() {
		return keycloakConfigurationEndpoint;
	}
	public void setKeycloakConfigurationEndpoint(String keycloakConfigurationEndpoint) {
		this.keycloakConfigurationEndpoint = keycloakConfigurationEndpoint;
	}
	public static String getAuthUserFileDefault() {
		return AUTH_USER_FILE_DEFAULT;
	}
	public void setTruststorePassword(String truststorePassword) {
		this.truststorePassword = truststorePassword;
	}
	@Override
	public String toString() {
		return "FederationHubUIConfig [keystoreType=" + keystoreType + ", keystoreFile="
				+ keystoreFile + ", truststoreType=" + truststoreType + ", truststoreFile=" + truststoreFile
				+ ", keyAlias=" + keyAlias + ", authUsers=" + authUsers + ", port=" + port + ", allowOauth="
				+ allowOauth + ", oauthPort=" + oauthPort + ", keycloakServerName=" + keycloakServerName
				+ ", keycloakTlsCertFile=" + keycloakTlsCertFile + ", keycloakClientId=" + keycloakClientId
				+ ", keycloakrRedirectUri=" + keycloakrRedirectUri + ", keycloakConfigurationEndpoint="
				+ keycloakConfigurationEndpoint + ", keycloakClaimName=" + keycloakClaimName
				+ ", keycloakAdminClaimValue=" + keycloakAdminClaimValue + ", oauthPortTls=" + oauthPortTls
				+ ", enableFlowIndicators=" + enableFlowIndicators + "]";
	}
}
