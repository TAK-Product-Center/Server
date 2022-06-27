package com.bbn.marti.tests;

import org.junit.Test;

public class FederationV2Tests extends AbstractFederationTests {
	@Test(timeout = 920000)
	public void basicFederationTest() {
		String sessionIdentifier = initTestMethod();
		executeBasicFederationTest(false, true, sessionIdentifier);
	}

	@Test(timeout = 960000)
	public void basicMultiInputFederationTest() {
		String sessionIdentifier = initTestMethod();
		executeBasicMultiInputFederationTest(false, true, sessionIdentifier);
	}

	@Test(timeout = 8400000)
	public void advancedFederationTest() {
		String sessionIdentifier = initTestMethod();
		executeAdvancedFederationTest(false, true, sessionIdentifier);
	}

	@Test(timeout = 1400000)
	public void federateConnectionInitiatorWaitTest() {
		String sessionIdentifier = initTestMethod();
		executeFederateConnectionInitiatorWaitTest(false, true, sessionIdentifier);
	}
}
