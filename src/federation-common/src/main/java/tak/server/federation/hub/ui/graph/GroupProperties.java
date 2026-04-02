package tak.server.federation.hub.ui.graph;

public class GroupProperties extends NodeProperties {
    private boolean interconnected;
    private boolean allowTokenAuth;
    private long tokenAuthDuration;
    
    public boolean isInterconnected() {
        return interconnected;
    }

    public void setInterconnected(boolean interconnected) {
        this.interconnected = interconnected;
    }

	public boolean isAllowTokenAuth() {
		return allowTokenAuth;
	}

	public void setAllowTokenAuth(boolean allowTokenAuth) {
		this.allowTokenAuth = allowTokenAuth;
	}

	public long getTokenAuthDuration() {
		return tokenAuthDuration;
	}

	public void setTokenAuthDuration(long tokenAuthDuration) {
		this.tokenAuthDuration = tokenAuthDuration;
	}
    
}
