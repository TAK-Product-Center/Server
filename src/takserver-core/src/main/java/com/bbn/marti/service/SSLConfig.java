

package com.bbn.marti.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.UnrecoverableEntryException;
import java.security.cert.CRLException;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.CertPathTrustManagerParameters;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.config.ConfigHelper;
import com.bbn.marti.config.Tls;
import com.bbn.marti.nio.util.SslSource;
import com.bbn.marti.util.Assertion;
import com.google.common.base.Strings;

/**
 * This class is two things: an ssl loader that pulls its settings from core config,
 * and an SslSource, which is an object that returns a server/client ssl engine
 *
 * TODO: settle on engine configuration for server/client
 */
public class SSLConfig implements SslSource, Serializable {
    
	private static final long serialVersionUID = 5546680269230159556L;

	private final static Logger logger = LoggerFactory.getLogger(SSLConfig.class);
	
	private static final Map<String, SSLConfig> instanceMap = new ConcurrentHashMap<>();
	
	private KeyManagerFactory keyMgrFactory;
	
	private TrustManagerFactory trustMgrFactory;
	
	private Tls tlsConfig;

	// keep a singleton SSLConfig per TLS config object
	// update 3-24-20: lets use the filename as a key
	// since we wont have consistent configuration objects due to caching
	public static synchronized SSLConfig getInstance(@NotNull final Tls tlsConfig) {
	    logger.trace("instanceMap size: " + instanceMap.size());
	    
	    SSLConfig instance = instanceMap.get(tlsConfig.getTruststoreFile());
	    
	    if (instance != null) {
	        return instance;
	    }
	    
	    instance = new SSLConfig(tlsConfig);
	    
	    instanceMap.put(tlsConfig.getTruststoreFile(), instance);
	    
	    return instance;
	}
	
	private SSLContext sslContext; // context used to source the tabula rasa engine
	private final SSLParameters sslParameters; // paremeters injected into the base engine
	private KeyStore trust;
	private KeyStore self;

	public SSLConfig(@NotNull Tls tls) {
	    
	    this.tlsConfig = tls;
		this.sslContext = initSslContext(tls);
		this.sslParameters = initBaseSslParameters(sslContext, tls);
	}

	public SSLContext getSSLContext() {
		return sslContext;
	}

	public SSLParameters getSSLParameters() {
		return sslParameters;
	}

	@Override
	public SSLEngine buildServerEngine() {
		SSLEngine engine = baseSslEngine();

		engine.setUseClientMode(false);

		return engine;
	}

	@Override
	public SSLEngine buildClientEngine() {
		SSLEngine engine = baseSslEngine();

		engine.setUseClientMode(true);

		return engine;
	}

	public KeyStore getTrust() {
		return trust;
	}
	
	public KeyStore getSelf() {
		return self;
	}

	/**
	 * returns a new engine, but with the field ssl parameters set
	 */
	private SSLEngine baseSslEngine() {
		// init engine from context
		SSLEngine engine = sslContext.createSSLEngine();

		// embed preconfigured params into the engine
		engine.setSSLParameters(this.sslParameters);

		return engine;
	}
	
	public SSLEngine getNewSSLEngine(boolean isClient) {
	 // init engine from context
	    
	    refresh();
	    
        SSLEngine engine = sslContext.createSSLEngine();
        
        if (isClient) {
            engine.setUseClientMode(true);
        } else {
            engine.setUseClientMode(false);
        }

        // embed preconfigured params into the engine
        engine.setSSLParameters(this.sslParameters);

        return engine;
	}
	
	@Override
	public synchronized void refresh() {
	    try {
	        // initialize trust store, reloading it from disk.
            this.trust = initTrust(this.tlsConfig, this.trustMgrFactory);
	        
            sslContext.init(keyMgrFactory.getKeyManagers(), trustMgrFactory.getTrustManagers(), new SecureRandom());
        } catch (KeyManagementException e) {
            logger.warn("exception refreshing SSL context", e);
        }
	}

	private SSLContext initSslContext(@NotNull Tls tls) {
	    
	    if (tls == null) {
	        throw new IllegalArgumentException("null tls config");
	    }
	    
	    logger.debug("tls config: " + tls);
	    
		//never allow unsafe SSL re-negotiation
		System.setProperty("sun.security.ssl.allowUnsafeRenegotiation", Boolean.FALSE.toString());

		SSLContext sslContext = null;

		try {
		    
			if (logger.isDebugEnabled()) {
				logger.debug("available security providers: " + Security.getProviders());
			}
		    
		    String keyManager = tls.getKeymanager();
		    
		    if (Strings.isNullOrEmpty(keyManager)) {
		        throw new IllegalArgumentException("empty key manager configuration");
		    }
		    
		    keyMgrFactory = KeyManagerFactory.getInstance(keyManager);
		    
		    String keystoreType = tls.getKeystore();
		    
		    if (Strings.isNullOrEmpty(keystoreType)) {
		        throw new IllegalArgumentException("empty keystore type");
		    }
		    
			self = KeyStore.getInstance(keystoreType);
			
			String keystoreFile = tls.getKeystoreFile();
			
			if (Strings.isNullOrEmpty(keystoreFile)) {
			    throw new IllegalArgumentException("keystore file name empty");
			}
			
			String keystorePassword = tls.getKeystorePass();
			
			if (Strings.isNullOrEmpty(keystorePassword)) {
			    throw new IllegalArgumentException("empty keystore password");
			}

			try(FileInputStream fis = new FileInputStream(keystoreFile)) {
				// Filename of the keystore file
				self.load(fis, keystorePassword.toCharArray());
			}

			// Password of the keystore file
			keyMgrFactory.init(self, tls.getKeystorePass().toCharArray());

			// Trust Manager Factory type (e.g., ??)
			trustMgrFactory =
					TrustManagerFactory.getInstance(
							TrustManagerFactory.getDefaultAlgorithm());

			// initialize trust store
	        this.trust = initTrust(this.tlsConfig, this.trustMgrFactory);

			// Example: "TLS"
			sslContext = SSLContext.getInstance(tls.getContext());
			sslContext.init(keyMgrFactory.getKeyManagers(),    // Key Manager(s)
					trustMgrFactory.getTrustManagers(),    // Trust Manager(s)
					new SecureRandom());			// Random-ness


		} catch (Exception e) {
			logger.warn("Problem initializing the default SSL context: ", e);
		}

		Assertion.post(sslContext != null, "SSL context not initialized");
		
		return sslContext;
	}
	
	public static KeyStore initTrust(Tls tlsConfig, TrustManagerFactory trustMgrFactory) {
	    
	    KeyStore trust = null;
	    
	    try {
	        String truststoreType = tlsConfig.getTruststore();

	        if (Strings.isNullOrEmpty(truststoreType)) {
	            throw new IllegalArgumentException("empty truststore type");
	        }

	        // Truststore type (same as keystore types - e.g., "JKS")

	        trust = KeyStore.getInstance(truststoreType);


	        String truststoreFile = tlsConfig.getTruststoreFile();

	        if (Strings.isNullOrEmpty(truststoreFile)) {
	            throw new IllegalArgumentException("empty truststore file name");
	        }

	        String truststorePassword = tlsConfig.getTruststorePass();

	        if (Strings.isNullOrEmpty(truststorePassword)) {
	            throw new IllegalArgumentException("empty truststore password");
	        }

	        try(FileInputStream fis = new FileInputStream(truststoreFile)) {
				// Filename of the truststore file
				trust.load(fis, truststorePassword.toCharArray());
			}

	        // Load the certificate revocation list if possible
	        List<Tls.Crl> crlConfigs = tlsConfig.getCrl();

	        if (!tlsConfig.isEnableOCSP() && (crlConfigs == null || crlConfigs.isEmpty())) {
	            logger.warn("TLS enabled, but no certificate revocation lists, and OSCP is not enabled in Core Config!");
	            trustMgrFactory.init(trust);
	        } else {
	            // Lengthy and complex setup for trust manager that can use Certificate Revocation Lists.
	            // Get the trusted certificates from the trust store 
	            Set<TrustAnchor> trustAnchors = new HashSet<TrustAnchor>();
	            Enumeration<String> aliases = trust.aliases();
	            while (aliases.hasMoreElements()) {
	                String alias = aliases.nextElement();
	                KeyStore.Entry entry = trust.getEntry(alias, null);
	                if (entry instanceof KeyStore.TrustedCertificateEntry) {
	                    Certificate cert = ((KeyStore.TrustedCertificateEntry) entry).getTrustedCertificate();
	                    if (cert instanceof X509Certificate) {
	                        trustAnchors.add(new TrustAnchor((X509Certificate)cert, null));
	                    }
	                    logger.debug("Loaded trusted certificate " + alias + " from " + truststoreFile);
	                }
	            }
	            // Add trusted certs to PKIX builder parameters
	            PKIXBuilderParameters pkixParameters = new PKIXBuilderParameters(trustAnchors, null);

	            if (tlsConfig.isEnableOCSP()) {
	                // enable OCSP revocation server usage. The OCSP server spe ifi
	                pkixParameters.setRevocationEnabled(true);
	                Security.setProperty("ocsp.enable", "true");
	            }

	            Set<X509CRL> crls = new HashSet<X509CRL>();
	            
	            for (Tls.Crl crl : crlConfigs) {
	                String crlFilename = crl.getCrlFile();
	                InputStream inStream = null;
	                X509CRL revocationList = null;
	                try {
	                    inStream = new FileInputStream(crlFilename);
	                    revocationList = (X509CRL)CertificateFactory.getInstance("X509").generateCRL(inStream);
	                    if (revocationList != null) {
	                        logger.debug("Loaded CRL from " + crlFilename);
	                        crls.add(revocationList);
	                    }
	                } catch (IOException | CRLException ex) {
	                    logger.error("Failed to load CRL from " + crlFilename + ": " + ex.getMessage());
	                } finally {
	                    if (inStream != null) {
	                        inStream.close();
	                    }
	                }
	            }

	            if (!crls.isEmpty()) {
	                // Add CRL(s) to PKIX builder parameters
	                CollectionCertStoreParameters certStoreParameters = new CollectionCertStoreParameters(crls);
	                Provider[] securityProviders = Security.getProviders();
	                if (securityProviders.length < 1) {
	                    throw new IllegalStateException("No Java security providers configured.");
	                }
	                CertStore crlCertStore = CertStore.getInstance("Collection", certStoreParameters, securityProviders[0]);

	                pkixParameters.addCertStore(crlCertStore);
	            }
	            
	            CertPathTrustManagerParameters trustManagerParameters = new CertPathTrustManagerParameters(pkixParameters);
                trustMgrFactory.init(trustManagerParameters);
	        }
	        
	    if (logger.isDebugEnabled()) {
	    	logger.debug("Trust managers: " + ((trustMgrFactory == null) ? "Trust All" : trustMgrFactory.getTrustManagers().length));
	    }
        
	    } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException | InvalidAlgorithmParameterException | UnrecoverableEntryException e) {
            logger.warn("exception initializing trust store", e);
        }
	   
	    
	    return trust;
	    
	}

	private static SSLParameters initBaseSslParameters(SSLContext sslContext, Tls tls) {
		SSLParameters sslParameters = new SSLParameters();

		sslParameters.setCipherSuites(getCiphers(sslContext));
		sslParameters.setProtocols(getProtocols(tls));
		
		sslParameters.setNeedClientAuth(true);

		return sslParameters;
	}

	private static String[] getCiphers(SSLContext sslContext) {

		List<String> wantedCiphers = new LinkedList<String>();
		// these are Suite B ciphers from http://tools.ietf.org/html/rfc6460
		// always add the 256-bit one:
		wantedCiphers.add("TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384");
		wantedCiphers.add("TLS_AES_128_GCM_SHA256");
		wantedCiphers.add("TLS_AES_256_GCM_SHA384");

		String[] supportedCipherArray = sslContext != null ? sslContext.getSupportedSSLParameters().getCipherSuites() : new String[]{};
		Set<String> supportedCiphers = new HashSet<String>(Arrays.asList(supportedCipherArray));

		wantedCiphers.retainAll(supportedCiphers);

		if (logger.isDebugEnabled()) {
			logger.debug("Supported Ciphers: " );
			for(String cipher : wantedCiphers) {
				logger.debug("   " + cipher);
			}
		}

		return wantedCiphers.toArray(new String[]{ });
	}

	private static String[] getProtocols(final Tls tls) {
		
		logger.debug("TLS version: " + tls.getContext());
		
		return new String[] {
				(tls == null ? ConfigHelper.DEFAULT_SECURITY : tls.getContext())
		};
	}
	
	public static class SSLConfigParams {
	    public String keyManagerFactoryType;
	    public String keystoreType;
	    public String keystoreFile;
	    public String keystorePassword;
	    public String truststoreType;
	    public String truststoreFile;
	    public String truststorePassword;
	}
}
