package com.bbn.marti.takcl.AppModules;

import com.bbn.marti.takcl.AppModules.generic.AppModuleInterface;
import com.bbn.marti.takcl.TakclIgniteHelper;
import com.bbn.marti.takcl.TestConfiguration;
import com.bbn.marti.takcl.TestExceptions;
import com.bbn.marti.takcl.TestLogger;
import com.bbn.marti.takcl.cli.EndUserReadableException;
import com.bbn.marti.takcl.cli.simple.Command;
import com.bbn.marti.takcl.config.common.TakclRunMode;
import com.bbn.marti.takcl.connectivity.AbstractRunnableServer;
import com.bbn.marti.test.shared.AbstractSingleServerTestClass;
import com.bbn.marti.test.shared.AbstractTestClass;
import com.bbn.marti.tests.*;
import com.bbn.marti.tests.missions.*;
import com.cloudbees.junit.runner.App;
import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runners.MethodSorters;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.bbn.marti.takcl.config.common.TakclRunMode.*;

/**
 * Created on 6/16/16.
 */
public class TestRunnerModule implements AppModuleInterface {

	private final TakclRunMode[] takclRunModes = new TakclRunMode[]{
			REMOTE_SERVER_INTERACTION,
			LOCAL_SERVER_INTERACTION,
			LOCAL_SOURCE_INTERACTION
	};

	private final Class<?>[] testClasses = new Class[]{
			FederationV1Tests.class,
			FederationV2Tests.class,
			GeneralTests.class,
			InputTests.class,
			PointToPointTests.class,
			SubscriptionTests.class,
			WebsocketsTests.class,
			WebsocketsFederationTests.class,
			EnterpriseFileSync.class,
			MissionAddRetrieveRemove.class,
			MissionDataFlowTests.class,
			MissionFileSync.class,
			MissionUserCustomRolesTests.class,
			MissionUserDefaultRolesTests.class,
			UserManagementTests.class
//			SimpleTests.class
	};

	private final TestConfiguration tc;

	public TestRunnerModule() {
		this.tc = TestConfiguration.getInstance();
		this.tc.validate();
	}

	public enum DisplayValue {
		PASS("PASS", "\033[02;32m%s\033[0m"),
		FAIL("FAIL", "\033[02;91;1;5m%s\033[0m"),
		SKIP("SKIP", "\033[02;91;1;5m%s\033[0m");

		public final String value;
		public final String bash;

		DisplayValue(String value, String bashFormatting) {
			this.value = value;
			this.bash = String.format(bashFormatting, value);
		}
	}

	public static class RequestContainer {
		public final Method testMethod;
		public final Class<?> testClass;
		public final Request testRequest;
		public final String simpleTaggedName;
		public final String formalTaggedName;

		private RequestContainer(@NotNull Class<?> testClass, @NotNull Method method) {
			this.testMethod = method;
			this.testClass = testClass;
			this.testRequest = Request.method(this.testClass, method.getName());
			this.simpleTaggedName = TestConfiguration.getInstance().toSimpleName(this.testClass, method, true);
			this.formalTaggedName = TestConfiguration.getInstance().toFormalTaggedName(method);
		}

		public static List<RequestContainer> fromTestIdentifiers(@NotNull String... testIdentifiers) {

			TestConfiguration tc = TestConfiguration.getInstance();
			List<RequestContainer> result = new LinkedList<>();
			Class<?> testClass;
			Method testMethod = null;

			for (String identifier : testIdentifiers) {

				try {
					try {
						testClass = tc.classFromSimpleName(identifier);
					} catch (ClassNotFoundException e) {
						testClass = tc.methodClassFromSimpleName(identifier);
						testMethod = tc.methodFromSimpleName(identifier);
					}

					if (testMethod != null) {
						result.add(new RequestContainer(testClass, testMethod));
					} else {
						// TODO: Make single-server test parameters non-static, which requires them to be run in a single suite.
						//					if (AbstractSingleServerTestClass.class.isAssignableFrom(clazz)) {
						//						result.add(new RequestContainer(Request.aClass(clazz), coreNetworkVersion));
						//					} else {

						List<Method> testMethods = null;

						Method[] clazzMethods = testClass.getMethods();

						if (testClass.isAnnotationPresent(FixMethodOrder.class)) {
							FixMethodOrder annotation = testClass.getAnnotation(FixMethodOrder.class);
							if (annotation.value() == MethodSorters.NAME_ASCENDING) {
								testMethods = Arrays.stream(clazzMethods)
										.filter(x -> x.isAnnotationPresent(Test.class))
										.sorted(Comparator.comparing(Method::getName))
										.collect(Collectors.toList());
							}
						}

						if (testMethods == null) {
							testMethods = Arrays.stream(clazzMethods).filter(x -> x.isAnnotationPresent(Test.class)).collect(Collectors.toList());
						}
						for (Method method : testMethods) {
							result.add(new RequestContainer(testClass, method));
						}
					}

				} catch (ClassNotFoundException | NoSuchMethodException e) {
					throw new EndUserReadableException(identifier + " is not a valid test suite!");
				}
			}
			return result;
		}

	}


	private List<String> getTestList(boolean includeSingleServerTests, boolean includeTags, boolean includeParentClasses) {
		TreeSet<String> testSet = new TreeSet<>();

		for (Class<?> clazz : testClasses) {
			if (includeParentClasses) {
				testSet.add(tc.toSimpleName(clazz, includeTags));
			}

			if (AbstractTestClass.class.isAssignableFrom(clazz)) {
				Method[] methods = clazz.getMethods();
				for (Method method : methods) {
					if (method.getAnnotation(Test.class) != null) {
						testSet.add(tc.toSimpleName(clazz, method, includeTags));
					}
				}
			}
			if (includeSingleServerTests && AbstractSingleServerTestClass.class.isAssignableFrom(clazz)) {
				Method[] methods = clazz.getMethods();
				for (Method method : methods) {
					if (method.getAnnotation(Test.class) != null) {
						testSet.add(tc.toSimpleName(clazz, method, includeTags));
					}
				}
			}
		}
		return new ArrayList<>(testSet);
	}

	@Command(description = "List the tests available to run.")
	public String list() {
		List<String> testList = getTestList(false, false, true);
		return "\nEnvironment Tags: " + tc.getTestTags() + "\n\n\t" + String.join("\n\t", testList);
	}

	private void handleTestComparisonResults(@Nullable List<String> errorList) {
		if (errorList != null && !errorList.isEmpty()) {
			for (String value : errorList) {
				System.err.println(value);
				System.err.println("FAIL!");
				System.exit(1);
			}
		} else {
			System.out.println("Pass");
		}
	}

	@Command(description = "Compares the json activity logs of two test artifact directories")
	public void compareArtifactDirectories(String testRunDirA, String testRunDirB) {
		List<String> result = TestLogger.compareTestArtifactDirectoriesJsonFiles(testRunDirA, testRunDirB);
		handleTestComparisonResults(result);
	}

	@Command(description = "Compares the activities of two test runs")
	public void compareJsonLogs(String jsonRunLogA, String jsonRunLogB) {
		List<String> result = TestLogger.compareJsonFiles(jsonRunLogA, jsonRunLogB);
		handleTestComparisonResults(result);
	}

	@Command(description = "Runs a set of test suites. '/opt/tak/TEST_TMP' will be used as the temporary directory by default.")
	public void run(String... testIdentifier) throws EndUserReadableException {
		TestConfiguration.getInstance().validate();
		boolean testResult = true;

		String testOverride = System.getenv().getOrDefault("TESTNAME", null);

		Map<RequestContainer, Result> resultMap = new HashMap<>();


		if (testOverride != null) {
			testIdentifier = new String[]{testOverride};
		}

		List<RequestContainer> requests = RequestContainer.fromTestIdentifiers(testIdentifier);

		for (RequestContainer request : requests) {
			Result result = runTestImpl(AbstractRunnableServer.RUNMODE.AUTOMATIC, null, request);
			resultMap.put(request, result);
			if (result.getFailureCount() > 0) {
				testResult = false;
			}
		}
		innerDisplayResults(resultMap, System.err);
		TestExceptions.DO_NOT_CLOSE_IGNITE_INSTANCES = false;
		TakclIgniteHelper.closeAllIgniteInstances();
		System.exit(testResult ? 0 : 1);
	}


	public static String repeat(char character, int count) {
		return new String(new char[count]).replace("\0", String.valueOf(character));
	}

	private static String padRight(@NotNull String str, int finalLength) {
		return str + repeat(' ', (finalLength - str.length()));
	}

	public static List<String> makeChart(@NotNull List<String> columnHeaders, @NotNull List<Map<String, String>> rows, @NotNull String orderingKey) {
		// First gather some information on all known columns and their max necessary width
		TreeMap<String, Integer> columnSizeMap = new TreeMap<>();

		for (String header : columnHeaders) {
			int columnSize = header.length();
			for (Map<String, String> row : rows) {
				if (row.containsKey(header)) {
					String value = row.get(header);
					columnSize = Math.max(columnSize, value.length());
				} else {
					row.put(header, "");
				}
			}
			columnSizeMap.put(header, columnSize);
		}

		List<String> rval = new ArrayList<>(rows.size() + 1);

		StringBuilder headerRow = new StringBuilder("|");
		TreeMap<String, StringBuilder> resultsRows = new TreeMap<>();
		for (String header : columnHeaders) {
			int columnSize = columnSizeMap.get(header);
			headerRow.append(" ").append(padRight(header, columnSize)).append(" |");
			for (Map<String, String> rowData : rows) {
				String key = rowData.get(orderingKey);
				if (!resultsRows.containsKey(key)) {
					resultsRows.put(key, new StringBuilder("|"));
				}
				resultsRows.get(key).append(" ").append(padRight(rowData.get(header), columnSize)).append(" |");
			}
		}
		rval.add(headerRow.toString());
		for (String key : resultsRows.keySet()) {
			rval.add(resultsRows.get(key).toString());
		}
		return rval;
	}

	@Command(description = "Displays the results of the tests in the specified folder", isDev = true)
	public void displayResults(String targetDirectory) throws EndUserReadableException {
		File resultsFilepath = new File(targetDirectory);

		if (!resultsFilepath.exists()) {
			throw new EndUserReadableException("The specified path does not exist!");
		}

		TreeMap<String, Boolean> testResults = new TreeMap<>();
		for (File file : Objects.requireNonNull(resultsFilepath.listFiles())) {
			if (file.isFile() && file.getName().endsWith(".xml")) {
				try {
					Document doc = SAXReader.createDefault().read(file);
					Element rootElement = doc.getRootElement();
					List<Element> testcases = rootElement.elements("testcase");
					for (Element testcase : testcases) {
						Class<?> testClass = TestRunnerModule.class.getClassLoader().loadClass(testcase.attributeValue("classname"));
						Method testMethod = testClass.getMethod(testcase.attributeValue("name"));
						String name = tc.toSimpleName(testClass, testMethod, true);

						if (testcase.elements().stream().anyMatch(i -> i.getName().equals("error"))) {
							testResults.put(name, false);
						} else {
							testResults.put(name, true);
						}
					}

				} catch (DocumentException | ClassNotFoundException | NoSuchMethodException e) {
					throw new RuntimeException(e);
				}
			}
		}
		innerDisplayResults(testResults, System.err);
		System.exit(0);
	}

	@Command(description = "Validates the test results in the expected results directory for the specified core network version", isDev = true)
	public void validateCumulativeResults(@NotNull String includeRegex) {
		validateCumulativeResults(includeRegex, null);
	}


	@Command(description = "Validates the test results in the expected results directory for the specified core network version", isDev = true)
	public void validateCumulativeResults(@NotNull String includeRegex, @Nullable String excludeRegex) {
		TestConfiguration tc = TestConfiguration.getInstance();
		Pattern includePattern = Pattern.compile(includeRegex);
		Pattern excludePattern = excludeRegex == null ? null : Pattern.compile(excludeRegex);

		// Deconstructing and then validating tests to ensure this method covers them all
		List<String> fullTestList = getTestList(true, true, false);

		List<String> expectedTests = fullTestList.stream().filter(i -> includePattern.matcher(i).matches()).collect(Collectors.toList());

		if (excludePattern != null) {
			expectedTests = expectedTests.stream().filter(i -> !excludePattern.matcher(i).matches()).collect(Collectors.toList());
		}

		if (expectedTests.size() == 0) {
			System.err.println("No matching expected tests were found!");
			System.exit(-996);
		}

		List<String> unexpectedTests = new LinkedList<>();
		boolean skippedErrorsOrFailures = false;

		File resultsFilepath = Paths.get(TAKCLConfigModule.getInstance().getTemporaryDirectory()).resolve("TEST_ARTIFACTS").toFile();

		int passCount = 0;
		TreeMap<String, Boolean> testResults = new TreeMap<>();
		for (File file : Objects.requireNonNull(resultsFilepath.listFiles())) {
			if (file.isFile() && file.getName().endsWith(".xml")) {
				try {
					Document doc = SAXReader.createDefault().read(file);
					Element rootElement = doc.getRootElement();
					if (!rootElement.attributeValue("errors").equals("0") ||
							!rootElement.attributeValue("failures").equals("0") ||
							!rootElement.attributeValue("skipped").equals("0")) {
						skippedErrorsOrFailures = true;
					}

					List<Element> testcases = rootElement.elements("testcase");
					for (Element testcase : testcases) {
						Class<?> testClass = tc.classFromSimpleName(testcase.attributeValue("classname"));
						Method testMethod = testClass.getMethod(testcase.attributeValue("name"));
						String name = tc.toSimpleName(testClass, testMethod, true);

						if (expectedTests.contains(name)) {
							if (testcase.elements().stream().anyMatch(i -> i.getName().equals("error"))) {
								testResults.put(name, false);
							} else {
								passCount++;
								testResults.put(name, true);
							}
						} else {
							unexpectedTests.add(name);
						}
					}

				} catch (DocumentException e) {
					System.out.println("Error parsing file '" + file + "'!");
					System.err.println("Error parsing file '" + file + "'!");
					throw new RuntimeException(e);
				} catch (ClassNotFoundException | NoSuchMethodException e) {
					throw new RuntimeException(e);
				}
			}
		}

		for (String testName : expectedTests) {
			if (!testResults.containsKey(testName)) {
				testResults.put(testName, null);
			}
		}
		innerDisplayResults(testResults, System.err);

		int result = passCount - expectedTests.size();

		if (result == 0) {
			if (!unexpectedTests.isEmpty()) {
				StringBuilder sb = new StringBuilder("The following unexpected tests were executed: \n\t");
				for (String unexpectedTest : unexpectedTests) {
					sb.append(unexpectedTest).append("\n\t");
				}
				System.err.println(sb);
				System.exit(-999);

			} else if (skippedErrorsOrFailures) {
				System.err.println("Some tests were skipped, had errors, or had failures!");
				System.exit(-999);
			} else {
				System.exit(0);
			}
		} else {
			System.exit(result);
		}
	}

	private void innerDisplayResults(TreeMap<String, Boolean> resultMap, PrintStream target) {
		List<String> headers = Arrays.asList("Name", "Result");

		List<Map<String, String>> rows = new LinkedList<>();

		target.println("RESULTS:");
		for (String testName : resultMap.keySet()) {
			Map<String, String> rowMap = new HashMap<>();
			rows.add(rowMap);
			Boolean result = resultMap.get(testName);
			rowMap.put("Name", testName);
			rowMap.put("Result", result == null ? DisplayValue.SKIP.bash : result ? DisplayValue.PASS.bash : DisplayValue.FAIL.bash);
		}

		for (String row : makeChart(headers, rows, "Name")) {
			target.println(row);
		}
	}


	private void innerDisplayResults(Map<RequestContainer, Result> resultMap, PrintStream target) {
		List<String> headers = Arrays.asList("Name", "Result");

		List<Map<String, String>> rows = new LinkedList<>();

		target.println("RESULTS:");
		for (RequestContainer requestContainer : resultMap.keySet()) {
			Request request = requestContainer.testRequest;
			Map<String, String> rowMap = new HashMap<>();
			rows.add(rowMap);

			Description classDesc = request.getRunner().getDescription();

			if (classDesc.getChildren().size() == 1) {
				rowMap.put("Name", requestContainer.simpleTaggedName);
				rowMap.put("Result", (resultMap.get(requestContainer).getFailureCount() > 0 ? DisplayValue.FAIL.bash : DisplayValue.PASS.bash));
			} else {
				// TODO: Make single-server test parameters non-static, which requires them to be run in a single suite.
//				if (AbstractSingleServerTestClass.class.isAssignableFrom(classDesc.getTestClass())) {
//					Result result = resultMap.get(requestContainer);
//
//					for (Description methodDesc : classDesc.getChildren()) {
//						Failure failure = result.getFailures().get(0);
//						System.err.println("TEST_HEADER=" + failure.getTestHeader());
//
//						rowMap.put("Name", methodDesc.getClassName() + "." + methodDesc.getMethodName());
//						rowMap.put("Result", (resultMap.get(requestContainer).getFailureCount() > 0 ? DisplayValue.FAIL.bash : DisplayValue.PASS.bash));
//					}
//
//				} else {
				throw new RuntimeException("Due to splitting tests apart to prevent issues only one child should be in each Request!");
//				}

			}
		}

		for (String row : makeChart(headers, rows, "Name")) {
			target.println(row);
		}
	}

//	private boolean handleResult(Result result) {
//		int failureCount = result.getFailureCount();
//		if (failureCount <= 0) {
//			System.out.println("All tests passed.");
//			return true;
//		} else {
//			System.err.println("Test failure" + (failureCount == 1 ? "" : "s") + "! Details:");
//			for (Failure failure : result.getFailures()) {
//
//				String[] strings = failure.getTrace().split("\n");
//				int lastLineLogged = 0;
//				String simpleTrace = null;
//
//				for (int i = 0; i < strings.length; i++) {
//					String line = strings[i];
//					if (line.contains(failure.getTestHeader())) {
//
//						if (simpleTrace == null) {
//							simpleTrace = line;
//
//						} else if (i > (lastLineLogged + 1)) {
//							simpleTrace += ("\n...\n" + strings[i]);
//
//						} else {
//							simpleTrace += ("\n" + strings[i]);
//						}
//						lastLineLogged = i;
//					}
//				}
//
//				System.err.println(
//						failure.getDescription()
//								+ "\n\t" + failure.getMessage()
//								+ (simpleTrace == null ? "" : "\nSmart Trace: \n" + simpleTrace)
//								+ "\n\n" + "Full Trace: \n" + failure.getTrace());
//			}
//			return false;
//		}
//	}

	private Result runTestImpl(@NotNull AbstractRunnableServer.RUNMODE runMode, @Nullable Integer debugServerProfile,
	                           @NotNull RequestContainer testRequestContainer) throws EndUserReadableException {
		Request testRequest = testRequestContainer.testRequest;

		if (testRequest.getRunner().getDescription().getChildren().size() != 1) {
//			for (Description desc : testRequest.getRunner().getDescription().getChildren()) {
			// TODO: Make single-server test parameters non-static, which requires them to be run in a single suite.
//				if (!AbstractSingleServerTestClass.class.isAssignableFrom(desc.getTestClass())) {
			throw new RuntimeException("To minimize conflicts only one test may be run at a time!");
//				}
//			}
		}

		String simpleTaggedName = testRequestContainer.simpleTaggedName;
		String formalTaggedName = testRequestContainer.formalTaggedName;
		;
		Description testDescription = testRequest.getRunner().getDescription().getChildren().get(0);
		String formalTestName = testDescription.getClassName() + "." + testDescription.getMethodName();

		JUnitCore jUnitCore = new JUnitCore();

		final Path resultsPath = Paths.get(TAKCLConfigModule.getInstance().getTemporaryDirectory());

		if (!Files.exists(resultsPath)) {
			try {
				Files.createDirectory(resultsPath);
			} catch (IOException e) {
				throw new EndUserReadableException("Could not create the directory '" + resultsPath + "'!");
			}
		}
		Path xmlResultsPath = resultsPath.resolve("TEST_ARTIFACTS");
		if (!xmlResultsPath.toFile().exists()) {
			xmlResultsPath.toFile().mkdirs();
		}

		File xmlResultsFile = xmlResultsPath.resolve("TEST-" + formalTaggedName + ".xml").toFile();
		try {
			FileOutputStream xmlResultsOutputStream = new FileOutputStream(xmlResultsFile);

			App.JUnitResultFormatterAsRunListener listener = new App.JUnitResultFormatterAsRunListener(new XMLJUnitResultFormatter()) {
				@Override
				public void testStarted(Description description) throws Exception {
					assert ((description.getClassName() + "." + description.getMethodName()).equals(formalTestName));
					System.err.print("Starting " + simpleTaggedName + "... ");
					System.err.flush();
					if (!Files.exists(xmlResultsPath)) {
						Files.createDirectory(xmlResultsPath);
					}
					formatter.setOutput(xmlResultsOutputStream);
					super.testStarted(description);
				}
			};

			jUnitCore.addListener(listener);

			Result result = jUnitCore.run(testRequest);

			xmlResultsOutputStream.flush();
			xmlResultsOutputStream.close();

			System.err.println(result.getFailureCount() == 0 ? DisplayValue.PASS.bash : DisplayValue.FAIL.bash);
			System.err.flush();

			String stdCombined = listener.getCombinedStdoutStderr();
			if (stdCombined != null) {
				try {
					File target_file = xmlResultsPath.resolve("TEST-" + formalTaggedName + "-out.txt").toFile();
					FileOutputStream textOutputStream = new FileOutputStream(target_file);
					IOUtils.write(stdCombined, textOutputStream, "UTF-8");
					textOutputStream.flush();
					textOutputStream.close();
				} catch (IOException e) {
					System.err.println(e.getMessage());
				}
			}
			return result;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@NotNull
	@Override
	public TakclRunMode[] getRunModes() {
		return takclRunModes;
	}

	@Override
	public ServerState getRequiredServerState() {
		return null;
	}

	@Override
	public String getCommandDescription() {
		return "Provides a mechanism for running tests.";
	}

	@Override
	public void init() {

	}
}
