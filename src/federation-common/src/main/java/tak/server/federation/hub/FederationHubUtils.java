package tak.server.federation.hub;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.configuration.ClientConnectorConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.failure.NoOpFailureHandler;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.roger.fig.FederationUtils;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Strings;

public class FederationHubUtils {
	private static final Logger logger = LoggerFactory.getLogger(FederationHubUtils.class);

    public static IgniteConfiguration getIgniteConfiguration(String profile, boolean isClient) {
    	FederationHubIgniteConfig igniteConfig = null;
    	
		String igniteFile = System.getProperty("fedhub.ignite.config");
    	if (Strings.isNullOrEmpty(igniteFile)) {
			igniteFile =  "/opt/tak/federation-hub/configs/ignite.yml";
    		logger.info("Ignite config file not supplied. Assigning default to: " + igniteFile);
    	} else {
    		logger.info("Ignite Config file supplied: " + igniteFile);
    	}
    	
    	try {
    		igniteConfig = new FederationHubUtils().loadIgniteConfig(igniteFile);
    		logger.info("Loaded ignite config from file");
    	} catch (Exception e) {
    		logger.info("Ignite config not found, generating default one");
    		// failed to load file, use defaults
			igniteConfig = new FederationHubIgniteConfig();
		}
    	
        IgniteConfiguration conf = new IgniteConfiguration();
        
        String defaultWorkDir = "/opt/tak/federation-hub";
		try {
			 defaultWorkDir = U.defaultWorkDirectory();
		} catch (IgniteCheckedException e) {
			logger.error(" error getting Ignite work dir, default to /opt/tak/federation-hub ", e);
		}

		conf.setWorkDirectory(defaultWorkDir + "/" + profile + "-tmp-work");

        String address = FederationHubConstants.FEDERATION_HUB_IGNITE_HOST + ":" +
            FederationHubConstants.NON_MULTICAST_DISCOVERY_PORT + ".." +
            (FederationHubConstants.NON_MULTICAST_DISCOVERY_PORT +
                FederationHubConstants.NON_MULTICAST_DISCOVERY_PORT_COUNT);
        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
        ipFinder.setAddresses(Arrays.asList(address));

        TcpDiscoverySpi spi = new TcpDiscoverySpi();
        spi.setIpFinder(ipFinder);
        spi.setLocalPort(FederationHubConstants.NON_MULTICAST_DISCOVERY_PORT);
        spi.setLocalPortRange(FederationHubConstants.NON_MULTICAST_DISCOVERY_PORT_COUNT);

        conf.setDiscoverySpi(spi);

        TcpCommunicationSpi comms = new TcpCommunicationSpi();
        comms.setLocalPort(FederationHubConstants.COMMUNICATION_PORT);
        comms.setLocalPortRange(FederationHubConstants.COMMUNICATION_PORT_COUNT);
        comms.setLocalAddress(FederationHubConstants.FEDERATION_HUB_IGNITE_HOST);
        comms.setMessageQueueLimit(512);

        conf.setCommunicationSpi(comms);

        conf.setClientMode(isClient);

        conf.setUserAttributes(
            Collections.singletonMap(
                FederationHubConstants.FEDERATION_HUB_IGNITE_PROFILE_KEY,
                profile));

        conf.setFailureHandler(new NoOpFailureHandler());
        
        int poolSize;
        // dynamic
        if (igniteConfig.getIgnitePoolSize() < 0) {
        	poolSize = Math.min(Runtime.getRuntime().availableProcessors() * igniteConfig.getIgnitePoolSizeMultiplier(), 1024);
        } else {
        	poolSize = igniteConfig.getIgnitePoolSize();
        }
        
        if (isClient) {
        	ClientConnectorConfiguration ccc = conf.getClientConnectorConfiguration();
        	ccc.setThreadPoolSize(poolSize);
        }
        
        conf.setSystemThreadPoolSize(poolSize + 1);
        conf.setPublicThreadPoolSize(poolSize);
        conf.setQueryThreadPoolSize(poolSize);
        conf.setServiceThreadPoolSize(poolSize);
        conf.setStripedPoolSize(poolSize);
        conf.setDataStreamerThreadPoolSize(poolSize);
        conf.setRebalanceThreadPoolSize(poolSize);

        return conf;
    }
    
	private FederationHubIgniteConfig loadIgniteConfig(String configFile)
			throws JsonParseException, JsonMappingException, FileNotFoundException, IOException {
		if (getClass().getResource(configFile) != null) {
			// It's a resource.
			return new ObjectMapper(new YAMLFactory()).readValue(getClass().getResourceAsStream(configFile),
					FederationHubIgniteConfig.class);
		}

		// It's a file.
		return new ObjectMapper(new YAMLFactory()).readValue(new FileInputStream(configFile),
				FederationHubIgniteConfig.class);
	}
	
    public static String getCN(String dn) throws RuntimeException {
        if (Strings.isNullOrEmpty(dn)) {
            throw new IllegalArgumentException("empty DN");
        }

        try {
            LdapName ldapName = new LdapName(dn);

            for (Rdn rdn : ldapName.getRdns()) {
                if (rdn.getType().equalsIgnoreCase("CN")) {
                    return rdn.getValue().toString();
                }
            }

            throw new RuntimeException("No CN found in DN: " + dn);
        } catch (InvalidNameException e) {
            throw new RuntimeException(e);
        }
    }
    
	public static X509Certificate loadX509CertFromBytes(byte[] cert) throws CertificateException, IOException {

		if (cert == null) {
			throw new IllegalArgumentException("empty cert");
		}

		CertificateFactory cf = CertificateFactory.getInstance("X.509");

		InputStream is = new ByteArrayInputStream(cert);
		try {
			return (X509Certificate) cf.generateCertificate(is);
		} finally {
			is.close();
		}
	}

	public static X509Certificate loadX509CertFile(String caFilename) throws CertificateException, IOException {

		if (Strings.isNullOrEmpty(caFilename)) {
			throw new IllegalArgumentException("empty ca file name");
		}

		CertificateFactory cf = CertificateFactory.getInstance("X.509");

		InputStream is = new FileInputStream(caFilename);
		try {
			return (X509Certificate) cf.generateCertificate(is);
		} finally {
			is.close();
		}
	}
    
    public static List<X509Certificate> verifyTrustedClientCert(TrustManagerFactory tmf, X509Certificate clientCert) {
    	List<X509Certificate> signingCa = new ArrayList<>();
    	for (TrustManager trustManager : tmf.getTrustManagers()) {
			if (trustManager instanceof X509TrustManager) {
				try {
					X509TrustManager x509TrustManager = (X509TrustManager) trustManager;
					
					// first validate and check if client certificate is trusted
					x509TrustManager.checkClientTrusted(new X509Certificate[] { clientCert }, "RSA");

					// next find the CA(s) that signed the client certificate
					for (X509Certificate trustedCa : x509TrustManager.getAcceptedIssuers()) {
						try {
							clientCert.verify(trustedCa.getPublicKey());
							signingCa.add(trustedCa);
						} catch (Exception e) {}
					}
				} catch (Exception e) {}
			}
		}
    	return signingCa;
    }
    
    public static List<String> getCaGroupIdsFromCerts(Certificate[] peerCertificates) {
    	List<String> caCertGroups = new LinkedList<>();
		/*
		 * The cert array returned by gRPC's SSLSession is padded with null entries.
		 * This loop adds all certs in the array from index 1 (index 0 is the peer's
		 * cert, index 1+ are CA certs) til the first null entry (the start of the
		 * padding) to a list of CA certs.
		 */
		for (int i = 1; i < peerCertificates.length; i++) {
			if (peerCertificates[i] == null) {
				break;
			}
			
			X509Certificate caCert = ((X509Certificate) peerCertificates[i]);
    		caCertGroups.add(getCaGroupIdFromCert(caCert));
		}
		return caCertGroups;
    }
    
    public static String getCaGroupIdFromCert(X509Certificate caCert) {
    	try {
    		String fingerprint = FederationUtils.getBytesSHA256(caCert.getEncoded());
    		String issuerDN = caCert.getIssuerX500Principal().getName();
    		String issuerCN = Optional.ofNullable(getCN(issuerDN)).map(cn -> cn.toLowerCase()).orElse("");
    		
    		return issuerDN + "-" + fingerprint;
    	} catch (Exception e) {
    		logger.error("getCaGroupIdFromCert error", e);
    		return null;
		}
    }
}
