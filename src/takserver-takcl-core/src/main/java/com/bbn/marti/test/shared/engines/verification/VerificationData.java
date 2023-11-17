package com.bbn.marti.test.shared.engines.verification;

import com.bbn.marti.test.shared.TestConnectivityState;
import com.bbn.marti.test.shared.data.users.AbstractUser;
import com.bbn.marti.test.shared.engines.state.StateEngine;
import com.bbn.marti.tests.Assert;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Created on 2/9/18.
 */
public class VerificationData {

	static VerificationData instance = new VerificationData();

	private VerificationData() {
	}

	private final TreeMap<AbstractUser, VerificationClient> userVerificationIterationData = new TreeMap<>();

	public static class VerificationClient {
		private final AbstractUser profile;

		private final TreeSet<AbstractUser> expectedSenders;
		private final TestConnectivityState expectedConnectivityState;


		private VerificationClient(@NotNull AbstractUser profile, @Nullable TreeSet<AbstractUser> expectedSenders,
		                           @NotNull TestConnectivityState expectedConnectivityState) {
			this.profile = profile;
			this.expectedSenders = expectedSenders == null ? new TreeSet<>() : expectedSenders;
			this.expectedConnectivityState = expectedConnectivityState;
		}

		public AbstractUser getProfile() {
			return profile;
		}

		public synchronized TreeSet<AbstractUser> getExpectedSenders() {
			return new TreeSet<>(expectedSenders);
		}

		public TestConnectivityState getExpectedConnectivityState() {
			return expectedConnectivityState;
		}
	}

	public synchronized void setUserExpectations(@NotNull AbstractUser user, @Nullable TreeSet<AbstractUser> expectedSenders,
	                                             @NotNull TestConnectivityState expectedConnectivityState) {
		if (user.doValidation()) {
			if (userVerificationIterationData.containsKey(user)) {
				String message = "Cannot clobber already set User Expectations!";
				Assert.fail(message);
				throw new RuntimeException(message);
			}
			System.out.println("--- VerificationData setUserExpectations, put user: " + user +", expectedSenders.size(): "+ expectedSenders.size()+ ", expectedConnectivityState: " + expectedConnectivityState);
			userVerificationIterationData.put(user, new VerificationClient(user, expectedSenders, expectedConnectivityState));
		}
	}

	synchronized void validateAllUserExpectations(String justification) {
		// TODO: All users should really be validated, not just those tagged with a validator.
		for (Map.Entry<AbstractUser, VerificationClient> entry : userVerificationIterationData.entrySet()) {
			System.out.println("\t ---VerificationData: validateAllUserExpectations: entry.getKey():" + entry.getKey() + ", entry.getValue().getProfile(): "+ entry.getValue().getProfile());
			UserExpectationValidator uev = new UserExpectationValidator(
					StateEngine.data.getState(entry.getKey()), entry.getValue());
			uev.validateExpectations(justification, true);
		}
	}

	synchronized void engineIterationDataClear() {
		userVerificationIterationData.clear();
	}

	public synchronized VerificationClient getState(AbstractUser user) {
		if (!userVerificationIterationData.containsKey(user)) {
			String message = "No VerificationClient has been created for user '" + user.toString() + "'!";
			Assert.fail(message);
			throw new RuntimeException(message);
		}
		return userVerificationIterationData.get(user);
	}

	public synchronized TreeSet<VerificationClient> getAllUserStates() {
		TreeSet<VerificationClient> rval = new TreeSet<>();
		for (AbstractUser value : userVerificationIterationData.keySet()) {
			rval.add(userVerificationIterationData.get(value));
		}
		return rval;
	}
}
