package tak.server.federation.hub.ui.graph;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FederationTokenGroupProperties extends NodeProperties {
    private boolean interconnected;
    private List<TokenNode> tokens;

    public boolean isInterconnected() {
        return interconnected;
    }

    public void setInterconnected(boolean interconnected) {
        this.interconnected = interconnected;
    }

    public List<TokenNode> getTokens() {
		return tokens;
	}

	public void setTokens(List<TokenNode> tokens) {
		this.tokens = tokens;
	}
}
