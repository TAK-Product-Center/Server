package tak.server.federation.hub.broker.events;

import java.util.List;

import tak.server.federation.hub.ui.graph.FederationOutgoingCell;

public class UpdatePolicy extends BrokerServerEvent {

	private static final long serialVersionUID = -7955831750683372723L;
	
	private List<FederationOutgoingCell> outgoings;
	public UpdatePolicy(Object source, List<FederationOutgoingCell> outgoings) {
		super(source);
		this.outgoings = outgoings;
	}
	
	public List<FederationOutgoingCell> getOutgoings() {
		return outgoings;
	}
}
