package com.bbn.marti.takcl.AppModules.generic;

import com.bbn.marti.takcl.cli.EndUserReadableException;
import com.bbn.marti.takcl.cli.advanced.ParamTag;
import com.bbn.marti.takcl.cli.advanced.ParameterizedCommand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface AdvancedFileAuthModuleInterface {
	@ParameterizedCommand(description = "Facilitates the viewing, addition, or modification of the users using their username.")
	String usermod(
			@ParamTag(optional = false, description = "The name of the user to be added or modified")
			@NotNull String username,

			@ParamTag(optional = true, shortSpecifier = "-D", longSpecifier = "--delete-user", isToggle = true,
					description = "Deletes the user. All other optional parameters will be ignored.")
			@Nullable Boolean delete,

			@ParamTag(optional = true, shortSpecifier = "-p", longSpecifier = "--password",
					description = "The password for the user. Password requirements: minimum of 15 characters to " +
							"include 1 uppercase, 1 lowercase, 1 number, and 1 special character from this list "
							+ "[-_!@#$%^&*(){}[]+=~`|:;<>,./\\?]. " +
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
}
