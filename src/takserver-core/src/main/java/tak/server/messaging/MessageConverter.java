package tak.server.messaging;

import java.io.IOException;
import java.lang.reflect.Type;
import java.rmi.RemoteException;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;

import gov.tak.cop.proto.v1.Binarypayload;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.oxm.XmlMappingException;

import com.atakmap.Tak.ROL;
import com.bbn.cluster.ClusterGroupDefinition;
import com.bbn.cot.CotParserCreator;
import com.bbn.cot.filter.StreamingEndpointRewriteFilter;
import com.bbn.marti.config.Input;
import com.bbn.marti.remote.ServerInfo;
import com.bbn.marti.remote.exception.NotFoundException;
import com.bbn.marti.remote.exception.TakException;
import com.bbn.marti.remote.groups.Direction;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.remote.groups.Node;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.protobuf.InvalidProtocolBufferException;

import atakmap.commoncommo.protobuf.v1.MessageOuterClass.Message;
import atakmap.commoncommo.protobuf.v1.Missionannouncement.MissionAnnouncement;
import atakmap.commoncommo.protobuf.v1.Takmessage.TakMessage;
import tak.server.Constants;
import tak.server.cluster.ClusterManager.ClusterMissionAnnouncementDetail;
import tak.server.cluster.ClusterMessageWrapper;
import tak.server.cot.CotEventContainer;
import tak.server.cot.CotParser;
import tak.server.ignite.IgniteHolder;
import tak.server.proto.StreamingProtoBufHelper;

/*
 *
 * Message conversion utilities for Protobuf, clustering and internal pub-sub
 *
 */
public class MessageConverter {

	private ThreadLocal<CotParser> cotParser = new ThreadLocal<>();

	@Autowired
	private GroupManager groupManager;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private ServerInfo serverInfo;

	private static final Logger logger = LoggerFactory.getLogger(MessageConverter.class);
	
	// Convert CotEventContainer to proto encoding
	public byte[] cotToDataMessage(CotEventContainer message) {
		return cotToDataMessage(message, false);
	}

	public byte[] cotToDataMessage(CotEventContainer message, boolean padEmptyGroups) {
		return cotToDataMessage(message, padEmptyGroups, serverInfo.getServerId());
	}

	// Convert CotEventContainer to proto encoding
	public static byte[] cotToDataMessage(CotEventContainer message, boolean padEmptyGroups, String serverId) {
		return cotToMessage(message, padEmptyGroups, serverId).toByteArray();
	}

	// Convert CotEventContainer to proto encoding
	public static Message cotToMessage(CotEventContainer message, boolean padEmptyGroups, String serverId) {

		Message.Builder mb = Message.newBuilder();

		// We can revisit if additional context keys are needed.
		//		for (Map.Entry<String, Object> entry : message.getContext().entrySet()) {
		//			clusterMessage.getContext().put(entry.getKey(), gson.toJson(entry.getValue()));
		//		}

		// alternate way to explicitly include only groups
		@SuppressWarnings("unchecked")
		NavigableSet<Group> groups = (NavigableSet<Group>) message.getContext().get(Constants.GROUPS_KEY);
		
		if (!padEmptyGroups && (groups == null || groups.isEmpty())) {
			throw new NotFoundException("not clustering message with no groups");
		}
		
		if (padEmptyGroups && (groups == null || groups.isEmpty())) {
			
			NavigableSet<Group> paddedGroups = new TreeSet<>();
			
			paddedGroups.add(new Group("__ANON__", Direction.IN));
			
			groups = paddedGroups;
			
			message.setContext(Constants.GROUPS_KEY, paddedGroups);
		}

		groups.forEach((group) -> mb.addGroups(group.getName()));

		mb.setSource(serverId);

		mb.setPayload(StreamingProtoBufHelper.cot2protoBuf(message));

		if (message.getBinaryPayloads() != null && !message.getBinaryPayloads().isEmpty()) {
			mb.addAllBloads(message.getBinaryPayloads());
		}

		String clientId = (String)message.getContext().get(Constants.CLIENT_UID_KEY);
		if (clientId != null) {
			mb.setClientId(clientId);
		}

		String connectionId = (String)message.getContextValue(Constants.CONNECTION_ID_KEY);
		if (connectionId != null) {
			mb.setConnectionId(connectionId);
		}

		@SuppressWarnings("unchecked")
		List<String> provenance = (List<String>)message.getContextValue(Constants.PLUGIN_PROVENANCE);
		if (provenance != null) {
		    mb.addAllProvenance(provenance);
		}
		
		Boolean archivedEnabled = (Boolean)message.getContextValue(Constants.ARCHIVE_EVENT_KEY);
		// This is hacky, BUT the default behavior in the rest of TAK Server is that archiving should only be disabled
		// 	iff the archive key is present and set to false. 
		// Therefore, set it enabled by default here, then let it get set to the actual value of the event key if its present
		mb.setArchive(true);
		if (archivedEnabled != null) {
			mb.setArchive(archivedEnabled.booleanValue());
		}

		List<String> destUids = (List<String>) message.getContextValue(StreamingEndpointRewriteFilter.EXPLICIT_UID_KEY);
		if (destUids != null && !destUids.isEmpty()) {
			mb.addAllDestClientUids(destUids);
		}

		List<String> destCallsigns = (List<String>) message.getContextValue(StreamingEndpointRewriteFilter.EXPLICIT_CALLSIGN_KEY);
		if (destCallsigns != null && !destCallsigns.isEmpty()) {
			mb.addAllDestCallsigns(destCallsigns);
		}
		
		String dataFeedUid = (String) message.getContextValue(Constants.DATA_FEED_UUID_KEY);
		if (!Strings.isNullOrEmpty(dataFeedUid)) {
			mb.setFeedUuid(dataFeedUid);
		}

		if (logger.isTraceEnabled()) {
			logger.trace("TAK proto message converted: " + mb);
		}

		return mb.build();
	}


	public ROL controlMessageToRol(byte[] controlMessageBytes) throws InvalidProtocolBufferException {

		if (controlMessageBytes == null || controlMessageBytes.length == 0) {
			throw new IllegalArgumentException("null or empty control message receceived from cluster message broker");
		}

		return ROL.parseFrom(controlMessageBytes);
	}
	
	// Convert MissionAnnouncement announcement message to CoT
	public CotEventContainer getCotFromMissionAnnouncement(MissionAnnouncement missionannouncement) {
		return StreamingProtoBufHelper.proto2cot(missionannouncement.getPayload());
	}
	
	// Convert ClusterMissionAnnouncementDetail to proto encoding
	public byte[] missionAnnouncementToDataMessage(ClusterMissionAnnouncementDetail detail) {

		MissionAnnouncement.Builder mb = MissionAnnouncement.newBuilder();

		if (!Strings.isNullOrEmpty(detail.missionName)) {
			mb.setMissionName(detail.missionName);
		}
		
		if (detail.missionGuid != null) {
			mb.setMissionGuid(detail.missionGuid.toString()); // pass string form through protobuf
		}
		
		if (!Strings.isNullOrEmpty(detail.creatorUid)) {
			mb.setCreatorUid(detail.creatorUid);
		}
		
		if (!Strings.isNullOrEmpty(detail.clientUid)) {
			mb.setClientUid(detail.clientUid);
		}
		
		if (!Strings.isNullOrEmpty(detail.groupVector)) {
			mb.setGroupVector(detail.groupVector);
		}
		
		if (detail.uids != null) {
			for (String uid : detail.uids) 
				mb.addUids(uid);
		}

		mb.setMissionAnnouncementType(detail.missionAnnouncementType);
		mb.setPayload(StreamingProtoBufHelper.cot2protoBuf(detail.cot));

		return mb.build().toByteArray();		
	}
	
	// Convert proto-encoded cluster message to CoT
	public CotEventContainer dataMessageToCot(byte[] dataMessageBytes) throws DocumentException, RemoteException {
		return dataMessageToCot(dataMessageBytes, true);
	}

	// Convert proto-encoded cluster message to CoT
	public CotEventContainer dataMessageToCot(byte[] dataMessageBytes, boolean setClusterKey) throws DocumentException, RemoteException {

		Message m;
		try {
			m = Message.parseFrom(dataMessageBytes);
		} catch (InvalidProtocolBufferException e) {
			throw new TakException(e);
		}

		return dataMessageToCot(m, setClusterKey);
	}
	
	// Convert proto-encoded cluster message to CoT
	public CotEventContainer dataMessageToCot(Message m, boolean setClusterKey) throws DocumentException, RemoteException {

		if (cotParser.get() == null) {
			cotParser.set(CotParserCreator.newInstance());
		}


		TakMessage takMessage = m.getPayload();

		CotEventContainer cot = StreamingProtoBufHelper.proto2cot(takMessage);

		List<Binarypayload.BinaryPayload> binaryPayloads = m.getBloadsList();

		cot.setBinaryPayloads(binaryPayloads);

		NavigableSet<Group> takGroups = new ConcurrentSkipListSet<>();

		m.getGroupsList().forEach((groupName) -> {
			Group takGroup = groupManager.getGroup(groupName, Direction.IN);

			if (takGroup != null) {
				takGroups.add(takGroup);
			}
		});

		cot.setContext(Constants.GROUPS_KEY, takGroups);

		if (setClusterKey) {
			cot.setContext(Constants.CLUSTER_MESSAGE_KEY, m.getSource());
			cot.setContext(Constants.MESSAGING_ARCHIVER, ClusterGroupDefinition.getMessagingClusterDeploymentGroup(IgniteHolder.getInstance().getIgnite())
					.forRandom().node().id().toString());
		}
		
		String feedUid = m.getFeedUuid();
		if (!Strings.isNullOrEmpty(feedUid)) {
			cot.setContext(Constants.DATA_FEED_UUID_KEY, feedUid);
		}

		String clientId = m.getClientId();
		if (clientId != null) {
			cot.setContext(Constants.CLIENT_UID_KEY, clientId);
		}
		
		String connectionId = m.getConnectionId();
		if (connectionId != null) {
			cot.setContext(Constants.CONNECTION_ID_KEY, connectionId);
		}

		List<String> provenance = m.getProvenanceList();
		if (provenance != null) {
			cot.setContextValue(Constants.PLUGIN_PROVENANCE, provenance);
		}
		
		if (provenance != null && provenance.contains(Constants.PLUGIN_MANAGER_PROVENANCE)) {
			cot.setContextValue(Constants.ARCHIVE_EVENT_KEY, m.getArchive());
		}

		List<String> destUids = m.getDestClientUidsList();
		if (destUids != null && !destUids.isEmpty()) {
			cot.setContext(StreamingEndpointRewriteFilter.EXPLICIT_UID_KEY, destUids);
		}

		List<String> destCallsigns = m.getDestCallsignsList();
		if (destCallsigns != null && !destCallsigns.isEmpty()) {
			cot.setContext(StreamingEndpointRewriteFilter.EXPLICIT_CALLSIGN_KEY, destCallsigns);
		}

		if (logger.isTraceEnabled()) {
			logger.trace("CoT from data message: " + cot.asXml());
		}

		return cot;
	}

	// produces a JSON serialized ClusterMessageWrapper that contains a JSON serialized input object
	public String inputObjectToClusterMessage(Input input) throws XmlMappingException, IOException {

	    ClusterMessageWrapper wrapper = new ClusterMessageWrapper();

		wrapper.setSourceNode(serverInfo.getServerId());

	    wrapper.setMessagePayload(mapper.writeValueAsString(input));

	    return getGson().toJson(wrapper);
	}


	// produces a JSON serialized ClusterMessageWrapper that contains just the input name
	public String inputNameToClusterMessage(String inputName) throws XmlMappingException, IOException {

	    ClusterMessageWrapper wrapper = new ClusterMessageWrapper();

		wrapper.setSourceNode(serverInfo.getServerId());

	    wrapper.setMessagePayload(inputName);

	    return getGson().toJson(wrapper);
	}

	public ImmutablePair<String, Input> inputClusterMessageToInput(byte[] inputClusterMessage) throws com.fasterxml.jackson.core.JsonParseException, JsonMappingException, IOException {

		Gson gson = getGson();

		ClusterMessageWrapper clusterMessageWrapper = gson.fromJson(new String(inputClusterMessage, Charsets.UTF_8), ClusterMessageWrapper.class);

		Input input = mapper.readValue(clusterMessageWrapper.getMessagePayload(), Input.class);

		return new ImmutablePair<>(clusterMessageWrapper.getSourceNode(), input);
	}

	public ImmutablePair<String, String> inputNameClusterMessageToString(byte[] inputClusterMessage) throws com.fasterxml.jackson.core.JsonParseException, JsonMappingException, IOException {

		Gson gson = getGson();

		ClusterMessageWrapper clusterMessageWrapper = gson.fromJson(new String(inputClusterMessage, Charsets.UTF_8), ClusterMessageWrapper.class);

		return new ImmutablePair<>(clusterMessageWrapper.getSourceNode(), clusterMessageWrapper.getMessagePayload());
	}

	final class InterfaceAdapter<T> implements JsonSerializer<T>, JsonDeserializer<T> {
		public JsonElement serialize(T object, Type interfaceType, JsonSerializationContext context) {
			final JsonObject wrapper = new JsonObject();
			wrapper.addProperty("type", object.getClass().getName());
			wrapper.add("data", context.serialize(object));
			return wrapper;
		}

		public T deserialize(JsonElement elem, Type interfaceType, JsonDeserializationContext context) throws JsonParseException {
			final JsonObject wrapper = (JsonObject) elem;
			final JsonElement typeName = get(wrapper, "type");
			final JsonElement data = get(wrapper, "data");
			final Type actualType = typeForName(typeName);
			return context.deserialize(data, actualType);
		}

		private Type typeForName(final JsonElement typeElem) {
			try {
				return Class.forName(typeElem.getAsString());
			} catch (ClassNotFoundException e) {
				throw new JsonParseException(e);
			}
		}

		private JsonElement get(final JsonObject wrapper, String memberName) {
			final JsonElement elem = wrapper.get(memberName);
			if (elem == null) {
                throw new JsonParseException("no '" + memberName + "' member found in what was expected to be an interface wrapper");
            }
			return elem;
		}
	}

	private Gson getGson() {
		return new GsonBuilder().registerTypeAdapter(Node.class, new InterfaceAdapter<Node>()).create();
	}

	public Document parseXml(String xmlString) throws DocumentException {

		if (cotParser.get() == null) {
			cotParser.set(CotParserCreator.newInstance());
		}

		return cotParser.get().parse(xmlString);
	}
}
