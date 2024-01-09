package com.bbn.marti.nio.grpc;

import java.net.InetSocketAddress;
import java.security.cert.X509Certificate;

import com.bbn.cot.filter.DataFeedFilter;
import com.bbn.marti.config.DataFeed;
import com.bbn.marti.config.Input;
import com.bbn.marti.nio.channel.base.AbstractBroadcastingChannelHandler;
import com.bbn.marti.nio.netty.handlers.NioNettyTlsServerHandler;
import com.bbn.marti.remote.InputMetric;
import com.bbn.marti.remote.groups.ConnectionInfo;

import atakmap.commoncommo.protobuf.v1.Takmessage.TakMessage;
import io.grpc.stub.StreamObserver;
import io.netty.channel.ChannelHandlerContext;
import tak.server.cot.CotEventContainer;
import tak.server.ignite.IgniteHolder;
import tak.server.proto.StreamingProtoBufHelper;

/*
 */
public class NioGrpcChannelHandler extends NioNettyTlsServerHandler {
    private final StreamObserver<TakMessage> stream;
    private final X509Certificate clientCertificate;
    
    public NioGrpcChannelHandler(Input input, StreamObserver<TakMessage> stream, X509Certificate clientCertificate, InetSocketAddress local, InetSocketAddress remote) {
        super(input);
        protobufSupported.set(true);;
        this.stream = stream;
        this.clientCertificate = clientCertificate;
        this.localSocketAddress = local;
        this.remoteSocketAddress = remote;
		this.authenticationType = input.getAuth();
    }
    
    public void submitTakMessage(TakMessage message) {
    	CotEventContainer cotEventContainer = StreamingProtoBufHelper.proto2cot(message);
    		
    	if (isNotDOSLimited(cotEventContainer) && isNotReadLimited(cotEventContainer)) {
			if (isDataFeedInput()) {
				DataFeedFilter.getInstance().filter(cotEventContainer, (DataFeed) input);
			}
			protocolListeners.forEach(listener -> listener.onDataReceived(cotEventContainer, channelHandler, protocol));
		}
    	
    	InputMetric inputMetric = submissionService().getInputMetric(input.getName());
    	if (inputMetric != null) {
    		inputMetric.getMessagesReceived().incrementAndGet();
    		inputMetric.getBytesRecieved().addAndGet(message.toString().length());
    	}
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        createConnectionInfo();
        createAdaptedNettyProtocol();
        createAdaptedNettyHandler(connectionInfo);
        ((AbstractBroadcastingChannelHandler) channelHandler).withHandlerType("NettyGRPCWrapper");
        createAuthenticationCodecs();
        setReader();
        setWriter();
        setNegotiator();
        buildCallbacks();
		createSubscription();
    }
    
    @Override
    protected void createConnectionInfo() {
        connectionInfo = new ConnectionInfo();
        connectionInfo.setConnectionId(IgniteHolder.getInstance().getIgniteStringId() + Integer.valueOf(stream.hashCode()).toString());
        connectionInfo.setAddress(remoteSocketAddress.getHostString());
        connectionInfo.setPort(remoteSocketAddress.getPort());
        connectionInfo.setTls(true);
        connectionInfo.setClient(true);
        connectionInfo.setInput(input);
        connectionInfo.setCert(clientCertificate);
    }
    
    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) {    	
    	if (channelHandler != null) {
            submissionService().handleChannelDisconnect(channelHandler);
            protocolListeners.forEach(listener -> listener.onOutboundClose(channelHandler, protocol));
        }
    	
		super.channelUnregistered(ctx);

    }
    
    @Override
    protected void setReader() {
        reader = (msg) -> {};
    }
    
    @Override
    protected void setWriter() {
        writer = (data) -> {
        	stream.onNext(StreamingProtoBufHelper.cot2protoBuf(data));
        };
    }
    
    @Override
    protected void setNegotiator() {
        negotiator = () -> {};
    }
    
}
