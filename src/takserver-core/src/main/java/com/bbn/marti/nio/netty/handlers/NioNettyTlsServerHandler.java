package com.bbn.marti.nio.netty.handlers;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.dom4j.DocumentException;

import com.bbn.cot.filter.DataFeedFilter;
import com.bbn.marti.config.AuthType;
import com.bbn.marti.config.DataFeed;
import com.bbn.marti.config.Input;
import com.bbn.marti.groups.GroupFederationUtil;
import com.bbn.marti.nio.channel.base.AbstractBroadcastingChannelHandler;
import com.bbn.marti.nio.channel.connections.TcpChannelHandler;
import com.bbn.marti.nio.codec.impls.AbstractAuthCodec;
import com.bbn.marti.nio.codec.impls.AnonymousAuthCodec;
import com.bbn.marti.nio.codec.impls.FileAuthCodec;
import com.bbn.marti.nio.codec.impls.LdapAuthCodec;
import com.bbn.marti.nio.codec.impls.X509AuthCodec;
import com.bbn.marti.nio.listener.ProtocolListener;
import com.bbn.marti.nio.netty.NioNettyBuilder;
import com.bbn.marti.nio.protocol.connections.StreamingCotProtocol;
import com.bbn.marti.nio.protocol.connections.StreamingProtoBufOrCoTProtocol;
import com.bbn.marti.nio.protocol.connections.StreamingProtoBufProtocol;
import com.bbn.marti.remote.groups.AuthStatus;
import com.bbn.marti.remote.groups.ConnectionInfo;
import com.bbn.marti.service.Resources;
import com.bbn.marti.service.TransportCotEvent;
import com.bbn.marti.util.MessageConversionUtil;
import com.google.common.base.Charsets;

import atakmap.commoncommo.protobuf.v1.Takmessage.TakMessage;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import tak.server.Constants;
import tak.server.cot.CotEventContainer;
import tak.server.ignite.IgniteHolder;
import tak.server.proto.StreamingProtoBufHelper;

/*
 */
public class NioNettyTlsServerHandler extends NioNettyHandlerBase {
	private final static Logger log = Logger.getLogger(NioNettyTlsServerHandler.class);
	private ByteBuffer leftovers = null;
	private boolean gotMagic = false;
	private boolean gotSize = false;
	private int nextShift = 0;
	private int nextSize = 0;
	protected AbstractAuthCodec authCodec;
	protected final static byte MAGIC = (byte) 0xbf;
	protected AuthType authenticationType;
	protected ScheduledFuture<?> flushFuture;
	protected TransportCotEvent transport = null;
	
	protected Counter preconvertCounter = null;
	protected Counter writeCounter = null;
	protected Timer writeLatencyTimer = null;
	protected Counter watermarkSkipCounter = null;
	protected Counter readCounter = null;
	protected Counter queueFullCounter = null;
	
	public NioNettyTlsServerHandler(Input input) {
		this.input = input;
		this.authenticationType = input.getAuth();
		
		transport = TransportCotEvent.findByID(input.getProtocol());
		
		// proto only channel so go right to proto without negotiation 		
		if (transport == TransportCotEvent.PROTOTLS) {
			protobufSupported.set(true);
		}
		
		// init metrics
		preconvertCounter = Metrics.counter(Constants.METRIC_MESSAGE_PRECONVERT_COUNT, "takserver", "messaging");
		writeCounter = Metrics.counter(Constants.METRIC_MESSAGE_WRITE_COUNT, "takserver", "messaging");
		writeLatencyTimer = Metrics.timer(Constants.METRIC_MESSAGE_WRITE_LATENCY, "takserver", "messaging");
		watermarkSkipCounter = Metrics.counter(Constants.METRIC_MESSAGE_WATERMARK_SKIP_COUNT, "takserver", "messaging");
		readCounter = Metrics.counter(Constants.METRIC_MESSAGE_READ_COUNT, "takserver", "messaging");
		queueFullCounter = Metrics.counter(Constants.METRIC_MESSAGE_QUEUE_FULL_SKIP);
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		ctx.pipeline()
				.get(SslHandler.class)
				.handshakeFuture()
				.addListener(new GenericFutureListener<Future<Channel>>() {
					@Override
					public void operationComplete(Future<Channel> future) throws Exception {
						if (future.isSuccess()) {
							remoteSocketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
							localSocketAddress = (InetSocketAddress) ctx.channel().localAddress();
							nettyContext = ctx;						
							createConnectionInfo();
							createAdaptedNettyProtocol();
							createAdaptedNettyHandler(connectionInfo);
							((AbstractBroadcastingChannelHandler) channelHandler).withHandlerType("NettyTLS");
							createAuthenticationCodecs();
							setReader();
							setWriter();
							setNegotiator();
							buildCallbacks();
							setupFlushHandler();
							createSubscription();				
						} else {
							ctx.close();
						}
						
					}
				});
		
		try {
			super.channelActive(ctx);
		} catch (Exception e) {
			log.error("exception processing channel activation", e);
		}

	}

	@Override
	protected void createConnectionInfo() {
		connectionInfo = new ConnectionInfo();
		connectionInfo.setAddress(remoteSocketAddress.getHostString());
		connectionInfo.setConnectionId(IgniteHolder.getInstance().getIgniteStringId() + MessageConversionUtil.getConnectionId((SocketChannel) nettyContext.channel()));
		connectionInfo.setPort(remoteSocketAddress.getPort());
		connectionInfo.setTls(true);
		connectionInfo.setClient(true);
		connectionInfo.setInput(input);
		connectionInfo.setCert(getCertFromSslChain(0));
	}

	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) {
		if (connectionInfo != null) {
			if (connectionInfo.getConnectionId() != null) {
				AtomicBoolean cancelFlag = GroupFederationUtil.getInstance().updateCancelMap.get(connectionInfo.getConnectionId());

				if (cancelFlag != null) {
					cancelFlag.set(true);
				}
			}
		}

		if(channelHandler != null) {
			submissionService.handleChannelDisconnect(channelHandler);
			protocolListeners.forEach(listener -> listener.onOutboundClose(channelHandler, protocol));
		}
		
		if (authCodec != null) {
			authCodec.onInboundClose();
			authCodec.onOutboundClose();
		}

		if (ctx != null) {
			ctx.close();
		}
		
		super.channelUnregistered(ctx);
	}

	private void setReader() {
		reader = (msgBytes) -> {
			try {				
				Resources.readParseProcessor.execute(() -> {
					
					readCounter.increment();

					ByteBuffer msgBuf = authCodec.decode(ByteBuffer.wrap(msgBytes));

					if (msgBuf == null || msgBuf.remaining() == 0)
						return;

					if (protobufSupported.get()) {
						convertAndSubmitProtoBufBytesAsCot(msgBuf);
					} else {
						convertAndSubmitBytesAsCot(msgBuf);
					}
				});
			}  catch (Exception e) {
				if (log.isDebugEnabled()) {
					log.debug("error on read convert and submit ", e);
				}
			}
		};
	}
	
	private void setWriter() {
		writer = (data) -> {
			try {
				if (nettyContext.channel().isWritable()) {				
					byte[] bytesToWrite = null;
					if (!protobufSupported.get()) {
						bytesToWrite = data.getOrInstantiateEncoding();
					} else {

						if (data.getProtoBufBytes() != null) {

							bytesToWrite = data.getProtoBufBytes().array();

							preconvertCounter.increment();

						} else {
							bytesToWrite = StreamingProtoBufProtocol.convertCotToProtoBufBytes(data).array();							
						}
					}
					
					currentMessageCount.getAndIncrement();
					
					try {
						
						writeCounter.increment();

						if (data.getCreationTime() > 0) {
							writeLatencyTimer.record(Duration.ofMillis(System.currentTimeMillis() - data.getCreationTime()));
						}
					} catch (Exception e) {
						if (log.isDebugEnabled()) {
							log.debug("metrics exception", e);
						}
					}
					
					// flush if instant flush is set or if queued bytes > flushThreshold
					if (isInstantFlush.get() || (NioNettyBuilder.highMark - nettyContext.channel().bytesBeforeUnwritable()) > NioNettyBuilder.flushThreshold) {
						nettyContext.writeAndFlush(bytesToWrite);
					} else {
						nettyContext.write(bytesToWrite);
					}	
					
					AbstractBroadcastingChannelHandler.totalBytesWritten.getAndAdd(bytesToWrite.length);
					AbstractBroadcastingChannelHandler.totalNumberOfWrites.getAndIncrement();
					((TcpChannelHandler) channelHandler).totalTcpBytesWritten.getAndAdd(bytesToWrite.length);
					((TcpChannelHandler) channelHandler).totalTcpNumberOfWrites.getAndIncrement();	
				} else {
					// do not broker - watermark skip
					watermarkSkipCounter.increment();
				}
			} catch (Exception e) {
				if (log.isTraceEnabled()) {
					if (log.isTraceEnabled()) {
						log.trace("exception writing message", e);
					}
				}
			}
		};
	}


	private void setNegotiator() {
		negotiator = () -> {
			// we only need to negotiate cot proto tls inputs 			
			if (transport == TransportCotEvent.COTPROTOTLS) {
				if (authCodec != null && authCodec.getAuthStatus().get() != AuthStatus.SUCCESS) return;

				try {
					CotEventContainer announcement = StreamingProtoBufOrCoTProtocol
							.buildProtocolAnnouncement(negotiationUuid = UUID.randomUUID().toString());

					nettyContext.writeAndFlush(announcement.getOrInstantiateEncoding());
				} catch (DocumentException e) {
					log.error(e);
				}
			}
		};
	}
	protected void convertAndSubmitBytesAsCot(byte[] msg) {
		convertAndSubmitBytesAsCot(ByteBuffer.wrap(msg));
	}

	protected void convertAndSubmitBytesAsCot(ByteBuffer msg) {

		try {
			
			StreamingCotProtocol.add(builder, Charsets.UTF_8.decode(msg), cotParser(), channelHandler).forEach(c -> {
				if (isNotDOSLimited(c) && isNotReadLimited(c)) {
					if (isDataFeedInput()) {
						DataFeedFilter.getInstance().filter(c, (DataFeed) input);
					}

					protocolListeners.forEach(listener -> {
						try {
							listener.onDataReceived(c, channelHandler, protocol);
						} catch (RejectedExecutionException ree) {
							// count how often full queue has blocked message send

							queueFullCounter.increment();

						}
					});
				}
			});
		} catch (Exception e) {
			if (log.isWarnEnabled()) {
				log.warn("Exception receiving message", e);
			}
		}
	}

	protected void convertAndSubmitProtoBufBytesAsCot(byte[] msg) {
		convertAndSubmitProtoBufBytesAsCot(ByteBuffer.wrap(msg));
	}

	protected void convertAndSubmitProtoBufBytesAsCot(ByteBuffer msg) {
		try {
			ByteBuffer fullBuf = null;
			ByteBuffer buffer = msg;
			if (leftovers == null) {
				// first time through leftovers is null, set fullbuf to new buffer
				fullBuf = buffer;
			} else {
				// set fullbuf to leftovers + buffer
				int binaryLength = buffer.remaining();
				fullBuf = ByteBuffer.allocate(leftovers.remaining() + binaryLength);
				fullBuf.put(leftovers);
				fullBuf.put(buffer);
				fullBuf.flip();
				leftovers = null;
			}

			// try to parse messages out of fullbuf
			while (fullBuf.remaining() > 0) {

				// have we read a size from the stream yet?
				if (nextSize == 0) {

					// size is preceded by the magic byte
					if (!gotMagic) {
						byte nextMagic = fullBuf.get();
						gotMagic = nextMagic == MAGIC;
						if (!gotMagic) {
							log.error("Failed to find magic byte, instead found " + nextMagic);
							break;
						}
					}
				}

				if (!gotSize) {
					gotSize = readSize(fullBuf);
					if (!gotSize) {
						break;
					}
				}

				// do we have enough left in the buffer to read out a full message?
				if (fullBuf.remaining() < nextSize) {
					// haven't got enough for a message, stash the fullbuf in leftovers for next
					// time around
					leftovers = ByteBuffer.allocate(fullBuf.remaining());
					leftovers.put(fullBuf);
					leftovers.flip();
					break;
				}

				// copy bytes for next message into eventBytes
				byte[] eventBytes = new byte[nextSize];
				fullBuf.get(eventBytes);

				// parse and broadcast the message
				TakMessage takMessage = TakMessage.parseFrom(eventBytes);
				CotEventContainer cotEventContainer = StreamingProtoBufHelper.getInstance().proto2cot(takMessage);
				
				if (isNotDOSLimited(cotEventContainer)  && isNotReadLimited(cotEventContainer)) {
					
					if (isDataFeedInput()) {
						DataFeedFilter.getInstance().filter(cotEventContainer, (DataFeed) input);
					}
					
					for (ProtocolListener<CotEventContainer> listener :  protocolListeners) {
						listener.onDataReceived(cotEventContainer, channelHandler, protocol);
					}
				}
				
				// reset parser state
				nextSize = 0;
				nextShift = 0;
				gotMagic = false;
				gotSize = false;
				leftovers = null;
			}
		} catch (Exception e) {
			log.error("Exception in convertAndSubmitProtoBufBytesAsCot!", e);
			channelHandler.forceClose();
		}
	}

	private boolean readSize(ByteBuffer buffer) {
		while (buffer.remaining() > 0) {
			byte b = buffer.get();
			if ((b & 0x80) == 0) {
				nextSize = nextSize | (b << nextShift);
				return true;
			} else {
				nextSize |= (b & 0x7F) << nextShift;
				nextShift += 7;
			}
		}
		return false;
	}

	protected void createAuthenticationCodecs() {
		if (authenticationType == AuthType.FILE) {
			authCodec = new FileAuthCodec(createAdaptedNettyPipelineContext());
			authCodec.setConnectionInfo(connectionInfo);
			authCodec.onConnect();
		} else if (authenticationType == AuthType.LDAP) {
			authCodec = new LdapAuthCodec(createAdaptedNettyPipelineContext());
			authCodec.setConnectionInfo(connectionInfo);
			authCodec.onConnect();
		} else if (authenticationType == AuthType.X_509) {
			authCodec = new X509AuthCodec(createAdaptedNettyPipelineContext());
			authCodec.setConnectionInfo(connectionInfo);
			authCodec.onConnect();
		} else if(authenticationType == AuthType.ANONYMOUS) {
			authCodec = new AnonymousAuthCodec(createAdaptedNettyPipelineContext(), input);
			authCodec.setConnectionInfo(connectionInfo);
			authCodec.onConnect();
		}
	}
}
