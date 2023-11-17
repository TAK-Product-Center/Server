package tak.server.federation.hub.broker;

import java.util.Set;
import java.util.stream.Collectors;

import tak.server.federation.FederateIdentity;

public class HubConnectionInfo {
	
	private String connectionId;
	private String localConnectionType;
	private String remoteConnectionType;
    private String remoteServerId;
    private int federationProtocolVersion;
    private Set<String> groupIdentities;
    private String remoteAddress;    
    
	public String getRemoteAddress() {
		return remoteAddress;
	}
	public void setRemoteAddress(String remoteAddress) {
		this.remoteAddress = remoteAddress;
	}
	public String getConnectionId() {
		return connectionId;
	}
	public void setConnectionId(String connectionId) {
		this.connectionId = connectionId;
	}
	public String getLocalConnectionType() {
		return localConnectionType;
	}
	public void setLocalConnectionType(String localConnectionType) {
		this.localConnectionType = localConnectionType;
	}
	public String getRemoteConnectionType() {
		return remoteConnectionType;
	}
	public void setRemoteConnectionType(String remoteConnectionType) {
		this.remoteConnectionType = remoteConnectionType;
	}
	public String getRemoteServerId() {
		return remoteServerId;
	}
	public void setRemoteServerId(String remoteServerId) {
		this.remoteServerId = remoteServerId;
	}
	public int getFederationProtocolVersion() {
		return federationProtocolVersion;
	}
	public void setFederationProtocolVersion(int federationProtocolVersion) {
		this.federationProtocolVersion = federationProtocolVersion;
	}
	public Set<String> getGroupIdentities() {
		return groupIdentities;
	}
	public void setGroupIdentities(Set<FederateIdentity> groupIdentities) {
		this.groupIdentities = groupIdentities.stream().map(f -> f.getFedId()).collect(Collectors.toSet());
	}
	@Override
	public String toString() {
		return "HubConnectionInfo [connectionId=" + connectionId + ", localConnectionType=" + localConnectionType
				+ ", remoteConnectionType=" + remoteConnectionType + ", remoteServerId=" + remoteServerId
				+ ", federationProtocolVersion=" + federationProtocolVersion + "]";
	}
}
