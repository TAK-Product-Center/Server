package com.bbn.marti.nio.netty.initializers;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;

import org.apache.log4j.Logger;

import com.bbn.marti.config.Federation.FederationOutgoing;
import com.bbn.marti.config.Federation.FederationServer;
import com.bbn.marti.config.Federation.FederationServer.V1Tls;
import com.bbn.marti.config.Filter;
import com.bbn.marti.config.Input;
import com.bbn.marti.config.Tls;
import com.bbn.marti.nio.netty.handlers.NioNettyFederationClientHandler;
import com.bbn.marti.nio.netty.handlers.NioNettyFederationServerHandler;
import com.bbn.marti.nio.netty.handlers.NioNettyStcpServerHandler;
import com.bbn.marti.nio.netty.handlers.NioNettyStcpStaticSubHandler;
import com.bbn.marti.nio.netty.handlers.NioNettyTcpServerHandler;
import com.bbn.marti.nio.netty.handlers.NioNettyTcpStaticSubConnectionHandler;
import com.bbn.marti.nio.netty.handlers.NioNettyTlsServerHandler;
import com.bbn.marti.remote.ConnectionStatus;
import com.bbn.marti.remote.groups.User;
import com.bbn.marti.service.SSLConfig;
import com.google.common.base.Strings;

import io.grpc.netty.GrpcSslContexts;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslProvider;
import tak.server.cot.CotEventContainer;

/*
 */
public abstract class NioNettyInitializer extends ChannelInitializer<SocketChannel> {
	private final static Logger log = Logger.getLogger(NioNettyInitializer.class);
	private Input input;
	private SslContext sslContext;
	private TrustManagerFactory trustMgrFactory;
	private KeyManagerFactory keyMgrFactory;
	private NioNettyInitializer() {}
	
	private NioNettyInitializer(Input input) {
		this.input = input;
	}
	
	public enum Pipeline {
		
		initializer;

		public NioNettyInitializer federationServer(FederationServer federationServer) {
			return new NioNettyInitializer() {
				SslContext sslContext = buildServerSslContext(federationServer.getTls());
				
				@Override
				protected void initChannel(SocketChannel channel) throws Exception {
					SslHandler sslHandler = sslContext.newHandler(channel.alloc());
					sslHandler.engine()
							.setEnabledProtocols(federationServer.getV1Tls()
									.stream()
									.map(V1Tls::getTlsVersion)
									.toArray(String[]::new));

					channel.pipeline()
							.addLast("ssl", sslHandler)
							.addLast(new ByteArrayDecoder())
							.addLast(new ByteArrayEncoder())
							.addLast(new NioNettyFederationServerHandler());
				}

				@Override
				public String toString() {
					return "FederationServerInitializer";
				}
			};
		}

		public NioNettyInitializer federationClient(FederationServer federationServer, FederationOutgoing outgoing, ConnectionStatus status) {
			return new NioNettyInitializer() {
				SslContext sslContext = buildClientSslContext(federationServer.getTls());

				@Override
				protected void initChannel(SocketChannel channel) throws Exception {
					SslHandler sslHandler = sslContext.newHandler(channel.alloc());

					sslHandler.engine()
							.setEnabledProtocols(federationServer.getV1Tls()
									.stream()
									.map(V1Tls::getTlsVersion)
									.toArray(String[]::new));
					
					channel.pipeline()
							.addLast("ssl", sslHandler)
							.addLast(new ByteArrayDecoder())
							.addLast(new ByteArrayEncoder())
							.addLast(new NioNettyFederationClientHandler(outgoing, status));
				};
				
				@Override
				public String toString() {
					return "FederationClientInitializer";
				}
			};
		}
		
		public NioNettyInitializer stcpStaticSubClient(String uid, String protocolStr, String xpath, String name, User user, Filter filter) {
			return new NioNettyInitializer() {
				@Override
				protected void initChannel(SocketChannel channel) throws Exception {
					channel.pipeline()
							.addLast(new ByteArrayDecoder())
							.addLast(new ByteArrayEncoder())
							.addLast(new NioNettyStcpStaticSubHandler(uid, protocolStr, xpath, name, user, filter));
				};
				
				@Override
				public String toString() {
					return "StcpStaticSubClientInitializer";
				}
			};
		}

		
		public NioNettyInitializer tcpStaticSubClient(CotEventContainer cot, boolean isProto) {
			return new NioNettyInitializer() {
				@Override
				protected void initChannel(SocketChannel channel) throws Exception {
					channel.pipeline()
							.addLast(new ByteArrayDecoder())
							.addLast(new ByteArrayEncoder())
							.addLast(new NioNettyTcpStaticSubConnectionHandler(cot, isProto));
				};
				
				@Override
				public String toString() {
					return "TcpStaticSubClientInitializer";
				}
			};
		}
		
		public NioNettyInitializer tcpServer(Input input) {
			return new NioNettyInitializer() {
				@Override
				protected void initChannel(SocketChannel channel) throws Exception {
					channel.pipeline()
							.addLast(new ByteArrayDecoder())
							.addLast(new ByteArrayEncoder())
							.addLast(new NioNettyTcpServerHandler(input));
				}
				
				@Override
				public String toString() {
					return "TcpServerInitializer";
				}
			};
		}

		public NioNettyInitializer stcpServer(Input input) {
			return new NioNettyInitializer() {
				@Override
				protected void initChannel(SocketChannel channel) throws Exception {
					channel.pipeline()
							.addLast(new ByteArrayDecoder())
							.addLast(new ByteArrayEncoder())
							.addLast(new NioNettyStcpServerHandler(input));
				}
				
				@Override
				public String toString() {
					return "StcpServerInitializer";
				}
			};
		}

		public NioNettyInitializer tlsServer(Input input, Tls tls) {
			return new NioNettyInitializer() {
				SslContext sslContext = buildServerSslContext(tls);
				@Override
				protected void initChannel(SocketChannel channel) throws Exception {
					SslHandler sslHandler = sslContext.newHandler(channel.alloc());
					sslHandler.engine()
						.setEnabledProtocols(input.getCoreVersion2TlsVersions().split(","));

					channel.pipeline()
							.addLast("ssl", sslHandler)
							.addLast(new ByteArrayDecoder())
							.addLast(new ByteArrayEncoder())
							.addLast(new NioNettyTlsServerHandler(input));
				}
				
				@Override
				public String toString() {
					return "TlsServerInitializer";
				}
			};
		}
		
		public NioNettyInitializer grpcServer(Input input, Tls tls) {
			return new NioNettyInitializer() {
				{
					buildGrpcServerSslContext(input, tls);
				}

				@Override
				protected void initChannel(SocketChannel ch) throws Exception {}
			};
		}
	}
	
	public Input getInput() {
		return input;
	}
	
	public SslContext getSslContext() {
		return sslContext;
	}

	protected SslContext buildServerSslContext(Tls tls) {
		try {
			initTrust(tls);
		} catch (Exception e) {
			if (log.isDebugEnabled()) {
				log.debug("Could not init trust ", e);
			}
		}
		
		try {
			sslContext = SslContextBuilder.forServer(keyMgrFactory)
					.trustManager(trustMgrFactory)
					.clientAuth(ClientAuth.REQUIRE)
					.sslProvider(SslProvider.OPENSSL)
					.build();
		} catch (SSLException e) {
			log.error("Could not build server ssl context " + e);
		}

		return sslContext;
	}
	
	protected SslContext buildClientSslContext(Tls tls) {
		try {
			initTrust(tls);
		} catch (Exception e) {
			if (log.isDebugEnabled()) {
				log.debug("Could not init trust ", e);
			}
		}
		
		try {
			sslContext = SslContextBuilder.forClient()
					.keyManager(keyMgrFactory)
					.trustManager(trustMgrFactory)
					.clientAuth(ClientAuth.REQUIRE)
					.sslProvider(SslProvider.OPENSSL)
					.build();
		} catch (SSLException e) {
			log.error("Could not build client ssl context " + e);
		}

		return sslContext;
	}
	
	protected SslContext buildGrpcServerSslContext(Input input, Tls tls) {
		try {
			initTrust(tls);
		} catch (Exception e) {
			if (log.isDebugEnabled()) {
				log.debug("Could not init trust ", e);
			}
		}
		
		try {
			sslContext = GrpcSslContexts.configure(SslContextBuilder.forServer(keyMgrFactory), SslProvider.OPENSSL) // this ensures that we are using OpenSSL, not JRE SSL
					.protocols(input.getCoreVersion2TlsVersions().split(","))
					.trustManager(trustMgrFactory)
                    .clientAuth(ClientAuth.REQUIRE) // client auth always required
                    .build();
		} catch (SSLException e) {
			log.error("Could not build grpc server ssl context " + e);
		}
				
        return sslContext;
	}

	
	private void initTrust(Tls tls) throws Exception {
		String keyManager = tls.getKeymanager();
	    
	    if (Strings.isNullOrEmpty(keyManager)) {
	        throw new IllegalArgumentException("empty key manager configuration");
	    }
	    
	    keyMgrFactory = KeyManagerFactory.getInstance(keyManager);
	    
	    String keystoreType = tls.getKeystore();
	    
	    if (Strings.isNullOrEmpty(keystoreType)) {
	        throw new IllegalArgumentException("empty keystore type");
	    }
	    
	    KeyStore self = KeyStore.getInstance(keystoreType);
		
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
		trustMgrFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

		// initialize trust store
		SSLConfig.initTrust(tls, trustMgrFactory);
	}

	protected InputStream getFileOrResource(String name) throws IOException {
		if (getClass().getResource(name) != null) {
			return this.getClass().getResourceAsStream(name);
		}

		return new FileInputStream(name);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		log.error("error initializing pipeline ", cause);
	}
}
