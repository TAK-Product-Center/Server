package com.bbn.marti.tests;

import com.bbn.marti.takcl.connectivity.server.AbstractRunnableServer;
import com.bbn.marti.test.shared.AbstractTestClass;
import com.bbn.marti.test.shared.data.servers.ImmutableServerProfiles;
import com.bbn.marti.test.shared.engines.TestEngine;
import org.junit.BeforeClass;

/**
 * Created on 11/23/15.
 */
public class ManualTests extends AbstractTestClass {

	@BeforeClass
	public static void setup() {
		// TODO: change to unverified users!
		AbstractRunnableServer.setLogDirectory(TEST_ARTIFACT_DIRECTORY);
		engine = new TestEngine(ImmutableServerProfiles.SERVER_0);
	}

//    @Test
//    public void flood() {
//        GenValidatingUsers nonIgnoringUser = GenValidatingUsers.UNCHECKEDs0Bstreamtcp;
//        GenValidatingUsers sendingUser = GenValidatingUsers.UNCHECKEDs0Astdtcp;
//
//        engine.offlineAddUsersAndConnectionsIfNecessary(GenValidatingUsers.UNCHECKEDs0Bstreamtcp);
//        engine.offlineAddUsersAndConnectionsIfNecessary(GenValidatingUsers.UNCHECKEDs0Astdtcp);
//
//        final AtomicInteger nonIgnoredReceivedCount = new AtomicInteger(0);
//
//        final long startTime = (new Date()).getTime();
//
//        SendingClient sendingClient = new SendingClient(sendingUser, null);
//        ConnectibleClient nonIgnoringClient = new ConnectibleClient(nonIgnoringUser, new ClientResponseListener() {
//            @Override
//            public synchronized void onMessageReceived(String response) {
//                int value = nonIgnoredReceivedCount.addAndGet(1);
//                if (value % 10000 == 0) {
//                    long timeDelta = (new Date()).getTime() - startTime;
//                    System.out.println("NonIgnoring Count: " + value + ", Time delta: " + timeDelta);
//
//                }
//            }
//
//            @Override
//            public void onDisconnected() {
//
//            }
//        }, null);
//
//        engine.startServer(ServerProfiles.SERVER_0_MANIPULATOR);
//
//        nonIgnoringClient.connect(true, null);
//
//        int SEND_COUNT = 10000;
//
//
//        try {
//            Thread.sleep(4000);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
//
//        for (int i = 0; i < SEND_COUNT; i++) {
////            engine.attemptSendFromUserAndVerify(sendingUser);
//            sendingClient.sendMessage(CotGenerator.createLatestSAMessage(sendingUser).asXML());
//
////                if (i % 1000 == 0) {
////                    try {
////                        System.out.println("Send count: " + i);
////                        Thread.sleep(2000);
////                    } catch (InterruptedException e) {
////                        throw new RuntimeException(e);
////                    }
////                }
//        }
//
//        long sendEndTime = (new Date()).getTime();
//
//        long totalTime = sendEndTime - startTime;
//
//        try {
//            Thread.sleep(100000);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
//
//        System.out.println("SendCount: " + SEND_COUNT);
//        System.out.println("Listening User Received: " + nonIgnoredReceivedCount);
//    }

//    @Test
//    public void connectFloodAndIgnore() {
//        GenValidatingUsers ignoringUser = GenValidatingUsers.UNCHECKEDs0Hstreamtcp;
//        GenValidatingUsers nonIgnoringUser = GenValidatingUsers.UNCHECKEDs0Gstreamtcp;
//        GenValidatingUsers sendingUser = GenValidatingUsers.UNCHECKEDs0Hstdtcp;
//
//        engine.offlineAddUsersAndConnectionsIfNecessary(ignoringUser);
//        engine.offlineAddUsersAndConnectionsIfNecessary(nonIgnoringUser);
//        engine.offlineAddUsersAndConnectionsIfNecessary(sendingUser);
//
//        final AtomicInteger nonIgnoredReceivedCount = new AtomicInteger(0);
//
//        final long startTime = (new Date()).getTime();
//
//        SendingClient sendingClient = new SendingClient(sendingUser, null);
//        ConnectibleClient nonIgnoringClient = new ConnectibleClient(nonIgnoringUser, new ClientResponseListener() {
//            @Override
//            public synchronized void onMessageReceived(String response) {
//                int value = nonIgnoredReceivedCount.addAndGet(1);
//                if (value % 10000 == 0) {
//                    long timeDelta = (new Date()).getTime() - startTime;
//                    System.out.println("NonIgnoring Count: " + value + ", Time delta: " + timeDelta);
//
//                }
//            }
//
//            @Override
//            public void onDisconnected() {
//
//            }
//        }, null);
//
//        // TODO: change to unverified users!
//        engine.startServer(ServerProfiles.SERVER_0, ServerInstance.ServerOutputTarget.FILE);
//
//        engine.connectClientAndVerify(true, nonIgnoringUser);
//
//        final AtomicInteger ignoredReceivedCount = new AtomicInteger();
//        ignoredReceivedCount.set(0);
//
//        ConnectibleClient ignoringClient = new ConnectibleClient(ignoringUser, new ClientResponseListener() {
//            @Override
//            public synchronized void onMessageReceived(String response) {
//                int value = ignoredReceivedCount.addAndGet(1);
//                if (value % 10000 == 0) {
//                    long timeDelta = (new Date()).getTime() - startTime;
//                    System.out.println("NonIgnoring Count: " + value + ", Time delta: " + timeDelta);
//
//                }
//            }
//
//            @Override
//            public void onDisconnected() {
//
//            }
//        }, null);
//
//        int SEND_COUNT = Integer.MAX_VALUE;
//        int WAIT_TIME = Integer.MAX_VALUE;
//
//        ignoringClient.connectAndWaitTillListen(true, null, WAIT_TIME);
//
//        nonIgnoringClient.connect(true, null);
//
//
//
//        try {
//            Thread.sleep(4000);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
//
//        for (int i = 0; i < SEND_COUNT; i++) {
////            engine.attemptSendFromUserAndVerify(sendingUser);
//            sendingClient.sendMessage(CotGenerator.createLatestSAMessage(sendingUser).asXML());
//
//            if (i % 1000 == 0) {
//                try {
//                    System.out.println("Send count: " + i);
//                    Thread.sleep(2000);
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//        }
//
//        long sendEndTime = (new Date()).getTime();
//
//        long totalTime = sendEndTime - startTime;
//
//        try {
//            Thread.sleep(120000);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
//
//        System.out.println("SendCount: " + SEND_COUNT);
//        System.out.println("Listening User Received: " + nonIgnoredReceivedCount);
//        System.out.println("Ignoring User Received: " + ignoredReceivedCount);
//    }
}
