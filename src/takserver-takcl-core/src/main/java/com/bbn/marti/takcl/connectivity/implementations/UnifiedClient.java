package com.bbn.marti.takcl.connectivity.implementations;

import com.bbn.marti.takcl.connectivity.interfaces.ClientResponseListener;
import com.bbn.marti.takcl.connectivity.interfaces.ConnectingInterface;
import com.bbn.marti.takcl.connectivity.interfaces.ReceivingInterface;
import com.bbn.marti.takcl.connectivity.interfaces.SendingInterface;
import com.bbn.marti.takcl.connectivity.missions.MissionDataSyncClient;
import com.bbn.marti.test.shared.TestConnectivityState;
import com.bbn.marti.test.shared.data.protocols.ProtocolProfilesInterface;
import com.bbn.marti.test.shared.data.users.AbstractUser;
import org.dom4j.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;

/**
 * Useless client
 * <p>
 * Created on 11/13/15.
 */
public class UnifiedClient implements ConnectingInterface, SendingInterface, ReceivingInterface {

	private ClientResponseListener innerActivityListener = new ClientResponseListener() {
		@Override
		public void onMessageReceived(String response) {
			synchronized (listeners) {
				for (ClientResponseListener listener : listeners) {
					listener.onMessageReceived(response);
				}
			}
		}

		@Override
		public void onMessageSent(String message) {
			synchronized (listeners) {
				for (ClientResponseListener listener : listeners) {
					listener.onMessageSent(message);
				}
			}
		}

		@Override
		public void onConnectivityStateChange(TestConnectivityState state) {
			for (ClientResponseListener listener : listeners) {
				synchronized (listeners) {
					listener.onConnectivityStateChange(state);
				}
			}
		}
	};

//    private void setConnectivityState(TestConnectivityState newState) {
//        _connectivityState = newState;
//        if (responseListener != null) {
//            responseListener.onConnectivityStateChange(newState);
//        }
//    }

//    protected final List<String> receivedMessages = new LinkedList<>();
//    protected final TreeSet<String> receivedSenderCallsigns = new TreeSet<>();
//    protected final TreeSet<String> receivedSenderUids = new TreeSet<>();

//    private TestConnectivityState _connectivityState = TestConnectivityState.Disconnected;

	private String latestSA;

	private final AbstractUser profile;

	private final SendingInterface sender;
	private final ReceivingInterface receiver;
	private final ConnectingInterface connector;

	public final MissionDataSyncClient mission;

	private final HashSet<ClientResponseListener> listeners = new HashSet<>();

	public UnifiedClient(@NotNull AbstractUser user) {
		this.profile = user;

		ProtocolProfilesInterface protocol = user.getConnection().getProtocol();

		if (protocol.canConnect()) {
			if (user.getCertPrivateJksPath() != null) {
				connector = new ConnectibleTakprotoClient(user, innerActivityListener);
				mission = new MissionDataSyncClient(user);
			} else {
				connector = new ConnectibleClient(user, innerActivityListener);
				mission = null;
			}
			sender = connector;
			receiver = connector;

		} else if (protocol.canSend()) {
			sender = new SendingClient(user, innerActivityListener);
			connector = null;
			receiver = null;
			mission = null;

		} else if (protocol.canListen()) {
			receiver = new ReceivingClient(user, innerActivityListener);
			connector = null;
			sender = null;
			mission = null;
		} else {
			throw new RuntimeException("Provided user '" + user.getConsistentUniqueReadableIdentifier() + "' with protocol '" +
					user.getConnection().getProtocol() + "' cannot be set up as a sender, receiver, or connector!");
		}
	}

	public void addListener(ClientResponseListener listener) {
		synchronized (listeners) {
			listeners.add(listener);
		}

	}

	@Override
	public AbstractUser getProfile() {
		return profile;
	}

	@Override
	public String toString() {
		return profile.getConsistentUniqueReadableIdentifier();
	}

	@Override
	public synchronized TestConnectivityState connect(boolean authenticateIfNecessary, @Nullable String xmlData) {
		if (connector == null) {
			throw new RuntimeException("User " + toString() + " is not using a connectable protocol!");
		}
		TestConnectivityState state = connector.connect(authenticateIfNecessary, xmlData);

		if (state == TestConnectivityState.ConnectedAuthenticatedIfNecessary) {
			latestSA = xmlData;
		}
		return state;
	}

	@Override
	public synchronized void disconnect(boolean logInconsistentState) {
		if (connector == null && logInconsistentState) {
			throw new RuntimeException("User " + toString() + " is not using a connectable protocol!");
		}

		connector.disconnect(logInconsistentState);
		latestSA = null;
	}

	@Override
	public synchronized boolean isConnected() {
		if (connector == null) {
			throw new RuntimeException("User " + toString() + " is not using a connectable protocol!");
		}
		return connector.isConnected();
	}

	@Override
	public void cleanup() {
		if (sender != null) {
			sender.cleanup();
		}

		if (receiver != null) {
			receiver.cleanup();
		}

		if (connector != null) {
			connector.cleanup();
		}
	}

	@Override
	public void setAdditionalOutputTarget(OutputTarget target) {
		if (receiver != null) {
			receiver.setAdditionalOutputTarget(target);
		}
	}

	@Override
	public boolean sendMessage(Document doc) {
		if (sender == null) {
			throw new RuntimeException("Cannot send with '" + toString() + "'!");
		} else {
			boolean sentSuccessfully = sender.sendMessage(doc);
			if (sentSuccessfully) {
				latestSA = doc.asXML();
			}
			return sentSuccessfully;
		}
	}

	@Override
	public boolean sendMessageWithoutXmlDeclaration(@NotNull Document doc) {
		return sender.sendMessageWithoutXmlDeclaration(doc);
	}

	@Override
	public synchronized TestConnectivityState authenticate() {
		if (connector == null) {
			throw new RuntimeException("Cannot authenticate with '" + toString() + "'!");
		} else {
			return connector.authenticate();
		}
	}

	public TestConnectivityState getConnectivityState() {
		if (connector != null) {
			return connector.getConnectivityState();
		} else {
			return getProfile().getConnection().getProtocol().getInitialState();
		}
	}

	public TestConnectivityState getActualConnectivityState() {
		if (connector != null) {
			return connector.getActualConnectivityState();
		} else {
			return getProfile().getConnection().getProtocol().getInitialState();
		}
	}

	public String getLatestSA() {
		return latestSA;
	}
}
