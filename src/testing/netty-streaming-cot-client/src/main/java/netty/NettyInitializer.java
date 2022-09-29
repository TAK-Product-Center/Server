package netty;

import java.io.FileInputStream;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslProvider;

public class NettyInitializer extends ChannelInitializer<SocketChannel> {

	private SslContext sslContext;
	private TrustManagerFactory trustMgrFactory;
	private KeyManagerFactory keyMgrFactory;
	
	private NettyClientInputTest.ConnectionMeta cm;
	NettyInitializer(NettyClientInputTest.ConnectionMeta cm) {
		this.cm = cm;
	}
	
	@Override
	protected void initChannel(SocketChannel channel) throws Exception {
		buildClientSslContext();
		
		SslHandler sslHandler = sslContext.newHandler(channel.alloc());
		sslHandler.engine().setEnabledProtocols(new String[] {"TLSv1.2", "TLSv1.3"});
		
		channel.pipeline()
				.addLast("ssl", sslHandler)
				.addLast(new ByteArrayDecoder())
				.addLast(new ByteArrayEncoder())
				.addLast(new NioNettyHandler());
	}
	
	protected SslContext buildClientSslContext() {
		try {
			initTrust();
		} catch (Exception e) {
			System.out.println("Could not init trust " + e);
		}
		
		try {
			sslContext = SslContextBuilder.forClient()
					.keyManager(keyMgrFactory)
					.trustManager(trustMgrFactory)
					.clientAuth(ClientAuth.REQUIRE)
					.sslProvider(SslProvider.OPENSSL)
					.build();
		} catch (SSLException e) {
			System.out.println("Could not build client ssl context " + e);
		}

		return sslContext;
	}
	
	private void initTrust() throws Exception {
		String keyManager = cm.keyManager;
	    keyMgrFactory = KeyManagerFactory.getInstance(keyManager);
	    
	    String keystoreType = cm.keystoreType;
	    
	    if (keystoreType == null || keystoreType == "") {
	        throw new IllegalArgumentException("empty keystore type");
	    }
	    
	    KeyStore self = KeyStore.getInstance(keystoreType);
		
		String keystoreFile = cm.keystoreFile;
		
		if (keystoreFile == null || keystoreFile == "") {
		    throw new IllegalArgumentException("keystore file name empty");
		}
		
		String keystorePassword = cm.keystorePassword;
		
		if (keystorePassword == null || keystorePassword == "") {
		    throw new IllegalArgumentException("empty keystore password");
		}

		try(FileInputStream fis = new FileInputStream(keystoreFile)) {
			// Filename of the keystore file
			self.load(fis, keystorePassword.toCharArray());
		}

		// Password of the keystore file
		keyMgrFactory.init(self, keystorePassword.toCharArray());

		// Trust Manager Factory type (e.g., ??)
		trustMgrFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

		// initialize trust store
		initTrust(trustMgrFactory);
	}

	public KeyStore initTrust(TrustManagerFactory trustMgrFactory) {
	    
	    KeyStore trust = null;
	    
	    try {
	        String truststoreType = cm.truststoreType;

	        if (truststoreType == null || truststoreType == "") {
	            throw new IllegalArgumentException("empty truststore type");
	        }

	        // Truststore type (same as keystore types - e.g., "JKS")
	        trust = KeyStore.getInstance(truststoreType);


	        String truststoreFile = cm.truststoreFile;

	        if (truststoreFile == null || truststoreFile == "") {
	            throw new IllegalArgumentException("empty truststore file name");
	        }

	        String truststorePassword = cm.truststorePassword;

	        if (truststorePassword == null || truststorePassword == "") {
	            throw new IllegalArgumentException("empty truststore password");
	        }

	        try(FileInputStream fis = new FileInputStream(truststoreFile)) {
				// Filename of the truststore file
				trust.load(fis, truststorePassword.toCharArray());
			}
	        
	        // NOTE we are not adding any cert revocations like TAK Server does	        
	        trustMgrFactory.init(trust);

        
	    } catch (Exception e) {
            System.out.println("exception initializing trust store " + e);
        }
	   
	    
	    return trust;
	    
	}
}
