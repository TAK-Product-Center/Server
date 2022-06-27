package com.bbn.marti.test.shared.data.servers;

/**
 * Multiple test sources are defined to allow multiple threads of events (different servers, abstract test
 * modifications, etc) to occur at once and verify against one another and reduce risk that they will conflict
 * with one another due to input/user/etc removals and modifications.
 * <p/>
 * Created on 10/22/15.
 */
public enum CLIImmutableServerProfiles {
	UNDEFINED(ImmutableServerProfiles.UNDEFINED), // Generic test source meant to be used for anything
	SERVER_0(ImmutableServerProfiles.SERVER_0), // Used for generated tests
	SERVER_1(ImmutableServerProfiles.SERVER_1), // Used for generated tests
	SERVER_2(ImmutableServerProfiles.SERVER_2), // Used for generated tests
	SERVER_CLI(ImmutableServerProfiles.SERVER_CLI); // Used for CLI tools

	private final ImmutableServerProfiles serverProfile;

	CLIImmutableServerProfiles(ImmutableServerProfiles server) {
		serverProfile = server;
	}

	public ImmutableServerProfiles getServer() {
		return serverProfile;
	}
}
