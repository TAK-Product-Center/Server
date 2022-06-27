//package com.bbn.marti.tests;
//
//import com.bbn.marti.config.Connection;
//import com.bbn.marti.test.shared.AbstractTestClass;
//import com.bbn.marti.test.shared.TestManipulatorInterface;
//import com.bbn.marti.test.shared.data.GroupProfiles;
//import com.bbn.marti.test.shared.data.connections.ConnectionInterface;
//import com.bbn.marti.test.shared.data.connections.MutableConnection;
//import com.bbn.marti.test.shared.data.generated.ImmutableConnections;
//import com.bbn.marti.test.shared.data.generated.ImmutableUsers;
//import com.bbn.marti.test.shared.data.servers.ImmutableServerProfiles;
//import com.bbn.marti.test.shared.data.servers.MutableServerProfile;
//import com.bbn.marti.test.shared.data.users.BaseUsers;
//import com.bbn.marti.test.shared.data.users.MutableUser;
//import com.bbn.marti.test.shared.data.users.UserFilter;
//import com.bbn.marti.test.shared.data.users.UserInterface;
//import com.bbn.marti.takcl.connectivity.RunnableServer;
//import org.jetbrains.annotations.Nullable;
//import org.junit.Test;
//
//import java.util.HashSet;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Set;
//
///**
// * Created on 10/26/15.
// */
//public class InputTests2 extends AbstractTestClass {
//
//    private static final String className = "InputTests";
//
//    @Test
//    public void onlineInputTest() {
//        try {
//
//            String sessionIdentifier = className + ".onlineInputTest";
//            engine.engineFactoryReset();
//
//            MutableServerProfile s0 = ImmutableServerProfiles.SERVER_0.getMutableInstance();
//
//            MutableConnection auth = s0.generateConnection(ImmutableConnections.s0_authssl);
//            MutableUser auth01A = auth.generateConnectionUser(BaseUsers.authuser01, true, "A");
//            MutableUser authB = auth.generateConnectionUser(BaseUsers.authuser, true, "B");
//            MutableUser auth012C = auth.generateConnectionUser(BaseUsers.authuser012, true, "C");
//            MutableUser auth2D = auth.generateConnectionUser(BaseUsers.authuser2, true, "D");
//
//            MutableConnection udp1 = s0.generateConnection(ImmutableConnections.s0_udp01);
//            MutableUser udp1A = udp1.generateConnectionUser(BaseUsers.anonuser, true, "A");
//            MutableUser udp1B = udp1.generateConnectionUser(BaseUsers.anonuser, true, "B");
//            MutableUser udp1C = udp1.generateConnectionUser(BaseUsers.anonuser, true, "C");
//
//            MutableConnection stcp12 = s0.generateConnection(ImmutableConnections.s0_stcp12);
//            MutableUser stcp12A = stcp12.generateConnectionUser(BaseUsers.anonuser, true, "A");
//            MutableUser stcp12B = stcp12.generateConnectionUser(BaseUsers.anonuser, true, "B");
//            MutableUser stcp12C = stcp12.generateConnectionUser(BaseUsers.anonuser, true, "C");
//
//            engine.offlineEnableLatestSA(true, ImmutableServerProfiles.SERVER_0);
//
//            engine.offlineAddUsersAndConnectionsIfNecessary(auth01A);
//            engine.offlineAddUsersAndConnectionsIfNecessary(authB);
//            engine.offlineAddUsersAndConnectionsIfNecessary(auth012C);
//            engine.offlineAddUsersAndConnectionsIfNecessary(auth2D);
//
//            engine.offlineAddUsersAndConnectionsIfNecessary(udp1A);
//            engine.offlineAddUsersAndConnectionsIfNecessary(udp1B);
//            engine.offlineAddUsersAndConnectionsIfNecessary(udp1C);
//
//            engine.offlineAddUsersAndConnectionsIfNecessary(stcp12A);
//            engine.offlineAddUsersAndConnectionsIfNecessary(stcp12B);
//            engine.offlineAddUsersAndConnectionsIfNecessary(stcp12C);
//
//            engine.startServer(ImmutableServerProfiles.SERVER_0, RunnableServer.ServerOutputTarget.FILE, sessionIdentifier);
//
//            engine.connectClientAndSendMessage(true, auth01A);
//            engine.connectClientAndVerify(true, auth012C);
//            engine.connectClientAndSendMessage(true, authB);
//            engine.connectClientAndSendMessage(true, auth2D);
//
//            engine.attemptSendFromUserAndVerify(udp1A);
//
//            engine.onlineAddInputToGroup(udp1, GroupProfiles.group2);
//
//            engine.connectClientAndSendMessage(true, stcp12A);
//
//            engine.attemptSendFromUserAndVerify(udp1B);
//            engine.attemptSendFromUserAndVerify(udp1A);
//
//            engine.onlineRemoveInputFromGroup(udp1, GroupProfiles.group1);
//            engine.onlineRemoveInputFromGroup(udp1, GroupProfiles.group2);
//
//            engine.attemptSendFromUserAndVerify(udp1A);
//            engine.attemptSendFromUserAndVerify(udp1B);
//            engine.attemptSendFromUserAndVerify(udp1C);
//
//
//
//            MutableConnection xtcp12 = s0.generateConnection(ImmutableConnections.s0_tcp12);
//            MutableUser xtcp12D = xtcp12.generateConnectionUser(BaseUsers.anonuser, true, "D");
//            MutableUser xtcp12E = xtcp12.generateConnectionUser(BaseUsers.anonuser, true, "E");
//            MutableUser xtcp12F = xtcp12.generateConnectionUser(BaseUsers.anonuser, true, "F");
//
//            MutableConnection xstcp01 = s0.generateConnection(ImmutableConnections.s0_stcp01);
//            MutableUser xstcp01D = xstcp01.generateConnectionUser(BaseUsers.anonuser, true, "D");
//            MutableUser xstcp01E = xstcp01.generateConnectionUser(BaseUsers.anonuser, true, "E");
//            MutableUser xstcp01F = xstcp01.generateConnectionUser(BaseUsers.anonuser, true, "F");
//
//            engine.onlineAddInput(xtcp12);
//            engine.onlineAddInput(xstcp01);
//            engine.onlineRemoveInputFromGroup(xstcp01, GroupProfiles.group0);
//
//            engine.onlineAddUser(ImmutableServerProfiles.SERVER_0, xtcp12D);
//            engine.onlineAddUser(ImmutableServerProfiles.SERVER_0, xtcp12E);
//            engine.onlineAddUser(ImmutableServerProfiles.SERVER_0, xtcp12F);
//            engine.onlineAddUser(ImmutableServerProfiles.SERVER_0, xstcp01D);
//            engine.onlineAddUser(ImmutableServerProfiles.SERVER_0, xstcp01E);
//            engine.onlineAddUser(ImmutableServerProfiles.SERVER_0, xstcp01F);
//
//            engine.connectClientAndSendMessage(true, xstcp01D);
//
//            engine.attemptSendFromUserAndVerify(xtcp12D);
//
//            engine.connectClientAndVerify(true, xstcp01E);
//
//            engine.attemptSendFromUserAndVerify(udp1A);
//            engine.attemptSendFromUserAndVerify(stcp12A);
//
//            engine.onlineRemoveInputFromGroup(xtcp12, GroupProfiles.group1);
//
//            engine.attemptSendFromUserAndVerify(auth012C);
//
//            engine.onlineAddInputToGroup(xstcp01, GroupProfiles.group2);
//
//            engine.attemptSendFromUserAndVerify(stcp12A);
//
//        } finally {
//            engine.stopServers(ImmutableServerProfiles.SERVER_0);
//        }
//    }
//
//    @Test
//    public void inputRemoveAddTest() {
//        inputRemoveAddTest(null);
//    }
//
//
//    static class UserRunner extends ListRunner<UserInterface> {
//
//        public UserRunner(@Nullable List<UserInterface> initialValues, boolean randomize) {
//            super(initialValues, randomize);
//        }
//
//        @Override
//        public synchronized void doAction(UserInterface object) {
//        }
//    }
//
//    UserRunner userRunner = new UserRunner(null, true);
//    List<UserInterface> laterUsers = new LinkedList<>();
//
//    ListRunner connectionRunner = new ListRunner<ConnectionInterface>(null, true) {
//
//        @Override
//        public void doAction(ConnectionInterface object) {
//            UserInterface[] inputUsers = object.getUsers(new UserFilter().setUserActivityIsValidated(true)).toArray(new UserInterface[0]);
//            UserInterface offlineUser0 = inputUsers[0];
//            UserInterface offlineUser1 = inputUsers[1];
//            UserInterface onlineUser2 = inputUsers[3];
//            userRunner.add(offlineUser0);
//            userRunner.add(offlineUser1);
//            laterUsers.add(onlineUser2);
//            engine.offlineAddUsersAndConnectionsIfNecessary(offlineUser0);
//            engine.offlineAddUsersAndConnectionsIfNecessary(offlineUser1);
//        }
//    };
//
//
//    public void inputRemoveAddTest(TestManipulatorInterface manipulator) {
//        try {
//            String sessionIdentifier = className + ".inputRemoveAddTest";
//            engine.engineFactoryReset();
//
//            // Connections to be added before the server starts up
//            List<ConnectionInterface> offlineInputList = new LinkedList<>();
//            offlineInputList.add(ImmutableConnections.s0_stcp3);
//            offlineInputList.add(ImmutableConnections.s0_stcp12);
//            offlineInputList.add(ImmutableConnections.s0_stcp01t);
//            offlineInputList.add(ImmutableConnections.s0_stcp0);
//            offlineInputList.add(ImmutableConnections.s0_stcp01);
//            offlineInputList.add(ImmutableConnections.s0_stcp2f);
//            offlineInputList.add(ImmutableConnections.s0_stcp);
//            offlineInputList.add(ImmutableConnections.s0_mcast3f);
//            offlineInputList.add(ImmutableConnections.s0_mcast01);
//            offlineInputList.add(ImmutableConnections.s0_tcp12);
//            offlineInputList.add(ImmutableConnections.s0_tcp01t);
//            offlineInputList.add(ImmutableConnections.s0_udp12t);
//            offlineInputList.add(ImmutableConnections.s0_authssl);
//            offlineInputList.add(ImmutableConnections.s0_ssl);
//            offlineInputList.add(ImmutableConnections.s0_saproxy);
//            offlineInputList.add(ImmutableConnections.s0_authstcp);
//            randomize(offlineInputList);
//
//            // Connections to be added after the server starts up
//            List<ConnectionInterface> onlineInputList = new LinkedList<>();
//            onlineInputList.add(ImmutableConnections.s0_stcpA);
//            onlineInputList.add(ImmutableConnections.s0_stcp12);
//            onlineInputList.add(ImmutableConnections.s0_stcp3);
//            onlineInputList.add(ImmutableConnections.s0_stcp01);
//            onlineInputList.add(ImmutableConnections.s0_stcp2f);
//            onlineInputList.add(ImmutableConnections.s0_stcp0);
//            onlineInputList.add(ImmutableConnections.s0_mcast);
//            onlineInputList.add(ImmutableConnections.s0_mcast12t);
//            onlineInputList.add(ImmutableConnections.s0_tcp);
//            onlineInputList.add(ImmutableConnections.s0_tcp2f);
//            onlineInputList.add(ImmutableConnections.s0_udp);
//            onlineInputList.add(ImmutableConnections.s0_udp3f);
//            onlineInputList.add(ImmutableConnections.s0_authsslA);
//            onlineInputList.add(ImmutableConnections.s0_authstcpA);
//            onlineInputList.add(ImmutableConnections.s0_saproxyA);
//            onlineInputList.add(ImmutableConnections.s0_tls);
//            randomize(onlineInputList);
//
//
//
//
//
//
//
//
//            // Users that can connect or send based on their connections being on the server
//            Set<UserInterface> eligibleUsers = new HashSet<>();
//
//            // Connections that are eligible to be added to the server
//            Set<ConnectionInterface> eligibleConnections = new HashSet<>();
//
//
//            // Lists to track the different user types and their abilities/needs
//            List<UserInterface> offlineUserList = new LinkedList<>();
//            List<UserInterface> offlineSendableUserList = new LinkedList<>();
//            List<UserInterface> offlineConnectableUserList = new LinkedList<>();
//            List<UserInterface> onlineUserList = new LinkedList<>();
//            List<UserInterface> onlineSendableUserList = new LinkedList<>();
//            List<UserInterface> onlineConnectableUserList = new LinkedList<>();
//
//            List<ConnectionInterface> nonexistantInputs = new LinkedList<>();
//
//
//            // For all offline inputs, add a couple users to the offline users and a user to the online users, populate the lists, and add the connections
//            for (ConnectionInterface input : offlineInputList) {
//                UserInterface[] inputUsers = input.getUsers(new UserFilter().setUserActivityIsValidated(true)).toArray(new UserInterface[0]);
//                UserInterface offlineUser0 = inputUsers[0];
//                UserInterface offlineUser1 = inputUsers[1];
//                UserInterface onlineUser2 = inputUsers[3];
//                offlineUserList.add(offlineUser0);
//                offlineUserList.add(offlineUser1);
//                onlineUserList.add(onlineUser2);
//                engine.offlineAddUsersAndConnectionsIfNecessary(offlineUser0);
//                engine.offlineAddUsersAndConnectionsIfNecessary(offlineUser1);
//                if (input.getProtocol().canConnect()) {
//                    offlineConnectableUserList.add(offlineUser0);
//                    offlineConnectableUserList.add(offlineUser1);
//                    onlineConnectableUserList.add(onlineUser2);
//                }
//                if (input.getProtocol().canSend()) {
//                    offlineSendableUserList.add(offlineUser0);
//                    offlineSendableUserList.add(offlineUser1);
//                    onlineConnectableUserList.add(onlineUser2);
//                }
//            }
//            randomize(offlineSendableUserList);
//            randomize(offlineConnectableUserList);
//
//
//            // Do the same thing for the online conntections that we did for the offline connections, but don't add the inputs yet
//            for (ConnectionInterface input : onlineInputList) {
//                UserInterface[] inputUsers = input.getUsers(new UserFilter().setUserActivityIsValidated(true)).toArray(new UserInterface[0]);
//                UserInterface onlineUser0 = inputUsers[0];
//                UserInterface onlineUser1 = inputUsers[1];
//                onlineUserList.add(onlineUser0);
//                onlineUserList.add(onlineUser1);
//                nonexistantInputs.add(input);
//                if (input.getProtocol().canConnect()) {
//                    onlineConnectableUserList.add(onlineUser0);
//                    onlineConnectableUserList.add(onlineUser1);
//                }
//                if (input.getProtocol().canSend()) {
//                    onlineSendableUserList.add(onlineUser0);
//                    onlineSendableUserList.add(onlineUser1);
//                }
//            }
//            randomize(onlineConnectableUserList);
//            randomize(onlineSendableUserList);
//
//
//            if (manipulator != null) {
//                manipulator.preTestRunPreServerStart(engine);
//            }
//
//            engine.startServer(ImmutableServerProfiles.SERVER_0, RunnableServer.ServerOutputTarget.FILE, sessionIdentifier);
//
//            if (manipulator != null) {
//                manipulator.preTestRunPostServerStart(engine);
//            }
//
//            // Connect all the clients and send from them
//            engine.connectClientAndVerify(true, offlineConnectableUserList.toArray(new UserInterface[0]));
//
//            for (UserInterface user : offlineSendableUserList) {
//                engine.attemptSendFromUserAndVerify(user);
//            }
//
//            randomize(offlineInputList);
//
//            // Remove the inputs and send to test behavior
//            for (ConnectionInterface input : offlineInputList) {
//                engine.onlineRemoveInputAndVerify(input);
//                nonexistantInputs.add(input);
//
//                List<UserInterface> loopList = engine.getUsersThatCanCurrentlySend();
//                randomize(loopList);
//
//                for (UserInterface user : loopList) {
//                    engine.attemptSendFromUserAndVerify(user);
//                }
//            }
//
////            // Bring the online-only users and connections into play
////            List<ConnectionInterface> allInputList = new LinkedList<>(onlineInputList);
////            allInputList.addAll(offlineInputList);
////            randomize(allInputList);
////
////            // This set will be used to connect or add users. As connections are connected, users will be added to this, it will be randomized, and three users will connect and/or send.
////            Set<UserInterface> usersToConnectOrAdd = new HashSet<>();
////            for (ConnectionInterface input : allInputList) {
////
////                // If the input is not on the server, add it
////                if (nonexistantInputs.contains(input)) {
////                    engine.onlineAddInput(input);
////                }
////
////                if (engine.get)
////                if (input.)
////
////                if (onlineInputList.contains(input)) {
////                    engine.onlineAddInput(input);
////                }
////
////
////                engine.onlineAddInput(input);
////
////                if (offlineInputList.contains(input)) {
////                    UserInterface[] inputUsers = input.getUsers(new UserFilter().setUserActivityIsValidated(true)).toArray(new UserInterface[0]);
////                    UserInterface[] myUsers = { inputUsers[0], inputUsers[1], inputUsers[2] };
////
////                    for (UserInterface user : myUsers) {
////
////                        if (onlineUserList.contains(user)) {
////                            engine.onlineA
////                            engine.onlineAddUser(ImmutableServerProfiles.SERVER_0, user);
////                        }
////
////                        if (offlineUserList.contains(user)) {
////
////                            if (conn)
////
////
////                        } else {
////
////                        }
////                    }
////
////                } else {
////
////                }
////            }
//
////
////            for (ConnectionInterface input : inputList) {
////                engine.onlineAddInput(input);
////
////                UserInterface[] inputUsers = input.getUsers(new UserFilter().setUserActivityIsValidated(true)).toArray(new UserInterface[0]);
////                UserInterface authssl01A = inputUsers[0];
////                UserInterface authsslB = inputUsers[1];
////
////                if (connectableUserList.contains(authssl01A)) {
////                    engine.connectClientAndVerify(true, authssl01A);
////                }
////
////                if (connectableUserList.contains(authsslB)) {
////                    engine.connectClientAndVerify(true, authsslB);
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
//            engine.stopServers(ImmutableServerProfiles.SERVER_0);
//            if (manipulator != null) {
//                manipulator.postTestRunPostServerStop(engine);
//            }
//        }
//    }
//}
