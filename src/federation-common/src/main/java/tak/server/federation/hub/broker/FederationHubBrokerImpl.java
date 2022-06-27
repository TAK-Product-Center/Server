package tak.server.federation.hub.broker;

import java.io.FileOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import com.google.common.base.Strings;
import org.apache.ignite.services.Service;
import org.apache.ignite.services.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tak.server.federation.hub.FederationHubDependencyInjectionProxy;
import tak.server.federation.hub.policy.FederationHubPolicyManager;

public class FederationHubBrokerImpl implements FederationHubBroker, Service {

    private static final long serialVersionUID = -4468694862348986215L;

    private static final Logger logger = LoggerFactory.getLogger(FederationHubBrokerImpl.class);

    private static String getCN(String dn) throws RuntimeException {
        if (Strings.isNullOrEmpty(dn)) {
            throw new IllegalArgumentException("empty DN");
        }

        try {
            LdapName ldapName = new LdapName(dn);

            for (Rdn rdn : ldapName.getRdns()) {
                if (rdn.getType().equalsIgnoreCase("CN")) {
                    return rdn.getValue().toString();
                }
            }

            throw new RuntimeException("No CN found in DN: " + dn);
        } catch (InvalidNameException e) {
            throw new RuntimeException(e);
        }
    }

    private synchronized void saveTruststoreFile(SSLConfig sslConfig,
            FederationHubServerConfig fedHubConfig) {
        try {

            if (Strings.isNullOrEmpty(fedHubConfig.getTruststorePassword())) {
                throw new IllegalArgumentException("empty or null truststore password ");
            }
            FileOutputStream fos = new FileOutputStream(
                fedHubConfig.getTruststoreFile());
            sslConfig.getTrust().store(fos,
                fedHubConfig.getTruststorePassword().toCharArray());
            fos.close();
            logger.trace("Federation Hub truststore file save complete");
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IllegalArgumentException | IOException e) {
            logger.error("Exception saving Federation Hub truststore file", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void addGroupCa(X509Certificate ca) {
        FederationHubDependencyInjectionProxy depProxy =
            FederationHubDependencyInjectionProxy.getInstance();
        SSLConfig sslConfig = depProxy.sslConfig();
        FederationHubPolicyManager fedHubPolicyManager =
            depProxy.fedHubPolicyManager();
        FederationHubServerConfig fedHubConfig =
            depProxy.fedHubServerConfig();

        try {
            String dn = ca.getSubjectX500Principal().getName();
            String alias = getCN(dn);
            sslConfig.getTrust().setEntry(alias, new KeyStore.TrustedCertificateEntry(ca), null);
            saveTruststoreFile(sslConfig, fedHubConfig);
            sslConfig.refresh();
            FederationHubBrokerUtils.sendCaGroupToFedManager(fedHubPolicyManager, ca);
            depProxy.restartV2Server();
        } catch (KeyStoreException | RuntimeException e) {
            logger.error("Exception adding CA", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void cancel(ServiceContext ctx) {
        if (logger.isDebugEnabled()) {
            logger.debug("cancel() in " + getClass().getName());
        }
    }

    @Override
    public void init(ServiceContext ctx) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("init() in " + getClass().getName());
        }
    }

    @Override
    public void execute(ServiceContext ctx) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("execute() in " + getClass().getName());
        }
    }
}
