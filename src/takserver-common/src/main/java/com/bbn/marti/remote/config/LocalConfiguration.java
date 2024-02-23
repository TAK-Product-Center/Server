package com.bbn.marti.remote.config;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.List;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import com.bbn.marti.remote.util.SpringContextBeanForApi;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;

import org.apache.commons.io.IOUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tak.server.util.ActiveProfiles;
import com.bbn.marti.config.Auth;
import com.bbn.marti.config.Buffer;
import com.bbn.marti.config.Cluster;
import com.bbn.marti.config.Configuration;
import com.bbn.marti.config.Connection;
import com.bbn.marti.config.DosLimitRule;
import com.bbn.marti.config.DosRateLimiter;
import com.bbn.marti.config.Federation;
import com.bbn.marti.config.Federation.FederationOutgoing;
import com.bbn.marti.config.Federation.FederationServer.V1Tls;
import com.bbn.marti.config.Filter;
import com.bbn.marti.config.Flowtag;
import com.bbn.marti.config.Network;
import com.bbn.marti.config.Network.Connector;
import com.bbn.marti.config.Plugins;
import com.bbn.marti.config.Qos;
import com.bbn.marti.config.Queue;
import com.bbn.marti.config.ReadRateLimiter;
import com.bbn.marti.config.Repeater;
import com.bbn.marti.config.DeliveryRateLimiter;
import com.bbn.marti.config.RateLimitRule;
import com.bbn.marti.config.Repository;
import com.bbn.marti.config.Security;
import com.bbn.marti.config.Streamingbroker;
import com.bbn.marti.config.Subscription;
import com.bbn.marti.config.Thumbnail;
import com.bbn.marti.config.Tls;
import com.bbn.marti.config.Urladd;
import com.bbn.marti.config.Vbm;
import com.bbn.marti.remote.exception.NotFoundException;
import com.bbn.marti.remote.exception.TakException;

import tak.server.Constants;
import tak.server.util.JAXBUtils;

public class LocalConfiguration {
	private static final Logger logger = LoggerFactory.getLogger(LocalConfiguration.class);

	public static final String CONFIG_NAMESPACE = "http://bbn.com/marti/xml/config";
	public static final String DEFAULT_TRUSTSTORE = "certs/files/fed-truststore.jks";
	public static String CONFIG_FILE = null;
	static final String DEFAULT_CONFIG_FILE = "CoreConfig.xml";
	static final String ALT_DEFAULT_CONFIG_FILE = "data/CoreConfig.xml";
	static final String EXAMPLE_BASE_CONFIG_FILE = "CoreConfig.example.xml";
	public static boolean doUpgrades = true;
	private Configuration configuration;

	public Configuration getConfiguration() {
		return configuration;
	}

	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}
	
	private static LocalConfiguration instance;

	public static LocalConfiguration getInstance() {
		if (instance == null) {
			synchronized(LocalConfiguration.class) {
				if (instance == null) {
					try {
						instance = SpringContextBeanForApi.getSpringContext().getBean(
								LocalConfiguration.class);
					}catch (Exception e) {
						try {
							instance = new LocalConfiguration();
						}
						catch (Exception e2)
						{
							if (logger.isErrorEnabled()) {
								logger.error("Exception instantiating LocalConfiguration : {} ", e2);
							}
						}

					}
				}
			}
		}

		return instance;
	}

	private LocalConfiguration() throws RemoteException {

		// Load CoreConfig.xml from file. This always needs to be done first - even
		// when using the cluster. We need to look at the config file to figure if
		// clustering is enabled, and
		// whether we are using Kubernetes. This info is important for ignite
		// (distributed cache) initialization.
		Configuration conf = null;
		try {
			if (CONFIG_FILE == null) {
				CONFIG_FILE = DEFAULT_CONFIG_FILE;
				Path configPath = Paths.get(CONFIG_FILE);
				Path altConfigPath = Paths.get(ALT_DEFAULT_CONFIG_FILE);

				if (Files.exists(altConfigPath)) {
					CONFIG_FILE = ALT_DEFAULT_CONFIG_FILE;
				} else if (!Files.exists(configPath)) {
					Files.copy(Paths.get(EXAMPLE_BASE_CONFIG_FILE), configPath);
					configPath.toFile().setWritable(true);
				}
			} else {
				if (!Files.exists(Paths.get(CONFIG_FILE))) {
					throw new NotFoundException("The configuration file '" + CONFIG_FILE + "' does not exist!");
				}
			}

			conf = JAXBUtils.loadJAXifiedXML(CONFIG_FILE, Configuration.class.getPackage().getName());

		} catch (jakarta.xml.bind.UnmarshalException ue) {
			// There is a good chance it is lacking a namespace if it is an older file.
			// Let's check...
			try {

				File xmlFile = new File(CONFIG_FILE);
				SAXReader reader = new SAXReader();
				Document doc = reader.read(xmlFile);
				Element root = doc.getRootElement();
				Namespace namespace = root.getNamespace();

				// It's lacking a namespace. So let's add it!
				if (Strings.isNullOrEmpty(namespace.getURI())) {
					namespace = new Namespace(null, CONFIG_NAMESPACE);
					root.add(namespace);
					String str = root.asXML();

					// Get rid of those pesky element-specific declarations that aren't necessary...
					str = str.replaceAll("xmlns=\"\"", "");

					// Now let's read it in!
					JAXBContext jc = JAXBContext.newInstance(Configuration.class.getPackage().getName());
					Unmarshaller u = jc.createUnmarshaller();
					conf = (Configuration) u.unmarshal(new ByteArrayInputStream(str.getBytes()));
					JAXBUtils.saveJAXifiedObject(CONFIG_FILE, conf, false);

				} else {
					// It has a namespace. No idea what happened....
					throw new RuntimeException(ue);
				}

				// It still didn't like the file. No idea what happened...

			} catch (DocumentException | JAXBException | IOException e0) {
				throw new RuntimeException(e0);
			}

		} catch (NotFoundException | NoSuchFileException nfe) {
			logger.error("Config file does not exist.");
		} catch (Exception e) {
			logger.error("Exception parsing config", e);
			throw new TakException(e);
		}

		// for unit tests
		if (conf == null) {
			conf = new Configuration();
		}

		setConfiguration(conf);

		boolean changedDefaults = setDefaults();


		// Lets force outgoing federation connections to be disabled in
		// the cluster.. otherwise the first node to join will always start all the connections.
		// By forcing this off, it will have to be enabled in the UI, which will attempt to
		// load balance each outgoing connection for us
		if (configuration.getCluster().isEnabled()) {
			for (FederationOutgoing outgoing : configuration.getFederation().getFederationOutgoing()) {
				outgoing.setEnabled(false);
			}
			saveChanges();
		}

		boolean changedUpgrade = false;

		boolean upgradedQoS = doUpgradeQoS();

		if (upgradedQoS) {
			logger.info("saving upgraded QoS configuration");
			saveChanges();
		}

		boolean detectedUpgrade = detectUpgrade(configuration.getNetwork().getVersion());

		if (detectedUpgrade) {
			logger.info("upgrade detected");
		}

		if (doUpgrades && detectedUpgrade) {
			changedUpgrade = doUpgrade() || upgradedQoS;
		}

		if (changedDefaults || changedUpgrade) {
			logger.info("saving upgraded configuration");
			saveChanges();
		}

		ConfigHelper.validateConfiguration(configuration);
	}

	public boolean setDefaults() {

		boolean changed = false;

		if (configuration.getFilter() == null) {
			configuration.setFilter(new Filter());
			changed = true;
		}
		if (configuration.getFilter().getFlowtag() == null) {
			configuration.getFilter().setFlowtag(new Flowtag());
			changed = true;
		}

		if (configuration.getFilter().getThumbnail() == null) {
			configuration.getFilter().setThumbnail(new Thumbnail());
			changed = true;
		}

		if (configuration.getFilter().getUrladd() == null) {
			configuration.getFilter().setUrladd(new Urladd());
			changed = true;
		}

		if (configuration.getFilter().getUrladd().getHost() == null) {
			String host;
			try {
				host = "http://" + InetAddress.getLocalHost().getHostAddress() + ":8080";
			} catch (UnknownHostException uhe) {
				logger.error("Looking up local host name", uhe);
				host = "http://127.0.0.1:8080";
			}
			configuration.getFilter().getUrladd().setHost(host);
			changed = true;
		}

		if (configuration.getFilter().getStreamingbroker() == null) {
			configuration.getFilter().setStreamingbroker(new Streamingbroker());
			changed = true;
		}

		if (configuration.getBuffer() == null) {
			configuration.setBuffer(new Buffer());
			changed = true;
		}
		if (configuration.getBuffer().getLatestSA() == null) {
			configuration.getBuffer().setLatestSA(new Buffer.LatestSA());
			changed = true;
		}
		if (configuration.getBuffer().getQueue() == null) {
			configuration.getBuffer().setQueue(new Queue());
			changed = true;
		}

		if (configuration.getBuffer().getQueue().getPriority() == null) {
			configuration.getBuffer().getQueue().setPriority(new Queue.Priority());
			changed = true;
		}

		if (configuration.getNetwork() == null) {
			configuration.setNetwork(new Network());
			changed = true;
		}

		if (configuration.getRepository() == null) {
			configuration.setRepository(new Repository());
			changed = true;
		}
		if (configuration.getRepository().getConnection() == null) {
			configuration.getRepository().setConnection(new Connection());
			changed = true;
		}

		if (configuration.getRepeater() == null) {
			configuration.setRepeater(new Repeater());
			changed = true;
		}

		if (configuration.getNetwork().getAnnounce() == null) {
			configuration.getNetwork().setAnnounce(new Network.Announce());
			changed = true;
		}

		if (configuration.getSubscription() == null) {
			configuration.setSubscription(new Subscription());
			changed = true;
		}

		// TODO: This can probably be done in the actual xsd file....
		if (configuration.getAuth() == null) {
			configuration.setAuth(new Auth());
			changed = true;
		}
		if (configuration.getAuth().getFile() == null) {
			configuration.getAuth().setFile(new Auth.File());
			changed = true;
		}

		if (configuration.getPlugins() == null) {
			configuration.setPlugins(new Plugins());
			changed = true;
		}

		// Add a cluster tag to the config if absent (added in 1.3.12)
		if (configuration.getCluster() == null) {
			configuration.setCluster(new Cluster());
			changed = true;
		}

		// No Try/Catch since this will result in other less clear failures and
		// exceptions
		// If Federation section isn't in CoreConfig add it because it's required now
		if (configuration.getFederation() == null) {
			Federation.FederationServer fedServer = new Federation.FederationServer();
			Tls existingTls = getSecurity().getTls();
			Tls tls = new Tls();
			tls.setKeystore(existingTls.getKeystore());
			tls.setKeystoreFile(existingTls.getKeystoreFile());
			tls.setKeystorePass(existingTls.getKeystorePass());
			tls.setTruststore(existingTls.getTruststore());
			tls.setTruststoreFile(DEFAULT_TRUSTSTORE);
			tls.setTruststorePass(existingTls.getTruststorePass());
			tls.setKeymanager(existingTls.getKeymanager());
			fedServer.setTls(tls);
			Federation federation = new Federation();
			federation.setFederationServer(fedServer);
			configuration.setFederation(federation);

			changed = true;
		}

		if (configuration.getFederation() != null) {
			// set the default file extension filter to "pref"
			if (configuration.getFederation().getFileFilter() == null) {
				Federation.FileFilter fileFilter = new Federation.FileFilter();
				fileFilter.getFileExtension().add("pref");
				configuration.getFederation().setFileFilter(fileFilter);
				changed = true;
			}
		}

		try {
			if (configuration.getFederation() != null
					&& Strings.isNullOrEmpty(configuration.getFederation().getFederationServer().getWebBaseUrl())) {

				String ip = null;

				// make an educated guess about the external IP address of this machine
				try {
					ip = InetAddress.getLocalHost().getHostAddress();
				} catch (Exception e) {
					logger.warn("exception while trying to determine external IP address", e);
				}

				if (Strings.isNullOrEmpty(ip)) {
					logger.warn("unable to determine IP address for mission package base URL. Using localhost.");
					ip = "localhost";
				}

				String baseUrl = "https://" + ip + ":8443/Marti";

				configuration.getFederation().getFederationServer().setWebBaseUrl(baseUrl);

				changed = true;

				logger.info("base url for V2 federated mission packages: " + baseUrl);
			}
		} catch (Exception e) {
			logger.warn("exception setting webBaseUrl default", e);
		}

		// Add a plugins tag to the config if absent
		if (configuration.getPlugins() == null) {
			configuration.setPlugins(new Plugins());
			changed = true;
		}

		// Add a cluster tag to the config if absent (added in 1.3.12)
		if (configuration.getCluster() == null) {
			configuration.setCluster(new Cluster());
			changed = true;
		}
		
		if (configuration.getVbm() == null) {
			configuration.setVbm(new Vbm());
		}

		return changed;
	}

	public Security getSecurity() {

		// for unit tests
		if (getConfiguration().getSecurity() == null) {
			getConfiguration().setSecurity(new Security());
		}

		// for unit tests
		if (getConfiguration().getSecurity().getTls() == null) {
			getConfiguration().getSecurity().setTls(new Tls());
		}

		return getConfiguration().getSecurity();
	}

	private boolean doUpgrade() {

		boolean changed = false;

		logger.info("upgrading configuration");

		// for upgrade case - set up default connectors if there are none defined in
		// CoreConfig
		try {
			if (configuration.getNetwork() != null && configuration.getNetwork().getConnector().isEmpty()
					&& ActiveProfiles.getInstance().isApiProfileActive()) {

				List<Connector> connectors = configuration.getNetwork().getConnector();

				Connector conn8443 = new Connector();
				conn8443.setName("https");
				conn8443.setPort(8443);

				Connector conn8444 = new Connector();
				conn8444.setName("fed_https");
				conn8444.setPort(8444);
				conn8444.setUseFederationTruststore(true);

				Connector conn8446 = new Connector();
				conn8446.setName("cert_https");
				conn8446.setPort(8446);
				conn8446.setClientAuth("false");

				Connector conn8080 = new Connector();
				conn8080.setName("http_plaintext");
				conn8080.setPort(8080);
				conn8080.setTls(false);

				connectors.add(conn8443);
				connectors.add(conn8444);
				connectors.add(conn8446);
				connectors.add(conn8080);

				logger.info("set default web connectors for port 8443, 8444, 8446 and 8080.");

				changed = true;
			}
		} catch (Exception e) {
			logger.warn("exception setting up configuration for default web connectors", e);
		}

		try {
			if (configuration.getRepository() != null && configuration.getRepository().getNumDbConnections() < Constants.CONNECTION_POOL_DEFAULT_SIZE) {

				configuration.getRepository().setNumDbConnections(Constants.CONNECTION_POOL_DEFAULT_SIZE);

				logger.info("set default database connection pool size (per-process) to "
						+ Constants.CONNECTION_POOL_DEFAULT_SIZE);

				changed = true;
			}
		} catch (Exception e) {
			logger.warn("exception setting up default connection pool size", e);
		}
		
		try {
			if (configuration.getFederation() != null && configuration.getFederation().getFederationServer() != null 
					&& configuration.getFederation().getFederationServer().getV1Tls().size() == 0) {

				V1Tls v1tTls2 = new V1Tls();
				V1Tls v1tTls3 = new V1Tls();
				
				v1tTls2.setTlsVersion("TLSv1.2");
				v1tTls3.setTlsVersion("TLSv1.3");
				
				configuration.getFederation().getFederationServer().getV1Tls().add(v1tTls2);
				configuration.getFederation().getFederationServer().getV1Tls().add(v1tTls3);
				
				// use lower default (also in xsd)
				configuration.getFederation().setMissionFederationDisruptionToleranceRecencySeconds(43200L);

				changed = true;
			}
		} catch (Exception e) {
			logger.warn("exception setting up default v1 tls version(s)", e);
		}

		try {

			// save current version, so that we won't try upgrade next time takserver is run
			configuration.getNetwork().setVersion(getCurrentVer());

			changed = true;

		} catch (Exception e) {
			logger.warn("exception saving version in configuration file", e);
		}
		
		return changed;
	}
	
	private boolean doUpgradeQoS() {

		boolean changed = false;

		// Add default QoS configuration (TAK Server 4.3 and higher)
		try {
			
			Filter filter = configuration.getFilter();
			
			Qos qos = filter.getQos();
			
			if (qos == null) {
				// no QoS config present
				// create default QoS config
				
				qos = new Qos();
				
				filter.setQos(qos);
				
				changed = true;
			}
			
			DeliveryRateLimiter drl = qos.getDeliveryRateLimiter();
			if (drl == null) {
				drl = new DeliveryRateLimiter();
				drl.setEnabled(true);
				qos.setDeliveryRateLimiter(drl);
				changed = true;
			}
			
			changed = setupRateLimiter(drl.getRateLimitRule()) || changed;
			
			ReadRateLimiter rrl = qos.getReadRateLimiter();
			if (rrl == null) {
				rrl = new ReadRateLimiter();
				rrl.setEnabled(false);
				qos.setReadRateLimiter(rrl);
				changed = true;
			}
			
			changed = setupRateLimiter(rrl.getRateLimitRule()) || changed;

			DosRateLimiter dosrl = qos.getDosRateLimiter();
			if (dosrl == null) {
				dosrl = new DosRateLimiter();
				dosrl.setEnabled(false);
				dosrl.setIntervalSeconds(60);
				qos.setDosRateLimiter(dosrl);
				changed = true;
			}
			
			if (dosrl.getDosLimitRule().isEmpty()) {
				DosLimitRule rule1 = new DosLimitRule();
				rule1.setClientThresholdCount(1);
				rule1.setMessageLimitPerInterval(60);
				dosrl.getDosLimitRule().add(rule1);
				changed = true;
			}
		} catch (Exception e) {
			logger.warn("exception saving version in configuration file", e);
		}

		return changed;
	}
	
	private boolean setupRateLimiter(List<RateLimitRule> rules) {
		
		if (rules.isEmpty()) {				
			
			RateLimitRule rule500 = new RateLimitRule();
			
			rule500.setClientThresholdCount(500);
			rule500.setReportingRateLimitSeconds(200);
			
			rules.add(rule500);
			
			RateLimitRule rule1000 = new RateLimitRule();
			
			rule1000.setClientThresholdCount(1000);
			rule1000.setReportingRateLimitSeconds(300);
			
			rules.add(rule1000);
			
			RateLimitRule rule2000 = new RateLimitRule();
			
			rule2000.setClientThresholdCount(2000);
			rule2000.setReportingRateLimitSeconds(400);
			
			rules.add(rule2000);
			
			RateLimitRule rule5000 = new RateLimitRule();
			
			rule5000.setClientThresholdCount(5000);
			rule5000.setReportingRateLimitSeconds(800);
			
			rules.add(rule5000);
			
			RateLimitRule rule10000 = new RateLimitRule();
			
			rule10000.setClientThresholdCount(10000);
			rule10000.setReportingRateLimitSeconds(1200);
			
			rules.add(rule10000);
			
			return true;
		}
		return false;
	}

	private boolean detectUpgrade(String previousVer) {

		if (Strings.isNullOrEmpty(previousVer)) {
			return true;
		}

		String currentVer = getCurrentVer();

		return !currentVer.equals(previousVer);
	}

	private String getCurrentVer() {
		String currentVer = null;

		try {
			currentVer = IOUtils.toString(
					LocalConfiguration.class.getResourceAsStream(Constants.SHORT_VER_RESOURCE_PATH), Charsets.UTF_8);
		} catch (Exception e) {
			throw new TakException(e);
		}

		if (Strings.isNullOrEmpty(currentVer)) {
			throw new IllegalArgumentException("unable to detect current TAK Server version");
		}

		return currentVer;
	}

	public void saveChanges() {
		try {
			synchronized (LocalConfiguration.class) {
				JAXBUtils.saveJAXifiedObject(CONFIG_FILE, configuration, false);
			}
		} catch (FileNotFoundException fnfe) {
			// do nothing. Happens in unit test.
		} catch (JAXBException | IOException e) {
			throw new RuntimeException(e);
		}
	}

}
