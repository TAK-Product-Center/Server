package com.bbn.marti.tests;

import org.junit.Test;

public class FederationV1Tests extends AbstractFederationTests {
	@Test(timeout = 920000)
	public void basicFederationTest() {
		String sessionIdentifier = initTestMethod();
		executeBasicFederationTest(true, false, sessionIdentifier);
	}

	@Test(timeout = 960000)
	public void basicMultiInputFederationTest() {
		String sessionIdentifier = initTestMethod();
		executeBasicMultiInputFederationTest(true, false, sessionIdentifier);
	}

	@Test(timeout = 8400000)
	public void advancedFederationTest() {
		String sessionIdentifier = initTestMethod();
		executeAdvancedFederationTest(true, false, sessionIdentifier);
	}

	@Test(timeout = 1400000)
	public void federateConnectionInitiatorWaitTest() {
		String sessionIdentifier = initTestMethod();
		executeFederateConnectionInitiatorWaitTest(true, false, sessionIdentifier);
	}
}
