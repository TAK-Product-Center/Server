package com.bbn.marti.takcl;

import com.bbn.marti.config.Configuration;
import com.bbn.marti.takcl.AppModules.TAKCLConfigModule;
import com.bbn.marti.xml.bindings.UserAuthenticationFile;
import com.google.common.base.Strings;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created on 11/2/15.
 */
public class Util {

	private static final Logger logger = TAKCLogging.getLogger(Util.class);

	private static final String DEFAULT_SERVER = "127.0.0.1";

	private static final Random random = new Random();

	/**
	 * Parses an XML resource stream to the specified packageName.  It assumes the packageName is valid and that is has been
	 * generated from the schema
	 *
	 * @param xmlInputStream The input stream for the XML data to load
	 * @param packageName    The getConsistentUniqueReadableIdentifier of the package that corresponds to the contents of the file
	 * @param <T>            The type corresponding to the packagename
	 * @return The object of type {@link T} if the file exists, null if it does not
	 * @throws FileNotFoundException If the file was not found
	 * @throws JAXBException         If a parsing exception occurs
	 */
	public static <T> T loadJAXifiedXML(InputStream xmlInputStream, String packageName) throws FileNotFoundException, JAXBException {
		JAXBContext jc = JAXBContext.newInstance(packageName);
		Unmarshaller u = jc.createUnmarshaller();
		return (T) u.unmarshal(xmlInputStream);
	}


	/**
	 * Parses an XML file to the specified packageName.  It assumes the packageName is valid and that is has been
	 * generated from the schema
	 *
	 * @param xmlFile     The file to open
	 * @param packageName The getConsistentUniqueReadableIdentifier of the package that corresponds to the contents of the file
	 * @param <T>         The type corresponding to the packagename
	 * @return The object of type {@link T} if the file exists, null if it does not
	 * @throws FileNotFoundException If the file was not found
	 * @throws JAXBException         If a parsing exception occurs
	 */
	public static <T> T loadJAXifiedXML(String xmlFile, String packageName) throws FileNotFoundException, JAXBException {
		File f = new File(xmlFile);
		InputStream is = new FileInputStream(f);
		return loadJAXifiedXML(is, packageName);
	}


	/**
	 * Saves The object created with JAXB to an xml file
	 *
	 * @param xmlFile              The target file
	 * @param obj                  THe object to serialize
	 * @param createNewIfNecessary If true, a new file will be created if one does not exist
	 * @throws IOException   If an IOException occurs. May result in a mangled file
	 * @throws JAXBException If a JAXBException occurs. Will not result in a mangled file
	 */
	public static void saveJAXifiedObject(String xmlFile, Object obj, boolean createNewIfNecessary) throws IOException, JAXBException {
		// First write it into a ByteArrayOutputStream so JAXBExceptions don't result in a mangled original file
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		String packageName = obj.getClass().getPackage().getName();
		JAXBContext jc = JAXBContext.newInstance(packageName);
		Marshaller m = jc.createMarshaller();
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		m.marshal(obj, baos);

		// Yay, not JAXBException. Now write to the file
		File file = new File(xmlFile);

		// Create a new file if necessary and allowed
		if (!file.exists() && createNewIfNecessary) {
			file.createNewFile();
		}

		// Write to the file
		OutputStream fos = new FileOutputStream(file);
		baos.writeTo(fos);
		fos.flush();
		fos.close();
	}


	@NotNull
	public static String getBestHost(@Nullable String potentialHost) {
		if (potentialHost != null) {
			return potentialHost;

		} else {
			String confAddress = TAKCLConfigModule.getInstance().getClientSendToAddress();

			if (confAddress != null) {
				return confAddress;
			} else {
				return DEFAULT_SERVER;
			}
		}
	}


	public static String joinString(String[] stringArray, String delimiter) {
		if (stringArray != null && stringArray.length > 0) {
			StringBuilder sb = new StringBuilder();

			boolean firstAdd = true;

			for (String str : stringArray) {
				if (firstAdd) {
					firstAdd = false;
				} else {
					sb.append(delimiter);
				}
				sb.append(str);
			}
			return sb.toString();
		} else {
			return "";
		}
	}

	private static String joinString(Collection<String> stringCollection, String delimiter) {
		return joinString(stringCollection.toArray(new String[stringCollection.size()]), delimiter);
	}

	private static String padString(String str, int length) {
		return String.format("%1$-" + length + "s", str);
	}

	public static String displayableTable(Map<String, Set<String>> source, String subjectLabel, String objectLabel) {
		StringBuilder sb = new StringBuilder();

		int subjMaxLen = subjectLabel.length();

		for (String val : source.keySet()) {
			subjMaxLen = Math.max(subjMaxLen, val.length());
		}

		subjMaxLen += 4;

		sb.append(padString(subjectLabel, subjMaxLen)).append(objectLabel).append("\n");

		for (String key : source.keySet()) {
			sb.append(padString(key, subjMaxLen));

			Collection<String> val = source.get(key);

			if (val != null && val.size() > 0) {
				sb.append("\"").append(joinString(val, "\", \"")).append("\"\n");
			}
		}
		return sb.toString();
	}

	public static String getUserDisplayString(UserAuthenticationFile.User user) {
		boolean showAll = TAKCLCore.TEST_MODE;
		StringBuilder sb = new StringBuilder();

		sb.append("\tUsername:      \'").append(user.getIdentifier()).append("\'\n");
		sb.append("\tRole:          ").append(user.getRole().value()).append("\n");
		
		if (!Strings.isNullOrEmpty(user.getFingerprint())) {
			sb.append("\tFingerprint:   ").append(user.getFingerprint()).append("\n");
		}
		
		if (!user.getGroupList().isEmpty()) {
			
			sb.append("\tGroups (read and write permission):        \n");
			if (showAll) {
				sb.append("\t");
			}
			
			for (String group : user.getGroupList()) {
				if (showAll) {
					sb.append(group).append(", ");

				} else {
					sb.append("\t\t").append(group).append("\n");
				}
			}
		}
		
		if (!user.getGroupListIN().isEmpty()) {
			sb.append("\tGroups IN (write permission):        \n");
			if (showAll) {
				sb.append("\t");
			}
			for (String group : user.getGroupListIN()) {
				if (showAll) {
					sb.append(group).append(", ");

				} else {
					sb.append("\t\t").append(group).append("\n");
				}
			}
		}
		
		if (!user.getGroupListOUT().isEmpty()) {
			sb.append("\tGroups OUT (read permission):        \n");
			if (showAll) {
				sb.append("\t");
			}
			for (String group : user.getGroupListOUT()) {
				if (showAll) {
					sb.append(group).append(", ");

				} else {
					sb.append("\t\t").append(group).append("\n");
				}
			}
		}
		
		if (user.getGroupList().isEmpty() && user.getGroupListIN().isEmpty() && user.getGroupListOUT().isEmpty()) {
			sb.append("\t\tNo groups specified, user will be assigned __ANON__ group by default\n");
		}
		
		return sb.toString();
	}

	public static Path getBuildRoot() {
		try {
			Path currentPath = Paths.get(Util.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();

			List<String> expectedEntries = Arrays.asList("gradlew", "takserver-common", "federation-common", "takserver-core", "takserver-takcl-core", "takserver-usermanager");

			do {
				boolean allFilesExist = true;
				for (String entry : expectedEntries) {
					if (!Files.exists(currentPath.resolve(entry))) {
						allFilesExist = false;
						break;
					}
				}

				if (allFilesExist) {
					return currentPath;
				} else {
					currentPath = currentPath.getParent();
				}
			} while (currentPath != null);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}

		throw new RuntimeException("Could not find TAKServer source root!");
	}

	@Nullable
	public static Path getCoreConfigPath() {
		try {
			if (TAKCLCore.coreConfigPath != null) {
				Path ccPath = Paths.get(TAKCLCore.coreConfigPath).toAbsolutePath();
				logger.debug("Reading CoreConfig from system property with value '" + ccPath.toAbsolutePath().toString() + "'.");
				return ccPath;
			}

			Path jarPath = Paths.get(Util.class.getProtectionDomain().getCodeSource().getLocation()
					.toURI()).getParent();
			if (Files.exists(jarPath.resolve("CoreConfig.xml"))) {
				Path ccPath = jarPath.resolve("CoreConfig.xml").toAbsolutePath();
				logger.debug("Reading CoreConfig from '" + ccPath.toAbsolutePath().toString() + "'.");
				return ccPath;

			} else if (Files.exists(jarPath.getParent().resolve("CoreConfig.xml"))) {
				Path ccPath = jarPath.getParent().resolve("CoreConfig.xml").toAbsolutePath();
				logger.debug("Reading CoreConfig from '" + ccPath.toAbsolutePath().toString() + "'.");
				return ccPath;
			} else if (Files.exists(Paths.get(".").resolve("CoreConfig.xml"))) {
				Path ccPath = Paths.get("CoreConfig.xml").toAbsolutePath();
				logger.debug("Reading CoreConfig from '" + ccPath.toAbsolutePath().toString() + "'.");
				return ccPath;
			} else {
				logger.debug("No CoreConfig file found.");
			}
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
		return null;
	}

	@Nullable
	public static Configuration getCoreConfig() {
		try {
			Path ccPath = getCoreConfigPath();
			if (ccPath != null) {
				return Util.loadJAXifiedXML(ccPath.toString(), Configuration.class.getPackage().getName());
			} else {
				return null;
			}
		} catch (IOException | JAXBException e) {
			throw new RuntimeException(e);
		}
	}

	public static byte[] generateDummyData(int maxLen) {
		int dataLength = 0;
		while (dataLength <= 0) {
			dataLength = random.nextInt(maxLen);
		}
		byte[] data = new byte[dataLength];
		random.nextBytes(data);
		return data;
	}

}
