package tak.server.federation;

import com.bbn.marti.config.Federation.Federate;
import com.bbn.marti.remote.groups.ConnectionInfo;

/**
 */
public class FederationSubscriptionCacheDAO {
	
	public ConnectionInfo connectionInfo;
	public Federate federate;
	
	public FederationSubscriptionCacheDAO(ConnectionInfo connectionInfo, Federate federate) {
		this.connectionInfo = connectionInfo;
		this.federate = federate;
	}

	@Override
	public String toString() {
		return "FederationSubscriptionCacheDAO [connectionInfo=" + connectionInfo + ", federate=" + federate + "]";
	}
}
