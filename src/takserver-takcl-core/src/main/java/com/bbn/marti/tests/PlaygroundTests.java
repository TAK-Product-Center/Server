//package com.bbn.marti.tests;
//
//import com.bbn.marti.test.shared.data.generated.GenConnections;
//import com.bbn.marti.test.shared.*;
//import com.bbn.marti.test.shared.data.users.GenValidatingUsers;
//import com.bbn.marti.test.shared.data.ServerProfiles;
//import com.bbn.marti.test.shared.data.users.UserInterface;
//import com.bbn.marti.test.shared.engines.ServerInstance;
//import com.bbn.marti.test.shared.engines.TestEngine;
//import org.jetbrains.annotations.Nullable;
//import org.junit.BeforeClass;
//import org.junit.Test;
//
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.LinkedList;
//import java.util.List;
//
///**
// * Created on 10/1/15.
// */
////@FixMethodOrder(MethodSorters.NAME_ASCENDING)
//public class PlaygroundTests extends AbstractTestClass {
//
////    private static final TestEngine engine = new TestEngine(ServerProfiles.SERVER_0);
//
////    static {
////        engine.enableServerControlOverride(true);
////    }
//
//    @BeforeClass
//    public static void init() {
//    }
//
//
//    @Test
//    public void dualServerTest() {
//    }
//
//    @Test
//    public void tempTest() {
//        engine.engineFactoryReset();
//
//        engine.offlineAddUsersAndConnectionsIfNecessary(GenValidatingUsers.s0u0streamtcp);
//        engine.offlineAddUsersAndConnectionsIfNecessary(GenValidatingUsers.s0u0streamtcpA);
//        engine.offlineAddUsersAndConnectionsIfNecessary(GenValidatingUsers.s0u0streamtcp_01);
//        engine.offlineAddUsersAndConnectionsIfNecessary(GenValidatingUsers.s0u0streamtcp_01t);
//
//        engine.startServer(ServerProfiles.SERVER_0, ServerInstance.ServerOutputTarget.FILE);
//
//        engine.stopServers(ServerProfiles.SERVER_0);
//
//    }
//
//
//
//
//    public void SubscriptionTest(TestManipulatorInterface manipulator) {
//        try {
//            engine.engineFactoryReset();
//
//            // add all the inputs with their initial users
//
//            List<GenConnections> inputList = new ArrayList<>(GenConnections.valuesFiltered(ServerProfiles.SERVER_0, null));
//            List<UserInterface> userList = new ArrayList<>(2 * inputList.size());
//            List<UserInterface> sendableUserList = new LinkedList<>();
//            List<UserInterface> connectableUserList = new LinkedList<>();
//
//            randomize(inputList);
//
//            for (int i = 0; i < inputList.size(); i++) {
//                GenConnections input = inputList.get(i);
//                UserInterface u0 = input.getUserWithIdentifier(true, 0);
//                UserInterface u1 = input.getUserWithIdentifier(true, 1);
//                userList.add(u0);
//                userList.add(u1);
//                engine.offlineAddUsersAndConnectionsIfNecessary(u0);
//                engine.offlineAddUsersAndConnectionsIfNecessary(u1);
//                if (input.getProtocol().canConnect()) {
//                    connectableUserList.add(u0);
//                    connectableUserList.add(u1);
//                }
//                if (input.getProtocol().canSend()) {
//                    sendableUserList.add(u0);
//                    sendableUserList.add(u1);
//                }
//            }
//
//            if (manipulator != null) {
//                manipulator.preTestRunPreServerStart(engine);
//            }
//
//            engine.startServer(ServerProfiles.SERVER_0, ServerInstance.ServerOutputTarget.FILE);
//
//            if (manipulator != null) {
//                manipulator.preTestRunPostServerStart(engine);
//            }
//
//            randomize(connectableUserList);
//
//            engine.connectClientAndVerify(true, connectableUserList.toArray(new UserInterface[0]));
//
//            randomize(sendableUserList);
//
//            for (UserInterface user : sendableUserList) {
//                engine.attemptSendFromUserAndVerify(user);
//            }
//
////            randomize(inputList);
////
////            for (GenConnections input : inputList) {
////                engine.onlineRemoveInputAndVerify(input);
////
////                List<UserInterface> loopList = engine.getUsersThatCanCurrentlySend();
////                randomize(loopList);
////
////                for (UserInterface user : loopList) {
////                    engine.attemptSendFromUserAndVerify(user);
////                }
////            }
////
////            randomize(inputList);
////
////            for (GenConnections input : inputList) {
////                engine.onlineAddInput(input);
////
////                if (connectableUserList.contains(u0)) {
////                    engine.connectClientAndVerify(true, u0);
////                }
////
////                if (connectableUserList.contains(u1)) {
////                    engine.connectClientAndVerify(true, u1);
////                }
////
////                List<UserInterface> looplist = engine.getUsersThatCanCurrentlySend();
////                randomize(looplist);
////
////                for (UserInterface user : looplist) {
////                    engine.attemptSendFromUserAndVerify(user);
////                }
////            }
//
//        } finally {
//            if (manipulator != null) {
//                manipulator.postTestRunPreServerStop(engine);
//            }
//            engine.stopServers(ServerProfiles.SERVER_0);
//            if (manipulator != null) {
//                manipulator.postTestRunPostServerStop(engine);
//            }
//        }
//    }
//
//
//
//
//    @Test
//    public void sanityTest() {
//        TestEngine dumbEngine = new TestEngine(ServerProfiles.SERVER_0);
//        FloodManipulator manipulator = new FloodManipulator(10, 400, 4);
//        manipulator.preTestRunPreServerStart(dumbEngine);
//        dumbEngine.startServer(ServerProfiles.SERVER_0, ServerInstance.ServerOutputTarget.FILE);
//        manipulator.preTestRunPostServerStart(dumbEngine);
//
//        try {
//            Thread.sleep(60000);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
//
//        manipulator.postTestRunPreServerStop(dumbEngine);
//        dumbEngine.stopServers(ServerProfiles.SERVER_0);
//        manipulator.postTestRunPostServerStop(dumbEngine);
//
//        manipulator.printResults();
//    }
//
//
//    public void runningTest(@Nullable TestManipulatorInterface manipulator, int roughTimer) {
//        try {
//            engine.engineFactoryReset();
//
//            // add all the inputs with their initial users
//
//            List<GenConnections> inputList = new ArrayList<>(GenConnections.valuesFiltered(ServerProfiles.SERVER_0, null));
//            List<UserInterface> userList = new ArrayList<>(2 * inputList.size());
//            List<UserInterface> sendableUserList = new LinkedList<>();
//            List<UserInterface> connectableUserList = new LinkedList<>();
//
//            randomize(inputList);
//
//            for (int i = 0; i < inputList.size(); i++) {
//                GenConnections input = inputList.get(i);
//                UserInterface u0 = input.getUserWithIdentifier(true, 0);
//                UserInterface u1 = input.getUserWithIdentifier(true, 1);
//                userList.add(u0);
//                userList.add(u1);
//                engine.offlineAddUsersAndConnectionsIfNecessary(u0);
//                engine.offlineAddUsersAndConnectionsIfNecessary(u1);
//                if (input.getProtocol().canConnect()) {
//                    connectableUserList.add(u0);
//                    connectableUserList.add(u1);
//                }
//                if (input.getProtocol().canSend()) {
//                    sendableUserList.add(u0);
//                    sendableUserList.add(u1);
//                }
//            }
//
//            if (manipulator != null) {
//                manipulator.preTestRunPreServerStart(engine);
//            }
//
//            engine.startServer(ServerProfiles.SERVER_0, ServerInstance.ServerOutputTarget.FILE);
//
//            if (manipulator != null) {
//                manipulator.preTestRunPostServerStart(engine);
//            }
//
//            try {
//                Thread.sleep(roughTimer);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//
//            randomize(connectableUserList);
//
//            engine.connectClientAndVerify(true, connectableUserList.toArray(new UserInterface[0]));
//
//            randomize(sendableUserList);
//
//            long endTime = (new Date()).getTime() + roughTimer;
//
//            while ((new Date()).getTime() < endTime) {
//                for (UserInterface user : sendableUserList) {
//                    engine.attemptSendFromUserAndVerify(user);
//                }
//                randomize(sendableUserList);
//            }
//
//        } finally {
//            if (manipulator != null) {
//                manipulator.postTestRunPreServerStop(engine);
//            }
//            engine.stopServers(ServerProfiles.SERVER_0);
//            if (manipulator != null) {
//                manipulator.postTestRunPostServerStop(engine);
//            }
//        }
//    }
//
////    @Test
////    public void test02_UDPGroupsTest() {
////        UnifiedExpectantClient c1 = new UnifiedExpectantClient(true, true);
////
////    }
//
////    @Test
////    public void fileGroups() {
////        engine.serverStop();
//////        try {
//////            Thread.sleep(4000);
//////        } catch (InterruptedException e) {
//////            throw new RuntimeException(e);
//////        }
////
////        engine.serverResetConfigFile();
////        engine.getOfflineConfig().addInput(TemplateConnections.astcp);
////        engine.getOfflineConfig().fileAuth(true);
////        engine.getOfflineUserManagement().addUpdateUser("homer", "homerPass");
////        engine.getOfflineUserManagement().addUpdateUser("marge", "margePass");
////        engine.getOfflineUserManagement().addUpdateUser("bart", "bartPass");
////        engine.getOfflineUserManagement().addUserToGroup("homer", "group0");
////        engine.getOfflineUserManagement().addUserToGroup("homer", "group1");
////        engine.getOfflineUserManagement().addUserToGroup("marge", "group1");
////        engine.getOfflineUserManagement().addUserToGroup("marge", "group2");
////        engine.getOfflineUserManagement().addUserToGroup("bart", "group0");
////        engine.getOfflineUserManagement().addUserToGroup("bart", "group1");
////
////        engine.serverStart(false);
//////        try {
//////            Thread.sleep(4000);
//////        } catch (InterruptedException e) {
//////            throw new RuntimeException(e);
//////        }
////
////        UnifiedExpectantClient c1 = new UnifiedExpectantClient(TemplateUsers.Homer_012, TemplateConnections.astcp, 1, false);
////        c1.connect(true, null);
////
////        UnifiedExpectantClient c2 = new UnifiedExpectantClient(TemplateUsers.Marge_12, TemplateConnections.astcp, 0, false);
////        c2.connect(true, "sueSend.xml");
////        try {
////            Thread.sleep(1000);
////        } catch (InterruptedException e) {
////            throw new RuntimeException(e);
////        }
////
////        c1.checkAndClearExpectations();
////        c2.checkAndClearExpectations();
////
////        engine.serverStop();
////    }
//
////    @Test
////    public void testTest() {
//////        engine.templateLatestSA(TemplateConnections.astcp);
////    }
//
////    @Test
////    public void latestSA() {
////        engine.templateLatestSA(OnlineInputModule.TemplateConnections.tcp);
////    }
//
//
////    @Test
////    public void test03_LatestSAFileAuth() {
////        TestEngine engine = new TestEngine(actionEngine);
////        engine.offlineFactoryResetServers();
////        engine.offlineEnableLatestSA(true);
////
////        engine.offlineAddUsersAndConnectionsIfNecessary(TemplateUsers.Homer_012, TemplateConnections.astcp);
////        engine.offlineAddUsersAndConnectionsIfNecessary(TemplateUsers.Marge_12, TemplateConnections.astcp);
////
////        offlineConfigModule.addInput(OnlineInputModule.TemplateConnections.astcp);
////
////        server.startServer(true);
////
////        remoteUserAuthFileModule.init();
////        remoteUserAuthFileModule.addUser("Homer_012", "doh");
////        remoteUserAuthFileModule.addUser("DocBrown", "weDontNeedRoads");
////        remoteUserAuthFileModule.addUserToGroup("Homer_012", "NoHomers");
////        remoteUserAuthFileModule.addUserToGroup("DocBrown", "NoHomers");
////        remoteUserAuthFileModule.fileSaveChanges();
////        sleep(1000);
////
////
////        // TODO: Enable file auth and enable latest SA
////        UnifiedExpectantClient c1 = new UnifiedExpectantClient(false, false);
////        c1.setUserCredentials("Homer_012", "doh");
////        c1.connect(OnlineInputModule.TemplateConnections.astcp, true, "sample-atak-announce.txt");
////        c1.checkAndClearExpectations();
////
////        UnifiedExpectantClient c2 = new UnifiedExpectantClient(true, false);
////        c2.setUserCredentials("DocBrown", "weDontNeedRoads");
////        c2.connect(OnlineInputModule.TemplateConnections.astcp, true, null);
////        c2.checkAndClearExpectations();
////    }
//
//
////
////    @Test
////    public void engineTest() {
////        TestEngine testEngine = new TestEngine(engine);
////
////        testEngine.offlineAddUsersAndConnectionsIfNecessary(TemplateUsers.Homer_012, TemplateConnections.astcp);
////        testEngine.offlineAddUsersAndConnectionsIfNecessary(TemplateUsers.Lisa_0, TemplateConnections.astcp);
////        testEngine.offlineAddUsersAndConnectionsIfNecessary(TemplateUsers.Marge_12, TemplateConnections.astcp);
////        testEngine.offlineAddUsersAndConnectionsIfNecessary(TemplateUsers.ANONA, TemplateConnections.stcp01);
////        testEngine.offlineAddUsersAndConnectionsIfNecessary(TemplateUsers.ANONB, TemplateConnections.stcp);
////
////        // Message, connect client to INPUT_TCP, send INPUT_UDP, not getting
////        // CLose and reopen, it works.
////
////        testEngine.offlineSetup();
////
////        testEngine.sendFromUserAndVerify(TemplateUsers.Lisa_0);
////
////        testEngine.sendFromUserAndVerify(TemplateUsers.Lisa_0);
////
////        testEngine.sendFromUserAndVerify(TemplateUsers.ANONB);
////
////        testEngine.offlineAddUsersAndConnectionsIfNecessary(TemplateUsers.ANONC, TemplateConnections.stcp);
////
////        testEngine.onlineRemoveInputAndVerify(TemplateConnections.stcp);
////
////        testEngine.onlineAddInput(TemplateConnections.sslfileauth);
////
//////        testEngine.onlineAddUserWithInput(TemplateUsers.MrBurns_0, TemplateConnections.sslfileauth);
//////        testEngine.sendFromUserAndVerify(TemplateUsers.Lisa_0);
////    }
//
////    @Test
////    public void testMcast() {
////        TestEngine testEngine = new TestEngine(engine);
////        testEngine.
//
//
//    //    }
////    @Test
////    public void testUnauthedAuthableAnonReachability() {
////        try {
////            engine.offlineFactoryResetServers();
////
////            engine.offlineAddUsersAndConnectionsIfNecessary(TemplateUsers.Homer_012, TemplateConnections.astcp);
////            engine.offlineAddUsersAndConnectionsIfNecessary(TemplateUsers.Marge_12, TemplateConnections.astcp);
////            engine.offlineAddUsersAndConnectionsIfNecessary(TemplateUsers.Moe_012, TemplateConnections.astcp);
////            engine.offlineAddUsersAndConnectionsIfNecessary(TemplateUsers.ANONA, TemplateConnections.stcp01);
////            engine.offlineAddUsersAndConnectionsIfNecessary(TemplateUsers.ANONB, TemplateConnections.stcp);
////            engine.offlineAddUsersAndConnectionsIfNecessary(TemplateUsers.Abe, TemplateConnections.astcp);
////
////            //
////            engine.offlineEnableLatestSA(true);
////            //
////
////            engine.startServer(ServerProfiles.SERVER_0);
////
//////            testEngine.monitorUser(TemplateUsers.ANONB);
////
////            engine.connectClientAndVerify(true, TemplateUsers.Homer_012, TemplateUsers.Marge_12, TemplateUsers.ANONA, TemplateUsers.ANONB);
////
////            engine.connectClientAndVerify(false, TemplateUsers.Moe_012, TemplateUsers.Abe);
////
//////            testEngine.attemptSendFromUserAndVerify(TemplateUsers.Abe);
////
////            engine.attemptSendFromUserAndVerify(TemplateUsers.ANONB);
////            engine.attemptSendFromUserAndVerify(TemplateUsers.ANONA);
////            engine.attemptSendFromUserAndVerify(TemplateUsers.ANONB);
////
////            engine.attemptSendFromUserAndVerify(TemplateUsers.Moe_012);
////
////            engine.authenticateAndVerifyClient(TemplateUsers.Abe);
////
////            engine.attemptSendFromUserAndVerify(TemplateUsers.ANONA);
////            engine.attemptSendFromUserAndVerify(TemplateUsers.ANONB);
////
//////            testEngine.connectClientAndVerify(true, TemplateUsers.Homer_012, TemplateUsers.Lisa_0, TemplateUsers.Marge_12, TemplateUsers.ANONA, TemplateUsers.ANONB);
//////            testEngine.connectClientAndVerify(false, TemplateUsers.Moe_012);
////
////
//////            testEngine.attemptSendFromUserAndVerify(TemplateUsers.Lisa_0);
//////
//////            testEngine.attemptSendFromUserAndVerify(TemplateUsers.Moe_012);
//////
//////            testEngine.disconnectClientAndVerify(TemplateUsers.Moe_012);
////
//////        testEngine.authenticateAndVerifyClient(TemplateUsers.Moe_012);
////
//////            testEngine.connectClientAndVerify(false, TemplateUsers.Moe_012);
//////
//////            testEngine.authenticateAndVerifyClient(TemplateUsers.Moe_012);
////
//////            try {
//////                Thread.sleep(1000);
//////            } catch (InterruptedException e) {
//////                throw new RuntimeException(e);
//////            }
//////
//////            testEngine.attemptSendFromUserAndVerify(TemplateUsers.Moe_012);
//////
//////            testEngine.attemptSendFromUserAndVerify(TemplateUsers.Lisa_0);
//////
//////            testEngine.connectClientAndVerify(true, TemplateUsers.MrBurns_0);
////        } finally {
////            engine.stopServers(ServerProfiles.SERVER_0);
////        }
////    }
//
//}
