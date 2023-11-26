package com.bbn.marti.takcl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created on 1/15/16.
 */
public class TAKCLCore {

	public static final PrintStream defaultStdout = System.out;
	public static final PrintStream defaultStderr = System.err;

	public static final String CORE_NETWORK_V1_TAG = "coreNetworkV1-";
	public static final String CORE_NETWORK_V2_TAG = "coreNetworkV2-";

	public static final PrintStream stdout = System.out;
	public static final PrintStream stderr = System.err;

	public static boolean TEST_MODE = true;
	private static final String SYSARG_PREFIX = "com.bbn.marti.takcl.";
	public static final double sleepMultiplier;
	public static final Long igniteNetworkTimeout;
	@Nullable
	public static final Long igniteClientFailureDetectionTimeout;
	@Nullable
	public static final Long igniteFailureDetectionTimeout;
	@Nullable
	public static final Integer ignitePortOverride;
	@Nullable
	public static final Integer ignitePortRangeOverride;
	@Nullable
	public static final String igniteIpAddressOverride;
	public static final long userManagerTimeout;
	public static final boolean igniteScanInterfaces;
	@Nullable
	public static final String coreConfigPath;
	public static final boolean cliIgnoreCoreConfig;
	public static final long igniteManualRetryTimeout;
	public static boolean useTakclIgniteConfig;
	public static final boolean disableMessagingProcess;
	public static final boolean disableApiProcess;

	public static final boolean disableRetentionProcess;

	public static final boolean disablePluginManagerProcess;
	
	public static final boolean disableFederationHubProcess;
	@Nullable
	public static final Path testCertSourceDir;
	public static final boolean keepServersRunning;
	public static final boolean printRestDetails;
	@Nullable
	public static final Integer serverStartupWaitTime;
	@Nullable
	public static final String postgresPassword;

	@Nullable
	public static final String serverLogLevelOverrides;

	@Nullable
	public static final Integer systemMonitorPeriodSeconds;

	public static boolean k8sMode;

	public enum TakclOption {
		// TestConfiguration settings
		EnableDB(SYSARG_PREFIX + "dbEnabled", "TAKCL_ENABLE_DB", "true", true),
		Server0DbHost(SYSARG_PREFIX + "server0DbHost", "TAKCL_SERVER0_DB_HOST", null, false),
		Server1DbHost(SYSARG_PREFIX + "server1DbHost", "TAKCL_SERVER1_DB_HOST", null, false),
		Server2DbHost(SYSARG_PREFIX + "server2DbHost", "TAKCL_SERVER2_DB_HOST", null, false),
		JavaVersion(SYSARG_PREFIX + "javaVersion", "TAKCL_JAVA_VERSION", null, false),

		// General Settings
		SleepMultiplier(SYSARG_PREFIX + "sleepMultiplier", "TAKCL_SLEEP_MULTIPLIER", "1", false),
		IgniteManualRetryTimeout(SYSARG_PREFIX + "igniteManualRetryTimeout", "TAKCL_IGNITE_MANUAL_RETRY_TIMEOUT", "0", false),
		IgniteClientFailureDetectionTimeout(SYSARG_PREFIX + "igniteClientFailureDetectionTimeout", "TAKCL_IGNITE_CLIENT_FAILURE_DETECTION_TIMEOUT", null, false),
		IgniteFailureDetectionTimeout(SYSARG_PREFIX + "igniteFailureDetectionTimeout", "TAKCL_IGNITE_FAILURE_DETECTION_TIMEOUT", null, false),
		IgniteNetworkTimeout(SYSARG_PREFIX + "igniteNetworkTimeout", "TAKCL_IGNITE_NETWORK_TIMEOUT", null, false),
		IgniteScanInterfaces(SYSARG_PREFIX + "igniteScanInterfaces", "TAKCL_IGNITE_SCAN_INTERFACES", "false", true),
		IgnitePortOverride(SYSARG_PREFIX + "ignitePortOverride", "TAKCL_IGNITE_PORT_OVERRIDE", null, false),
		IgnitePortRangeOverride(SYSARG_PREFIX + "ignitePortRangeOverride", "TAKCL_IGNITE_PORT_RANGE_OVERRIDE", null, false),
		IgniteIpAddressOverride(SYSARG_PREFIX + "igniteIpAddressOverride", "TAKCL_IGNITE_IP_ADDRESS_OVERRIDE", null, false),
		CoreConfigPath(SYSARG_PREFIX + "coreConfigPath", "TAKCL_CORECONFIG_PATH", null, false),
		UserManagerTimeout("com.bbn.marti.usermanager.timeout", "USERMANAGER_TIMEOUT", "120000", false),
		CliIgnoreCoreConfig(SYSARG_PREFIX + "ignoreCoreConfig", "TAKCL_CLI_IGNORE_CORE_CONFIG", "false", true),
		TakclConfigPath(SYSARG_PREFIX + "config.filepath", "TAKCL_CONFIG_PATH", null, false),
		// TODO: This is a hack. We should figure out why the tests don't like the ConfigurationHolder from takserver-common
		UseTakclIgniteConfig(SYSARG_PREFIX + "takclIgniteConfig", "TAKCL_IGNITE_CONFIG", "true", true),
		DisableMessagingProcess(SYSARG_PREFIX + "disableMessagingProcess", "TAKCL_DISABLE_MESSAGING_PROCESS", "false", true),
		DisableApiProcess(SYSARG_PREFIX + "disableApiProcess", "TAKCL_DISABLE_API_PROCESS", "false", true),
		DisableFederationHubProcess(SYSARG_PREFIX + "disableFederationHubProcess", "TAKCL_DISABLE_FEDERATION_HUB_PROCESS", "true", true),
		DisableRetentionProcess(SYSARG_PREFIX + "disableRetentionProcess", "TAKCL_DISABLE_RETENTION_PROCESS", "true", true),
		DisablePluginManagerProcess(SYSARG_PREFIX + "disablePluginManagerProcess", "TAKCL_DISABLE_PLUGIN_MANAGER_PROCESS", "true", true),
		TestCertSourceDir(SYSARG_PREFIX + "testCertSourceDir", "TAKCL_TEST_CERT_SRC_DIR", null, false),
		KeepServersRunning(SYSARG_PREFIX + "keepServersRunning", "TAKCL_KEEP_SERVERS_RUNNING", "false", true),
		PrintRestDetails(SYSARG_PREFIX + "printRestDetails", "TAKCL_PRINT_REST_DETAILS", "false", true),
		ServerStartupWaitTime(SYSARG_PREFIX + "serverStartupWaitTime", "TAKCL_SERVER_STARTUP_WAIT_TIME", null, false),
		PostgresPassword(SYSARG_PREFIX + "postgresPassword", "TAKCL_SERVER_POSTGRES_PASSWORD", null, false),
		ServerLogLevelOverrides(SYSARG_PREFIX + "serverLogLevelOverrides", "TAKCL_SERVER_LOG_LEVEL_OVERRIDES", null, false),
		SystemMonitorPeriodSeconds(SYSARG_PREFIX + "systemMonitorPeriodSeconds", "TAKCL_SYSTEM_MONITOR_PERIOD_SECONDS", null, false),
		K8SMode(SYSARG_PREFIX + "k8sMode", "TAKCL_K8S_MODE", "false", true);

		public final String sysPropKey;
		public final String value;

		TakclOption(@NotNull String sysPropKey, @NotNull String envVarKey, String defaultValue, boolean isSimpleFlag) {
			this.sysPropKey = sysPropKey;

			if (System.getProperties().containsKey(sysPropKey)) {
				String tmpValue = System.getProperty(sysPropKey);
				if (isSimpleFlag) {
					if (tmpValue == null || tmpValue.equals("") || tmpValue.toLowerCase().equals("true")) {
						this.value = "true";
					} else if (tmpValue.toLowerCase().equals("false")) {
						this.value = "false";
					} else {
						throw new RuntimeException("Invalid value of '" + tmpValue + "' provided for property '" + sysPropKey + "'!");
					}
				} else {
					this.value = System.getProperty(sysPropKey);
				}
			} else if (System.getenv().containsKey(envVarKey)) {

				String tmpValue = System.getenv(envVarKey);
				if (isSimpleFlag) {
					if (tmpValue == null || tmpValue.equals("") || tmpValue.toLowerCase().equals("true")) {
						this.value = "true";
					} else if (tmpValue.toLowerCase().equals("false")) {
						this.value = "false";
					} else {
						throw new RuntimeException("Invalid value of '" + tmpValue + "' provided for environment variable '" + envVarKey + "'!");
					}
				} else {
					this.value = System.getenv(envVarKey);
				}
			} else {
				this.value = defaultValue;
			}
		}

		public String getString() {
			if (value == null) {
				throw new RuntimeException("Invalid null value provided for " + name() + "!");
			}
			return value;
		}

		public String getStringOrNull() {
			return value;
		}

		@Nullable
		public Long getLongOrNull() {
			return value == null ? null : Long.parseLong(value);
		}

		public long getLong() {
			if (value == null) {
				throw new RuntimeException("Invalid null value provided for " + name() + "!");
			}
			return Long.parseLong(value);

		}

		public Boolean getBooleanOrNull() {
			return value == null ? null : Boolean.parseBoolean(value);
		}

		public boolean getBoolean() {
			if (value == null) {
				throw new RuntimeException("Invalid null value provided for " + name() + "!");
			}
			return Boolean.parseBoolean(value);
		}

		public double getDouble() {
			if (value == null) {
				throw new RuntimeException("Invalid null value provided for " + name() + "!");
			}
			return Double.parseDouble(value);
		}

		@Nullable
		public Integer getIntegerOrNull() {
			return value == null ? null : Integer.parseInt(value);
		}

		public Path getPath(boolean mustExist) {
			Path value = Paths.get(getString());
			if (mustExist) {
				if (Files.exists(value)) {
					return value;
				} else {
					throw new RuntimeException("The provided path of '" + value + "' for value '" + name() + "' does not exist!");
				}
			} else {
				return value;
			}
		}

		@Nullable
		public Path getPathOrNull(boolean mustExist) {
			String valueString = getStringOrNull();
			if (valueString == null) {
				return null;
			} else {
				return getPath(mustExist);
			}
		}
	}

	public static void setUseTakclIgniteConfig(boolean value) {
		useTakclIgniteConfig = value;
	}

	public static void printConfigValues() {
		System.out.println("TAKCL CONFIG: ");
		for (TakclOption option : TakclOption.values()) {
			System.out.println(option.sysPropKey + "=" + option.getStringOrNull());
		}
	}

	static {
		sleepMultiplier = TakclOption.SleepMultiplier.getDouble();
		igniteManualRetryTimeout = TakclOption.IgniteManualRetryTimeout.getLong();
		igniteClientFailureDetectionTimeout = TakclOption.IgniteClientFailureDetectionTimeout.getLongOrNull();
		igniteFailureDetectionTimeout = TakclOption.IgniteFailureDetectionTimeout.getLongOrNull();
		igniteNetworkTimeout = TakclOption.IgniteNetworkTimeout.getLongOrNull();
		userManagerTimeout = TakclOption.UserManagerTimeout.getLong();
		igniteScanInterfaces = TakclOption.IgniteScanInterfaces.getBoolean();
		ignitePortOverride = TakclOption.IgnitePortOverride.getIntegerOrNull();
		ignitePortRangeOverride = TakclOption.IgnitePortRangeOverride.getIntegerOrNull();
		igniteIpAddressOverride = TakclOption.IgniteIpAddressOverride.getStringOrNull();
		coreConfigPath = TakclOption.CoreConfigPath.getStringOrNull();
		cliIgnoreCoreConfig = TakclOption.CliIgnoreCoreConfig.getBoolean();
		useTakclIgniteConfig = TakclOption.UseTakclIgniteConfig.getBoolean() && TestExceptions.USE_TAKCL_IGNITE_CONFIGURATION_AS_INDICATED;
		disableMessagingProcess = TakclOption.DisableMessagingProcess.getBoolean();
		disableApiProcess = TakclOption.DisableApiProcess.getBoolean();
		disableFederationHubProcess = TakclOption.DisableFederationHubProcess.getBoolean();
		disablePluginManagerProcess = TakclOption.DisablePluginManagerProcess.getBoolean();
		disableRetentionProcess = TakclOption.DisableRetentionProcess.getBoolean();
		testCertSourceDir = TakclOption.TestCertSourceDir.getPathOrNull(true);
		keepServersRunning = TakclOption.KeepServersRunning.getBoolean();
		printRestDetails = TakclOption.PrintRestDetails.getBoolean();
		serverStartupWaitTime = TakclOption.ServerStartupWaitTime.getIntegerOrNull();
		postgresPassword = TakclOption.PostgresPassword.getStringOrNull();
		serverLogLevelOverrides = TakclOption.ServerLogLevelOverrides.getStringOrNull();
		systemMonitorPeriodSeconds = TakclOption.SystemMonitorPeriodSeconds.getIntegerOrNull();
		k8sMode = TakclOption.K8SMode.getBoolean();
	}
}
