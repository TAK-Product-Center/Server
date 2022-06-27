package com.bbn.marti.takcl.AppModules;

import com.bbn.marti.takcl.AppModules.generic.AppModuleInterface;
import com.bbn.marti.takcl.SSLHelper;
import com.bbn.marti.takcl.cli.EndUserReadableException;
import com.bbn.marti.takcl.cli.simple.Command;
import com.bbn.marti.takcl.config.common.TakclRunMode;
import com.bbn.marti.takcl.connectivity.RunnableServerManager;
import com.bbn.marti.takcl.connectivity.implementations.UnifiedClient;
import com.bbn.marti.takcl.connectivity.interfaces.ClientResponseListener;
import com.bbn.marti.test.shared.CotGenerator;
import com.bbn.marti.test.shared.TestConnectivityState;
import com.bbn.marti.test.shared.data.GroupProfiles;
import com.bbn.marti.test.shared.data.generated.CLINonvalidatingReceivingUsers;
import com.bbn.marti.test.shared.data.generated.CLINonvalidatingSendingUsers;
import com.bbn.marti.test.shared.data.generated.CLINonvalidatingUsers;
import com.bbn.marti.test.shared.data.generated.ImmutableUsers;
import com.bbn.marti.test.shared.data.servers.CLIImmutableServerProfiles;
import org.jetbrains.annotations.NotNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertificateException;

/**
 * Used to maniuplate the simulated test environment, including setting up servers, federating servers,
 * adding and removing users, and sending messages from users.
 * <p>
 * Created on 12/2/15.
 */
public class EnvironmentHelperModule implements AppModuleInterface {

	public EnvironmentHelperModule() {
	}

	@NotNull
	@Override
	public TakclRunMode[] getRunModes() {
		return new TakclRunMode[]{TakclRunMode.LOCAL_SERVER_INTERACTION};
	}

	@Override
	public ServerState getRequiredServerState() {
		return ServerState.NOT_APPLICABLE;
	}

	@Override
	public String getCommandDescription() {
		return "Used to maniuplate the simulated test environment, including setting up servers, federating servers, adding and removing users, and sending messages from users.";
	}

	@Override
	public void init() {
	}

	/**
	 * Sets the directory created servers will be deployed to
	 *
	 * @param directoryPath The target directory servers will be deployed to
	 */
	@Command(description = "Sets the directory created servers will be deployed to.")
	@SuppressWarnings("unused")
	public void setServerFarmDirectory(String directoryPath) {
		TAKCLConfigModule conf = TAKCLConfigModule.getInstance();
		conf.setServerFarmDir(directoryPath.endsWith("/") ? directoryPath : directoryPath + "/");
		try {
			if (!Files.exists(Paths.get(directoryPath))) {
				Files.createDirectory(Paths.get(directoryPath));
			}
		} catch (IOException e) {
			System.err.println("Unable to create new directory! directory will not be changed in configuration and will remain '" + conf.getServerFarmDir() + "'!");
		}
		conf.saveChanges();
		System.out.println("Server farm directory changed to '" + directoryPath + "' successfully!");
	}

	/**
	 * Gets the directory created servers will be deployed to
	 *
	 * @return The target directory servers will be deployed to
	 */
	@Command(description = "Gets the directory created servers will be deployed to.")
	@SuppressWarnings("unused")
	public String getServerFarmDirectory() {
		return TAKCLConfigModule.getInstance().getServerFarmDir();
	}

	/**
	 * Deploys a TAKServer instance with the specified profile, removing existing files first
	 *
	 * @param serverIdentifier The server profile to construct the server based on
	 */
	@Command(description = "Deploys a TAKServer instance with the specified profile, removing existing files first.")
	@SuppressWarnings("unused")
	public void constructServer(@NotNull CLIImmutableServerProfiles serverIdentifier) {
		RunnableServerManager.cloneFromModelServer(serverIdentifier.getServer());
	}

	/**
	 * Adds the specified connection, enables file authuser if necessary, and adds the user if necessary
	 *
	 * @param user The user to add
	 */
	@Command(description = "Adds the specified connection, enables file authuser if necessary, and adds the user if necessary.")
	@SuppressWarnings("unused")
	public void addUpdateUserAndConnection(@NotNull CLINonvalidatingUsers user) throws EndUserReadableException {
		ImmutableUsers actualuser = user.getUser();

		OfflineConfigModule offlineConfigModule = new OfflineConfigModule();
		offlineConfigModule.init(actualuser.getServer());

		if (actualuser.getConnection().requiresAuthentication()) {
			offlineConfigModule.fileAuth(true);

			Path filepath = actualuser.getCertPublicPemPath();
			String fingerprint = SSLHelper.loadCertFingerprintForEndUserIfAvailable(actualuser);

			OfflineFileAuthModule offlineFileAuthModule = new OfflineFileAuthModule();
			offlineFileAuthModule.init(actualuser.getServer());
			offlineFileAuthModule.addUpdateUser(actualuser.getUserName(), actualuser.getPassword(), fingerprint,
					actualuser.getBaseGroupSetAccess() == null ? null : actualuser.getBaseGroupSetAccess().groupSet);
		}

		offlineConfigModule.addConnectionIfNecessary(actualuser.getConnection());
	}

	/**
	 * Does all the legwork to create a directional federation for a one-way connection from one server to another
	 *
	 * @param sourceServer    Te origin server
	 * @param targetServer    The target server
	 * @param groupIdentifier The group identifier to use on both servers for the connection
	 */
	@Command(description = "Does all the legwork to create a directional federation for a one-way connection from one server to another, with the same groupIdentifier used in each for simplicity.")
	@SuppressWarnings("unused")
	public void createDirectionalFederation(boolean useV1Federation, boolean useV2Federation, @NotNull CLIImmutableServerProfiles sourceServer, @NotNull CLIImmutableServerProfiles targetServer, @NotNull String groupIdentifier) {
		OfflineConfigModule sourceConfig = new OfflineConfigModule();
		OfflineConfigModule targetConfig = new OfflineConfigModule();

		sourceConfig.init(sourceServer.getServer());
		targetConfig.init(targetServer.getServer());

		if (!sourceConfig.isServerFederated()) {
			sourceConfig.enableFederationServer(useV1Federation, useV2Federation);
		}

		if (!targetConfig.isServerFederated()) {
			targetConfig.enableFederationServer(useV1Federation, useV2Federation);
		}

		if (!(sourceConfig.containsFederationOutgoing(targetServer.getServer()) && sourceConfig.federationOutgoingIsEnabled(targetServer.getServer())) &&
				!(targetConfig.containsFederationOutgoing(sourceServer.getServer()) && targetConfig.federationOutgoingIsEnabled(sourceServer.getServer()))) {
			sourceConfig.addFederationOutgoing(useV2Federation, targetServer.getServer());
		}

		if (!sourceConfig.containsFederate(targetServer.getServer())) {
			sourceConfig.addFederate(targetServer.getServer());
		}

		if (!targetConfig.containsFederate(sourceServer.getServer())) {
			targetConfig.addFederate(sourceServer.getServer());
		}

		if (!sourceConfig.containsFederateOutboundGroup(targetServer.getServer(), groupIdentifier)) {
			sourceConfig.addFederateOutboundGroup(targetServer.getServer(), groupIdentifier);
		}

		if (!targetConfig.containsFederateInboundGroup(sourceServer.getServer(), groupIdentifier)) {
			targetConfig.addFederateInboundGroup(sourceServer.getServer(), groupIdentifier);
		}
	}


	/**
	 * Does all the legwork to create a bidirectional federation for a two-way communication
	 *
	 * @param serverA         One of the servers to federate
	 * @param serverB         One of the servers to federate
	 * @param groupIdentifier The group identifier to use on both servers for the connection
	 */
	@Command(description = "Does all the legwork to create a bidirectional federation for a two-way connection from one server to another, with the same groupIdentifier used in each for simplicity.")
	@SuppressWarnings("unused")
	public void createBidirectionalFederation(boolean useV1Federation, boolean useV2Federation, @NotNull CLIImmutableServerProfiles serverA, @NotNull CLIImmutableServerProfiles serverB, @NotNull String groupIdentifier) {
		createDirectionalFederation(useV1Federation, useV2Federation, serverA, serverB, groupIdentifier);
		createDirectionalFederation(useV1Federation, useV2Federation, serverB, serverA, groupIdentifier);
	}

	/**
	 * Does all the legwork to create a bidirectional federation for a two-way communication
	 *
	 * @param serverA One of the servers to federate
	 * @param serverB One of the servers to federate
	 * @param group   The group set to use on both servers for the connection
	 */

	@Command(description = "Does all the legwork to create a directional federation for a one-way connection from one server to another, with the same groupIdentifier used in each for simplicity.")
	@SuppressWarnings("unused")
	public void createBidirectionalFederation(boolean useV1Federation, boolean useV2Federation, @NotNull CLIImmutableServerProfiles serverA, @NotNull CLIImmutableServerProfiles serverB, @NotNull GroupProfiles group) {
		createDirectionalFederation(useV1Federation, useV2Federation, serverA, serverB, group.name());
		createDirectionalFederation(useV1Federation, useV2Federation, serverB, serverA, group.name());
	}

	/**
	 * Sends a message from the user to their associated server.
	 *
	 * @param user The user to send from
	 */
	@Command(description = "Sends a message from the user to their associated server.")
	@SuppressWarnings("unused")
	public void sendMessageFromUser(@NotNull CLINonvalidatingSendingUsers user) {
		ImmutableUsers nvUser = user.getUser();

		UnifiedClient client = new UnifiedClient(nvUser);

		if (nvUser.getConnection().getProtocol().canConnect()) {
			client.connect(true, null);
		}

		client.sendMessage(CotGenerator.createMessage(nvUser));
	}

	/**
	 * Sends a latestSA from the user to their associated server.
	 *
	 * @param user The user to send from
	 * @return The human-readable result of the send attempt
	 */
	@Command(description = "Sends a latestSA from the user to their associated server.")
	@SuppressWarnings("unused")
	public String sendLatestSAFromUser(@NotNull CLINonvalidatingSendingUsers user) {
		ImmutableUsers nvUser = user.getUser();
		UnifiedClient client = new UnifiedClient(nvUser);

		if (nvUser.getConnection().getProtocol().canConnect()) {
			client.connect(true, null);
		}
		client.sendMessage(CotGenerator.createLatestSAMessage(nvUser));

		return userSendAttemptDisplayMessage(nvUser);
	}

	private String userSendAttemptDisplayMessage(ImmutableUsers nvUser) {
		if (nvUser.getConnection().getProtocol().canConnect()) {
			if (nvUser.getConnection().requiresAuthentication()) {
				return "Attempted connecting and sending message with username '" + nvUser.getUserName() +
						"' and password '" + nvUser.getPassword() + "' to port " + nvUser.getConnection().getPort() +
						" using protocol '" + nvUser.getConnection().getProtocol().getValue() + "'.";
			} else {
				return "Attempted connecting and sending message to port " + nvUser.getConnection().getPort() +
						" using protocol '" + nvUser.getConnection().getProtocol().getValue() + "'.";
			}
		} else {
			return "Attempted sending message to port " + nvUser.getConnection().getPort() +
					" using protocol '" + nvUser.getConnection().getProtocol().getValue() + "'.";
		}
	}

	/**
	 * Sends a latestSA with an image from the user to their associated server.
	 *
	 * @param user The user to send from
	 * @return The human-readable result of the send attempt
	 */
	@Command(description = "Sends the latestSA with a default image to the specified server using the specified user")
	@SuppressWarnings("unused")
	public String sendLatestSAFromUserWithImage(@NotNull CLINonvalidatingSendingUsers user) {
		ImmutableUsers nvUser = user.getUser();

		UnifiedClient client = new UnifiedClient(nvUser);

		if (nvUser.getConnection().getProtocol().canConnect()) {
			client.connect(true, null);
		}

		client.sendMessage(CotGenerator.createLatestSAMessageWithImage(nvUser));

		return userSendAttemptDisplayMessage(nvUser);
	}

	/**
	 * Receives messages as the specified user
	 *
	 * @param user The user to receive as
	 */
	@Command(description = " Receives messages as the specified user.")
	@SuppressWarnings("unused")
	public void receiveMessagesAsUser(@NotNull CLINonvalidatingReceivingUsers user) {
		ImmutableUsers nvUser = user.getUser();

		UnifiedClient client = new UnifiedClient(nvUser);
		client.addListener(new ClientResponseListener() {
			@Override
			public void onMessageReceived(String response) {
				System.out.println(toString() + " received \"" + response + "\"");
			}

			@Override
			public void onMessageSent(String message) {

			}

			@Override
			public void onConnectivityStateChange(TestConnectivityState state) {

			}
		});

		if (nvUser.getConnection().getProtocol().canConnect()) {
			client.connect(true, null);
		}
	}
}
