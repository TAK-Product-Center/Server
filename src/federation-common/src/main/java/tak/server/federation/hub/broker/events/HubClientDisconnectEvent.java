package tak.server.federation.hub.broker.events;

public class HubClientDisconnectEvent extends BrokerServerEvent {
	private static final long serialVersionUID = 2984272369353500481L;
	
	private String hubId;
	
	public HubClientDisconnectEvent(Object source, String hubId) {
		super(source);
		this.hubId = hubId;
	}

	public String getHubId() {
		return hubId;
	}
}
