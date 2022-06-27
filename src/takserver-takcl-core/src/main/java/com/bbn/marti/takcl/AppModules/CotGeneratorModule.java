package com.bbn.marti.takcl.AppModules;

import com.bbn.marti.takcl.AppModules.generic.AppModuleInterface;
import com.bbn.marti.takcl.cli.simple.Command;
import com.bbn.marti.takcl.config.common.TakclRunMode;
import com.bbn.marti.test.shared.CotGenerator;
import com.bbn.marti.test.shared.data.users.BaseUsers;
import org.jetbrains.annotations.NotNull;

/**
 * Used to generate cot messages
 *
 */
public class CotGeneratorModule implements AppModuleInterface {

	@NotNull
	@Override
	public TakclRunMode[] getRunModes() {
		return new TakclRunMode[0];
	}

	@Override
	public ServerState getRequiredServerState() {
		return ServerState.NOT_APPLICABLE;
	}

	@Override
	public String getCommandDescription() {
		return "Used to generate a number of different types of Cot messages based on the provided user details.";
	}

	@Override
	public void init() {
	}

	/**
	 * Generates an auth message with the provided username and password. The username will also be used as the uid.
	 *
	 * @param username The username
	 * @param password The password
	 * @return The auth message formatted as an XML string
	 */
	@Command(description = "Generates an auth message with the provided username and password")
	public String generateAuthMessage(@NotNull String username, @NotNull String password) {
		return CotGenerator.createAuthMessage(username, password, username).asXML();
	}

	/**
	 * Generates an auth message with the provided user's credentials. The username will also be used for the uid.
	 *
	 * @param user The user to get the credentials from
	 * @return The auth message formatted as an XML string
	 */
	@Command(description = "Generates an auth message with the provided user's credentials")
	public String generateAuthMessage(@NotNull BaseUsers user) {
		return CotGenerator.createAuthMessage(user.getUserName(), user.getPassword(), user.getUserName()).asXML();
	}

	/**
	 * Generates a generic non-latestSA message with the provided uid and a random US location
	 *
	 * @param uid The uid to utilize
	 * @return THe xml formatted message
	 */
	@Command(description = "Generates a non-latestSA xml message")
	public String generateMessage(@NotNull String uid) {
		return CotGenerator.createMessage(uid).asXML();
	}

	/**
	 * Generates a non-latestSA message with the provided user's username and a random US location. The username will also be used for the uid.
	 *
	 * @param user The user to get the username from
	 * @return The xml formatted message
	 */
	@Command(description = "Generates a message using the provided user's details and a random US location")
	public String generateMessage(@NotNull BaseUsers user) {
		return CotGenerator.createMessage(user.getUserName()).asXML();
	}

	/**
	 * Generates a latestSA message with the provided user's details and a random US location.
	 * The uid will be the user's username
	 * The endpoint will be "&lt;username&gt;_endpoint"
	 * The group will be "&lt;username&gt;_group
	 *
	 * @param user The user to generate the latestSA message from
	 * @return The latestSA xml Message
	 */
	@Command(description = "Generates a LatestSA message using the provided user's details and a random US location")
	public String generateLatestSA(@NotNull BaseUsers user) {
		return CotGenerator.createLatestSAMessage(user.getUserName(), user.getUserName() + "_endpoint", user.getUserName() + "_group").asXML();
	}

	/**
	 * Generates a latestSA message with the provided user's details
	 *
	 * @param userNameUid The value to use for the user's uid and username
	 * @param endpoint    The value to use for the endpoint
	 * @param group       The value to use for the group
	 * @return The xml-formatted latestSA message
	 */
	@Command(description = "Generates a LatestSA message using the provided details and a random US location")
	public String generateLatestSA(@NotNull String userNameUid, @NotNull String endpoint, @NotNull String group) {
		return CotGenerator.createLatestSAMessage(userNameUid, endpoint, group).asXML();
	}


}
