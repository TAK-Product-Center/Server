package com.bbn.marti.nio.protocol.connections;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;

import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.nio.listener.AbstractAutoProtocolListener;
import com.bbn.marti.nio.listener.ProtocolListener;
import com.bbn.marti.nio.listener.ProtocolListenerInstantiator;
import com.bbn.marti.nio.protocol.Protocol;
import com.bbn.marti.nio.protocol.ProtocolInstantiator;
import com.bbn.marti.nio.protocol.base.AbstractBroadcastingProtocol;
import com.bbn.marti.nio.util.Protocols;
import com.bbn.marti.remote.util.DateUtil;
import com.bbn.marti.service.Resources;
import com.bbn.marti.util.Assertion;
import com.bbn.marti.util.MessagingDependencyInjectionProxy;
import com.bbn.marti.util.concurrent.future.AsyncFuture;

import tak.server.Constants;
import tak.server.cot.CotEventContainer;
import tak.server.proto.StreamingProtoBufHelper;


public class StreamingProtoBufOrCoTProtocol extends AbstractBroadcastingProtocol<CotEventContainer>  {

    private final static Logger log = Logger.getLogger(StreamingProtoBufProtocol.class);
    private final static int TIMEOUT_MILLIS = 60000;
    private final static String TAK_ANNOUNCE_TYPE = "t-x-takp-v";
    private final static String TAK_REQUEST_TYPE = "t-x-takp-q";
    private final static String TAK_RESPONSE_TYPE = "t-x-takp-r";

    private String negotiationUuid;
    private ChannelHandler handler;
    private volatile ProtocolListener<CotEventContainer> negotiationListener = null;
    private volatile AbstractBroadcastingProtocol<CotEventContainer> protocol = null;
    private ScheduledFuture<?> scheduledFuture = null;
    private static String version = null;

    /**
     * A static, inner factory class that returns a new instance of the outer.
     */
    public final static ProtocolInstantiator<CotEventContainer> streamingProtoBufOrCotInstantiator =
            new ProtocolInstantiator<CotEventContainer>() {
        @Override
        public StreamingProtoBufOrCoTProtocol newInstance(ChannelHandler handler) {
            return new StreamingProtoBufOrCoTProtocol(handler);
        }
        public String toString() { return "ProtoBuf_or_CoT_streaming_protocol_builder"; }
    };


    public StreamingProtoBufOrCoTProtocol(ChannelHandler handler) {
        this.handler = handler;
    }

    /**
     * Called when the handler first connects.
     *
     * Instantiate our parser/message buffer.
     */
    @Override
    public void onConnect(ChannelHandler handler) {
        Assertion.notNull(handler);

        // default to the streaming cot protocol
        protocol = new StreamingCotProtocol();

        // call the StreamingCotProtocol onConnect handler. do this prior to copying our listeners over since
        // we want the onConnect broadcast to come from a StreamingProtoBufOrCoTProtocol instance
        protocol.onConnect(handler);

        // establish a listener here for the TakRequest message sent as part of the negotiation
        setupNegotiationListener();

        // now go ahead and copy our listeners to the StreamingCotProtocol
        copyProtocolListeners(protocol);

        // finally go ahead and broadcast the onConnect to our listeners
        super.broadcastOnConnect(handler, this);
    }

    @Override
    public void onDataReceived(ByteBuffer buffer, ChannelHandler handler) {
        protocol.onDataReceived(buffer, handler);
    }

    /**
     * Called when the application wants to send data to the network
     *
     * Convert data to its byte format, hand data to the channel's handler.
     */
    @Override
    public AsyncFuture<Integer> write(CotEventContainer data, ChannelHandler handler) {
        return protocol.write(data, handler);
    }

    /**
     * Called when the handler has finished propagating
     * data coming from the network, signifies an EOS
     */
    @Override
    public void onInboundClose(ChannelHandler handler) {
        // give the protocol a chance to respond to the inbound close
        protocol.onInboundClose(handler);
    }

    /**
     * Called when the handler would like to
     */
    @Override
    public void onOutboundClose(ChannelHandler handler) {
        // give the protocol a chance to respond to the outbound close
        protocol.onOutboundClose(handler);
    }

    /**
     * @note DO NOT put the channel handler in the string -- typically prints out its listener as
     * part of its toString method
     */
    @Override
    public String toString() {
        return "server_streaming_protobuf_or_cot";
    }

    //
    // Negotiation protocol
    //

    @Override
    public void negotiate() {
        sendProtocolAnnouncement();
    }

    public void sendProtocolAnnouncement() {
        try {
            CotEventContainer cotEventContainer = buildProtocolAnnouncement(negotiationUuid = UUID.randomUUID().toString());
            write(cotEventContainer, handler);

        } catch (Exception e) {
            log.error("exception in sendProtocolAnnouncement!", e);
        }
    }

    private static String getVersion() {
    	if (version == null) {
    		synchronized (StreamingProtoBufOrCoTProtocol.class) {
    			try {
    		
    				return MessagingDependencyInjectionProxy.getInstance().versionBean().getVer();

    			} catch (Exception e) {
    				log.error("Exception in getVersion", e);
    			}
    		}
    	}

        return version;
    }

	public static CotEventContainer buildProtocolAnnouncement(String negotiationUuid) throws DocumentException {

        if (log.isDebugEnabled()) {
            log.debug("buildProtocolAnnouncement for : " + negotiationUuid);
        }

		long millis = System.currentTimeMillis();
		String startAndTime = DateUtil.toCotTime(millis);
		String stale = DateUtil.toCotTime(millis + TIMEOUT_MILLIS);

		// TODO need to add supported versions to CoreConfig and to this message

		String announcement = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?>" +
		        "<event version='2.0' uid='" + negotiationUuid + "' type='" + TAK_ANNOUNCE_TYPE +"' time='"
		        + startAndTime + "' start='" + startAndTime + "' stale='" + stale + "' how='m-g'>" +
		        "<point lat='0.0' lon='0.0' hae='0.0' ce='999999' le='999999'/>" +
		        "<detail><TakControl><TakProtocolSupport version='1'/>" +
                "<TakServerVersionInfo serverVersion='" + getVersion() + "' apiVersion='" + Constants.API_VERSION + "'/>" +
                "</TakControl></detail>" +
		        "</event>";

		SAXReader reader = new SAXReader();
		Document doc = reader.read(new ByteArrayInputStream(announcement.getBytes()));
		CotEventContainer cotEventContainer = new CotEventContainer(doc);
		return cotEventContainer;
	}

    private void processProtocolRequest(CotEventContainer protocolRequest) {
        try {
            String versionRequested = protocolRequest.getDocument().
                    selectSingleNode("/event/detail/TakControl/TakRequest/@version").getText();

            // make sure the client is requesting the currently supported protocol version
            boolean supported = versionRequested.compareTo(StreamingProtoBufHelper.TAK_PROTO_VERSION) == 0;

            // Stop listening now that it's been set -- execute as a separate task, avoid hitting our own rwlock
            Resources.removeProtoListenerPool.execute(
                    Protocols.removeProtocolListenerTask(protocol, negotiationListener));

            // cancel the scheduled listener removal
            if (scheduledFuture != null) {
                scheduledFuture.cancel(false);
                scheduledFuture = null;
            }

            sendProtocolResponse(supported);

            if (supported) {
                // TODO need to add version to StreamingProtoBufProtocol constructor
                // and modify StreamingProtoBufProtocol to support multiple protocol versions
                protocol = new StreamingProtoBufProtocol();
                copyProtocolListeners(protocol);
            } else {
                setupNegotiationListener();
            }

        } catch (Exception e) {
            log.error("exception in processProtocolRequest!", e);
        }
    }

    private void sendProtocolResponse(boolean status) {
        try {
            CotEventContainer cotEventContainer = buildProtocolResponse(status,negotiationUuid);

            write(cotEventContainer, handler);

        } catch (Exception e) {
            log.error("exception in sendProtocolAnnouncement!", e);
        }
    }

	public static CotEventContainer buildProtocolResponse(boolean status, String negotiationUuid) throws DocumentException {

        if (log.isDebugEnabled()) {
            log.debug("buildProtocolResponse for : " + negotiationUuid + ", " + status);
        }

		long millis = System.currentTimeMillis();
		String startAndTime = DateUtil.toCotTime(millis);
		String stale = DateUtil.toCotTime(millis + TIMEOUT_MILLIS);

		String response = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?>" +
		        "<event version='2.0' uid='" + negotiationUuid + "' type='" + TAK_RESPONSE_TYPE +"' time='"
		        + startAndTime + "' start='" + startAndTime + "' stale='" + stale + "' how='m-g'>" +
		        "<point lat='0.0' lon='0.0' hae='0.0' ce='999999' le='999999'/>" +
		        "<detail><TakControl><TakResponse status='" + status + "'/></TakControl></detail>" +
		        "</event>";

		SAXReader reader = new SAXReader();
		Document doc = reader.read(new ByteArrayInputStream(response.getBytes()));
		CotEventContainer cotEventContainer = new CotEventContainer(doc);
		return cotEventContainer;
	}

    private void setupNegotiationListener() {
        negotiationListener = negotiationCallback.newInstance(handler, protocol);

        Resources.addListenerPool.execute(
                Protocols.addProtocolListenerTask(protocol, negotiationListener));

        scheduledFuture = Resources.removeProtoListenerPool.schedule(
                Protocols.removeProtocolListenerTask(
                        protocol, negotiationListener), TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

    }

    public final ProtocolListenerInstantiator<CotEventContainer> negotiationCallback =
            new AbstractAutoProtocolListener<CotEventContainer>() {
    	
    	private static final long serialVersionUID = 9879877823L;

		@Override
        public void onDataReceived(
                CotEventContainer data, ChannelHandler handler, Protocol<CotEventContainer> protocol) {
            if (data.getType().compareTo(TAK_REQUEST_TYPE) == 0) {
                processProtocolRequest(data);
            }
        }

        @Override
        public String toString() {
            return "negotiation_listener";
        }
    };
}