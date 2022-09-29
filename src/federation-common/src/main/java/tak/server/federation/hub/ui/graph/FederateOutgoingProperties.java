package tak.server.federation.hub.ui.graph;

public class FederateOutgoingProperties extends NodeProperties {
	
    private String outgoingName;
    private String host;
    private int port;
    private boolean outgoingEnabled;
    
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
	@Override
	public String toString() {
		return "FederateOutgoingProperties [outgoingName=" + outgoingName + ", host=" + host + ", port=" + port
				+ ", outgoingEnabled=" + outgoingEnabled + "]";
	}
}
