package com.bbn.marti.takcl.AppModules;

import com.bbn.marti.takcl.AppModules.generic.AppModuleInterface;
import com.bbn.marti.takcl.TAKCLCore;
import com.bbn.marti.takcl.Util;
import com.bbn.marti.takcl.cli.simple.Command;
import com.bbn.marti.takcl.config.TAKCLConfiguration;
import com.bbn.marti.takcl.config.common.ConnectableTAKServerConfig;
import com.bbn.marti.takcl.config.common.RunnableTAKServerConfig;
import com.bbn.marti.takcl.config.common.TAKCLTestSourceGenerationConfig;
import com.bbn.marti.takcl.config.common.TakclRunMode;
import com.bbn.marti.takcl.taka.config.TAKAConfiguration;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Used to manipulate the configruation of TAKCL.
 *
 * @command
 */
public class TAKCLConfigModule implements AppModuleInterface {

	private static final String SYSTEM_PROPERTY_XML_CONFIG = "com.bbn.marti.takcl.config.filepath";

	private static TAKCLConfigModule configurationModule;

	private FileRouter fileRouter;

	private boolean isInitialized = false;


	@NotNull
	@Override
	public TakclRunMode[] getRunModes() {
		return new TakclRunMode[0];
	}

	@Override
	public ServerState getRequiredServerState() {
		return ServerState.NOT_APPLICABLE;
	}

	@Override
	public String getCommandDescription() {
		return "Modifies parameters used by TAKCL.";
	}

	@Override
	public void init() {
		fileRouter = new FileRouter();
		isInitialized = true;
	}

	public void saveChanges() {
		fileRouter.saveChanges();
	}

	private TAKCLConfigModule() {

	}

	public static TAKCLConfigModule buildInstance() {
		if (configurationModule == null) {
			configurationModule = new TAKCLConfigModule();
			configurationModule.init();
		}
		return configurationModule;
	}

	public static TAKCLConfigModule getInstance() {
		if (configurationModule == null) {
			configurationModule = new TAKCLConfigModule();
			configurationModule.init();
		}
		return configurationModule;
	}


	@Command(description = "Gets the current location of the server and CoreConfig.xml.")
	public String getTakServerPath() {
		String str = fileRouter.getAbsoluteFilePath(fileRouter.getRunnableServerConfig().getModelServerDir(), true);
		return str + (str.endsWith("/") ? "" : "/");
	}

	public String getTakServerPath(@NotNull String serverIdentifier) {
		String str = Paths.get(getServerFarmDir(), serverIdentifier).toString();
		return str + (str.endsWith("/") ? "" : "/");
	}

	public String getTakJarFilepath() {
		return Paths.get(getTakServerPath(), fileRouter.getRunnableServerConfig().getJarName()).toString();
	}

	public String getTakJarFilepath(@NotNull String serverIdentifier) {
		return Paths.get(getTakServerPath(), fileRouter.getRunnableServerConfig().getJarName()).toString();
	}

	public String getTakJarFilename() {
		return fileRouter.getRunnableServerConfig().getJarName();
	}

	/**
	 * Gets the address all connections from TAKCL are directed towards
	 *
	 * @return The current address being used
	 * @command
	 */
	@Command
	public String getClientSendToAddress() {
		return fileRouter.getConnectableServerConfig().getUrl();
	}

	public String getCleanConfigFilepath() {
		return Paths.get(getTakServerPath(), fileRouter.getRunnableServerConfig().getCleanConfigFile()).toString();
	}

	/**
	 * Sets the location the the server. This is used to indicate where the CoreConfig.xml is and other related files
	 * modified by TAKCL
	 *
	 * @param path The path to the server
	 * @command
	 */
	@Command(description = "Sets the location of the server and CoreConfig.xml.")
	public void setTAKServerPath(String path) {
        fileRouter.getRunnableServerConfig().setModelServerDir(path);
		saveChanges();
	}


	/**
	 * Gets the path to use for the generated user and connection files
	 *
	 * @return The path to use
	 */
	public String getJavaGenerationPath() {
		String genPath = fileRouter.getSourceModificationConfig().getJavaGenerationPackage();
		genPath = genPath.replaceAll("\\.", "/");
		if (!genPath.endsWith("/")) {
			genPath += "/";
		}

		String srcPath = fileRouter.getSourceModificationConfig().getJavaSrcDir();
		if (!srcPath.endsWith("/")) {
			srcPath += "/";
		}

		return fileRouter.getAbsoluteFilePath(srcPath + genPath, true);
	}

	/**
	 * Gets the path that contains the template classes to generate users and connections from using the generation utility
	 *
	 * @return The path to the templates
	 */
	public String getJavaTemplatePath() {
		String genPath = fileRouter.getSourceModificationConfig().getJavaTemplatePackage().replaceAll("\\.", "/");
		if (!genPath.endsWith("/")) {
			genPath += "/";
		}

		String srcPath = fileRouter.getSourceModificationConfig().getJavaSrcDir();
		if (!srcPath.endsWith("/")) {
			srcPath += "/";
		}

		return fileRouter.getAbsoluteFilePath(srcPath + genPath, true);
	}

	public String getJavaTemplatePackage() {
		return fileRouter.getSourceModificationConfig().getJavaTemplatePackage();
	}

	public String getJavaGenerationPackage() {
		return fileRouter.getSourceModificationConfig().getJavaGenerationPackage();
	}

	/**
	 * Sets the address all connections from TAKCL are directed towards
	 *
	 * @param address The address connections should be directed towards
	 * @command
	 */
	@Command
	public void setClientSendToAddress(String address) {
		fileRouter.getConnectableServerConfig().setUrl(address);
		saveChanges();
	}

	public void setServerFarmDir(@NotNull String serverFarmDir) {
		fileRouter.getRunnableServerConfig().setServerFarmDir(serverFarmDir);
		saveChanges();
	}

	public String getServerConfigFilepath() {
		return Paths.get(getTakServerPath(), fileRouter.getRunnableServerConfig().getConfigFile()).toString();
	}

	public String getServerConfigFilepath(@NotNull String serverIdentifier) {
		return Paths.get(getTakServerPath(serverIdentifier), fileRouter.getRunnableServerConfig().getConfigFile()).toString();
	}

	public String getServerUserFile() {
		return Paths.get(getTakServerPath(), fileRouter.getRunnableServerConfig().getUserFile()).toString();
	}

	public String getServerUserFile(@NotNull String serverIdentifier) {
		return Paths.get(getTakServerPath(serverIdentifier), fileRouter.getRunnableServerConfig().getUserFile()).toString();
	}

	public String getServerFarmDir() {
		String str = fileRouter.getAbsoluteFilePath(fileRouter.getRunnableServerConfig().getServerFarmDir(), false);
		return str + (str.endsWith("/") ? "" : "/");
	}

	public String getClientKeystoreP12Filepath() {
		return fileRouter.getAbsoluteFilePath(fileRouter.getConnectableServerConfig().getClientKeystoreP12Filepath(), true);
	}

	public String getClientKeystorePass() {
		return fileRouter.getConnectableServerConfig().getClientKeystorePass();
	}

	public String getTruststoreJKSFilepath() {
		return fileRouter.getAbsoluteFilePath(fileRouter.getConnectableServerConfig().getTruststoreJKSFilepath(), true);
	}

	public String getTruststorePass() {
		return fileRouter.getConnectableServerConfig().getTruststorePass();
	}


	public Path getCertificateDir() {
		return Paths.get(fileRouter.getAbsoluteFilePath(fileRouter.getRunnableServerConfig().getCertificateDirectory(), false));
	}

	public Path getCertificateToolDir() {
		return Paths.get(fileRouter.getAbsoluteFilePath(fileRouter.getRunnableServerConfig().getCertToolDirectory(), true));
	}

	public String getCertificateFilepath(@NotNull String serverIdentifier, @NotNull String filetype) {
		return fileRouter.getAbsoluteFilePath(fileRouter.getRunnableServerConfig().getCertificateDirectory() + serverIdentifier + '.' + filetype, true);
	}

	@Nullable
	public String getTemporaryDirectory() {
		return fileRouter.getTemporaryDirectory();
	}

	public String getTestArtifactDirectory() {
		Path artifacts = Paths.get(fileRouter.getTemporaryDirectory(), "TEST_ARTIFACTS").toAbsolutePath();
		if (!Files.exists(artifacts)) {
			try {
				Files.createDirectory(artifacts);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return artifacts.toString();
	}

	private class FileRouter {
		private static final String DEFAULT_LOCAL_CONFIG_FILE_LOCATION = "TAKCLConfig_local.xml";
		private static final String DEFAULT_TAKA_CONFIG_FILE_LOCATION = "TAKAConfig.xml";
		private static final String DEFAULT_CONFIG_FILE_LOCATION = "TAKCLConfig.xml";

		private final String homeDirectory;

		private final String configFilepath;


		private TAKCLConfiguration sysPropTakclConfiguration;
		private TAKCLConfiguration takclConfiguration;

		private TAKAConfiguration takaConfiguration;

		private final String temporaryDirectory;
		private final String localPath;
		private final String workingPath;

		public FileRouter() {

			// Get the local filepath and working filepath
			workingPath = Paths.get(".").toAbsolutePath().normalize().toString();

			homeDirectory = System.getProperty("user.home");

			// Get the path relative to the executable class or jar file
			URL resourceLocation = FileRouter.class.getClassLoader().getResource("com/bbn/marti/takcl/AppModules/TAKCLConfigModule.class");

			if (resourceLocation != null) {
				String path = resourceLocation.getPath();

				if (path.startsWith("file:") && path.endsWith(".jar!/com/bbn/marti/takcl/AppModules/TAKCLConfigModule.class")) {
					path = path.substring(5).replace(".jar!/com/bbn/marti/takcl/AppModules/TAKCLConfigModule.class", "");
					path = path.substring(0, path.lastIndexOf("/") + 1);

//				} else if (path.endsWith("build/classes/java/core/com/bbn/marti/takcl/AppModules/TAKCLConfigModule.class")) {
//					path = path.replace("build/classes/java/core/com/bbn/marti/takcl/AppModules/TAKCLConfigModule.class", "");
//
				} else if (path.endsWith("com/bbn/marti/takcl/AppModules/TAKCLConfigModule.class")) {
					path = path.replace("com/bbn/marti/takcl/AppModules/TAKCLConfigModule.class", "");

				} else {
					throw new RuntimeException("Unable to determine path of files located with the jar!");
				}

				localPath = path;

			} else {
				throw new RuntimeException("Unable to determine path of files located with the jar!");
			}

			// Load the configuration file
			Path configFilepath;
			try {

				String sysPropConfigFilepath = TAKCLCore.TakclOption.TakclConfigPath.getStringOrNull();
				if (sysPropConfigFilepath != null) {
					sysPropTakclConfiguration = Util.loadJAXifiedXML(sysPropConfigFilepath, TAKCLConfiguration.class.getPackage().getName());
				} else {
					sysPropTakclConfiguration = null;
				}

				if (Files.exists(configFilepath = Paths.get(workingPath, DEFAULT_LOCAL_CONFIG_FILE_LOCATION))) {
					takclConfiguration = Util.loadJAXifiedXML(configFilepath.toString(), TAKCLConfiguration.class.getPackage().getName());

				} else if (Files.exists(configFilepath = Paths.get(localPath, DEFAULT_LOCAL_CONFIG_FILE_LOCATION))) {
					takclConfiguration = Util.loadJAXifiedXML(configFilepath.toString(), TAKCLConfiguration.class.getPackage().getName());

				} else if (Files.exists(configFilepath = Paths.get(workingPath, DEFAULT_TAKA_CONFIG_FILE_LOCATION))) {
					takaConfiguration = Util.loadJAXifiedXML(configFilepath.toString(), TAKAConfiguration.class.getPackage().getName());

				} else if (Files.exists(configFilepath = Paths.get(workingPath, DEFAULT_CONFIG_FILE_LOCATION))) {
					takclConfiguration = Util.loadJAXifiedXML(configFilepath.toString(), TAKCLConfiguration.class.getPackage().getName());

				} else if (Files.exists(configFilepath = Paths.get(localPath, DEFAULT_TAKA_CONFIG_FILE_LOCATION))) {
					takaConfiguration = Util.loadJAXifiedXML(configFilepath.toString(), TAKAConfiguration.class.getPackage().getName());

				} else if (Files.exists(configFilepath = Paths.get(localPath, DEFAULT_CONFIG_FILE_LOCATION))) {
					takclConfiguration = Util.loadJAXifiedXML(configFilepath.toString(), TAKCLConfiguration.class.getPackage().getName());
				} else {
					InputStream takclConfigStream = TAKCLConfigModule.class.getClassLoader().getResourceAsStream("TAKCLConfig.xml");
					if (takclConfigStream != null) {
						takclConfiguration = Util.loadJAXifiedXML(takclConfigStream, TAKCLConfiguration.class.getPackage().getName());
					}
				}

				this.configFilepath = configFilepath.toAbsolutePath().toString();

			} catch (JAXBException | IOException e) {
				throw new RuntimeException(e);
			}

			String tempDirectory;
			String fallbackTempDirectory;

			if (takaConfiguration != null) {
				tempDirectory = getAbsoluteFilePath(takaConfiguration.getTemporaryDirectory(), false);
				fallbackTempDirectory = getAbsoluteFilePath(takaConfiguration.getFallbackTemporaryDirectory(), false);

			} else if (takclConfiguration != null) {
				tempDirectory = getAbsoluteFilePath(takclConfiguration.getTemporaryDirectory(), false);
				fallbackTempDirectory = getAbsoluteFilePath(takclConfiguration.getFallbackTemporaryDirectory(), false);

			} else if (sysPropTakclConfiguration != null) {
				tempDirectory = getAbsoluteFilePath(sysPropTakclConfiguration.getTemporaryDirectory(), false);
				fallbackTempDirectory = getAbsoluteFilePath(sysPropTakclConfiguration.getFallbackTemporaryDirectory(), false);

			} else {
				throw new RuntimeException("No tmp directory is defined in TAKCLConfig, TAKAConfig, or the system property '" + SYSTEM_PROPERTY_XML_CONFIG + "'!");
			}

			if (new File(tempDirectory).exists()) {
				this.temporaryDirectory = tempDirectory;

			} else {
				if (fallbackTempDirectory == null || "".equals(fallbackTempDirectory)) {
					throw new RuntimeException("The temp directory defined in TAKCLConfig, TAKAConfig, or the system " +
							"property 'com.bbn.marti.takcl.tmpDir' does not exist and no fallbackTempDirectory is set!");
				}

				System.out.println("Temporary directory is not set. Falling back to '" + fallbackTempDirectory + "'.");

				if (!new File(fallbackTempDirectory).isDirectory()) {
					try {
						Files.createDirectory(Paths.get(fallbackTempDirectory));
						this.temporaryDirectory = fallbackTempDirectory;
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				} else {
					this.temporaryDirectory = fallbackTempDirectory;
				}
			}

			RunnableTAKServerConfig rsc =
					sysPropTakclConfiguration != null ? sysPropTakclConfiguration.getRunnableTAKServerConfig() :
							takaConfiguration != null ? takaConfiguration.getRunnableTAKServerConfig() :
									takclConfiguration != null ? takclConfiguration.getRunnableTAKServerConfig() : null;


			System.out.println(
					"Configuration File: " + configFilepath
							+ (rsc != null ? "\nRunnable Server Model: " + rsc.getModelServerDir() : ""));
		}

		private ConnectableTAKServerConfig getConnectableServerConfig() {
			if (sysPropTakclConfiguration != null && sysPropTakclConfiguration.getConnectableTAKServerConfig() != null) {
				return sysPropTakclConfiguration.getConnectableTAKServerConfig();
			} else if (takaConfiguration != null && takaConfiguration.getConnectableTAKServerConfig() != null) {
				return takaConfiguration.getConnectableTAKServerConfig();
			} else if (takclConfiguration != null && takclConfiguration.getConnectableTAKServerConfig() != null) {
				return takclConfiguration.getConnectableTAKServerConfig();
			} else {
				throw new RuntimeException("No connectable server is defined in TAKCLConfig or TAKAConfig!");
			}
		}

		private RunnableTAKServerConfig getRunnableServerConfig() {
			if (sysPropTakclConfiguration != null && sysPropTakclConfiguration.getRunnableTAKServerConfig() != null) {
				return sysPropTakclConfiguration.getRunnableTAKServerConfig();
			} else if (takaConfiguration != null && takaConfiguration.getRunnableTAKServerConfig() != null) {
				return takaConfiguration.getRunnableTAKServerConfig();
			} else if (takclConfiguration != null && takclConfiguration.getRunnableTAKServerConfig() != null) {
				return takclConfiguration.getRunnableTAKServerConfig();
			} else {
				throw new RuntimeException("No runnable server is defined in TAKCLConfig or TAKAConfig!");
			}
		}

		private TAKCLTestSourceGenerationConfig getSourceModificationConfig() {
			if (sysPropTakclConfiguration != null && sysPropTakclConfiguration.getTAKCLTestSourceGenerationConfig() != null) {
				return sysPropTakclConfiguration.getTAKCLTestSourceGenerationConfig();
			} else if (takclConfiguration != null && takclConfiguration.getTAKCLTestSourceGenerationConfig() != null) {
				return takclConfiguration.getTAKCLTestSourceGenerationConfig();
			} else {
				throw new RuntimeException("No source modification details are defined in TAKCLConfig!");
			}
		}

		public String getTemporaryDirectory() {
			return temporaryDirectory;
		}

		private String getAbsoluteFilePath(@NotNull String filename, boolean mustExist) {

			// Determine the temporary directory and create it if necessary
			String homeDir = System.getProperty("user.home");

			if (filename.startsWith("${HOME}")) {
				filename = filename.replace("${HOME}", homeDir);
			} else if (filename.startsWith("$HOME")) {
				filename = filename.replace("$HOME", homeDir);
			} else if (filename.startsWith("~/")) {
				filename = filename.replace("~/", homeDir);
			} else if (filename.startsWith("${TAKCL_TMP}")) {
				filename = filename.replace("${TAKCL_TMP}", temporaryDirectory);
			} else if (filename.startsWith("$TAKCL_TMP")) {
				filename = filename.replace("TAKCL_TMP", temporaryDirectory);
			} else if (filename.startsWith("${TAKSERVER_BUILD_ROOT}")) {
				filename = filename.replace("${TAKSERVER_BUILD_ROOT}", Util.getBuildRoot().toString());
			} else if (filename.startsWith("$TAKSERVER_BUILD_ROOT")) {
				filename = filename.replace("$TAKSERVER_BUILD_ROOT", Util.getBuildRoot().toString());
			}

			if (Paths.get(filename).isAbsolute()) {
				return filename;
			}

			if (!mustExist || Files.exists(Paths.get(workingPath, filename))) {
				return Paths.get(workingPath, filename).toString();
			}

			if (Files.exists(Paths.get(localPath, filename))) {
				return Paths.get(localPath, filename).toString();

			} else {
				try {
					InputStream inputStream = FileRouter.class.getClassLoader().getResourceAsStream(filename);

					String filePath = Paths.get(temporaryDirectory, filename).toString();
					FileUtils.copyInputStreamToFile(inputStream, new File(filePath));
					return filePath;

				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}

		public void saveChanges() {
			try {
				if (takclConfiguration != null) {
					Util.saveJAXifiedObject(configFilepath, takclConfiguration, true);
				}

				if (takaConfiguration != null) {
					Util.saveJAXifiedObject(configFilepath, takaConfiguration, true);
				}
			} catch (IOException | JAXBException e) {
				throw new RuntimeException(e);
			}
		}

	}

}
