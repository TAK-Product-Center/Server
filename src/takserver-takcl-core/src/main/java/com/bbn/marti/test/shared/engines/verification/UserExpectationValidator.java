package com.bbn.marti.test.shared.engines.verification;

import com.bbn.marti.takcl.TestExceptions;
import com.bbn.marti.takcl.TestLogger;
import com.bbn.marti.test.shared.CotGenerator;
import com.bbn.marti.test.shared.TestConnectivityState;
import com.bbn.marti.test.shared.data.users.AbstractUser;
import com.bbn.marti.test.shared.engines.ActionEngine;
import com.bbn.marti.test.shared.engines.state.StateEngine;
import com.bbn.marti.test.shared.engines.state.UserState;
import com.bbn.marti.tests.Assert;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created on 2/1/18.
 */
public class UserExpectationValidator {

	// Used to allow time for client threads to receive messages that may have been delayed to indicate a need for timing adjustments
	private static Integer FAILURE_DELAY_TIME = 80000;

	private final Logger logger = LoggerFactory.getLogger(UserExpectationValidator.class);

	private final UserState state;
	private final VerificationData.VerificationClient transientUserData;

	public UserExpectationValidator(UserState state, VerificationData.VerificationClient transientUserData) {
		this.state = state;
		this.transientUserData = transientUserData;
	}

	public String toString() {
		return state.toString();
	}

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
//            clientExpectations = new TransientUserData(state.getProfile().getConsistentUniqueReadableIdentifier(), sendingUsers, expectedState == null ? state.getConnectivityState() : expectedState);
//        }
//        state.getRecievedMessages().clear();
//    }
//
//    public synchronized TransientUserData checkExpectations(@NotNull String justification) {
//        boolean metExpectations;
//        if (state.getProfile().doValidation()) {
//            metExpectations = metExpectations(justification, true);
//            clientExpectations.setExpectationsMet(metExpectations);
//        } else {
//            metExpectations = true;
//        }
//        return clientExpectations;
//    }
//
//    public synchronized TransientUserData getAndClearExpectations() {
//        TransientUserData expectations = null;
//        if (clientExpectations != null) {
//            expectations = clientExpectations;
//            clientExpectations = null;
//        }
//        state.getRecievedMessages().clear();
//        return expectations;
//    }

	public boolean validateExpectations(@NotNull String justification, boolean failIfUnmet) {
		String errorString = "";
		ActionEngine.ActionClient client = ActionEngine.data.getState(state.getProfile());

		if (client.getConnectivityState() != transientUserData.getExpectedConnectivityState()) {
			if (transientUserData.getExpectedConnectivityState() == TestConnectivityState.Disconnected) {
				errorString += toString() + " expected disconnection but still connected!";
			} else {
				errorString += toString() + " expected to be connected but was disconnected!";
			}
		} else {
			if (client.getConnectivityState() == TestConnectivityState.Disconnected &&
					state.getConnectivityState() != TestConnectivityState.Disconnected) {
				TestLogger.logUserDisconnected(toString(), justification);

			} else if (client.getConnectivityState() == TestConnectivityState.ConnectedUnauthenticated &&
					state.getConnectivityState() != TestConnectivityState.ConnectedUnauthenticated) {
				TestLogger.logUserConnected(toString(), justification);

			} else if (client.getConnectivityState() == TestConnectivityState.ConnectedAuthenticatedIfNecessary) {
				if (state.getConnectivityState() == TestConnectivityState.Disconnected) {
					TestLogger.logUserConnected(toString(), justification);
					TestLogger.logUserAuthenticated(toString(), justification);

				} else if (state.getConnectivityState() == TestConnectivityState.ConnectedUnauthenticated) {
					TestLogger.logUserAuthenticated(toString(), justification);
				}
			}
		}

		Map<String, Integer> expectedDidSendMap = new HashMap<>();
		Map<String, Integer> shouldHaveSentMap = new HashMap<>();
		Map<String, Integer> shouldntHaveSentMap = new HashMap<>();

		List<AbstractUser> expectedSenders = new ArrayList<>(transientUserData.getExpectedSenders());

		List<String> receivedMessages = ActionEngine.data.getState(state.getProfile()).getRecievedMessages();

		// Go through all the received messages
		for (String msg : receivedMessages) {
			// If it is a protobuf protocol control message, ignore
			// TODO: I should probably learn more about this and properly test it
			if (TestExceptions.IGNORE_EMPTY_MESSAGES && CotGenerator.isEmpty(msg)) {
				continue;
			}
			String type = CotGenerator.parseType(msg);
			if (type.equals(CotGenerator.PROTOBUF_UPGRADE_TYPE)) {
				continue;
			}
			String uid = CotGenerator.parseClientUID(msg);
			String endpoint = CotGenerator.parseEndpoint(msg);

			// If validation shouldn't be done, ignore the user's messages
			AbstractUser sendingUser = StateEngine.data.getUserState(endpoint == null ? uid : endpoint).getProfile();


			String sendingUserDisplayString = sendingUser.getDynamicName();

			if (sendingUser.doValidation()) {
				// If the getConsistentUniqueReadableIdentifier exists in the expectedSender, increment the receive count for that user by 1 and remove the first occurrence of the getConsistentUniqueReadableIdentifier from the expectedSenders
				// If the user should have sent, increment their send count in the expectedDidSendMap
				if (expectedSenders.contains(sendingUser)) {
					if (expectedDidSendMap.containsKey(sendingUserDisplayString)) {
						expectedDidSendMap.put(sendingUserDisplayString, expectedDidSendMap.get(sendingUserDisplayString) + 1);

					} else {
						expectedDidSendMap.put(sendingUserDisplayString, 1);
					}
					expectedSenders.remove(sendingUser);

					// Otherwise, increment the user's send count in the shouldntHaveSent list
				} else {
					if (shouldntHaveSentMap.containsKey(sendingUserDisplayString)) {
						shouldntHaveSentMap.put(sendingUserDisplayString, shouldntHaveSentMap.get(sendingUserDisplayString) + 1);

					} else {
						shouldntHaveSentMap.put(sendingUserDisplayString, 1);
					}
				}
			}
		}

		// If there are any expectedSenders that nothing was received from, add their counts to the shouldHaveSentMap
		for (AbstractUser sendingUser : expectedSenders) {
			String sendingUserDisplayString = sendingUser.getDynamicName();
			if (shouldHaveSentMap.containsKey(sendingUserDisplayString)) {
				shouldHaveSentMap.put(sendingUserDisplayString, shouldHaveSentMap.get(sendingUserDisplayString) + 1);
			} else {
				shouldHaveSentMap.put(sendingUserDisplayString, 1);
			}
		}

		// Go through all the users that did send, comparing the actual and expected send count if necessary to determine the error
		for (String sender : expectedDidSendMap.keySet()) {
			if (shouldHaveSentMap.containsKey(sender)) {
				int totalSendCount = expectedDidSendMap.get(sender) + shouldHaveSentMap.get(sender);
				String error = sender + " >-" + expectedDidSendMap.get(sender) + "!=" + totalSendCount + "-> " + state.getProfile().getDynamicName() + " - Justification: " + justification;
				error = error + "Messages received by " + state.getProfile().getDynamicName() + ": \n[\n\t" + String.join("\n\t", receivedMessages) + "\n]";
				logger.info("#!#!@ " + error);
				errorString += (error + "\n");
				shouldHaveSentMap.remove(sender);

			} else if (shouldntHaveSentMap.containsKey(sender)) {
				int totalSendCount = expectedDidSendMap.get(sender) + shouldntHaveSentMap.get(sender);
				String error = sender + " >-" + totalSendCount + "!=" + expectedDidSendMap.get(sender) + "-> " + state.getProfile().getDynamicName() + " - Justification: " + justification;
				error = error + "Messages received by " + state.getProfile().getDynamicName() + ": \n[\n\t" + String.join("\n\t", receivedMessages) + "\n]";
				logger.info("#!#!@ " + error);
				errorString += (error + "\n");
				shouldntHaveSentMap.remove(sender);

			} else {
				TestLogger.logUserSend(sender, expectedDidSendMap.get(sender), state.getProfile().getDynamicName(), justification);
			}
		}

		// Process the senders that should have sent but didn't
		for (String sender : shouldHaveSentMap.keySet()) {
			if (!TestExceptions.IGNORE_DISCONNECT_LATESTSA_FAILURES || !justification.equals("UserDisconnected")) {
				String error = sender + " >-0!=" + shouldHaveSentMap.get(sender) + "-> " + state.getProfile().getDynamicName() + " - Justification: " + justification;
				logger.info("#!#!@ " + error);

				errorString += (error + "\n");
			}
		}

		// Process the senders that shouldn't have sent but did
		for (String sender : shouldntHaveSentMap.keySet()) {
			String error = sender + " >-" + shouldntHaveSentMap.get(sender) + "!=0-> " + state.getProfile().getDynamicName() + " - Justification: " + justification;
			logger.info("#!#!@ " + error);
			errorString += (error + "\n");
		}

//        if (!Strings.isNullOrEmpty(errorString)) {
//            for (String msg : recievedMessages) {
//                System.out.println(msg);
//            }
//        }
//
		if (errorString != null && !errorString.isEmpty()) {
			if (failIfUnmet) {
				if (FAILURE_DELAY_TIME != null) {
					try {
						Thread.sleep(FAILURE_DELAY_TIME);
					} catch (InterruptedException e) {
						// Do nothing
					}
				}
				Assert.fail(errorString);
			}
			return false;
		}
		return true;
	}

}
