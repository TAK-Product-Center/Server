package org.apache.http.client.fluent;

import javax.net.ssl.SSLContext;

import org.apache.http.client.HttpClient;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.config.Configuration;
import com.bbn.marti.remote.exception.TakException;
import com.bbn.marti.service.DistributedConfiguration;
import com.bbn.marti.service.SSLConfig;

/*
 * 
 * Extend the fluent-hc Executor class so that we can control the http connection pool and SSL/TLS configuration, getting it in the same way as the federate ssl context
 * 
 */
public class FederateHttpClientExecutor extends Executor {

    private static final Logger logger = LoggerFactory.getLogger(FederateHttpClientExecutor.class);

    final static PoolingHttpClientConnectionManager connectionManager;
    final static HttpClient federateHttpClient;
    
    static SSLContext sslContext = null;
    
    static LayeredConnectionSocketFactory sslSocketFactory = null;

    static {
        
        try {
            sslSocketFactory = SSLConnectionSocketFactory.getSystemSocketFactory();
            
            try {

                Configuration config = DistributedConfiguration.getInstance().getRemoteConfiguration();
                
                if (logger.isTraceEnabled()) {
                	logger.trace("federation: " + config.getFederation());
                }

                if (config.getFederation() == null || config.getFederation().getFederationServer() == null || config.getFederation().getFederationServer().getTls() == null) {
                    throw new TakException("Federation TLS configuration not found - unable to initialize federate TLS HTTP client");
                }

                sslContext = SSLConfig.getInstance(DistributedConfiguration.getInstance().getRemoteConfiguration().getFederation().getFederationServer().getTls()).getSSLContext();
                
                if (logger.isTraceEnabled()) {
                	logger.trace("sslContext: " + sslContext);
                }

                // disable hostname validation
                sslSocketFactory = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
               
                // this would be how to do it leaving hostname verifcation on. But we are using the remote host IP address from the federate, so this would never work  
                // sslSocketFactory = new SSLConnectionSocketFactory(sslContext);
                
            } catch (SecurityException e) {
                throw new TakException("exception initializing federate TLS http executor", e);
            }
        } catch (final SSLInitializationException e) {
            throw new TakException("TLS initialization exeception", e);
        }

        final Registry<ConnectionSocketFactory> sfr = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", sslSocketFactory != null ? sslSocketFactory : SSLConnectionSocketFactory.getSocketFactory())
                .build();

        // Set up http connection pool. TODO: move these settings to federation config
        connectionManager = new PoolingHttpClientConnectionManager(sfr);
        connectionManager.setDefaultMaxPerRoute(100);
        connectionManager.setMaxTotal(200);
        connectionManager.setValidateAfterInactivity(1000);
        federateHttpClient = HttpClientBuilder.create().setConnectionManager(connectionManager).build();
    }

    public static Executor newInstance() {
        logger.trace("getting fluent pooling tls http executor: " + federateHttpClient);

        return new Executor(federateHttpClient);
    }

    FederateHttpClientExecutor(HttpClient httpclient) {
        super(httpclient);
    }
}
