package com.bbn.marti.takcl;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;

public class TestExceptions {

	public static boolean parseEnvVarBool(@NotNull String envVarKey, boolean defaultValue) {
		if (System.getenv().containsKey(envVarKey)) {
			String value = System.getenv(envVarKey);
			System.out.println("Overriding test exception: " + envVarKey + "=" + value);
			return Boolean.getBoolean(value);
		} else {
			return defaultValue;
		}
	}

	// This relates to an issue where a client disconnecting wasn't sending the related status update as LatestSA
	// to clients that had previously recieved LatestSA
	public static boolean IGNORE_DISCONNECT_LATESTSA_FAILURES = parseEnvVarBool("IGNORE_DISCONNECT_LATESTSA_FAILURES", true);

	// Connecting to a server, waiting several seconds, and then sending authentication data fails with Core Network V2.
	// It is unconventional enough that this hack removes the delay for the scenario for Core Network V2. This also
	// surfaces as an issue where a message immediately following an authentication message (programmatically this
	// occurs when you construct a string with an auth message and the first LatestSA message and then send it).
	public static boolean FORCE_IMMEDIATE_AUTH = parseEnvVarBool("FORCE_IMMEDIATE_AUTH_FOR_CORE_V2", true);

	// As indicated, empty messages are ignored
	public static final boolean IGNORE_EMPTY_MESSAGES = parseEnvVarBool("IGNORE_EMPTY_MESSAGES", true);

	// Used to get around issue where closing Ignite instances causes issues starting new instances due to ThreadGroup
	// issues. Will be fixed in a future version of ignite.
	public static boolean DO_NOT_CLOSE_IGNITE_INSTANCES = parseEnvVarBool("DO_NOT_CLOSE_IGNITE_INSTANCES", true);

	// These cover the same issue where the groups are missing from the Mission API response when a mission is created
	// and all missions are fetched. This also applies to the endpoint to get all missions. The groups are listed when
	// a specific mission is fetched.
	public static boolean MISSION_IGNORE_GROUPS_MISSING_IN_ADD_REMOVE_RESPONSES =
			parseEnvVarBool("MISSION_IGNORE_GROUPS_MISSING_IN_ADD_REMOVE_RESPONSES", true);

	// For some reason, non-user-manager tests don't seem to work well using the takserver-core ignite configuration
	// As the usermanager tests use the takserver-core configuration, this does not indicate a server issue. But the
	// time necessary to investigate why this is necessary hasn't been taken.
	public static boolean USE_TAKCL_IGNITE_CONFIGURATION_AS_INDICATED =
			parseEnvVarBool("USE_TAKCL_IGNITE_CONFIGURATION_AS_INDICATED", true);

	// The MissionDataSyncInterface.addMissionContents does not hydrate UID objects for the return call
	public static boolean MISSION_IGNORE_ADD_RESOURCE_RESPONSE_MISSING_UID_OJBECTS =
			parseEnvVarBool("MISSION_IGNORE_ADD_RESOURCE_RESPONSE_MISSING_UID_OJBECTS", true);

	// If the default role is read only, an admin cannot send CoT data to the mission. This is compounted by the fact
	// that an admin can't have it's group changed, and has no way to send CoT data to the mission.
	public static boolean MISSION_IGNORE_ADMIN_COT_WHEN_DEFAULT_ROLE_IS_READONLY =
			parseEnvVarBool("MISSION_IGNORE_ADMIN_COT_WHEN_DEFAULT_ROLE_IS_READONLY", true);
}
