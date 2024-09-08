package tak.server.federation.hub.ui.graph;

public class JwtTokenRequestModel {
	private String clientFingerprint;
	private String clientGroup;
	private long expiration;
	public String getClientFingerprint() {
		return clientFingerprint;
	}
	public void setClientFingerprint(String clientFingerprint) {
		this.clientFingerprint = clientFingerprint;
	}
	public String getClientGroup() {
		return clientGroup;
	}
	public void setClientGroup(String clientGroup) {
		this.clientGroup = clientGroup;
	}
	public long getExpiration() {
		return expiration;
	}
	public void setExpiration(long expiration) {
		this.expiration = expiration;
	}
	@Override
	public String toString() {
		return "JwtTokenRequestModel [clientFingerprint=" + clientFingerprint + ", clientGroup=" + clientGroup
				+ ", expiration=" + expiration + "]";
	}
}
