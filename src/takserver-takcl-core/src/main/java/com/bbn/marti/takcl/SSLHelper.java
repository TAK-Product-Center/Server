package com.bbn.marti.takcl;

import com.bbn.marti.remote.exception.TakException;
import com.bbn.marti.takcl.AppModules.TAKCLConfigModule;
import com.bbn.marti.takcl.cli.EndUserReadableException;
import com.bbn.marti.test.shared.data.users.AbstractUser;
import com.bbn.marti.test.shared.data.users.BaseUsers;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.net.ssl.*;
import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created on 11/18/15.
 */
public class SSLHelper {

	private SSLContext sslContext;

	private static final Logger logger = LoggerFactory.getLogger(SSLHelper.class);

	private static SSLHelper sslHelper;

	private static boolean certsGenerated = false;

	private static final List<String> generatedFileExtensions = List.of(
			".csr",
			".jks",
			".key",
			".p12",
			".pem",
			"-trusted.pem"
	);

	private static final List<String> serverCertNames = List.of(
			"SERVER_0",
			"SERVER_1",
			"SERVER_2",
			"SERVER_3"
	);

	private static final List<String> caFiles = List.of(
			"ca.crl",
			"ca-do-not-share.key",
			"ca.pem",
			"ca-trusted.pem",
			"crl_index.txt",
			"crl_index.txt.attr",
			"fed-truststore.jks",
			"root-ca-do-not-share.key",
			"root-ca.pem",
			"root-ca-trusted.pem",
			"truststore-root.jks",
			"truststore-root.p12"
	);

	private static final List<String> clientCertNames = List.of(
			"TAKCL",
			BaseUsers.s0_anonmissionuser.name(),
			BaseUsers.authwssuser.name(),
			BaseUsers.authusert.name(),
			BaseUsers.authwssusert.name(),
			BaseUsers.authwssuser0.name(),
			BaseUsers.authwssuser01.name(),
			BaseUsers.authwssuser2.name(),
			BaseUsers.authwssuser3.name(),
			BaseUsers.authwssuser12.name(),
			BaseUsers.authwssuser012.name(),
			BaseUsers.s0_anonmissionuserA.name(),
			BaseUsers.authwssuserA.name(),
			BaseUsers.authwssusertA.name(),
			BaseUsers.authwssusertB.name(),
			BaseUsers.authwssusertC.name(),
			BaseUsers.authwssuser0A.name(),
			BaseUsers.authwssuser01A.name(),
			BaseUsers.authwssuser2A.name(),
			BaseUsers.authwssuser3A.name(),
			BaseUsers.authwssuser12A.name(),
			BaseUsers.authwssuser012A.name(),
			"user000",
			"user001",
			"user002",
			"user003"
	);

	private static X509TrustManager initTrustManager(String storeType, String password, File trustSoreFile) throws GeneralSecurityException, IOException {
		KeyStore tmks = KeyStore.getInstance(storeType);
		tmks.load(new FileInputStream(trustSoreFile), password.toCharArray());
		TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		trustManagerFactory.init(tmks);
		return (X509TrustManager) trustManagerFactory.getTrustManagers()[0];
	}

	private static SSLContext initSslContext(String storeType, String password, File keyStoreFile, TrustManager trustManager) throws GeneralSecurityException, IOException {
		KeyStore kmks = KeyStore.getInstance(storeType);
		kmks.load(new FileInputStream(keyStoreFile), password.toCharArray());
		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
		keyManagerFactory.init(kmks, password.toCharArray());

		SSLContext theSslContext = SSLContext.getInstance("TLSv1.2");
		theSslContext.init(keyManagerFactory.getKeyManagers(), new TrustManager[]{trustManager}, null);

		return theSslContext;
	}

	private SSLHelper() {

		try {
			TAKCLConfigModule conf = TAKCLConfigModule.getInstance();
			TrustManager trustManager = initTrustManager("PKCS12", conf.getClientKeystorePass(), new File(conf.getTruststoreJKSFilepath()));
			sslContext = initSslContext("PKCS12", conf.getClientKeystorePass(),
					new File(conf.getClientKeystoreP12Filepath()), trustManager);
		} catch (GeneralSecurityException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static SSLHelper getInstance() {
		if (sslHelper == null) {
			sslHelper = new SSLHelper();
		}
		return sslHelper;
	}

	public Socket createSSLSocket() {
		try {
			return sslContext.getSocketFactory().createSocket();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public ServerSocket createSSLServerSocket(int port) {
		try {
			return sslContext.getServerSocketFactory().createServerSocket(port);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static X509Certificate getCertificate(@NotNull String filepath) throws CertificateException, FileNotFoundException {
		InputStream fileInputStream = new FileInputStream(filepath);
		CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
		return (X509Certificate) certificateFactory.generateCertificate(fileInputStream);
	}

	// attempt to get the CN in a robust way
	public static String getCN(String dn) {
		if (dn == null || dn.isEmpty()) {
			throw new IllegalArgumentException("empty DN");
		}

		try {
			LdapName ldapName = new LdapName(dn);

			for (Rdn rdn : ldapName.getRdns()) {
				if (rdn.getType().equalsIgnoreCase("CN")) {

					return rdn.getValue().toString();
				}
			}

			throw new TakException("No CN found in DN: " + dn);

		} catch (InvalidNameException e) {
			throw new TakException(e);
		}
	}

	@Nullable
	public static String getUserFingerprintIfAvailable(@NotNull AbstractUser user) throws CertificateException, FileNotFoundException {
		Path path = user.getCertPublicPemPath();
		if (path == null) {
			return null;
		}
		return getCertificateFingerprint(getCertificate(path.toAbsolutePath().toString()));
	}

	public static String getCertificateFingerprint(@NotNull X509Certificate certificate) throws CertificateException {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			String fingerprint = DatatypeConverter.printHexBinary(md.digest(certificate.getEncoded()));

			StringBuilder fpBuilder = new StringBuilder();

			for (int i = 0; i < fingerprint.length(); i++) {
				if (i > 0 && i % 2 == 0) {
					fpBuilder.append(':');
				}
				fpBuilder.append(Character.toUpperCase(fingerprint.charAt(i)));
			}
			return fpBuilder.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	public static String getCertificateUserName(@NotNull X509Certificate certificate) {
		return getCN(certificate.getSubjectDN().getName());
	}

	public final void copyServerTruststoreJks(@NotNull String targetFilepath) {
		try {
			InputStream certStream = new FileInputStream(TAKCLConfigModule.getInstance().getTruststoreJKSFilepath());
			FileUtils.copyInputStreamToFile(certStream, new File(targetFilepath));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public final void copyServerKeystoreJks(@NotNull String serverIdentifier, @NotNull String targetFilepath) {
		String sourceFilepath = TAKCLConfigModule.getInstance().getCertificateFilepath(serverIdentifier, "jks");
		try {
			Files.copy(Paths.get(sourceFilepath), Paths.get(targetFilepath));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public final String getTruststorePass() {
		return "atakatak";
	}


	public final String getKeystorePass() {
		return "atakatak";
	}

	public final String getServerFingerprint(@NotNull String serverIdentifier) {
		try {
			String sourceFilepath = TAKCLConfigModule.getInstance().getCertificateFilepath(serverIdentifier, "pem");
			X509Certificate certificate = getCertificate(sourceFilepath);
			return getCertificateFingerprint(certificate);
		} catch (CertificateException | FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}


	private static X509Certificate loadCertForEndUser(String certpath) throws EndUserReadableException {
		try {
			return getCertificate(certpath);

		} catch (FileNotFoundException e) {
			throw new EndUserReadableException(
					"Could not open certificate file! Are you sure a certificate exists at '" + certpath + "'?", e);

		} catch (CertificateException e) {
			throw new EndUserReadableException(
					"Could not read the certificate file! Are you sure it is a valid .pem file?", e);
		}
	}

	@Nullable
	public static String loadCertFingerprintForEndUserIfAvailable(@NotNull AbstractUser user) throws EndUserReadableException {
		Path p = user.getCertPublicPemPath();
		if (!Files.exists(p)) {
			return null;
		}
		return loadCertFingerprintForEndUser(p.toAbsolutePath().toString());
	}

	public static String loadCertFingerprintForEndUser(@NotNull String certPath) throws EndUserReadableException {
		X509Certificate certificate = loadCertForEndUser(certPath);
		try {
			return SSLHelper.getCertificateFingerprint(certificate);
		} catch (CertificateException e) {
			throw new EndUserReadableException(
					"Could not read the fingerprint from the certificate file!", e);
		}
	}

	public static String loadCertUsernameForEndUser(@Nullable String certpath) throws EndUserReadableException {
		X509Certificate certificate = loadCertForEndUser(certpath);
		return SSLHelper.getCertificateUserName(certificate);
	}

	private static void produceCerts(@NotNull String certType, @NotNull Path makeCertPath, @Nullable Path sourceCertPath,
	                                 List<String> certNames) throws IOException, InterruptedException {

		for (String certName : certNames) {
			boolean useSource;

			if (sourceCertPath == null) {
				useSource = false;

			} else {
				List<Path> expectedFiles = generatedFileExtensions.stream().map(x -> Paths.get(certName + x)).collect(Collectors.toList());

				useSource = expectedFiles.stream().allMatch(Files::isRegularFile);

				if (useSource) {
					Path targetCertDir = TAKCLConfigModule.getInstance().getCertificateDir();
					for (Path src : expectedFiles) {
						Files.copy(src, targetCertDir.resolve(src.getFileName()));
					}
				}
			}

			if (!useSource) {
				List<String> cmd = Arrays.asList(makeCertPath.toString(), certType, certName);
				File cwd = makeCertPath.getParent().toFile();
				ProcessBuilder pb = new ProcessBuilder(cmd);
				pb.directory(cwd);
				pb.inheritIO();
				pb.environment().put("COUNTRY", "US");
				pb.environment().put("STATE", "MA");
				pb.environment().put("CITY", "Cambridge");
				pb.environment().put("ORGANIZATIONAL_UNIT", "TAKSERVER-TEST");
				Process p = pb.start();
				int exitValue = p.waitFor();
				if (exitValue != 0) {
					throw new RuntimeException("Error generating certs! \n\tCWD: " + cwd + "\n\tCMD: [\n\t" +
							String.join(" \\\n\t", cmd) + "\n]");
				}
			}
		}
	}

	private static void updateCertMetadata(@NotNull Path certMetadataPath, @NotNull Path certPath, @Nullable String organization) throws IOException {
		List<String> outputLines = new LinkedList<>();
		for (String line : Files.readAllLines(certMetadataPath)) {
			if (line.startsWith("#")) {
				continue;
			}

			if (line.startsWith("ORGANIZATION=")) {
				if (organization == null) {
					outputLines.add(line);
				} else {
					outputLines.add("ORGANIZATION=" + organization);
				}
			} else if (line.startsWith("DIR=")) {
				outputLines.add("DIR=" + certPath.toAbsolutePath());

			} else if (!line.equals("echo \"Please edit cert-metadata.sh before running this script!\"; exit -1")) {
				outputLines.add(line);
			}
		}
		Files.write(certMetadataPath, outputLines);
	}

	private static void configureCa() {
		try {
			Path tmpPath = Paths.get(TAKCLConfigModule.getInstance().getTemporaryDirectory());
			Path certToolPath = TAKCLConfigModule.getInstance().getCertificateToolDir();
			Path certTargetPath = TAKCLConfigModule.getInstance().getCertificateDir();

			// Copy cert generation files
			if (!Files.exists(tmpPath.resolve("makeRootCa.sh"))) {
				Files.copy(certToolPath.resolve("makeRootCa.sh"), tmpPath.resolve("makeRootCa.sh"));
			}

			if (!Files.exists(tmpPath.resolve("makeCert.sh"))) {
				Files.copy(certToolPath.resolve("makeCert.sh"), tmpPath.resolve("makeCert.sh"));
			}

			if (!Files.exists(certTargetPath)) {
				Files.createDirectory(certTargetPath);
			}

			Path predefinedCertRoot = TAKCLCore.testCertSourceDir;

			// If predefined cert root set
			if (predefinedCertRoot != null) {
				if (!Files.exists(predefinedCertRoot)) {
					throw new RuntimeException("The predefined cert root " + predefinedCertRoot + " is set but it does not exist!");
				}

				// Set the expected files
				Path certMetadataPath = predefinedCertRoot.resolve("cert-metadata.sh");
				Path configPath = predefinedCertRoot.resolve("config.cfg");
				List<Path> sourceCaGenerationArtifacts = caFiles.stream().map(predefinedCertRoot::resolve)
						.filter(Files::exists).collect(Collectors.toList());

				// And then validate they exist
				if (!Files.exists(certMetadataPath)) {
					throw new RuntimeException("The predefined cert root " + predefinedCertRoot + " does not contain a 'cert-metadata.sh' file!");
				}

				if (!Files.exists(configPath)) {
					throw new RuntimeException("The predefined cert root " + predefinedCertRoot + " does not contain a 'config.cfg' file!");
				}

				if (caFiles.size() != sourceCaGenerationArtifacts.size()) {
					throw new RuntimeException("Could not load CA files from '" + predefinedCertRoot + "'! You may need to regenerate your certificates!");
				}

				// Copy the bulk of the files
				Files.copy(configPath, tmpPath.resolve(configPath.getFileName()));

				// Copy the configuration-specific ones
				Path certMetadataTarget = tmpPath.resolve(certMetadataPath.getFileName());
				Files.copy(certMetadataPath, certMetadataTarget);
				// Update the metadata target path
				updateCertMetadata(certMetadataTarget, certTargetPath, null);

				// And copy the files that belong in the cert directory
				for (Path source : sourceCaGenerationArtifacts) {
					Files.copy(source, certTargetPath.resolve(source.getFileName()));
				}

			} else {
				// Copy default config files
				Path certMetadataPath = tmpPath.resolve("cert-metadata.sh");

				boolean updateMetadata = false;

				if (!Files.exists(tmpPath.resolve("config.cfg"))) {
					Files.copy(certToolPath.resolve("config.cfg"), tmpPath.resolve("config.cfg"));
					updateMetadata = true;
				}

				if (!Files.exists(certMetadataPath)) {
					Files.copy(certToolPath.resolve("cert-metadata.sh"), certMetadataPath);
					updateMetadata = true;
				}

				if (updateMetadata) {
					// Update the metadata
					updateCertMetadata(certMetadataPath, certTargetPath, "_TAKTEST");
				}

				List<Path> missingArtifacts = caFiles.stream().map(certTargetPath::resolve)
						.filter(x -> !Files.exists(x)).collect(Collectors.toList());

				if (missingArtifacts.size() > 0) {
					List<String> artifactList = new LinkedList<>();
					for (Path artifact : missingArtifacts) {
						artifactList.add(artifact.toString());
					}
					System.err.println("ARTIFACTS: [" + String.join(",", artifactList) + "]");
					// Generate the root CA
					ProcessBuilder pbx = new ProcessBuilder(tmpPath.resolve("makeRootCa.sh").toString(), "--ca-name", "TakTestRootCA");
					pbx.directory(tmpPath.toFile());
					pbx.environment().put("COUNTRY", "US");
					pbx.environment().put("STATE", "MA");
					pbx.environment().put("CITY", "Cambridge");
					pbx.environment().put("ORGANIZATIONAL_UNIT", "TAKSERVER-TEST");

					Process px = pbx.start();

					// This unfortunately errors out in OS X even though it works fine... Ignoring for now and checking that the file was created as expected
					px.waitFor();
					if (!Files.exists(certTargetPath.resolve("root-ca.pem"))) {
						throw new RuntimeException("Failed to execute makeRootCA.sh command!");
					}
				} else {
					System.err.println("NO MISSING ARTIFACTS!");
				}
			}

		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public static synchronized boolean genCertsIsNecessary() {
		Path targetCertDir = TAKCLConfigModule.getInstance().getCertificateDir();

		List<String> allCertNames = new LinkedList<>(clientCertNames);
		allCertNames.addAll(clientCertNames);
		allCertNames.addAll(serverCertNames);

		List<Path> allCertPaths = new LinkedList<>();
		for (String certName : allCertNames) {
			for (String fileExtension : generatedFileExtensions) {
				allCertPaths.add(targetCertDir.resolve(certName + fileExtension));
			}
		}

		allCertPaths.addAll(caFiles.stream().map(targetCertDir::resolve).collect(Collectors.toList()));

		for (Path filepath : allCertPaths) {
			if (!Files.exists(filepath)) {
				return true;
			}
		}
		return false;
	}

	public static synchronized void genCertsIfNecessary() throws IOException {
		if (certsGenerated) {
			return;
		}
		if (!genCertsIsNecessary()) {
			certsGenerated = true;
			return;
		}

		configureCa();

		Path makeCertPath = Paths.get(TAKCLConfigModule.getInstance().getTemporaryDirectory()).resolve("makeCert.sh");

		Path predefinedCertRoot = TAKCLCore.testCertSourceDir;
		if (predefinedCertRoot != null && !Files.exists(predefinedCertRoot)) {
			throw new RuntimeException("The predefined cert root " + predefinedCertRoot + " is set but it does not exist!");
		}

		try {
			produceCerts("server", makeCertPath, predefinedCertRoot, serverCertNames);
			produceCerts("client", makeCertPath, predefinedCertRoot, clientCertNames);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		certsGenerated = true;
	}

	public static class TakClientSslContext {

		private static final String STORETYPE = "JKS";
		private static final String CLIENT_CERT_PASSWORD = "atakatak";

		private final File truststoreJksPath;
		private final File userCertPrivateJksPath;

		private X509TrustManager trustManager;
		private SSLContext sslContext;

		public TakClientSslContext(@NotNull AbstractUser user) {
			this.userCertPrivateJksPath = user.getCertPrivateJksPath().toFile();
			this.truststoreJksPath = new File(TAKCLConfigModule.getInstance().getTruststoreJKSFilepath());
		}

		public X509TrustManager getTrustManager() {
			return this.trustManager;
		}

		public SSLSocketFactory getSslSocketFactory() {
			return sslContext.getSocketFactory();
		}

		public TakClientSslContext init() throws GeneralSecurityException, IOException {
			trustManager = initTrustManager(STORETYPE, CLIENT_CERT_PASSWORD, truststoreJksPath);
			sslContext = initSslContext(STORETYPE, CLIENT_CERT_PASSWORD, userCertPrivateJksPath, trustManager);
			return this;
		}
	}
}
