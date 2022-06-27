package com.bbn.marti.test.shared;

import com.bbn.marti.test.shared.engines.TestEngine;

/**
 * Created to be used as a suppliable argument to tests to modify server state
 * Created on 10/26/15.
 */
public interface TestManipulatorInterface {
	public void preTestRunPreServerStart(TestEngine engine);

	public void preTestRunPostServerStart(TestEngine engine);

	public void postTestRunPreServerStop(TestEngine engine);

	public void postTestRunPostServerStop(TestEngine engine);

	public void printResults();
}
