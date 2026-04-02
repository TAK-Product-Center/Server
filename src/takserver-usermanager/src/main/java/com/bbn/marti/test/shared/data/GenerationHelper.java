package com.bbn.marti.test.shared.data;

import com.bbn.marti.config.AuthType;
import com.bbn.marti.takcl.AppModules.TAKCLConfigModule;
import com.bbn.marti.takcl.AppModules.generic.AppModuleInterface;
import com.bbn.marti.takcl.cli.simple.Command;
import com.bbn.marti.takcl.config.common.TakclRunMode;
import com.bbn.marti.test.shared.data.connections.BaseConnections;
import com.bbn.marti.test.shared.data.connections.ConnectionFilter;
import com.bbn.marti.test.shared.data.protocols.ProtocolProfileFilter;
import com.bbn.marti.test.shared.data.protocols.ProtocolProfiles;
import com.bbn.marti.test.shared.data.servers.ImmutableServerProfiles;
import com.bbn.marti.test.shared.data.templates.ImmutableConnectionsTemplate_471E9257;
import com.bbn.marti.test.shared.data.templates.ImmutableUsersTemplate_47FA2889;
import com.bbn.marti.test.shared.data.templates.ReferencedProtocolProfilesInputTemplate_A26D0F85;
import com.bbn.marti.test.shared.data.users.AbstractUser;
import com.bbn.marti.test.shared.data.users.BaseUsers;
import com.bbn.marti.test.shared.data.users.UserFilter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 */
public class GenerationHelper implements AppModuleInterface {

	private static final String TEMPLATE_GEN_BEGIN_KEY = "68EBC0C6-82C3-4C2F-9779-9191B5B432EC";
	private static final String TEMPLATE_GEN_END_KEY = "BD6A4745-76B4-42DD-AE35-5C2251DD6301";

	private static final String CLASS_REFERENCE_USER_TEMPLATE = "ReferencedUserTemplate_58F784B5";
	private static final String CLASS_IMMUTABLE_CONNECTIONS_TEMPLATE = "ImmutableConnectionsTemplate_471E9257";
	private static final String CLASS_IMMUTABLE_USERS_TEMPLATE = "ImmutableUsersTemplate_47FA2889";
	private static final String CLASS_REFERENCE_PROTOCOL_TEMPLATE = "ReferencedProtocolProfilesInputTemplate_A26D0F85";

	private final String GEN_IMMUTABLE_USERS_IDENTIFIER = "ImmutableUsers";
	private final String GEN_IMMUTABLE_CONNECTIONS_IDENTIFIER = "ImmutableConnections";
	private final String GEN_VALIDATING_USERS_IDENTIFIER = "ValidatingUsers";
	private final String GEN_NONVALIDATING_USERS_IDENTIFIER = "NonvalidatingUsers";
	private final String GEN_CLI_NONVALIDATING_SENDING_USERS_IDENTIFIER = "CLINonvalidatingSendingUsers";
	private final String GEN_CLI_NONVALIDATING_RECEIVING_USERS_IDENTIFIER = "CLINonvalidatingReceivingUsers";
	private final String GEN_CLI_NONVALIDATING_USERS_IDENTIFIER = "CLINonvalidatingUsers";
	private final String GEN_CLI_SENDING_PROTOCOLS = "CLISendingInputProtocols";
	private final String GEN_CLI_RECEIVING_PROTOCOLS = "CLIReceivingInputProtocols";

	private static final int START_PORT = 17730;
	private static int nextPort = START_PORT;

	private static final GroupSetProfiles[] groupSets = GroupSetProfiles.values();
	public static final int groupSetsLength = groupSets.length;

	private static final BaseConnections[] templateConnections = BaseConnections.values();
	private static final BaseUsers[] templateUsers = BaseUsers.values();

	// This causes multiple instances of a given connection type to be generated. Be sure to include an empty string in the options if you want the default identifier included!
	private static Map<BaseConnections, List<String>> connectionMultiplierMap = new HashMap<>();

	static {
		List stcpList = new ArrayList(2);
		stcpList.add("");
		stcpList.add("A");
		connectionMultiplierMap.put(BaseConnections.stcp, stcpList);
		connectionMultiplierMap.put(BaseConnections.authssl, stcpList);
		connectionMultiplierMap.put(BaseConnections.authstcp, stcpList);
		connectionMultiplierMap.put(BaseConnections.saproxy, stcpList);
		connectionMultiplierMap.put(BaseConnections.stcp, stcpList);
		connectionMultiplierMap.put(BaseConnections.stcp, stcpList);
	}

	private static Map<BaseUsers, List<String>> userMultiplierMap = new HashMap<>();

	static {
		List anonList = new ArrayList(2);
		anonList.add("");
		anonList.add("A");
		anonList.add("B");
		userMultiplierMap.put(BaseUsers.anonuser, anonList);
	}


	public Map<String, ImmutableConnectionsTemplate_471E9257> generateConnectionDeclarationsForServer(@NotNull ImmutableServerProfiles server, @NotNull BaseConnections[] connections) {
		Map<String, ImmutableConnectionsTemplate_471E9257> returnMap = new HashMap<>();

		if (server != ImmutableServerProfiles.UNDEFINED) {
			for (BaseConnections connection : connections) {

				if (connectionMultiplierMap.containsKey(connection)) {
					for (String differentator : connectionMultiplierMap.get(connection)) {
						String connectionName = DataUtil.generateConnectionName(server, connection, differentator);
						returnMap.put(connectionName, new ImmutableConnectionsTemplate_471E9257(connectionName, server, connection, nextPort++));

					}
				} else {
					String connectionName = DataUtil.generateConnectionName(server, connection, null);
					returnMap.put(connectionName, new ImmutableConnectionsTemplate_471E9257(connectionName, server, connection, nextPort++));
				}
			}
		}

		return returnMap;
	}

	public Map<String, ImmutableUsersTemplate_47FA2889> generateValidUserDeclarations(@NotNull ImmutableConnectionsTemplate_471E9257 connection, @NotNull BaseUsers[] templateUsers) {
		Map<String, ImmutableUsersTemplate_47FA2889> returnMap = new HashMap<>();

		for (BaseUsers templateUser : templateUsers) {

			if ((connection.getAuthType() != AuthType.ANONYMOUS && templateUser.hasAuthCredentials()) ||
					(connection.getAuthType() == AuthType.ANONYMOUS && !templateUser.hasAuthCredentials())) {

				if (userMultiplierMap.containsKey(templateUser)) {
					for (String differentiator : userMultiplierMap.get(templateUser)) {
						String userName = DataUtil.generateUserName(templateUser, true, connection, differentiator);
						returnMap.put(userName, new ImmutableUsersTemplate_47FA2889(userName, templateUser, true, connection.getConsistentUniqueReadableIdentifier(), true));
						userName = DataUtil.generateUserName(templateUser, false, connection, differentiator);
						returnMap.put(userName, new ImmutableUsersTemplate_47FA2889(userName, templateUser, false, connection.getConsistentUniqueReadableIdentifier(), true));
					}
				} else {
					String userName = DataUtil.generateUserName(templateUser, true, connection, null);
					returnMap.put(userName, new ImmutableUsersTemplate_47FA2889(userName, templateUser, true, connection.getConsistentUniqueReadableIdentifier(), true));
					userName = DataUtil.generateUserName(templateUser, false, connection, null);
					returnMap.put(userName, new ImmutableUsersTemplate_47FA2889(userName, templateUser, false, connection.getConsistentUniqueReadableIdentifier(), true));
				}
			}
		}

		return returnMap;
	}


	public List<String> generateNonvalidatingRedirectionLines(Set<String> userIdentifiers) {
		List<String> userDeclarationLines = new LinkedList<>();

		for (String userIdentifier : userIdentifiers) {
			String userName = userIdentifier.replace(DataUtil.NONVALIDATING_PREPEND, "");

			String line = "    " + userName + "(\"" + userIdentifier + "\"),";
			userDeclarationLines.add(line);
		}
		return userDeclarationLines;
	}

	public void updateFileGeneratedData(@NotNull List<String> newItemLines, @NotNull String sourceClassName, @NotNull String targetClassName) {
		try {
			List<String> newLines = new ArrayList<>(newItemLines);

			String templatePackageIdentifier = TAKCLConfigModule.getInstance().getJavaTemplatePackage();
			String generatedPackageIdentifier = TAKCLConfigModule.getInstance().getJavaGenerationPackage();

			Path sourceFilePath = Paths.get(TAKCLConfigModule.getInstance().getJavaTemplatePath(), sourceClassName + ".java");
			// Read in the generated file


			List<String> processingLines = Files.readAllLines(sourceFilePath);
			int insertionPoint = -1;
			int deletionPoint = -1;

			// Get the start and end of the generated data
			for (int i = 0; i < processingLines.size(); i++) {
				if (processingLines.get(i).contains(TEMPLATE_GEN_BEGIN_KEY)) {
					insertionPoint = i + 1;
				} else if (processingLines.get(i).contains(TEMPLATE_GEN_END_KEY)) {
					deletionPoint = i;
				} else {
					if (processingLines.get(i).contains(CLASS_REFERENCE_USER_TEMPLATE)) {
						processingLines.set(i, processingLines.get(i).replaceAll(CLASS_REFERENCE_USER_TEMPLATE, targetClassName));
					}
					if (processingLines.get(i).contains(CLASS_IMMUTABLE_CONNECTIONS_TEMPLATE)) {
						processingLines.set(i, processingLines.get(i).replaceAll(CLASS_IMMUTABLE_CONNECTIONS_TEMPLATE, targetClassName));
					}
					if (processingLines.get(i).contains(CLASS_IMMUTABLE_USERS_TEMPLATE)) {
						processingLines.set(i, processingLines.get(i).replaceAll(CLASS_IMMUTABLE_USERS_TEMPLATE, targetClassName));
					}
					if (processingLines.get(i).contains(CLASS_REFERENCE_PROTOCOL_TEMPLATE)) {
						processingLines.set(i, processingLines.get(i).replaceAll(CLASS_REFERENCE_PROTOCOL_TEMPLATE, targetClassName));
					}
					if (processingLines.get(i).contains(templatePackageIdentifier)) {
						processingLines.set(i, processingLines.get(i).replaceAll(templatePackageIdentifier, generatedPackageIdentifier));
					}
				}
			}

			// Remove the generated data
			for (int i = insertionPoint; i < deletionPoint; i++) {
				processingLines.remove(insertionPoint);
			}

			String lastLine = newLines.get(newLines.size() - 1);
			lastLine = lastLine.substring(0, lastLine.length() - 1) + ";";
			newLines.set(newLines.size() - 1, lastLine);
			processingLines.addAll(insertionPoint, newLines);

			Path targetFilePath = Paths.get(TAKCLConfigModule.getInstance().getJavaGenerationPath(), targetClassName + ".java");
			Files.write(targetFilePath, processingLines);

			System.out.println("Generated content in '" + targetFilePath.toString() + "' updated successfully.");

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


	@NotNull
	@Override
	public TakclRunMode[] getRunModes() {
		return new TakclRunMode[]{TakclRunMode.LOCAL_SOURCE_INTERACTION};
	}

	@Override
	public ServerState getRequiredServerState() {
		return ServerState.NOT_APPLICABLE;
	}

	@Override
	public String getCommandDescription() {
		return "Used to generate test enum definitions.";
	}

	@Override
	public void init() {
	}

	@Command
	public void generateTestData() {
		int generationCount = 2;
		int startPort = 17730;


		Map<String, ImmutableConnectionsTemplate_471E9257> generatedConnections = new HashMap<>();
		Map<String, ImmutableUsersTemplate_47FA2889> generatedUsers = new HashMap<>();

		// Iterate through each server
		for (ImmutableServerProfiles serverProfile : ImmutableServerProfiles.valueSet()) {

			// Generate the connections
			if (serverProfile != ImmutableServerProfiles.UNDEFINED) {
				generatedConnections.putAll(generateConnectionDeclarationsForServer(serverProfile, templateConnections));
			}

			// Genrate the users
			for (ImmutableConnectionsTemplate_471E9257 connection : generatedConnections.values()) {
				Map<String, ImmutableUsersTemplate_47FA2889> userMap = generateValidUserDeclarations(connection, templateUsers);

				connection.setUsers(userMap.keySet().toArray(new String[0]));

				generatedUsers.putAll(userMap);
			}
		}


		// Write the users to the main GenUsers file
		List<String> userDeclarationLines = new LinkedList<>();
		for (ImmutableUsersTemplate_47FA2889 user : generatedUsers.values()) {
			userDeclarationLines.add(user.generateDeclarationLine(GEN_IMMUTABLE_USERS_IDENTIFIER));
		}
		Collections.sort(userDeclarationLines);
		updateFileGeneratedData(userDeclarationLines, CLASS_IMMUTABLE_USERS_TEMPLATE, GEN_IMMUTABLE_USERS_IDENTIFIER);


		// Write the connections to the main GenConnections file
		List<String> connectionDeclarationLines = new LinkedList<>();
		for (ImmutableConnectionsTemplate_471E9257 connection : generatedConnections.values()) {
			connectionDeclarationLines.add(connection.generateDeclarationLine(GEN_IMMUTABLE_CONNECTIONS_IDENTIFIER, false, false));
		}
		Collections.sort(userDeclarationLines);
		updateFileGeneratedData(connectionDeclarationLines, CLASS_IMMUTABLE_CONNECTIONS_TEMPLATE, GEN_IMMUTABLE_CONNECTIONS_IDENTIFIER);


		// Write the validating users file
		UserFilter validatingUserFilter = new UserFilter().setUserActivityIsValidated(true);
		List<String> validatingUserDeclarationLines = new LinkedList<>();
		for (AbstractUser user : ImmutableUsersTemplate_47FA2889.valuesFiltered(validatingUserFilter)) {
			validatingUserDeclarationLines.add(((ImmutableUsersTemplate_47FA2889) user).generateReferenceLine(true));
		}
		Collections.sort(validatingUserDeclarationLines);
		updateFileGeneratedData(validatingUserDeclarationLines, CLASS_REFERENCE_USER_TEMPLATE, GEN_VALIDATING_USERS_IDENTIFIER);


		// Write the nonvalidating users file
		UserFilter nonvalidatingUserFilter = new UserFilter().setUserActivityIsValidated(false);
		List<String> nonvalidatingUserDeclarationLines = new LinkedList<>();
		for (AbstractUser user : ImmutableUsersTemplate_47FA2889.valuesFiltered(nonvalidatingUserFilter)) {
			nonvalidatingUserDeclarationLines.add(((ImmutableUsersTemplate_47FA2889) user).generateReferenceLine(true));
		}
		Collections.sort(nonvalidatingUserDeclarationLines);
		updateFileGeneratedData(nonvalidatingUserDeclarationLines, CLASS_REFERENCE_USER_TEMPLATE, GEN_NONVALIDATING_USERS_IDENTIFIER);


		// Write the cli nonvalidating users file
		UserFilter cliNonvalidatingUserFilter = new UserFilter().setUserActivityIsValidated(false);
		List<String> cliNonvalidatingUserDeclarationLines = new LinkedList<>();
		for (AbstractUser user : ImmutableUsersTemplate_47FA2889.valuesFiltered(cliNonvalidatingUserFilter)) {
			cliNonvalidatingUserDeclarationLines.add(((ImmutableUsersTemplate_47FA2889) user).generateReferenceLine(false));
		}
		Collections.sort(cliNonvalidatingUserDeclarationLines);
		updateFileGeneratedData(cliNonvalidatingUserDeclarationLines, CLASS_REFERENCE_USER_TEMPLATE, GEN_CLI_NONVALIDATING_USERS_IDENTIFIER);


		// Write the cli sending protocols and cli nonvalidating sending users files
		ProtocolProfiles[] sendingProtocols = new ProtocolProfileFilter().setCanSend(true).filterProtocolProfiles(ProtocolProfiles.values());
		List<String> cliSendingProtocolsDeclarationLines = new LinkedList<>();
		for (ProtocolProfiles protocol : sendingProtocols) {
			cliSendingProtocolsDeclarationLines.add(ReferencedProtocolProfilesInputTemplate_A26D0F85.generateReferenceLine(protocol));
		}
		Collections.sort(cliSendingProtocolsDeclarationLines);
		updateFileGeneratedData(cliSendingProtocolsDeclarationLines, CLASS_REFERENCE_PROTOCOL_TEMPLATE, GEN_CLI_SENDING_PROTOCOLS);


		UserFilter cliNonvalidatingSendingUserFilter = new UserFilter()
				.setUserActivityIsValidated(false)
				.setConnectionFilter(new ConnectionFilter().addProtocolProfiles(sendingProtocols));

		List<String> cliNonvalidatingSendingUserDeclarationLines = new LinkedList<>();
		for (AbstractUser user : ImmutableUsersTemplate_47FA2889.valuesFiltered(cliNonvalidatingSendingUserFilter)) {
			cliNonvalidatingSendingUserDeclarationLines.add(((ImmutableUsersTemplate_47FA2889) user).generateReferenceLine(false));
		}
		Collections.sort(cliNonvalidatingSendingUserDeclarationLines);
		updateFileGeneratedData(cliNonvalidatingSendingUserDeclarationLines, CLASS_REFERENCE_USER_TEMPLATE, GEN_CLI_NONVALIDATING_SENDING_USERS_IDENTIFIER);


		// Write the cli nonvalidating receiving users file
		ProtocolProfiles[] receivingProtocols = new ProtocolProfileFilter().setCanReceive(true).filterProtocolProfiles(ProtocolProfiles.values());
		List<String> cliReceivingProtocolsDeclarationLines = new LinkedList<>();
		for (ProtocolProfiles protocol : receivingProtocols) {
			cliReceivingProtocolsDeclarationLines.add(ReferencedProtocolProfilesInputTemplate_A26D0F85.generateReferenceLine(protocol));
		}
		Collections.sort(cliSendingProtocolsDeclarationLines);
		updateFileGeneratedData(cliSendingProtocolsDeclarationLines, CLASS_REFERENCE_PROTOCOL_TEMPLATE, GEN_CLI_RECEIVING_PROTOCOLS);


		UserFilter cliNonvalidatingReceivingUserFilter = new UserFilter()
				.setUserActivityIsValidated(false)
				.setConnectionFilter(new ConnectionFilter().addProtocolProfiles(receivingProtocols));

		List<String> cliNonvalidatingReceivingUserDeclarationLines = new LinkedList<>();
		for (AbstractUser user : ImmutableUsersTemplate_47FA2889.valuesFiltered(cliNonvalidatingReceivingUserFilter)) {
			cliNonvalidatingReceivingUserDeclarationLines.add(((ImmutableUsersTemplate_47FA2889) user).generateReferenceLine(false));
		}
		Collections.sort(cliNonvalidatingReceivingUserDeclarationLines);
		updateFileGeneratedData(cliNonvalidatingReceivingUserDeclarationLines, CLASS_REFERENCE_USER_TEMPLATE, GEN_CLI_NONVALIDATING_RECEIVING_USERS_IDENTIFIER);

	}

}
