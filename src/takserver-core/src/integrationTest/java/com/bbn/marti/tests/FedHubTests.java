package com.bbn.marti.tests;

import java.io.File;
import java.io.FileInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.bbn.marti.takcl.SSLHelper;
import com.bbn.marti.takcl.connectivity.server.AbstractRunnableServer;
import com.bbn.marti.takcl.connectivity.server.ServerProcessConfiguration;
import com.bbn.marti.test.shared.AbstractTestClass;
import com.bbn.marti.test.shared.data.generated.ImmutableUsers;
import com.bbn.marti.test.shared.data.servers.ImmutableServerProfiles;
import com.bbn.marti.test.shared.engines.TestEngine;
import com.bbn.roger.fig.FederationUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import tak.server.federation.hub.broker.FederationHubServerConfig;
import tak.server.federation.hub.ui.graph.EdgeCell;
import tak.server.federation.hub.ui.graph.EdgeProperties;
import tak.server.federation.hub.ui.graph.FederateOutgoingProperties;
import tak.server.federation.hub.ui.graph.FederationOutgoingCell;
import tak.server.federation.hub.ui.graph.FederationUIGraphPolicyModel;
import tak.server.federation.hub.ui.graph.FederationUIPolicyModel;
import tak.server.federation.hub.ui.graph.FilterNode;
import tak.server.federation.hub.ui.graph.GroupCell;
import tak.server.federation.hub.ui.graph.GroupProperties;
import tak.server.federation.hub.ui.graph.PolicyObjectCell;

public class FedHubTests extends AbstractTestClass {
	private static final ImmutableServerProfiles[] testServers = new ImmutableServerProfiles[] {
			ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.SERVER_1, ImmutableServerProfiles.SERVER_2,
			ImmutableServerProfiles.FEDHUB_0, ImmutableServerProfiles.FEDHUB_1 };

	private static boolean useUniqueCerts = true;

	@BeforeClass
	public static void setup() {
		try {
			SSLHelper.genCertsIfNecessary(true);
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

	@Test(timeout = 920000)
	public void basicFedHubGroupMappingTest() {
		useUniqueCerts = true;
		try {
			SSLHelper.setUseUniqueCertsPerServer(useUniqueCerts);

			String sessionIdentifier = initTestMethod();

			// get fedhub broker config
			FederationHubServerConfig fedBrokerConfig = getFederationHubConfig();

			List<String> allowedGroups = new ArrayList<>();
			allowedGroups.add("group0");			
			// create policy for allowing connections from the hub's ca
			Collection<PolicyObjectCell> policyCells = useUniqueCerts ? createPolicyCellsUniqueCerts("allowed", allowedGroups, new ArrayList<>())
					: createPolicyCellsDefault();

			createPolicy(fedBrokerConfig, policyCells);

			// initial setup of the hub without actually starting it
			engine.overrideDefaultProcessConfiguration(ImmutableServerProfiles.FEDHUB_0,
					ServerProcessConfiguration.FedhubBrokerFedhubPolicy);

			// start fedhub
			engine.startServer(ImmutableServerProfiles.FEDHUB_0, sessionIdentifier);
			// enable federation on server 0 and server 1
			engine.offlineFederateServers(false, true, ImmutableServerProfiles.SERVER_0,
					ImmutableServerProfiles.SERVER_1);
			// enable federation outgoing on both server 0 + 1 to federation hub
			engine.offlineAddOutboundFederateConnection(true, ImmutableServerProfiles.SERVER_0,
					ImmutableServerProfiles.FEDHUB_0);
			engine.offlineAddOutboundFederateConnection(true, ImmutableServerProfiles.SERVER_1,
					ImmutableServerProfiles.FEDHUB_0);
			// add the fedhub identify to server 0 and 1
			engine.offlineAddFederate(ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.FEDHUB_0);
			engine.offlineAddFederate(ImmutableServerProfiles.SERVER_1, ImmutableServerProfiles.FEDHUB_0);
			// add outbound groups
			engine.offlineAddOutboundFederateGroup(ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.FEDHUB_0,
					"group0");
			engine.offlineAddOutboundFederateGroup(ImmutableServerProfiles.SERVER_1, ImmutableServerProfiles.FEDHUB_0,
					"group0");
			// enable group mapping
			engine.offlineEnableFederatedGroupMapping(ImmutableServerProfiles.SERVER_0,
					ImmutableServerProfiles.FEDHUB_0, true);
			engine.offlineEnableFederatedGroupMapping(ImmutableServerProfiles.SERVER_1,
					ImmutableServerProfiles.FEDHUB_0, true);
			// add inbound group mappings
			engine.offlineAddInboundFederateGroupMapping(ImmutableServerProfiles.SERVER_0,
					ImmutableServerProfiles.FEDHUB_0, "group0", "group0");
			engine.offlineAddInboundFederateGroupMapping(ImmutableServerProfiles.SERVER_1,
					ImmutableServerProfiles.FEDHUB_0, "group0", "group0");

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
	
	@Test(timeout = 920000)
	public void basicFedHubTest() {
		useUniqueCerts = false;
		try {
			SSLHelper.setUseUniqueCertsPerServer(useUniqueCerts);

			String sessionIdentifier = initTestMethod();

			// get fedhub broker config
			FederationHubServerConfig fedBrokerConfig = getFederationHubConfig();
			// create policy for allowing connections from the hub's ca
			Collection<PolicyObjectCell> policyCells = useUniqueCerts ? createPolicyCellsUniqueCerts()
					: createPolicyCellsDefault();

			createPolicy(fedBrokerConfig, policyCells);
		    
			// initial setup of the hub without actually starting it
			engine.overrideDefaultProcessConfiguration(ImmutableServerProfiles.FEDHUB_0,
					ServerProcessConfiguration.FedhubBrokerFedhubPolicy);

			// start fedhub
			engine.startServer(ImmutableServerProfiles.FEDHUB_0, sessionIdentifier);

			engine.offlineFederateServers(false, true, ImmutableServerProfiles.SERVER_0,
					ImmutableServerProfiles.SERVER_1);
			engine.offlineAddOutboundFederateConnection(true, ImmutableServerProfiles.SERVER_0,
					ImmutableServerProfiles.FEDHUB_0);
			engine.offlineAddOutboundFederateConnection(true, ImmutableServerProfiles.SERVER_1,
					ImmutableServerProfiles.FEDHUB_0);
			engine.offlineAddFederate(ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.FEDHUB_0);
			engine.offlineAddFederate(ImmutableServerProfiles.SERVER_1, ImmutableServerProfiles.FEDHUB_0);
			engine.offlineAddOutboundFederateGroup(ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.FEDHUB_0,
					"group0");
			engine.offlineAddOutboundFederateGroup(ImmutableServerProfiles.SERVER_1, ImmutableServerProfiles.FEDHUB_0,
					"group0");
			engine.offlineAddInboundFederateGroup(ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.FEDHUB_0,
					"group0");
			engine.offlineAddInboundFederateGroup(ImmutableServerProfiles.SERVER_1, ImmutableServerProfiles.FEDHUB_0,
					"group0");
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
		useUniqueCerts = false;

		try {
			SSLHelper.setUseUniqueCertsPerServer(useUniqueCerts);

			String sessionIdentifier = initTestMethod();

			// get fedhub broker config
			FederationHubServerConfig fedBrokerConfig = getFederationHubConfig();

			// create policy for allowing connections from the hub's ca
			Collection<PolicyObjectCell> policyCells = useUniqueCerts ? createPolicyCellsUniqueCerts()
					: createPolicyCellsDefault();

			createPolicy(fedBrokerConfig, policyCells);

			// initial setup of the hub without actually starting it
			engine.overrideDefaultProcessConfiguration(ImmutableServerProfiles.FEDHUB_0,
					ServerProcessConfiguration.FedhubBrokerFedhubPolicy);

			// start fedhub
			engine.startServer(ImmutableServerProfiles.FEDHUB_0, sessionIdentifier);

			engine.offlineFederateServers(false, true, ImmutableServerProfiles.SERVER_0,
					ImmutableServerProfiles.SERVER_2);
			engine.offlineAddOutboundFederateConnection(true, ImmutableServerProfiles.SERVER_0,
					ImmutableServerProfiles.FEDHUB_0);
			engine.offlineAddOutboundFederateConnection(true, ImmutableServerProfiles.SERVER_2,
					ImmutableServerProfiles.FEDHUB_0);
			engine.offlineAddFederate(ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.FEDHUB_0);
			engine.offlineAddFederate(ImmutableServerProfiles.SERVER_2, ImmutableServerProfiles.FEDHUB_0);
			engine.offlineAddInboundFederateGroup(ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.FEDHUB_0,
					"group0");
			engine.offlineAddInboundFederateGroup(ImmutableServerProfiles.SERVER_2, ImmutableServerProfiles.FEDHUB_0,
					"group0");
			engine.offlineAddOutboundFederateGroup(ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.FEDHUB_0,
					"group0");
			engine.offlineAddOutboundFederateGroup(ImmutableServerProfiles.SERVER_2, ImmutableServerProfiles.FEDHUB_0,
					"group0");

			ImmutableUsers[] users = new ImmutableUsers[] { ImmutableUsers.s0_authstcp_authuser01_01f,
					ImmutableUsers.s2_authstcp_authuser01_01f, ImmutableUsers.s0_ssl_anonuser_t,
					ImmutableUsers.s2_ssl_anonuser_t, ImmutableUsers.s0_stcp0_anonuser_0f,
					ImmutableUsers.s2_stcp0_anonuser_0f, ImmutableUsers.s0_stcp12_anonuser_12f,
					ImmutableUsers.s2_stcp12_anonuser_12f, ImmutableUsers.s0_udp12t_anonuser_12t,
					ImmutableUsers.s2_udp12t_anonuser_12t };

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
		useUniqueCerts = false;

		try {
			SSLHelper.setUseUniqueCertsPerServer(false);

			String sessionIdentifier = initTestMethod();

			// initial setup of the hub without actually starting it
			engine.overrideDefaultProcessConfiguration(ImmutableServerProfiles.FEDHUB_0,
					ServerProcessConfiguration.FedhubBrokerFedhubPolicy);

			// get fedhub broker config
			FederationHubServerConfig fedBrokerConfig = getFederationHubConfig();

			// create policy for allowing connections from the hub's ca
			Collection<PolicyObjectCell> policyCells = useUniqueCerts ? createPolicyCellsUniqueCerts()
					: createPolicyCellsDefault();

			createPolicy(fedBrokerConfig, policyCells);

			// start fedhub
			engine.startServer(ImmutableServerProfiles.FEDHUB_0, sessionIdentifier);

			engine.offlineFederateServers(false, true, ImmutableServerProfiles.SERVER_0,
					ImmutableServerProfiles.SERVER_1);

			engine.offlineAddOutboundFederateConnection(true, ImmutableServerProfiles.SERVER_0,
					ImmutableServerProfiles.FEDHUB_0);
			engine.offlineAddOutboundFederateConnection(true, ImmutableServerProfiles.SERVER_1,
					ImmutableServerProfiles.FEDHUB_0);

			engine.offlineAddFederate(ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.FEDHUB_0);
			engine.offlineAddFederate(ImmutableServerProfiles.SERVER_1, ImmutableServerProfiles.FEDHUB_0);

			engine.offlineAddOutboundFederateGroup(ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.FEDHUB_0,
					"group0");
			engine.offlineAddOutboundFederateGroup(ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.FEDHUB_0,
					"group2");
			engine.offlineAddOutboundFederateGroup(ImmutableServerProfiles.SERVER_1, ImmutableServerProfiles.FEDHUB_0,
					"group0");
			engine.offlineAddOutboundFederateGroup(ImmutableServerProfiles.SERVER_1, ImmutableServerProfiles.FEDHUB_0,
					"group1");

			engine.offlineAddInboundFederateGroup(ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.FEDHUB_0,
					"group1");
			engine.offlineAddInboundFederateGroup(ImmutableServerProfiles.SERVER_1, ImmutableServerProfiles.FEDHUB_0,
					"group2");

			ImmutableUsers[] users = new ImmutableUsers[] { ImmutableUsers.s0_authstcp_authuser01_01f,
					ImmutableUsers.s1_authstcp_authuser01_01f, ImmutableUsers.s0_ssl_anonuser_t,
					ImmutableUsers.s1_ssl_anonuser_t, ImmutableUsers.s0_stcp0_anonuser_0f,
					ImmutableUsers.s1_stcp0_anonuser_0f, ImmutableUsers.s0_stcp12_anonuser_12f,
					ImmutableUsers.s1_stcp12_anonuser_12f, ImmutableUsers.s0_udp12t_anonuser_12t,
					ImmutableUsers.s1_udp12t_anonuser_12t, };

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

	private FederationHubServerConfig getFederationHubConfig() {
		FederationHubServerConfig fedBrokerConfig = null;
		try {
			String DEFAULT_FEDERATION_BROKER_CONFIG_FILE = "/opt/tak/federation-hub/configs/federation-hub-broker.yml";

			String fedhubId = ImmutableServerProfiles.FEDHUB_0.getConsistentUniqueReadableIdentifier();

			fedBrokerConfig = new ObjectMapper(new YAMLFactory()).readValue(
					new FileInputStream(DEFAULT_FEDERATION_BROKER_CONFIG_FILE), FederationHubServerConfig.class);

			fedBrokerConfig.setKeystoreFile("certs/files/takserver.jks");
			fedBrokerConfig.setTruststoreFile("certs/files/fed-truststore.jks");
			fedBrokerConfig.setV1Port(ImmutableServerProfiles.FEDHUB_0.getFederationV1ServerPort());
			fedBrokerConfig.setV2Port(ImmutableServerProfiles.FEDHUB_0.getFederationV2ServerPort());
			fedBrokerConfig.setId(fedhubId);

			ObjectMapper om = new ObjectMapper(new YAMLFactory());
			om.writeValue(new File(DEFAULT_FEDERATION_BROKER_CONFIG_FILE), fedBrokerConfig);
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		return fedBrokerConfig;
	}

	private String getCAGroupNameByServerId(String serverId) {
		String caPath = "/opt/tak/TEST_RESULTS/" + serverId + "/ca.pem";
		return getCAGroupNameByPath(caPath);
	}

	private String getCAGroupNameForDefaultCerts() {
		String caPath = "/opt/tak/TEST_RESULTS/TEST_CERTS/ca.pem";
		return getCAGroupNameByPath(caPath);
	}

	private String getCAGroupNameByPath(String caPath) {
		try {
			CertificateFactory factory = CertificateFactory.getInstance("X.509");
			X509Certificate ca = (X509Certificate) factory.generateCertificate(new FileInputStream(new File(caPath)));
			String issuerName = ca.getIssuerX500Principal().getName();
			return issuerName + "-" + FederationUtils.getBytesSHA256(ca.getEncoded());
		} catch (Exception e) {
			e.printStackTrace(System.err);
			return null;
		}
	}
	
	private Collection<PolicyObjectCell> createPolicyCellsUniqueCerts() {
		return createPolicyCellsUniqueCerts(null, null, null);
	}

	private Collection<PolicyObjectCell> createPolicyCellsUniqueCerts(String groupFilterType,
			List<String> allowedGroups, List<String> disallowedGroups) {

		Collection<PolicyObjectCell> cells = new ArrayList<>();

		String HUB_0 = getCAGroupNameByServerId(
				ImmutableServerProfiles.FEDHUB_0.getConsistentUniqueReadableIdentifier());
		GroupCell HUB_0_CELL = createGroupCell(HUB_0, false);
		cells.add(HUB_0_CELL);

		String SERVER_0 = getCAGroupNameByServerId(
				ImmutableServerProfiles.SERVER_0.getConsistentUniqueReadableIdentifier());
		GroupCell SERVER_0_CELL = createGroupCell(SERVER_0, false);
		cells.add(SERVER_0_CELL);

		String SERVER_1 = getCAGroupNameByServerId(
				ImmutableServerProfiles.SERVER_1.getConsistentUniqueReadableIdentifier());
		GroupCell SERVER_1_CELL = createGroupCell(SERVER_1, false);
		cells.add(SERVER_1_CELL);

		EdgeProperties edgeProperties = new EdgeProperties();
		
		if (groupFilterType != null) {
			// allgroups, allowed, disallowed, allowedanddisallowed
			edgeProperties.setGroupsFilterType(groupFilterType);
			edgeProperties.setAllowedGroups(allowedGroups);
			edgeProperties.setDisallowedGroups(disallowedGroups);
		}

		EdgeCell SERVER_0_TO_SERVER_1_EDGE_CELL = createEdgeCell(SERVER_0_CELL, SERVER_1_CELL, edgeProperties);
		cells.add(SERVER_0_TO_SERVER_1_EDGE_CELL);

		EdgeCell SERVER_1_TO_SERVER_0_EDGE_CELL = createEdgeCell(SERVER_1_CELL, SERVER_0_CELL, edgeProperties);
		cells.add(SERVER_1_TO_SERVER_0_EDGE_CELL);

		return cells;
	}

	private Collection<PolicyObjectCell> createPolicyCellsDefault() {
		Collection<PolicyObjectCell> cells = new ArrayList<>();

		String DEFAULT_CA = getCAGroupNameForDefaultCerts();
		GroupCell DEFAULT_CELL = createGroupCell(DEFAULT_CA, true);
		cells.add(DEFAULT_CELL);

		return cells;
	}

	private void createPolicy(FederationHubServerConfig fedBrokerConfig, Collection<PolicyObjectCell> cells) {
		FederationUIGraphPolicyModel graphPolicyModel = new FederationUIGraphPolicyModel();
		graphPolicyModel.setNodes(cells);
		
		FederationUIPolicyModel policyModel = new FederationUIPolicyModel();		
		policyModel.setGraphData(graphPolicyModel);
		policyModel.setName(this.getClass().getName());

		String DEFAULT_FEDERATION_POLICY_CONFIG_FILE = "/opt/tak/federation-hub/ui_generated_policy.json";
		ObjectMapper mapper = new ObjectMapper();
		try {
			mapper.writerWithDefaultPrettyPrinter().writeValue(new File(DEFAULT_FEDERATION_POLICY_CONFIG_FILE),
					policyModel);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Could not write policy to file " + e.getStackTrace());
		}
	}

	private GroupCell createGroupCell(String caName, boolean interconnected) {
		GroupProperties groupProps = new GroupProperties();
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

		return createEdgeCell(source, dest, edgeProperties);
	}

	private EdgeCell createEdgeCell(GroupCell source, GroupCell dest, EdgeProperties edgeProperties) {
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
