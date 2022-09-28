package com.bbn.marti.nio.netty.handlers;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateRevokedException;
import java.security.cert.X509Certificate;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.SSLPeerUnverifiedException;

import org.apache.log4j.Logger;

import com.bbn.cot.CotParserCreator;
import com.bbn.marti.config.DataFeed;
import com.bbn.marti.config.Input;
import com.bbn.marti.groups.GroupFederationUtil;
import com.bbn.marti.groups.MessagingUtilImpl;
import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.nio.channel.base.AbstractBroadcastingChannelHandler;
import com.bbn.marti.nio.channel.connections.TcpChannelHandler;
import com.bbn.marti.nio.codec.PipelineContext;
import com.bbn.marti.nio.listener.AbstractAutoProtocolListener;
import com.bbn.marti.nio.listener.ProtocolListener;
import com.bbn.marti.nio.netty.NioNettyBuilder;
import com.bbn.marti.nio.protocol.Protocol;
import com.bbn.marti.nio.protocol.base.AbstractBroadcastingProtocol;
import com.bbn.marti.nio.protocol.connections.StreamingProtoBufOrCoTProtocol;
import com.bbn.marti.nio.server.Server;
import com.bbn.marti.remote.groups.ConnectionInfo;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.service.DistributedConfiguration;
import com.bbn.marti.service.DistributedSubscriptionManager;
import com.bbn.marti.service.Resources;
import com.bbn.marti.service.SubmissionService;
import com.bbn.marti.service.TransportCotEvent;
import com.bbn.marti.util.MessagingDependencyInjectionProxy;
import com.bbn.marti.util.concurrent.future.AsyncFuture;
import com.bbn.marti.util.spring.SpringContextBeanForApi;
import com.google.common.base.Strings;

import io.micrometer.core.instrument.Metrics;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.ssl.SslHandler;
import tak.server.Constants;
import tak.server.cot.CotEventContainer;
import tak.server.cot.CotParser;
import tak.server.federation.DistributedFederationManager;
import tak.server.qos.MessageDOSStrategy;
import tak.server.qos.MessageDeliveryStrategy;
import tak.server.qos.MessageReadStrategy;

/*
 */
public abstract class NioNettyHandlerBase extends SimpleChannelInboundHandler<byte[]> {
	private final static Logger log = Logger.getLogger(NioNettyHandlerBase.class);
	protected SubmissionService submissionService;
	protected DistributedSubscriptionManager subscriptionManager;
	protected DistributedFederationManager federationManager;
	protected GroupFederationUtil groupFederationUtil;
	protected MessagingUtilImpl messagingUtil;
	protected DistributedConfiguration config;
	protected final GroupManager groupManager;
	protected Input input;
	private AtomicBoolean isDataFeedInput = null;
	protected String negotiationUuid;
	protected final static String TAK_PROTO_VERSION = "1";
	protected final static String TAK_REQUEST_TYPE = "t-x-takp-q";
	protected AtomicBoolean protobufSupported = new AtomicBoolean();
	protected volatile StringBuffer builder = new StringBuffer("");
	protected ChannelHandler channelHandler;
	protected Protocol<CotEventContainer> protocol;
	protected ConnectionInfo connectionInfo;
	protected InetSocketAddress remoteSocketAddress;
	protected InetSocketAddress localSocketAddress;
	protected Reader reader;
	protected Writer writer;
	protected Negotiator negotiator;
	protected ChannelHandlerContext nettyContext;
	protected ProtocolListener<CotEventContainer> negotiationListener;
	protected ConcurrentLinkedQueue<ProtocolListener<CotEventContainer>> protocolListeners;
	protected static final int MINUTE_IN_MILLIS = 60000;
	protected AtomicInteger currentMessageCount = new AtomicInteger();
	protected AtomicLong lastMessageCountResetTime = new AtomicLong(System.currentTimeMillis());
	protected AtomicBoolean isInstantFlush = new AtomicBoolean(true);
	protected ScheduledFuture<?> flushFuture;


	public NioNettyHandlerBase()  {
		groupManager = getGroupManager();
		submissionService = SubmissionService.getInstance();
		subscriptionManager = DistributedSubscriptionManager.getInstance();
		federationManager = DistributedFederationManager.getInstance();
		groupFederationUtil = GroupFederationUtil.getInstance();
		messagingUtil = MessagingUtilImpl.getInstance();
		config = DistributedConfiguration.getInstance();
	}
	
	private ThreadLocal<CotParser> cotParser = new ThreadLocal<>();
	
	protected CotParser cotParser() {
		if (cotParser.get() == null) {
			cotParser.set(new CotParser(false));
		}
		
		return cotParser.get();
	}

	@FunctionalInterface
	protected interface Reader {
		void read(byte[] msg);
	}

	@FunctionalInterface
	protected interface Writer {
		void write(CotEventContainer data);
	}

	@FunctionalInterface
	protected interface Negotiator {
		void negotiate();
	}

	private static final AtomicReference<GroupManager> groupManagerRef = new AtomicReference<>();

	private GroupManager getGroupManager() {
		if (groupManagerRef.get() == null) {
			synchronized (groupManagerRef) {
				if (groupManagerRef.get() == null) {
					groupManagerRef.set(SpringContextBeanForApi.getSpringContext().getBean(GroupManager.class));
				}
			}
		}

		return groupManagerRef.get();
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, byte[] msg) throws Exception {

		AbstractBroadcastingChannelHandler.totalBytesRead.getAndAdd(msg.length);
		AbstractBroadcastingChannelHandler.totalNumberOfReads.getAndIncrement();

		((TcpChannelHandler) channelHandler).totalTcpBytesRead.getAndAdd(msg.length);
		((TcpChannelHandler) channelHandler).totalTcpNumberOfReads.getAndIncrement();
		connectionInfo.getReadCount().getAndIncrement();
		reader.read(msg);

	}

	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) {
		try {
			Metrics.counter(Constants.METRIC_CLIENT_DISCONNECT, "takserver", "messaging").increment();
		} catch (Exception e) {
			if (log.isDebugEnabled()) {
				log.debug("exception writing metric", e);
			}
		}

		if (ctx != null) {
			try {
				super.channelUnregistered(ctx);
			} catch (Exception e) {
				log.error("exception unregistering client channel", e);
			}
		}
	}

	@Override
	public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
		super.channelWritabilityChanged(ctx);
		// if we somehow fill the buffer without flushing - force the flush now
		if (!ctx.channel().isWritable()) {
			nettyContext.flush();
		}
	}

	protected abstract void createConnectionInfo();

	protected boolean isNotDOSLimited(CotEventContainer c) {
		MessageDOSStrategy mdoss = MessagingDependencyInjectionProxy.getInstance().mdoss();
		return mdoss == null || connectionInfo == null || Strings.isNullOrEmpty(connectionInfo.getAddress()) || mdoss.isAllowed(c, connectionInfo.getAddress());
	}

	protected boolean isNotReadLimited(CotEventContainer c) {
		MessageReadStrategy mrs = MessagingDependencyInjectionProxy.getInstance().mrs();
		return mrs == null || connectionInfo == null || Strings.isNullOrEmpty(connectionInfo.getConnectionId()) || mrs.isAllowed(c, connectionInfo.getConnectionId());
	}

	protected boolean isNotDeliveryLimited(CotEventContainer c) {
		MessageDeliveryStrategy mds = MessagingDependencyInjectionProxy.getInstance().mds();
		return mds == null || connectionInfo == null || Strings.isNullOrEmpty(connectionInfo.getConnectionId()) || mds.isAllowed(c, connectionInfo.getConnectionId());
	}

	protected void createAdaptedNettyProtocol() {
		protocol = new AbstractBroadcastingProtocol<CotEventContainer>() {
			@Override
			public void negotiate() {
				negotiator.negotiate();
			}

			@Override
			public void onConnect(ChannelHandler handler) {
			}

			@Override
			public void onDataReceived(ByteBuffer buffer, ChannelHandler handler) {
			}

			@Override
			public AsyncFuture<Integer> write(CotEventContainer data, ChannelHandler handler) {
				connectionInfo.getProcessedCount().getAndIncrement();

				if (isNotDeliveryLimited(data)) {
					writer.write(data);
				}

				return null;
			}

			@Override
			public void onInboundClose(ChannelHandler handler) {
			}

			@Override
			public void onOutboundClose(ChannelHandler handler) {
			}

		};
		protocolListeners = ((AbstractBroadcastingProtocol<CotEventContainer>) protocol).getProtocolListeners();
	}

	protected void createAdaptedNettyHandler(ConnectionInfo ci) {
		channelHandler = new TcpChannelHandler() {

			{
				withConnectionInfo(ci);
				ci.setHandler(this);

				if (nettyContext != null)
					storeLocalPort(localSocketAddress.getPort());

				if (connectionInfo.getInput() != null)
					withInput(connectionInfo.getInput());
			}

			private final String notImplementedMessage = "No-op Netty ChannelHandler";

			@Override
			public boolean handleRead(SelectableChannel channel, Server server, ByteBuffer buff) {
				return false;
			}

			@Override
			public boolean handleWrite(SelectableChannel channel, Server server, ByteBuffer buff) {
				return false;
			}

			@Override
			public boolean handleConnect(SelectableChannel channel, Server server) {
				return false;
			}

			@Override
			public boolean handleAccept(SelectableChannel channel, Server server) {
				return false;
			}

			@Override
			public AsyncFuture<ChannelHandler> connect() {
				throw new UnsupportedOperationException(notImplementedMessage + " connect");
			}

			@Override
			public AsyncFuture<Integer> write(ByteBuffer buffer) {
				throw new UnsupportedOperationException(notImplementedMessage + " write");
			}

			@Override
			public AsyncFuture<ChannelHandler> close() {
				throw new UnsupportedOperationException(notImplementedMessage + " close");
			}

			@Override
			public void forceClose() {
				if (nettyContext != null && nettyContext.channel().isActive()) {
					nettyContext.close();
				}
			}

			@Override
			public InetAddress host() {
				return remoteSocketAddress.getAddress();
			}

			@Override
			public int port() {
				return remoteSocketAddress.getPort();
			}

			@Override
			public String netProtocolName() {
				return connectionInfo.isTls() ? "tls" : "tcp";
			}

			@Override
			public boolean isMatchingInput(Input input) {
				return false;
			}

			@Override
			public String toString() {
				return "Netty Dummy TCP Channel server on local port " + localPort() + " client: " + host() + ":" + port();
			}
		};
	}

	protected PipelineContext createAdaptedNettyPipelineContext() {
		return new PipelineContext() {

			@Override
			public void scheduleReadCheck() {}

			@Override
			public void scheduleWriteCheck() {}

			@Override
			public void scheduleRead(ByteBuffer buffer) {}

			@Override
			public void scheduleWrite(ByteBuffer buffer) {}

			@Override
			public void reportException(Exception e) {}
		};
	}

	protected void buildCallbacks() {
		if (connectionInfo.isTls() && !protobufSupported.get()) {
			protocol.addProtocolListener(
					negotiationListener = negotiationListenerCallback.newInstance(channelHandler, protocol));
		}

		TransportCotEvent transport = TransportCotEvent.findByID(input.getProtocol());

		if (transport != TransportCotEvent.TCP)
			protocol.addProtocolListener(submissionService.callsignExtractorCallback.newInstance(channelHandler, protocol));

		if (input.isArchiveOnly()) {
			protocol.addProtocolListener(
					SubmissionService.InputListenerAuxillaryRouter.onArchiveOnlyDataReceivedCallback
							.newInstance(channelHandler, protocol));
		}

		if (!input.isArchive()) {
			protocol.addProtocolListener(SubmissionService.InputListenerAuxillaryRouter.onNoArchiveDataReceivedCallback
					.newInstance(channelHandler, protocol));
		}

		protocol.addProtocolListener(submissionService.onDataReceivedCallback.newInstance(channelHandler, protocol));

	}

	protected X509Certificate getCertFromSslChain(int index) {
		X509Certificate cert = null;
		try {
			SslHandler sslhandler = (SslHandler) nettyContext.channel().pipeline().get("ssl");
			cert = (X509Certificate) sslhandler.engine().getSession().getPeerCertificates()[index];
		} catch (SSLPeerUnverifiedException e) {
			if(log.isDebugEnabled()) {
				log.debug("Could not get cert at from chain at index " + index, e);
			}
		}
		return cert;
	}

	private AbstractAutoProtocolListener<CotEventContainer> negotiationListenerCallback = new AbstractAutoProtocolListener<CotEventContainer>() {
		@Override
		public void onDataReceived(CotEventContainer data, ChannelHandler handler,
								   Protocol<CotEventContainer> protocol) {
			if (data.getType().compareTo(TAK_REQUEST_TYPE) == 0) {
				processProtocolRequest(data);
			}
		}

		@Override
		public String toString() {
			return "negotiation_listener";
		}
	};

	protected void processProtocolRequest(CotEventContainer protocolRequest) {
		Resources.negotiationProcessor.execute( () -> {
			try {
				String versionRequested = protocolRequest.getDocument()
						.selectSingleNode("/event/detail/TakControl/TakRequest/@version")
						.getText();

				// make sure the client is requesting the currently supported protocol version
				boolean supported = versionRequested.compareTo(TAK_PROTO_VERSION) == 0;
				((AbstractBroadcastingProtocol<CotEventContainer>) protocol)
						.removeProtocolListener(negotiationListener);
				CotEventContainer response = StreamingProtoBufOrCoTProtocol.buildProtocolResponse(supported,
						negotiationUuid);
				nettyContext.writeAndFlush(response.getOrInstantiateEncoding());
				protobufSupported.set(supported);
			} catch (Exception e) {
				log.error("exception in processProtocolRequest! ", e);
			}
		});
	}

	protected void createSubscription() {
		if (input == null) return;
		
		submissionService.createSubscriptionFromConnection(channelHandler, protocol);
	}

	protected void createWebsocketSubscription(UUID websocketApiNode) {
		submissionService.createSubscriptionFromConnection(channelHandler, protocol, websocketApiNode);
	}

	public Exception spelunkToBottomOfExceptionChain(Throwable e) {

		if(e.getCause() != null) {
			return spelunkToBottomOfExceptionChain(e.getCause());
		}
		return (Exception) e;
	}

	public String getCertificateName(Throwable t) {

		try {
			if (t instanceof CertPathValidatorException) {
				CertPathValidatorException certPathValidatorException = (CertPathValidatorException) t;
				X509Certificate x509Certificate = (X509Certificate) certPathValidatorException
						.getCertPath().getCertificates().get(certPathValidatorException.getIndex());
				return x509Certificate.getSubjectX500Principal().getName();
			} else if (t.getCause() != null) {
				return getCertificateName(t.getCause());
			}
		} catch (Exception e) {
			log.error(String.format("Exception in getCertificateName: %s", e.getLocalizedMessage()));
		}
		return "";
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {

		StringBuffer additionalInfo = new StringBuffer();

		// get info from remote socket
		InetSocketAddress remoteAddress = (InetSocketAddress)ctx.channel().remoteAddress();
		if (remoteAddress != null) {
			if (remoteAddress.getAddress()!= null) {
				if (remoteAddress.getAddress().getHostAddress() != null) {
					additionalInfo.append("Remote address: ").append(remoteAddress.getAddress().getHostAddress()).append("; ");
				}
			}
			additionalInfo.append("Remote port: ").append(remoteAddress.getPort()).append("; ");
		}

		// get info from local
		InetSocketAddress localAddress = (InetSocketAddress)ctx.channel().localAddress();
		if (localAddress != null) {
			additionalInfo.append("Local port: ").append(localAddress.getPort()).append("; ");
		}

		Exception bottomOfExceptionChain = spelunkToBottomOfExceptionChain(cause);
		if (bottomOfExceptionChain != null && bottomOfExceptionChain instanceof CertificateExpiredException) {
			log.error(String.format("Connection rejected for expired client certificate: %s, %s, %s.",
					getCertificateName(cause), bottomOfExceptionChain.getMessage()));
		} else if (bottomOfExceptionChain != null && bottomOfExceptionChain instanceof CertificateRevokedException) {
			log.error(String.format("Connection rejected for revoked client certificate: %s, %s.",
					getCertificateName(cause), bottomOfExceptionChain.getMessage()));
		} else {
			// get certificate info if available
			SslHandler sslhandler = (SslHandler) ctx.channel().pipeline().get("ssl");
			if (sslhandler != null && sslhandler.engine() != null && sslhandler.engine().getSession()!=null) {
				try {
					Certificate[] certificates = sslhandler.engine().getSession().getPeerCertificates();
					X509Certificate x509Certificate = (X509Certificate)certificates[0];

					additionalInfo.append("Peer certificate Subject DN: ").append(x509Certificate.getSubjectDN()).append("; ");

					if (log.isDebugEnabled()) {
						log.debug("Peer certificate: " + x509Certificate.toString());
					}

				} catch (SSLPeerUnverifiedException e) {
					additionalInfo.append("Certificate error: ").append(e.getLocalizedMessage()).append("; ");
				} catch (IndexOutOfBoundsException e) {
					additionalInfo.append("No peer certificate found; ");
				}

			}

			log.error(String.format("NioNettyServerHandler error. Cause: %s. Additional info: %s", cause.getMessage(), additionalInfo.toString()));

		}

		channelUnregistered(ctx);
		ctx.close();
	}

	public ChannelHandler getChannelHandler() {
		return channelHandler;
	}

	public Protocol<CotEventContainer> getProtocol() {
		return protocol;
	}

	protected void setupFlushHandler() {
		flushFuture = Resources.flushPool.scheduleAtFixedRate(() -> {
			// connection closed - cancel scheduler
			if (!nettyContext.channel().isOpen()) {
				flushFuture.cancel(true);
				return;
			}

			// flush if we have buffered bytes
			if (nettyContext.channel().bytesBeforeUnwritable() != NioNettyBuilder.highMark) {
				nettyContext.flush();
			}

			long curtime = System.currentTimeMillis();

			// if we handled more messages than the preferred amount per minute, cancel instant flushes
			if (currentMessageCount.get() >= NioNettyBuilder.maxOptimalMessagesPerMinute) {
				currentMessageCount.set(0);
				isInstantFlush.set(curtime > lastMessageCountResetTime.getAndSet(curtime) + MINUTE_IN_MILLIS);
			}

			// if the load has subsided, be proactive about turning instant flushes back on
			// rather than waiting for the message threshold to be passed
			if (curtime > lastMessageCountResetTime.get() + MINUTE_IN_MILLIS) {
				isInstantFlush.set(true);
				currentMessageCount.set(0);
				lastMessageCountResetTime.set(curtime);
			}

		}, 0, config.getBuffer().getQueue().getFlushInterval(), TimeUnit.MILLISECONDS);
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) {

		try {
			Metrics.counter(Constants.METRIC_CLIENT_CONNECT, "takserver", "messaging").increment();
		} catch (Exception e) {
			if (log.isDebugEnabled()) {
				log.debug("exception writing metric", e);
			}
		}

		try {
			super.channelActive(ctx);
		} catch (Exception e) {
			log.error("error recording channel activation", e);
		}

	}
	
	protected boolean isDataFeedInput() {
		if (input == null) return false;
		
		if (isDataFeedInput == null) {
			if (input instanceof DataFeed) {
				isDataFeedInput = new AtomicBoolean(true);
			} else {
				isDataFeedInput = new AtomicBoolean(false);
			}
		}
		
		return isDataFeedInput.get();
	}

}