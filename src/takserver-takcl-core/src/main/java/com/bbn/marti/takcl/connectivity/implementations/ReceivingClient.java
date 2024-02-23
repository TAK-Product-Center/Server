package com.bbn.marti.takcl.connectivity.implementations;

import com.bbn.marti.takcl.SSLHelper;
import com.bbn.marti.takcl.TAKCLCore;
import com.bbn.marti.takcl.connectivity.interfaces.ClientResponseListener;
import com.bbn.marti.takcl.connectivity.interfaces.ReceivingInterface;
import com.bbn.marti.test.shared.data.users.AbstractUser;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static com.bbn.marti.takcl.TAKCLProfilingLogging.DurationLogger;
import static com.bbn.marti.takcl.TAKCLProfilingLogging.LogActivity;

/**
 * Created on 11/13/15.
 */
public class ReceivingClient extends Thread implements ReceivingInterface {


	private static final int BUFFER_SIZE = 1048576;

	private final AbstractUser user;
	private final ClientResponseListener listener;
	private final Logger log;
	private final DurationLogger dl;

	private ServerSocket tcpSocket;

	private OutputTarget secondaryOutputTarget = OutputTarget.DEV_NULL;
	private OutputStream secondaryOutputStream;

	ReceivingClient(@NotNull AbstractUser user, @NotNull final ClientResponseListener listener) {
		if (!user.getConnection().getProtocol().canListen() || user.getConnection().getProtocol().canConnect()) {
			throw new RuntimeException("ProtocolProfiles '" + user.getConnection().getProtocol().toString() + "' is not a receive-only protocol!");
		}

		this.user = user;
		this.listener = listener;
		this.log = LoggerFactory.getLogger(ReceivingClient.class);
		this.dl = new DurationLogger(user.getConsistentUniqueReadableIdentifier(), log);
		start();
	}

	@Override
	public void run() {
		switch (user.getConnection().getProtocol()) {

			case SUBSCRIPTION_MCAST:
				runMCast();
				break;

			case SUBSCRIPTION_UDP:
				runUDP();
				break;

			case SUBSCRIPTION_TCP:
				runTCP();
				break;

			case SUBSCRIPTION_STCP:
				runSTCP();
				break;

			case SUBSCRIPTION_SSL:
			case SUBSCRIPTION_TLS:
				runSSL();
				break;

			default:
				throw new RuntimeException("Unepxected Receiving protocol '" + user.getConnection().getProtocol() + "'!");
		}

	}

	private void runSSL() {
		try {
			tcpSocket = SSLHelper.getInstance().createSSLServerSocket(user.getConnection().getPort());
			dl.begin(LogActivity.connect);
			Socket connectionSocket = tcpSocket.accept();
			dl.end(LogActivity.connect);
			InputStream socketInputStream = connectionSocket.getInputStream();

			int bytesRead;
			byte[] bytes = new byte[BUFFER_SIZE];

			while ((bytesRead = socketInputStream.read(bytes)) != 0 && !isInterrupted()) {
				if (bytesRead > 0) {
					dl.begin(LogActivity.getReceive(user));
					String str = new String(bytes, 0, bytesRead);
					responseReceived(str);
					dl.end(LogActivity.getReceive(user));
				}
			}

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void runSTCP() {
		try {
			tcpSocket = new ServerSocket(user.getConnection().getPort());
			dl.begin(LogActivity.connect);
			Socket connectionSocket = tcpSocket.accept();
			dl.end(LogActivity.connect);
			InputStream socketInputStream = connectionSocket.getInputStream();

			int bytesRead;
			byte[] bytes = new byte[BUFFER_SIZE];

			while ((bytesRead = socketInputStream.read(bytes)) != 0 && !isInterrupted()) {
				if (bytesRead > 0) {
					dl.begin(LogActivity.getReceive(user));
					String str = new String(bytes, 0, bytesRead);
					responseReceived(str);
					dl.end(LogActivity.getReceive(user));
				}
			}

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void runTCP() {
		try {
			tcpSocket = new ServerSocket(user.getConnection().getPort());

			while (!interrupted()) {
				try {
					dl.begin(LogActivity.connect);
					Socket connectionSocket = tcpSocket.accept();
					dl.end(LogActivity.connect);

					InputStream socketInputStream = connectionSocket.getInputStream();

					int bytesRead;
					byte[] bytes = new byte[BUFFER_SIZE];

					while ((bytesRead = socketInputStream.read(bytes)) != 0) {
						if (bytesRead > 0) {
							dl.begin(LogActivity.getReceive(user));
							String str = new String(bytes, 0, bytesRead);
							responseReceived(str);
							connectionSocket.close();
							dl.end(LogActivity.getReceive(user));
						}
					}
				} catch (SocketException e) {
					// Expected, open-squirt-close
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void runUDP() {
		dl.begin(LogActivity.listen);
		try {
			DatagramSocket udpSocket = new DatagramSocket(user.getConnection().getPort());
			byte[] receiveBuffer = new byte[BUFFER_SIZE];

			while (!interrupted()) {
				try {
					DatagramPacket packet = new DatagramPacket(receiveBuffer, receiveBuffer.length);
					udpSocket.receive(packet);
					dl.begin(LogActivity.getReceive(user));
					String str = new String(packet.getData(), 0, packet.getLength());
					responseReceived(str);
					dl.end(LogActivity.getReceive(user));
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}

		} catch (SocketException e) {
			throw new RuntimeException(e);
		} finally {
			dl.end(LogActivity.listen);
		}
	}

	private void runMCast() {
		dl.begin(LogActivity.listen);
		try {
			MulticastSocket mcastSocket = new MulticastSocket(user.getConnection().getPort());
			mcastSocket.joinGroup(Inet4Address.getByName(user.getConnection().getMCastGroup()));

			byte[] buffer = new byte[BUFFER_SIZE];
			DatagramPacket data = new DatagramPacket(buffer, buffer.length);

			// TODO: Maybe make this stoppable. Normally I would. But for the time being it's easiest to not bother since this is a test framework
			while (!isInterrupted()) {
				try {
					mcastSocket.receive(data);
					dl.begin(LogActivity.getReceive(user));
					String str = new String(buffer, 0, data.getLength());
					responseReceived(str);
					dl.end(LogActivity.getReceive(user));
				} catch (IOException e) {
					System.err.println(e.getMessage());
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			dl.end(LogActivity.listen);
		}
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
						Files.createDirectory(Paths.get(rootDir));
						secondaryOutputStream = Files.newOutputStream(Paths.get(rootDir + user.getConsistentUniqueReadableIdentifier()));

					} catch (IOException e) {
						throw new RuntimeException(e);
					}
					break;

				case INHERIT_IO:
					// Do nothing
					break;

				case DEV_NULL:
					// Do nothing
					break;
			}
			secondaryOutputTarget = target;
		}

	}

	@Override
	public AbstractUser getProfile() {
		return user;
	}

	private synchronized void responseReceived(String response) {
		if (secondaryOutputTarget != OutputTarget.DEV_NULL) {
			try {
//				dl.begin("Write Response to Secondary Stream");
				secondaryOutputStream.write(response.getBytes());
				secondaryOutputStream.flush();
//				dl.end("Write Response to Secondary Stream");
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

//		dl.begin("listener.onMessageReceived");
		listener.onMessageReceived(response);
//		dl.end("listener.onMessageReceived");
	}


	public void cleanup() {
		interrupt();
	}

}
