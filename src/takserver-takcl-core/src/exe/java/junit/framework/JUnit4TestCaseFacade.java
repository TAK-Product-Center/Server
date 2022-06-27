package junit.framework;

import com.bbn.marti.takcl.TAKCLCore;
import com.bbn.marti.takcl.TestConfiguration;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitResultFormatter;
import org.junit.runner.Description;

public class JUnit4TestCaseFacade implements Test {
	private final Description description;

	public JUnit4TestCaseFacade(Description description) {
		this.description = description;
	}

	public int countTestCases() {
		return 1;
	}

	public void run(TestResult result) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@link JUnitResultFormatter} determines the test name by reflection.
	 */
	public String getName() {
		return description.getMethodName();
	}

	@Override
	public String toString() {
		return description.getMethodName() + "(" +
				TestConfiguration.getInstance().toFormalTaggedName(description.getTestClass()) + ")";

	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		JUnit4TestCaseFacade that = (JUnit4TestCaseFacade) o;

		if (!description.equals(that.description)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		return description.hashCode();
	}

}
