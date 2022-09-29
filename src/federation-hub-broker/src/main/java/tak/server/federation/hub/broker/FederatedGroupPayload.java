package tak.server.federation.hub.broker;

import com.atakmap.Tak.FederateGroups;

public class FederatedGroupPayload implements Payload<FederateGroups> {

    private FederateGroups content;

    public FederatedGroupPayload() { }

    public FederatedGroupPayload(FederateGroups event) {
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
    public FederateGroups getContent() {
        return content;
    }

    @Override
    public void setContent(FederateGroups content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "FederateGroups: " + content;
    }
}
