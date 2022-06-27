package tak.server.federation.message;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.security.auth.Subject;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"PMD.ExcessivePublicCount", "PMD.GodClass", "PMD.TooManyMethods"})
public class Message implements Comparable<Message> {
	private static final String NEWLINE_TAB = "\n\t";
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Message.class);

	// higher priority = more important
	private static int defaultPriority = 4;

	// Map of metadata attribute names to their values
	private Map<String, Object> metadata = new ConcurrentHashMap<>();
	

	/**
	 * Used within transactions to allow richer metadata objects
	 * to be passed between plugins in one transaction. 
	 * 
	 * Wiped at the end of a transaction
	 * 
	 * Note: regular metadata may need to be serialized and 
	 * therefore further restrictions on what it can contain may
	 * be implemented
	 */
	private Map<String, Object> transactionMetadata = new ConcurrentHashMap<>();
	
	// The contents of the message
	private Payload<?> payload;


	public Message() {
	    this(new ByteArrayPayload());
	}
	
	public Message(Payload<?> payload) {
        this.payload = requireNonNull(payload, "payload");
        this.metadata.put(MetadataConstants.Message_UUID.toString(), UUID.randomUUID());
        this.metadata.put(MetadataConstants.Destinations.toString(), new SetWrapper<AddressableEntity>(AddressableEntity.class.getName(), new HashSet<>()));
        this.metadata.put(MetadataConstants.Priority.toString(), defaultPriority);
        this.metadata.put(MetadataConstants.TimeCreated.toString(), System.currentTimeMillis());
    }

	/**
	 * Copy constructor
	 * copied message has the same timestamp as the message it's cloning
	 * makes a new list for provenance events that contains current message's previous provenance events 
	 * gives the copy of a message a new UUID
	 * @param message message to clone
	 */
	public Message(Message message) {
		this.metadata = message.getMetadataDeepCopy();
		this.transactionMetadata = message.getTransactionMetadata();
		this.metadata.put(MetadataConstants.Message_UUID.toString(), UUID.randomUUID());
		this.payload = message.getPayload();
	}

	/**
	 * <p>Creates a complete copy of the message, but replaces destination list with a list containing
	 * only the given destination.</p>
	 * <p>The provenance information is not copied, and we expect only the Network Communication Service
	 * to use this constructor. When examining provenance, we will see that the message was processed
	 * by a variety of different Network Communication Plugins (even if the copy was only processed by one of those
	 * plugins), and we will know which plugin processed the message via the destination on the message.</p>
	 * @param message the message to copy
	 * @param destination the destination to use
	 */
	public Message(Message message, AddressableEntity<?> destination) {
		this.metadata = message.getMetadataDeepCopy();
		this.transactionMetadata = message.getTransactionMetadata();
		this.payload = message.getPayload();
		
		Set<AddressableEntity<?>> dests = new SetWrapper<>(AddressableEntity.class.getName(), new HashSet<>());
		dests.add(destination);
		this.metadata.put(MetadataConstants.Destinations.toString(), dests);
	}

	/**
	 * <p>Creates a complete copy of the message with the given list of destinations.</p>
	 *
	 * <p>The provenance information is not copied, and we expect only the Network Communication Service
	 * to use this constructor. When examining provenance, we will see that the message was processed
	 * by a variety of different Network Communication Plugins (even if the copy was only processed by one of those
	 * plugins), and we will know which plugin processed the message via the destination on the message.</p>
	 * @param message the message to copy
	 * @param destinations the list of destinations
	 */
	public Message(Message message, Set<AddressableEntity<?>> destinations) {
		this.metadata = message.getMetadataDeepCopy();
		this.transactionMetadata = message.getTransactionMetadata();
		this.payload = message.getPayload();

		Set<AddressableEntity<?>> dests = new SetWrapper<>(AddressableEntity.class.getName(), new HashSet<>());
		dests.addAll(destinations);
		this.metadata.put(MetadataConstants.Destinations.toString(), dests);
	}
	
	/**
	 * Create message with given metadata and payload bytes.
	 * 
	 * @param metadata The map of metadata key/valyue pairs (if null the map will be initialized to an empty map)
	 * @param bytes The payload bytes of the message. Note that the payload is used directly, not copied.
	 */
	@SuppressWarnings("PMD.ArrayIsStoredDirectly")
	public Message(Map<String, Object> metadata, byte[] bytes) {
		this(metadata, new ByteArrayPayload(bytes));
	}
	
	/**
	 * Create message with given metadata and payload.
	 * 
	 * @param metadata The map of metadata key/valyue pairs (if null the map will be initialized to an empty map)
	 * @param payload The payload of the message. Note that the payload is used directly, not copied.
	 */
	@SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
	public Message(Map<String, Object> metadata, Payload<?> payload) {
		this(metadata);
		setPayload(payload);
	}
	
	/**
	 * Create message with given metadata and an empty byte array payload.
	 * 
	 * @param metadata The map of metadata key/valyue pairs (if null the map will be initialized to an empty map)
	 */
	public Message(Map<String, Object> metadata) {
		if (metadata == null) { // some tests supply no metadata, yet call this ctor
			this.metadata = new HashMap<>();
		} else {
			this.metadata = metadata;
		}
		
		setPayload(new ByteArrayPayload());
		this.transactionMetadata = new HashMap<>();
		if (!this.metadata.containsKey(MetadataConstants.Message_UUID.toString())){
			this.metadata.put(MetadataConstants.Message_UUID.toString(), UUID.randomUUID());
		}
		if (!this.metadata.containsKey(MetadataConstants.Priority.toString())){
			this.metadata.put(MetadataConstants.Priority.toString(), defaultPriority);
		}
		if (!this.metadata.containsKey(MetadataConstants.Destinations.toString())){
			this.metadata.put(MetadataConstants.Destinations.toString(), new SetWrapper<AddressableEntity<?>>(AddressableEntity.class.getName(), new HashSet<>()));
		}	
		if (!this.metadata.containsKey(MetadataConstants.TimeCreated.toString())){
			this.metadata.put(MetadataConstants.TimeCreated.toString(), System.currentTimeMillis());
		}
	}
	
	/**
	 * Creates and returns a copy of the parent message with a new ID and a pointer to 
	 * the parent's ID in the metadata. 
	 * After using this method, the caller should consider<br>
	 * <ul>
	 * <li>updating the child message's payload</li>
	 * <li>updating the child message's MimeType</li>
	 * <li>If the child messages may be aggregated later, certain combiners require that MedatadaConstants.NumChildrenCreated and 
	 * MedatadaConstants.ChildMessageIndex are set.</li>
	 * <li>This method does not set the parent metadata (see MetadataConstants), because it is expected to be a hashmap. 
	 * If multiple layers of splitting and combining are occurring in process, the caller should set parentMetadata.</li>
	 * </ul>
	 */
	public Message createChildMessage() {
		Message childMessage = new Message(this.getMetadataDeepCopy());
		childMessage.setMessageID(UUID.randomUUID()); // child Message gets its own ID
		childMessage.setMetadataValue(MetadataConstants.ParentMessageID, this.getMessageID().toString());
		childMessage.transactionMetadata = this.getTransactionMetadata();
		return childMessage;
	}

	// The following are getters for the instance variables:
	public FederationConnection getSource() {
		return (FederationConnection) this.metadata.get(MetadataConstants.Source.toString());
	}

	public String getCertPath() {
		return (String) this.metadata.get(MetadataConstants.CertPath.toString());
	}

	/**
	 * Returns the actual list of destinations. Any changes you make will be reflected in the list.
	 * In other words, this list is NOT read-only.
	 */
	@SuppressWarnings("unchecked")
	public Set<AddressableEntity<?>> getDestinations() {
		Set<AddressableEntity<?>> dests = (Set<AddressableEntity<?>>) this.metadata.get(MetadataConstants.Destinations.toString());
		if (dests != null){
			return dests;
		} else {
			return new SetWrapper<AddressableEntity<?>>(AddressableEntity.class.getName(), new HashSet<>());
		}
	}

	/**
	 * Convenience method for adding URIs as destinations.
	 * @param uri the endpoint to which this message should be sent
	 */
	public boolean addDestination(URI uri) {
		AddressableEntity<URI> destination = new AddressableEntity<URI>(uri);
		return addDestination(destination);
	}

	@SuppressWarnings("unchecked")
	public boolean addDestination(AddressableEntity<?> destination) {
		Set<AddressableEntity<?>> dests = (Set<AddressableEntity<?>>) this.metadata.get(MetadataConstants.Destinations.toString());
		return dests.add(destination);
	}

	@SuppressWarnings("unchecked")
	public boolean addAllDestinations(Set<AddressableEntity<?>> destinations) {
		Set<AddressableEntity<?>> originalDestinations = (Set<AddressableEntity<?>>) this.metadata.get(MetadataConstants.Destinations.toString());
		return originalDestinations.addAll(destinations);
	}

	/**
	 * Convenience method for removing URIs as destinations.
	 * @param uri the endpoint to remove from the list of destinations
	 */
	public boolean removeDestination(URI uri) {
		AddressableEntity<URI> destination = new AddressableEntity<URI>(uri);
		return removeDestination(destination);
	}

	@SuppressWarnings("unchecked")
	public boolean removeDestination(AddressableEntity<?> destination) {
		Set<AddressableEntity<?>> dests = (Set<AddressableEntity<?>>) this.metadata.get(MetadataConstants.Destinations.toString());
		return dests.remove(destination);
	}

	@SuppressWarnings("unchecked")
	public void clearDestinations() {
		Set<AddressableEntity<?>> dests = (Set<AddressableEntity<?>>) this.metadata.get(MetadataConstants.Destinations.toString());
		dests.clear();
	}

	public void setDestinations(Set<AddressableEntity<?>> destinations) {
	    SetWrapper<AddressableEntity<?>> destinationsSetWrapper;
	    if (destinations instanceof SetWrapper) {
	        destinationsSetWrapper = (SetWrapper<AddressableEntity<?>>) destinations;
	    }else {
	        destinationsSetWrapper = new SetWrapper<AddressableEntity<?>>(AddressableEntity.class.getName(), destinations);
	    }
		this.metadata.replace(MetadataConstants.Destinations.toString(), destinationsSetWrapper);
	}
	
	public Object getMetadata(String attributeName) {
		return metadata.get(attributeName);
	}
	
	public boolean containsMetadata(String attributeName) {
		return metadata.containsKey(attributeName);
	}
	
	public Set<String> getMetadataKeys() {
		return metadata.keySet();
	}
	
	public Map<String, Object> getMetadataReadOnly() {
		return new HashMap<>(metadata);
	}
	
	public Object removeMetadata(String key){
		return metadata.remove(key);
	}
	
	public Map<String, Object> getMetadataDeepCopy() {
		Map<String, Object> deepCopy = new HashMap<>();
		for (Entry<String, Object> entry : metadata.entrySet()){
			String key = entry.getKey();
			Object value = entry.getValue();
			try {
				deepCopy.put(key, (value==null)? null : MetadataUtils.deepClone(value));
			} catch (ClassNotFoundException | IOException e) {
				if (LOGGER.isDebugEnabled()) {
				    LOGGER.debug("Unable to make deep copy of object {}, for metadata key {}, "
						+ "due to error {}. Using shallow copy of this object.", 
						value, key, e.getLocalizedMessage());
				}
				deepCopy.put(key, value);
			}

		}
		return deepCopy;
	}

	/**
	 * Returns the payload of the message. Note that the value returned is a reference, not a copy.
	 * @return The payload of the message
	 */
	@SuppressWarnings("PMD.MethodReturnsInternalArray")
	public Payload<?> getPayload() {
		return payload;
	}
	
	public int getPriority() {
		return (int) this.metadata.get(MetadataConstants.Priority.toString()); 
	}

	public UUID getIFPnodeID(){
		return (UUID) this.metadata.get(MetadataConstants.NEXT_IFP_NODE.toString());
	}
	
	public UUID getMessageID(){
		return (UUID) this.metadata.get(MetadataConstants.Message_UUID.toString());
	}

	public Subject getSubject() {
		return (Subject) metadata.get(MetadataConstants.Subject.toString());
	}

	/**
	 * Plug-ins SHOULD NOT CALL THIS METHOD
	 * @param nodeID
	 */
	public void setIFPnodeID(UUID nodeID){
		this.metadata.put(MetadataConstants.NEXT_IFP_NODE.toString(), nodeID);
	}

	public Map<String, Object> getTransactionMetadata() {
		return transactionMetadata;
	}

	// The following are setters for the instance variables:
	
	public void setMessageID(UUID uuid){
		this.metadata.put(MetadataConstants.Message_UUID.toString(), uuid);
	}

	public void setSource(URI rogerUri, URI peerUri) {
		setSource(new Object());
	}


	public void setSource(AddressableEntity<?> rogerID, AddressableEntity<?> peerID) {
		setSource(new Object());
	}

	public void setSource(Object src) {
		this.metadata.put(MetadataConstants.Source.toString(), src);
	}

	public void setCertPath(String certPath) {
		this.metadata.put(MetadataConstants.CertPath.toString(), certPath);
	}
	
	public void setTransactionMetadata(Map<String, Object> transactionMetadata) {
		this.transactionMetadata = transactionMetadata;
	}

	public void setMetadataValue(String attributeName, String value) {
		this.metadata.put(attributeName, value);
	}
	
	public void setMetadataValue(String attributeName, String... value) {
		this.metadata.put(attributeName, value);
	}
	
	public void setMetadataValue(String attributeName, Number value) {
		this.metadata.put(attributeName, value);
	}
	
	public void setMetadataValue(String attributeName, Number... value) {
		this.metadata.put(attributeName, value);
	}
	
	public void setMetadataValue(String attributeName, Boolean value) {
		this.metadata.put(attributeName, value);
	}
	
	public void setMetadataValue(String attributeName, Boolean... value) {
		this.metadata.put(attributeName, value);
	}
	
	public void setMetadataValue(String attributeName, byte value) {
		this.metadata.put(attributeName, value);
	}
	
	public void setMetadataValue(String attributeName, byte... value) {
		this.metadata.put(attributeName, value);
	}
	
	public void setMetadataValue(String attributeName, URI value) {
		this.metadata.put(attributeName, value);
	}
	
	public void setMetadataValue(String attributeName, URI... value) {
		this.metadata.put(attributeName, value);
	}

	public void setMetadataValue(String attributeName, Date value) {
		this.metadata.put(attributeName, value);
	}
	
	/**
	 * Allows adding any Object to the metadata.
	 * @param attributeName The name of the metadata attribute to set
	 * @param value The value of the metadata attribute
	 */
	public void setMetadataValue(String attributeName, Object value) {
		this.metadata.put(attributeName, value);
	}

	public void setSubject(Subject subject) {
		this.metadata.put(MetadataConstants.Subject.toString(), subject);
	}

	/**
	 * Sets the given payload as the payload of this message. Note that for efficiency the payload is used directly - a copy is not made.
	 * @param payload
	 */
	public void setPayload(Payload<?> payload) {
		this.payload = payload;
	}
	
	public void setPriority(int priority) {
		this.metadata.put(MetadataConstants.Priority.toString(), priority);
	}
	
	public void clearTransactionMetadata() {
		this.transactionMetadata.clear();
	}
	
	/**
	 * Compares the priority of this message to Message other
	 * @return negative integer if this is lower priority than other
	 * 		   positive integer if this is higher priority than other
	 * 		   the message Ids are compared for two different messages with equal priority
	 */
	@Override
	public int compareTo(Message other) {
		int priorityDifference = other.getPriority() - this.getPriority();
		if (priorityDifference == 0){
			return this.getMessageID().compareTo(other.getMessageID());
		}
		return priorityDifference;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}

		Message message = (Message) obj;
		return this.getMessageID().equals(message.getMessageID());
	}

	@Override
	public int hashCode() {
		return this.getMessageID().hashCode();
	}

	@SuppressWarnings("unchecked")
	public String printDestinations() {
		Set<AddressableEntity<?>> dests = (Set<AddressableEntity<?>>) this.metadata.get(MetadataConstants.Destinations.toString());
		if (dests == null) {
		    dests = new SetWrapper<>(AddressableEntity.class.getName(), new HashSet<>());
		    this.metadata.put(MetadataConstants.Destinations, dests);
		}
		return "To: [" + dests.stream().collect(
				StringBuilder::new,
				(stringBuilder, rogerConnection) -> {stringBuilder.append(NEWLINE_TAB).append(
				         (rogerConnection == null) ? "null" : rogerConnection.toString());},
				(StringBuilder::append)) + "\n]";
	}

	public String printMetadata() {
		StringBuilder result = new StringBuilder("Metadata: {");
		for (Map.Entry<String, Object> entry : metadata.entrySet()) {
			result.append("\n\t('");
			result.append(entry.getKey());
			result.append("', ");
			if (entry.getKey().equals(MetadataConstants.HttpHeaders.toString())) {
				String [] headers = (String []) entry.getValue();
				for (String header : headers) {
					result.append(header).append(", ");
				}
			} if (entry.getKey().equals(MetadataConstants.Subject.toString())) {
				((Subject)entry.getValue()).getPrincipals().stream().forEach(prc -> result.append(prc.getName()).append(","));
			} else {
				result.append(entry.getValue() == null ? "null" : entry.getValue().toString());
			}
			result.append(')');
		}
		result.append("\n}");
		return result.toString();
	}
	
	public String printIFPnodeID() {
		StringBuilder result = new StringBuilder("IFP Node: ");
		result.append(getIFPnodeID());
		return result.toString();
	}

	/**
	 * Returns a SHA-256 hash of the message payload
	 */
	public String printPayloadHash() {
		if (this.payload == null) {
			return null;
		}
		String result = null;
		try {
			MessageDigest hash  = MessageDigest.getInstance("SHA-256");
			hash.update(this.payload.getBytes());
			String lowercase = new String(Hex.encodeHex(hash.digest()));
			result = lowercase.toUpperCase(Locale.getDefault());
		} catch (NoSuchAlgorithmException e) {
			System.err.println("Unable to generate message payload hash: " + e);
		}
		return result;
	}

	@Override
	public String toString() {
		StringBuilder sbuilder = new StringBuilder(100);
		
		sbuilder.append("\n=== BEGIN Message ").append(getMessageID())
		.append(NEWLINE_TAB).append(printIFPnodeID()).append(NEWLINE_TAB).append(printMetadata()).append(NEWLINE_TAB)
		.append(payload.getClass().getName()).append('\n')
		.append(printPayloadHash())
		.append("\n=== END ===");
		
		return sbuilder.toString();
	}
	
}
