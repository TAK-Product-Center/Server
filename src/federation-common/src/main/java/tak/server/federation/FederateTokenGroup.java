package tak.server.federation;

public class FederateTokenGroup extends FederateGroup {

	private String token;
	
	public FederateTokenGroup(FederateIdentity federateIdentity) {
		super(federateIdentity);
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}
}
