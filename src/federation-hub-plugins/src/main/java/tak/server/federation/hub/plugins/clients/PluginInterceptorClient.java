package tak.server.federation.hub.plugins.clients;

import java.util.concurrent.CompletableFuture;

import com.atakmap.Tak.BinaryBlob;
import com.atakmap.Tak.FederateGroups;
import com.atakmap.Tak.FederatedEvent;
import com.atakmap.Tak.ROL;

public interface PluginInterceptorClient extends PluginClient {
	CompletableFuture<FederatedEvent> interceptFederatedEvent(FederatedEvent event);
	CompletableFuture<FederateGroups> interceptFederateGroups(FederateGroups groups);
	CompletableFuture<BinaryBlob> interceptBinaryBlob(BinaryBlob event);
	CompletableFuture<ROL> interceptROL(ROL event);
}
