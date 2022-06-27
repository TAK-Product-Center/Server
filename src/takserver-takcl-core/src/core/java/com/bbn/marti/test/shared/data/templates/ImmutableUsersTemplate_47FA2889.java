package com.bbn.marti.test.shared.data.templates;

import com.bbn.marti.test.shared.data.DataUtil;
import com.bbn.marti.test.shared.data.GroupSetProfiles;
import com.bbn.marti.test.shared.data.connections.AbstractConnection;
import com.bbn.marti.test.shared.data.generated.ImmutableConnections;
import com.bbn.marti.test.shared.data.servers.AbstractServerProfile;
import com.bbn.marti.test.shared.data.users.AbstractUser;
import com.bbn.marti.test.shared.data.users.BaseUsers;
import com.bbn.marti.test.shared.data.users.UserFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Created on 10/22/15.
 */
public class ImmutableUsersTemplate_47FA2889 extends AbstractUser {
	private final static HashMap<String, ImmutableUsersTemplate_47FA2889> valueMap = new HashMap<>();

	//////////////////////////
	// Begin Generated Users
	////////////////////////// 68EBC0C6-82C3-4C2F-9779-9191B5B432EC

	// This is also a conveneint place to put utility methods for generation since they will be wiped out in the generated file...

	public ImmutableUsersTemplate_47FA2889(@NotNull String userIdentifier, @NotNull BaseUsers userModel, boolean doValidation, @NotNull String generatedConnectionIdentifier, @NotNull boolean thisIsFromTheGenerator) {
		if (!thisIsFromTheGenerator) {
			throw new RuntimeException("As a boolean to change the signature of the public variant of this message that should bbe generated out, you should know to set it to true...");
		}

		this.userIdentifier = userIdentifier;
		this.username = userModel.getUserName();
		this.password = userModel.getPassword();
		this.groupSet = userModel.getBaseGroupSet();
		this.certPrivateJksPath = userModel.getCertPrivateJksPath();
		this.certPublicPemPath = userModel.getCertPublicPemPath();
		this.genConnectionsIdentifier = generatedConnectionIdentifier;
		this.doValidation = doValidation;
		this.userModel = userModel;
		valueMap.put(userIdentifier, this);
	}

	public BaseUsers getUserModel() {
		return this.userModel;
	}

	public String generateDeclarationLine(@NotNull String newClassName) {
		return "    public static final " + newClassName + " " + getConsistentUniqueReadableIdentifier() + " = new " + newClassName + "(\"" + getConsistentUniqueReadableIdentifier() + "\", BaseUsers." + userModel.name() + ", " + (doValidation() ? "true, " : "false, ") + "\"" + genConnectionsIdentifier + "\");";
	}

	public String generateReferenceLine(boolean indicateNonvalidatingIfNonvalidating) {
		String name = getConsistentUniqueReadableIdentifier();

		if (!indicateNonvalidatingIfNonvalidating && name.startsWith(DataUtil.NONVALIDATING_PREPEND)) {
			name = name.substring(DataUtil.NONVALIDATING_PREPEND.length(), name.length());
		}
		return "    " + name + "(\"" + getConsistentUniqueReadableIdentifier() + "\"),";
	}

	////////////////////////// BD6A4745-76B4-42DD-AE35-5C2251DD6301
	// End Generated Users
	//////////////////////////


	private static CopyOnWriteArraySet<AbstractUser> valueSet;

	@NotNull
	private final String userIdentifier;
	@Nullable
	private final String username;
	@Nullable
	private final String password;
	@Nullable
	private final Path certPrivateJksPath;
	@Nullable
	private final Path certPublicPemPath;
	@NotNull
	private final GroupSetProfiles groupSet;
	@NotNull
	private final BaseUsers userModel;
	@NotNull
	private final String genConnectionsIdentifier;
	private final boolean doValidation;
	private ImmutableConnections connection;

	private ImmutableUsersTemplate_47FA2889(@NotNull String userIdentifier, @NotNull BaseUsers userModel, boolean doValidation, @NotNull String generatedConnectionIdentifier) {
		this.userIdentifier = userIdentifier;
		this.username = userModel.getUserName();
		this.password = userModel.getPassword();
		this.groupSet = userModel.getBaseGroupSet();
		this.certPrivateJksPath = userModel.getCertPrivateJksPath();
		this.certPublicPemPath = userModel.getCertPublicPemPath();
		this.genConnectionsIdentifier = generatedConnectionIdentifier;
		this.doValidation = doValidation;
		this.userModel = userModel;
		valueMap.put(userIdentifier, this);
	}

	private static synchronized void initStaticValuesIfNecessary() {
		if (!valueMap.isEmpty()) {
			if (valueSet == null) {
				valueSet = new CopyOnWriteArraySet<AbstractUser>(valueMap.values());
			}
		}
	}

	public static CopyOnWriteArraySet<AbstractUser> valuesFiltered(@NotNull UserFilter filter) {
		initStaticValuesIfNecessary();

		return filter.filterUsers(valueSet);
	}

	public static CopyOnWriteArraySet<AbstractUser> valueSet() {
		initStaticValuesIfNecessary();
		return valueSet;
	}

	public static ImmutableUsersTemplate_47FA2889 valueOf(@NotNull String key) {
		return valueMap.get(key);
	}

	public Collection<ImmutableUsersTemplate_47FA2889> values() {
		return valueMap.values();
	}

	@Override
	public final String getUserName() {
		return username;
	}

	@Override
	public final String getPassword() {
		return password;
	}

	@Override
	public final Path getCertPublicPemPath() {
		return certPublicPemPath;
	}

	@Override
	public final Path getCertPrivateJksPath() {
		return certPrivateJksPath;
	}

	@Override
	public String getDynamicName() {
		return getConsistentUniqueReadableIdentifier();
	}

	@Override
	public final AbstractServerProfile getServer() {
		return getConnection().getServer();
	}

	@Override
	public final AbstractConnection getConnection() {
		if (connection == null) {
			connection = ImmutableConnections.valueOf(genConnectionsIdentifier);
		}
		return connection;
	}

	@Override
	public final boolean doValidation() {
		return doValidation;
	}

	@Override
	public final GroupSetProfiles getDefinedGroupSet() {
		return groupSet;
	}

	@Override
	public final String getConsistentUniqueReadableIdentifier() {
		return userIdentifier;
	}
}
