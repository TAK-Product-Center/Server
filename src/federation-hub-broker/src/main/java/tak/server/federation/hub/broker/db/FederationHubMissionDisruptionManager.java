package tak.server.federation.hub.broker.db;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.atakmap.Tak.*;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;
import com.mongodb.BasicDBObject;

import tak.server.federation.Federate;
import tak.server.federation.FederateEdge;
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
				FederationHubDependencyInjectionProxy.getInstance().fedHubPolicyManager().getActivePolicyGraph()
					.allReceivableFederates(sourceServerId)
					.stream()
					.map(f -> f.getFederateIdentity().getFedId())
					.collect(Collectors.toCollection(HashSet::new));
		
		return receivableFederates;
	}

	@SuppressWarnings("unchecked")
	public OfflineMissionChanges getMissionChangesAndTrackConnectEvent(String federateServerId, List<String> clientGroups) {

		logger.trace("getMissionChangesAndTrackConnectEvent called.");

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
			FederationPolicyGraph policyGraph = FederationHubDependencyInjectionProxy.getInstance().fedHubPolicyManager().getActivePolicyGraph();
						
			for (String receivableFederate: receivableFederates) {
				// for every federate this newly connected federate can receive from,
				// pull the offline changes 
				List<Document> updates = federationHubDatabaseService.getOfflineUpdates(receivableFederate, new Date(bestRecencyMillis));
				for (Document update: updates) {
					ROL.Builder rolBuilder = ROL.newBuilder();
					Document eventRol = update.get("event_rol", Document.class);

					rolBuilder.setProgram(eventRol.getString("rol_program"));

					int rolPayloadCount = eventRol.getInteger("rol_payload_count");
					for (int i = 0; i < rolPayloadCount; i++) {
						Document d = (Document) eventRol.get("rol_payload_" + i);
						// if d is null then the payload was a file, let the rol hydration handle adding it to the rls
						
						if (d != null) {
							BinaryBlob payload = BlobBsonMapper.fromBson(d);
							logger.trace("for payload {}, got binary blob {}", i, payload);
							rolBuilder.addPayload(payload);
						}
					}
										
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
					
					if (eventRol.containsKey("federate_group_hop_limits")) {
						FederateGroupHopLimits.Builder builder = FederateGroupHopLimits.newBuilder();
						
						Document groupHopLimits = eventRol.get("federate_group_hop_limits", Document.class);
						builder.setUseFederateGroupHopLimits(groupHopLimits.getBoolean("use_federate_group_hop_limits", false));
	
						List<Document> limits = groupHopLimits.getList("limits", Document.class);
						limits.forEach(limit -> {
							FederateGroupHopLimit.Builder limitBuilder = FederateGroupHopLimit.newBuilder();
							limitBuilder.setGroupName(limit.getString("group_name"));
							limitBuilder.setMaxHops(limit.getLong("max_hops"));
							limitBuilder.setCurrentHops(limit.getLong("current_hops"));
							builder.addLimits(limitBuilder.build());
						});
						rolBuilder.setFederateGroupHopLimits(builder.build());
					}
															
					
					// make sure we check  group filtering reachability
					FederateEdge edge = policyGraph.getEdge(policyGraph.getNode(receivableFederate), policyGraph.getNode(federateServerId));
					if (FederationHubBrokerService.isDestinationEdgeReachableByGroupFilter(edge, rolBuilder.getFederateGroupsList())) {
						FederateGroupHopLimits groupHopLimits =  rolBuilder.getFederateGroupHopLimits();
    					if (groupHopLimits != null && groupHopLimits.getLimitsList().size() > 0) {
    						groupHopLimits = FederationHubBrokerService.removeEdgeFilteredHopLimitedGroupsFromList(groupHopLimits, edge);
    						rolBuilder.setFederateGroupHopLimits(groupHopLimits);
    					}
    					
    					List<String> federateGroups =  rolBuilder.getFederateGroupsList();
    					if (federateGroups != null && federateGroups.size() > 0) {
    						List<String> filteredFederateGroups = FederationHubBrokerService.removeFilteredGroups(federateGroups, edge);
    						rolBuilder.clearFederateGroups().addAllFederateGroups(filteredFederateGroups);			
    					}
    					
    					ROL rol = rolBuilder.build();
						if (eventRol.containsKey("resource_object_id")) {
							logger.info("adding resource rol: {}", rol);
							changes.addResourceROL(eventRol.getObjectId("resource_object_id"), rolBuilder.build());
						}
						else {
							logger.trace("adding change rol: {}", rol);
							changes.addROL(rol);
						}
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
	
	public ROL hydrateResourceROL(ObjectId resourceObjectId, ROL rol) {
		ROL.Builder builder = rol.toBuilder();

		try {			
			ByteString resource = federationHubDatabaseService.getResource(resourceObjectId);
			if (resource != null) {
			    builder.addPayload(BinaryBlob.newBuilder().setData(resource).build());
			}
		} catch (Exception e) {
			logger.error("hydrateResourceROL error", e);
		}	
		
		return builder.build();
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
			
			BasicDBObject federateGroupHopLimits = null;
			if (event.hasFederateGroupHopLimits()) {
				federateGroupHopLimits = new BasicDBObject();
				federateGroupHopLimits.append("use_federate_group_hop_limits", event.getFederateGroupHopLimits().getUseFederateGroupHopLimits());

				List<BasicDBObject> limits = event.getFederateGroupHopLimits().getLimitsList()
						.stream()
						.map(l -> {
							BasicDBObject prov = new BasicDBObject();
							prov.append("max_hops", l.getMaxHops());
							prov.append("current_hops", l.getCurrentHops());
							prov.append("group_name", l.getGroupName());
							return prov;
						}).collect(Collectors.toList());
				
				federateGroupHopLimits.append("limits", limits);
			}
			
			BasicDBObject eventRol = new BasicDBObject();
			eventRol.put("rol_program", event.getProgram());
			eventRol.put("rol_resource", res);
			eventRol.put("rol_operation", op);
			eventRol.put("rol_parameters", rolParameters);
			eventRol.put("federate_groups", event.getFederateGroupsList());
			eventRol.put("federate_provenance", federateProvenances);
			eventRol.put("federate_hops", federateHops);
			eventRol.put("federate_group_hop_limits", federateGroupHopLimits);
			eventRol.put("rol_payload_count", event.getPayloadCount());
			for (int i = 0; i < event.getPayloadCount(); i++) {
				BinaryBlob payload = event.getPayload(i);
				// TODO: for now just assume that if payload description is not empty, that the payload type is a file and we should not store it in the database as bson
				if (payload.getType().equals(BINARY_TYPES.OTHER) && !payload.getDescription().isEmpty()) {
					eventRol.put("rol_payload_" + i, BlobBsonMapper.toBson(payload));
				}
			}
			
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
		Map<ObjectId, ROL> resourceRols = new HashMap<>();
		List<ROL> rols = new ArrayList<>();
		
		public Map<ObjectId, ROL> getResourceRols() {
			return resourceRols;
		}
		
		public List<ROL> getRols() {
			return rols;
		}
		
		public void addResourceROL(ObjectId key, ROL value) {
			resourceRols.put(key, value);
		}
		
		public void addROL(ROL e) {
			rols.add(e);
		}
	}
}
