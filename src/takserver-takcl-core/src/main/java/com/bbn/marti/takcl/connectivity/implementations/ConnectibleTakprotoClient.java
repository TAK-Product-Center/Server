package com.bbn.marti.takcl.connectivity.implementations;

import atakmap.commoncommo.protobuf.v1.Takmessage;
import com.bbn.marti.takcl.SSLHelper;
import com.bbn.marti.takcl.TAKCLCore;
import com.bbn.marti.takcl.connectivity.interfaces.ClientResponseListener;
import com.bbn.marti.takcl.connectivity.interfaces.ConnectingInterface;
import com.bbn.marti.test.shared.CotGenerator;
import com.bbn.marti.test.shared.TestConnectivityState;
import com.bbn.marti.test.shared.data.generated.ImmutableUsers;
import com.bbn.marti.test.shared.data.users.AbstractUser;
import com.bbn.marti.tests.Assert;
import com.google.protobuf.CodedOutputStream;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import tak.server.cot.CotEventContainer;
import tak.server.proto.StreamingProtoBufHelper;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;

import static com.bbn.marti.takcl.TAKCLProfilingLogging.DurationLogger;
import static com.bbn.marti.takcl.TAKCLProfilingLogging.LogActivity;

/**
 */
public class ConnectibleTakprotoClient implements ConnectingInterface {

	private final ClientResponseListener stateChangeListener;
	private final AbstractUser user;


	private OutputTarget secondaryOutputTarget = OutputTarget.DEV_NULL;
	private OutputStream secondaryOutputStream = null;

	private TestConnectivityState _connectivityState;

	private TakWebsocketClientInterface client;

	private final Logger log = LoggerFactory.getLogger(ConnectibleTakprotoClient.class);
	private final DurationLogger dl;

	public synchronized TestConnectivityState getActualConnectivityState() {
		// If it should be connected but the socket is closed, it is actually disconnected.
		if ((_connectivityState == TestConnectivityState.ConnectedAuthenticatedIfNecessary ||
				_connectivityState == TestConnectivityState.ConnectedCannotAuthenticate ||
				_connectivityState == TestConnectivityState.ConnectedUnauthenticated) && !isConnected()) {
			Assert.fail("The state of " + user + " should be " + _connectivityState + " but it is not actually connected!");
		}
		return _connectivityState;
	}

	public synchronized TestConnectivityState getConnectivityState() {
		return getActualConnectivityState();
	}

	public ConnectibleTakprotoClient(@NotNull AbstractUser user, @NotNull final ClientResponseListener listener) {
		if (!user.getConnection().getProtocol().canConnect()) {
			throw new RuntimeException("ProtocolProfiles " + user.getConnection().getProtocol().toString() + " is not a connectable protocol!");
		}
		this.user = user;
		this.stateChangeListener = listener;
		this._connectivityState = TestConnectivityState.Disconnected;
		this.dl = new DurationLogger(user.getConsistentUniqueReadableIdentifier(), log);
	}


	private void setConnectivityState(TestConnectivityState newState) {
		if (_connectivityState != newState) {
			_connectivityState = newState;
			stateChangeListener.onConnectivityStateChange(newState);
		}
	}

	@Override
	public synchronized TestConnectivityState connect(boolean authenticateIfNecessary, @Nullable String xmlData) {
		if (user.getCertPrivateJksPath() == null) {
			throw new RuntimeException("No certificate found, this user cannot be used for WSS authentication!");
		}
		if (isConnected()) {
			System.err.println("Cannot connect client '" + user.getConsistentUniqueReadableIdentifier() + "' because it is already connected!");
			return getActualConnectivityState();
		}

		try {
//			client = NettyWebsocketClient.buildAndConnectWebsocketClient(user, this::responseReceived, dl);
			client = TakWebsocketClient.buildAndConnectWebsocketClient(user, this::responseReceived, dl);
		} catch (GeneralSecurityException | IOException | URISyntaxException e) {
			throw new RuntimeException(e);
		} finally {
			setConnectivityState(TestConnectivityState.ConnectedAuthenticatedIfNecessary);
		}

		// If it has something to send, send it
		if (xmlData != null) {
			client.send(xmlData);
		}

		return getActualConnectivityState();
	}

	private synchronized boolean sendMessage(@NotNull Document doc, boolean omitXmlDeclaration) {
		String xmlString = omitXmlDeclaration ? CotGenerator.stripXmlDeclaration(doc) : doc.asXML();

		if (!isConnected()) {
			System.err.println("Cannot send message from client '" + user.getConsistentUniqueReadableIdentifier() + "' because it is not connected!");
			return false;
		}
		dl.begin(LogActivity.sendWss);
		client.send(xmlString);
		dl.end(LogActivity.sendWss);
		stateChangeListener.onMessageSent(xmlString);
		return true;
	}

	@Override
	public synchronized boolean sendMessage(Document doc) {
		boolean result = sendMessage(doc, false);
		return result;
	}

	@Override
	public synchronized boolean sendMessageWithoutXmlDeclaration(@NotNull Document doc) {
		return sendMessage(doc, true);
	}

	@Override
	public synchronized TestConnectivityState authenticate() {
		// Not necessary since it is part of the connect protocol
		System.err.println("User '" + user.getConsistentUniqueReadableIdentifier() + "' does not need to send an authentication message!");
		return getActualConnectivityState();
	}


	@Override
	public synchronized void disconnect(boolean logInconsistentState) {
		if (!this.isConnected() && logInconsistentState) {
			System.err.println("Cannot disconnect client '" + user.getConsistentUniqueReadableIdentifier() + "' because it is not connected!");
		}
		try {
			client.closeBlocking();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		setConnectivityState(TestConnectivityState.Disconnected);
	}


	@Override
	public boolean isConnected() {
		if (client != null) {
			log.trace(user.getConsistentUniqueReadableIdentifier() +  "-client.isOpen=" + client.isOpen());
			log.trace(user.getConsistentUniqueReadableIdentifier() + "-client.isClosed=" + client.isClosed());
		}
		log.trace(user.getConsistentUniqueReadableIdentifier() + "ConnectivityState=" + _connectivityState);
		boolean actuallyConnected = client != null && client.isOpen() && !client.isClosed();
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
		if (client != null) {
			client.close();
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
		if (secondaryOutputTarget != OutputTarget.DEV_NULL) {
			try {
				secondaryOutputStream.write(response.getBytes());
				secondaryOutputStream.flush();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		stateChangeListener.onMessageReceived(response);
	}

	public interface TakWebsocketClientInterface {
		void send(@NotNull String xmlData);

		boolean isOpen();

		boolean isClosed();

		void close();

		void closeBlocking() throws InterruptedException;
	}

	public static class SessionIdFetcher {

		public static final int MAX_SIZE = 65536;
		public static final String TAKPROTO_PATH = "/takproto/1";

		private interface TakserverService {
			@GET("/index.html")
			Call<ResponseBody> getSessionToken();
		}

		private final SSLHelper.TakClientSslContext sslContext;
		private final String serverUrl;

		public SessionIdFetcher(@NotNull SSLHelper.TakClientSslContext sslContext, @NotNull String serverUrl) {
			this.sslContext = sslContext;
			this.serverUrl = serverUrl;
		}

		public String getSessionId() {
			try {
				sslContext.init();
				OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder()
						.sslSocketFactory(sslContext.getSslSocketFactory(), sslContext.getTrustManager())
						.hostnameVerifier((s, sslSession) -> {
							// TODO: Not this
							return true;
						});

				Retrofit retrofit = new Retrofit.Builder()
						.baseUrl(serverUrl)
						.client(okHttpClientBuilder.build())
						.build();
				TakserverService service = retrofit.create(TakserverService.class);
				String cookie = service.getSessionToken().execute().headers().get("Set-Cookie");
				return cookie.substring(0, cookie.indexOf(";"));
			} catch (GeneralSecurityException | IOException e) {
				throw new RuntimeException(e);
			}
		}
	}


	private static class TakWebsocketClient extends WebSocketClient implements TakWebsocketClientInterface {

		private interface TakserverService {
			@GET("/index.html")
			Call<ResponseBody> getSessionToken();
		}

		private static final int MAX_SIZE = 65536;
		private static final String STORETYPE = "JKS";
		private static final String CLIENT_CERT_PASSWORD = "atakatak";
		private static final String TAKPROTO_PATH = "/takproto/1";

		@Override
		public void send(byte[] data) {
			if (data[0] == '<') {
				super.send(new String(data));
			} else {
				super.send(data);
			}
		}

		private ByteBuffer convertStringToProtoBufBuffer(String xml) {
			ByteBuffer buffer;
			try {
				CotEventContainer data = new CotEventContainer(DocumentHelper.parseText(xml));

				//
				// Convert CotEventContainer to protobuf
				//
				Takmessage.TakMessage takMessage = StreamingProtoBufHelper.getInstance().cot2protoBuf(data);
				if (takMessage == null) {
					System.err.println("cot2protoBuf failed to parse message!");
					return null;
				}

				//
				// allocate a buffer for the message
				//
				int takMessageSize = takMessage.getSerializedSize();
				if (takMessageSize > MAX_SIZE) {
					throw new RuntimeException(("TOO BIG!"));
				}

				int sizeOfSize = CodedOutputStream.computeUInt32SizeNoTag(takMessageSize);
				buffer = ByteBuffer.allocate(1 + sizeOfSize + takMessageSize);

				//
				// write out the message to the buffer
				//
				CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(buffer);
				codedOutputStream.write(StreamingProtoBufHelper.MAGIC);
				codedOutputStream.writeUInt32NoTag(takMessageSize);
				takMessage.writeTo(codedOutputStream);
				((Buffer) buffer).rewind();

			} catch (DocumentException | IOException e) {
				throw new RuntimeException(e);
			}
			return buffer;
		}


		protected static TakWebsocketClient buildAndConnectWebsocketClient(AbstractUser user, Consumer<String> responseListener, DurationLogger dl) throws GeneralSecurityException, IOException, URISyntaxException {
			String baseUrl = "https://" + user.getServer().getUrl() + ":" + user.getServer().getCertHttpsPort();
			SSLHelper.TakClientSslContext tcsc = new SSLHelper.TakClientSslContext(user);
			SessionIdFetcher sif = new SessionIdFetcher(tcsc, baseUrl);
			String cookie = sif.getSessionId();

			Map<String, String> headers = new HashMap<>();
			headers.put("Cookie", cookie);
			TakWebsocketClient client = new TakWebsocketClient(new URI(baseUrl + TAKPROTO_PATH), headers, responseListener, dl);
			client.setTcpNoDelay(true);
			client.setSocketFactory(tcsc.getSslSocketFactory());
			try {
				client.connectBlocking();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			return client;
		}

		private final static ThreadPoolExecutor tpe = (ThreadPoolExecutor) Executors.newCachedThreadPool();
		private final Object lock = new Object();

		private final Consumer<String> responseListener;
		private final DurationLogger dl;

		public TakWebsocketClient(@NotNull URI serverUri, @NotNull Map<String, String> httpHeaders,
		                          @NotNull Consumer<String> responseListener, @NotNull DurationLogger dl) {
			super(serverUri, httpHeaders);
			this.responseListener = responseListener;
			this.dl = dl;
		}

		@Override
		public void send(String text) {
			ByteBuffer bb = convertStringToProtoBufBuffer(text);
			this.send(bb);
		}

		@Override
		public void send(ByteBuffer buffer) {
			super.send(buffer);
		}

		@Override
		public void onOpen(ServerHandshake handshakedata) {
			System.out.println("Socket Opened");
		}

		@Override
		public void onMessage(String message) {
			throw new RuntimeException("Unexpected Message type 'String'!");
		}

		private ByteBuffer leftovers = null;
		private boolean gotMagic = false;
		private int nextShift = 0;
		private int nextSize = 0;

		private boolean readSize(ByteBuffer buffer) {
			while (buffer.remaining() > 0) {
				byte b = buffer.get();
				if ((b & 0x80) == 0) {
					nextSize = nextSize | (b << nextShift);
					return true;
				} else {
					nextSize |= (b & 0x7F) << nextShift;
					nextShift += 7;

					// TODO check for varint max size of 64 bits
				}
			}
			return false;
		}

		@Override
		public void onMessage(ByteBuffer buffer) {
			dl.begin(LogActivity.receiveWss);
			dl.end(LogActivity.receiveWss);

			tpe.submit(() -> {
				try {
					synchronized (lock) {
						ByteBuffer fullBuf;
						if (leftovers == null) {
							// first time through leftovers is null, set fullbuf to new buffer
							fullBuf = buffer;
						} else {
							//  set fullbuf to leftovers + buffer
							int binaryLength = buffer.remaining();
							fullBuf = ByteBuffer.allocate(leftovers.remaining() + binaryLength);
							fullBuf.put(leftovers);
							fullBuf.put(buffer);
							((Buffer) fullBuf).flip();
							leftovers = null;
						}

						// try to parse messages out of fullbuf
						while (fullBuf.remaining() > 0) {

							// have we read a size from the stream yet?
							if (nextSize == 0) {

								// size is preceded by the magic byte
								if (!gotMagic) {
									byte nextMagic = fullBuf.get();
									gotMagic = nextMagic == StreamingProtoBufHelper.MAGIC;
									if (!gotMagic) {
										System.err.println("Failed to find magic byte, instead found " + nextMagic);
										break;
									}
								}

								if (!readSize(fullBuf)) {
									// haven't read complete size, stash the fullbuf in leftovers for next time around
									leftovers = ByteBuffer.allocate(fullBuf.remaining());
									leftovers.put(fullBuf);
									((Buffer) leftovers).flip();
									break;
								}
							}

							// do we have enough left in the buffer to read out a full message?
							if (fullBuf.remaining() < nextSize) {
								// haven't got enough for a message, stash the fullbuf in leftovers for next time around
								leftovers = ByteBuffer.allocate(fullBuf.remaining());
								leftovers.put(fullBuf);
								((Buffer) leftovers).flip();
								break;
							}

							// copy bytes for next message into eventBytes
							byte[] eventBytes = new byte[nextSize];
							fullBuf.get(eventBytes);

							// parse and broadcast the message
							Takmessage.TakMessage takMessage = Takmessage.TakMessage.parseFrom(eventBytes);
							CotEventContainer cotEventContainer = StreamingProtoBufHelper.getInstance().proto2cot(takMessage);
							String result = cotEventContainer.asXml();
							responseListener.accept(result);

							// reset parser state
							nextSize = 0;
							nextShift = 0;
							gotMagic = false;
							leftovers = null;
						}
					}
				} catch (Exception e) {
					System.err.println("Exception in onDataReceived: " + e.getMessage());
					close();
				}
			});
		}

		@Override
		public void onClose(int code, String reason, boolean remote) {
			if (remote) {
				System.out.println("Remote closed connection with code " + code + ". Reason: " + reason);
			} else {
				System.out.println("Closed connection with code " + code + ". Reason: " + reason);
			}
		}

		@Override
		public void onError(Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public static void main(String[] args) {
		System.setProperty("org.jboss.security.ignoreHttpsHost", "true");
		ConnectibleTakprotoClient client = new ConnectibleTakprotoClient(ImmutableUsers.s0_authstcp_authwssuser01_01f, new ClientResponseListener() {
			@Override
			public void onMessageReceived(String response) {
				System.out.println("RECEIVED: " + response);
			}

			@Override
			public void onMessageSent(String message) {
				System.out.println("SENDING: " + message);

			}

			@Override
			public void onConnectivityStateChange(TestConnectivityState state) {

			}
		});
		client.connect(true, null);
	}
}
