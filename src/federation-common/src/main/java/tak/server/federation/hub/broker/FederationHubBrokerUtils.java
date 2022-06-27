package tak.server.federation.hub.broker;

import com.bbn.roger.fig.FederationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tak.server.federation.FederateGroup;
import tak.server.federation.FederateIdentity;
import tak.server.federation.hub.policy.FederationHubPolicyManager;

import java.security.KeyStoreException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

public class FederationHubBrokerUtils {

    private static final Logger logger = LoggerFactory.getLogger(FederationHubBrokerUtils.class);

    public static void sendCaGroupToFedManager(FederationHubPolicyManager fedHubPolicyManager,
            X509Certificate cert) throws KeyStoreException {
        try {
            String issuerName = cert.getIssuerX500Principal().getName();
            String groupName = issuerName + "-" + FederationUtils.getBytesSHA256(cert.getEncoded());
            FederateGroup group = new FederateGroup(new FederateIdentity(groupName));
            fedHubPolicyManager.addCaGroup(group);
        } catch (CertificateEncodingException cee) {
            logger.error("Could not encode certificate", cee);
        }
    }
}
