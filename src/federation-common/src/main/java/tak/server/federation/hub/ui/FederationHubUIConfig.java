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
    
    private boolean allowOauth  = false;
    private Integer oauthPort;
    private String keycloakServerName;
    private String keycloakDerLocation;
    private String keycloakClientId;
    private String keycloakSecret;
    private String keycloakrRedirectUri;
    private String keycloakAuthEndpoint;
    private String keycloakTokenEndpoint;
    private String keycloakClaimName;
    private String keycloakAdminClaimValue;
    private String keycloakAccessTokenName = "access_token";
    private String keycloakRefreshTokenName = "refresh_token";

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
	public String getKeycloakDerLocation() {
		return keycloakDerLocation;
	}
	public void setKeycloakDerLocation(String keycloakDerLocation) {
		this.keycloakDerLocation = keycloakDerLocation;
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
	public String getKeycloakAuthEndpoint() {
		return keycloakAuthEndpoint;
	}
	public void setKeycloakAuthEndpoint(String keycloakAuthEndpoint) {
		this.keycloakAuthEndpoint = keycloakAuthEndpoint;
	}
	public String getKeycloakTokenEndpoint() {
		return keycloakTokenEndpoint;
	}
	public void setKeycloakTokenEndpoint(String keycloakTokenEndpoint) {
		this.keycloakTokenEndpoint = keycloakTokenEndpoint;
	}
	public boolean isAllowOauth() {
		return allowOauth;
	}
	public void setAllowOauth(boolean allowOauth) {
		this.allowOauth = allowOauth;
	}
	public String getKeycloakAccessTokenName() {
		return keycloakAccessTokenName;
	}
	public void setKeycloakAccessTokenName(String keycloakAccessTokenName) {
		this.keycloakAccessTokenName = keycloakAccessTokenName;
	}
	public String getKeycloakRefreshTokenName() {
		return keycloakRefreshTokenName;
	}
	public void setKeycloakRefreshTokenName(String keycloakRefreshTokenName) {
		this.keycloakRefreshTokenName = keycloakRefreshTokenName;
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
	@Override
	public String toString() {
		return "FederationHubUIConfig [keystoreType=" + keystoreType + ", keystoreFile=" + keystoreFile
				+ ", keystorePassword=" + keystorePassword + ", truststoreType=" + truststoreType + ", truststoreFile="
				+ truststoreFile + ", keyAlias=" + keyAlias + ", authUsers=" + authUsers + ", port=" + port
				+ ", allowOauth=" + allowOauth + ", oauthPort=" + oauthPort + ", keycloakServerName="
				+ keycloakServerName + ", keycloakDerLocation=" + keycloakDerLocation + ", keycloakClientId="
				+ keycloakClientId + ", keycloakSecret=" + keycloakSecret + ", keycloakrRedirectUri="
				+ keycloakrRedirectUri + ", keycloakAuthEndpoint=" + keycloakAuthEndpoint + ", keycloakTokenEndpoint="
				+ keycloakTokenEndpoint + ", keycloakAccessTokenName=" + keycloakAccessTokenName
				+ ", keycloakRefreshTokenName=" + keycloakRefreshTokenName + ", keycloakClaimName=" + keycloakClaimName
				+ ", keycloakAdminClaimValue=" + keycloakAdminClaimValue + "]";
	}
}
