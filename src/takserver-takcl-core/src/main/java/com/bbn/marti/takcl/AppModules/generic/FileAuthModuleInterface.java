package com.bbn.marti.takcl.AppModules.generic;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.bbn.marti.takcl.cli.EndUserReadableException;
import com.bbn.marti.takcl.cli.advanced.ParamTag;
import com.bbn.marti.takcl.cli.advanced.ParameterizedCommand;
import com.bbn.marti.takcl.cli.simple.Command;
import com.bbn.marti.test.shared.data.generated.CLINonvalidatingUsers;
import com.bbn.marti.xml.bindings.Role;

/**
 * Created on 7/28/17.
 */
public interface FileAuthModuleInterface extends AdvancedFileAuthModuleInterface {

	@ParameterizedCommand(description = "Facilitates the viewing, addition, or modification of the users using their username.")
	String usermod(
			@ParamTag(optional = false, description = "The name of the user to be added or modified")
			@NotNull String username,

			@ParamTag(optional = true, shortSpecifier = "-D", longSpecifier = "--delete-user", isToggle = true,
					description = "Deletes the user. All other optional parameters will be ignored.")
			@Nullable Boolean delete,

			@ParamTag(optional = true, shortSpecifier = "-p", longSpecifier = "--password",
					description = "The password for the user. Password requirements: minimum of 15 characters to " +
					"include 1 uppercase, 1 lowercase, 1 number, and 1 special character from this list " +
							"[-_!@#$%^&*(){}[]+=~`|:;<>,./\\?]. " +
							"NOTE: you may need to surround the password string with single quotation marks ")
			@Nullable String password,

			@ParamTag(optional = true, shortSpecifier = "-c", longSpecifier = "--certificate",
					description = "The filepath to the certificate that contains the users fingerprint. If the fingerprint is specified as a command line option this will be ignored!")
			@Nullable String certpath,

			@ParamTag(optional = true, shortSpecifier = "-A", longSpecifier = "--administrator",
					isToggle = true, description = "Provides the user with administrative access")
			@Nullable Boolean administrator,

			@ParamTag(optional = true, shortSpecifier = "-f", longSpecifier = "--fingerprint",
					description = "The fingerprint the user can use to authenticate with")
			@Nullable String fingerprint,

			@ParamTag(optional = true, shortSpecifier = "-g", longSpecifier = "--group", allowedMultiple = true,
			description = "Specifies an 'in' and 'out' group permission for a user. This command sets a user permission to let the user read and write messages to the specified group. If no groups are specified with this or other group options, the user will be added to the anonymous group. Can be specified multiple times.")
			@Nullable String[] group,

			@ParamTag(optional = true, shortSpecifier = "-ig", longSpecifier = "--in-group", allowedMultiple = true,
			description = "Specifies an 'in' group permission for a user. This command sets a user permission to let the user write (but not necessarily read) messages to the specified group. If no groups are specified for a user with this or other group options, the user will be added to the anonymous group. Can be specified multiple times.")
			@Nullable String[] inGroup,

			@ParamTag(optional = true, shortSpecifier = "-og", longSpecifier = "--out-group", allowedMultiple = true,
			description = "Specifies an 'out' group permission for a user. This command sets a user permission to let the user read (but not necessarily write) messages from the specified group. If no groups are specified for a user with this or other group options, the user will be added to the anonymous group. Can be specified multiple times.")
			@Nullable String[] outGroup
	) throws EndUserReadableException;

	@ParameterizedCommand(description = "Facilitates the viewing, addition, or modification of the users using certificates.")
	String certmod(
			@ParamTag(optional = false,
					description = "The filepath to the certificate for this user")
			@NotNull String certpath,

			@ParamTag(optional = true, shortSpecifier = "-D", longSpecifier = "--delete-user", isToggle = true,
					description = "Deletes the user. All other optional parameters will be ignored.")
			@Nullable Boolean delete,

			@ParamTag(optional = true, shortSpecifier = "-p", longSpecifier = "--password",
					description = "The password for the user. Password requirements: minimum of 15 characters to " +
					"include 1 uppercase, 1 lowercase, 1 number, and 1 special character from this list " +
							"[-_!@#$%^&*(){}[]+=~`|:;<>,./\\?]. " +
							"NOTE: you may need to surround the password string with single quotation marks ")
			@Nullable String password,

			@ParamTag(optional = true, shortSpecifier = "-A", longSpecifier = "--administrator", isToggle = true,
					description = "Provides the user with administrative access")
			@Nullable Boolean administrator,

			@ParamTag(optional = true, shortSpecifier = "-f", longSpecifier = "--fingerprint",
					description = "Overrides the certificate fingerprint")
			@Nullable String fingerprint,

			@ParamTag(optional = true, shortSpecifier = "-g", longSpecifier = "--group", allowedMultiple = true,
			description = "Specifies an 'in' and 'out' group permission for a user. This command sets a user permission to let the user read and write messages to the specified group. If no groups are specified with this or other group options, the user will be added to the anonymous group. Can be specified multiple times.")
			@Nullable String[] group,

			@ParamTag(optional = true, shortSpecifier = "-ig", longSpecifier = "--in-group", allowedMultiple = true,
			description = "Specifies an 'in' group permission for a user. This command sets a user permission to let the user write (but not necessarily read) messages to the specified group. If no groups are specified for a user with this or other group options, the user will be added to the anonymous group. Can be specified multiple times.")
			@Nullable String[] inGroup,

			@ParamTag(optional = true, shortSpecifier = "-og", longSpecifier = "--out-group", allowedMultiple = true,
			description = "Specifies an 'out' group permission for a user. This command sets a user permission to let the user read (but not necessarily write) messages from the specified group. If no groups are specified for a user with this or other group options, the user will be added to the anonymous group. Can be specified multiple times.")
			@Nullable String[] outGroup
	) throws EndUserReadableException;

	/**
	 * Adds a user to the server. The password will automatically be encrypted.
	 *
	 * @param userIdentifier The user's username
	 * @param userPassword   The user's password
	 */
	@Command(description = "Adds or updates the user with the specified username with the specified password")
	String addOrUpdateUser(String userIdentifier, String userPassword) throws EndUserReadableException;

	/**
	 * Removes a user with the provided username from the server if they exist
	 *
	 * @param userName The username of the user to remove
	 */
	@Command(description = "Remove any number of users with the specified identifiers")
	String removeUsers(String... userName);

	/**
	 * Adds the specified existing user to the specified existing or non-existant group.
	 *
	 * @param groupIdentifier The group that may or may not already exist
	 * @param userName        The user identifier that must already exist as a user
	 */
	@Command(description = "Add any  number of users to a group if they are not already a member (The group will be created if necessary)")
	String addUsersToGroup(String groupIdentifier, String... userName);

	@Command(description = "Adds the users specified by the certificates to a group, adding them to the the file if they do not already exist.")
	String addCertificatesToGroup(String groupIdentifier, String... certificateFilepath);

	/**
	 * Adds the specified existing user to the specified existing or non-existant groups.
	 *
	 * @param userName        The user identifier that must already exist as a user
	 * @param groupIdentifier The groups that may or may not already exist
	 */
	@Command(description = "Add an existing user to any number of groups (The group will be created if necessary)")
	String addUserToGroups(String userName, String... groupIdentifier);


	@Command(description = "Adds the user certificate to the specified groups, creating the user if necessary.")
	String addCertificateToGroups(String certificateFilepath, String... groupIdentifier);

	/**
	 * Removes the specified existing user from the specified group(s)
	 *
	 * @param userName        The user to remove from a group
	 * @param groupIdentifier The group to remove the user from
	 */
	@Command(description = "Remove a user from any number of groups if they exist")
	String removeUserFromGroups(String userName, String... groupIdentifier);

	/**
	 * Removes the specified existing user from the specified group
	 *
	 * @param groupIdentifier The group to remove the user from
	 * @param userName        The user to remove from a group
	 */
	@Command(description = "Remove any number of users from a group if it exists")
	String removeUsersFromGroup(String groupIdentifier, String... userName);

	@Command(description = "Add any number of predefined users with an automatically generated password. Existing users will be ignored.", isDev = true)
	void addPredefinedUserAndGroups(@NotNull CLINonvalidatingUsers user);

	@Command(description = "Add any number of users with an automatically generated password. Existing users will be ignored.")
	String addUsers(String... userName);

	@Command(description = "Get the role for the indicated user")
	String getUserRole(String userName);

	@Command(description = "get a list of roles and the users associated with them")
	String getRoleList();


	/**
	 * Gets a list of all the users currently in the file auth mechanism
	 *
	 * @return All the users currently in the file auth mechanism
	 */
	@Command(description = "Gets the list of users and their group memberships")
	String getUserList();

	@Command(description = "Gets the list of groups and their associated users")
	String getGroupList();

	@Command(description = "Gets the user's associated fingerprint, if any")
	String getUserFingerprint(String userName) throws EndUserReadableException;

	@Command(description = "Set the role for the provided user")
	String setUserRole(String userName, Role role);

	@Command(description = "Sets the user's fingerprint")
	String setUserFingerprint(String username, String fingerprint);

	@Command(description = "Adds or updates user/fingerprint combinations represented by the certificates as admin users.")
	String addAdminCertificates(String... certificateFilepath);

	@Command(description = "Adds or updates user/fingerprint combination represented by the certificate as an admin and adds them to the specified groups.")
	String addAdminCertificateToGroups(String certificateFilepath, String... groupIdentifier);

	@Command(description = "Adds or updates user with the usernames and fingerprints on the specified certificates.")
	String addCertificates(String... certificateFilepath);
}
