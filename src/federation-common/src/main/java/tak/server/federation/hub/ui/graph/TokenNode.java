package tak.server.federation.hub.ui.graph;

public class TokenNode {
	private String token;
	private long expiration;
	
	public String getToken() {
		return token;
	}
	public void setToken(String token) {
		this.token = token;
	}
	public long getExpiration() {
		return expiration;
	}
	public void setExpiration(long expiration) {
		this.expiration = expiration;
	}
}
