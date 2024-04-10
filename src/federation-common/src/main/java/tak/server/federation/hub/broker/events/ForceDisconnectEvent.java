package tak.server.federation.hub.broker.events;

public class ForceDisconnectEvent extends BrokerServerEvent {
	
	private String connectionId;
	
	public ForceDisconnectEvent(Object source, String connectionId) {
		super(source);
		this.connectionId = connectionId;
	}

	public String getConnectionId() {
		return connectionId;
	}
}
