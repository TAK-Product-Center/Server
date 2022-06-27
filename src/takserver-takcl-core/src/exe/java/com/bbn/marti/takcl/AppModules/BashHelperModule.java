package com.bbn.marti.takcl.AppModules;

import com.bbn.marti.takcl.AppModules.generic.AppModuleInterface;
import com.bbn.marti.takcl.BashCompletionHelper;
import com.bbn.marti.takcl.TAKCL;
import com.bbn.marti.takcl.TAKCLCore;
import com.bbn.marti.takcl.cli.simple.Command;
import com.bbn.marti.takcl.config.common.TakclRunMode;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Runs a selection of tests
 *
 */
public class BashHelperModule implements AppModuleInterface {

	@NotNull
	@Override
	public TakclRunMode[] getRunModes() {
		return new TakclRunMode[]{TakclRunMode.LOCAL_SERVER_INTERACTION, TakclRunMode.REMOTE_SERVER_INTERACTION};
	}

	@Override
	public ServerState getRequiredServerState() {
		return ServerState.STOPPED;
	}

	@Override
	public String getCommandDescription() {
		return "Provides utilities for bash autocompletion support.";
	}

	@Override
	public void init() {
		TAKCLCore.TEST_MODE = true;
	}

//    /**
//     * Runs a selection of general tests against the server that may take a few minutes.
//     */
//    @Command(description = "Runs several tests against the server that may take a few minutes")
//    public void runGeneralTests() {
//        GeneralTests tests = new GeneralTests();
//        tests.anonWithGroupInputTest();
//        tests.groupToNonGroup();
//        tests.latestSAAnon();
//        tests.latestSADisconnectTest();
//        tests.LatestSAFileAuth();
//        tests.LatestSAInputGroups();
//        tests.mcastTest();
//        tests.streamTcpTest();
//        tests.tcpTest();
//        tests.udpTest();
//    }

//    /**
//     * Runs a few tests related to inputs that may take several minutes.
//     */
//    @Command(description = "Runs a couple input related tests that may take several minutes")
//    public void runInputTests() {
//        InputTests tests = new InputTests();
//        tests.inputRemoveAddTest();
//    }

//    @Command
//    public void testImmortality() {
//        ImmortalityTest test = new ImmortalityTest();
//        test.setup();
//        test.test0();
//    }
//
//    @Command
//    public void bandwidthTest(@NotNull BaseConnections connectionType, @NotNull Integer clientCount, @NotNull Integer sendInterval, @NotNull Integer sendDurationMS, @NotNull Integer clientConnectionDurationMS, @Nullable String serverIp) {
//       innerBandwidthTest(serverIp, connectionType, clientCount, sendInterval, sendDurationMS, clientConnectionDurationMS);
//    }
//
//    @Command
//    public void bandwidthTest(@NotNull BaseConnections connectionType, @NotNull Integer clientCount, @NotNull Integer sendInterval, @NotNull Integer sendDurationMS, @NotNull Integer clientConnectionDurationMS) {
//        innerBandwidthTest(null, connectionType, clientCount, sendInterval, sendDurationMS, clientConnectionDurationMS);
//    }
//
//
//    public void innerBandwidthTest(@Nullable String serverIp, @NotNull ConnectionInterface connectionType, @NotNull Integer clientCount, @NotNull Integer sendInterval, @NotNull Integer sendDurationMS, @NotNull Integer clientConnectionDurationMS) {
//        Random random = new Random();
//        final ScheduledExecutorService executor = Executors.newScheduledThreadPool(clientCount);
//        final Set<ScheduledFuture> runningTasks = new HashSet<>();
//        Set<UserInterface> users = new HashSet<>();
//        final Set<UnifiedClient> clients = new HashSet<>();
//        Set<Runnable> runnables = new HashSet<>();
//
//        final AtomicBoolean waitLock = new AtomicBoolean(false);
//
//        final InputStreamReader cin = new InputStreamReader(System.in);
//
//        // Construct a general dummy message to send
//        final String cotMessage = CotGenerator.createMessage("dummy").asXML();
//
//
//        // Determine the best user to base the test on
//        TemplateUsers modelUser = null;
//
//        for (TemplateUsers loopUser : TemplateUsers.values()) {
//            if (loopUser.getDefinedGroupSet() == GroupSetProfiles.Set_None) {
//                if (connectionType.requiresAuthentication()) {
//                    if (loopUser.getUserName() != null && loopUser.getPassword() != null) {
//                        modelUser = loopUser;
//                        break;
//                    }
//                } else {
//                    if (loopUser.getUserName() == null && loopUser.getPassword() == null) {
//                        modelUser = loopUser;
//                        break;
//                    }
//                }
//            }
//        }
//
//        TestEngine engine = null;
//
//        // If the serverIp is not defined, create a server to use
//        if (serverIp == null) {
//            engine = new TestEngine(ServerProfiles.SERVER_CLI);
//            engine.engineFactoryReset();
//        }
//
//        // This shouldn't be possible, but the IDE complains less...
//        if (modelUser != null) {
//            for (int i = 0; i < clientCount; i++) {
//                UserInterface user;
//                if (engine != null) {
//                    // If the engine is in use, use predefined values
//                    user = TestDataFactory.generateUser(modelUser, String.valueOf(i), connectionType, ServerProfiles.SERVER_CLI);
//                    engine.offlineAddUsersAndConnectionsIfNecessary(user);
//                } else {
//                    // Otherwise, supply the serverIp to create a new server with the specified IP
//                    user = TestDataFactory.generateUser(modelUser, String.valueOf(i), connectionType, serverIp);
//                }
//                users.add(user);
//            }
//
//            if (engine != null) {
//                System.out.println("Creating new server and starting it.");
//                engine.startServer(ServerProfiles.SERVER_CLI, ServerInstance.ServerOutputTarget.FILE);
//
//            } else {
//                System.out.println("Please ensure a server with an " + connectionType.getConsistentUniqueReadableIdentifier() + " connection on port " + connectionType.getPort() +
//                        (connectionType.getProtocol() == ProtocolProfiles.INPUT_MCAST ? "with mcast group " + connectionType.getMCastGroup() : "") +
//                " is running on " + serverIp +  " and then press any key.");
//                try {
//                    cin.read();
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//
//            for (UserInterface user : users) {
//
//                // For each user, set up a listener that will wait until the waitLock is false before it continues. Crude, but does the job and doesn't require modifying the Client classes
//                ClientResponseListener listener = new ClientResponseListener() {
//                    @Override
//                    public void onMessageReceived(String response) {
//                        while (waitLock.get()) {
//                            try {
//                                Thread.sleep(1000);
//                            } catch (InterruptedException e) {
//                                throw new RuntimeException();
//                            }
//                        }
//                    }
//
//                    @Override
//                    public void onDisconnected() {
//                        System.out.println("Disconnected");
//                    }
//                };
//
//                Runnable runnable;
//
//                final SendingInterface client;
//
//
//                if (user.getConnection().getProtocol().canConnect()) {
//                    // If the user can connect, connect, create a ConnectibleClient and connect
//                    ConnectibleClient newClient = new ConnectibleClient(user, listener, serverIp);
//                    client = newClient;
//                    newClient.connect(true, null);
//                } else {
//                    // Otherwise, create a sending client
//                    client = new SendingClient(user, serverIp);
//                }
//
//                runnable = new Runnable() {
//                    @Override
//                    public void run() {
//                        client.sendMessage(cotMessage);
//                    }
//                };
//
//                runnables.add(runnable);
//                runningTasks.add(executor.scheduleWithFixedDelay(runnable, (long) random.nextInt(sendDurationMS / 4), (long) sendInterval, TimeUnit.MILLISECONDS));
//
//
//            }
//
//            final Timer timer = new Timer();
//
//            // A TimerTask to stop sending after the specified time interval
//            final TimerTask stopSendTask = new TimerTask() {
//                @Override
//                public synchronized void run() {
//                    System.out.println("Halting sending...");
//                    executor.shutdown();
//                }
//            };
//
//            // A TimerTask to disconnect all connected clients after the specified time interval
//            final TimerTask disconnectTask = new TimerTask() {
//                @Override
//                public void run() {
//                    System.out.println("Disconnecting clients...");
//                    for (SendingInterface client : clients) {
//                        if (client instanceof ConnectibleClient) {
//                            ConnectibleClient c = (ConnectibleClient) client;
//                            c.disconnect();
//                        }
//                    }
//                }
//            };
//
//            timer.schedule(stopSendTask, sendDurationMS.longValue());
//            timer.schedule(disconnectTask, clientConnectionDurationMS.longValue());
//
//            // Crude but effective way to toggle pausing and resuming client reads...
//            while (true) {
//                try {
//                    if (waitLock.get()) {
//                        System.out.println("Press any key to continue client reads.");
//                    } else {
//                        System.out.println("Press any key to halt client reads.");
//                    }
//                    cin.read();
//                    boolean value = waitLock.get();
//                    waitLock.set(!value);
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//        }
//    }

	/**
	 * Creates a BASH script that can be sourced to add "takcl" as a command line tool and enable bash-completion.
	 * <p>
	 * The "takcl" alias references the jar file used to generate the file
	 * <p>
	 * The bash-completion uses a unique format to indicate parameters that need to be typed for each command as follows:
	 * <p>
	 * For a method "foo(String bar)", the bash autocompletion would be "foo[bar]"
	 * For a method with multiple parameters such as "foo(String bar, String cube)", the bash autocompletion would be "foo[bar,cube]".
	 * <p>
	 * Also, if the first parameter specified is a boolean or an enum, that will be bash-completion option. It will still be indicated via the above rule.
	 * <p>
	 * For example:
	 * For a method "foo(boolean bar, String cube)", after completing "foo", it would give you the autocompletion options of "true" and "false". The autocompletion method would be "foo[bar,cube]" similarly to above.
	 *
	 * @param targetFile The target location for the script. if it exists, an error will occur.
	 */
	@Command(description = "Creates a script that can be sourced to add the curently running takcl to your path and add bash completion support.")
	@SuppressWarnings("unused")
	public void createSourceableScript(@NotNull String targetFile) {
		try {
			String jarLocation = new File(TAKCLCore.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getAbsolutePath();
			String autocompletionText = BashCompletionHelper.generateBashCompletionFileContents(TAKCL.moduleMap, jarLocation);
			Files.write(Paths.get(targetFile), autocompletionText.getBytes(), StandardOpenOption.CREATE_NEW);

		} catch (IOException | URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}
}
