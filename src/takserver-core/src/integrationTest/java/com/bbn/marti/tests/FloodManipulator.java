package com.bbn.marti.tests;

import com.bbn.marti.takcl.AppModules.TAKCLConfigModule;
import com.bbn.marti.takcl.connectivity.implementations.SendingClient;
import com.bbn.marti.test.shared.CotGenerator;
import com.bbn.marti.test.shared.TestManipulatorInterface;
import com.bbn.marti.test.shared.data.protocols.ProtocolProfiles;
import com.bbn.marti.test.shared.data.protocols.ProtocolProfilesInterface;
import com.bbn.marti.test.shared.data.servers.ImmutableServerProfiles;
import com.bbn.marti.test.shared.data.connections.ConnectionFilter;
import com.bbn.marti.test.shared.data.connections.AbstractConnection;
import com.bbn.marti.test.shared.data.generated.ImmutableConnections;
import com.bbn.marti.test.shared.data.users.UserFilter;
import com.bbn.marti.test.shared.data.users.AbstractUser;
import com.bbn.marti.test.shared.engines.TestEngine;

import java.net.BindException;
import java.util.*;
import java.util.concurrent.*;

/**
 * Created on 11/5/15.
 */
public class FloodManipulator implements TestManipulatorInterface {

    private final ScheduledExecutorService executor;
    private final Random random;

    private final List<AbstractUser> sendableUserList = new LinkedList<>();
    private int sendableUserCount;

    private final List<AbstractUser> udpUserList = new LinkedList<>();
    private int udpUserCount;

    private final List<AbstractUser> connectableUserList = new LinkedList<>();

    private final int threadCount;
    private final int interval;
    private final int udpToAllRatio;

    private final String cotMessage = CotGenerator.createMessage("dummy").asXML();

    Set<ScheduledFuture> runningTasks = new HashSet<>();

    private int sendCount;
    private int receiveCount;

    /**
     * Floods the server with submissions. The total approximate rate (assuming things can keep up) is the following:
     * ((threadCount * (udpToAllRatio + 1))/interval) per millisecond
     * <p/>
     * The threads will run in parallel, and at each interval, the number of INPUT_UDP threads specified and one random thread will send out a message.
     *
     * @param threadCount   The number of threads to spawn
     * @param interval      How often to execute sending
     * @param udpToAllRatio The ratio of udp tests to all tests run each interval
     */
    public FloodManipulator(int threadCount, int interval, int udpToAllRatio) {
        executor = Executors.newScheduledThreadPool(threadCount);
        random = new Random();
        this.threadCount = threadCount;
        this.interval = interval;
        this.udpToAllRatio = udpToAllRatio;

    }

    private synchronized AbstractUser nextUser(boolean restrictToUDP) {
        if (restrictToUDP) {
            int idx = random.nextInt(udpUserCount - 1);
            return udpUserList.get(idx);
        } else {
            int idx = random.nextInt(sendableUserCount - 1);
            return sendableUserList.get(idx);
        }
    }

    @Override
    public void preTestRunPreServerStart(TestEngine engine) {
        Set<AbstractConnection> mainInputList =
                ImmutableConnections.valuesFiltered(new ConnectionFilter()
                        .addServerProfile(ImmutableServerProfiles.SERVER_0)
                        .addConnectionTypes(ProtocolProfiles.ConnectionType.INPUT));

        for (AbstractConnection connection : mainInputList) {
            ProtocolProfilesInterface protocolProfile = connection.getProtocol();

            List<AbstractUser> users = new ArrayList<>(
                    connection.getUsers(new UserFilter().setUserActivityIsValidated(false)));

            if (protocolProfile.canSend()) {
                AbstractUser u0 = users.get(0);
                AbstractUser u1 = users.get(1);
                sendableUserList.add(u0);
                sendableUserList.add(u1);
                engine.offlineAddUsersAndConnectionsIfNecessary(u0);
                engine.offlineAddUsersAndConnectionsIfNecessary(u1);
            }

            if (protocolProfile.canConnect()) {
                AbstractUser u0 = users.get(0);
                AbstractUser u1 = users.get(1);
                connectableUserList.add(u0);
                connectableUserList.add(u1);
                engine.offlineAddUsersAndConnectionsIfNecessary(u0);
                engine.offlineAddUsersAndConnectionsIfNecessary(u1);
            }

            if (protocolProfile == ProtocolProfiles.INPUT_UDP) {
                AbstractUser u0 = users.get(0);
                AbstractUser u1 = users.get(1);
                udpUserList.add(u0);
                udpUserList.add(u1);
                engine.offlineAddUsersAndConnectionsIfNecessary(u0);
                engine.offlineAddUsersAndConnectionsIfNecessary(u1);
            }
        }

        sendableUserCount = sendableUserList.size();
        udpUserCount = udpUserList.size();

//        udpInputList.stream().filter(p -> p.getProtocol().)

//        sendableUserList = mainInputList.
//        connectableUserList;
//        udpUserList;


//        for (GenConnectionss connection : mainInputList) {
//            if (connection.getProtocol().canSend()) {
//                if (connection.getProtocol().canConnect()) {
//
//
//                } else if (connection.getProtocol() == ProtocolProfiles.INPUT_UDP ||
//                        connection.getProtocol() == ProtocolProfiles.INPUT_MCAST) {
//
//                }
//            }
//
//            if (connection.getProtocol().canConnect()) {
//
//
//            } else if (connection.getProtocol().canListen()) {
//
//            } else if (connection.getProtocol().canSend()) {
//
//            }
//        }
//
//
//        for (int i = 0; i < mainInputList.size(); i++) {
//            GenConnectionss connections = mainInputList.get(i);
//            GenConnectionss input = mainInputList.get(i);
//            udpUserList.add(input.getTestUsers()[0]);
//            udpUserList.add(input.getTestUsers()[1]);
//            engine.offlineAddUsersAndConnectionsIfNecessary(input.getTestUsers()[0]);
//            engine.offlineAddUsersAndConnectionsIfNecessary(input.getTestUsers()[1]);
//            if (input.getProtocol().canConnect()) {
//                connectableUserList.add(input.getTestUsers()[0]);
//                connectableUserList.add(input.getTestUsers()[1]);
//            }
//            if (input.getProtocol().canSend()) {
//                sendableUserList.add(input.getTestUsers()[0]);
//                sendableUserList.add(input.getTestUsers()[1]);
//            }
//        }
//
//        for (GenConnectionss input : udpInputList) {
//            udpUserList.addAll(Lists.newArrayList(input.getTestUsers()));
//        }
//
//        for (GenValidatingUsers user : udpUserList) {
//            engine.offlineAddUsersAndConnectionsIfNecessary(user);
//        }
//
//        udpUserCount = udpUserList.size();
//        sendableUserCount = sendableUserList.size();
    }

    @Override
    public void preTestRunPostServerStart(TestEngine engine) {

        for (AbstractUser user : connectableUserList) {
            engine.connectClientAndVerify(true, user);
        }

        Set<Runnable> runnables = new HashSet<>();

        for (int i = 0; i < threadCount; i++) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    int nonUdpSelection = random.nextInt(udpToAllRatio);
                    boolean udpDone = false;
                    for (int i = 0; i <= udpToAllRatio; i++) {
                        try {
                            AbstractUser user;
                            if (i == nonUdpSelection) {
                                user = nextUser(false);
                            } else {
                                user = nextUser(true);
                            }
//                            TAKServerClient.sendUDPMessage(user, cotMessage, Integer.valueOf(user.getConnection().getPort()));
                            SendingClient.sendUDPMessage(TAKCLConfigModule.getInstance().getClientSendToAddress(), user.getConnection().getPort(), null, cotMessage);
                        } catch (BindException e) {
                            // This happens when you are rapidly binding INPUT_UDP ports in a randomish manner over a preefined port range....
                        }
                        sendCount++;
                    }
                }
            };
            runnables.add(r);
        }

        for (Runnable runnable : runnables) {
            runningTasks.add(executor.scheduleWithFixedDelay(runnable, (long) random.nextInt(interval), (long) interval, TimeUnit.MILLISECONDS));
        }
    }

    @Override
    public void postTestRunPreServerStop(TestEngine engine) {
        for (ScheduledFuture future : runningTasks) {
            future.cancel(true);
        }

        try {
            Thread.sleep(8 * interval);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        executor.shutdown();
    }

    @Override
    public void postTestRunPostServerStop(TestEngine testEngine) {
    }

    @Override
    public void printResults() {
        String printString = "SendCount: " + sendCount;
        System.out.println(printString);
    }
}
