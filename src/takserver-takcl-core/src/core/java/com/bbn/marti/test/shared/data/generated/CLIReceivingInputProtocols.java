package com.bbn.marti.test.shared.data.generated;

import com.bbn.marti.test.shared.TestConnectivityState;
import com.bbn.marti.test.shared.data.protocols.ProtocolProfiles;
import com.bbn.marti.test.shared.data.protocols.ProtocolProfilesInterface;

/**
 * Created on 7/21/16.
 */
public enum CLIReceivingInputProtocols {
    //////////////////////////
    // Begin Generated ProtocolProfiles
    ////////////////////////// 68EBC0C6-82C3-4C2F-9779-9191B5B432EC
    mcast("mcast"),
    ssl("ssl"),
    stcp("stcp"),
    tcp("tcp"),
    tls("tls"),
    udp("udp");
    ////////////////////////// BD6A4745-76B4-42DD-AE35-5C2251DD6301
    // End Generated ProtocolProfiles
    //////////////////////////

    private final String protocolProfileIdentifier;
    private ProtocolProfiles protocolProfile;

    CLIReceivingInputProtocols(String protocolProfileIdentifier) {
        this.protocolProfileIdentifier = protocolProfileIdentifier;
    }

    public ProtocolProfiles getProtocolProfile() {
        if (protocolProfile == null) {
            protocolProfile = ProtocolProfiles.getInputByValue(protocolProfileIdentifier);
        }
        return protocolProfile;
    }
}
