package tak.server.federation.hub.ui.graph;

public class JwtTokenResponseModel {
	private String token;

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	@Override
	public String toString() {
		return "JwtTokenResponseModel [token=" + token + "]";
	}
}
