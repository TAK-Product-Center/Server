package com.bbn.marti.takcl.cursedtak;

import com.bbn.marti.takcl.connectivity.implementations.UnifiedClient;
import com.bbn.marti.takcl.connectivity.interfaces.ClientResponseListener;
import com.bbn.marti.test.shared.CotGenerator;
import com.bbn.marti.test.shared.TestConnectivityState;
import com.bbn.marti.test.shared.data.protocols.ProtocolProfiles;
import com.bbn.marti.test.shared.data.users.AbstractUser;
import com.bbn.marti.test.shared.engines.UserIdentificationData;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.terminal.Terminal;
import org.dom4j.Document;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created on 3/13/18.
 */
final class CursedTAKController {

	private final ClientResponseListener clientActivityListener = new ClientResponseListener() {
		@Override
		public void onMessageReceived(String response) {
			for (CursedTakControllerListener listener : controllerListeners) {
				executor.submit(new Runnable() {
					@Override
					public void run() {
						listener.messageReceived(response);

					}
				});
			}
		}

		@Override
		public void onMessageSent(String message) {
			for (CursedTakControllerListener listener : controllerListeners) {
				executor.submit(new Runnable() {
					@Override
					public void run() {
						listener.messageSent(message);

					}
				});
			}
		}

		@Override
		public void onConnectivityStateChange(TestConnectivityState state) {
			for (CursedTakControllerListener listener : controllerListeners) {
				executor.submit(new Runnable() {
					@Override
					public void run() {
						listener.connectivityStateChanged(state);

					}
				});
			}
		}
	};

	private final ExecutorService executor = Executors.newFixedThreadPool(8);

	private final HashSet<CursedTakControllerListener> controllerListeners = new HashSet<>();

	private final Terminal terminal;
//    private final Window window;

//    private CursedTAKTerminal.CursedTakMode currentMode;

	private final UnifiedClient client;
	public final LinkedBlockingDeque<String> unrenderedSentMessages = new LinkedBlockingDeque<>();
	public final LinkedBlockingDeque<String> unrenderedReceivedMessages = new LinkedBlockingDeque<>();

	public AbstractUser getUser() {
		return client.getProfile();
	}

//    public boolean isActiveComponent(Component component) {
//        return window.getComponent() == component;
//    }

	public CursedTAKController(@NotNull Terminal terminal, @NotNull Window window, @NotNull UnifiedClient client, @NotNull CursedTakMode initialMode) {
		this.terminal = terminal;
//        this.window = window;
		this.client = client;
//        this.currentMode = initialMode;
		client.addListener(clientActivityListener);
	}

	public TerminalSize getTerminalSize() {
		try {
			return terminal.getTerminalSize();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized void addListener(CursedTakControllerListener listener) {
		controllerListeners.add(listener);
	}

	public synchronized void switchMode(CursedTakMode newMode) {
		for (CursedTakControllerListener listener : controllerListeners) {
			listener.modeSwitched(newMode);
		}
//        currentMode = newMode;
	}


	public synchronized void connect(boolean authenticateIfnecessary, boolean doSendMessage) {
		if (doSendMessage) {
			client.connect(authenticateIfnecessary,
					CotGenerator.createLatestSAMessage(getProfile()).asXML());
		} else {
			client.connect(authenticateIfnecessary, null);
		}
	}

	public synchronized void authenticate() {
		client.authenticate();
	}

	public AbstractUser getProfile() {
		return client.getProfile();
	}

	public void disconnect() {
		client.disconnect(false);
	}


	private static final AtomicInteger counter = new AtomicInteger(0);

	public synchronized void sendMessage(boolean includeUid, boolean includeCallsign, boolean includeImage) {
		UserIdentificationData uid;
		if (includeUid) {
			if (includeCallsign) {
				uid = UserIdentificationData.UID_AND_CALLSIGN;
			} else {
				uid = UserIdentificationData.UID;
			}
		} else {
			if (includeCallsign) {
				uid = UserIdentificationData.CALLSIGN;

			} else {
				uid = UserIdentificationData.NONE;
			}
		}


		Document msg = CotGenerator.createLatestSAMessage(uid, client.getProfile(), UserIdentificationData.NONE, includeImage, null);
		String msgStr = msg.asXML();

		client.sendMessage(msg);

//        String c = Integer.toString(counter.getAndIncrement());
//
//        String sample = "r";
//        String val = "";
//        for (int i = 0; i < 16; i++) {
//            val = val + sample + c;
//            sample = sample + "=";
//        }

//        clientActivityListener.onMessageSent(msgStr);

//        sample = "s";
//        val = "";
//        for (int i = 0; i < 16; i++) {
//            val = val + sample + c;
//            sample = sample + "=";
//        }
//        clientActivityListener.onMessageReceived(val);
	}
}