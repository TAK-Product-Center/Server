package tak.server.federation.jwt;

public class JwtTokenResponseModel {
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

	@Override
	public String toString() {
		return "JwtTokenResponseModel [token=" + token + ", expiration=" + expiration + "]";
	}
}
