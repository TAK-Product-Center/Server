//package com.bbn.marti.takcl.connectivity.implementations;
//
//import com.bbn.marti.takcl.TestLogger;
//import com.bbn.marti.test.shared.CotGenerator;
//import com.bbn.marti.test.shared.TestConnectivityState;
//import com.bbn.marti.test.shared.data.connections.AbstractConnection;
//import com.bbn.marti.test.shared.data.users.AbstractUser;
//import com.bbn.marti.test.shared.engines.state.UserState;
//import com.google.common.base.Strings;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//import org.junit.Assert;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
///**
// * Created on 11/13/15.
// */
////public class UnifiedExpectantClient extends UnifiedClient implements Comparable<UnifiedExpectantClient> { 
//public class UnifiedExpectantClient implements Comparable<UnifiedExpectantClient> {
//
////    @Override
////    public int compareTo(UnifiedExpectantClient o) {
////        if (o == null) {
////            return 1;
////        } else {
////            return (getUser().getConsistentUniqueReadableIdentifier().compareTo(o.getUser().getConsistentUniqueReadableIdentifier()));
////        }
////    }
////
////    public static class ClientExpectations {
////        private final String clientIdentifier;
////        private final List<AbstractUser> expectedSenders;
////        private final TestConnectivityState expectedConnectivityState;
////        private Boolean expectationsMet;
////
////        public ClientExpectations(@NotNull String clientIdentifier, @Nullable List<AbstractUser> expectedSenders, @NotNull TestConnectivityState expectedConnectivityState) {
////            this.clientIdentifier = clientIdentifier;
////            this.expectedSenders = expectedSenders;
////            this.expectedConnectivityState = expectedConnectivityState;
////        }
////
////        public List<AbstractUser> getExpectedSenders() {
////            return expectedSenders;
////        }
////
////        public TestConnectivityState getExpectedConnectivityState() {
////            return expectedConnectivityState;
////        }
////
////        void setExpectationsMet(boolean value) {
////            this.expectationsMet = value;
////        }
////
////        public Boolean expectationsMet() {
////            return expectationsMet;
////        }
////
////        public String getOwnIdentifier() {
////            return clientIdentifier;
////        }
////    }
//
//    private boolean isServerInputActive;
//
//    private UserState.ClientExpectations clientExpectations = null;
//
//    private final UnifiedClient client;
//
//    private final UserState state;
//
//    public UnifiedExpectantClient(@NotNull UserState state) {
//        this.state = state;
//        client = new UnifiedClient(state);
//    }
//
//    public synchronized void setExpectations(@Nullable List<String> expectedSenders, @Nullable TestConnectivityState expectedState) {
//        if (clientExpectations != null || !state.getRecievedMessages().isEmpty()) {
//            String message = "Cannot clobber currently set expectations or received messages!";
//            Assert.fail(message);
//            throw new RuntimeException(message);
//        } else {
//            List<AbstractUser> sendingUsers;
//
//            if (expectedSenders == null) {
//                sendingUsers = new ArrayList<>(0);
//            } else {
//                sendingUsers = stateManager.getUsers(expectedSenders);
//            }
//            clientExpectations = new UserState.ClientExpectations(this.getUser().getConsistentUniqueReadableIdentifier(), sendingUsers, expectedState == null ? state.getConnectivityState() : expectedState);
//        }
//        state.getRecievedMessages().clear();
//    }
//
//    public synchronized UserState.ClientExpectations checkExpectations(@NotNull String justification) {
//        boolean metExpectations;
//        if (user.doValidation()) {
//            metExpectations = metExpectations(justification, true);
//            clientExpectations.setExpectationsMet(metExpectations);
//        } else {
//            metExpectations = true;
//        }
//        return clientExpectations;
//    }
//
//    public synchronized UserState.ClientExpectations getAndClearExpectations() {
//        UserState.ClientExpectations expectations = null;
//        if (clientExpectations != null) {
//            expectations = clientExpectations;
//            clientExpectations = null;
//        }
//        state.getRecievedMessages().clear();
//        return expectations;
//    }
//
//    private boolean metExpectations(@NotNull String justification, boolean failIfUnmet) {
//        String errorString = "";
//
//        if (state.getConnectivityState() != clientExpectations.getExpectedConnectivityState()) {
//            if (clientExpectations.getExpectedConnectivityState() == TestConnectivityState.Disconnected) {
//                errorString += toString() + " expected disconnection but still connected!";
//            } else {
//                errorString += toString() + " expected to be connected but was disconnected!";
//            }
//        } else {
//            if (state.getConnectivityState() == TestConnectivityState.Disconnected && state.getConnectivityState() != state.getPreviouslyCheckedConnectivityState()) {
//                TestLogger.logUserDisconnected(toString(), justification);
//                state.setPreviouslyCheckedConnectivityState(state.getConnectivityState());
//            }
//        }
//
//        Map<String, Integer> expectedDidSendMap = new HashMap<>();
//        Map<String, Integer> shouldHaveSentMap = new HashMap<>();
//        Map<String, Integer> shouldntHaveSentMap = new HashMap<>();
//
//        List<AbstractUser> expectedSenders = new ArrayList<>(clientExpectations.getExpectedSenders());
//
//        // Go through all the received messages
//        for (String msg : state.getRecievedMessages()) {
//            String uid = CotGenerator.parseUID(msg);
//            String endpoint = CotGenerator.parseEndpoint(msg);
//
//            // If validation shouldn't be done, ignore the user's messages
//            AbstractUser sendingUser = stateManager.getUserWithIdentifier(endpoint == null ? uid : endpoint);
//            String sendingUserDisplayString = sendingUser.getDynamicName();
//
//            if (sendingUser.doValidation()) {
//                // If the getConsistentUniqueReadableIdentifier exists in the expectedSender, increment the receive count for that user by 1 and remove the first occurrence of the getConsistentUniqueReadableIdentifier from the expectedSenders
//                // If the user should have sent, increment their send count in the expectedDidSendMap
//                if (expectedSenders.contains(sendingUser)) {
//                    if (expectedDidSendMap.containsKey(sendingUserDisplayString)) {
//                        expectedDidSendMap.put(sendingUserDisplayString, expectedDidSendMap.get(sendingUserDisplayString) + 1);
//
//                    } else {
//                        expectedDidSendMap.put(sendingUserDisplayString, 1);
//                    }
//                    expectedSenders.remove(sendingUser);
//
//                    // Otherwise, increment the user's send count in the shouldntHaveSent list
//                } else {
//                    if (shouldntHaveSentMap.containsKey(sendingUserDisplayString)) {
//                        shouldntHaveSentMap.put(sendingUserDisplayString, shouldntHaveSentMap.get(sendingUserDisplayString) + 1);
//
//                    } else {
//                        shouldntHaveSentMap.put(sendingUserDisplayString, 1);
//                    }
//                }
//            }
//        }
//
//        // If there are any expectedSenders that nothing was received from, add their counts to the shouldHaveSentMap
//        for (AbstractUser sendingUser : expectedSenders) {
//            String sendingUserDisplayString = sendingUser.getDynamicName();
//            if (shouldHaveSentMap.containsKey(sendingUserDisplayString)) {
//                shouldHaveSentMap.put(sendingUserDisplayString, shouldHaveSentMap.get(sendingUserDisplayString) + 1);
//            } else {
//                shouldHaveSentMap.put(sendingUserDisplayString, 1);
//            }
//        }
//
//        // Go through all the users that did send, comparing the actual and expected send count if necessary to determine the error
//        for (String sender : expectedDidSendMap.keySet()) {
//            if (shouldHaveSentMap.containsKey(sender)) {
//                int totalSendCount = expectedDidSendMap.get(sender) + shouldHaveSentMap.get(sender);
//                String error = sender + " >-" + expectedDidSendMap.get(sender) + "!=" + totalSendCount + "-> " + getUser().getDynamicName() + " - Justification: " + justification;
//                System.out.println(error);
//                errorString += (error + "\n");
//                shouldHaveSentMap.remove(sender);
//
//            } else if (shouldntHaveSentMap.containsKey(sender)) {
//                int totalSendCount = expectedDidSendMap.get(sender) + shouldntHaveSentMap.get(sender);
//                String error = sender + " >-" + totalSendCount + "!=" + expectedDidSendMap.get(sender) + "-> " + getUser().getDynamicName() + " - Justification: " + justification;
//                System.out.println(error);
//                errorString += (error + "\n");
//                shouldntHaveSentMap.remove(sender);
//
//            } else {
//                //                System.out.println(sender + " >-" + expectedDidSendMap.get(sender) + "-> " + getUserWithIdentifier().getDynamicName() + " - Justification: " + justification);
//                TestLogger.logUserSend(sender, expectedDidSendMap.get(sender), getUser().getDynamicName(), justification);
//            }
//        }
//
//        // Process the senders that should have sent but didn't
//        for (String sender : shouldHaveSentMap.keySet()) {
//            String error = sender + " >-0!=" + shouldHaveSentMap.get(sender) + "-> " + getUser().getDynamicName() + " - Justification: " + justification;
//            System.out.println(error);
//            errorString += (error + "\n");
//        }
//
//        // Process the senders that shouldn't have sent but did
//        for (String sender : shouldntHaveSentMap.keySet()) {
//            String error = sender + " >-" + shouldntHaveSentMap.get(sender) + "!=0-> " + getUser().getDynamicName() + " - Justification: " + justification;
//            System.out.println(error);
//            errorString += (error + "\n");
//        }
//
////        if (!Strings.isNullOrEmpty(errorString)) {
////            for (String msg : recievedMessages) {
////                System.out.println(msg);
////            }
////        }
////
//        if (!Strings.isNullOrEmpty(errorString)) {
//            if (failIfUnmet) {
//                Assert.fail(errorString);
//            }
//            return false;
//        }
//        return true;
//    }
//
//    public AbstractUser getUser() {
//        return user;
//    }
//
//    public AbstractConnection getConnection() {
//        return state.getConnection();
//    }
//
//    public final void setIsServerInputActive(boolean value) {
//        // As of 01/13/2016, Clients are no longer disconnected when an input is removed. This was necessary to resolve a significant memory leak. Similar check is also commented out in UnifiedExpectantClient CanCurrentlyReceive and CanCurrentlySend checks
//        if (!value && state.getConnection().getProtocol().canConnect() && client.isConnected() && state.getConnection().getProtocol().clientConnectionSeveredWithInputRemoval()) {
//            throw new RuntimeException("Client '" + toString() + "' is still connected even though the server has turned off the connection!");
//        }
//        isServerInputActive = value;
//    }
//
//    public final boolean canPotentiallyConnect() {
//        return state.getConnection().getProtocol().canConnect() && isServerInputActive;
//    }
//
//}
