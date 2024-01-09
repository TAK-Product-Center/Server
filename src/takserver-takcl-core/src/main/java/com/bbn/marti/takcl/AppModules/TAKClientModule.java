package com.bbn.marti.takcl.AppModules;

import com.bbn.marti.takcl.AppModules.generic.AppModuleInterface;
import com.bbn.marti.takcl.cli.simple.Command;
import com.bbn.marti.takcl.config.common.TakclRunMode;
import com.bbn.marti.takcl.connectivity.implementations.SendingClient;
import com.bbn.marti.takcl.connectivity.implementations.UnifiedClient;
import com.bbn.marti.takcl.connectivity.interfaces.ReceivingInterface;
import com.bbn.marti.test.shared.CotGenerator;
import com.bbn.marti.test.shared.data.connections.BaseConnections;
import com.bbn.marti.test.shared.data.connections.MutableConnection;
import com.bbn.marti.test.shared.data.generated.CLINonvalidatingReceivingUsers;
import com.bbn.marti.test.shared.data.generated.CLINonvalidatingSendingUsers;
import com.bbn.marti.test.shared.data.generated.CLIReceivingInputProtocols;
import com.bbn.marti.test.shared.data.generated.CLISendingInputProtocols;
import com.bbn.marti.test.shared.data.protocols.ProtocolProfiles;
import com.bbn.marti.test.shared.data.protocols.ProtocolProfilesInterface;
import com.bbn.marti.test.shared.data.servers.ImmutableServerProfiles;
import com.bbn.marti.test.shared.data.users.AbstractUser;
import com.bbn.marti.test.shared.data.users.BaseUsers;
import com.bbn.marti.test.shared.data.users.MutableUser;
import org.dom4j.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 */
public class TAKClientModule implements AppModuleInterface {

	public static class StreamHandler {

		private final OutputStream outputStream;
		private final InputStream inputStream;

		public StreamHandler(String filePath) {
			try {

				File file = new File(filePath);
				this.outputStream = new FileOutputStream(file);
				this.inputStream = null;


			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}

		}

		public StreamHandler(OutputStream outputStream) {
			this.outputStream = outputStream;
			this.inputStream = null;
		}

		public StreamHandler(OutputStream outputStream, InputStream inputStream) {
			this.outputStream = outputStream;
			this.inputStream = inputStream;
		}

		public void write(String string) {
			write(string.getBytes());
		}

		public void write(byte[] bytes, int offset, int length) {
			write(new String(bytes, offset, length));
		}

		public void write(byte[] bytes) {
			try {
				outputStream.write(bytes);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}


	@NotNull
	@Override
	public TakclRunMode[] getRunModes() {
		return new TakclRunMode[0];
	}

	@Override
	public ServerState getRequiredServerState() {
		return ServerState.RUNNING;
	}

	@Override
	public String getCommandDescription() {
		return "Server client.";
	}

	@Override
	public void init() {
	}

	private void performUserAction(AbstractUser user, boolean doSend) {
		ProtocolProfiles protocol = user.getConnection().getProtocol();

		if (protocol.canConnect()) {
			UnifiedClient client = new UnifiedClient(user);

			if (doSend) {
				client.connect(true, CotGenerator.createLatestSAMessage(user).asXML());
			} else {
				client.connect(true, null);
			}

			tryListen(protocol, client);

		} else if (protocol.canSend() && doSend) {
			SendingClient client = new SendingClient(user, null);
			client.sendMessage(CotGenerator.createLatestSAMessage(user));

		} else {
			if (doSend) {
				System.err.println("Cannot send with protocol '" + protocol.name() + "'!");
			} else {
				System.err.println("Cannot listen with protocol '" + protocol.name() + "'!");
			}
		}
	}

	@Command
	public void sendWithUser(CLINonvalidatingSendingUsers user) {
		performUserAction(user.getUser(), true);
	}

	@Command
	public void receiveWithUser(CLINonvalidatingReceivingUsers user) {
		performUserAction(user.getUser(), false);
	}

	@Command
	public void sendWithInput(CLISendingInputProtocols protocol, Integer port) {
		mutableUserAction(protocol.getProtocolProfile(), port, true, null, null, null);

	}

	@Command
	public void receiveWithInput(CLIReceivingInputProtocols protocol, Integer port) {
		mutableUserAction(protocol.getProtocolProfile(), port, false, null, null, null);
	}

	@Command
	public void sendWithCredentials(CLISendingInputProtocols protocol, Integer port, String userName, String userPassword) {
		mutableUserAction(protocol.getProtocolProfile(), port, true, userName, userPassword, null);

	}

	@Command
	public void receiveWithCredentials(CLIReceivingInputProtocols protocol, Integer port, String userName, String userPassword) {
		mutableUserAction(protocol.getProtocolProfile(), port, false, userName, userPassword, null);
	}

	public void mutableUserAction(@NotNull ProtocolProfilesInterface protocol, @NotNull Integer port, @NotNull boolean doSend, @Nullable String userName, @Nullable String userPassword, @Nullable String[] destinations) {

		// Get the connection based on the protocol
		BaseConnections baseConnection = BaseConnections.getByProtocolProfile(protocol);

		if (protocol.canConnect()) {
			boolean doAuth = (userName != null && userPassword != null);

			// If authentication credentials were provided, switch to authenticating connections
			if (doAuth) {
				baseConnection = (
						protocol.getValue().equals(ProtocolProfiles.INPUT_SSL.getValue()) ? BaseConnections.authssl :
								protocol.getValue().equals(ProtocolProfiles.INPUT_STCP.getValue()) ? BaseConnections.authstcp :
										protocol.getValue().equals(ProtocolProfiles.INPUT_TLS.getValue()) ? BaseConnections.authtls :
												null);
			}


			// Construct the connection
			MutableConnection connection = new MutableConnection("CLI", baseConnection, port, ImmutableServerProfiles.SERVER_CLI.getMutableInstance());
			String userModifier = UUID.randomUUID().toString().substring(0, 8);
			MutableUser user;

			if (userName != null && userPassword != null) {
				user = connection.generateConnectionUser(userName, userPassword, false, userModifier);
			} else {
				user = connection.generateConnectionUser(BaseUsers.anonuser, false, userModifier);
			}

			UnifiedClient client = new UnifiedClient(user);


			if (doSend) {
				String message = CotGenerator.createLatestSAMessage(user).asXML();
				client.connect(true, message);
				System.out.println("Sent message :\n\n" + message);

			} else {
				client.connect(true, null);
			}

			client.setAdditionalOutputTarget(ReceivingInterface.OutputTarget.INHERIT_IO);
			tryListen(protocol, client);


		} else {
			if (userName != null || userPassword != null) {
				System.err.println("User credentials supplied with non-authenticatable protocol '" + protocol.getValue() + "'!");

			} else if (!doSend) {
				System.err.println("Cannot do anything other than send with protocol '" + protocol.getValue() + "'!");

			} else {
				MutableConnection connection = new MutableConnection("CLI", baseConnection, port, ImmutableServerProfiles.SERVER_CLI.getMutableInstance());
				String userModifier = UUID.randomUUID().toString().substring(0, 8);
				MutableUser user = connection.generateConnectionUser(BaseUsers.getByConnection(connection), false, userModifier);
				UnifiedClient client = new UnifiedClient(user);
				client.sendMessage(CotGenerator.createLatestSAMessage(user));
			}
		}
	}

	private void tryListen(ProtocolProfilesInterface protocol, UnifiedClient client) {
		if (protocol.canConnect()) {
			System.out.println("\n\nIt appears you send with a connection that can receive. The session will remain open so you can examine data received. You can also send additional messages by entering the following keys followed by enter:" +
					"\t'L' or 'l' - Sends Latest SA message" +
					"\t'M' or 'm' - Sends a general location message"
			);

			Document message;

			while (true) {
				try {
					byte[] buffer = new byte[4096];
					int readCount = System.in.read(buffer);

					String value = new String(buffer, 0, readCount);

					if (value.equals("L\n") || value.equals("l\n")) {
						message = CotGenerator.createLatestSAMessage(client.getProfile());
						client.sendMessage(message);
						System.out.println("Sent message :\n" + message.asXML() + "\n");

					} else if (value.equals("M\n") || value.equals("m\n")) {
						message = CotGenerator.createMessage(client.getProfile());
						client.sendMessage(message);
						System.out.println("Sent message :\n" + message.asXML() + "\n");

					} else {
						System.out.println("Invalid command '" + value + "' entered!");
					}

				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

    /*
    public void ConnectAndListenSSL(String username, String password) {
        final BooleanObject isConnected = new BooleanObject();

        isConnected.setValue(connect(TemplateConnections.ssl, TemplateUsers.ANONA, xmlFileToSend, new TAKServerResponseListener() {
            @Override
            public void onMessageReceived(String response) {
                System.out.println(response);
            }

            @Override
            public void onDisconnected() {
                isConnected.setValue(false);
            }
        }));

        while (isConnected.getValue()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                isConnected.setValue(null);
            }
        }
    }
    */

//    @Command
//    public void ConnectAndListenSSL(String xmlFileToSend) {
//        final BooleanObject isConnected = new BooleanObject();
//
//        ConnectibleClient client = new ConnectibleClient(GenValidatingUsers.s0Astdssl, new ClientResponseListener() {
//            @Override
//            public void onMessageReceived(String response) {
//
//            }
//
//            @Override
//            public void onDisconnected() {
//
//            }
//        }, null);
//
//        isConnected.setValue(client.connect(true));
//
//        isConnected.setValue(connect(TemplateConnections.ssl, GenValidatingUsers.s0Astdssl, GenValidatingUsers.s0Astdssl.getUserName(), xmlFileToSend, new TAKServerResponseListener() {
//            @Override
//            public void onMessageReceived(String response) {
//                System.out.println(response);
//            }
//
//            @Override
//            public void onDisconnected() {
//                isConnected.setValue(false);
//            }
//        }));
//
//        while (isConnected.getValue()) {
//            try {
//                Thread.sleep(100);
//            } catch (InterruptedException e) {
//                isConnected.setValue(null);
//            }
//        }
//    }

//
//    @Command
//    public void ConnectAndListen(@Nullable String xmlFileToSend) {
//        final BooleanObject isConnected = new BooleanObject();
//
//
//        ConnectibleClient client = new ConnectibleClient(GenValidatingUsers.s0Astreamtcp, new ClientResponseListener() {
//            @Override
//            public void onMessageReceived(String response) {
//                System.out.println(response);
//            }
//
//            @Override
//            public void onDisconnected() {
//                System.out.println("Disconnected.");
//
//            }
//        }, null);
//
//        isConnected.setValue(connect(TemplateConnections.stcp, GenValidatingUsers.s0Bstreamtcp, GenValidatingUsers.s0Bstreamtcp.getUserName(), xmlFileToSend, new TAKServerResponseListener() {
//            @Override
//            public void onMessageReceived(String response) {
//                System.out.println(response);
//            }
//
//            @Override
//            public void onDisconnected() {
//                isConnected.setValue(false);
//            }
//        }));
//
//        while (isConnected.getValue()) {
//            try {
//                Thread.sleep(100);
//            } catch (InterruptedException e) {
//                isConnected.setValue(null);
//            }
//        }
//    }
//
//    public void listenMulticast() {
//        int sourcePort = 6969;
//        String group = "239.2.3.1";
//        try {
//            MulticastSocket socket = new MulticastSocket(sourcePort);
//            socket.joinGroup(Inet4Address.getByName(group));
//
//            byte[] buffer = new byte[BUFFER_SIZE];
//
//            DatagramPacket data = new DatagramPacket(buffer, BUFFER_SIZE);
//
//            while (true) {
//                socket.receive(data);
//                String receivedString = new String(buffer, 0, data.getLength());
//                System.out.println(receivedString);
//            }
//
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }
}
