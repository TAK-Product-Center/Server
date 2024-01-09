package com.bbn.marti.takcl.connectivity.implementations;

import com.bbn.marti.config.AuthType;
import com.bbn.marti.takcl.SSLHelper;
import com.bbn.marti.takcl.TAKCLCore;
import com.bbn.marti.takcl.connectivity.interfaces.ClientResponseListener;
import com.bbn.marti.takcl.connectivity.interfaces.ConnectingInterface;
import com.bbn.marti.test.shared.CotGenerator;
import com.bbn.marti.test.shared.TestConnectivityState;
import com.bbn.marti.test.shared.data.connections.AbstractConnection;
import com.bbn.marti.test.shared.data.users.AbstractUser;
import com.bbn.marti.tests.Assert;
import org.dom4j.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static com.bbn.marti.takcl.TAKCLProfilingLogging.DurationLogger;
import static com.bbn.marti.takcl.TAKCLProfilingLogging.LogActivity;

/**
 */
public class ConnectibleClient implements ConnectingInterface {

	private static final String DEFAULT_SERVER = "127.0.0.1";

	private static final int BUFFER_SIZE = 1048576;

	private Thread listeningThread;
	private Thread broadcastingThread;

	private final ClientResponseListener stateChangeListener;
	private final AbstractUser user;
	private final AbstractConnection connection;

	private TestConnectivityState _connectivityState;
	private final Logger log;
	private final DurationLogger dl;

	@Override
	public synchronized TestConnectivityState getConnectivityState() {
		return getActualConnectivityState();
	}

	@Override
	public synchronized TestConnectivityState getActualConnectivityState() {
		// If it should be connected but the socket is closed, it is actually disconnected.
		if ((_connectivityState == TestConnectivityState.ConnectedAuthenticatedIfNecessary ||
				_connectivityState == TestConnectivityState.ConnectedCannotAuthenticate ||
				_connectivityState == TestConnectivityState.ConnectedUnauthenticated) && !isConnected()) {
			Assert.fail("The state of " + user + " should be " + _connectivityState + " but it is not actually connected!");
			_connectivityState = TestConnectivityState.Disconnected;
		}
		return _connectivityState;
	}

	private Socket socket;
	private BufferedReader socketReader;
	private OutputStream socketOutputStream;

	private OutputTarget secondaryOutputTarget = OutputTarget.DEV_NULL;
	private OutputStream secondaryOutputStream = null;

	public ConnectibleClient(@NotNull AbstractUser user, @NotNull final ClientResponseListener listener) {
		if (!user.getConnection().getProtocol().canConnect()) {
			throw new RuntimeException("ProtocolProfiles " + user.getConnection().getProtocol().toString() + " is not a connectable protocol!");
		}

		AbstractConnection connection = user.getConnection();

		this.user = user;
		this.connection = connection;
		this.stateChangeListener = listener;
		this._connectivityState = TestConnectivityState.Disconnected;
		this.log = LoggerFactory.getLogger(ConnectibleClient.class);
		this.dl = new DurationLogger(user.getConsistentUniqueReadableIdentifier(), log);
	}


	private void setConnectivityState(TestConnectivityState newState) {
		if (_connectivityState != newState) {
			_connectivityState = newState;
//			dl.begin("stateChangeListener.onConnectivityStateChange");
			stateChangeListener.onConnectivityStateChange(newState);
//			dl.end("stateChangeListener.onConnectivityStateChange");
		}
	}

	@Override
	public synchronized TestConnectivityState connect(boolean authenticateIfNecessary, @Nullable String xmlData) {
		if (isConnected()) {
			System.err.println("Cannot connect client '" + user.getConsistentUniqueReadableIdentifier() + "' because it is already connected!");
			return getActualConnectivityState();
		}

		boolean isAuthing = (authenticateIfNecessary && user.getConnection().requiresAuthentication());

		try {
			dl.begin(LogActivity.connect);

			String userIdentifier = null;
			String userPassword = null;
			String uid = null;

			if (isAuthing) {
				userIdentifier = user.getUserName();
				userPassword = user.getPassword();
				uid = user.getConsistentUniqueReadableIdentifier();
			}


			try {
				if (connection.getProtocol().isTLS()) {
					socket = SSLHelper.getInstance().createSSLSocket();
					InetSocketAddress address = new InetSocketAddress(user.getServer().getUrl(), connection.getPort());
					socket.connect(address);
				} else {
					socket = new Socket(user.getServer().getUrl(), connection.getPort());
				}

				socketOutputStream = new DataOutputStream(socket.getOutputStream());


				// If it has something to send, send it
				if (isAuthing || xmlData != null) {
//				dl.begin("Create Auth Payload");
					String sendString = CotGenerator.createAuthPayload(userIdentifier, userPassword, uid, xmlData);
					sendString = sendString.substring(sendString.indexOf('\n') + 1);

//				dl.end("Create Auth Payload");

					if (isAuthing) {
						dl.begin(LogActivity.auth);
					}

					if (xmlData != null) {
						dl.begin(LogActivity.getSend(user));
					}

					socketOutputStream.write(sendString.getBytes());

					if (xmlData != null) {
						dl.end(LogActivity.getSend(user));
					}

					if (isAuthing) {
						dl.end(LogActivity.auth);
					}


					if (isAuthing || !getProfile().getConnection().requiresAuthentication()) {
						// If it authenticated as part of this or doesn't require auth, it is ready to send
						setConnectivityState(TestConnectivityState.ConnectedAuthenticatedIfNecessary);

					} else {
						// Otherwise, It requires authentication, didn't send it, and sent a message, so it is no longer able to authenticate
						setConnectivityState(TestConnectivityState.ConnectedCannotAuthenticate);
					}
				} else if (socket.isConnected() && !socket.isClosed()) {
					if (user.getConnection().requiresAuthentication()) {
						setConnectivityState(TestConnectivityState.ConnectedUnauthenticated);
					} else {
						setConnectivityState(TestConnectivityState.ConnectedAuthenticatedIfNecessary);
					}
				}

//			dl.begin("Init Listening Thread");
				listeningThread = new Thread() {
					@Override
					public void run() {
						try {
							// This parsing logic is slow, but it works for now for all the protocols and detects disconnections properly
							String currentString = "";

							socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

							int bytesRead;
							char[] receiveBuffer = new char[BUFFER_SIZE];
//						dl.end("Init Listening Thread");

							while ((bytesRead = socketReader.read(receiveBuffer)) != 0 && !isInterrupted()) {
								if (bytesRead > 0) {
//								dl.log("BytesRead(" + ReceivingInterface.getGeneralPropertiesDisplayString(user.getConnection()) + ")");

									currentString = currentString + (new String(receiveBuffer, 0, bytesRead));

									if (currentString.endsWith(">")) {
//									dl.begin("Parse CoT Messages");
										List<String> messages = CotGenerator.parseMessages(currentString);
//									dl.end("Parse CoT Messages");
										for (String str : messages) {
											responseReceived(str);
										}
										currentString = "";

									}

								} else {
									// Has been disconnected. I think...
									break;
								}
							}


						} catch (IOException e) {
							System.out.println("EXCEPTION IN DATA READ");
							System.out.println("Error reading from socket: " + e.getMessage());
						}

						try {
							if (socketOutputStream != null) socketOutputStream.close();
							if (socketReader != null) socketReader.close();
							if (socket != null) socket.close();

						} catch (IOException e) {
							System.err.println(e.getMessage());
						} finally {
							socketOutputStream = null;
							socketReader = null;
							socket = null;
							listeningThread = null;
							setConnectivityState(TestConnectivityState.Disconnected);
						}
					}
				};

				listeningThread.start();

			} catch (IOException e) {
				System.err.println(e.toString());

				try {
					if (socketReader != null) socketReader.close();
					if (socketOutputStream != null) socketOutputStream.close();
					if (socket != null) socket.close();

				} catch (IOException e2) {
					System.err.println(e2.getMessage());
				} finally {
					setConnectivityState(TestConnectivityState.Disconnected);
				}
			}
			dl.end(LogActivity.connect);
		} finally {
			if (getProfile().getConnection().requiresAuthentication()) {
				if (isAuthing) {
						setConnectivityState(TestConnectivityState.ConnectedAuthenticatedIfNecessary);
				} else {
					if (xmlData == null) {
						setConnectivityState(TestConnectivityState.ConnectedUnauthenticated);
					} else {
						setConnectivityState(TestConnectivityState.ConnectedCannotAuthenticate);
					}
				}
			} else {
				setConnectivityState(TestConnectivityState.ConnectedAuthenticatedIfNecessary);
			}
		}
		return getActualConnectivityState();
	}

	private synchronized boolean sendMessage(@NotNull Document doc, boolean omitXmlDeclaration) {
		String xmlString = omitXmlDeclaration ? CotGenerator.stripXmlDeclaration(doc) : doc.asXML();

		if (!isConnected()) {
			System.err.println("Cannot send message from client '" + user.getConsistentUniqueReadableIdentifier() + "' because it is not connected!");
			return false;
		}
		try {
			dl.begin(LogActivity.getSend(user));
			socketOutputStream.write(xmlString.getBytes());
			dl.end(LogActivity.getSend(user));
			// After sending an initial message, the user can no longer authenticate
			if (getActualConnectivityState() == TestConnectivityState.ConnectedUnauthenticated) {
				setConnectivityState(TestConnectivityState.ConnectedCannotAuthenticate);
			}
//			dl.begin("stateChangeListener.onMessageSent");
			stateChangeListener.onMessageSent(xmlString);
//			dl.end("stateChangeListener.onMessageSent");
			return true;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public synchronized boolean sendMessage(Document doc) {
		return sendMessage(doc, false);
	}

	@Override
	public synchronized boolean sendMessageWithoutXmlDeclaration(Document doc) {
		return sendMessage(doc, true);
	}

	@Override
	public synchronized TestConnectivityState authenticate() {
		TestConnectivityState initialState = getActualConnectivityState();
		TestConnectivityState rval = initialState;

		if (connection.getAuthType() == AuthType.ANONYMOUS) {
			System.err.println("User '" + user.getConsistentUniqueReadableIdentifier() + "' does not need to authenticate!");

		} else {
			switch (initialState) {
				case Disconnected:
					System.err.println("User '" + user.getConsistentUniqueReadableIdentifier() + "' Must connect prior to authenticating!");
					if (isConnected()) {
						throw new RuntimeException("User '" + user.getConsistentUniqueReadableIdentifier() + "' is connected even though the stored state indicates it is Disconnected!");
					}
					break;

				case ConnectedAuthenticatedIfNecessary:
					System.err.println("User '" + user.getConsistentUniqueReadableIdentifier() + "' has already authenticated!");
					break;

				case ConnectedUnauthenticated:
					dl.begin(LogActivity.auth);
					Document sendMessage = CotGenerator.createAuthMessage(user.getUserName(), user.getPassword(), user.getConsistentUniqueReadableIdentifier());
					boolean sendSuccess = sendMessageWithoutXmlDeclaration(sendMessage);
					if (sendSuccess) {
						setConnectivityState(TestConnectivityState.ConnectedAuthenticatedIfNecessary);
						rval = TestConnectivityState.ConnectedAuthenticatedIfNecessary;
					} else {
						System.err.println("User '" + user.getConsistentUniqueReadableIdentifier() + "' could not successfully send authentication message!");
					}
					dl.end(LogActivity.auth);
					break;

				case ConnectedCannotAuthenticate:
					System.err.println("User '" + user.getConsistentUniqueReadableIdentifier() + "' can no longer auth on this connection!");
					break;

				case SendOnly:
				case ReceiveOnly:
					throw new RuntimeException("User '" + user.getConsistentUniqueReadableIdentifier() + "' is in an unexpected state '" + initialState.name() + "'!");
			}
		}
		return rval;
	}


	@Override
	public synchronized void disconnect(boolean logInconsistentState) {
		if (!this.isConnected() && logInconsistentState) {
			System.err.println("Cannot disconnect client '" + user.getConsistentUniqueReadableIdentifier() + "' because it is not connected!");
		}
		try {
			socket.close();
			listeningThread.interrupt();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
//        listeningThread = null;

		if (broadcastingThread != null) {
			broadcastingThread.interrupt();
//            broadcastingThread = null;
		}
		setConnectivityState(TestConnectivityState.Disconnected);
//        this.interrupt();
	}


	@Override
	public boolean isConnected() {
		boolean actuallyConnected = socket != null && socket.isConnected() && !socket.isClosed() && listeningThread != null;
		boolean shouldBeConnected = _connectivityState == TestConnectivityState.ConnectedAuthenticatedIfNecessary ||
				_connectivityState == TestConnectivityState.ConnectedCannotAuthenticate ||
				_connectivityState == TestConnectivityState.ConnectedUnauthenticated;

		if (!shouldBeConnected) {
			if (actuallyConnected) {
				throw new RuntimeException("The client " + user + " Is connected when it shouldn't be!");
			} else {
				return false;
			}
		} else {
			if (actuallyConnected) {
				return true;
			} else {
				throw new RuntimeException("The client " + user + " is not connected when it should be!");
			}
		}
	}

	@Override
	public void cleanup() {

		if (listeningThread != null) {
			listeningThread.interrupt();
			listeningThread = null;
		}

		if (broadcastingThread != null) {
			broadcastingThread.interrupt();
			broadcastingThread = null;
		}

		if (socketReader != null) {

			try {
				socketReader.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		if (socketOutputStream != null) {
			try {
				socketOutputStream.flush();
				socketOutputStream.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		if (socket != null) {
			try {
				socket.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

	}

	@Override
	public AbstractUser getProfile() {
		return user;
	}

	public synchronized void setAdditionalOutputTarget(@NotNull OutputTarget target) {
		if (target != secondaryOutputTarget) {

			// It has changed, so close the file writer if it exists
			if (secondaryOutputStream != null) {
				try {
					if (secondaryOutputTarget == OutputTarget.INHERIT_IO) {
						secondaryOutputStream = null;
					} else {
						secondaryOutputStream.flush();
						secondaryOutputStream.close();
						secondaryOutputStream = null;
					}
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}

			switch (target) {
				case FILE:
					try {
						String jarLocation = TAKCLCore.class.getProtectionDomain().getCodeSource().getLocation().getPath();
						// TODO: put clientLogs in the TAKCLConfig
						String rootDir = jarLocation.substring(0, jarLocation.lastIndexOf('/') + 1) + "clientLogs/";
						if (!Files.exists(Paths.get(rootDir))) {
							Files.createDirectory(Paths.get(rootDir));
						}
						secondaryOutputStream = Files.newOutputStream(Paths.get(rootDir + user.getConsistentUniqueReadableIdentifier()));
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
					break;

				case INHERIT_IO:
					secondaryOutputStream = System.out;
					// Do nothing
					break;

				case DEV_NULL:
					// Do nothing
					break;
			}
			secondaryOutputTarget = target;
		}

	}

	private synchronized void responseReceived(String response) {
		dl.begin(LogActivity.getReceive(user));
		if (secondaryOutputTarget != OutputTarget.DEV_NULL) {
			try {

				secondaryOutputStream.write(response.getBytes());
				secondaryOutputStream.flush();

			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		stateChangeListener.onMessageReceived(response);
		dl.end(LogActivity.getReceive(user));
	}

}
