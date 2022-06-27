package tak.server.federation.hub.broker;

import com.atakmap.Tak.FederatedEvent;

public class FederatedEventPayload implements Payload<FederatedEvent> {

    private FederatedEvent content;

    public FederatedEventPayload() { }

    public FederatedEventPayload(FederatedEvent event) {
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
    public FederatedEvent getContent() {
        return content;
    }

    @Override
    public void setContent(FederatedEvent content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "FederatedEventPayload: " + content;
    }
}
