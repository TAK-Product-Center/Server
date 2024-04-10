package com.bbn.marti.tests;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;
import java.util.UUID;

import javax.net.ssl.TrustManagerFactory;

import com.bbn.marti.takcl.connectivity.server.ServerProcessConfiguration;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.bbn.marti.takcl.SSLHelper;
import com.bbn.marti.takcl.connectivity.server.AbstractRunnableServer;
import com.bbn.marti.takcl.connectivity.server.ServerProcessDefinition;
import com.bbn.marti.test.shared.AbstractTestClass;
import com.bbn.marti.test.shared.data.generated.ImmutableUsers;
import com.bbn.marti.test.shared.data.servers.ImmutableServerProfiles;
import com.bbn.marti.test.shared.engines.ActionEngine;
import com.bbn.marti.test.shared.engines.TestEngine;
import com.bbn.roger.fig.FederationUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import tak.server.federation.hub.broker.FederationHubServerConfig;
import tak.server.federation.hub.ui.graph.EdgeCell;
import tak.server.federation.hub.ui.graph.EdgeProperties;
import tak.server.federation.hub.ui.graph.FederateOutgoingProperties;
import tak.server.federation.hub.ui.graph.FederationOutgoingCell;
import tak.server.federation.hub.ui.graph.FederationPolicyModel;
import tak.server.federation.hub.ui.graph.FilterNode;
import tak.server.federation.hub.ui.graph.GroupCell;
import tak.server.federation.hub.ui.graph.GroupProperties;
import tak.server.federation.hub.ui.graph.PolicyObjectCell;

public class FedHubTests extends AbstractTestClass {
	private static final ImmutableServerProfiles[] testServers = new ImmutableServerProfiles[]{ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.SERVER_1, ImmutableServerProfiles.SERVER_2, ImmutableServerProfiles.FEDHUB_0, ImmutableServerProfiles.FEDHUB_1};

	
	@BeforeClass
	public static void setup() {
		try {
			SSLHelper.genCertsIfNecessary();
			if (engine != null) {
				engine.engineFactoryReset();
			}
			AbstractRunnableServer.setLogDirectory(TEST_ARTIFACT_DIRECTORY);
			com.bbn.marti.takcl.TestLogger.setFileLogging(TEST_ARTIFACT_DIRECTORY);
			engine = new TestEngine(defaultServerProfile);
		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace(System.err);
			throw new RuntimeException(e);
		}
		// Federate things tend to take a little longer to propagate...
		engine.setSleepMultiplier(4.0);
		engine.setSendValidationDelayMultiplier(20);
	}
	
	private FederationHubServerConfig getFederationHubConfig() {
		FederationHubServerConfig fedBrokerConfig = null;
		try {
			String DEFAULT_FEDERATION_BROKER_CONFIG_FILE = ImmutableServerProfiles.FEDHUB_0.getServerPath() + "/federation-hub/configs/federation-hub-broker.yml";

			fedBrokerConfig = new ObjectMapper(new YAMLFactory()).readValue(new FileInputStream(DEFAULT_FEDERATION_BROKER_CONFIG_FILE), FederationHubServerConfig.class);
						
			fedBrokerConfig.setKeystoreFile("/opt/tak/TEST_RESULTS/TEST_CERTS/" + ImmutableServerProfiles.FEDHUB_0.getConsistentUniqueReadableIdentifier() + ".jks");
			fedBrokerConfig.setTruststoreFile("/opt/tak/TEST_RESULTS/TEST_CERTS/fed-truststore.jks");
			fedBrokerConfig.setV1Port(ImmutableServerProfiles.FEDHUB_0.getFederationV1ServerPort());
			fedBrokerConfig.setV2Port(ImmutableServerProfiles.FEDHUB_0.getFederationV2ServerPort());
			fedBrokerConfig.setId(ImmutableServerProfiles.FEDHUB_0.getConsistentUniqueReadableIdentifier());
			
			ObjectMapper om = new ObjectMapper(new YAMLFactory());
	    	om.writeValue(new File(DEFAULT_FEDERATION_BROKER_CONFIG_FILE), fedBrokerConfig);
	    	
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		return fedBrokerConfig;
	}
	
	private String getHubCAGroupName(FederationHubServerConfig fedBrokerConfig) {
		KeyStore trust = null;
		try {
			TrustManagerFactory trustMgrFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			trust = KeyStore.getInstance(fedBrokerConfig.getTruststoreType());
	        trust.load(new FileInputStream(fedBrokerConfig.getTruststoreFile()), fedBrokerConfig.getTruststorePassword().toCharArray());
	        trustMgrFactory.init(trust);
	        
			for (Enumeration<String> e = trust.aliases(); e.hasMoreElements();) {
	            String alias = e.nextElement();
	            X509Certificate cert = (X509Certificate)trust.getCertificate(alias);
	            String issuerName = cert.getIssuerX500Principal().getName();
	            String groupName = issuerName + "-" + FederationUtils.getBytesSHA256(cert.getEncoded());
	        }
			
			CertificateFactory factory = CertificateFactory.getInstance("X.509");
	        X509Certificate ca = (X509Certificate)factory.generateCertificate(new FileInputStream(new File("/opt/tak/TEST_RESULTS/TEST_CERTS/ca.pem")));
	        String issuerName = ca.getIssuerX500Principal().getName();
	        return issuerName + "-" + FederationUtils.getBytesSHA256(ca.getEncoded());
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		return null;
	}
	
	private void createPolicyForHubCA(FederationHubServerConfig fedBrokerConfig) {
		// get the ca name from our hub cert
		String caGroupName = getHubCAGroupName(fedBrokerConfig);

		FederationPolicyModel policyModel = new FederationPolicyModel();
		GroupCell group1Cell = createGroupCell(caGroupName, true);

		Collection<PolicyObjectCell> cells = new ArrayList<>();
		cells.add(group1Cell);

		policyModel.setCells(cells);
		policyModel.setName(this.getClass().getName());
		
		String DEFAULT_FEDERATION_POLICY_CONFIG_FILE = ImmutableServerProfiles.FEDHUB_0.getServerPath() + "/federation-hub/ui_generated_policy.json";
		ObjectMapper mapper = new ObjectMapper();
		try {
			policyModel.toString();
			policyModel.getFederationPolicyObjectFromModel();
			mapper.writerWithDefaultPrettyPrinter().writeValue(new File(DEFAULT_FEDERATION_POLICY_CONFIG_FILE), policyModel.getFederationPolicyObjectFromModel());
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Could not write policy to file " + e.getStackTrace());
		}
	}

	@Test(timeout = 920000)
	public void basicFedHubTest() {
		try {
			String sessionIdentifier = initTestMethod();
			
			// initial setup of the hub without actually starting it
			engine.overrideDefaultProcessConfiguration(ImmutableServerProfiles.FEDHUB_0, ServerProcessConfiguration.FedhubBrokerFedhubPolicy);

			// get fedhub broker config
			FederationHubServerConfig fedBrokerConfig = getFederationHubConfig();
			// create policy for allowing connections from the hub's ca (all servers will have same CA, aka interconnected)
			createPolicyForHubCA(fedBrokerConfig);
			
			// start fedhub
			engine.startServer(ImmutableServerProfiles.FEDHUB_0, sessionIdentifier);
			
			engine.offlineFederateServers(false, true, ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.SERVER_1);
			engine.offlineAddOutboundFederateConnection(true, ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.FEDHUB_0);
			engine.offlineAddOutboundFederateConnection(true, ImmutableServerProfiles.SERVER_1, ImmutableServerProfiles.FEDHUB_0);
			engine.offlineAddFederate(ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.FEDHUB_0);
			engine.offlineAddFederate(ImmutableServerProfiles.SERVER_1, ImmutableServerProfiles.FEDHUB_0);
			engine.offlineAddOutboundFederateGroup(ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.FEDHUB_0, "group0");
			engine.offlineAddOutboundFederateGroup(ImmutableServerProfiles.SERVER_1, ImmutableServerProfiles.FEDHUB_0, "group0");
			engine.offlineAddInboundFederateGroup(ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.FEDHUB_0, "group0");
			engine.offlineAddInboundFederateGroup(ImmutableServerProfiles.SERVER_1, ImmutableServerProfiles.FEDHUB_0, "group0");
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s0_stcp0_anonuser_0f);
			engine.offlineAddUsersAndConnectionsIfNecessary(ImmutableUsers.s1_stcp0_anonuser_0f);
			
			// start both TAK Servers
			engine.startServer(ImmutableServerProfiles.SERVER_0, sessionIdentifier);
			engine.startServer(ImmutableServerProfiles.SERVER_1, sessionIdentifier);
			
//			// Inserting sleep since the servers need some time to federate
			Thread.sleep(30000);
			
			engine.connectClientAndVerify(true, ImmutableUsers.s0_stcp0_anonuser_0f);
			engine.connectClientAndVerify(true, ImmutableUsers.s1_stcp0_anonuser_0f);
			
			engine.attemptSendFromUserAndVerify(ImmutableUsers.s0_stcp0_anonuser_0f);
			engine.attemptSendFromUserAndVerify(ImmutableUsers.s1_stcp0_anonuser_0f);
		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace(System.err);
			Assert.fail(e.getMessage());
		} finally {
			try {
				Thread.sleep(10000);
				engine.stopServers(testServers);
			} catch (Exception e) {
				System.err.println(e.getMessage());
				e.printStackTrace(System.err);
			}
		}
	}
	
	@Test(timeout = 960000)
	public void basicMultiInputFedHubTest() {
		try {
			String sessionIdentifier = initTestMethod();
			
			// initial setup of the hub without actually starting it
			engine.overrideDefaultProcessConfiguration(ImmutableServerProfiles.FEDHUB_0, ServerProcessConfiguration.FedhubBrokerFedhubPolicy);

			// get fedhub broker config
			FederationHubServerConfig fedBrokerConfig = getFederationHubConfig();
			// create policy for allowing connections from the hub's ca (all servers will have same CA, aka interconnected)
			createPolicyForHubCA(fedBrokerConfig);
			
			// start fedhub
			engine.startServer(ImmutableServerProfiles.FEDHUB_0, sessionIdentifier);
			
			engine.offlineFederateServers(false, true, ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.SERVER_2);
			engine.offlineAddOutboundFederateConnection(true, ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.FEDHUB_0);
			engine.offlineAddOutboundFederateConnection(true, ImmutableServerProfiles.SERVER_2, ImmutableServerProfiles.FEDHUB_0);
			engine.offlineAddFederate(ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.FEDHUB_0);
			engine.offlineAddFederate(ImmutableServerProfiles.SERVER_2, ImmutableServerProfiles.FEDHUB_0);
			engine.offlineAddInboundFederateGroup(ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.FEDHUB_0, "group0");
			engine.offlineAddInboundFederateGroup(ImmutableServerProfiles.SERVER_2, ImmutableServerProfiles.FEDHUB_0, "group0");
			engine.offlineAddOutboundFederateGroup(ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.FEDHUB_0, "group0");
			engine.offlineAddOutboundFederateGroup(ImmutableServerProfiles.SERVER_2, ImmutableServerProfiles.FEDHUB_0, "group0");
			
			ImmutableUsers[] users = new ImmutableUsers[]{
					ImmutableUsers.s0_authstcp_authuser01_01f,
					ImmutableUsers.s2_authstcp_authuser01_01f,
					ImmutableUsers.s0_ssl_anonuser_t,
					ImmutableUsers.s2_ssl_anonuser_t,
					ImmutableUsers.s0_stcp0_anonuser_0f,
					ImmutableUsers.s2_stcp0_anonuser_0f,
					ImmutableUsers.s0_stcp12_anonuser_12f,
					ImmutableUsers.s2_stcp12_anonuser_12f,
					ImmutableUsers.s0_udp12t_anonuser_12t,
					ImmutableUsers.s2_udp12t_anonuser_12t
			};

			for (ImmutableUsers user : users) {
				engine.offlineAddUsersAndConnectionsIfNecessary(user);
			}

			engine.startServer(ImmutableServerProfiles.SERVER_0, sessionIdentifier);
			engine.startServer(ImmutableServerProfiles.SERVER_2, sessionIdentifier);

			// Inserting sleep since the servers need some time to federate
			Thread.sleep(30000);

			for (ImmutableUsers user : users) {
				if (user.getConnection().getProtocol().canConnect()) {
					engine.connectClientAndVerify(true, user);
				}
			}

			for (ImmutableUsers user : users) {
				engine.attemptSendFromUserAndVerify(user);
			}


		} catch (InterruptedException e) {
			System.err.println(e.getMessage());
			e.printStackTrace(System.err);
			Assert.fail(e.getMessage());
		} finally {
			try {
				engine.stopServers(testServers);
			} catch (Exception e) {
				System.err.println(e.getMessage());
				e.printStackTrace(System.err);
			}
		}
	}
	
	@Test(timeout = 8400000)
	public void advancedFedHubTest() {
		try {
			String sessionIdentifier = initTestMethod();
			
			// initial setup of the hub without actually starting it
			engine.overrideDefaultProcessConfiguration(ImmutableServerProfiles.FEDHUB_0, ServerProcessConfiguration.FedhubBrokerFedhubPolicy);

			// get fedhub broker config
			FederationHubServerConfig fedBrokerConfig = getFederationHubConfig();
			// create policy for allowing connections from the hub's ca (all servers will have same CA, aka interconnected)
			createPolicyForHubCA(fedBrokerConfig);
			
			// start fedhub
			engine.startServer(ImmutableServerProfiles.FEDHUB_0, sessionIdentifier);
			
			engine.offlineFederateServers(false, true, ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.SERVER_1);

			engine.offlineAddOutboundFederateConnection(true, ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.FEDHUB_0);
			engine.offlineAddOutboundFederateConnection(true, ImmutableServerProfiles.SERVER_1, ImmutableServerProfiles.FEDHUB_0);

			engine.offlineAddFederate(ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.FEDHUB_0);
			engine.offlineAddFederate(ImmutableServerProfiles.SERVER_1, ImmutableServerProfiles.FEDHUB_0);

			engine.offlineAddOutboundFederateGroup(ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.FEDHUB_0, "group0");
			engine.offlineAddOutboundFederateGroup(ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.FEDHUB_0, "group2");
			engine.offlineAddOutboundFederateGroup(ImmutableServerProfiles.SERVER_1, ImmutableServerProfiles.FEDHUB_0, "group0");
			engine.offlineAddOutboundFederateGroup(ImmutableServerProfiles.SERVER_1, ImmutableServerProfiles.FEDHUB_0, "group1");

			engine.offlineAddInboundFederateGroup(ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.FEDHUB_0, "group1");
			engine.offlineAddInboundFederateGroup(ImmutableServerProfiles.SERVER_1, ImmutableServerProfiles.FEDHUB_0, "group2");

			ImmutableUsers[] users = new ImmutableUsers[]{
					ImmutableUsers.s0_authstcp_authuser01_01f,
					ImmutableUsers.s1_authstcp_authuser01_01f,
					ImmutableUsers.s0_ssl_anonuser_t,
					ImmutableUsers.s1_ssl_anonuser_t,
					ImmutableUsers.s0_stcp0_anonuser_0f,
					ImmutableUsers.s1_stcp0_anonuser_0f,
					ImmutableUsers.s0_stcp12_anonuser_12f,
					ImmutableUsers.s1_stcp12_anonuser_12f,
					ImmutableUsers.s0_udp12t_anonuser_12t,
					ImmutableUsers.s1_udp12t_anonuser_12t,
			};

			for (ImmutableUsers user : users) {
				engine.offlineAddUsersAndConnectionsIfNecessary(user);
			}

			engine.startServer(ImmutableServerProfiles.SERVER_0, sessionIdentifier);
			engine.startServer(ImmutableServerProfiles.SERVER_1, sessionIdentifier);

			// Inserting sleep since the servers need some time to federate
			Thread.sleep(30000);

			for (ImmutableUsers user : users) {
				if (user.getConnection().getProtocol().canConnect()) {
					engine.connectClientAndVerify(true, user);
				}
			}

			for (ImmutableUsers user : users) {
				engine.attemptSendFromUserAndVerify(user);
			}

		} catch (InterruptedException e) {
			System.err.println(e.getMessage());
			e.printStackTrace(System.err);
			Assert.fail(e.getMessage());
		} finally {
			try {
				engine.stopServers(testServers);
			} catch (Exception e) {
				System.err.println(e.getMessage());
				e.printStackTrace(System.err);
			}
		}
	}

	private GroupCell createGroupCell(String caName, boolean interconnected) {
		GroupProperties groupProps = new GroupProperties();
		groupProps.setFilters(new ArrayList<FilterNode>());
		groupProps.setAttributes(new ArrayList<Object>());
		groupProps.setName(caName);
		groupProps.setId(caName);
		groupProps.setInterconnected(interconnected);

		GroupCell groupCell = new GroupCell();
		groupCell.setProperties(groupProps);
		groupCell.setId(UUID.randomUUID().toString());
		
		return groupCell;
	}
	
	private EdgeCell createEdgeCell(GroupCell source, GroupCell dest) {
		EdgeProperties edgeProperties = new EdgeProperties();
		edgeProperties.setFilters(new ArrayList<FilterNode>());
		
		EdgeCell edgeCell = new EdgeCell();
		edgeCell.setProperties(edgeProperties);
		edgeCell.setId(UUID.randomUUID().toString());
		edgeCell.addOther("source", Map.of("id", source.getId()));
		edgeCell.addOther("target", Map.of("id", dest.getId()));
		
		return edgeCell;
	}
	
	private FederationOutgoingCell createOutgoingCell(String host, int port, String name, boolean enabled) {
		FederateOutgoingProperties outgoingProps = new FederateOutgoingProperties();
		outgoingProps.setHost(host);
		outgoingProps.setPort(port);
		outgoingProps.setName(name);
		outgoingProps.setOutgoingName(name);
		outgoingProps.setId(name);
		outgoingProps.setOutgoingEnabled(enabled);

		FederationOutgoingCell outgoing = new FederationOutgoingCell();
		outgoing.setProperties(outgoingProps);
		outgoing.setId(UUID.randomUUID().toString());

		return outgoing;
	}
}
