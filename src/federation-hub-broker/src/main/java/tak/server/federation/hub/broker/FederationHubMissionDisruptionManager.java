package tak.server.federation.hub.broker.db;

import java.io.ByteArrayInputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atakmap.Tak.BinaryBlob;
import com.atakmap.Tak.FederateHops;
import com.atakmap.Tak.FederateProvenance;
import com.atakmap.Tak.ROL;
import com.bbn.roger.fig.FederationUtils;
import com.google.protobuf.ByteString;
import com.mongodb.BasicDBObject;

import tak.server.federation.Federate;
import tak.server.federation.FederateIdentity;
import tak.server.federation.FederationPolicyGraph;
import tak.server.federation.hub.FederationHubDependencyInjectionProxy;
import tak.server.federation.hub.broker.FederationHubBrokerService;

public class FederationHubMissionDisruptionManager {
	private static final Logger logger = LoggerFactory.getLogger(FederationHubMissionDisruptionManager.class);
	
	private FederationHubDatabaseService federationHubDatabaseService;
	
	public FederationHubMissionDisruptionManager (FederationHubDatabaseService federationHubDatabaseService) {
		this.federationHubDatabaseService = federationHubDatabaseService;
	}
	
	@SuppressWarnings("unchecked")
	// get all the federates that this new connection is allowed to receive from
	private Collection<String> getReceivableFederates(String sourceServerId) throws Exception {
		// make sure all of the persisted federates are in the policy graph
		for(Document document : federationHubDatabaseService.getFederateMetadatas()) {
			String federateId = document.getString("federate_id");
			
			List<String> clientGroups = (List<String>) document.get("client_groups");
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			for (String clientGroup : clientGroups) {
				Federate federate = new Federate(new FederateIdentity(federateId));
				federate.addGroupIdentity(new FederateIdentity(clientGroup));
				List<String> federateGroups = new ArrayList<>();
				federateGroups.add(clientGroup);
				
				FederationHubDependencyInjectionProxy.getInstance().fedHubPolicyManager().addCaFederate(federate, federateGroups);
			}
		}

		Collection<String> receivableFederates = 
				FederationHubDependencyInjectionProxy.getInstance().fedHubPolicyManager().getPolicyGraph()
					.allReceivableFederates(sourceServerId)
					.stream()
					.map(f -> f.getFederateIdentity().getFedId())
					.collect(Collectors.toCollection(HashSet::new));
		
		return receivableFederates;
	}

	@SuppressWarnings("unchecked")
	public OfflineMissionChanges getMissionChangesAndTrackConnectEvent(String federateServerId, List<String> clientGroups) {
		OfflineMissionChanges changes = new OfflineMissionChanges();
	
		try {
			Document metadata = federationHubDatabaseService.getFederateMetadata(federateServerId);
			
			// if this federate previously connected, only pull changes from last update time
			Date lastUpdate = new Date(0L);
			if (metadata != null) {
				lastUpdate = (Date) metadata.get("last_update");
			}
						
			Date now = new Date();
			
			long recencySecs = FederationHubDependencyInjectionProxy.getInstance().fedHubServerConfigManager().getConfig().getMissionFederationRecencySeconds();
			long maxRecencyMillis;
			if (recencySecs == -1) {
			    maxRecencyMillis = lastUpdate.getTime();
			} else {
			    maxRecencyMillis = now.getTime() - (recencySecs * 1000);
			}

			long bestRecencyMillis = lastUpdate.getTime() > maxRecencyMillis ? lastUpdate.getTime() : maxRecencyMillis;
									
			Collection<String> receivableFederates = getReceivableFederates(federateServerId);
			// must get updated policy after we check for receivable federates
			// because getReceivableFederates(...) will modify the graph
			FederationPolicyGraph policyGraph = FederationHubDependencyInjectionProxy.getInstance().fedHubPolicyManager().getPolicyGraph();
			
			for (String receivableFederate: receivableFederates) {
				// for every federate this newly connected federate can receive from,
				// pull the offline changes 
				List<Document> updates = federationHubDatabaseService.getOfflineUpdates(receivableFederate, new Date(bestRecencyMillis));
				for (Document update: updates) {
					ROL.Builder rolBuilder = ROL.newBuilder();
					Document eventRol = update.get("event_rol", Document.class);
					rolBuilder.setProgram(eventRol.getString("rol_program"));				
					
					if (eventRol.containsKey("federate_groups")) {
						List<String> federateGroups = eventRol.getList("federate_groups", String.class);
						rolBuilder.addAllFederateGroups(federateGroups);
					}
					
					if (eventRol.containsKey("federate_provenance")) {
						List<Document> federateProvenances = eventRol.getList("federate_provenance", Document.class);
						for (Document federateProvenance: federateProvenances) {
							rolBuilder.addFederateProvenance(FederateProvenance.newBuilder()
									.setFederationServerName(federateProvenance.getString("federation_server_name"))
									.setFederationServerId(federateProvenance.getString("federation_server_id")));
						}
					}
					
					if (eventRol.containsKey("federate_hops")) {
						Document federateHops = eventRol.get("federate_hops", Document.class);
						rolBuilder.setFederateHops(FederateHops.newBuilder()
								.setMaxHops(federateHops.getLong("max_hops"))
								.setCurrentHops(federateHops.getLong("current_hops")));
					}
					
					ROL rol = rolBuilder.build();
					
					// make sure we check reachability if group filtering is active
					Set<Federate> reachableFederates = FederationHubBrokerService.getGroupFilteredDestinations(rol.getFederateGroupsList(), new FederateIdentity(receivableFederate), policyGraph);
					boolean isReachable = reachableFederates
						.stream()
						.map(f -> f.getFederateIdentity().getFedId())
						.anyMatch(id -> id.equals(federateServerId));
					
					if (isReachable) {
						if (eventRol.containsKey("resource_object_id")) 
							changes.addResourceROL(eventRol.getObjectId("resource_object_id"), rolBuilder);
						else
							changes.addROL(rol);
					}
				}
			}
			
			federationHubDatabaseService.addFederateMetadata(federateServerId, clientGroups);
		} catch (Exception e) {
			logger.error("getMissionChangesAndTrackConnectEvent error", e);
		}
		
		logger.info("Found " + (changes.getRols().size() + changes.getResourceRols().size()) + " mission changes for " + federateServerId);
		
		return changes;
	}
	
	public ROL hydrateResourceROL(ObjectId resourceObjectId, ROL.Builder rol) {
		try {
			byte[] resource = federationHubDatabaseService.getResource(resourceObjectId);
			
			if (resource == null) return rol.build();
			
			BinaryBlob blob = BinaryBlob.newBuilder().setData(ByteString.readFrom(new ByteArrayInputStream(resource))).build();
			rol.addPayload(blob);
		} catch (Exception e) {
			logger.error("hydrateResourceROL error", e);
		}	
		
		return rol.build();
	}
	
	public ObjectId addResource(byte[] data, Map<String, Object>  parameters) {
		try {			
			return federationHubDatabaseService.addResource(data, parameters);
		} catch (Exception e) {
			logger.error("Error adding resource to DB", e);
			return null;
		}
	}
	
	public void storeRol(ROL event, String res, String op, Map<String, Object>  parameters, String federateServerId) {	
		this.storeRol(event, res, op, parameters, federateServerId, null);
	}
	
	public void storeRol(ROL event, String res, String op, Map<String, Object>  parameters, String federateServerId, ObjectId resourceObjectId) {		
		try {
			BasicDBObject rolParameters = new BasicDBObject();
			rolParameters.putAll(parameters);
					
			List<BasicDBObject> federateProvenances = null;
			if (event.getFederateProvenanceList() != null) {
				federateProvenances = event.getFederateProvenanceList()
						.stream()
						.map(p -> {
							BasicDBObject prov = new BasicDBObject();
							prov.append("federation_server_id", p.getFederationServerId());
							prov.append("federation_server_name", p.getFederationServerName());
							return prov;
						}).collect(Collectors.toList());
			}
			
			BasicDBObject federateHops = null;
			if (event.getFederateHops() != null) {
				federateHops = new BasicDBObject();
				federateHops.append("max_hops", event.getFederateHops().getMaxHops());
				federateHops.append("current_hops", event.getFederateHops().getCurrentHops());
			}
			
			BasicDBObject eventRol = new BasicDBObject();
			eventRol.put("rol_program", event.getProgram());
			eventRol.put("rol_resource", res);
			eventRol.put("rol_operation", op);
			eventRol.put("rol_parameters", rolParameters);
			eventRol.put("federate_groups", event.getFederateGroupsList());
			eventRol.put("federate_provenance", federateProvenances);
			eventRol.put("federate_hops", federateHops);
			
			if (resourceObjectId != null) {
				eventRol.put("resource_object_id", resourceObjectId);
			}
			
			Document doc = new Document();
			doc.append("federate_id", federateServerId);
			doc.append("federate_metadata_id", federationHubDatabaseService.getFederateMetadata(federateServerId).get("_id"));
			doc.append("event_kind", "send-changes");
			doc.append("received_time", Instant.now());
			doc.append("event_rol", eventRol);

			federationHubDatabaseService.storeRol(doc);
		} catch (Exception e) {
			logger.error("Error storing ROL in DB", e);
		}
	}
	
	private void isDBAvailable() {
		
	}
	
	public static final class OfflineMissionChanges {
		Map<ObjectId, ROL.Builder> resourceRols = new HashMap<>();
		List<ROL> rols = new ArrayList<>();
		
		public Map<ObjectId, ROL.Builder> getResourceRols() {
			return resourceRols;
		}
		
		public List<ROL> getRols() {
			return rols;
		}
		
		public void addResourceROL(ObjectId key, ROL.Builder value) {
			resourceRols.put(key, value);
		}
		
		public void addROL(ROL e) {
			rols.add(e);
		}
	}
}
