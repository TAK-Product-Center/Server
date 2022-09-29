package tak.server.federation;

import java.util.HashSet;

public class FederateOutgoing extends FederationNode {

    public FederateOutgoing(FederateIdentity federateIdentity) {
        super(federateIdentity);
    }

    public FederateOutgoing(String nodeName, FederateIdentity federateIdentity) {
        super(nodeName, federateIdentity);
    }

}
