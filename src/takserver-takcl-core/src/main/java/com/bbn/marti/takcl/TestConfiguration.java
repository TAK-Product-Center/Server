package com.bbn.marti.takcl;

import com.bbn.marti.config.Repository;
import com.bbn.marti.test.shared.data.servers.AbstractServerProfile;
import com.bbn.marti.test.shared.data.servers.ImmutableServerProfiles;
import com.bbn.marti.test.shared.data.servers.MutableServerProfile;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

public class TestConfiguration {

	private static final String GENERAL_TEST_PKG = "com.bbn.marti.tests.";

	private static TestConfiguration instance;

	private static final Logger log = LoggerFactory.getLogger(TestConfiguration.class);

	private boolean hasBeenVerified = false;

	public synchronized static TestConfiguration getInstance() {
		if (instance == null) {
			instance = new TestConfiguration();
		}
		return instance;
	}

	private static String getJavaVersion() {
		try {
			Process p = new ProcessBuilder().command("java", "-version").start();
			p.waitFor(20, TimeUnit.SECONDS);
			byte[] value = IOUtils.toByteArray(p.getErrorStream());
			String version = new String(value).split("\n")[0];
			version = version.substring(version.indexOf("\"") + 1, version.lastIndexOf("\""));
			return version;
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private static boolean testDb(AbstractServerProfile server) {
		if (server.getDbHost() == null) {
			return false;
		}
		try {
			Connection c = DriverManager.getConnection("jdbc:postgresql://" + server.getDbHost() + ":5432/cot", "martiuser", server.getDbPassword());
			c.close();
			log.debug(server + " SQL Host: " + server.getDbHost() + ". Status: Available");
			return true;
		} catch (SQLException e) {
			log.debug(server + " SQL Host: " + server.getDbHost() + ". Status: Unavailable");
			return false;
		}
	}

	public final String server0DBHost;
	public final String server1DBHost;
	public final String server2DBHost;
	public final boolean dbEnabled;
	public Boolean dbAvailable = null;
	public final String javaVersion;

	public TestConfiguration() {
		this.dbEnabled = TAKCLCore.TakclOption.EnableDB.getBoolean();
		// If the java version is null, it wasn't supplied, so just use the current java version
		String tmpJavaVersion = TAKCLCore.TakclOption.JavaVersion.getStringOrNull();
		this.javaVersion = tmpJavaVersion == null ? getJavaVersion() : tmpJavaVersion;
		this.server0DBHost = TAKCLCore.TakclOption.Server0DbHost.getStringOrNull();
		this.server1DBHost = TAKCLCore.TakclOption.Server1DbHost.getStringOrNull();
		this.server2DBHost = TAKCLCore.TakclOption.Server2DbHost.getStringOrNull();
	}

	public synchronized void validate() {
		if (hasBeenVerified) {
			return;
		}
		// dbEnabled is a server flag, so no environment validation necessary
		// coreNetworkVersion is a server flag, so no environment validation necessary

		// If the desired java version does not equal the actual java version, the test is invalid

		String detectedJavaVersion = getJavaVersion();
		log.info("Actual/Defined java Version: " + detectedJavaVersion + "/" + this.javaVersion);
		if (!this.javaVersion.equals(detectedJavaVersion)) {
			throw new RuntimeException("The active java version '" + detectedJavaVersion + "' does not match the " +
					"desired java version '" + this.javaVersion + "'!");
		}

		boolean server0Available = testDb(ImmutableServerProfiles.SERVER_0);
		
		if (TestConfiguration.getInstance().dbEnabled) {
			if (server0Available) {
				dbAvailable = true;
			} else {
				// If not available and the host is not defined, assume Unavailable is ok
				if (server0DBHost == null) {
					dbAvailable = false;
				} else {
					// Otherwise, it should have been available, so throw an exception
					throw new RuntimeException("Cannot connect to the defined postgresql server '" + this.server0DBHost + "'!");
				}
			}
		} else {
			dbAvailable = false;
		}

		if (dbAvailable) {
			// if server0DBHost is available, federation server db paths are set, and they cannot be connected, to,
			// raise an exception. Otherwise assume as expected.
			if (this.server1DBHost != null && !testDb(ImmutableServerProfiles.SERVER_1)) {
				throw new RuntimeException("Cannot connect to the defined postgresql server '" + this.server1DBHost + "'!");
			}
			if (this.server2DBHost != null && !testDb(ImmutableServerProfiles.SERVER_2)) {
				throw new RuntimeException("Cannot connect to the defined postgresql server '" + this.server2DBHost + "'!");
			}
		}
		hasBeenVerified = true;
	}

	public String getTestTags() {
		if (!hasBeenVerified) {
			throw new RuntimeException("The test configuration must be verified before it can be used!");
		}
		StringBuilder sb = new StringBuilder();

		sb.append("[db");

		if (dbAvailable) {
			sb.append("Available");
		} else {
			sb.append("Unavailable");
		}

		if (dbEnabled) {
			sb.append("Enabled");
		} else {
			sb.append("Disabled");
		}

		sb.append("]");

		if (javaVersion != null) {
			sb.append("[Java").append(javaVersion).append("]");
		}

		return sb.toString();
	}

	public Class<?> classFromSimpleName(@NotNull String testName) throws ClassNotFoundException {
		if (testName.contains("]")) {
			testName = testName.substring(testName.lastIndexOf("]") + 1);
		}

		try {
			return TestConfiguration.class.getClassLoader().loadClass(testName);
		} catch (ClassNotFoundException e) {
			return TestConfiguration.class.getClassLoader().loadClass(GENERAL_TEST_PKG + testName);
		}
	}

	public Class<?> methodClassFromSimpleName(@NotNull String testName) throws ClassNotFoundException {
		if (testName.contains("]")) {
			testName = testName.substring(testName.lastIndexOf("]") + 1);
		}
		int splitterIndex = testName.lastIndexOf(".");
		String testClass = testName.substring(0, splitterIndex);
		return classFromSimpleName(testClass);
	}

	public Method methodFromSimpleName(@NotNull String testName) throws NoSuchMethodException {
		if (testName.contains("]")) {
			testName = testName.substring(testName.lastIndexOf("]") + 1);
		}
		try {
			int splitterIndex = testName.lastIndexOf(".");
			String taggedClass = testName.substring(0, splitterIndex);
			String methodName = testName.substring(splitterIndex + 1);
			return classFromSimpleName(taggedClass).getMethod(methodName);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public String toSimpleName(@NotNull Class<?> testClass, boolean includeTags) {
		if (includeTags) {
			return testClass.getCanonicalName().replace(GENERAL_TEST_PKG, getTestTags());
		} else {
			return testClass.getCanonicalName().replace(GENERAL_TEST_PKG, "");
		}
	}

	public String toSimpleName(@NotNull Class<?> testClass, @NotNull Method method, boolean includeTags) {
		return toSimpleName(testClass, includeTags) + "." + method.getName();
	}

	public String toFormalTaggedName(@NotNull Class<?> clazz) {
		return getTestTags() + clazz.getCanonicalName();
	}

	public String toFormalTaggedName(@NotNull Method method) {
		return toFormalTaggedName(method.getDeclaringClass()) + "." + method.getName();
	}

	@Nullable
	public String getDbHost(AbstractServerProfile profile) {
		AbstractServerProfile baseProfile = null;
		if (profile instanceof MutableServerProfile) {
			baseProfile = ((MutableServerProfile)profile).baseProfile;
		}

		String dbHost = null;
		if (profile == ImmutableServerProfiles.SERVER_0 ||
				baseProfile != null && baseProfile == ImmutableServerProfiles.SERVER_0) {
			if (server0DBHost != null) {
				dbHost = server0DBHost;
			}
		} else if (profile == ImmutableServerProfiles.SERVER_1 ||
				baseProfile != null && baseProfile == ImmutableServerProfiles.SERVER_1) {
			if (server1DBHost != null) {
				dbHost = server1DBHost;
			}
		} else if (profile == ImmutableServerProfiles.SERVER_2 ||
				baseProfile != null && baseProfile == ImmutableServerProfiles.SERVER_2) {
			if (server2DBHost != null) {
				dbHost = server2DBHost;
			}
		} else {
			throw new RuntimeException("Could not find a database for server profile '" + profile + "'!");
		}
		return dbHost;
	}

	public void configureDatabase(AbstractServerProfile profile) {
		if (!hasBeenVerified) {
			throw new RuntimeException("The test configuration must be verified before it can be used!");
		}
		String dbHost = getDbHost(profile);

		// If the DB Host is set add the credentials
		if (dbHost != null) {
			try {
				Path dbToolPath = Paths.get(profile.getServerPath()).resolve("db-utils").toAbsolutePath();
				ProcessBuilder smProcessBuilder =
						new ProcessBuilder("java", "-jar", dbToolPath.resolve("SchemaManager.jar").toString(),
								"-url", "jdbc:postgresql://" + profile.getDbHost() + ":5432/cot",
								"-user", "martiuser", "-password", profile.getDbPassword(), "upgrade");
				smProcessBuilder.directory(dbToolPath.toFile());
				smProcessBuilder.start().waitFor();
			} catch (IOException | InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
