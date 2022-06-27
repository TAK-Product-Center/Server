package tak.server.federation.hub.broker;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"PMD.ExcessivePublicCount", "PMD.GodClass", "PMD.TooManyMethods"})
public class Message {

    private static final Logger logger = LoggerFactory.getLogger(Message.class);

    /* Map of metadata attribute names to their values. */
    private Map<String, Object> metadata = new ConcurrentHashMap<>();

    /* The contents of the message. */
    private Payload<?> payload;

    private AddressableEntity<?> source;

    private Set<AddressableEntity<?>> dests;

    private UUID messageID;

    /*
     * Create message with given metadata and payload.
     *
     * metadata is the map of metadata key/value pairs (if null the
     * map will be initialized to an empty map).
     */
    @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
    public Message(Map<String, Object> metadata, Payload<?> payload) {
        this.messageID = UUID.randomUUID();

        if (metadata == null) {
            this.metadata = new HashMap<>();
        } else {
            this.metadata = metadata;
        }

        setPayload(payload);
    }

    /*
     * Returns the payload of the message. Note that the value
     * returned is a reference, not a copy.
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public Payload<?> getPayload() {
        return payload;
    }

    public void setPayload(Payload<?> payload) {
        this.payload = payload;
    }

    /* The following are getters for the instance variables. */
    public AddressableEntity<?> getSource() {
        return this.source;
    }

    public void setSource(AddressableEntity<?> source) {
        this.source = source;
    }

    /*
     * Returns the actual list of destinations. Any changes you
     * make will be reflected in the list. In other words, this
     * list is NOT read-only.
     */
    public Set<AddressableEntity<?>> getDestinations() {
        if (this.dests != null) {
            return dests;
        }

        this.dests = new SetWrapper<AddressableEntity<?>>(
            AddressableEntity.class.getName(), new HashSet<>());
        return this.dests;
    }

    public Object getMetadataValue(String attributeName) {
        return metadata.get(attributeName);
    }

    public boolean containsMetadataKey(String attributeName) {
        return metadata.containsKey(attributeName);
    }

    /**
     * Allows adding any Object to the metadata.
     * @param attributeName The name of the metadata attribute to set
     * @param value The value of the metadata attribute
     */
    public void setMetadataValue(String attributeName, Object value) {
        this.metadata.put(attributeName, value);
    }

    public UUID getMessageID() {
        return messageID;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        Message message = (Message)obj;
        return this.getMessageID().equals(message.getMessageID());
    }

    @Override
    public int hashCode() {
        return this.getMessageID().hashCode();
    }

    private String getSourceStr() {
        AddressableEntity<?> source = this.getSource();
        return (source == null) ? "From: null" : "From: " + source.toString();
    }

    private String getDestinationsStr() {
        if (this.dests == null) {
            this.dests = new SetWrapper<>(AddressableEntity.class.getName(), new HashSet<>());
        }
        return "To: [" + this.dests.stream().collect(
            StringBuilder::new,
            (stringBuilder, dest) -> {stringBuilder.append(NEWLINE_TAB).append(
                 (dest == null) ? "null" : dest.toString());},
            (StringBuilder::append)) + "\n]";
    }

    private String getMetadataStr() {
        StringBuilder result = new StringBuilder("Metadata: {");
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            result.append("\n\t('");
            result.append(entry.getKey());
            result.append("', ");
            result.append(entry.getValue() == null ? "null" : entry.getValue().toString());
            result.append(')');
        }
        result.append("\n}");
        return result.toString();
    }

    /* Returns a SHA-256 hash of the message payload. */
    private String getPayloadHashStr() {
        if (this.payload == null) {
            return null;
        }

        String result = null;
        try {
            MessageDigest hash = MessageDigest.getInstance("SHA-256");
            hash.update(this.payload.getBytes());
            String lowercase = new String(Hex.encodeHex(hash.digest()));
            result = lowercase.toUpperCase(Locale.getDefault());
        } catch (NoSuchAlgorithmException e) {
            logger.error("Unable to generate message payload hash", e);
        }
        return result;
    }

    private static final String NEWLINE_TAB = "\n\t";

    @Override
    public String toString() {
        StringBuilder sbuilder = new StringBuilder(100);

        sbuilder.append("\n=== BEGIN Message ").append(getMessageID())
        .append(" ===\n\tHEADER:\n\t").append(getSourceStr()).append(NEWLINE_TAB).append(getDestinationsStr())
        .append(NEWLINE_TAB).append(getMetadataStr()).append(NEWLINE_TAB)
        .append("\n\n===\n\nPAYLOAD: (").append(payload.getBytes().length).append(" bytes)\ntype: ")
        .append(payload.getClass().getName()).append('\n')
        .append(getPayloadHashStr())
        .append("\n=== END ===");

        return sbuilder.toString();
    }
}
