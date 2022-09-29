package com.bbn.marti.test.shared.data.templates;

import com.bbn.marti.config.AuthType;
import com.bbn.marti.test.shared.data.DataUtil;
import com.bbn.marti.test.shared.data.GroupSetProfiles;
import com.bbn.marti.test.shared.data.connections.AbstractConnection;
import com.bbn.marti.test.shared.data.connections.BaseConnections;
import com.bbn.marti.test.shared.data.connections.ConnectionFilter;
import com.bbn.marti.test.shared.data.generated.ImmutableUsers;
import com.bbn.marti.test.shared.data.protocols.ProtocolProfiles;
import com.bbn.marti.test.shared.data.servers.AbstractServerProfile;
import com.bbn.marti.test.shared.data.servers.ImmutableServerProfiles;
import com.bbn.marti.test.shared.data.users.AbstractUser;
import com.bbn.marti.test.shared.data.users.UserFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Created on 10/22/15.
 */
public class ImmutableConnectionsTemplate_471E9257 extends AbstractConnection {
	private static final HashMap<String, ImmutableConnectionsTemplate_471E9257> valueMap = new HashMap<>();

	//////////////////////////
	// Begin Generated Users
	////////////////////////// 68EBC0C6-82C3-4C2F-9779-9191B5B432EC

	// This is also a conveneint place to put utility methods for generation since they will be wiped out in the generated file...

	public ImmutableConnectionsTemplate_471E9257(@NotNull String identifier, @NotNull ImmutableServerProfiles source, @NotNull BaseConnections modelInput, @NotNull int port) {
		this.consistentUniqueReadableIdentifier = identifier;
		this.server = source;
		this.protocol = modelInput.getProtocol();
		this.port = port;
		this.authType = modelInput.getAuthType();
		this.groupSet = modelInput.getGroupSet();
		this.isAnon = modelInput.getRawAnon();
		this.mcastGroup = modelInput.getMcastGroup();
		this.type = modelInput.getType();
		this.connectionModel = modelInput;

		valueMap.put(identifier, this);
	}

	public String generateDeclarationLine(@NotNull String newClassName, boolean omitServerTag, boolean omitNonvalidatingTag) {

		String declaredName = getConsistentUniqueReadableIdentifier();

		if (omitServerTag) {
			declaredName = declaredName.replace(server.getTag() + "_", "");
		}
		if (omitNonvalidatingTag) {
			declaredName = declaredName.replace(DataUtil.NONVALIDATING_PREPEND, "");
		}

		String beginning = "    public static final " + newClassName + " " + declaredName + " = new " + newClassName + "(\"" + getConsistentUniqueReadableIdentifier() + "\", ImmutableServerProfiles." + this.getServer().getConsistentUniqueReadableIdentifier() + ", BaseConnections." + connectionModel.name() + ", " + this.getPort();

		String end = "";

		if (genUsersStrings != null) {
			for (String userIdentifier : genUsersStrings) {
				end += (", \"" + userIdentifier + "\"");
			}
		}
		end += ");";

		return beginning + end;
	}

	public void setUsers(@NotNull String... userIdentifers) {
		this.genUsersStrings = userIdentifers;
	}

	////////////////////////// BD6A4745-76B4-42DD-AE35-5C2251DD6301
	// End Generated Users
	//////////////////////////


	private static CopyOnWriteArraySet<AbstractConnection> valueSet;

	private CopyOnWriteArraySet<AbstractUser> genUserSet;

	@NotNull
	private final ImmutableServerProfiles server;
	@NotNull
	private final ProtocolProfiles protocol;
	@NotNull
	private final int port;
	@NotNull
	private final AuthType authType;
	@Nullable
	private final Boolean isAnon;
	@NotNull
	private final GroupSetProfiles groupSet;
	@NotNull
	private final String consistentUniqueReadableIdentifier;
	@NotNull
	private final BaseConnections connectionModel;
	@Nullable
	private final String mcastGroup;
	@Nullable
	private final String type;
	private String[] genUsersStrings;


	public BaseConnections getConnectionModel() {
		return connectionModel;
	}

	private ImmutableConnectionsTemplate_471E9257(@NotNull String identifier, @NotNull ImmutableServerProfiles source, @NotNull BaseConnections modelInput, @NotNull int port, @NotNull String... genUserNameList) {
		this.consistentUniqueReadableIdentifier = identifier;
		this.server = source;
		this.protocol = modelInput.getProtocol();
		this.port = port;
		this.authType = modelInput.getAuthType();
		this.groupSet = modelInput.getGroupSet();
		this.isAnon = modelInput.getRawAnon();
		this.mcastGroup = modelInput.getMcastGroup();
		this.type = modelInput.getType();
		this.genUsersStrings = genUserNameList;
		this.connectionModel = modelInput;

		valueMap.put(identifier, this);
	}

	public static CopyOnWriteArraySet<AbstractConnection> valueSet() {
		return valueSet;
	}

	public static CopyOnWriteArraySet<AbstractConnection> valuesFiltered(@NotNull ConnectionFilter filter) {
		initStaticValuesIfNecessary();

		return filter.filterConnections(valueSet);
	}

	public static ImmutableConnectionsTemplate_471E9257 valueOf(@NotNull String key) {
		return valueMap.get(key);
	}

	public static ImmutableConnectionsTemplate_471E9257[] values() {
		return (ImmutableConnectionsTemplate_471E9257[]) valueMap.values().toArray();
	}


	private static synchronized void initStaticValuesIfNecessary() {
		if (!valueMap.isEmpty()) {
			if (valueSet == null) {
				valueSet = new CopyOnWriteArraySet<AbstractConnection>(valueMap.values());
			}
		}
	}

	private synchronized void initValuesIfNecessary() {
		if (genUsersStrings != null) {
			if (genUserSet == null) {
				Set<AbstractUser> userSet = new HashSet<>();

				for (int i = 0; i < genUsersStrings.length; i++) {
					String username = genUsersStrings[i];
					ImmutableUsers user = ImmutableUsers.valueOf(username);
					userSet.add(user);
				}
				genUserSet = new CopyOnWriteArraySet<>(userSet);
			}
			genUsersStrings = null;
		}
	}

	public synchronized CopyOnWriteArraySet<AbstractUser> getUsers(@Nullable UserFilter filter) {
		initValuesIfNecessary();

		if (filter == null) {
			return genUserSet;
		} else {
			return filter.filterUsers(genUserSet);
		}
	}

	@Override
	public Boolean getRawAnonAccessFlag() {
		return isAnon;
	}

	@Override
	public ProtocolProfiles getProtocol() {
		return protocol;
	}

	@Override
	public int getPort() {
		return port;
	}

	@Override
	public AuthType getAuthType() {
		return authType;
	}

	@Override
	public GroupSetProfiles getGroupSet() {
		return groupSet;
	}

	@Override
	public String getMCastGroup() {
		return mcastGroup;
	}

	@Override
	public String getType() {
		return type;
	}
	
	@Override
	public String getConsistentUniqueReadableIdentifier() {
		return consistentUniqueReadableIdentifier;
	}

	@Override
	public String getDynamicName() {
		return consistentUniqueReadableIdentifier;
	}

	@Override
	public AbstractServerProfile getServer() {
		return server;
	}
}
