package tak.server.federation.hub.broker;

import com.atakmap.Tak.BinaryBlob;
import com.atakmap.Tak.ROL;

import mil.af.rl.rol.value.ResourceDetails;

/*
 *
 * ROGER payload for ROL with binary data
 *
 */
public class ROLPayload implements Payload<ROL> {

    /**
     * Metadata associated with ROL
     */
    private ResourceDetails details;
    private ROL content;

    public ROLPayload() { }

    public ROLPayload(ROL rol) {
        this.content = rol;
    }

    @Override
    public byte[] getBytes() {
        return content.toByteArray();
    }

    @Override
    public void setBytes(byte[] bytes) {
        throw new UnsupportedOperationException("content must be set directly using setContent()");
    }

    @Override
    public ROL getContent() {
        return content;
    }

    @Override
    public void setContent(ROL content) {
        this.content = content;
    }

    @Override
    public String toString() {
        long size = 0;

        for (BinaryBlob payload : content.getPayloadList()) {
            size =+ payload.getData().size();
        }

        return "ROLPayload: serialized size " + content.getSerializedSize() + " total deserialized size: " + size;
    }

    public ResourceDetails getResourceDetails() {
        return details;
    }

    public void setResourceDetails(ResourceDetails details) {
        this.details = details;
    }
}
