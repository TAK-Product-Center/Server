package com.bbn.marti.takcl.connectivity.implementations;

import com.bbn.marti.takcl.connectivity.interfaces.ClientResponseListener;
import com.bbn.marti.takcl.connectivity.interfaces.SendingInterface;
import com.bbn.marti.test.shared.CotGenerator;
import com.bbn.marti.test.shared.data.connections.AbstractConnection;
import com.bbn.marti.test.shared.data.protocols.ProtocolProfiles;
import com.bbn.marti.test.shared.data.users.AbstractUser;
import org.dom4j.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.*;

import static com.bbn.marti.takcl.TAKCLProfilingLogging.DurationLogger;
import static com.bbn.marti.takcl.TAKCLProfilingLogging.LogActivity;


/**
 * Created on 11/13/15.
 */
public class SendingClient implements SendingInterface {

	private static int minPort = 51532;
	private static int currentPort = minPort;
	private static int maxPort = 58144;

	private final ClientResponseListener stateChangeListener;

	private static synchronized int getNextPort() {
		if (currentPort > maxPort) {
			currentPort = minPort;
		}
		currentPort++;
		return currentPort;
	}

	private final AbstractUser user;

	private final MulticastSocket socket;
	private final InetAddress group;

	private final int udpPort;
	private final Logger log;
	private final DurationLogger dl;

	public SendingClient(@NotNull AbstractUser user, @NotNull ClientResponseListener listener) {
		if (!user.getConnection().getProtocol().canSend() || user.getConnection().getProtocol().canConnect()) {
			throw new RuntimeException("ProtocolProfiles " + user.getConnection().getProtocol().toString() + "' is not a send-only protocol!");
		}

		this.user = user;
		this.udpPort = getNextPort();
		this.stateChangeListener = listener;

		if (user.getConnection().getProtocol() == ProtocolProfiles.INPUT_MCAST) {
			try {
				group = Inet4Address.getByName(user.getConnection().getMCastGroup());
				socket = new MulticastSocket(user.getConnection().getPort());
				socket.joinGroup(group);

			} catch (IOException e) {
				System.err.println("Try turning off any additional connections (such as wifi). MCast sometimes doesn't like that....");
				throw new RuntimeException(e);
			}
		} else {
			socket = null;
			group = null;
		}
		this.log = LoggerFactory.getLogger(SendingClient.class);
		this.dl = new DurationLogger(user.getConsistentUniqueReadableIdentifier(), log);
	}

	private boolean sendMessage(@NotNull Document doc, boolean omitXmlDeclaration) {
		dl.begin(LogActivity.getSend(user));
//		dl.begin("Document to String");
		String xmlData = omitXmlDeclaration ? CotGenerator.stripXmlDeclaration(doc) : doc.asXML();
//		dl.end("Document to String");

		switch (user.getConnection().getProtocol()) {
			case INPUT_TCP:
				sendTCPMessage(user.getServer().getUrl(), user.getConnection().getPort(), xmlData);
//				dl.begin("stateChangeListener.onMessageSent");
				stateChangeListener.onMessageSent(xmlData);
//				dl.end("stateChangeListener.onMessageSent");
				break;

			case INPUT_UDP:
				try {
					sendUDPMessage(user.getServer().getUrl(), user.getConnection().getPort(), udpPort, xmlData);
//					dl.begin("stateChangeListener.onMessageSent");
					stateChangeListener.onMessageSent(xmlData);
//					dl.end("stateChangeListener.onMessageSent");
				} catch (BindException e) {
					System.err.println(e.getMessage());
				}
				break;

			case INPUT_MCAST:
				try {
//					dl.begin("Construct DatagramPacket");
					DatagramPacket data = new DatagramPacket(xmlData.getBytes(), xmlData.length(),
							group, user.getConnection().getPort());
//					dl.end("Construct DatagramPacket");
					socket.send(data);
//					dl.begin("stateChangeListener.onMessageSent");
					stateChangeListener.onMessageSent(xmlData);
//					dl.end("stateChangeListener.onMessageSent");
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				break;
		}

		dl.end(LogActivity.getSend(user));

		// TODO: Maybe this shouldn't always be false?
		return false;
	}

	@Override
	public boolean sendMessage(Document doc) {
		return sendMessage(doc, false);
	}

	@Override
	public boolean sendMessageWithoutXmlDeclaration(@NotNull Document doc) {
		return sendMessage(doc, true);
	}

	@Override
	public void cleanup() {
		if (socket != null) {
			socket.close();
		}
	}

	@Override
	public AbstractUser getProfile() {
		return user;
	}

	public static void sendUDPMessage(@NotNull String address, int destPort, @Nullable Integer srcPort, @Nullable String userIdentifier, @Nullable String userPassword, @Nullable String uid, @NotNull String sendXML) throws BindException {
		try {
			String sendString = CotGenerator.createAuthPayload(userIdentifier, userPassword, uid, sendXML);

			if (srcPort == null) {
				srcPort = getNextPort();
			}

			sendUDPMessage(address, destPort, srcPort, sendString);
		} catch (BindException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}


	public static void sendUDPMessage(@NotNull String address, int destPort, @Nullable Integer srcPort, @NotNull String sendXML) throws BindException {

		DatagramSocket datagramSocket = null;

		try {

			if (srcPort == null) {
				srcPort = getNextPort();
			}

			InetSocketAddress destAddress = new InetSocketAddress(address, destPort);
			InetSocketAddress srcAddress = new InetSocketAddress(address, srcPort);
			datagramSocket = new DatagramSocket(srcAddress);

			byte[] sendData;

			sendData = sendXML.getBytes();

			DatagramPacket packet = new DatagramPacket(sendData, sendData.length, destAddress);

			datagramSocket.send(packet);


		} catch (BindException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if (datagramSocket != null) {
				datagramSocket.disconnect();
				datagramSocket.close();
			}
		}
	}

	public static void sendMCASTMessage(int port, @NotNull String groupString, @NotNull String sendString) {
		try {
			InetAddress group = Inet4Address.getByName(groupString);
			MulticastSocket socket = new MulticastSocket(port);
			DatagramPacket data = new DatagramPacket(
					sendString.getBytes(), sendString.length(), group, port);
			socket.send(data);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}


	public static void sendTCPMessage(@NotNull String address, int port, String userIdentifier, String userPassword, String uid, String sendXML) {
		String sendString = CotGenerator.createAuthPayload(userIdentifier, userPassword, uid, sendXML);
		sendTCPMessage(address, port, sendString);
	}


	public static void sendTCPMessage(@NotNull String address, int port, String sendXML) {
		OutputStream socketOutputStream = null;
		Socket socket = null;

		try {
			socket = new Socket(address, port);
			socketOutputStream = new DataOutputStream(socket.getOutputStream());
			socketOutputStream.write(sendXML.getBytes());
			socketOutputStream.flush();
		} catch (IOException e) {
			System.err.println(e.toString());
		}

		try {
			if (socketOutputStream != null) socketOutputStream.close();
			if (socket != null) socket.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


	public static void sendTCPMessage(@NotNull String address, @NotNull AbstractConnection input, @NotNull AbstractUser user, @NotNull String sendXML) {
		sendTCPMessage(address, input.getPort(), user.getUserName(), user.getPassword(), user.getUserName(), sendXML);
	}
}
