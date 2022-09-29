package com.bbn.marti.test.shared;

import ch.qos.logback.classic.Level;
import com.bbn.marti.takcl.AppModules.TAKCLConfigModule;
import com.bbn.marti.takcl.*;
import com.bbn.marti.takcl.connectivity.server.AbstractRunnableServer;
import com.bbn.marti.test.shared.data.servers.AbstractServerProfile;
import com.bbn.marti.test.shared.data.servers.ImmutableServerProfiles;
import com.bbn.marti.test.shared.engines.TestEngine;
import org.junit.BeforeClass;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Created on 10/26/15.
 */
public abstract class AbstractTestClass {

	// DO NOT change this. It is used to determine the split between tests when analyzing logs throughout the tests
	public static final String LOG_START_FORMATTER = "$#$# Starting Execution: {%s}";

	static {
		TAKCLCore.TEST_MODE = true;

		TAKCLogging.setClassLoggerBuilder((aClass, s) -> {
			ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(aClass);
			if (s != null) {
				System.err.println("Setting logging level for '" + aClass.getCanonicalName() + "' to '" + s + "'.");
				logger.setLevel(Level.valueOf(s));
			}
			return logger;
		}, new HashSet<>(Arrays.asList("ALL", "DEBUG", "ERROR", "INFO", "OFF", "TRACE", "WARN")));

		TAKCLogging.setStringLoggerBuilder((aClassName, s) -> {
			ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(aClassName);
			if (s != null) {
				System.err.println("Setting logging level for '" + aClassName + "' to '" + s + "'.");
				logger.setLevel(Level.valueOf(s));
			}
			return logger;
		}, new HashSet<>(Arrays.asList("ALL", "DEBUG", "ERROR", "INFO", "OFF", "TRACE", "WARN")));
	}

	protected String initTestMethod() {
		try {
			StackTraceElement element = Thread.currentThread().getStackTrace()[2];
			Class<?> testClass = AbstractTestClass.class.getClassLoader().loadClass(element.getClassName());
			Method testMethod = testClass.getMethod(element.getMethodName());
			String sessionIdentifier = TestConfiguration.getInstance().toFormalTaggedName(testMethod);
			LoggerFactory.getLogger(testClass).info(String.format(LOG_START_FORMATTER, sessionIdentifier));

			engine.engineFactoryReset();
			TestLogger.startTestWithIdentifier(sessionIdentifier);
			return sessionIdentifier;
		} catch (ClassNotFoundException | NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	public static final String TEST_ARTIFACT_DIRECTORY = TAKCLConfigModule.getInstance().getTestArtifactDirectory();

	public static TestEngine engine;

	public static boolean randomize = true;

	public static Long randomSeed = null;

	private static Random random;

	public static final AbstractServerProfile defaultServerProfile = ImmutableServerProfiles.SERVER_0;

	public static void randomize(List l) {
		if (random == null) {
			if (randomSeed == null) {
				random = new Random();
			} else {
				random = new Random(randomSeed);
			}
		}

		if (randomize) {
			Collections.shuffle(l, random);
		}
	}

	public static boolean testMode;

	@BeforeClass
	public static void setup() {
		try {
			SSLHelper.genCertsIfNecessary();
			if (engine != null) {
				engine.engineFactoryReset();
			}
			AbstractRunnableServer.setLogDirectory(TEST_ARTIFACT_DIRECTORY);
			com.bbn.marti.takcl.TestLogger.setFileLogging(TEST_ARTIFACT_DIRECTORY);
			engine = new TestEngine(defaultServerProfile);
		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace(System.err);
			throw new RuntimeException(e);
		}
	}

//    @AfterClass
//    public static final void cleanup() {
//        if (engine != null) {
//            engine.engineFactoryReset();
//        }
//    }
//
//    @BeforeClass
//    public static void init() {
//        if (testMode) {
//
//        }
//    }
//
//
//    @AfterClass
//    public static void cleanup() {
//        engine.stopServer();
//    }
}
