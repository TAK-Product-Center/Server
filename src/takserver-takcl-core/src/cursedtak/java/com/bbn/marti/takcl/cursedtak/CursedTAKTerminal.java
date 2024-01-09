package com.bbn.marti.takcl.cursedtak;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import com.bbn.marti.takcl.connectivity.implementations.UnifiedClient;
import com.bbn.marti.test.shared.TestConnectivityState;
import com.bbn.marti.test.shared.data.protocols.ProtocolProfiles;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.CheckBox;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LayoutData;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created on 2/26/18.
 */
public class CursedTAKTerminal {

	public static Logger logger = LoggerFactory.getLogger("ROOT");

	static {


	}

	CursedTakControllerListener listener = new CursedTakControllerListener() {
		@Override
		public void modeSwitched(@NotNull CursedTakMode newMode) {
			currentMode = newMode;
			render();
		}

		@Override
		public void messageSent(@NotNull String message) {
			if (mainPanel.containsComponent(cursedCotTxPanel)) {
				cursedCotTxPanel.render();
				render();
			}
		}

		@Override
		public void messageReceived(@NotNull String message) {
			if (mainPanel.containsComponent(cursedCotRxPanel)) {
				cursedCotRxPanel.render();
				render();
			}
		}

		@Override
		public void connectivityStateChanged(TestConnectivityState connectivityState) {

		}
	};

	public static List<String> splitString(String str, int maxLength) {
		int size = (str.length() / maxLength - 1) + (str.length() % (maxLength - 1) > 0 ? 1 : 0);
		ArrayList<String> rval = new ArrayList<>(size);

		while (str.length() > 0) {
			int len = Math.min(str.length(), maxLength - 1);
			String value = str.substring(0, len);
			str = str.substring(len);
			if (rval.isEmpty()) {
				rval.add(value);
			} else {
				rval.add(" " + value);
			}
		}
		return rval;
	}

	public class CursedCotSwappablePanel extends Panel {

		private final Logger logger;

		private final LinkedBlockingDeque<String> unrenderedMessages = new LinkedBlockingDeque<>();

		private final CursedCotSwappablePanel self = this;

		private final CursedTakControllerListener controllerListener = new CursedTakControllerListener() {
			private void receiveMessage(String message) {
				logger.trace("receiveMessage - " + message);
//				synchronized (controller) {
//					if
//					if (controller.isActiveComponent(self)) {
//						TerminalSize ts = textBox.getSize();
//						List<String> values = splitString(message, ts.getColumns());
//
//						for (String str : values) {
//							textBox.addLine(str);
//							textBox.handleKeyStroke(new KeyStroke(KeyType.ArrowDown));
//						}
//					} else {
				unrenderedMessages.add(message);
				if (mainPanel.containsComponent(self)) {
					render();
				}
//					}
//				}
			}

			@Override
			public void modeSwitched(@NotNull CursedTakMode newMode) {
				logger.trace("modeSwitched: " + newMode.toString());
				synchronized (controller) {
					if (newMode == mode) {
						self.render();
					}
				}
			}

			@Override
			public void messageSent(@NotNull String message) {
				logger.trace("messageSent");
				if (mode == CursedTakMode.COT_SEND) {
					logger.trace("messageSentAction");
					receiveMessage(message);
				}
			}

			@Override
			public void messageReceived(@NotNull String message) {
				logger.trace("messageReceived");
				if (mode == CursedTakMode.COT_RECEIVE) {
					logger.trace("messageReceivedAction");
					receiveMessage(message);
				}

			}

			@Override
			public void connectivityStateChanged(TestConnectivityState connectivityState) {

			}
		};

		private final List<CursedCotSwappablePanel> panels = new LinkedList<>();

		private final CursedTakMode mode;

		public final TextBox textBox;

		protected final CursedTAKController controller;

		public CursedCotSwappablePanel(@NotNull CursedTAKController controller, @NotNull CursedTakMode mode) {
			super(new GridLayout(1));
			logger = LoggerFactory.getLogger(mode.toString());
			this.controller = controller;
			this.mode = mode;
			controller.addListener(controllerListener);

			TerminalSize currentTerminalSize = controller.getTerminalSize();
			setLayoutData((LayoutData) GridLayout.createHorizontallyFilledLayoutData(1));
			addComponent(createTitlebar());
			setSize(currentTerminalSize);

			textBox = new TextBox();
			textBox.setReadOnly(true);

			addComponent(textBox.withBorder(Borders.singleLine(mode.modeTitle)).setPreferredSize(new TerminalSize(160, currentTerminalSize.getRows() - 2)));

			synchronized (panels) {
				panels.add(this);
			}
		}

		public synchronized void render() {
			logger.trace("render:wait");
			synchronized (controller) {
				logger.trace("render:begin");
				TerminalSize currentTerminalSize = controller.getTerminalSize();
				resize(currentTerminalSize);
				while (!unrenderedMessages.isEmpty()) {
					String xmlData = unrenderedMessages.remove();

					TerminalSize ts = textBox.getSize();
					List<String> values = splitString(xmlData, currentTerminalSize.getColumns());

					for (String str : values) {
						textBox.addLine(str);
//						textBox.handleKeyStroke(new KeyStroke(KeyType.ArrowDown));
					}
				}
				logger.trace("render:end");
			}
		}

		public synchronized void resize(TerminalSize currentTerminalSize) {
			logger.trace("resize: wait");
			synchronized (controller) {
				logger.trace("resize: begin");
				setSize(currentTerminalSize);
				textBox.setSize(new TerminalSize(160, currentTerminalSize.getRows() - 2));
				logger.trace("resize: end");
			}
		}
	}

	public static class SendPanel extends Panel {

		private final Logger logger = LoggerFactory.getLogger("SendPanel");

		CursedTakControllerListener listener = new CursedTakControllerListener() {
			@Override
			public void modeSwitched(@NotNull CursedTakMode newMode) {
				logger.trace("modeSwitched: " + newMode.toString());


			}

			@Override
			public void messageSent(@NotNull String message) {
				logger.trace("messageSent");

			}

			@Override
			public void messageReceived(@NotNull String message) {
				logger.trace("messageReceived");

			}

			@Override
			public void connectivityStateChanged(TestConnectivityState connectivityState) {
				logger.trace("connectivityStateChanged: " + connectivityState.name());
				switch (connectivityState) {
					case SendOnly:
					case ConnectedAuthenticatedIfNecessary:
					case ConnectedUnauthenticated:
					case ConnectedCannotAuthenticate:
						sendMessageButton.setEnabled(true);
						break;

					case Disconnected:
					case ReceiveOnly:
						sendMessageButton.setEnabled(false);
						break;
				}

			}
		};

		Button sendMessageButton;

		public SendPanel(@NotNull CursedTAKController controller) {
			super(new LinearLayout(Direction.VERTICAL));

			controller.addListener(listener);

			LayoutData optionLayoutData = LinearLayout.createLayoutData(LinearLayout.Alignment.End);

			CheckBox callsignTx = new CheckBox("Callsign").setChecked(true).setLayoutData(optionLayoutData);
			CheckBox imageTx = new CheckBox("Image   ").setLayoutData(optionLayoutData);
			CheckBox uidTx = new CheckBox("UID     ").setChecked(true).setLayoutData(optionLayoutData);

			ProtocolProfiles protocol = controller.getProfile().getConnection().getProtocol();
			boolean enableSend = (!protocol.canConnect() && protocol.canSend());

			sendMessageButton = new Button("Transmit", new Runnable() {
				@Override
				public void run() {
					controller.sendMessage(uidTx.isChecked(), callsignTx.isChecked(), imageTx.isChecked());
				}
			}).setEnabled(enableSend);

//			sendMessageButton.handleKeyStroke(KeyStroke.fromString("t"));
			sendMessageButton.handleInput(KeyStroke.fromString("t"));

			addComponent(sendMessageButton);
			addComponent(callsignTx);
			addComponent(imageTx);
			addComponent(uidTx);

			this.handleInput(KeyStroke.fromString("t"));

			sendMessageButton.handleInput(KeyStroke.fromString("t"));
		}
	}


	public static class StatusPanel extends Panel {

		private static final Logger logger = LoggerFactory.getLogger("DISPLAY_STATUS");

		CursedTakControllerListener listener = new CursedTakControllerListener() {
			@Override
			public void modeSwitched(@NotNull CursedTakMode newMode) {
				logger.trace("modeSwitched: " + newMode.toString());
			}

			@Override
			public void messageSent(@NotNull String message) {
				logger.trace("messageSent");

			}

			@Override
			public void messageReceived(@NotNull String message) {
				logger.trace("messageReceived");

			}

			@Override
			public void connectivityStateChanged(TestConnectivityState connectivityState) {
				logger.trace("connectivityStateChanged: " + connectivityState.name());
				switch (connectivityState) {
					case Disconnected:
						connectedLabel.setText("Connected:     F");
						authenticatedLabel.setText("Authenticated: F");
						break;

					case ConnectedAuthenticatedIfNecessary:
						connectedLabel.setText("Connected:      T");
						authenticatedLabel.setText("Authenticated: T");
						break;

					case ConnectedUnauthenticated:
						connectedLabel.setText("Connected:      T");
						authenticatedLabel.setText("Authenticated: F");

						break;
					case ConnectedCannotAuthenticate:
						connectedLabel.setText("Connected:      T");
						authenticatedLabel.setText("Authenticated: F");

						break;
					case SendOnly:
						connectedLabel.setText("Connected:    N/A");
						authenticatedLabel.setText("Authenticated: T");

						break;
					case ReceiveOnly:
						connectedLabel.setText("Connected:    N/A");
						authenticatedLabel.setText("Authenticated: T");
						break;
				}
			}
		};

		Label connectedLabel;
		Label authenticatedLabel;

		public StatusPanel(@NotNull CursedTAKController controller) {
			super(new LinearLayout(Direction.VERTICAL));
			connectedLabel = new Label("Connected:     F");
			authenticatedLabel = new Label("Authenticated: F");
			controller.addListener(listener);
			addComponent(connectedLabel);
			addComponent(authenticatedLabel);
		}
	}

	public static class ConnectivityPanel extends Panel {
		private static final Logger logger = LoggerFactory.getLogger("ConnectivityPanel");

		CursedTakControllerListener listener = new CursedTakControllerListener() {

			@Override
			public void modeSwitched(@NotNull CursedTakMode newMode) {
				logger.trace("modeSwitched: " + newMode.toString());
			}

			@Override
			public void messageSent(@NotNull String message) {
				logger.trace("messageSent");

			}

			@Override
			public void messageReceived(@NotNull String message) {
				logger.trace("messageReceived");

			}

			@Override
			public void connectivityStateChanged(TestConnectivityState connectivityState) {
				logger.trace("connectivityStateChanged: " + connectivityState.toString());
				switch (connectivityState) {

					case Disconnected:
						connectButton.setEnabled(true);
						disconnectButton.setEnabled(false);
						authenticateButton.setEnabled(false);
						break;

					case ConnectedAuthenticatedIfNecessary:
						connectButton.setEnabled(false);
						disconnectButton.setEnabled(true);
						authenticateButton.setEnabled(true);
						break;

					case ConnectedUnauthenticated:
						connectButton.setEnabled(false);
						disconnectButton.setEnabled(true);
						authenticateButton.setEnabled(true);
						break;

					case ConnectedCannotAuthenticate:
						connectButton.setEnabled(false);
						disconnectButton.setEnabled(true);
						authenticateButton.setEnabled(true);
						break;

					case SendOnly:
						connectButton.setEnabled(false);
						disconnectButton.setEnabled(false);
						authenticateButton.setEnabled(false);
						break;

					case ReceiveOnly:
						connectButton.setEnabled(false);
						disconnectButton.setEnabled(false);
						authenticateButton.setEnabled(false);
						break;
				}
			}
		};

		private Button connectButton;
		private Button disconnectButton;
		private Button authenticateButton;

		public ConnectivityPanel(@NotNull CursedTAKController controller) {
			super(new LinearLayout(Direction.VERTICAL));

			LayoutData optionLayoutData = LinearLayout.createLayoutData(LinearLayout.Alignment.End);

			CheckBox authenticate = new CheckBox("Auth    ").setLayoutData(optionLayoutData);
			CheckBox send = new CheckBox("Msg     ").setLayoutData(optionLayoutData);

			connectButton = new Button(
					"Connect",
					() -> controller.connect(authenticate.isChecked(), send.isChecked())
			);
			addComponent(connectButton);

			addComponent(authenticate);
			addComponent(send);

			authenticateButton = new Button(
					"Authenticate",
					() -> controller.authenticate()
			);
			addComponent(authenticateButton);

			disconnectButton = new Button(
					"Disconnect",
					controller::disconnect
			);

			controller.addListener(listener);

			addComponent(disconnectButton);
		}
	}

	private Panel sidePanel;
	private CursedCotSwappablePanel cursedCotRxPanel;
	private CursedCotSwappablePanel cursedCotTxPanel;

//    private DISPLAY_MODE current_mode = DISPLAY_MODE.COT_RX;

	private static final String title = "CursedTAK";

	private final Screen screen;

	private final Window window;

	private final Terminal terminal;

	private final WindowBasedTextGUI textGUI;

	private final Panel mainPanel;

	private static Panel createTitlebar() {
		Panel titlePanel = new Panel(new GridLayout(1));
		titlePanel.setLayoutData((LayoutData) GridLayout.createLayoutData(
				GridLayout.Alignment.CENTER,
				GridLayout.Alignment.CENTER,
				true,
				false
		));
		Label titlebar = new Label(title);
		titlebar.setBackgroundColor(TextColor.ANSI.BLUE);
		titlebar.setForegroundColor(TextColor.ANSI.DEFAULT);

		return titlePanel;
	}


	private final CursedTAKController controller;

	private CursedTakMode currentMode;

	public void render() {
		logger.trace("render");
		synchronized (this) {
			mainPanel.removeAllComponents();
//		mainPanel.addComponent(new Button("MEH"));
//		mainPanel.removeAllComponents();
			mainPanel.addComponent(sidePanel);

			if (currentMode == CursedTakMode.COT_SEND) {
				mainPanel.addComponent(cursedCotTxPanel);
			} else {
				mainPanel.addComponent(cursedCotRxPanel);
			}
		}

//		mainPanel.invalidate();

//		try {
//			textGUI = new MultiWindowTextGUI(screen);
//			screen.refresh();
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}
	}

	public CursedTAKTerminal(UnifiedClient client, CursedTakMode initialMode, boolean connectOnStartup, PrintStream stdoutStream) {

//		if (logFile != null) {
//			LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
//			FileAppender<ILoggingEvent> fa = new FileAppender<>();
//			fa.setFile(logFile);
//			fa.setContext(lc);
//			fa.setAppend(true);
//
//			PatternLayoutEncoder ple = new PatternLayoutEncoder();
//			ple.setContext(lc);
//			ple.setPattern("%date %level [%thread] %logger{10} %msg%n");
//			ple.start();
//			fa.setEncoder(ple);
//
//			fa.start();
//
//			fa.start();
//
//			Logger rootLogger = lc.getLogger(Logger.ROOT_LOGGER_NAME);
//			((ch.qos.logback.classic.Logger) rootLogger).detachAndStopAllAppenders();
//			((ch.qos.logback.classic.Logger) rootLogger).addAppender(fa);
//		}

		try {
			terminal = new DefaultTerminalFactory(stdoutStream, System.in, Charset.defaultCharset()).createTerminal();
			window = new BasicWindow(title);
			window.setHints(Arrays.asList(Window.Hint.FULL_SCREEN, Window.Hint.NO_DECORATIONS));
			controller = new CursedTAKController(terminal, window, client, initialMode);

			mainPanel = new Panel(new LinearLayout(Direction.HORIZONTAL));

			screen = new TerminalScreen(terminal);
			screen.startScreen();

			Button viewButton = new Button("Change View",
					() -> {
						controller.switchMode(currentMode.nextMode());

					});

			textGUI = new MultiWindowTextGUI(screen);

			Button quitButton = new Button("Quit",
					() -> {
						System.exit(0);
					});

			sidePanel = new Panel(new LinearLayout(Direction.VERTICAL));

			sidePanel.addComponent(viewButton);

			StatusPanel statusPanel = new StatusPanel(controller); sidePanel.addComponent(statusPanel.withBorder(Borders.singleLine("Status")));
			SendPanel sendPanel = new SendPanel(controller);
			sidePanel.addComponent(sendPanel.withBorder(Borders.singleLine("Transmission")));

			ConnectivityPanel connectivityPanel = new ConnectivityPanel(controller);
			sidePanel.addComponent(connectivityPanel.withBorder(Borders.singleLine("Connectivity")));

			sidePanel.addComponent(quitButton);

			mainPanel.addComponent(sidePanel);

			cursedCotRxPanel = new CursedCotSwappablePanel(controller, CursedTakMode.COT_RECEIVE);
			cursedCotTxPanel = new CursedCotSwappablePanel(controller, CursedTakMode.COT_SEND);

			if (initialMode == CursedTakMode.COT_SEND) {
				mainPanel.addComponent(cursedCotTxPanel);
			} else {
				mainPanel.addComponent(cursedCotRxPanel);
			}
			this.currentMode = initialMode;

			window.setComponent(mainPanel);
			controller.addListener(listener);

			if (connectOnStartup) {
				controller.connect(true, false);
			}

			textGUI.addWindowAndWait(window);

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}

