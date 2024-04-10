package com.bbn.marti.remote;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

import com.bbn.marti.config.Federation.FederationServer.FederationPort;
import com.bbn.marti.config.Federation.FederationServer.V1Tls;
import com.bbn.marti.config.Federation.MissionDisruptionTolerance.Mission;;

public class FederationConfigInfo implements Serializable {

	private static final long serialVersionUID = 5098916040276969425L;

	private boolean isEnabled;
	private boolean serverPortEnabled;
	private int serverPortv1;
	private int serverPortv2;
	private boolean serverPortEnabledv2;
	private String truststorePath;
	private String truststorePass;
	private String tlsVersion;
	private String webBaseURL;
	private boolean allowMissionFederation;
	private boolean allowDataFeedFederation;
	private boolean allowFederatedDelete;
	private boolean enableMissionFederationDisruptionTolerance;
	private long missionFederationDisruptionToleranceRecencySeconds;
	private List<Mission> missionInterval;
	private int coreVersion;
	private List<FederationPort> v1Ports = new ArrayList<FederationPort>();
	private List<V1Tls> v1Tls = new ArrayList<>();
	private boolean enableDataPackageAndMissionFileFilter;
	private List<String> fileExtension;

	public FederationConfigInfo() {
	}

	public FederationConfigInfo(boolean isEnabled, List<V1Tls> v1Tls, List<FederationPort> v1Ports, int serverPortv1, int serverPortv2, boolean serverPortEnabled,
								boolean serverPortEnabledv2, String truststorePath, String truststorePass, String tlsVersion,
								String webBaseURL, boolean allowMissionFederation, boolean allowDataFeedFederation, boolean allowFederatedDelete, boolean enableMissionFederationDisruptionTolerance,
								long missionFederationDisruptionToleranceRecencySeconds, List<Mission> missionInterval, int coreVersion,
								boolean enableDataPackageAndMissionFileFilter, List<String> fileExtension) {

		this.isEnabled = isEnabled;
		this.v1Ports = v1Ports;
		this.serverPortv1 = serverPortv1;
		this.serverPortv2 = serverPortv2;
		this.serverPortEnabled = serverPortEnabled;
		this.serverPortEnabledv2 = serverPortEnabledv2;
		this.truststorePath = truststorePath;
		this.truststorePass = truststorePass;
		this.tlsVersion = tlsVersion;
		this.webBaseURL = webBaseURL;
		this.allowMissionFederation = allowMissionFederation;
		this.allowDataFeedFederation = allowDataFeedFederation;
		this.allowFederatedDelete = allowFederatedDelete;
		this.enableMissionFederationDisruptionTolerance = enableMissionFederationDisruptionTolerance;
		this.missionFederationDisruptionToleranceRecencySeconds = missionFederationDisruptionToleranceRecencySeconds;
		this.missionInterval = missionInterval;
		this.coreVersion = coreVersion;
		this.v1Tls = v1Tls;
		this.enableDataPackageAndMissionFileFilter = enableDataPackageAndMissionFileFilter;
		this.fileExtension = fileExtension;
	}

	public int getCoreVersion() {
		return coreVersion;
	}

	public void setCoreVersion(int coreVersion) {
		this.coreVersion = coreVersion;
	}

	public List<V1Tls> getV1Tls() {
		return v1Tls;
	}

	public void setV1Tls(List<V1Tls> v1Tls) {
		this.v1Tls = v1Tls;
	}

	public int getServerPortv1() {
		return serverPortv1;
	}

	public void setServerPortv1(int serverPortv1) {
		this.serverPortv1 = serverPortv1;
	}

	public int getServerPortv2() {
		return serverPortv2;
	}

	public void setServerPortv2(int serverPortv2) {
		this.serverPortv2 = serverPortv2;
	}

	public boolean isServerPortEnabled() {
		return serverPortEnabled;
	}

	public void setServerPortEnabled(boolean serverPortEnabled) {
		this.serverPortEnabled = serverPortEnabled;
	}

	public boolean isServerPortEnabledv2() {
		return serverPortEnabledv2;
	}

	public void setServerPortEnabledv2(boolean serverPortEnabledv2) {
		this.serverPortEnabledv2 = serverPortEnabledv2;
	}

	public String getTruststorePath() {
		return truststorePath;
	}

	public void setTruststorePath(String truststorePath) {
		this.truststorePath = truststorePath;
	}

	public String getTlsVersion() {
		return tlsVersion;
	}

	public void setTlsVersion(String tlsVersion) {
		this.tlsVersion = tlsVersion;
	}

	public boolean isEnabled() {
		return isEnabled;
	}

	public void setEnabled(boolean isEnabled) {
		this.isEnabled = isEnabled;
	}

	public String getTruststorePass() {
		return truststorePass;
	}

	public void setTruststorePass(String truststorePass) {
		this.truststorePass = truststorePass;
	}

	public String getWebBaseURL() {
		return webBaseURL;
	}

	public void setWebBaseURL(String webBaseURL) {
		this.webBaseURL = webBaseURL;
	}

	public boolean isAllowMissionFederation() {
		return allowMissionFederation;
	}

	public void setAllowMissionFederation(boolean allowMissionFederation) {
		this.allowMissionFederation = allowMissionFederation;
	}
	
	public boolean isAllowDataFeedFederation() {
		return allowDataFeedFederation;
	}

	public void setAllowDataFeedFederation(boolean allowDataFeedFederation) {
		this.allowDataFeedFederation = allowDataFeedFederation;
	}

	public boolean isAllowFederatedDelete() {
		return allowFederatedDelete;
	}

	public void setAllowFederatedDelete(boolean allowFederatedDelete) {
		this.allowFederatedDelete = allowFederatedDelete;
	}

	public boolean isEnableMissionFederationDisruptionTolerance() {
	    return this.enableMissionFederationDisruptionTolerance;
	}

	public void setEnableMissionFederationDisruptionTolerance(boolean enableMissionFederationDisruptionTolerance) {
	    this.enableMissionFederationDisruptionTolerance = enableMissionFederationDisruptionTolerance;
	}

	public long getMissionFederationDisruptionToleranceRecencySeconds() {
        return missionFederationDisruptionToleranceRecencySeconds;
    }

    public void setMissionFederationDisruptionToleranceRecencySeconds(
            long missionFederationDisruptionToleranceRecencySeconds) {
        this.missionFederationDisruptionToleranceRecencySeconds = missionFederationDisruptionToleranceRecencySeconds;
    }

    public List<Mission> getMissionInterval() {
        return missionInterval;
    }

    public void setMissionInterval(List<Mission> missionInterval) {
        this.missionInterval = missionInterval;
    }

    public List<FederationPort> getV1Ports() {
		return this.v1Ports;
	}

	public void setV1Ports(List<FederationPort> ports) {
		this.v1Ports = ports;
	}

	public String v1PortsToString() {
		String ports = "<";
		for (FederationPort p : this.v1Ports) {
			ports += "[port: " + p.getPort() + ", tlsVersion: " + p.getTlsVersion() + "]";
		}
		ports += ">";
		return ports;
	}

	public boolean isEnableDataPackageAndMissionFileFilter() {
		return enableDataPackageAndMissionFileFilter;
	}

	public void setEnableMissionFederationFileFilter(boolean enableDataPackageFileFilter) {
		this.enableDataPackageAndMissionFileFilter = enableDataPackageFileFilter;
	}

	public List<String> getFileExtension() {
		return fileExtension;
	}

	public void setFileExtension(List<String> fileExtension) {
		this.fileExtension = fileExtension;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (allowFederatedDelete ? 1231 : 1237);
		result = prime * result + (allowMissionFederation ? 1231 : 1237);
		result = prime * result + (isEnabled ? 1231 : 1237);
		result = prime * result + serverPortv2;
		result = prime * result + ((tlsVersion == null) ? 0 : tlsVersion.hashCode());
		result = prime * result + ((truststorePass == null) ? 0 : truststorePass.hashCode());
		result = prime * result + ((truststorePath == null) ? 0 : truststorePath.hashCode());
		result = prime * result + ((webBaseURL == null) ? 0 : webBaseURL.hashCode());
		if (!v1Ports.isEmpty()) {
			for (FederationPort p : v1Ports) {
				result = prime * result + p.getPort();
				result = prime * result + ((p.getTlsVersion() == null) ? 0 : p.getTlsVersion().hashCode());
			}
		}

		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
            return true;
        }
		if (obj == null) {
            return false;
        }
		if (getClass() != obj.getClass()) {
            return false;
        }
		FederationConfigInfo other = (FederationConfigInfo) obj;
		if (allowFederatedDelete != other.allowFederatedDelete) {
            return false;
        }
		if (allowMissionFederation != other.allowMissionFederation) {
            return false;
        }
		if (isEnabled != other.isEnabled) {
            return false;
        }
		if (v1Ports.isEmpty() != other.v1Ports.isEmpty()) {
            return false;
        }
		for (FederationPort p : v1Ports) {
			boolean matched = false;
			for (FederationPort q : other.v1Ports) {
				if (q.getPort() == p.getPort() && q.getTlsVersion().equals(p.getTlsVersion())) {
					matched = true;
					break;
				}
			}
			if (!matched) {
				return false;
			}
		}
		if (serverPortv2 != other.serverPortv2) {
            return false;
        }
		if (coreVersion != other.coreVersion) {
            return false;
        }
		if (tlsVersion == null) {
			if (other.tlsVersion != null) {
                return false;
            }
		} else if (!tlsVersion.equals(other.tlsVersion)) {
            return false;
        }
		if (truststorePass == null) {
			if (other.truststorePass != null) {
                return false;
            }
		} else if (!truststorePass.equals(other.truststorePass)) {
            return false;
        }
		if (truststorePath == null) {
			if (other.truststorePath != null) {
                return false;
            }
		} else if (!truststorePath.equals(other.truststorePath)) {
            return false;
        }
		if (webBaseURL == null) {
			if (other.webBaseURL != null) {
                return false;
            }
		} else if (!webBaseURL.equals(other.webBaseURL)) {
            return false;
        }
		return true;
	}

	@Override
	public String toString() {
		return "FederationConfigInfo [isEnabled=" + isEnabled + ", serverPortEnabled=" + serverPortEnabled
				+ ", serverPortv1=" + serverPortv1 + ", serverPortv2=" + serverPortv2 + ", serverPortEnabledv2="
				+ serverPortEnabledv2 + ", truststorePath=" + truststorePath + ", truststorePass=" + truststorePass
				+ ", tlsVersion=" + tlsVersion + ", webBaseURL=" + webBaseURL + ", allowMissionFederation="
				+ allowMissionFederation + ", allowDataFeedFederation=" + allowDataFeedFederation
				+ ", allowFederatedDelete=" + allowFederatedDelete + ", enableMissionFederationDisruptionTolerance="
				+ enableMissionFederationDisruptionTolerance + ", missionFederationDisruptionToleranceRecencySeconds="
				+ missionFederationDisruptionToleranceRecencySeconds + ", missionInterval=" + missionInterval
				+ ", coreVersion=" + coreVersion + ", v1Ports=" + v1Ports + ", v1Tls=" + v1Tls
				+ ", enableDataPackageAndMissionFileFilter=" + enableDataPackageAndMissionFileFilter
				+ ", fileExtension=" + fileExtension + "]";
	}
}
