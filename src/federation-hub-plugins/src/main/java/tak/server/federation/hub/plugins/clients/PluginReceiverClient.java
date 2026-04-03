package tak.server.federation.hub.plugins.clients;

import com.atakmap.Tak.BinaryBlob;
import com.atakmap.Tak.FederateGroups;
import com.atakmap.Tak.FederatedEvent;
import com.atakmap.Tak.ROL;

public interface PluginReceiverClient extends PluginClient {
	void receiveFederatedEvent(FederatedEvent event);
	void receiveFederateGroups(FederateGroups groups);
	void receiveBinaryBlob(BinaryBlob event);
	void receiveROL(ROL event);
}
