package com.bbn.marti.takcl.AppModules;

import com.bbn.marti.config.*;
import com.bbn.marti.takcl.AppModules.generic.ServerAppModuleInterface;
import com.bbn.marti.takcl.SSLHelper;
import com.bbn.marti.takcl.TAKCLCore;
import com.bbn.marti.takcl.TestExceptions;
import com.bbn.marti.takcl.Util;
import com.bbn.marti.takcl.cli.simple.Command;
import com.bbn.marti.takcl.config.common.TakclRunMode;
import com.bbn.marti.test.shared.data.connections.AbstractConnection;
import com.bbn.marti.test.shared.data.protocols.ProtocolProfiles;
import com.bbn.marti.test.shared.data.servers.AbstractServerProfile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Used to modify the server CoreConfig.xml file offline. If this is used while the server is running, the changes will not take effect, and may cause undesirable behavior.
 *
 * @command
 */
public class OfflineConfigModule implements ServerAppModuleInterface {

	private Configuration configuration;

	private AbstractServerProfile server;

	private String fileLocation = null;

	public OfflineConfigModule() {
	}

	@NotNull
	@Override
	public TakclRunMode[] getRunModes() {
		return new TakclRunMode[]{TakclRunMode.LOCAL_SERVER_INTERACTION};
	}

	@Override
	public ServerState getRequiredServerState() {
		return ServerState.STOPPED;
	}

	@Override
	public String getCommandDescription() {
		return "Modifies the ServerInstance configuration file defined in the TAKCL configuration file.  The TAKCL " +
				"configuration file should be in the same directory as the TAKCL executable and named TAKCLConfigModule.xml";
	}

	@Override
	public void init(@NotNull AbstractServerProfile serverIdentifier) {
		// TODO: This is a little hacky...
		if (!serverIdentifier.getUrl().equals("127.0.0.1") && !serverIdentifier.getUrl().equals("localhost")) {
			throw new RuntimeException("Cannot access the offline user auth file without a known server path!");
		}

		fileLocation = serverIdentifier.getUserAuthFilePath();
		this.server = serverIdentifier;

		try {
			fileLocation = server.getConfigFilePath();
			Path configPath = Paths.get(fileLocation);

			if (!Files.exists(configPath)) {
				Files.copy(Paths.get(TAKCLConfigModule.getInstance().getCleanConfigFilepath()), Paths.get(fileLocation), StandardCopyOption.REPLACE_EXISTING);
			} else {
				System.out.println("Configuration file \"" + fileLocation + "\" already exists. Using existing values.");
			}

			this.configuration = Util.loadJAXifiedXML(fileLocation, Configuration.class.getPackage().getName());

		} catch (IOException | JAXBException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void halt() {

	}

	/**
	 * Used to create or revert the existing CoreConfig.xml The previous one will be backed up to CoreConfig.xml.bak This will not revert the UserAuthenticationFile.xml. That must be done using the {@link OfflineFileAuthModule#resetConfig()} command.
	 */
	@Command(description = "Resets the default configuration file to the example config file.")
	public void resetConfig() {
		try {
			TAKCLConfigModule conf = TAKCLConfigModule.getInstance();

			Path configPath = Paths.get(fileLocation);

			if (Files.exists(configPath)) {
				Files.copy(Paths.get(fileLocation), Paths.get(fileLocation + ".bak"), StandardCopyOption.REPLACE_EXISTING);
			}
			Files.copy(Paths.get(conf.getCleanConfigFilepath()), Paths.get(fileLocation), StandardCopyOption.REPLACE_EXISTING);

			init(server);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public List<Network.Input> getInputs() {
		return configuration.getNetwork().getInput();
	}

	public void saveChanges() {
		try {
			Util.saveJAXifiedObject(fileLocation, configuration, true);
		} catch (IOException | JAXBException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Enables or disables latestSA on the server and saves the change.
	 *
	 * @param isEnabled Whether or not to enable latestSA
	 */
	@Command(description = "Enables or disables latestSA in the configuration file.")
	public void latestSA(final @NotNull Boolean isEnabled) {
		configuration.getBuffer().getLatestSA().setEnable(isEnabled);
		saveChanges();
	}

	/**
	 * Enables or disables file-based authorization on the server and saves the change. This will only impact inputs that have their authorization set to use it.
	 *
	 * @param isEnabled Whether or not to enable latestSA
	 */
	@Command(description = "Enables or disables file authorization in the configuration file for the default \"UserAuthenticationFile.xml\".")
	public void fileAuth(final @NotNull Boolean isEnabled) {
		if (isEnabled) {
			Auth.File authFile = new Auth.File();
			authFile.setLocation("UserAuthenticationFile.xml");
			configuration.getAuth().setFile(authFile);
		} else {
			configuration.getAuth().setFile(null);
		}
		saveChanges();
	}

	/**
	 * Tells if file authuser is enabled
	 *
	 * @return Whether or not it is enabled
	 * @command
	 */
	@Command(description = "Is file authuser enabled?")
	public boolean isFileAuthEnabled() {
		return (configuration.getAuth().getFile() != null);
	}

	public void addInput(final Network.Input input) {
		List<Network.Input> inputList = configuration.getNetwork().getInput();

		for (Network.Input loopInput : inputList) {
			if (loopInput.getName().equals(input.getName())) {
				inputList.remove(loopInput);
				break;
			}
		}
		inputList.add(input);
		saveChanges();
	}

	public void addConnectionIfNecessary(@NotNull AbstractConnection connection) {
		ProtocolProfiles.ConnectionType connectionType = connection.getConnectionType();

		if (connectionType == ProtocolProfiles.ConnectionType.INPUT) {
			Set<String> inputNameSet = new HashSet<>();
			for (Network.Input loopinput : getInputs()) {
				inputNameSet.add(loopinput.getName());
			}
			Network.Input input = connection.getConfigInput();
			if (!inputNameSet.contains(input.getName())) {
				addInput(input);
				if (connection.getProtocol().isTLS()) {
					setSSLSecuritySettings();
				}
			}

			if (connection.getAuthType() == AuthType.FILE) {
				fileAuth(true);
			}

		} else if (connectionType == ProtocolProfiles.ConnectionType.SUBSCRIPTION) {
			Set<String> subscriptionNameSet = new HashSet<>();
			for (Subscription.Static loopsubscription : getStaticSubscriptions()) {
				subscriptionNameSet.add(loopsubscription.getName());
			}

			Subscription.Static subscription = connection.getStaticSubscription();
			if (!subscriptionNameSet.contains(subscription.getName())) {
				addStaticSubscription(subscription);
				if (connection.getProtocol().isTLS()) {
					setSSLSecuritySettings();
				}
			}

		} else {
			throw new RuntimeException("Invalid ConnectionType of type '" + connection.getConsistentUniqueReadableIdentifier() + "'!");
		}
	}

	public void addStaticSubscription(@NotNull Subscription.Static subscription) {
		List<Subscription.Static> staticSubscriptions = configuration.getSubscription().getStatic();

		for (Subscription.Static loopSubscription : staticSubscriptions) {
			if (loopSubscription.getName().equals((subscription.getName()))) {
				staticSubscriptions.remove(loopSubscription);
				break;
			}
		}
		staticSubscriptions.add(subscription);
		saveChanges();
	}

	public List<Subscription.Static> getStaticSubscriptions() {
		return configuration.getSubscription().getStatic();
	}

	/**
	 * Removes the input with the specified getConsistentUniqueReadableIdentifier if it exists and saves the change.
	 *
	 * @param inputName The input getConsistentUniqueReadableIdentifier to remove
	 * @command
	 */
	@Command
	public void removeInput(final String inputName) {
		List<Network.Input> inputList = configuration.getNetwork().getInput();

		for (Network.Input input : inputList) {
			if (input.getName().equals(inputName)) {
				inputList.remove(input);
				break;
			}
		}
		saveChanges();
	}

	public void setFlowTag(String flowTag) {
		configuration.getFilter().getFlowtag().setText(flowTag);
		saveChanges();
	}

	public Repository getRepository() {
		return configuration.getRepository();
	}

	@Command
	public void setDbPassword(@NotNull String password) {
		configuration.getRepository().getConnection().setPassword(password);
		saveChanges();
	}

	@Command
	public void setDbUsername(@NotNull String username) {
		configuration.getRepository().getConnection().setUsername(username);
		saveChanges();
	}

	@Command
	public void setDbUrl(@NotNull String url) {
		configuration.getRepository().getConnection().setUrl(url);
		saveChanges();
	}

	@Command
	public void setMaxDbConnections(@NotNull Integer number) {
		configuration.getRepository().setNumDbConnections(number);
		saveChanges();
	}

	@Command
	public void enableDb() {
		configuration.getRepository().setEnable(true);
	}

	public void disableDb() {
		configuration.getRepository().setEnable(false);
	}

	@Command
	public void setHttpsPort(@NotNull Integer port) {
		setConnectorConfigurationAndSave("https", port, null, null, null);
	}

	@Command
	public void setIgnitePortRange(@Nullable Integer discoveryPort, @Nullable Integer discoveryPortCount) {
		Buffer buffer = configuration.getBuffer();
		if (buffer == null) {
			buffer = new Buffer();
			configuration.setBuffer(buffer);
		}
		if (discoveryPort != null) {
			buffer.setIgniteNonMulticastDiscoveryPort(discoveryPort);
			if (discoveryPortCount != null) {
				buffer.setIgniteNonMulticastDiscoveryPortCount(discoveryPortCount);
			}
		}
		saveChanges();
	}

	@Command
	public void setFedHttpsPort(@NotNull Integer port) {
		setConnectorConfigurationAndSave("fed_https", port, true, null, null);
	}

	@Command
	public void setCertHttpsPort(@NotNull Integer port) {
		setConnectorConfigurationAndSave("cert_https", port, null, "true", null);
	}

	@Command
	public void sethttpPlaintextPort(@NotNull Integer port) {
		setConnectorConfigurationAndSave("http_plaintext", port, null, null, false);
	}

	public void enableSwagger() {
		Docs docs = configuration.getDocs();
		if (docs == null) {
			docs = new Docs();
			configuration.setDocs(docs);
		}
		docs.setAdminOnly(false);
		saveChanges();
	}

	private void setConnectorConfigurationAndSave(@NotNull String name, @NotNull Integer port, @Nullable Boolean useFedTrustStore,
	                                              @Nullable String clientAuth, @Nullable Boolean tls) {
		Network.Connector connector = null;

		Network n = configuration.getNetwork();

		// Grab the existing connector if it exists
		for (Network.Connector c : n.getConnector()) {
			if (c.getName().equals(name)) {
				connector = c;
				break;
			}
		}

		// Make a new one if it does not
		if (connector == null) {
			connector = new Network.Connector();
			connector.setName(name);
			configuration.getNetwork().getConnector().add(connector);
		}

		connector.setPort(port);

		if (useFedTrustStore != null) {
			connector.setUseFederationTruststore(useFedTrustStore);
		}

		if (tls != null) {
			connector.setTls(tls);
		}

		if (clientAuth != null) {
			connector.setClientAuth(clientAuth);
		}

		saveChanges();
	}

	public void setSSLSecuritySettings() {
		SSLHelper ssl = SSLHelper.getInstance();
		String keystorePath = server.getServerPath() + "keystore.jks";
		String truststorePath = server.getServerPath() + "truststore.jks";

		if (!Files.exists(Paths.get(keystorePath))) {
			ssl.copyServerKeystoreJks(server.getConsistentUniqueReadableIdentifier(), keystorePath);
		}

		if (!Files.exists(Paths.get(truststorePath))) {
			ssl.copyServerTruststoreJks(truststorePath);
		}

		Tls tls = new Tls();
		configuration.getSecurity().setTls(tls);
		tls.setContext("TLSv1.2");
		tls.setKeymanager("SunX509");

		tls.setKeystore("JKS");
		tls.setKeystoreFile(keystorePath);
		tls.setKeystorePass(ssl.getKeystorePass());

		tls.setTruststore("JKS");
		tls.setTruststoreFile(truststorePath);
		tls.setTruststorePass(ssl.getTruststorePass());

		// TODO: There is probably a better way to do this
//        if (configuration.getFederation() == null) {
//            Federation.FederationServer fedServer = new Federation.FederationServer();
//            fedServer.setPort(server.getFederationServerPort());
//            fedServer.setTls(configuration.getSecurity().getTls());
//            Federation federation = new Federation();
//            federation.setFederationServer(fedServer);
//            configuration.setFederation(federation);
//        }
		saveChanges();
	}

	public boolean isServerFederated() {
		Federation federation = configuration.getFederation();

		return (federation != null && federation.getFederationServer() != null);
	}

	public void enableFederationServer(boolean useV1Federation, boolean useV2Federation) {
		SSLHelper ssl = SSLHelper.getInstance();

		String keystorePath = server.getServerPath() + "federationKeystore.jks";
		String truststorePath = server.getServerPath() + "federationTruststore.jks";

		ssl.copyServerKeystoreJks(server.getConsistentUniqueReadableIdentifier(), keystorePath);
		ssl.copyServerTruststoreJks(truststorePath);

		Federation federation = configuration.getFederation();

		if (federation == null) {
			federation = new Federation();
			configuration.setFederation(federation);
		}

		federation.setEnableFederation(true);

		Federation.FederationServer federationServer = new Federation.FederationServer();
		federation.setFederationServer(federationServer);
		federationServer.setV1Enabled(useV1Federation);
		federationServer.setPort(server.getFederationV1ServerPort());
		federationServer.setV2Enabled(useV2Federation);
		federationServer.setV2Port(server.getFederationV2ServerPort());
		Tls tls = new Tls();
		federationServer.setTls(tls);
		tls.setContext("TLSv1.2");
		tls.setKeymanager("SunX509");

		tls.setKeystore("JKS");
		tls.setKeystoreFile(keystorePath);
		tls.setKeystorePass(ssl.getKeystorePass());
		tls.setTruststore("JKS");
		tls.setTruststoreFile(truststorePath);
		tls.setTruststorePass(ssl.getTruststorePass());
		saveChanges();
	}

	public boolean containsFederate(@NotNull AbstractServerProfile federateServer) {
		Federation federation = configuration.getFederation();
		List<Federation.Federate> federateList = federation.getFederate();

		for (Federation.Federate loopfederate : federateList) {
			if (loopfederate.getName().equals(federateServer.getConsistentUniqueReadableIdentifier())) {
				return true;
			}
		}
		return false;
	}

	public void addFederate(@NotNull AbstractServerProfile federateServer) {
		Federation federation = configuration.getFederation();
		List<Federation.Federate> federateList = federation.getFederate();

		for (Federation.Federate loopfederate : federateList) {
			if (loopfederate.getName().equals(server.getConsistentUniqueReadableIdentifier())) {
				System.err.println("'" + federateServer.getConsistentUniqueReadableIdentifier() + " has already been added as a federate of '" + server.getConsistentUniqueReadableIdentifier() + "'!");
			}
		}

		Federation.Federate newFederate = new Federation.Federate();
		federateList.add(newFederate);
		newFederate.setName(federateServer.getConsistentUniqueReadableIdentifier());
		newFederate.setId(SSLHelper.getInstance().getServerFingerprint(federateServer.getConsistentUniqueReadableIdentifier()));
		saveChanges();
	}


	public boolean containsFederationOutgoing(@NotNull AbstractServerProfile targetServer) {
		List<Federation.FederationOutgoing> outgoingList = configuration.getFederation().getFederationOutgoing();

		if (outgoingList != null) {
			for (Federation.FederationOutgoing outgoingConnection : outgoingList) {
				if (outgoingConnection.getPort() == targetServer.getFederationV1ServerPort() ||
						outgoingConnection.getPort() == targetServer.getFederationV2ServerPort()) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean federationOutgoingIsEnabled(@NotNull AbstractServerProfile targetServer) {
		if (!containsFederationOutgoing(targetServer)) {
			return false;
		} else {
			List<Federation.FederationOutgoing> outgoingList = configuration.getFederation().getFederationOutgoing();

			for (Federation.FederationOutgoing outgoingConnection : outgoingList) {
				if (outgoingConnection.getPort() == targetServer.getFederationV1ServerPort() ||
						outgoingConnection.getPort() == targetServer.getFederationV2ServerPort()) {
					return outgoingConnection.isEnabled();
				}
			}
		}
		return false;
	}

	public void addFederationOutgoing(boolean useV2Federation, @NotNull AbstractServerProfile targetServer) {
		List<Federation.FederationOutgoing> outgoingList = configuration.getFederation().getFederationOutgoing();

		Federation.FederationOutgoing outgoingServer = new Federation.FederationOutgoing();
		outgoingList.add(outgoingServer);
		outgoingServer.setDisplayName(targetServer.getConsistentUniqueReadableIdentifier() + "-out");
		outgoingServer.setAddress("127.0.0.1");
		if (useV2Federation) {
			outgoingServer.setProtocolVersion(2);
			outgoingServer.setPort(targetServer.getFederationV2ServerPort());
		} else {
			outgoingServer.setPort(targetServer.getFederationV1ServerPort());
		}
		saveChanges();
	}

	public boolean containsFederateOutboundGroup(@NotNull AbstractServerProfile federate, @NotNull String groupIdentifier) {
		List<Federation.Federate> federateList = configuration.getFederation().getFederate();

		for (Federation.Federate loopfederate : federateList) {
			if (loopfederate.getName().equals(federate.getConsistentUniqueReadableIdentifier())) {
				List<String> outboundGroups = loopfederate.getOutboundGroup();

				if (outboundGroups.contains(groupIdentifier)) {
					return true;
				}
			}
		}
		return false;
	}

	public void addFederateOutboundGroup(@NotNull AbstractServerProfile federate, @NotNull String groupIdentifier) {
		List<Federation.Federate> federateList = configuration.getFederation().getFederate();

		for (Federation.Federate loopfederate : federateList) {
			if (loopfederate.getName().equals(federate.getConsistentUniqueReadableIdentifier())) {
				List<String> outboundGroups = loopfederate.getOutboundGroup();

				if (outboundGroups.contains(groupIdentifier)) {
					throw new RuntimeException(server.getConsistentUniqueReadableIdentifier() + "already has an outbound federate group for " + federate.getConsistentUniqueReadableIdentifier() + " named '" + groupIdentifier + "'!");
				}
				outboundGroups.add(groupIdentifier);
				saveChanges();
				return;

			}
		}

		throw new RuntimeException("Cannot add outbound group for " + server.getConsistentUniqueReadableIdentifier() + "because the federate " + federate.getConsistentUniqueReadableIdentifier() + " has not been added!");
	}

	public boolean containsFederateInboundGroup(@NotNull AbstractServerProfile federate, @NotNull String groupIdentifier) {
		List<Federation.Federate> federateList = configuration.getFederation().getFederate();

		for (Federation.Federate loopfederate : federateList) {
			if (loopfederate.getName().equals(federate.getConsistentUniqueReadableIdentifier())) {
				List<String> inboundGroups = loopfederate.getInboundGroup();

				if (inboundGroups.contains(groupIdentifier)) {
					return true;
				}
			}
		}
		return false;
	}

	public void addFederateInboundGroup(@NotNull AbstractServerProfile federate, @NotNull String groupIdentifier) {
		List<Federation.Federate> federateList = configuration.getFederation().getFederate();

		for (Federation.Federate loopfederate : federateList) {
			if (loopfederate.getName().equals(federate.getConsistentUniqueReadableIdentifier())) {
				List<String> inboundGroups = loopfederate.getInboundGroup();

				if (inboundGroups.contains(groupIdentifier)) {
					throw new RuntimeException(server.getConsistentUniqueReadableIdentifier() + "already has an inbound federate group for " + federate.getConsistentUniqueReadableIdentifier() + " named '" + groupIdentifier + "'!");
				}
				inboundGroups.add(groupIdentifier);
				saveChanges();
				return;

			}
		}

		throw new RuntimeException("Cannot add inbound group for " + server.getConsistentUniqueReadableIdentifier() + "because the federate " + federate.getConsistentUniqueReadableIdentifier() + " has not been added!");
	}
}
