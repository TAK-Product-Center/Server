package com.bbn.marti.test.shared.data;

import com.bbn.marti.test.shared.data.connections.AbstractConnection;
import com.bbn.marti.test.shared.data.connections.BaseConnections;
import com.bbn.marti.test.shared.data.servers.ImmutableServerProfiles;
import com.bbn.marti.test.shared.data.users.BaseUsers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created on 3/11/16.
 */
public class DataUtil {

	public static final String NONVALIDATING_PREPEND = "NV_";


	public static final String generateConnectionName(@NotNull ImmutableServerProfiles server, @NotNull BaseConnections templateConnection, @Nullable String differentiationTag) {
		return server.getTag() + "_" + templateConnection.name() + (differentiationTag == null ? "" : differentiationTag);
	}


	public static final String generateUserName(@NotNull BaseUsers userModel, @NotNull boolean doValidation, @NotNull AbstractConnection connection, @Nullable String userDifferentiationTag) {
		return generateCustomUserName(userModel.name(), userModel.getBaseGroupSet(), doValidation, connection, userDifferentiationTag);
	}

	public static final String generateCustomUserName(@NotNull String userName, @NotNull GroupSetProfiles groupSet, @NotNull boolean doValidation, @NotNull AbstractConnection connection, @Nullable String userDifferentiationTag) {
		String groupTag = generateUserAccessTag(groupSet, connection);

		String identifier = (doValidation ? "" : NONVALIDATING_PREPEND) +
				connection.getConsistentUniqueReadableIdentifier() + "_" +
				userName + "_" + groupTag + ((userDifferentiationTag == null || userDifferentiationTag.equals("")) ? "" : ("_" + userDifferentiationTag));
		return identifier;
	}

	public static final String generateUserAccessTag(@NotNull GroupSetProfiles userDefinedGroupSet, @NotNull AbstractConnection connection) {
		if (connection.requiresAuthentication()) {
			return userDefinedGroupSet.getTag() + "f";
		} else {
			return connection.getGroupSet().getTag() + (connection.getAnonAccess() ? "t" : "f");
		}
	}
}
