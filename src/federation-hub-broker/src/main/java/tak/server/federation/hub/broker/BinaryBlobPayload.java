package tak.server.federation.hub.broker;

import com.atakmap.Tak.BinaryBlob;

public class BinaryBlobPayload implements Payload<BinaryBlob> {

    private BinaryBlob content;

    public BinaryBlobPayload() { }

    public BinaryBlobPayload(BinaryBlob event) {
        this.content = event;
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
    public BinaryBlob getContent() {
        return content;
    }

    @Override
    public void setContent(BinaryBlob content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "BinaryBlobPayload: serialized size " + content.getSerializedSize() + " deserialized size: " + content.getData().size();
    }
}
