package tak.server.federation.hub.broker.db;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bson.types.ObjectId;

public interface FederationHubDatabaseService {
	
	void storeRol(Document rol);

	Document addFederateMetadata(String id, List<String> clientGroups);
	Document getFederateMetadata(String id);
	List<Document> getFederateMetadatas();

	void trackFederateConnect(String id);

	ObjectId addResource(byte[] data, Map<String, Object> parameters);
	byte[] getResource(ObjectId resourceObjectId);

	List<Document> getOfflineUpdates(String id, Date lastUpdate);
}
