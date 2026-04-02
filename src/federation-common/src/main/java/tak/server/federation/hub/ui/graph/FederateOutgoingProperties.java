package tak.server.federation.hub.ui.graph;

public class FederateOutgoingProperties extends NodeProperties {
	
    private String outgoingName;
    private String host;
    private int port;
    private boolean outgoingEnabled;
    private boolean useToken;
    private String tokenType;
    private String token;
    
	public String getOutgoingName() {
		return outgoingName;
	}
	public void setOutgoingName(String outgoingName) {
		this.outgoingName = outgoingName;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	
	public boolean isOutgoingEnabled() {
		return outgoingEnabled;
	}
	public void setOutgoingEnabled(boolean outgoingEnabled) {
		this.outgoingEnabled = outgoingEnabled;
	}
	public String getToken() {
		return token;
	}
	public void setToken(String token) {
		this.token = token;
	}
	
	public boolean isUseToken() {
		return useToken;
	}
	public void setUseToken(boolean useToken) {
		this.useToken = useToken;
	}
	public String getTokenType() {
		return tokenType;
	}
	public void setTokenType(String tokenType) {
		this.tokenType = tokenType;
	}
	@Override
	public String toString() {
		return "FederateOutgoingProperties [outgoingName=" + outgoingName + ", host=" + host + ", port=" + port
				+ ", outgoingEnabled=" + outgoingEnabled + ", useToken=" + useToken + ", tokenType=" + tokenType
				+ ", token=" + token + "]";
	}
}
