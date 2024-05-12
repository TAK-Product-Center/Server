package tak.server.federation.hub.broker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.apache.commons.io.FileUtils;
import org.apache.ignite.services.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.roger.fig.FederationUtils;
import com.google.common.base.Strings;

import tak.server.federation.Federate;
import tak.server.federation.FederateGroup;
import tak.server.federation.FederateIdentity;
import tak.server.federation.FederateOutgoing;
import tak.server.federation.FederationNode;
import tak.server.federation.FederationPolicyGraph;
import tak.server.federation.hub.FederationHubDependencyInjectionProxy;
import tak.server.federation.hub.broker.events.ForceDisconnectEvent;
import tak.server.federation.hub.broker.events.UpdatePolicy;
import tak.server.federation.hub.policy.FederationHubPolicyManager;
import tak.server.federation.hub.ui.graph.FederationOutgoingCell;
import tak.server.federation.hub.ui.graph.FederationPolicyModel;
import tak.server.federation.hub.ui.graph.PolicyObjectCell;

public class FederationHubBrokerImpl implements FederationHubBroker, Service {

    private static final long serialVersionUID = -4468694862348986215L;

    private static final Logger logger = LoggerFactory.getLogger(FederationHubBrokerImpl.class);

    public static String getCN(String dn) throws RuntimeException {
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
            String alias = FederationUtils.getBytesSHA256(ca.getEncoded());

            sslConfig.getTrust().setEntry(alias, new KeyStore.TrustedCertificateEntry(ca), null);
            saveTruststoreFile(sslConfig, fedHubConfig);
            sslConfig.refresh();
            FederationHubBrokerUtils.sendCaGroupToFedManager(fedHubPolicyManager, ca);
            depProxy.restartV2Server();
        } catch (KeyStoreException | RuntimeException | CertificateEncodingException e) {
            logger.error("Exception adding CA", e);
            throw new RuntimeException(e);
        }
    }

    @Override
	public Map<String, X509Certificate> getCAsFromFile() {
    	Map<String, X509Certificate> cas = new HashMap<>();

		FederationHubDependencyInjectionProxy depProxy = FederationHubDependencyInjectionProxy.getInstance();
		SSLConfig sslConfig = depProxy.sslConfig();

		try {
			for (Enumeration<String> e = sslConfig.getTrust().aliases(); e.hasMoreElements();) {
				String alias = e.nextElement();
				X509Certificate cert = (X509Certificate) sslConfig.getTrust().getCertificate(alias);
				String issuerName = cert.getIssuerX500Principal().getName();
				String groupName = issuerName + "-" + FederationUtils.getBytesSHA256(cert.getEncoded());

				cas.put(groupName, cert);
			}
		} catch (Exception e) {
			logger.error("Exception deleteing CA", e);
		}

		return cas;
	}

	@Override
	public void deleteGroupCa(String groupId) {
		FederationHubDependencyInjectionProxy depProxy = FederationHubDependencyInjectionProxy.getInstance();
		FederationHubPolicyManager fedHubPolicyManager = depProxy.fedHubPolicyManager();
		SSLConfig sslConfig = depProxy.sslConfig();
		FederationHubServerConfig fedHubConfig = depProxy.fedHubServerConfig();

		try {
			for (Enumeration<String> e = sslConfig.getTrust().aliases(); e.hasMoreElements();) {
				String alias = e.nextElement();
				X509Certificate cert = (X509Certificate) sslConfig.getTrust().getCertificate(alias);
				String issuerName = cert.getIssuerX500Principal().getName();
				String groupName = issuerName + "-" + FederationUtils.getBytesSHA256(cert.getEncoded());

				if (groupName.equals(groupId)) {
					FederateGroup group = new FederateGroup(new FederateIdentity(groupId));
					fedHubPolicyManager.removeCaGroup(group);

					sslConfig.getTrust().deleteEntry(alias);
					saveTruststoreFile(sslConfig, fedHubConfig);
					sslConfig.refresh();

					depProxy.restartV2Server();
				}
			}
		} catch (Exception e) {
			logger.error("Exception deleteing CA", e);
		}
	}
	
	@Override
	public byte[] getSelfCaFile() {
		FederationHubDependencyInjectionProxy depProxy = FederationHubDependencyInjectionProxy.getInstance();
		FederationHubServerConfig fedHubConfig = depProxy.fedHubServerConfig();
		String caFilePath = fedHubConfig.getCaFile();
		
		try {
		    byte[] contents = FileUtils.readFileToByteArray(new File(caFilePath));
		    return contents;
		} catch (Exception e) {
			logger.error("Exception loading caFile location from " + fedHubConfig.getCaFile() 
				+ ". Ensure caFile in the federation-hub-broker.yml is set to a valid path." , e);
			return null;
		}
	}

    @Override
    public void cancel() {
        if (logger.isDebugEnabled()) {
            logger.debug("cancel() in " + getClass().getName());
        }
    }

    @Override
    public void init() throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("init() in " + getClass().getName());
        }
    }

    @Override
    public void execute() throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("execute() in " + getClass().getName());
        }
    }

	@Override
	public void updatePolicy(FederationPolicyModel federationPolicyModel) {
		Collection<PolicyObjectCell> cells = federationPolicyModel.getCells();

		if (cells != null) {
			List<FederationOutgoingCell> outgoings = new ArrayList<>();
			cells.stream().filter(cell -> cell instanceof FederationOutgoingCell).forEach( cell -> {
				outgoings.add((FederationOutgoingCell) cell);
	        });

			FederationHubDependencyInjectionProxy.getSpringContext().publishEvent(new UpdatePolicy(this, outgoings));
		}
	}

	@Override
	public List<HubConnectionInfo> getActiveConnections() {

		return FederationHubDependencyInjectionProxy.getInstance()
				.hubConnectionStore()
				.getConnectionInfos()
				.stream()
				.collect(Collectors.toList());
	}

	@Override
	public FederationHubBrokerMetrics getFederationHubBrokerMetrics() {

		return FederationHubDependencyInjectionProxy.getInstance()
				.federationHubBrokerMetrics();
	}

	@Override
	public List<String> getGroupsForNode(String federateId) {
		FederationPolicyGraph policyGraph = FederationHubDependencyInjectionProxy.getInstance()
				.fedHubPolicyManager()
				.getPolicyGraph();

		FederationNode sourceNode = policyGraph.getNode(federateId);

		if (sourceNode != null) {
			if (sourceNode instanceof Federate || sourceNode instanceof FederateOutgoing) {
				List<String> groups = FederationHubDependencyInjectionProxy.getInstance()
						.hubConnectionStore()
						.getClientGroupStreamMap()
						.entrySet()
						.stream()
						.filter(e -> sourceNode.getFederateIdentity().getFedId().equals(e.getValue().getFederateIdentity().getFedId()))
						.map(e -> e.getKey())
						.map(sessionIdentifier -> FederationHubDependencyInjectionProxy.getInstance().hubConnectionStore().getClientToGroupsMap().get(sessionIdentifier))
						.filter(g -> g != null)
						.flatMap(g -> g.getFederateGroupsList().stream())
						.distinct()
						.collect(Collectors.toList());

					return groups;
			}

			if (sourceNode instanceof FederateGroup) {
				FederateGroup sourceFederateGroup = (FederateGroup) policyGraph.getNode(federateId);

				Set<String> federateIdentitiesInGroup = sourceFederateGroup.getFederatesInGroup()
						.stream()
						.map(f -> f.getFederateIdentity().getFedId())
						.collect(Collectors.toSet());

				List<String> groups = FederationHubDependencyInjectionProxy.getInstance()
						.hubConnectionStore()
						.getClientGroupStreamMap()
						.entrySet()
						.stream()
						.filter(e -> federateIdentitiesInGroup.contains(e.getValue().getFederateIdentity().getFedId()))
						.map(e -> e.getKey())
						.map(sessionIdentifier -> FederationHubDependencyInjectionProxy.getInstance().hubConnectionStore().getClientToGroupsMap().get(sessionIdentifier))
						.filter(g -> g != null)
						.flatMap(g -> g.getFederateGroupsList().stream())
						.distinct()
						.collect(Collectors.toList());

					return groups;
			}
		}

		return new ArrayList<String>();
	}

	@Override
	public void disconnectFederate(String connectionId) {
		FederationHubDependencyInjectionProxy.getSpringContext().publishEvent(new ForceDisconnectEvent(this, connectionId));
	}
}